
package com.example.watchguide;

import com.example.watchguide.models.Anime;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class FirestoreUserLibrary {

    public interface Listener {
        void onLibraryChanged(Map<Integer, LibraryEntry> data);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String uid;
    private ListenerRegistration reg;
    private final Map<Integer, LibraryEntry> cache = new HashMap<>();

    public FirestoreUserLibrary(String uid) {
        this.uid = uid;
    }

    private CollectionReference libraryCol() {
        return db.collection("users").document(uid).collection("library");
    }

    private CollectionReference activitiesCol() {
        return db.collection("users").document(uid).collection("activities");
    }

    //Escuchar en tiempo real los cambios en la colección users/{uid}/library
    public void listen(Listener listener) {
        if (reg != null) reg.remove();
        reg = libraryCol().addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;
            cache.clear();
            for (DocumentSnapshot d : snap.getDocuments()) {
                LibraryEntry le = d.toObject(LibraryEntry.class);
                if (le != null) {
                    try {
                        int id = Integer.parseInt(d.getId());
                        cache.put(id, le);
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
            listener.onLibraryChanged(new HashMap<>(cache));
        });
    }

    public Map<Integer, LibraryEntry> getCache() {
        return cache;
    }

    public Task<Void> setFavorite(Anime anime, boolean fav) {
        String id = String.valueOf(anime.mal_id);
        Map<String, Object> data = new HashMap<>();
        data.put("favorite", fav);
        data.put("title", anime.title);
        String url = (anime.images != null && anime.images.jpg != null) ? anime.images.jpg.image_url : null;
        data.put("image_url", url);
        data.put("updatedAt", System.currentTimeMillis());

        // Guardar en library
        Task<Void> task = libraryCol().document(id).set(data, SetOptions.merge());

        // Crear actividad en la subcolección del usuario
        Map<String, Object> activity = new HashMap<>();
        activity.put("userId", uid);
        activity.put("userName", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        activity.put("animeId", anime.mal_id);
        activity.put("animeTitle", anime.title);
        activity.put("type", fav ? "favorite_added" : "favorite_removed");
        activity.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("activities")
                .add(activity);

        return task;
    }

    public Task<Void> setRating(Anime anime, float rating) {
        String id = String.valueOf(anime.mal_id);

        // --- Guardar en librería personal ---
        Map<String, Object> data = new HashMap<>();
        data.put("rating", rating);
        data.put("watched", rating > 0);
        data.put("title", anime.title);
        String url = (anime.images != null && anime.images.jpg != null) ? anime.images.jpg.image_url : null;
        data.put("image_url", url);
        data.put("updatedAt", System.currentTimeMillis());
        Task<Void> task = libraryCol().document(id).set(data, SetOptions.merge());

        // --- Actualizar colección global con transaction ---
        DocumentReference animeRef = FirebaseFirestore.getInstance()
                .collection("anime")
                .document(id);

        FirebaseFirestore.getInstance().runTransaction(transaction -> {
            DocumentSnapshot doc = transaction.get(animeRef);

            Map<String, Object> userRatings;
            double ratingSum;
            long ratingCount;

            if (doc.exists()) {
                userRatings = doc.contains("userRatings") ? (Map<String, Object>) doc.get("userRatings") : new HashMap<>();
                ratingSum = doc.contains("ratingSum") ? doc.getDouble("ratingSum") : 0;
                ratingCount = doc.contains("ratingCount") ? doc.getLong("ratingCount") : 0;

                Double oldRating = userRatings.containsKey(uid) ? ((Number) userRatings.get(uid)).doubleValue() : null;

                if (oldRating != null) {
                    ratingSum = ratingSum - oldRating + rating;
                } else {
                    ratingSum += rating;
                    ratingCount += 1;
                }

            } else {
                userRatings = new HashMap<>();
                ratingSum = rating;
                ratingCount = 1;
            }

            userRatings.put(uid, rating);

            Map<String, Object> updates = new HashMap<>();
            updates.put("title", anime.title);
            updates.put("image_url", url);
            updates.put("userRatings", userRatings);
            updates.put("ratingSum", ratingSum);
            updates.put("ratingCount", ratingCount);
            updates.put("averageRating", ratingCount > 0 ? ratingSum / ratingCount : 0);

            transaction.set(animeRef, updates, SetOptions.merge());
            return null;
        });

        // --- Crear actividad ---
        Map<String, Object> activity = new HashMap<>();
        activity.put("userId", uid);
        activity.put("userName", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        activity.put("animeId", anime.mal_id);
        activity.put("animeTitle", anime.title);
        activity.put("type", "rating");
        activity.put("value", rating);
        activity.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("activities")
                .add(activity);


        return task;
    }


    // ---------------------- WATCHED ----------------------
    public Task<Void> setWatched(Anime anime, boolean watched) {
        String id = String.valueOf(anime.mal_id);
        Map<String, Object> data = new HashMap<>();
        data.put("watched", watched);
        data.put("updatedAt", System.currentTimeMillis());

        Task<Void> task = libraryCol().document(id).set(data, SetOptions.merge());

        // Crear actividad
        Map<String, Object> activity = new HashMap<>();
        activity.put("userId", uid);
        activity.put("userName", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        activity.put("animeId", anime.mal_id);
        activity.put("animeTitle", anime.title);
        activity.put("type", watched ? "watched" : "unwatched");
        activity.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("activities")
                .add(activity);
        return task;
    }

    public Task<QuerySnapshot> getFavoritesOnce() {
        return libraryCol().whereEqualTo("favorite", true).get();
    }

    public void stop() {
        if (reg != null) reg.remove();
    }
}

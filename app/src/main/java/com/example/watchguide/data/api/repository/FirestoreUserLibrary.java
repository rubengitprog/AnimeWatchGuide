
package com.example.watchguide.data.api.repository;

import com.example.watchguide.models.Anime;
import com.example.watchguide.models.LibraryEntry;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
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


    //Establecer anime como favorito
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
        activity.put("username", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        activity.put("userPhotoUrl", FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl().toString() : null);
        activity.put("animeId", anime.mal_id);
        activity.put("animeTitle", anime.title);
        activity.put("animeImageUrl", url);
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

        // Guardar el rating del anime
        Map<String, Object> data = new HashMap<>();
        data.put("rating", rating);
        data.put("watched", rating > 0);
        data.put("title", anime.title);
        String url = (anime.images != null && anime.images.jpg != null) ? anime.images.jpg.image_url : null;
        data.put("image_url", url);
        data.put("updatedAt", System.currentTimeMillis());
        Task<Void> task = libraryCol().document(id).set(data, SetOptions.merge());

        // Actualizar el rating global
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

        //Crea la actividad
        Map<String, Object> activity = new HashMap<>();
        activity.put("userId", uid);
        activity.put("username", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        activity.put("userPhotoUrl", FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl().toString() : null);
        activity.put("animeId", anime.mal_id);
        activity.put("animeTitle", anime.title);
        activity.put("animeImageUrl", url);
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


    //  Establecer como visto
    public Task<Void> setWatched(Anime anime, boolean watched) {
        String id = String.valueOf(anime.mal_id);
        Map<String, Object> data = new HashMap<>();
        data.put("watched", watched);
        data.put("title", anime.title);
        String url = (anime.images != null && anime.images.jpg != null) ? anime.images.jpg.image_url : null;
        data.put("image_url", url);
        data.put("updatedAt", System.currentTimeMillis());

        Task<Void> task = libraryCol().document(id).set(data, SetOptions.merge());

        // Crear actividad
        Map<String, Object> activity = new HashMap<>();
        activity.put("userId", uid);
        activity.put("username", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        activity.put("userPhotoUrl", FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl().toString() : null);
        activity.put("animeId", anime.mal_id);
        activity.put("animeTitle", anime.title);
        activity.put("animeImageUrl", url);
        activity.put("type", watched ? "watched" : "unwatched");
        activity.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("activities")
                .add(activity);
        return task;
    }

    // Guardar rating CON review (obligatoria)
    public Task<Void> setRatingWithReview(Anime anime, float rating, String reviewText, String imageUrl) {
        String id = String.valueOf(anime.mal_id);

        // 1. Guardar el rating en la biblioteca del usuario
        Map<String, Object> data = new HashMap<>();
        data.put("rating", rating);
        data.put("watched", true);
        data.put("title", anime.title);
        data.put("image_url", imageUrl);
        data.put("updatedAt", System.currentTimeMillis());
        Task<Void> task = libraryCol().document(id).set(data, SetOptions.merge());

        // 2. Actualizar el rating global del anime
        DocumentReference animeRef = db.collection("anime").document(id);

        db.runTransaction(transaction -> {
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
            updates.put("image_url", imageUrl);
            updates.put("userRatings", userRatings);
            updates.put("ratingSum", ratingSum);
            updates.put("ratingCount", ratingCount);
            updates.put("averageRating", ratingCount > 0 ? ratingSum / ratingCount : 0);

            transaction.set(animeRef, updates, SetOptions.merge());
            return null;
        });

        // 3. Crear o actualizar la review en Firestore solo si hay texto
        if (reviewText != null && !reviewText.isEmpty()) {
            // Primero buscar si ya existe una review de este usuario para este anime
            db.collection("reviews")
                    .whereEqualTo("userId", uid)
                    .whereEqualTo("animeId", anime.mal_id)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            // Ya existe una review, actualizarla
                            String existingReviewId = querySnapshot.getDocuments().get(0).getId();

                            Map<String, Object> reviewUpdate = new HashMap<>();
                            reviewUpdate.put("rating", rating);
                            reviewUpdate.put("reviewText", reviewText);
                            reviewUpdate.put("timestamp", System.currentTimeMillis());

                            db.collection("reviews").document(existingReviewId).update(reviewUpdate);
                        } else {
                            // No existe, crear una nueva
                            Map<String, Object> review = new HashMap<>();
                            review.put("userId", uid);
                            review.put("username", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                            review.put("animeId", anime.mal_id);
                            review.put("animeTitle", anime.title);
                            review.put("animeImageUrl", imageUrl);
                            review.put("rating", rating);
                            review.put("reviewText", reviewText);
                            review.put("timestamp", System.currentTimeMillis());
                            review.put("status", "approved");
                            // Inicializar campos de likes/dislikes/respuestas
                            review.put("likes", new HashMap<String, Boolean>());
                            review.put("likeCount", 0);
                            review.put("dislikeCount", 0);
                            review.put("replyCount", 0);

                            db.collection("reviews").add(review)
                                    .addOnSuccessListener(docRef -> {
                                        // Actualizar el reviewId en el documento
                                        docRef.update("reviewId", docRef.getId());
                                    });
                        }
                    });

            // 4. Crear actividad en el feed (solo si hay review)
            Map<String, Object> activity = new HashMap<>();
            activity.put("userId", uid);
            activity.put("username", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
            activity.put("userPhotoUrl", FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl() != null ?
                    FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl().toString() : null);
            activity.put("animeId", anime.mal_id);
            activity.put("animeTitle", anime.title);
            activity.put("animeImageUrl", imageUrl);
            activity.put("type", "review");
            activity.put("value", rating);
            activity.put("reviewText", reviewText);
            activity.put("timestamp", System.currentTimeMillis());

            db.collection("users")
                    .document(uid)
                    .collection("activities")
                    .add(activity);
        } else {
            // Si no hay review, crear actividad de rating simple
            Map<String, Object> activity = new HashMap<>();
            activity.put("userId", uid);
            activity.put("username", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
            activity.put("userPhotoUrl", FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl() != null ?
                    FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl().toString() : null);
            activity.put("animeId", anime.mal_id);
            activity.put("animeTitle", anime.title);
            activity.put("animeImageUrl", imageUrl);
            activity.put("type", "rating");
            activity.put("value", rating);
            activity.put("timestamp", System.currentTimeMillis());

            db.collection("users")
                    .document(uid)
                    .collection("activities")
                    .add(activity);
        }

        return task;
    }

    // Resetear anime completamente (eliminar rating, review, y estados)
    public Task<Void> resetAnime(Anime anime) {
        String id = String.valueOf(anime.mal_id);

        // Primero obtenemos el rating actual del usuario para actualizar el promedio global
        return libraryCol().document(id).get().continueWithTask(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                DocumentSnapshot userDoc = task.getResult();
                Float oldRating = userDoc.contains("rating") ? userDoc.getDouble("rating").floatValue() : null;

                // Eliminar el documento de la biblioteca del usuario
                Task<Void> deleteTask = libraryCol().document(id).delete();

                // Si había un rating, actualizar las estadísticas globales
                if (oldRating != null) {
                    DocumentReference animeRef = db.collection("anime").document(id);

                    db.runTransaction(transaction -> {
                        DocumentSnapshot animeDoc = transaction.get(animeRef);

                        if (animeDoc.exists()) {
                            Map<String, Object> userRatings = animeDoc.contains("userRatings") ?
                                    (Map<String, Object>) animeDoc.get("userRatings") : new HashMap<>();
                            double ratingSum = animeDoc.contains("ratingSum") ? animeDoc.getDouble("ratingSum") : 0;
                            long ratingCount = animeDoc.contains("ratingCount") ? animeDoc.getLong("ratingCount") : 0;

                            // Remover el rating del usuario
                            if (userRatings.containsKey(uid)) {
                                userRatings.remove(uid);
                                ratingSum -= oldRating;
                                ratingCount -= 1;

                                Map<String, Object> updates = new HashMap<>();
                                updates.put("userRatings", userRatings);
                                updates.put("ratingSum", Math.max(0, ratingSum));
                                updates.put("ratingCount", Math.max(0, ratingCount));
                                updates.put("averageRating", ratingCount > 0 ? ratingSum / ratingCount : 0);

                                transaction.set(animeRef, updates, SetOptions.merge());
                            }
                        }
                        return null;
                    });
                }

                // Eliminar la review si existe
                db.collection("reviews")
                        .whereEqualTo("userId", uid)
                        .whereEqualTo("animeId", anime.mal_id)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                doc.getReference().delete();
                            }
                        });

                // Actualizar la cache local
                cache.remove(anime.mal_id);

                return deleteTask;
            } else {
                // Si no existe el documento, simplemente retornar una tarea completada
                return com.google.android.gms.tasks.Tasks.forResult(null);
            }
        });
    }


}

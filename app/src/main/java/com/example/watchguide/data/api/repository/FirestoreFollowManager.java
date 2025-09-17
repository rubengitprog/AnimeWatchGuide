package com.example.watchguide.data.api.repository;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreFollowManager {

    private final String currentUid;

    public FirestoreFollowManager(String currentUid) {
        this.currentUid = currentUid;
    }

    // Seguir a un usuario
    public Task<Void> follow(String targetUid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("followedAt", System.currentTimeMillis());

        Task<Void> t1 = db.collection("users")
                .document(uid)
                .collection("following")
                .document(targetUid)
                .set(data);

        Task<Void> t2 = db.collection("users")
                .document(targetUid)
                .collection("followers")
                .document(uid)
                .set(data);

        return Tasks.whenAll(t1, t2);
    }


    // Comprobar si seguimos a alguien
    public Task<Boolean> isFollowing(String otherUid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        return db.collection("users")
                .document(currentUid)
                .collection("following")
                .document(otherUid)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        return task.getResult().exists();
                    }
                    return false;
                });
    }

    // Obtener lista de UIDs que sigo
    public void listenFollowing(FollowingListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(currentUid)
                .collection("following")
                .get()
                .addOnSuccessListener(snap -> {
                    List<String> followingUids = new ArrayList<>();
                    for (var doc : snap.getDocuments()) {
                        followingUids.add(doc.getId());
                    }
                    listener.onResult(followingUids);
                });
    }

    public interface FollowingListener {
        void onResult(List<String> followingUids);
    }
}

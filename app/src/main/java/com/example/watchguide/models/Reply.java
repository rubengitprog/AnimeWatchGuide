package com.example.watchguide.models;

import java.util.HashMap;
import java.util.Map;

public class Reply {
    public String replyId;
    public String reviewId; // ID de la reseña a la que responde
    public String userId;
    public String username; // Nombre del usuario (temporal, se obtiene de Firestore)
    public String replyText;
    public long timestamp;
    public Map<String, Boolean> likes; // Map de userId -> true (like) o false (dislike)
    public int likeCount;
    public int dislikeCount;

    public Reply() {
        // Constructor vacío requerido por Firestore
        this.likes = new HashMap<>();
        this.likeCount = 0;
        this.dislikeCount = 0;
    }

    public Reply(String replyId, String reviewId, String userId, String replyText, long timestamp) {
        this.replyId = replyId;
        this.reviewId = reviewId;
        this.userId = userId;
        this.replyText = replyText;
        this.timestamp = timestamp;
        this.likes = new HashMap<>();
        this.likeCount = 0;
        this.dislikeCount = 0;
    }

    public void addLike(String userId) {
        if (likes.containsKey(userId) && likes.get(userId)) {
            // Si ya tiene like, lo quita
            likes.remove(userId);
            likeCount--;
        } else {
            // Si tiene dislike, lo quita y pone like
            if (likes.containsKey(userId) && !likes.get(userId)) {
                dislikeCount--;
            }
            likes.put(userId, true);
            likeCount++;
        }
    }

    public void addDislike(String userId) {
        if (likes.containsKey(userId) && !likes.get(userId)) {
            // Si ya tiene dislike, lo quita
            likes.remove(userId);
            dislikeCount--;
        } else {
            // Si tiene like, lo quita y pone dislike
            if (likes.containsKey(userId) && likes.get(userId)) {
                likeCount--;
            }
            likes.put(userId, false);
            dislikeCount++;
        }
    }

    public Boolean getUserVote(String userId) {
        return likes.get(userId); // null si no ha votado, true si like, false si dislike
    }
}

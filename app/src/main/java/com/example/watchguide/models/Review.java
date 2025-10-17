package com.example.watchguide.models;

public class Review {
    public String reviewId;
    public String userId;
    public int animeId;
    public String animeTitle;
    public String animeImageUrl;
    public float rating;
    public String reviewText;
    public long timestamp;
    public String status; // "approved", "pending", "deleted"
    public String username; // Nombre del usuario (temporal, se obtiene de Firestore)

    public Review() {
        // Constructor vacío requerido por Firestore
    }

    public Review(String reviewId, String userId, int animeId, String animeTitle, String animeImageUrl,
                  float rating, String reviewText, long timestamp, String status) {
        this.reviewId = reviewId;
        this.userId = userId;
        this.animeId = animeId;
        this.animeTitle = animeTitle;
        this.animeImageUrl = animeImageUrl;
        this.rating = rating;
        this.reviewText = reviewText;
        this.timestamp = timestamp;
        this.status = status;
    }
}

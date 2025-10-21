package com.example.watchguide.models;

public class ActivityItem {
    public String userId;
    public String username;
    public String userPhotoUrl;
    public Integer animeId;
    public String animeTitle;
    public String animeImageUrl;
    public String type; // "favorite", "rating", "watched"
    public float value;
    public long timestamp;

    public ActivityItem() {
    } // Firestore necesita constructor vacío
}
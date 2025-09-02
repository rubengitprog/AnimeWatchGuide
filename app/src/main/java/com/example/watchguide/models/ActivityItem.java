package com.example.watchguide.models;

public class ActivityItem {
    public String userId;
    public String userName;
    public int animeId;
    public String animeTitle;
    public String type; // "favorite", "rating", "watched"
    public int value;
    public long timestamp;

    public ActivityItem() {
    } // Firestore necesita constructor vacío
}


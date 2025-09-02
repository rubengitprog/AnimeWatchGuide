package com.example.watchguide;

public class LibraryEntry {
    public String title;
    public String image_url;
    public boolean favorite;
    public int rating;
    public boolean watched;
    public long updatedAt;

    public LibraryEntry() {
    } // Constructor vacío para Firestore
}
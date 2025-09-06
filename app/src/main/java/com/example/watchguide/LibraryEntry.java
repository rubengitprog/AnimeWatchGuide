package com.example.watchguide;

public class LibraryEntry {
    public String title;
    public String image_url;
    public boolean favorite;
    public float rating;
    public boolean watched;
    public long updatedAt;

    public LibraryEntry() {
    } // Constructor vacío para Firestore
}
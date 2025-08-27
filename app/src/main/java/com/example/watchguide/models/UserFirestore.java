package com.example.watchguide.models;

public class UserFirestore {
    public String username;
    public String email;
    public String photoURL;
    public UserFirestore() {} // Firestore necesita constructor vacío

    public UserFirestore(String username, String email,String photoURL) {
        this.username = username;
        this.email = email;
        this.photoURL = photoURL;
    }
}

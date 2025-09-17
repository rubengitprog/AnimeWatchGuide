package com.example.watchguide.models;

public class FollowingItem {
    public String username;
    public String photoURL;
    public String uid; // el UID del usuario seguido

    public FollowingItem() {
    }

    public FollowingItem(String uid, String username, String photoURL) {
        this.uid = uid;
        this.username = username;
        this.photoURL = this.photoURL;
    }

}

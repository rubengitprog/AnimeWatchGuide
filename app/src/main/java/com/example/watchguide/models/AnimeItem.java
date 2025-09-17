package com.example.watchguide.models;

public class AnimeItem {
    public String imageUrl;
    public String title;

    public AnimeItem(String imageUrl, String title) {
        this.imageUrl = imageUrl;
        this.title = title;
    }


    public String getTitle() {
        return title;
    }

}


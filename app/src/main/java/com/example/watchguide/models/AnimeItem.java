package com.example.watchguide.models;

public class AnimeItem {
    public String imageUrl;
    public String title;
    public int animeId;

    public AnimeItem(String imageUrl, String title) {
        this.imageUrl = imageUrl;
        this.title = title;
        this.animeId = -1;
    }

    public AnimeItem(String imageUrl, String title, int animeId) {
        this.imageUrl = imageUrl;
        this.title = title;
        this.animeId = animeId;
    }

    public String getTitle() {
        return title;
    }

    public int getAnimeId() {
        return animeId;
    }

}


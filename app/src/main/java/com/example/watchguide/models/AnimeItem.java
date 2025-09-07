package com.example.watchguide.models;

public class AnimeItem {
    public String imageUrl;
    public String title;

    public AnimeItem(String imageUrl, String title) {
        this.imageUrl = imageUrl;
        this.title = title;
    }

    public String getCrunchyrollUrl() {
        // Reemplazar espacios por '+' para la búsqueda en Crunchyroll
        String query = title.trim().replace(" ", "+");
        return "https://www.crunchyroll.com/es-es/search?q=" + query;
    }
    public String getTitle(){
        return title;
    }

}


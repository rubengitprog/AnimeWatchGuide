package com.example.watchguide.models;

public class Anime {
    public int mal_id;
    public String title;
    public String synopsis;
    public Images images;

    // Campos locales (no vienen de la API)
    public transient float rating = 0f;        // de 0 a 5 estrellas
    public transient boolean seen = false; // si el usuario lo marcó como visto

    public static class Images {
        public JPG jpg;

        public Images() {
            this.jpg = new JPG();
        }

        public static class JPG {
            public String image_url;

            public JPG() {
                this.image_url = "https://via.placeholder.com/150"; // URL de placeholder
            }
        }
    }


}
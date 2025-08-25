package com.example.watchguide.models;

public class Anime {
    public int mal_id;
    public String title;
    public String synopsis;
    public Images images;

    // Campos locales (no vienen de la API)
    public transient int rating = 0;        // de 0 a 5 estrellas
    public transient boolean visto = false; // si el usuario lo marcó como visto
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

package com.example.watchguide.models;

import java.util.List;

public class Anime {
    public int mal_id;
    public String title;
    public String synopsis;
    public Images images;

    // Información adicional de la API
    public int episodes;
    public String type; // TV, Movie, OVA, ONA, Special
    public String status; // Finished Airing, Currently Airing, Not yet aired
    public Aired aired; // Fecha de emisión (objeto)
    public Double score; // Score de MAL (0.0 - 10.0)
    public int rank; // Ranking en MAL
    public int year; // Año de emisión
    public String season; // winter, spring, summer, fall
    public List<Genre> genres; // Lista de géneros
    public List<Studio> studios; // Lista de estudios
    public String source; // Manga, Light Novel, Original, etc.
    public String duration; // Duración por episodio
    public String rating; // PG-13, R-17+, G, etc.

    // Campos locales (no vienen de la API)
    public transient float rating_user = 0f;        // de 0.0 a 10 estrellas
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

    public static class Genre {
        public int mal_id;
        public String name;
    }

    public static class Studio {
        public int mal_id;
        public String name;
    }

    public static class Aired {
        public String from;
        public String to;
        public Prop prop;

        public static class Prop {
            public DateInfo from;
            public DateInfo to;

            public static class DateInfo {
                public int day;
                public int month;
                public int year;
            }
        }
    }
}
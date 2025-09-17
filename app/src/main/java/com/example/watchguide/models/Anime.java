package com.example.watchguide.models;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Anime {
    public int mal_id;
    public String title;
    public String synopsis;
    public Images images;
    public Aired aired;

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

    public class Aired {
        public String from; // "2025-09-17" por ejemplo
        public String to;

        public Date getFromDate() {
            if (from == null || from.isEmpty()) return null;
            try {
                // Solo año-mes-día
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                return sdf.parse(from);
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
        }
    }


}
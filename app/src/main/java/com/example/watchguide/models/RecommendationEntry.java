package com.example.watchguide.models;

public class RecommendationEntry {
    public AnimeBasicInfo entry;
    public int votes;

    public static class AnimeBasicInfo {
        public int mal_id;
        public String title;
        public Images images;
    }

    public static class Images {
        public ImageSet jpg;
    }

    public static class ImageSet {
        public String image_url;
        public String small_image_url;
        public String large_image_url;
    }
}

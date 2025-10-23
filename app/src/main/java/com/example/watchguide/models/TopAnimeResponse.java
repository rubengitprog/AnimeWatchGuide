package com.example.watchguide.models;

import java.util.List;

public class TopAnimeResponse {
    public List<Anime> data;
    public Pagination pagination;

    public static class Pagination {
        public int last_visible_page;
        public boolean has_next_page;
        public int current_page;
    }
}

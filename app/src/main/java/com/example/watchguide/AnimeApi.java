package com.example.watchguide;

import com.example.watchguide.models.AnimeResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AnimeApi {
    @GET("anime")
    Call<AnimeResponse> searchAnime(@Query("q") String query);
}
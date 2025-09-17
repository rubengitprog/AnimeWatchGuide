package com.example.watchguide.data.api;

import com.example.watchguide.models.AnimeResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AnimeApi {
    @GET("anime")
    Call<AnimeResponse> searchAnime(@Query("q") String query);

    @GET("anime")
    Call<AnimeResponse> getAnimeByGenre(@Query("genres") int genreId);

    @GET("anime")
    Call<AnimeResponse> searchAnimeWithGenre(@Query("q") String query, @Query("genres") int genreId);


}
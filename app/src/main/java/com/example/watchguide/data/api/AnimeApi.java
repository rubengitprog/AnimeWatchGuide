package com.example.watchguide.data.api;

import com.example.watchguide.models.AnimeDetailResponse;
import com.example.watchguide.models.AnimeResponse;
import com.example.watchguide.models.RecommendationsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AnimeApi {
    @GET("anime")
    Call<AnimeResponse> searchAnime(@Query("q") String query);

    @GET("anime")
    Call<AnimeResponse> getAnimeByGenre(@Query("genres") int genreId);

    @GET("anime")
    Call<AnimeResponse> searchAnimeWithGenre(@Query("q") String query, @Query("genres") int genreId);

    @GET("anime/{id}")
    Call<AnimeDetailResponse> getAnimeById(@Path("id") int animeId);

    @GET("anime/{id}/recommendations")
    Call<RecommendationsResponse> getAnimeRecommendations(@Path("id") int animeId);

}
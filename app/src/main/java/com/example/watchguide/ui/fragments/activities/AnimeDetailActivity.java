package com.example.watchguide.ui.fragments.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.watchguide.R;
import com.example.watchguide.data.api.AnimeApi;
import com.example.watchguide.models.Anime;
import com.example.watchguide.models.AnimeDetailResponse;
import com.example.watchguide.models.RecommendationEntry;
import com.example.watchguide.models.RecommendationsResponse;
import com.example.watchguide.models.Review;

import com.example.watchguide.ui.fragments.adapters.RecommendationAdapter;
import com.example.watchguide.ui.fragments.adapters.ReviewAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AnimeDetailActivity extends AppCompatActivity {

    private ImageView animeImageDetail;
    private TextView animeTitleDetail, animeSynopsisDetail, animeAverageRatingDetail, textNoReviews;
    private TextView animeTypeDetail, animeEpisodesDetail, animeStatusDetail;
    private TextView animeScoreMALDetail, animeRankDetail, animeGenresDetail;
    private TextView animeYearSeasonDetail, animeSourceDetail, animeStudioDetail;
    private TextView animeDurationDetail, animeRatingDetail;
    private RecyclerView recyclerViewReviews;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList = new ArrayList<>();

    private TextView textRecommendationsTitle;
    private RecyclerView recyclerViewRecommendations;
    private RecommendationAdapter recommendationAdapter;
    private List<RecommendationEntry> recommendationList = new ArrayList<>();

    private int animeId;
    private String animeTitle;
    private String animeSynopsis;
    private String animeImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anime_detail);

        // Obtener datos del intent
        animeId = getIntent().getIntExtra("animeId", -1);
        animeTitle = getIntent().getStringExtra("animeTitle");
        animeSynopsis = getIntent().getStringExtra("animeSynopsis");
        animeImageUrl = getIntent().getStringExtra("animeImageUrl");

        if (animeId == -1) {
            finish();
            return;
        }

        // Inicializar vistas
        animeImageDetail = findViewById(R.id.animeImageDetail);
        animeTitleDetail = findViewById(R.id.animeTitleDetail);
        animeSynopsisDetail = findViewById(R.id.animeSynopsisDetail);
        animeAverageRatingDetail = findViewById(R.id.animeAverageRatingDetail);
        recyclerViewReviews = findViewById(R.id.recyclerViewReviews);
        textNoReviews = findViewById(R.id.textNoReviews);

        // Nuevas vistas
        animeTypeDetail = findViewById(R.id.animeTypeDetail);
        animeEpisodesDetail = findViewById(R.id.animeEpisodesDetail);
        animeStatusDetail = findViewById(R.id.animeStatusDetail);
        animeScoreMALDetail = findViewById(R.id.animeScoreMALDetail);
        animeRankDetail = findViewById(R.id.animeRankDetail);
        animeGenresDetail = findViewById(R.id.animeGenresDetail);
        animeYearSeasonDetail = findViewById(R.id.animeYearSeasonDetail);
        animeSourceDetail = findViewById(R.id.animeSourceDetail);
        animeStudioDetail = findViewById(R.id.animeStudioDetail);
        animeDurationDetail = findViewById(R.id.animeDurationDetail);
        animeRatingDetail = findViewById(R.id.animeRatingDetail);

        // Configurar datos
        animeTitleDetail.setText(animeTitle);

        // Si falta información (como la sinopsis), cargar desde la API
        if (animeSynopsis == null || animeSynopsis.isEmpty()) {
            loadAnimeFromApi();
        } else {
            animeSynopsisDetail.setText(animeSynopsis);
        }

        if (animeImageUrl != null && !animeImageUrl.isEmpty()) {
            Glide.with(this).load(animeImageUrl).into(animeImageDetail);
        }

        // Poblar información adicional
        String type = getIntent().getStringExtra("animeType");
        if (type != null && !type.isEmpty()) {
            animeTypeDetail.setText(type);
            animeTypeDetail.setVisibility(View.VISIBLE);
        } else {
            animeTypeDetail.setVisibility(View.GONE);
        }

        int episodes = getIntent().getIntExtra("animeEpisodes", 0);
        if (episodes > 0) {
            animeEpisodesDetail.setText(episodes + " episodes");
            animeEpisodesDetail.setVisibility(View.VISIBLE);
        } else {
            animeEpisodesDetail.setText("? episodes");
            animeEpisodesDetail.setVisibility(View.VISIBLE);
        }

        String status = getIntent().getStringExtra("animeStatus");
        if (status != null && !status.isEmpty()) {
            animeStatusDetail.setText(status);
            animeStatusDetail.setVisibility(View.VISIBLE);
        } else {
            animeStatusDetail.setVisibility(View.GONE);
        }

        double score = getIntent().getDoubleExtra("animeScore", 0.0);
        if (score > 0) {
            animeScoreMALDetail.setText("⭐ MAL Score: " + String.format("%.1f", score));
            animeScoreMALDetail.setVisibility(View.VISIBLE);
        } else {
            animeScoreMALDetail.setVisibility(View.GONE);
        }

        int rank = getIntent().getIntExtra("animeRank", 0);
        if (rank > 0) {
            animeRankDetail.setText("🏆 Rank #" + rank);
            animeRankDetail.setVisibility(View.VISIBLE);
        } else {
            animeRankDetail.setVisibility(View.GONE);
        }

        String genres = getIntent().getStringExtra("animeGenres");
        if (genres != null && !genres.isEmpty()) {
            animeGenresDetail.setText(genres);
            animeGenresDetail.setVisibility(View.VISIBLE);
        } else {
            animeGenresDetail.setVisibility(View.GONE);
        }

        int year = getIntent().getIntExtra("animeYear", 0);
        String season = getIntent().getStringExtra("animeSeason");
        if (season != null && !season.isEmpty() && year > 0) {
            String seasonCapitalized = season.substring(0, 1).toUpperCase() + season.substring(1);
            animeYearSeasonDetail.setText(seasonCapitalized + " " + year);
            animeYearSeasonDetail.setVisibility(View.VISIBLE);
        } else if (year > 0) {
            animeYearSeasonDetail.setText(String.valueOf(year));
            animeYearSeasonDetail.setVisibility(View.VISIBLE);
        } else {
            animeYearSeasonDetail.setVisibility(View.GONE);
        }

        String source = getIntent().getStringExtra("animeSource");
        if (source != null && !source.isEmpty()) {
            animeSourceDetail.setText("Source: " + source);
            animeSourceDetail.setVisibility(View.VISIBLE);
        } else {
            animeSourceDetail.setVisibility(View.GONE);
        }

        String studios = getIntent().getStringExtra("animeStudios");
        if (studios != null && !studios.isEmpty()) {
            animeStudioDetail.setText("Studio: " + studios);
            animeStudioDetail.setVisibility(View.VISIBLE);
        } else {
            animeStudioDetail.setVisibility(View.GONE);
        }

        String duration = getIntent().getStringExtra("animeDuration");
        if (duration != null && !duration.isEmpty()) {
            animeDurationDetail.setText(duration);
            animeDurationDetail.setVisibility(View.VISIBLE);
        } else {
            animeDurationDetail.setVisibility(View.GONE);
        }

        String rating = getIntent().getStringExtra("animeRating");
        if (rating != null && !rating.isEmpty()) {
            animeRatingDetail.setText("Rating: " + rating);
            animeRatingDetail.setVisibility(View.VISIBLE);
        } else {
            animeRatingDetail.setVisibility(View.GONE);
        }

        // Configurar RecyclerView de reviews
        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
        reviewAdapter = new ReviewAdapter(reviewList, this, false); // No mostrar info del anime
        recyclerViewReviews.setAdapter(reviewAdapter);

        // Configurar RecyclerView de recomendaciones (horizontal)
        textRecommendationsTitle = findViewById(R.id.textRecommendationsTitle);
        recyclerViewRecommendations = findViewById(R.id.recyclerViewRecommendations);
        recyclerViewRecommendations.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recommendationAdapter = new RecommendationAdapter(recommendationList, this);
        recyclerViewRecommendations.setAdapter(recommendationAdapter);

        // Cargar datos
        loadAverageRating();
        loadReviews();
        loadRecommendations();
    }

    private void loadAverageRating() {
        FirebaseFirestore.getInstance()
                .collection("anime")
                .document(String.valueOf(animeId))
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains("averageRating")) {
                        double avg = doc.getDouble("averageRating");
                        int total = doc.getLong("ratingCount").intValue();
                        animeAverageRatingDetail.setText(
                                getString(R.string.average_rating, avg, total)
                        );
                    } else {
                        animeAverageRatingDetail.setText(getString(R.string.no_ratings_yet));
                    }
                })
                .addOnFailureListener(e -> {
                    animeAverageRatingDetail.setText(getString(R.string.average_rating_dash));
                });
    }

    private void loadReviews() {
        FirebaseFirestore.getInstance()
                .collection("reviews")
                .whereEqualTo("animeId", animeId)
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    reviewList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Review review = doc.toObject(Review.class);
                        if (review != null) {
                            review.reviewId = doc.getId();
                            reviewList.add(review);
                        }
                    }

                    // Ordenar manualmente por timestamp (más reciente primero)
                    reviewList.sort((r1, r2) -> Long.compare(r2.timestamp, r1.timestamp));

                    if (reviewList.isEmpty()) {
                        textNoReviews.setVisibility(View.VISIBLE);
                        recyclerViewReviews.setVisibility(View.GONE);
                    } else {
                        textNoReviews.setVisibility(View.GONE);
                        recyclerViewReviews.setVisibility(View.VISIBLE);
                    }

                    reviewAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    textNoReviews.setVisibility(View.VISIBLE);
                    textNoReviews.setText(getString(R.string.error_loading_reviews, e.getMessage()));
                    android.util.Log.e("AnimeDetail", "Error cargando reviews", e);
                });
    }

    private void loadAnimeFromApi() {
        // Configurar Retrofit
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.jikan.moe/v4/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        AnimeApi api = retrofit.create(AnimeApi.class);

        // Hacer la llamada a la API
        api.getAnimeById(animeId).enqueue(new Callback<AnimeDetailResponse>() {
            @Override
            public void onResponse(Call<AnimeDetailResponse> call, Response<AnimeDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    Anime anime = response.body().data;

                    // Actualizar la UI con los datos completos
                    runOnUiThread(() -> {
                        // Sinopsis
                        if (anime.synopsis != null && !anime.synopsis.isEmpty()) {
                            animeSynopsisDetail.setText(anime.synopsis);
                        } else {
                            animeSynopsisDetail.setText("No synopsis available");
                        }

                        // Imagen (si no la teníamos)
                        if (animeImageUrl == null || animeImageUrl.isEmpty()) {
                            String imageUrl = (anime.images != null && anime.images.jpg != null)
                                ? anime.images.jpg.image_url : null;
                            if (imageUrl != null) {
                                Glide.with(AnimeDetailActivity.this).load(imageUrl).into(animeImageDetail);
                            }
                        }

                        // Type
                        if (anime.type != null && !anime.type.isEmpty()) {
                            animeTypeDetail.setText(anime.type);
                            animeTypeDetail.setVisibility(View.VISIBLE);
                        }

                        // Episodes
                        if (anime.episodes > 0) {
                            animeEpisodesDetail.setText(anime.episodes + " episodes");
                            animeEpisodesDetail.setVisibility(View.VISIBLE);
                        }

                        // Status
                        if (anime.status != null && !anime.status.isEmpty()) {
                            animeStatusDetail.setText(anime.status);
                            animeStatusDetail.setVisibility(View.VISIBLE);
                        }

                        // MAL Score
                        if (anime.score > 0) {
                            animeScoreMALDetail.setText("⭐ MAL Score: " + String.format("%.1f", anime.score));
                            animeScoreMALDetail.setVisibility(View.VISIBLE);
                        }

                        // Rank
                        if (anime.rank > 0) {
                            animeRankDetail.setText("🏆 Rank #" + anime.rank);
                            animeRankDetail.setVisibility(View.VISIBLE);
                        }

                        // Géneros
                        if (anime.genres != null && !anime.genres.isEmpty()) {
                            StringBuilder genresText = new StringBuilder();
                            for (int i = 0; i < anime.genres.size(); i++) {
                                genresText.append(anime.genres.get(i).name);
                                if (i < anime.genres.size() - 1) {
                                    genresText.append(", ");
                                }
                            }
                            animeGenresDetail.setText(genresText.toString());
                            animeGenresDetail.setVisibility(View.VISIBLE);
                        }

                        // Year & Season
                        if (anime.year > 0) {
                            if (anime.season != null && !anime.season.isEmpty()) {
                                String seasonCapitalized = anime.season.substring(0, 1).toUpperCase() + anime.season.substring(1);
                                animeYearSeasonDetail.setText(seasonCapitalized + " " + anime.year);
                            } else {
                                animeYearSeasonDetail.setText(String.valueOf(anime.year));
                            }
                            animeYearSeasonDetail.setVisibility(View.VISIBLE);
                        }

                        // Source
                        if (anime.source != null && !anime.source.isEmpty()) {
                            animeSourceDetail.setText("Source: " + anime.source);
                            animeSourceDetail.setVisibility(View.VISIBLE);
                        }

                        // Studios
                        if (anime.studios != null && !anime.studios.isEmpty()) {
                            StringBuilder studiosText = new StringBuilder();
                            for (int i = 0; i < anime.studios.size(); i++) {
                                studiosText.append(anime.studios.get(i).name);
                                if (i < anime.studios.size() - 1) {
                                    studiosText.append(", ");
                                }
                            }
                            animeStudioDetail.setText("Studio: " + studiosText.toString());
                            animeStudioDetail.setVisibility(View.VISIBLE);
                        }

                        // Duration
                        if (anime.duration != null && !anime.duration.isEmpty()) {
                            animeDurationDetail.setText(anime.duration);
                            animeDurationDetail.setVisibility(View.VISIBLE);
                        }

                        // Rating
                        if (anime.rating != null && !anime.rating.isEmpty()) {
                            animeRatingDetail.setText("Rating: " + anime.rating);
                            animeRatingDetail.setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        animeSynopsisDetail.setText("Error loading anime details");
                        Toast.makeText(AnimeDetailActivity.this, "Error loading anime information", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(Call<AnimeDetailResponse> call, Throwable t) {
                Log.e("AnimeDetail", "Error loading anime from API", t);
                runOnUiThread(() -> {
                    animeSynopsisDetail.setText("No synopsis available");
                    Toast.makeText(AnimeDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadRecommendations() {
        // Configurar Retrofit
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.jikan.moe/v4/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        AnimeApi api = retrofit.create(AnimeApi.class);

        // Hacer la llamada a la API
        api.getAnimeRecommendations(animeId).enqueue(new Callback<RecommendationsResponse>() {
            @Override
            public void onResponse(Call<RecommendationsResponse> call, Response<RecommendationsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    recommendationList.clear();

                    // Limitar a 10 recomendaciones
                    int count = Math.min(response.body().data.size(), 10);
                    for (int i = 0; i < count; i++) {
                        recommendationList.add(response.body().data.get(i));
                    }

                    runOnUiThread(() -> {
                        if (!recommendationList.isEmpty()) {
                            textRecommendationsTitle.setVisibility(View.VISIBLE);
                            recyclerViewRecommendations.setVisibility(View.VISIBLE);
                            recommendationAdapter.notifyDataSetChanged();
                        }
                    });
                } else {
                    Log.d("AnimeDetail", "No recommendations available or API error");
                }
            }

            @Override
            public void onFailure(Call<RecommendationsResponse> call, Throwable t) {
                Log.e("AnimeDetail", "Error loading recommendations from API", t);
                // No mostrar error al usuario, las recomendaciones son opcionales
            }
        });
    }
}

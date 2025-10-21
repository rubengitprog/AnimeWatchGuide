package com.example.watchguide.ui.fragments.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.watchguide.R;
import com.example.watchguide.models.Review;

import com.example.watchguide.ui.fragments.adapters.ReviewAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

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
        animeSynopsisDetail.setText(animeSynopsis != null ? animeSynopsis : "No synopsis available");

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

        // Configurar RecyclerView
        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
        reviewAdapter = new ReviewAdapter(reviewList, this);
        recyclerViewReviews.setAdapter(reviewAdapter);

        // Cargar datos
        loadAverageRating();
        loadReviews();
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
}

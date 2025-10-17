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

        // Configurar datos
        animeTitleDetail.setText(animeTitle);
        animeSynopsisDetail.setText(animeSynopsis != null ? animeSynopsis : "No synopsis available");

        if (animeImageUrl != null && !animeImageUrl.isEmpty()) {
            Glide.with(this).load(animeImageUrl).into(animeImageDetail);
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

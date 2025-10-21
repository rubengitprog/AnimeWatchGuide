package com.example.watchguide.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.watchguide.R;
import com.example.watchguide.models.Review;
import com.example.watchguide.ui.fragments.adapters.ReviewAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ReviewsFragment extends Fragment {

    private String uid;
    private RecyclerView recyclerView;
    private ReviewAdapter adapter;
    private List<Review> reviewList = new ArrayList<>();
    private TextView emptyMessage;

    public static ReviewsFragment newInstance(String uid) {
        ReviewsFragment fragment = new ReviewsFragment();
        Bundle args = new Bundle();
        args.putString("uid", uid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reviews, container, false);

        if (getArguments() != null) {
            uid = getArguments().getString("uid");
        }

        recyclerView = view.findViewById(R.id.recyclerViewReviews);
        emptyMessage = view.findViewById(R.id.emptyMessageReviews);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ReviewAdapter(reviewList, getContext());
        recyclerView.setAdapter(adapter);

        // Inicialmente mostrar el RecyclerView (vacío) en lugar del mensaje
        recyclerView.setVisibility(View.VISIBLE);
        emptyMessage.setVisibility(View.GONE);

        loadReviews();

        return view;
    }

    private void loadReviews() {
        FirebaseFirestore.getInstance()
                .collection("reviews")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        showEmptyMessage();
                        return;
                    }

                    reviewList.clear();
                    for (var doc : snapshots.getDocuments()) {
                        Review review = doc.toObject(Review.class);
                        if (review != null) {
                            review.reviewId = doc.getId();
                            reviewList.add(review);
                        }
                    }

                    if (reviewList.isEmpty()) {
                        showEmptyMessage();
                    } else {
                        hideEmptyMessage();
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    private void showEmptyMessage() {
        if (emptyMessage != null) {
            emptyMessage.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void hideEmptyMessage() {
        if (emptyMessage != null) {
            emptyMessage.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}

package com.example.watchguide.ui.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.watchguide.ui.fragments.adapters.AnimeImageAdapter;
import com.example.watchguide.ui.fragments.activities.AnimeDetailActivity;
import com.example.watchguide.R;
import com.example.watchguide.models.AnimeItem;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {

    private String uid;
    private boolean deleteEnabled;
    private RecyclerView recyclerView;
    private AnimeImageAdapter adapter;
    private List<AnimeItem> animeItems = new ArrayList<>();

    public static FavoritesFragment newInstance(String uid, boolean deleteEnabled) {
        FavoritesFragment fragment = new FavoritesFragment();
        Bundle args = new Bundle();
        args.putString("uid", uid);
        args.putBoolean("deleteEnabled", deleteEnabled);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recycler, container, false);

        if (getArguments() != null) {
            uid = getArguments().getString("uid");
            deleteEnabled = getArguments().getBoolean("deleteEnabled", false);
        }

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        adapter = new AnimeImageAdapter(animeItems, item -> {
            if (item != null) {
                if (deleteEnabled) {
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle(item.getTitle())
                            .setMessage("Do you want to remove this anime from your favorites?")
                            .setPositiveButton("Remove", (dialog, which) -> {
                                FirebaseFirestore.getInstance()
                                        .collection("users").document(uid)
                                        .collection("library")
                                        .whereEqualTo("title", item.getTitle())
                                        .get()
                                        .addOnSuccessListener(snap -> {
                                            for (DocumentSnapshot doc : snap.getDocuments()) {
                                                doc.getReference().update("favorite", false);
                                            }
                                            animeItems.remove(item);
                                            adapter.notifyDataSetChanged();
                                        });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    // Abrir AnimeDetailActivity con información del anime
                    Intent intent = new Intent(requireContext(), AnimeDetailActivity.class);
                    intent.putExtra("animeId", item.getAnimeId());
                    intent.putExtra("animeTitle", item.getTitle());
                    intent.putExtra("animeImageUrl", item.imageUrl);
                    startActivity(intent);
                }
            }
        });

        recyclerView.setAdapter(adapter);

        loadFavorites();

        return view;
    }

    private void loadFavorites() {
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("library")
                .whereEqualTo("favorite", true)
                .get()
                .addOnSuccessListener(snap -> {
                    animeItems.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String imageUrl = doc.getString("image_url");
                        String title = doc.getString("title");
                        // El ID del documento es el animeId (mal_id)
                        int animeId = -1;
                        try {
                            animeId = Integer.parseInt(doc.getId());
                        } catch (NumberFormatException e) {
                            // Si falla, intentar ignorar
                        }
                        if (imageUrl != null && title != null) {
                            animeItems.add(new AnimeItem(imageUrl, title, animeId));
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}

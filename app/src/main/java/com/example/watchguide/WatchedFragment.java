package com.example.watchguide;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.watchguide.models.AnimeItem;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class WatchedFragment extends Fragment {

    private final String uid;
    private RecyclerView recyclerView;
    private AnimeImageAdapter adapter;
    private List<AnimeItem> animeItems = new ArrayList<>();

    public WatchedFragment(String uid) {
        this.uid = uid;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recycler, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        adapter = new AnimeImageAdapter(animeItems, item -> {
            if (item != null) {
                // Crear diálogo para confirmar eliminación
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle(item.getTitle())
                        .setMessage("Do you want to remove this anime from watched?")
                        .setPositiveButton("Remove", (dialog, which) -> {
                            // Eliminar de Firestore
                            FirebaseFirestore.getInstance()
                                    .collection("users").document(uid)
                                    .collection("library")
                                    .whereEqualTo("title", item.getTitle())
                                    .get()
                                    .addOnSuccessListener(snap -> {
                                        for (DocumentSnapshot doc : snap.getDocuments()) {
                                            doc.getReference().update("watched", false);
                                        }
                                        // Quitar de la lista local y actualizar RecyclerView
                                        animeItems.remove(item);
                                        adapter.notifyDataSetChanged();
                                    });
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        });

        recyclerView.setAdapter(adapter);

        loadWatched(); // Cargar datos de Firestore

        return view;
    }

    private void loadWatched() {
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("library")
                .whereEqualTo("watched", true)
                .get()
                .addOnSuccessListener(snap -> {  // snap = QuerySnapshot
                    animeItems.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String imageUrl = doc.getString("image_url");
                        String title = doc.getString("title");
                        if (imageUrl != null && title != null) {
                            animeItems.add(new AnimeItem(imageUrl, title));
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}

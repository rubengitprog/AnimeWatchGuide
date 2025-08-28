package com.example.watchguide;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.watchguide.adapters.AnimeImageAdapter;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FavoritesFragment extends Fragment {

    private final String uid;
    private RecyclerView recyclerView;
    private AnimeImageAdapter adapter;
    private List<String> imageUrls = new ArrayList<>();

    public FavoritesFragment(String uid) {
        this.uid = uid;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recycler, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        adapter = new AnimeImageAdapter(imageUrls);
        recyclerView.setAdapter(adapter);

        loadFavorites();
        return view;
    }

    private void loadFavorites() {
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("library")
                .whereEqualTo("favorite", true)
                .get().addOnSuccessListener(snap -> {
                    imageUrls.clear();
                    for (var doc : snap) {
                        String url = (String) doc.get("image_url");
                        if (url != null) imageUrls.add(url);
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}

package com.example.watchguide.ui.fragments.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.watchguide.R;
import com.example.watchguide.models.AnimeItem;

import java.util.List;

public class AnimeImageAdapter extends RecyclerView.Adapter<AnimeImageAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(AnimeItem item);
    }

    private final List<AnimeItem> animeItems;
    private final OnItemClickListener listener;

    public AnimeImageAdapter(List<AnimeItem> animeItems, OnItemClickListener listener) {
        this.animeItems = animeItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_anime_image, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AnimeItem item = animeItems.get(position);
        Glide.with(holder.itemView.getContext()).load(item.imageUrl).into(holder.image);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return animeItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        ViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.animeImage);
        }
    }
}

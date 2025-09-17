package com.example.watchguide.ui.fragments.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.watchguide.R;
import com.example.watchguide.models.Anime;

import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {

    private List<Anime> favList;
    private Context context;

    public FavoritesAdapter(List<Anime> favList, Context context) {
        this.favList = favList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_fav, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Anime anime = favList.get(position);
        SharedPreferences prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE);

        // Cargar título e imagen
        holder.title.setText(anime.title);
        if (anime.images != null && anime.images.jpg != null)
            Glide.with(context).load(anime.images.jpg.image_url).into(holder.image);

        // Cargar rating del SharedPreferences
        int rating = prefs.getInt("rating_" + anime.mal_id, 0);
        holder.textRating.setText("Rating: " + rating);
    }

    @Override
    public int getItemCount() {
        return favList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;
        TextView textRating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.animeImage);
            title = itemView.findViewById(R.id.animeTitle);
            textRating = itemView.findViewById(R.id.textRating);
        }
    }
}
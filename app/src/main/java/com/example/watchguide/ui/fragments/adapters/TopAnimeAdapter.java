package com.example.watchguide.ui.fragments.adapters;

import android.content.Context;
import android.content.Intent;
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
import com.example.watchguide.ui.fragments.activities.AnimeDetailActivity;

import java.util.List;

public class TopAnimeAdapter extends RecyclerView.Adapter<TopAnimeAdapter.ViewHolder> {

    private final List<Anime> animeList;
    private final Context context;
    private final int highlightAnimeId; // ID del anime a resaltar

    public TopAnimeAdapter(List<Anime> animeList, Context context, int highlightAnimeId) {
        this.animeList = animeList;
        this.context = context;
        this.highlightAnimeId = highlightAnimeId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_top_anime, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Anime anime = animeList.get(position);

        // Posición en el ranking
        holder.rankPosition.setText("#" + anime.rank);

        // Título
        holder.title.setText(anime.title);

        // Score
        if (anime.score > 0) {
            holder.score.setText("⭐ " + String.format("%.1f", anime.score));
        } else {
            holder.score.setText("⭐ N/A");
        }

        // Info (Type y episodios o ratings)
        StringBuilder info = new StringBuilder();
        boolean isLocalRanking = "WatchGuide".equals(anime.type);

        if (anime.type != null && !anime.type.isEmpty() && !isLocalRanking) {
            info.append(anime.type);
        }

        if (anime.episodes > 0) {
            if (info.length() > 0) info.append(" • ");
            if (isLocalRanking) {
                // Para ranking local, mostrar número de ratings
                info.append(anime.episodes).append(anime.episodes == 1 ? " rating" : " ratings");
            } else {
                // Para ranking MAL, mostrar episodios
                info.append(anime.episodes).append(" episodes");
            }
        }
        holder.info.setText(info.length() > 0 ? info.toString() : "");

        // Imagen
        String imageUrl = anime.images != null && anime.images.jpg != null
                ? anime.images.jpg.image_url
                : null;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.image);
        }

        // Resaltar el anime actual si coincide con highlightAnimeId
        if (anime.mal_id == highlightAnimeId) {
            holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_light, null));
            holder.itemView.setAlpha(0.8f);
        } else {
            holder.itemView.setBackground(null);
            holder.itemView.setAlpha(1.0f);
        }

        // Click listener para abrir detalles
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AnimeDetailActivity.class);
            intent.putExtra("animeId", anime.mal_id);
            intent.putExtra("animeTitle", anime.title);
            intent.putExtra("animeImageUrl", imageUrl);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return animeList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView rankPosition, title, score, info;
        ImageView image;

        ViewHolder(View itemView) {
            super(itemView);
            rankPosition = itemView.findViewById(R.id.rankPosition);
            title = itemView.findViewById(R.id.topAnimeTitle);
            score = itemView.findViewById(R.id.topAnimeScore);
            info = itemView.findViewById(R.id.topAnimeInfo);
            image = itemView.findViewById(R.id.topAnimeImage);
        }
    }
}

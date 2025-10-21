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
import com.example.watchguide.models.RecommendationEntry;
import com.example.watchguide.ui.fragments.activities.AnimeDetailActivity;

import java.util.List;

public class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.ViewHolder> {

    private final List<RecommendationEntry> recommendations;
    private final Context context;

    public RecommendationAdapter(List<RecommendationEntry> recommendations, Context context) {
        this.recommendations = recommendations;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recommendation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecommendationEntry recommendation = recommendations.get(position);

        // Cargar imagen
        String imageUrl = recommendation.entry.images != null && recommendation.entry.images.jpg != null
                ? recommendation.entry.images.jpg.image_url
                : null;

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.animeImage);
        }

        // Mostrar título
        holder.animeTitle.setText(recommendation.entry.title);

        // Click listener para abrir detalles del anime
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AnimeDetailActivity.class);
            intent.putExtra("animeId", recommendation.entry.mal_id);
            intent.putExtra("animeTitle", recommendation.entry.title);
            intent.putExtra("animeImageUrl", imageUrl);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return recommendations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView animeImage;
        TextView animeTitle;

        ViewHolder(View itemView) {
            super(itemView);
            animeImage = itemView.findViewById(R.id.recommendationImage);
            animeTitle = itemView.findViewById(R.id.recommendationTitle);
        }
    }
}

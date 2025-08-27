package com.example.watchguide;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.watchguide.models.Anime;

import java.util.List;
import java.util.Map;

public class AnimeAdapter extends RecyclerView.Adapter<AnimeAdapter.ViewHolder> {

    private List<Anime> animeList;
    private Context context;
    private FirestoreUserLibrary userLibrary;

    public AnimeAdapter(List<Anime> animeList, Context context, FirestoreUserLibrary userLibrary) {
        this.animeList = animeList;
        this.context = context;
        this.userLibrary = userLibrary;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_anime, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Anime anime = animeList.get(position);
        holder.title.setText(anime.title);
        holder.synopsis.setText(anime.synopsis);

        if (anime.images != null && anime.images.jpg != null) {
            Glide.with(context).load(anime.images.jpg.image_url).into(holder.image);
        }

        // Estado actual desde cache
        Map<Integer, LibraryEntry> cache = userLibrary.getCache();
        LibraryEntry entry = cache.get(anime.mal_id);

        boolean isFav = entry != null && entry.favorite;
        boolean isWatched = entry != null && entry.watched;
        int rating = (entry != null) ? entry.rating : 0;

        anime.rating = rating;
        anime.visto = isWatched;

        holder.favButton.setImageResource(isFav ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        holder.itemView.setBackgroundColor(isWatched ? context.getResources().getColor(android.R.color.darker_gray)
                : context.getResources().getColor(android.R.color.white));

        // Toggle favorito
        holder.favButton.setOnClickListener(v -> {
            userLibrary.setFavorite(anime, !isFav)
                    .addOnSuccessListener(aVoid -> holder.favButton.setImageResource(!isFav ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off))
                    .addOnFailureListener(e -> Toast.makeText(context, "Error al cambiar favorito", Toast.LENGTH_SHORT).show());
        });

        // Click en item → RatingBar
        holder.itemView.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_anime, null);
            RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
            ratingBar.setRating(rating);

            ratingBar.setOnRatingBarChangeListener((rb, r, fromUser) -> {
                if (!fromUser) return;
                if (r == anime.rating) {
                    anime.rating = 0;
                    anime.visto = false;
                    rb.setRating(0);
                } else {
                    anime.rating = (int) r;
                    anime.visto = r > 0;
                }
            });

            new AlertDialog.Builder(context)
                    .setTitle(anime.title)
                    .setView(dialogView)
                    .setPositiveButton("Guardar", (dialog, which) -> {
                        userLibrary.setRating(anime, anime.rating)
                                .addOnSuccessListener(aVoid -> userLibrary.setWatched(anime, anime.visto)
                                        .addOnSuccessListener(aVoid1 -> holder.itemView.setBackgroundColor(anime.visto ?
                                                context.getResources().getColor(android.R.color.darker_gray) :
                                                context.getResources().getColor(android.R.color.white))))
                                .addOnFailureListener(e -> Toast.makeText(context, "Error al guardar rating", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() { return animeList.size(); }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, synopsis;
        ImageView image, favButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.animeTitle);
            synopsis = itemView.findViewById(R.id.animeSynopsis);
            image = itemView.findViewById(R.id.animeImage);
            favButton = itemView.findViewById(R.id.favButton);
        }
    }
}

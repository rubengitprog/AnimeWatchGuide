package com.example.watchguide;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.watchguide.models.Anime;

import java.util.List;

import android.content.SharedPreferences;
import android.widget.ImageButton;
import android.widget.Toast;

public class AnimeAdapter extends RecyclerView.Adapter<AnimeAdapter.ViewHolder> {

    private List<Anime> animeList;
    private Context context;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public AnimeAdapter(List<Anime> animeList, Context context) {
        this.animeList = animeList;
        this.context = context;

        prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE);
        editor = prefs.edit();
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

        Glide.with(context)
                .load(anime.images.jpg.image_url)
                .into(holder.image);

        // ⭐ Verificar si está en favoritos
        boolean isFav = prefs.contains(anime.mal_id + "_title");
        holder.favButton.setImageResource(
                isFav ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off
        );


        // 🟢 Cargar "visto" y "rating" de SharedPreferences
        boolean vistoGuardado = prefs.getBoolean("watched_" + anime.mal_id, false);
        int ratingGuardado = prefs.getInt("rating_" + anime.mal_id, 0);

        anime.visto = vistoGuardado;
        anime.rating = ratingGuardado;

        // aplicar fondo
        holder.itemView.setBackgroundColor(
                anime.visto ? context.getResources().getColor(android.R.color.darker_gray)
                        : context.getResources().getColor(android.R.color.white)
        );

        holder.favButton.setOnClickListener(v -> {
            String key = String.valueOf(anime.mal_id);
            if (prefs.contains(key)) {
                // Eliminar de favoritos
                editor.remove(key);
                editor.remove(key + "_title");
                editor.remove(key + "_image");
                editor.apply();
                holder.favButton.setImageResource(android.R.drawable.btn_star_big_off);
            } else {
                // Guardar en favoritos
                editor.putBoolean(key, true); // 🔹 Clave de referencia
                editor.putString(key + "_title", anime.title);
                editor.putString(key + "_image", anime.images.jpg.image_url);
                editor.apply();
                holder.favButton.setImageResource(android.R.drawable.btn_star_big_on);
            }
        });


        // 🟢 Click en el item → abrir diálogo
        holder.itemView.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_anime, null);

            RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);


            // cargar valores guardados
            ratingBar.setRating(anime.rating);


            // listener para toggle de estrellas
            ratingBar.setOnRatingBarChangeListener((rb, rating, fromUser) -> {
                if (!fromUser) return;

                int r = (int) rating;
                if (r == anime.rating) {
                    // si clic en la misma valoración → reset
                    anime.rating = 0;
                    anime.visto = false;
                    rb.setRating(0);

                } else if (r == 0) {
                    anime.rating = 0;
                    anime.visto = false;

                } else {
                    anime.rating = r;
                    anime.visto = true;
                }
            });

            new AlertDialog.Builder(context)
                    .setTitle(anime.title)
                    .setView(dialogView)
                    .setPositiveButton("Guardar", (dialog, which) -> {
                        // Guardar en SharedPreferences
                        editor.putBoolean("watched_" + anime.mal_id, anime.visto);
                        editor.putInt("rating_" + anime.mal_id, anime.rating);
                        editor.apply();

                        // actualizar fondo
                        holder.itemView.setBackgroundColor(
                                anime.visto ? context.getResources().getColor(android.R.color.darker_gray)
                                        : context.getResources().getColor(android.R.color.white)
                        );

                        Toast.makeText(context, "Guardado", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }



    @Override
    public int getItemCount() {
        return animeList.size();
    }

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
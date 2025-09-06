package com.example.watchguide;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.watchguide.models.Anime;
import com.google.firebase.firestore.FirebaseFirestore;

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
        holder.averageRating.setText("Nota: -"); // default

        FirebaseFirestore.getInstance()
                .collection("anime")
                .document(String.valueOf(anime.mal_id))
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains("averageRating")) {
                        double avg = doc.getDouble("averageRating");
                        holder.averageRating.setText("Nota: " + String.format("%.1f", avg + "⭐"));
                    } else {
                        holder.averageRating.setText("Nota: -");
                    }
                });
        // Obtener estado real desde la cache de Firestore
        LibraryEntry entry = userLibrary.getCache().get(anime.mal_id);
        boolean isFav = entry != null && entry.favorite;
        boolean isWatched = entry != null && entry.watched;

        // Inicializar botones
        holder.favButton.setImageResource(isFav ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        holder.buttonSeen.setImageResource(isWatched ? R.drawable.ojotachado : R.drawable.ojoabierto);
        holder.itemView.setBackgroundColor(isWatched ?
                context.getResources().getColor(android.R.color.darker_gray) :
                context.getResources().getColor(android.R.color.white));

        // Toggle favorito
        holder.favButton.setOnClickListener(v -> {
            boolean newFavState = !(entry != null && entry.favorite);
            userLibrary.setFavorite(anime, newFavState)
                    .addOnSuccessListener(aVoid -> {
                        if (entry != null) entry.favorite = newFavState;
                        holder.favButton.setImageResource(newFavState ? android.R.drawable.btn_star_big_on
                                : android.R.drawable.btn_star_big_off);
                    })
                    .addOnFailureListener(e -> Toast.makeText(context, "Error al cambiar favorito", Toast.LENGTH_SHORT).show());
        });

        // Toggle visto/no visto
        holder.buttonSeen.setOnClickListener(v -> {
            boolean newWatched = !(entry != null && entry.watched);
            userLibrary.setWatched(anime, newWatched)
                    .addOnSuccessListener(aVoid -> {
                        if (entry != null) entry.watched = newWatched;
                        holder.buttonSeen.setImageResource(newWatched ? R.drawable.ojotachado : R.drawable.ojoabierto);
                        holder.itemView.setBackgroundColor(newWatched ?
                                context.getResources().getColor(android.R.color.darker_gray) :
                                context.getResources().getColor(android.R.color.white));
                    })
                    .addOnFailureListener(e -> Toast.makeText(context, "Error al cambiar estado visto", Toast.LENGTH_SHORT).show());
        });

        // Click en item → Rating
        holder.itemView.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_anime, null);
            EditText inputRating = dialogView.findViewById(R.id.inputRating);

            // Si ya tiene una valoración guardada, mostrarla en el EditText
            if (entry != null && entry.rating > 0) {
                inputRating.setText(String.valueOf(entry.rating));
            }

            new AlertDialog.Builder(context)
                    .setTitle(anime.title)
                    .setView(dialogView)
                    .setPositiveButton("Guardar", (dialog, which) -> {
                        String valor = inputRating.getText().toString().trim();
                        if (!valor.isEmpty()) {
                            try {
                                float rating = Float.parseFloat(valor);
                                if (rating >= 1.0f && rating <= 10.0f) {
                                    anime.rating = rating;   // ahora es float, no int
                                    anime.seen = rating > 0;

                                    userLibrary.setRating(anime, anime.rating)
                                            .addOnSuccessListener(aVoid -> userLibrary.setWatched(anime, anime.seen)
                                                    .addOnSuccessListener(aVoid1 -> {
                                                        holder.itemView.setBackgroundColor(anime.seen ?
                                                                context.getResources().getColor(android.R.color.darker_gray) :
                                                                context.getResources().getColor(android.R.color.white));
                                                        holder.buttonSeen.setImageResource(anime.seen ? R.drawable.ojotachado : R.drawable.ojoabierto);
                                                    }))
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(context, "Error al guardar rating", Toast.LENGTH_SHORT).show());
                                } else {
                                    Toast.makeText(context, "La nota debe estar entre 1.0 y 10.0", Toast.LENGTH_SHORT).show();
                                }
                            } catch (NumberFormatException e) {
                                Toast.makeText(context, "Introduce un número válido", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        // Reset
        holder.buttonReset.setOnClickListener(v -> {
            if (entry != null) {
                userLibrary.setFavorite(anime, false);
                userLibrary.setWatched(anime, false);
                userLibrary.setRating(anime, 0);
            }
            holder.favButton.setImageResource(android.R.drawable.btn_star_big_off);
            holder.buttonSeen.setImageResource(R.drawable.ojoabierto);
            holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.white));
            Toast.makeText(context, "Anime reset", Toast.LENGTH_SHORT).show();
        });
    }


    @Override
    public int getItemCount() {
        return animeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, synopsis;
        ImageView image, favButton;
        ImageButton buttonReset;
        ImageButton buttonSeen;
        TextView averageRating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.animeTitle);
            synopsis = itemView.findViewById(R.id.animeSynopsis);
            image = itemView.findViewById(R.id.animeImage);
            favButton = itemView.findViewById(R.id.favButton);
            buttonReset = itemView.findViewById(R.id.buttonReset);
            buttonSeen = itemView.findViewById(R.id.buttonSeen);
            averageRating = itemView.findViewById(R.id.animeAverageRating);
        }
    }
}
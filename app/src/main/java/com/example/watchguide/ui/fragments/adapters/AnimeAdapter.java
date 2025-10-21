package com.example.watchguide.ui.fragments.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.watchguide.data.api.repository.FirestoreUserLibrary;
import com.example.watchguide.models.LibraryEntry;
import com.example.watchguide.R;
import com.example.watchguide.models.Anime;
import com.example.watchguide.ui.fragments.activities.AnimeDetailActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

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
        holder.synopsis.setText(anime.synopsis != null ? anime.synopsis : "No synopsis available");

        if (anime.images != null && anime.images.jpg != null) {
            Glide.with(context).load(anime.images.jpg.image_url).into(holder.image);
        }

        // Tipo (TV, Movie, etc.)
        if (anime.type != null && !anime.type.isEmpty()) {
            holder.animeType.setText(anime.type);
            holder.animeType.setVisibility(View.VISIBLE);
        } else {
            holder.animeType.setVisibility(View.GONE);
        }

        // Episodios
        if (anime.episodes > 0) {
            holder.animeEpisodes.setText(anime.episodes + " eps");
            holder.animeEpisodes.setVisibility(View.VISIBLE);
        } else {
            holder.animeEpisodes.setText("? eps");
            holder.animeEpisodes.setVisibility(View.VISIBLE);
        }

        // Score de MAL
        if (anime.score != null && anime.score > 0) {
            holder.animeScoreMAL.setText("⭐ " + String.format("%.1f", anime.score));
            holder.animeScoreMAL.setVisibility(View.VISIBLE);
        } else {
            holder.animeScoreMAL.setVisibility(View.GONE);
        }

        // Géneros
        if (anime.genres != null && !anime.genres.isEmpty()) {
            StringBuilder genresText = new StringBuilder();
            for (int i = 0; i < anime.genres.size(); i++) {
                genresText.append(anime.genres.get(i).name);
                if (i < anime.genres.size() - 1) {
                    genresText.append(", ");
                }
            }
            holder.animeGenres.setText(genresText.toString());
            holder.animeGenres.setVisibility(View.VISIBLE);
        } else {
            holder.animeGenres.setVisibility(View.GONE);
        }
        holder.averageRating.setText("N/A"); // default

        FirebaseFirestore.getInstance()
                .collection("anime")
                .document(String.valueOf(anime.mal_id))
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains("averageRating")) {
                        double avg = doc.getDouble("averageRating");
                        int total = doc.getLong("ratingCount").intValue();
                        holder.averageRating.setText(String.format("%.1f", avg) + " (" + total + ")");
                    } else {
                        holder.averageRating.setText("N/A"); // no hay aún valoraciones
                    }
                })
                .addOnFailureListener(e -> {
                    holder.averageRating.setText("N/A"); // si falla la lectura
                });
        // Obtener estado real desde la cache de Firestore
        LibraryEntry entry = userLibrary.getCache().get(anime.mal_id);
        boolean isFav = entry != null && entry.favorite;
        boolean isWatched = entry != null && entry.watched;

        // Inicializar botones
        holder.favButton.setImageResource(isFav ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        holder.buttonSeen.setIconResource(isWatched ? R.drawable.ojotachado : R.drawable.ojoabierto);

        // Toggle favorito
        holder.favButton.setOnClickListener(v -> {
            boolean newFavState = !(entry != null && entry.favorite);
            userLibrary.setFavorite(anime, newFavState)
                    .addOnSuccessListener(aVoid -> {
                        if (entry != null) entry.favorite = newFavState;
                        holder.favButton.setImageResource(newFavState ? android.R.drawable.btn_star_big_on
                                : android.R.drawable.btn_star_big_off);
                        // Toast informativo
                        String message = newFavState ? "Added to favorites" : "Removed from favorites";
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(context, "Error changing favorite", Toast.LENGTH_SHORT).show());
        });

        // Toggle visto/no visto
        holder.buttonSeen.setOnClickListener(v -> {
            boolean newWatched = !(entry != null && entry.watched);
            userLibrary.setWatched(anime, newWatched)
                    .addOnSuccessListener(aVoid -> {
                        if (entry != null) entry.watched = newWatched;
                        holder.buttonSeen.setIconResource(newWatched ? R.drawable.ojotachado : R.drawable.ojoabierto);
                        // Toast informativo
                        String message = newWatched ? "Marked as watched" : "Marked as not watched";
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(context, "Error changing watched status", Toast.LENGTH_SHORT).show());
        });

        // Click en item → Abrir detalle del anime con reviews
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AnimeDetailActivity.class);
            intent.putExtra("animeId", anime.mal_id);
            intent.putExtra("animeTitle", anime.title);
            intent.putExtra("animeSynopsis", anime.synopsis);
            String imageUrl = (anime.images != null && anime.images.jpg != null) ? anime.images.jpg.image_url : null;
            intent.putExtra("animeImageUrl", imageUrl);

            // Información adicional
            intent.putExtra("animeType", anime.type);
            intent.putExtra("animeEpisodes", anime.episodes);
            intent.putExtra("animeStatus", anime.status);
            intent.putExtra("animeScore", anime.score);
            intent.putExtra("animeRank", anime.rank);
            intent.putExtra("animeYear", anime.year);
            intent.putExtra("animeSeason", anime.season);
            intent.putExtra("animeSource", anime.source);
            intent.putExtra("animeDuration", anime.duration);
            intent.putExtra("animeRating", anime.rating);

            // Géneros (como String separado por comas)
            if (anime.genres != null && !anime.genres.isEmpty()) {
                StringBuilder genresText = new StringBuilder();
                for (int i = 0; i < anime.genres.size(); i++) {
                    genresText.append(anime.genres.get(i).name);
                    if (i < anime.genres.size() - 1) {
                        genresText.append(", ");
                    }
                }
                intent.putExtra("animeGenres", genresText.toString());
            }

            // Estudios (como String separado por comas)
            if (anime.studios != null && !anime.studios.isEmpty()) {
                StringBuilder studiosText = new StringBuilder();
                for (int i = 0; i < anime.studios.size(); i++) {
                    studiosText.append(anime.studios.get(i).name);
                    if (i < anime.studios.size() - 1) {
                        studiosText.append(", ");
                    }
                }
                intent.putExtra("animeStudios", studiosText.toString());
            }

            context.startActivity(intent);
        });

        // Botón RATE → Abrir diálogo de rating y reseña
        holder.buttonRate.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_anime, null);
            EditText inputRating = dialogView.findViewById(R.id.inputRating);
            EditText inputReviewText = dialogView.findViewById(R.id.inputReviewText);

            // Si ya tiene una valoración guardada, mostrarla en el EditText
            if (entry != null && entry.rating > 0) {
                inputRating.setText(String.valueOf(entry.rating));
            }

            AlertDialog alertDialog = new AlertDialog.Builder(context)
                    .setTitle(anime.title)
                    .setView(dialogView)
                    .setPositiveButton("Save", null)
                    .setNegativeButton("Cancel", null)
                    .create();

            alertDialog.setOnShowListener(dialogInterface -> {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                    String valor = inputRating.getText().toString().trim();
                    String reviewText = inputReviewText.getText().toString().trim();

                    // Validación: solo rating obligatorio, review opcional
                    if (valor.isEmpty()) {
                        Toast.makeText(context, "Please enter a rating (1.0 - 10.0)", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        float rating = Float.parseFloat(valor);
                        // Redondear a 1 decimal
                        rating = Math.round(rating * 10) / 10.0f;

                        if (rating >= 1.0f && rating <= 10.0f) {
                            anime.rating_user = rating;
                            anime.seen = true; // Al hacer review, se marca como visto

                            // Guardamos rating Y review
                            String imageUrl = (anime.images != null && anime.images.jpg != null) ? anime.images.jpg.image_url : null;
                            userLibrary.setRatingWithReview(anime, anime.rating_user, reviewText, imageUrl)
                                    .addOnSuccessListener(aVoid -> {
                                        // Guardamos visto
                                        userLibrary.setWatched(anime, true)
                                                .addOnSuccessListener(aVoid1 -> {
                                                    holder.buttonSeen.setIconResource(R.drawable.ojotachado);

                                                    // Actualizamos media en tiempo real
                                                    FirebaseFirestore.getInstance()
                                                            .collection("anime")
                                                            .document(String.valueOf(anime.mal_id))
                                                            .get()
                                                            .addOnSuccessListener(doc -> {
                                                                if (doc.exists() && doc.contains("averageRating")) {
                                                                    double avg = doc.getDouble("averageRating");
                                                                    int total = doc.getLong("ratingCount").intValue();
                                                                    holder.averageRating.setText(String.format("%.1f", avg) + " (" + total + ")");
                                                                } else {
                                                                    holder.averageRating.setText("N/A");
                                                                }
                                                            })
                                                            .addOnFailureListener(e -> holder.averageRating.setText("N/A"));

                                                    Toast.makeText(context, "Rating and review saved!", Toast.LENGTH_SHORT).show();
                                                    alertDialog.dismiss();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(context, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });

                        } else {
                            Toast.makeText(context, "Rating must be between 1.0 and 10.0", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(context, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                    }
                });
            });

            alertDialog.show();
        });


        // Reset
        holder.buttonReset.setOnClickListener(v -> {
            // Usar el metodo que elimina completamente el rating y la review
            userLibrary.resetAnime(anime);

            // Actualizar UI
            holder.favButton.setImageResource(android.R.drawable.btn_star_big_off);
            holder.buttonSeen.setIconResource(R.drawable.ojoabierto);
            holder.averageRating.setText("N/A");

            // Actualizar la media en tiempo real después de eliminar
            FirebaseFirestore.getInstance()
                    .collection("anime")
                    .document(String.valueOf(anime.mal_id))
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && doc.contains("averageRating")) {
                            double avg = doc.getDouble("averageRating");
                            Long countLong = doc.getLong("ratingCount");
                            int total = (countLong != null) ? countLong.intValue() : 0;
                            if (total > 0) {
                                holder.averageRating.setText(String.format("%.1f", avg) + " (" + total + ")");
                            } else {
                                holder.averageRating.setText("N/A");
                            }
                        } else {
                            holder.averageRating.setText("N/A");
                        }
                    });

            Toast.makeText(context, "Anime reset (rating and review deleted)", Toast.LENGTH_SHORT).show();
        });
    }


    @Override
    public int getItemCount() {
        return animeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, synopsis;
        ImageView image, favButton;
        MaterialButton buttonReset, buttonSeen, buttonRate;
        TextView averageRating;
        TextView animeType, animeEpisodes, animeScoreMAL, animeGenres;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.animeTitle);
            synopsis = itemView.findViewById(R.id.animeSynopsis);
            image = itemView.findViewById(R.id.animeImage);
            favButton = itemView.findViewById(R.id.favButton);
            buttonReset = itemView.findViewById(R.id.buttonReset);
            buttonSeen = itemView.findViewById(R.id.buttonSeen);
            buttonRate = itemView.findViewById(R.id.buttonRate);
            averageRating = itemView.findViewById(R.id.animeAverageRating);
            animeType = itemView.findViewById(R.id.animeType);
            animeEpisodes = itemView.findViewById(R.id.animeEpisodes);
            animeScoreMAL = itemView.findViewById(R.id.animeScoreMAL);
            animeGenres = itemView.findViewById(R.id.animeGenres);
        }
    }
}

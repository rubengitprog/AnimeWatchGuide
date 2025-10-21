package com.example.watchguide.ui.fragments.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.watchguide.R;
import com.example.watchguide.models.Reply;
import com.example.watchguide.models.Review;
import com.example.watchguide.ui.fragments.activities.AnimeDetailActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private final List<Review> reviewList;
    private final Context context;
    private final String currentUserId;

    public ReviewAdapter(List<Review> reviewList, Context context) {
        this.reviewList = reviewList;
        this.context = context;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
            ? FirebaseAuth.getInstance().getCurrentUser().getUid()
            : "";
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviewList.get(position);

        // Obtener username desde Firestore
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(review.userId)
                .get()
                .addOnSuccessListener(doc -> {
                    String username = doc.getString("username");
                    if (username != null) {
                        review.username = username;
                        holder.reviewUsername.setText(username);
                    } else {
                        holder.reviewUsername.setText("Unknown User");
                    }
                })
                .addOnFailureListener(e -> holder.reviewUsername.setText("Unknown User"));

        holder.reviewRating.setText(review.rating + "⭐");
        holder.reviewText.setText(review.reviewText);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String dateText = sdf.format(new Date(review.timestamp));
        holder.reviewDate.setText(dateText);

        // Mostrar información del anime
        if (review.animeTitle != null && !review.animeTitle.isEmpty()) {
            holder.reviewAnimeTitle.setText(review.animeTitle);
        } else {
            holder.reviewAnimeTitle.setText("Unknown Anime");
        }

        if (review.animeImageUrl != null && !review.animeImageUrl.isEmpty()) {
            Glide.with(context)
                    .load(review.animeImageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.reviewAnimeImage);
        }

        // Click listener para abrir AnimeDetailActivity
        holder.animeInfoContainer.setOnClickListener(v -> {
            Intent intent = new Intent(context, AnimeDetailActivity.class);
            intent.putExtra("animeId", review.animeId);
            intent.putExtra("animeTitle", review.animeTitle);
            intent.putExtra("animeImageUrl", review.animeImageUrl);
            context.startActivity(intent);
        });

        // Mostrar contadores de likes/dislikes/respuestas
        holder.reviewLikeCount.setText(String.valueOf(review.likeCount));
        holder.reviewDislikeCount.setText(String.valueOf(review.dislikeCount));
        holder.reviewReplyCount.setText(String.valueOf(review.replyCount));

        // Actualizar estado de los botones según el voto del usuario
        Boolean userVote = review.getUserVote(currentUserId);
        if (userVote != null) {
            if (userVote) {
                holder.buttonLikeReview.setAlpha(1.0f);
                holder.buttonDislikeReview.setAlpha(0.3f);
            } else {
                holder.buttonLikeReview.setAlpha(0.3f);
                holder.buttonDislikeReview.setAlpha(1.0f);
            }
        } else {
            holder.buttonLikeReview.setAlpha(0.5f);
            holder.buttonDislikeReview.setAlpha(0.5f);
        }

        // Mostrar botón de eliminar solo si es el propietario
        if (review.userId.equals(currentUserId)) {
            holder.buttonDeleteReview.setVisibility(View.VISIBLE);
            holder.buttonDeleteReview.setOnClickListener(v -> showDeleteDialog(review, position));
        } else {
            holder.buttonDeleteReview.setVisibility(View.GONE);
        }

        // Botón de reportar
        holder.buttonReportReview.setOnClickListener(v -> showReportDialog(review));

        // Botones de like/dislike
        holder.buttonLikeReview.setOnClickListener(v -> toggleLike(review, holder, position));
        holder.buttonDislikeReview.setOnClickListener(v -> toggleDislike(review, holder, position));

        // Botón de responder
        holder.buttonReplyReview.setOnClickListener(v -> showReplyDialog(review, position));

        // Cargar respuestas
        loadReplies(review, holder);
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    private void showReportDialog(Review review) {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // No puedes reportar tu propia review
        if (review.userId.equals(currentUid)) {
            Toast.makeText(context, "You cannot report your own review", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_report, null, false);
        EditText inputDescription = dialogView.findViewById(R.id.inputReportDescription);

        String[] reasons = {"Spam", "Offensive content", "Inappropriate", "Other"};

        new AlertDialog.Builder(context)
                .setTitle("Report Review")
                .setMessage("Why are you reporting this review?")
                .setSingleChoiceItems(reasons, 0, null)
                .setView(dialogView)
                .setPositiveButton("Submit Report", (dialog, which) -> {
                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    String selectedReason = reasons[selectedPosition].toLowerCase().replace(" ", "_");
                    String description = inputDescription.getText().toString().trim();

                    if (description.isEmpty()) {
                        Toast.makeText(context, "Please provide a description", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Obtener username del usuario que reporta
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(currentUid)
                            .get()
                            .addOnSuccessListener(doc -> {
                                String reporterUsername = doc.getString("username");
                                if (reporterUsername == null) reporterUsername = "Unknown";

                                // Crear reporte
                                Map<String, Object> report = new HashMap<>();
                                report.put("reportedBy", currentUid);
                                report.put("reportedByUsername", reporterUsername);
                                report.put("contentType", "review");
                                report.put("contentId", review.reviewId);
                                report.put("reason", selectedReason);
                                report.put("description", description);
                                report.put("timestamp", System.currentTimeMillis());
                                report.put("status", "pending");

                                // Guardar en Firestore
                                FirebaseFirestore.getInstance()
                                        .collection("reports")
                                        .add(report)
                                        .addOnSuccessListener(docRef -> {
                                            Toast.makeText(context, "Report submitted successfully", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(context, "Error submitting report", Toast.LENGTH_SHORT).show();
                                        });
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleLike(Review review, ViewHolder holder, int position) {
        review.addLike(currentUserId);

        // Actualizar en Firestore
        FirebaseFirestore.getInstance()
                .collection("reviews")
                .document(review.reviewId)
                .update(
                    "likes", review.likes,
                    "likeCount", review.likeCount,
                    "dislikeCount", review.dislikeCount
                )
                .addOnSuccessListener(aVoid -> {
                    notifyItemChanged(position);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error updating like", Toast.LENGTH_SHORT).show();
                });
    }

    private void toggleDislike(Review review, ViewHolder holder, int position) {
        review.addDislike(currentUserId);

        // Actualizar en Firestore
        FirebaseFirestore.getInstance()
                .collection("reviews")
                .document(review.reviewId)
                .update(
                    "likes", review.likes,
                    "likeCount", review.likeCount,
                    "dislikeCount", review.dislikeCount
                )
                .addOnSuccessListener(aVoid -> {
                    notifyItemChanged(position);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error updating dislike", Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteDialog(Review review, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Review")
                .setMessage("Are you sure you want to delete this review?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Eliminar de Firestore
                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    // Eliminar la review y todas sus respuestas
                    db.collection("reviews").document(review.reviewId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Eliminar todas las respuestas asociadas
                                db.collection("replies")
                                        .whereEqualTo("reviewId", review.reviewId)
                                        .get()
                                        .addOnSuccessListener(querySnapshot -> {
                                            querySnapshot.getDocuments().forEach(doc -> doc.getReference().delete());
                                        });

                                // Actualizar las estadísticas del anime (remover la valoración)
                                db.collection("anime").document(String.valueOf(review.animeId))
                                        .get()
                                        .addOnSuccessListener(doc -> {
                                            if (doc.exists()) {
                                                Map<String, Object> userRatings = (Map<String, Object>) doc.get("userRatings");
                                                if (userRatings != null && userRatings.containsKey(currentUserId)) {
                                                    userRatings.remove(currentUserId);

                                                    // Recalcular average
                                                    float sum = 0;
                                                    int count = userRatings.size();
                                                    for (Object rating : userRatings.values()) {
                                                        if (rating instanceof Number) {
                                                            sum += ((Number) rating).floatValue();
                                                        }
                                                    }
                                                    float avg = count > 0 ? sum / count : 0;

                                                    db.collection("anime").document(String.valueOf(review.animeId))
                                                            .update(
                                                                "userRatings", userRatings,
                                                                "ratingSum", sum,
                                                                "ratingCount", count,
                                                                "averageRating", avg
                                                            );
                                                }
                                            }
                                        });

                                // Eliminar de la lista local
                                reviewList.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, reviewList.size());

                                Toast.makeText(context, "Review deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Error deleting review", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showReplyDialog(Review review, int position) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reply, null, false);
        EditText inputReply = dialogView.findViewById(R.id.inputReplyText);

        new AlertDialog.Builder(context)
                .setTitle("Reply to Review")
                .setMessage("Write your reply to " + review.username + "'s review:")
                .setView(dialogView)
                .setPositiveButton("Submit", (dialog, which) -> {
                    String replyText = inputReply.getText().toString().trim();

                    if (replyText.isEmpty()) {
                        Toast.makeText(context, "Please write a reply", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Crear respuesta en Firestore
                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    Reply reply = new Reply();
                    reply.reviewId = review.reviewId;
                    reply.userId = currentUserId;
                    reply.replyText = replyText;
                    reply.timestamp = System.currentTimeMillis();

                    db.collection("replies")
                            .add(reply)
                            .addOnSuccessListener(docRef -> {
                                reply.replyId = docRef.getId();

                                // Actualizar el ID en Firestore
                                docRef.update("replyId", reply.replyId);

                                // Incrementar el contador de respuestas en la review
                                db.collection("reviews").document(review.reviewId)
                                        .get()
                                        .addOnSuccessListener(doc -> {
                                            int currentCount = doc.getLong("replyCount") != null
                                                ? doc.getLong("replyCount").intValue()
                                                : 0;
                                            db.collection("reviews").document(review.reviewId)
                                                    .update("replyCount", currentCount + 1);
                                            review.replyCount = currentCount + 1;
                                            notifyItemChanged(position);
                                        });

                                Toast.makeText(context, "Reply added successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Error adding reply", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadReplies(Review review, ViewHolder holder) {
        // Cargar respuestas desde Firestore
        FirebaseFirestore.getInstance()
                .collection("replies")
                .whereEqualTo("reviewId", review.reviewId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        holder.recyclerViewReplies.setVisibility(View.GONE);
                        return;
                    }

                    List<Reply> replies = new ArrayList<>();
                    for (var doc : snapshots.getDocuments()) {
                        Reply reply = doc.toObject(Reply.class);
                        if (reply != null) {
                            replies.add(reply);
                        }
                    }

                    if (replies.isEmpty()) {
                        holder.recyclerViewReplies.setVisibility(View.GONE);
                    } else {
                        holder.recyclerViewReplies.setVisibility(View.VISIBLE);
                        holder.recyclerViewReplies.setLayoutManager(new LinearLayoutManager(context));
                        holder.recyclerViewReplies.setAdapter(new ReplyAdapter(replies, context));
                    }
                });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView reviewUsername, reviewRating, reviewText, reviewDate;
        TextView reviewLikeCount, reviewDislikeCount, reviewReplyCount;
        ImageButton buttonReportReview, buttonDeleteReview;
        ImageButton buttonLikeReview, buttonDislikeReview, buttonReplyReview;
        RecyclerView recyclerViewReplies;
        ImageView reviewAnimeImage;
        TextView reviewAnimeTitle;
        LinearLayout animeInfoContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            reviewUsername = itemView.findViewById(R.id.reviewUsername);
            reviewRating = itemView.findViewById(R.id.reviewRating);
            reviewText = itemView.findViewById(R.id.reviewText);
            reviewDate = itemView.findViewById(R.id.reviewDate);
            reviewLikeCount = itemView.findViewById(R.id.reviewLikeCount);
            reviewDislikeCount = itemView.findViewById(R.id.reviewDislikeCount);
            reviewReplyCount = itemView.findViewById(R.id.reviewReplyCount);
            buttonReportReview = itemView.findViewById(R.id.buttonReportReview);
            buttonDeleteReview = itemView.findViewById(R.id.buttonDeleteReview);
            buttonLikeReview = itemView.findViewById(R.id.buttonLikeReview);
            buttonDislikeReview = itemView.findViewById(R.id.buttonDislikeReview);
            buttonReplyReview = itemView.findViewById(R.id.buttonReplyReview);
            recyclerViewReplies = itemView.findViewById(R.id.recyclerViewReplies);
            reviewAnimeImage = itemView.findViewById(R.id.reviewAnimeImage);
            reviewAnimeTitle = itemView.findViewById(R.id.reviewAnimeTitle);
            animeInfoContainer = itemView.findViewById(R.id.animeInfoContainer);
        }
    }
}

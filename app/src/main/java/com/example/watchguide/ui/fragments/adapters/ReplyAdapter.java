package com.example.watchguide.ui.fragments.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.watchguide.R;
import com.example.watchguide.models.Reply;
import com.example.watchguide.utils.TextValidator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ViewHolder> {

    private final List<Reply> replyList;
    private final Context context;
    private final String currentUserId;
    private boolean isAdmin = false;

    public ReplyAdapter(List<Reply> replyList, Context context) {
        this.replyList = replyList;
        this.context = context;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
            ? FirebaseAuth.getInstance().getCurrentUser().getUid()
            : "";

        // Verificar si el usuario actual es admin
        checkIfAdmin();
    }

    private void checkIfAdmin() {
        if (currentUserId.isEmpty()) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains("role")) {
                        String role = doc.getString("role");
                        isAdmin = "admin".equals(role);
                        notifyDataSetChanged(); // Actualizar la vista cuando se confirme el rol
                    }
                });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_reply, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reply reply = replyList.get(position);

        // Obtener username desde Firestore
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(reply.userId)
                .get()
                .addOnSuccessListener(doc -> {
                    String username = doc.getString("username");
                    if (username != null) {
                        reply.username = username;
                        holder.replyUsername.setText(username);
                    } else {
                        holder.replyUsername.setText("Unknown User");
                    }
                })
                .addOnFailureListener(e -> holder.replyUsername.setText("Unknown User"));

        holder.replyText.setText(reply.replyText);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String dateText = sdf.format(new Date(reply.timestamp));
        holder.replyDate.setText(dateText);

        // Mostrar contadores de likes/dislikes
        holder.replyLikeCount.setText(String.valueOf(reply.likeCount));
        holder.replyDislikeCount.setText(String.valueOf(reply.dislikeCount));

        // Actualizar estado de los botones según el voto del usuario
        Boolean userVote = reply.getUserVote(currentUserId);
        if (userVote != null) {
            if (userVote) {
                holder.buttonLikeReply.setAlpha(1.0f);
                holder.buttonDislikeReply.setAlpha(0.3f);
            } else {
                holder.buttonLikeReply.setAlpha(0.3f);
                holder.buttonDislikeReply.setAlpha(1.0f);
            }
        } else {
            holder.buttonLikeReply.setAlpha(0.5f);
            holder.buttonDislikeReply.setAlpha(0.5f);
        }

        // Mostrar botón de eliminar si es el propietario O si es admin
        boolean canDelete = reply.userId.equals(currentUserId) || isAdmin;
        if (canDelete) {
            holder.buttonDeleteReply.setVisibility(View.VISIBLE);
            holder.buttonDeleteReply.setOnClickListener(v -> showDeleteDialog(reply, position));
        } else {
            holder.buttonDeleteReply.setVisibility(View.GONE);
        }

        // Botones de like/dislike
        holder.buttonLikeReply.setOnClickListener(v -> toggleLike(reply, holder, position));
        holder.buttonDislikeReply.setOnClickListener(v -> toggleDislike(reply, holder, position));

        // Botón de reportar
        holder.buttonReportReply.setOnClickListener(v -> showReportDialog(reply));
    }

    @Override
    public int getItemCount() {
        return replyList.size();
    }

    private void toggleLike(Reply reply, ViewHolder holder, int position) {
        reply.addLike(currentUserId);

        // Actualizar en Firestore
        FirebaseFirestore.getInstance()
                .collection("replies")
                .document(reply.replyId)
                .update(
                    "likes", reply.likes,
                    "likeCount", reply.likeCount,
                    "dislikeCount", reply.dislikeCount
                )
                .addOnSuccessListener(aVoid -> {
                    notifyItemChanged(position);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error updating like", Toast.LENGTH_SHORT).show();
                });
    }

    private void toggleDislike(Reply reply, ViewHolder holder, int position) {
        reply.addDislike(currentUserId);

        // Actualizar en Firestore
        FirebaseFirestore.getInstance()
                .collection("replies")
                .document(reply.replyId)
                .update(
                    "likes", reply.likes,
                    "likeCount", reply.likeCount,
                    "dislikeCount", reply.dislikeCount
                )
                .addOnSuccessListener(aVoid -> {
                    notifyItemChanged(position);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error updating dislike", Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteDialog(Reply reply, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Reply")
                .setMessage("Are you sure you want to delete this reply?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Eliminar de Firestore
                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    db.collection("replies").document(reply.replyId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Decrementar el contador de respuestas en la review
                                db.collection("reviews").document(reply.reviewId)
                                        .get()
                                        .addOnSuccessListener(doc -> {
                                            int currentCount = doc.getLong("replyCount") != null
                                                ? doc.getLong("replyCount").intValue()
                                                : 0;
                                            db.collection("reviews").document(reply.reviewId)
                                                    .update("replyCount", Math.max(0, currentCount - 1));
                                        });

                                // Eliminar de la lista local
                                replyList.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, replyList.size());

                                Toast.makeText(context, "Reply deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Error deleting reply", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showReportDialog(Reply reply) {
        // No puedes reportar tu propia respuesta
        if (reply.userId.equals(currentUserId)) {
            Toast.makeText(context, "You cannot report your own reply", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_report, null, false);
        EditText inputDescription = dialogView.findViewById(R.id.inputReportDescription);

        String[] reasons = {"Spam", "Offensive content", "Inappropriate", "Other"};

        new AlertDialog.Builder(context)
                .setTitle("Report Reply")
                .setMessage("Why are you reporting this reply?")
                .setSingleChoiceItems(reasons, 0, null)
                .setView(dialogView)
                .setPositiveButton("Submit Report", (dialog, which) -> {
                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    String selectedReason = reasons[selectedPosition].toLowerCase().replace(" ", "_");
                    String description = inputDescription.getText().toString();

                    // Validar que la descripción no esté vacía y no contenga solo caracteres invisibles
                    if (!TextValidator.isValidText(description)) {
                        Toast.makeText(context, "Please provide a valid description", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Limpiar el texto
                    String cleanedDescription = TextValidator.cleanText(description);

                    // Obtener username del usuario que reporta
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(currentUserId)
                            .get()
                            .addOnSuccessListener(doc -> {
                                String reporterUsername = doc.getString("username");
                                if (reporterUsername == null) reporterUsername = "Unknown";

                                // Crear reporte
                                Map<String, Object> report = new HashMap<>();
                                report.put("reportedBy", currentUserId);
                                report.put("reportedByUsername", reporterUsername);
                                report.put("contentType", "reply");
                                report.put("contentId", reply.replyId);
                                report.put("reason", selectedReason);
                                report.put("description", cleanedDescription);
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView replyUsername, replyText, replyDate, replyLikeCount, replyDislikeCount;
        ImageButton buttonDeleteReply, buttonLikeReply, buttonDislikeReply, buttonReportReply;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            replyUsername = itemView.findViewById(R.id.replyUsername);
            replyText = itemView.findViewById(R.id.replyText);
            replyDate = itemView.findViewById(R.id.replyDate);
            replyLikeCount = itemView.findViewById(R.id.replyLikeCount);
            replyDislikeCount = itemView.findViewById(R.id.replyDislikeCount);
            buttonDeleteReply = itemView.findViewById(R.id.buttonDeleteReply);
            buttonLikeReply = itemView.findViewById(R.id.buttonLikeReply);
            buttonDislikeReply = itemView.findViewById(R.id.buttonDislikeReply);
            buttonReportReply = itemView.findViewById(R.id.buttonReportReply);
        }
    }
}

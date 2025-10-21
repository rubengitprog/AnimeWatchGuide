package com.example.watchguide.ui.fragments.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.watchguide.R;
import com.example.watchguide.models.Report;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    private List<Report> reportList;
    private Context context;

    public ReportAdapter(List<Report> reportList, Context context) {
        this.reportList = reportList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Report report = reportList.get(position);

        holder.textReason.setText("Reason: " + report.reason);
        holder.textDescription.setText(report.description != null ? report.description : "No description");
        holder.textReportedBy.setText("Reported by: " + report.reportedByUsername);

        // Cargar el contenido según el tipo de reporte
        String collectionName = "reply".equals(report.contentType) ? "replies" : "reviews";
        String fieldName = "reply".equals(report.contentType) ? "replyText" : "reviewText";

        FirebaseFirestore.getInstance()
                .collection(collectionName)
                .document(report.contentId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String contentText = doc.getString(fieldName);
                        String typeLabel = "reply".equals(report.contentType) ? "Reply" : "Review";
                        holder.textReviewContent.setText(contentText != null ? contentText : typeLabel + " not found");
                    } else {
                        String typeLabel = "reply".equals(report.contentType) ? "Reply" : "Review";
                        holder.textReviewContent.setText(typeLabel + " not found");
                    }
                });

        // Botón para eliminar el contenido reportado
        holder.btnDeleteReview.setOnClickListener(v -> {
            String contentTypeLabel = "reply".equals(report.contentType) ? "Reply" : "Review";
            String collectionToDelete = "reply".equals(report.contentType) ? "replies" : "reviews";

            new AlertDialog.Builder(context)
                    .setTitle("Delete " + contentTypeLabel)
                    .setMessage("Are you sure you want to delete this " + contentTypeLabel.toLowerCase() + " permanently?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        // Si es una reply, obtener el reviewId antes de eliminar
                        if ("reply".equals(report.contentType)) {
                            FirebaseFirestore.getInstance()
                                    .collection("replies")
                                    .document(report.contentId)
                                    .get()
                                    .addOnSuccessListener(replyDoc -> {
                                        String reviewId = replyDoc.exists() ? replyDoc.getString("reviewId") : null;

                                        // Eliminar la reply
                                        FirebaseFirestore.getInstance()
                                                .collection("replies")
                                                .document(report.contentId)
                                                .delete()
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(context, "Reply deleted", Toast.LENGTH_SHORT).show();

                                                    // Decrementar el contador de respuestas
                                                    if (reviewId != null) {
                                                        decrementReplyCount(reviewId);
                                                    }

                                                    // Marcar el reporte como resuelto
                                                    markReportAsResolved(report, position);
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(context, "Error deleting reply: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                    });
                        } else {
                            // Eliminar review directamente
                            FirebaseFirestore.getInstance()
                                    .collection(collectionToDelete)
                                    .document(report.contentId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(context, contentTypeLabel + " deleted", Toast.LENGTH_SHORT).show();
                                        markReportAsResolved(report, position);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(context, "Error deleting " + contentTypeLabel.toLowerCase() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Botón para marcar como resuelto sin eliminar la review
        holder.btnMarkResolved.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Mark as Resolved")
                    .setMessage("Mark this report as resolved without deleting the review?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        markReportAsResolved(report, position);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void markReportAsResolved(Report report, int position) {
        FirebaseFirestore.getInstance()
                .collection("reports")
                .document(report.reportId)
                .update("status", "resolved")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Report marked as resolved", Toast.LENGTH_SHORT).show();
                    reportList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, reportList.size());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error updating report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void decrementReplyCount(String reviewId) {
        // Decrementar el contador de respuestas en la review
        FirebaseFirestore.getInstance()
                .collection("reviews")
                .document(reviewId)
                .get()
                .addOnSuccessListener(reviewDoc -> {
                    if (reviewDoc.exists()) {
                        Long currentCount = reviewDoc.getLong("replyCount");
                        int newCount = (currentCount != null && currentCount > 0) ? currentCount.intValue() - 1 : 0;
                        FirebaseFirestore.getInstance()
                                .collection("reviews")
                                .document(reviewId)
                                .update("replyCount", newCount);
                    }
                })
                .addOnFailureListener(e -> {
                    // Error al actualizar el contador, no es crítico
                });
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textReason, textDescription, textReportedBy, textReviewContent;
        MaterialButton btnDeleteReview, btnMarkResolved;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textReason = itemView.findViewById(R.id.textReportReason);
            textDescription = itemView.findViewById(R.id.textReportDescription);
            textReportedBy = itemView.findViewById(R.id.textReportedBy);
            textReviewContent = itemView.findViewById(R.id.textReviewContent);
            btnDeleteReview = itemView.findViewById(R.id.btnDeleteReview);
            btnMarkResolved = itemView.findViewById(R.id.btnMarkResolved);
        }
    }
}

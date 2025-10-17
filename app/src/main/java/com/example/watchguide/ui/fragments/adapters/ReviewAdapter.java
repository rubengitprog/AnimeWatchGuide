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
import com.example.watchguide.models.Review;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private final List<Review> reviewList;
    private final Context context;

    public ReviewAdapter(List<Review> reviewList, Context context) {
        this.reviewList = reviewList;
        this.context = context;
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

        // Botón de reportar
        holder.buttonReportReview.setOnClickListener(v -> showReportDialog(review));
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView reviewUsername, reviewRating, reviewText, reviewDate;
        ImageButton buttonReportReview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            reviewUsername = itemView.findViewById(R.id.reviewUsername);
            reviewRating = itemView.findViewById(R.id.reviewRating);
            reviewText = itemView.findViewById(R.id.reviewText);
            reviewDate = itemView.findViewById(R.id.reviewDate);
            buttonReportReview = itemView.findViewById(R.id.buttonReportReview);
        }
    }
}

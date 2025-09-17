package com.example.watchguide.ui.fragments.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.watchguide.R;
import com.example.watchguide.models.ActivityItem;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ViewHolder> {

    private final List<ActivityItem> activityList;
    private final Context context;

    public FeedAdapter(List<ActivityItem> activityList, Context context) {
        this.activityList = activityList;
        this.context = context;
    }

    @NonNull
    @Override
    public FeedAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedAdapter.ViewHolder holder, int position) {
        ActivityItem item = activityList.get(position);

        // Obtener el username actualizado desde Firestore
        FirebaseFirestore.getInstance().collection("users").document(item.userId)
                .get().addOnSuccessListener(doc -> {
                    String username = doc.getString("username");
                    if (username == null) username = "Usuario";

                    String actionText = "";
                    switch (item.type) {
                        case "favorite_added":
                            actionText = username + " has added \"" + item.animeTitle + "\" to favorites!";
                            break;
                        case "favorite_removed":
                            actionText = username + " removed \"" + item.animeTitle + "\" from favorites!";
                            break;
                        case "rating":
                            if (item.value == 0) {
                                actionText = username + " did reset on \"" + item.animeTitle;
                            } else {
                                actionText = username + " has rated \"" + item.animeTitle + "\" with " + item.value + "⭐";
                            }
                            break;
                        case "watched":
                            actionText = username + " has finished watching \"" + item.animeTitle + "\"";
                            break;
                        default:
                            actionText = username + " did something with \"" + item.animeTitle + "\"";
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    String dateText = sdf.format(new Date(item.timestamp));

                    holder.textAction.setText(actionText);
                    holder.textDate.setText(dateText);
                });
    }

    @Override
    public int getItemCount() {
        return activityList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textAction, textDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textAction = itemView.findViewById(R.id.textAction);
            textDate = itemView.findViewById(R.id.textDate);
        }
    }
}
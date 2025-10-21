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
import com.example.watchguide.models.ActivityItem;
import com.example.watchguide.ui.fragments.activities.AnimeDetailActivity;
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

        // Cargar foto de perfil del usuario
        if (item.userPhotoUrl != null && !item.userPhotoUrl.isEmpty()) {
            Glide.with(context)
                    .load(item.userPhotoUrl)
                    .error(R.drawable.circle_background)
                    .into(holder.userPhoto);
        } else {
            holder.userPhoto.setImageResource(R.drawable.circle_background);
        }

        // Cargar imagen del anime
        if (item.animeImageUrl != null && !item.animeImageUrl.isEmpty()) {
            Glide.with(context)
                    .load(item.animeImageUrl)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(holder.animeImage);
        } else {
            holder.animeImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Configurar username
        String username = item.username != null ? item.username : "User";
        holder.textUsername.setText(username);

        // Configurar texto de acción e iconos según el tipo
        String actionText = "";
        holder.textRating.setVisibility(View.GONE);

        switch (item.type) {
            case "favorite_added":
                actionText = "added to favorites";
                holder.activityIcon.setImageResource(android.R.drawable.btn_star_big_on);
                break;
            case "favorite_removed":
                actionText = "removed from favorites";
                holder.activityIcon.setImageResource(android.R.drawable.btn_star_big_off);
                break;
            case "rating":
                if (item.value == 0) {
                    actionText = "reset rating for";
                    holder.activityIcon.setImageResource(R.drawable.reiniciar);
                } else {
                    actionText = "rated";
                    holder.activityIcon.setImageResource(android.R.drawable.btn_star_big_on);
                    holder.textRating.setText("⭐ " + String.format("%.1f", item.value));
                    holder.textRating.setVisibility(View.VISIBLE);
                }
                break;
            case "watched":
                actionText = "finished watching";
                holder.activityIcon.setImageResource(R.drawable.ojotachado);
                break;
            case "unwatched":
                actionText = "unmarked as watched";
                holder.activityIcon.setImageResource(R.drawable.ojoabierto);
                break;
            default:
                actionText = "interacted with";
                holder.activityIcon.setImageResource(android.R.drawable.ic_menu_info_details);
        }

        holder.textAction.setText(actionText + " " + item.animeTitle);

        // Formato de fecha más amigable
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        String dateText = sdf.format(new Date(item.timestamp));
        holder.textDate.setText(dateText);

        // Click en la carátula del anime para abrir detalles
        View.OnClickListener openDetailsListener = v -> {
            if (item.animeId != null && item.animeId > 0) {
                Intent intent = new Intent(context, AnimeDetailActivity.class);
                intent.putExtra("animeId", item.animeId);
                intent.putExtra("animeTitle", item.animeTitle);
                intent.putExtra("animeImageUrl", item.animeImageUrl);
                context.startActivity(intent);
            }
        };

        holder.animeImage.setOnClickListener(openDetailsListener);
    }

    @Override
    public int getItemCount() {
        return activityList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView userPhoto, animeImage, activityIcon;
        TextView textUsername, textDate, textAction, textRating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userPhoto = itemView.findViewById(R.id.userPhoto);
            animeImage = itemView.findViewById(R.id.animeImage);
            activityIcon = itemView.findViewById(R.id.activityIcon);
            textUsername = itemView.findViewById(R.id.textUsername);
            textDate = itemView.findViewById(R.id.textDate);
            textAction = itemView.findViewById(R.id.textAction);
            textRating = itemView.findViewById(R.id.textRating);
        }
    }
}
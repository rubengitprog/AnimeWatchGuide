package com.example.watchguide.ui.fragments.adapters;

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
import com.example.watchguide.ui.fragments.activities.ProfileActivity;
import com.example.watchguide.models.FollowingItem;

import java.util.List;

public class FollowersAdapter extends RecyclerView.Adapter<FollowersAdapter.ViewHolder> {

    private final List<FollowingItem> followersList;

    public FollowersAdapter(List<FollowingItem> list) {
        this.followersList = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_follower, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FollowingItem item = followersList.get(position);
        holder.username.setText(item.username);

        if (item.photoURL != null) {
            Glide.with(holder.itemView.getContext())
                    .load(item.photoURL)
                    .placeholder(R.drawable.circle_background)
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.circle_background);
        }

        // Al hacer click en cualquier parte del item, abrir el perfil
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), ProfileActivity.class);
            intent.putExtra("uid", item.uid);
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return followersList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView username;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.followerImage);
            username = itemView.findViewById(R.id.followerName);
        }
    }
}

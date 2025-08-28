package com.example.watchguide;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.watchguide.models.FollowingItem;

import java.util.List;

public class FollowingAdapter extends RecyclerView.Adapter<FollowingAdapter.ViewHolder> {

    private final List<FollowingItem> followingList;
    private final OnUnfollowListener listener;


    public interface OnUnfollowListener {
        void onUnfollow(FollowingItem item);
    }

    public FollowingAdapter(List<FollowingItem> list, OnUnfollowListener listener) {
        this.followingList = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_following, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FollowingItem item = followingList.get(position);
        holder.username.setText(item.username);

        if (item.photoURL != null) {
            Glide.with(holder.itemView.getContext())
                    .load(item.photoURL)
                    .placeholder(R.drawable.circle_background)
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.circle_background);
        }

        holder.unfollowButton.setOnClickListener(v -> listener.onUnfollow(item));
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), ProfileActivity.class);
            intent.putExtra("uid", item.uid);
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return followingList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView username;
        TextView unfollowButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.followingImage);
            username = itemView.findViewById(R.id.followingName);
            unfollowButton = itemView.findViewById(R.id.unfollowButton);
        }
    }
}


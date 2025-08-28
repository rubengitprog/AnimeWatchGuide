package com.example.watchguide;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private final List<UserItem> userList;
    private final Context context;
    private final FirestoreFollowManager followManager;

    public UserAdapter(List<UserItem> userList, Context context, FirestoreFollowManager followManager) {
        this.userList = userList;
        this.context = context;
        this.followManager = followManager;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserItem user = userList.get(position);
        holder.textUsername.setText(user.username);

        // Inicialmente mostramos "Seguir"
        holder.btnFollow.setText("Follow");

        // Comprobar si ya seguimos
        followManager.isFollowing(user.uid).addOnSuccessListener(isFollowing -> {
            holder.btnFollow.setText(isFollowing ? "Following" : "Follow");
        });

        holder.btnFollow.setOnClickListener(v -> {
            followManager.follow(user.uid)
                    .addOnSuccessListener(aVoid -> {
                        holder.btnFollow.setText("Following");
                        Toast.makeText(context, "Ahora sigues a " + user.username, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textUsername;
        Button btnFollow;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textUsername = itemView.findViewById(R.id.textUserName);
            btnFollow = itemView.findViewById(R.id.btnFollow);
        }
    }
}

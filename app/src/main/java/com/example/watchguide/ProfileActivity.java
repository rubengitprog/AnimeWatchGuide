package com.example.watchguide;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.watchguide.models.FollowingItem;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private TextView profileName, followersCount, followingCount;
    private TabLayout tabLayout;

    private final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileImage = findViewById(R.id.profileImage);
        profileName = findViewById(R.id.profileName);
        followersCount = findViewById(R.id.followersCount);
        followingCount = findViewById(R.id.followingCount);
        tabLayout = findViewById(R.id.tabLayout);

        loadUserInfo();
        loadCounters();

        // Tabs
        tabLayout.addTab(tabLayout.newTab().setText("Favoritos"));
        tabLayout.addTab(tabLayout.newTab().setText("Vistos"));

        // Por defecto mostramos favoritos
        replaceFragment(new FavoritesFragment(uid));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Fragment selected = null;
                if (tab.getPosition() == 0) {
                    selected = new FavoritesFragment(uid);
                } else if (tab.getPosition() == 1) {
                    selected = new WatchedFragment(uid);
                }
                if (selected != null) replaceFragment(selected);
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        profileName.setOnClickListener(v ->
                Toast.makeText(this, "Nombre: " + profileName.getText(), Toast.LENGTH_SHORT).show()
        );
        followingCount.setOnClickListener(v -> {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .collection("following")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        List<FollowingItem> list = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            String fUid = doc.getId();
                            String photoURL = doc.getString("photoURL");
                            String username = doc.getString("username");
                            list.add(new FollowingItem(fUid, username,photoURL));
                        }

                        View dialogView = getLayoutInflater().inflate(R.layout.dialog_following, null);
                        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerViewFollowing);
                        recyclerView.setLayoutManager(new LinearLayoutManager(this));

                        final FollowingAdapter[] adapterHolder = new FollowingAdapter[1];

                        adapterHolder[0] = new FollowingAdapter(list, item -> {
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            db.collection("users").document(uid)
                                    .collection("following")
                                    .document(item.uid)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Unfollowed " + item.username, Toast.LENGTH_SHORT).show();
                                        list.remove(item);
                                        adapterHolder[0].notifyDataSetChanged(); // ahora funciona
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                    );
                        });

                        recyclerView.setAdapter(adapterHolder[0]);

                        new AlertDialog.Builder(this)
                                .setTitle("Following")
                                .setView(dialogView)
                                .setPositiveButton("Close", null)
                                .show();
                    });
        });

    }

    private void loadUserInfo() {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("username");
                String photoUrl = doc.getString("photoURL");

                profileName.setText(name != null ? name : "Usuario");

                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Glide.with(this).load(photoUrl).into(profileImage);
                }
            }
        });
    }

    private void loadCounters() {
        db.collection("users").document(uid).collection("followers")
                .get().addOnSuccessListener(snap ->
                        followersCount.setText(snap.size() + " Followers"));

        db.collection("users").document(uid).collection("following")
                .get().addOnSuccessListener(snap ->
                        followingCount.setText(snap.size() + " Following"));
    }

    private void replaceFragment(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.tabContainer, fragment);
        ft.commit();
    }
}

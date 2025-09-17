package com.example.watchguide.ui.fragments.activities;

import android.content.SharedPreferences;
import android.content.res.TypedArray;
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
import com.example.watchguide.ui.fragments.adapters.FollowingAdapter;
import com.example.watchguide.R;
import com.example.watchguide.ui.fragments.FavoritesFragment;
import com.example.watchguide.ui.fragments.WatchedFragment;
import com.example.watchguide.models.FollowingItem;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private TextView profileName, followersCount, followingCount;
    private TabLayout tabLayout;
    private ImageView imageBanner;
    private SwitchMaterial deleteSwitch;

    private String uid;
    private boolean isMyProfile;
    private boolean deleteEnabled;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //  Cargar tema guardado
        int temaGuardado = getSharedPreferences("MisTemas", MODE_PRIVATE)
                .getInt("tema", R.style.TemaOnePiece);
        setTheme(temaGuardado);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Imagen principal según tema
        ImageView imagenBanner = findViewById(R.id.secondaryImage);
        int[] attrs = new int[]{R.attr.secondaryImage};
        TypedArray ta = obtainStyledAttributes(attrs);
        int imagenResId = ta.getResourceId(0, 0);
        ta.recycle();
        if (imagenResId != 0) {
            imagenBanner.setImageResource(imagenResId);
        }

        uid = getIntent().getStringExtra("uid");
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (uid == null) {
            uid = currentUid;
        }

        isMyProfile = uid.equals(currentUid);

        profileImage = findViewById(R.id.profileImage);
        profileName = findViewById(R.id.profileName);
        followersCount = findViewById(R.id.followersCount);
        followingCount = findViewById(R.id.followingCount);
        tabLayout = findViewById(R.id.tabLayout);
        deleteSwitch = findViewById(R.id.deleteSwitch);

        //  Configuración del switch
        if (isMyProfile) {
            // Leer preferencia
            SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
            deleteEnabled = prefs.getBoolean("deleteEnabled", false);
            deleteSwitch.setChecked(deleteEnabled);
            deleteSwitch.setVisibility(View.VISIBLE);

            deleteSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                deleteEnabled = isChecked;
                prefs.edit().putBoolean("deleteEnabled", isChecked).apply();
                Toast.makeText(this,
                        isChecked ? "Eliminación activada" : "Eliminación desactivada",
                        Toast.LENGTH_SHORT).show();

                // Recargar fragment actual con el nuevo estado
                int pos = tabLayout.getSelectedTabPosition();
                if (pos == 0) {
                    replaceFragment(FavoritesFragment.newInstance(uid, deleteEnabled));
                } else if (pos == 1) {
                    replaceFragment(WatchedFragment.newInstance(uid, deleteEnabled));
                }
            });
        } else {
            // Si no es mi perfil , ocultar switch y deshabilitar
            deleteEnabled = false;
            deleteSwitch.setVisibility(View.GONE);
        }

        // Cargar info del usuario y contadores
        loadUserInfo();
        loadCounters();

        // Tabs
        tabLayout.addTab(tabLayout.newTab().setText("Favorites"));
        tabLayout.addTab(tabLayout.newTab().setText("Seen"));

        replaceFragment(FavoritesFragment.newInstance(uid, deleteEnabled));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Fragment selected = null;
                if (tab.getPosition() == 0) {
                    selected = FavoritesFragment.newInstance(uid, deleteEnabled);
                } else if (tab.getPosition() == 1) {
                    selected = WatchedFragment.newInstance(uid, deleteEnabled);
                }
                if (selected != null) replaceFragment(selected);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        // Mostrar lista de following al pulsar
        followingCount.setOnClickListener(v -> loadFollowing());
    }

    private void loadUserInfo() {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("username");
                String photoUrl = doc.getString("photoURL");
                String bannerUrl = doc.getString("photoBanner");

                profileName.setText(name != null ? name : "Usuario");

                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Glide.with(this).load(photoUrl).into(profileImage);
                }

                if (bannerUrl != null && !bannerUrl.isEmpty()) {
                    Glide.with(this).load(bannerUrl).into(imageBanner);
                }
            }
        }).addOnFailureListener(e -> imageBanner.setImageResource(R.drawable.piece));
    }

    private void loadCounters() {
        db.collection("users").document(uid).collection("followers")
                .get().addOnSuccessListener(snap -> followersCount.setText(snap.size() + " Followers"));

        db.collection("users").document(uid).collection("following")
                .get().addOnSuccessListener(snap -> followingCount.setText(snap.size() + " Following"));
    }

    private void loadFollowing() {
        db.collection("users").document(uid)
                .collection("following")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<String> followingIds = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        followingIds.add(doc.getId());
                    }

                    if (followingIds.isEmpty()) {
                        Toast.makeText(this, "No estás siguiendo a nadie", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("users")
                            .whereIn(FieldPath.documentId(), followingIds)
                            .get()
                            .addOnSuccessListener(usersSnap -> {
                                List<FollowingItem> list = new ArrayList<>();
                                for (DocumentSnapshot userDoc : usersSnap.getDocuments()) {
                                    String fUid = userDoc.getId();
                                    String username = userDoc.getString("username");
                                    String photoURL = userDoc.getString("photoURL");
                                    list.add(new FollowingItem(fUid, username, photoURL));
                                }
                                showFollowingDialog(list);
                            });
                });
    }

    //Mostrar el dialog de personas a las que sigues
    private void showFollowingDialog(List<FollowingItem> list) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_following, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerViewFollowing);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        final FollowingAdapter[] adapterHolder = new FollowingAdapter[1];
        adapterHolder[0] = new FollowingAdapter(list, item -> {
            db.collection("users").document(uid)
                    .collection("following")
                    .document(item.uid)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        list.remove(item);
                        adapterHolder[0].notifyDataSetChanged();
                        Toast.makeText(this, "Unfollowed " + item.username, Toast.LENGTH_SHORT).show();
                    });
        });

        recyclerView.setAdapter(adapterHolder[0]);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Following")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }

    private void replaceFragment(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.tabContainer, fragment);
        ft.commit();
    }
}

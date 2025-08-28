package com.example.watchguide;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.watchguide.models.ActivityItem;
import com.example.watchguide.models.Anime;
import com.example.watchguide.models.AnimeResponse;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private EditText searchInput;
    private RecyclerView recyclerView;
    private AnimeAdapter adapter;
    private List<Anime> animeList = new ArrayList<>();
    private AnimeApi api;
    private Handler handler = new Handler();
    private Runnable searchRunnable;
    private FirestoreUserLibrary userLibrary; // 🔥 Manejo de favoritos en Firestore
    private RecyclerView recyclerViewFeed;
    private FeedAdapter feedAdapter;
    private List<ActivityItem> activityList = new ArrayList<>();
    private String currentUid;
    private List<ActivityItem> feedList = new ArrayList<>();
    private FirestoreFollowManager followManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);

        if (FirebaseApp.getApps(this).isEmpty()) {
            Log.e("FirebaseCheck", "Firebase NO está conectado");
        } else {
            Log.d("FirebaseCheck", "Firebase está conectado correctamente");
        }

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        followManager = new FirestoreFollowManager(currentUid);

        // Inicialización del RecyclerView del feed
        recyclerViewFeed = findViewById(R.id.recyclerViewFeed);
        recyclerViewFeed.setLayoutManager(new LinearLayoutManager(this));

        // Usamos feedList como fuente de datos
        feedAdapter = new FeedAdapter(feedList, this);
        recyclerViewFeed.setAdapter(feedAdapter);

        // ✅ Inicializar la librería de usuario en Firestore
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userLibrary = new FirestoreUserLibrary(uid);

        // Listener para cambios en la librería del usuario
        userLibrary.listen(data -> {
            Log.d("FirestoreLibrary", "Library updated: " + data.size() + " items");
        });

        searchInput = findViewById(R.id.searchInput);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AnimeAdapter(animeList, this, userLibrary); // UserLibrary al adapter
        recyclerView.setAdapter(adapter);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.jikan.moe/v4/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        api = retrofit.create(AnimeApi.class);

        searchAnime("One Piece"); // búsqueda inicial por defecto para mostrar
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (recyclerViewFeed.getVisibility() == View.VISIBLE) {
                    recyclerView.setVisibility(View.VISIBLE);
                    recyclerViewFeed.setVisibility(View.GONE);
                }


                if (searchRunnable != null) handler.removeCallbacks(searchRunnable);
                searchRunnable = () -> {
                    if (!query.isEmpty()) searchAnime(query);
                    else {
                        animeList.clear();
                        adapter.notifyDataSetChanged();
                    }
                };
                handler.postDelayed(searchRunnable, 500);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        //BottomNov itemListener
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_fav) {
                recyclerViewFeed.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                return true;
            }
            if (item.getItemId() == R.id.nav_feed) {
                recyclerView.setVisibility(View.GONE);
                recyclerViewFeed.setVisibility(View.VISIBLE);
                loadFeed();
                return true;
            }
            if (item.getItemId() == R.id.nav_friends) {
                showFriendsDialog();
                return true;
            }
            return false;
        });

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Menu lateral
        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);
        View headerView = navigationView.getHeaderView(0);
        ImageView headerImage = headerView.findViewById(R.id.headerImage);
        TextView headerTitle = headerView.findViewById(R.id.headerTitle);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String displayName = user.getDisplayName();
            String email = user.getEmail();
            String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null;

            // ✅ Guardar/actualizar en Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(uid)
                    .update(
                            "username", displayName,
                            "email", email,
                            "photoURL", photoUrl
                    )
                    .addOnSuccessListener(aVoid -> Log.d("FirestoreUser", "Perfil actualizado correctamente"))
                    .addOnFailureListener(e -> {
                        Log.w("FirestoreUser", "El documento no existía, creando uno nuevo...");
                        // En caso de que no exista aún el documento
                        java.util.Map<String, Object> userMap = new java.util.HashMap<>();
                        userMap.put("username", displayName);
                        userMap.put("email", email);
                        userMap.put("photoURL", photoUrl);
                        db.collection("users").document(uid).set(userMap);
                    });

            // 👇 Drawer UI
            headerTitle.setText(displayName);
            headerTitle.setOnClickListener(v -> {
                Toast.makeText(this, "Has pulsado tu nombre 😎", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
            });

            if (photoUrl != null) {
                Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.circle_background) // tu imagen por defecto
                        .into(headerImage);
            } else {
                headerImage.setImageResource(R.drawable.circle_background);
            }
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                0, 0
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();


        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_favorites) {
                showFavoritesDialog();
            }
            if (item.getItemId() == R.id.nav_Info) {
                new AlertDialog.Builder(this)
                        .setTitle("About")
                        .setMessage(
                                "App developed by: Rubén Robles Berlanga\n" +
                                        "Version: 1.0.0\n" +
                                        "© 2025 WatchGuide"
                        )
                        .setPositiveButton("Close", null)
                        .show();

                return true;
            }
            if (item.getItemId() == R.id.nav_exit) {
                new AlertDialog.Builder(this)
                        .setTitle("Exit")
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            FirebaseAuth.getInstance().signOut();

                            GoogleSignIn.getClient(this,
                                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                            ).signOut();

                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();

                return true;
            }
            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void searchAnime(String query) {
        api.searchAnime(query).enqueue(new Callback<AnimeResponse>() {
            @Override
            public void onResponse(Call<AnimeResponse> call, Response<AnimeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    animeList.clear();
                    animeList.addAll(response.body().data);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(MainActivity.this, "No results", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AnimeResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showFavoritesDialog() {
        userLibrary.getFavoritesOnce().addOnSuccessListener(snap -> {
            List<Anime> favList = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                Anime anime = new Anime();
                anime.mal_id = Integer.parseInt(d.getId());
                anime.title = d.getString("title");

                String image = d.getString("image_url");
                if (image != null) {
                    anime.images = new Anime.Images();
                    anime.images.jpg = new Anime.Images.JPG();
                    anime.images.jpg.image_url = image;
                }

                favList.add(anime);
            }

            if (favList.isEmpty()) {
                Toast.makeText(this, "No favorites", Toast.LENGTH_SHORT).show();
                return;
            }

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_favorites, null);
            RecyclerView favRecyclerView = dialogView.findViewById(R.id.recyclerViewFavs);
            favRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            FavoritesAdapter favAdapter = new FavoritesAdapter(favList, this);
            favRecyclerView.setAdapter(favAdapter);

            new AlertDialog.Builder(this)
                    .setTitle("Favorites list")
                    .setView(dialogView)
                    .setPositiveButton("Close", null)
                    .show();
        });
    }

    private void loadFeed() {
        followManager.listenFollowing(followingUids -> {
            // Asegurarnos de incluir siempre el UID del usuario actual
            if (!followingUids.contains(currentUid)) {
                followingUids.add(currentUid);
            }

            feedList.clear();

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            for (String uid : followingUids) {
                db.collection("users")
                        .document(uid)
                        .collection("activities")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .get()
                        .addOnSuccessListener(snap -> {
                            for (var doc : snap.getDocuments()) {
                                ActivityItem item = doc.toObject(ActivityItem.class);
                                if (item != null) feedList.add(item);
                            }
                            // Ordenar todas las actividades por timestamp globalmente
                            feedList.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
                            feedAdapter.notifyDataSetChanged();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FeedLoad", "Error loading activites " + uid + ": " + e.getMessage());
                        });
            }
        });
    }


    private void showFriendsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_friends, null);
        EditText editSearch = dialogView.findViewById(R.id.editSearchUser);
        RecyclerView recyclerViewUsers = dialogView.findViewById(R.id.recyclerViewUsers);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));

        List<UserItem> userList = new ArrayList<>();
        UserAdapter userAdapter = new UserAdapter(userList, this, followManager);
        recyclerViewUsers.setAdapter(userAdapter);

        // Buscar usuarios por nombre
        editSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String query = s.toString().trim();
                Log.d("FriendsSearch", "Users with query: " + query);
                if (query.isEmpty()) {
                    userList.clear();
                    userAdapter.notifyDataSetChanged();
                    return;
                }

                FirebaseFirestore.getInstance()
                        .collection("users")
                        .whereGreaterThanOrEqualTo("username", query)
                        .whereLessThanOrEqualTo("username", query + "\uf8ff")
                        .get()
                        .addOnSuccessListener(snap -> {
                            Log.d("FriendsSearch", "Documents found: " + snap.getDocuments().size());
                            userList.clear();
                            for (var doc : snap.getDocuments()) {
                                String uid = doc.getId();
                                String username = doc.getString("username");
                                Log.d("FriendsSearch", "User: " + username + " UID: " + uid);

                                if (uid.equals(currentUid)) continue; // no mostrar a ti mismo
                                if (username != null) userList.add(new UserItem(uid, username));
                            }
                            userAdapter.notifyDataSetChanged();
                        }).addOnFailureListener(e -> {
                            Log.e("FriendsSearch", "Error searching users: " + e.getMessage());
                        });
                ;
            }
        });

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Add new Friends")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

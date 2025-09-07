package com.example.watchguide;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private List<ActivityItem> feedList = new ArrayList<>();
    private String currentUid;
    private FirestoreFollowManager followManager;
    private int activeGenreId = -1; // -1 significa que no hay filtro activo
    private TextInputLayout inputLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SharedPreferences prefs = getSharedPreferences("MisTemas", MODE_PRIVATE);
        int temaGuardado = prefs.getInt("tema", R.style.TemaOnePiece);


        setTheme(temaGuardado); // 🔹 Aplicar antes de inflar layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 8️⃣ Aplicar imagen principal dinámica según el tema
        ImageView imagenMain = findViewById(R.id.imageMain);
        int[] attrs = new int[]{R.attr.imagenMain};
        TypedArray ta = obtainStyledAttributes(attrs);
        int imagenResId = ta.getResourceId(0, 0);
        ta.recycle();

        if (imagenResId != 0) {
            imagenMain.setImageResource(imagenResId);
            Log.d("🌟ThemeDebug🌟", "Imagen principal cargada: " + imagenResId);
        } else {
            Log.d("🌟ThemeDebug🌟", "No se encontró drawable para imagenMain");
        }

        // 9️⃣ Escalar la imagen
        imagenMain.setScaleType(ImageView.ScaleType.CENTER_CROP);

        FirebaseApp.initializeApp(this);

        if (FirebaseApp.getApps(this).isEmpty()) {
            Log.e("FirebaseCheck", "Firebase NO está conectado");
        } else {
            Log.d("FirebaseCheck", "Firebase está conectado correctamente");
        }

        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
        );

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        followManager = new FirestoreFollowManager(currentUid);

        // Inicialización del RecyclerView del feed
        recyclerViewFeed = findViewById(R.id.recyclerViewFeed);
        recyclerViewFeed.setLayoutManager(new LinearLayoutManager(this));
        feedAdapter = new FeedAdapter(feedList, this);
        recyclerViewFeed.setAdapter(feedAdapter);

        // ✅ Inicializar la librería de usuario en Firestore
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userLibrary = new FirestoreUserLibrary(uid);

        // Listener para cambios en la librería del usuario
        userLibrary.listen(data -> {
            Log.d("FirestoreLibrary", "Library updated: " + data.size() + " items");
        });
        inputLayout = findViewById(R.id.inputLayout);
        searchInput = findViewById(R.id.searchInput);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));




        adapter = new AnimeAdapter(animeList, this, userLibrary);
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

        searchAnime("One Piece"); // búsqueda inicial

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
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
            @Override public void afterTextChanged(Editable s) {}
        });
        ImageButton btnFilter = findViewById(R.id.btnFilter);
        btnFilter.setOnClickListener(v -> {
            String[] categories = { "Remove Filter","Action","Adventure","Cars","Comedy","Dementia",
                    "Demons","Mystery","Drama","Ecchi","Fantasy","Game","Hentai","Historical",
                    "Horror","Kids","Magic","Martial Arts","Mecha","Music","Parody","Samurai",
                    "Romance","School","Sci-Fi","Shoujo","Shoujo Ai","Shounen","Shounen Ai","Space",
                    "Sports","Super Power","Vampires","Yaoi","Yuri","Harem","Slice of Life",
                    "Supernatural","Military","Police","Psychological","Thriller","Seinen","Josei",
                    "Remove filter"};
            int[] categoryIds = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,
                    25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,-1};

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Select category")
                    .setItems(categories, (dialog, which) -> {
                        activeGenreId = categoryIds[which];
                        if (activeGenreId == -1 || activeGenreId == 0) {
                            Toast.makeText(MainActivity.this, "Filter removed", Toast.LENGTH_SHORT).show();
                            btnFilter.setImageResource(R.drawable.filtro);
                        } else {
                            searchAnimeByGenre(activeGenreId);
                            btnFilter.setImageResource(R.drawable.filtroon);
                        }
                    })
                    .show();
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_fav) {
                recyclerViewFeed.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                inputLayout.setVisibility(View.VISIBLE);
                btnFilter.setVisibility(View.VISIBLE);
                return true;
            }
            if (item.getItemId() == R.id.nav_feed) {
                recyclerView.setVisibility(View.GONE);
                recyclerViewFeed.setVisibility(View.VISIBLE);
                inputLayout.setVisibility(View.GONE);
                btnFilter.setVisibility(View.GONE);
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
        ImageButton iconLapiz = headerView.findViewById(R.id.iconLapiz);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null;

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            String googleName = user.getDisplayName();
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("username", googleName);
                            userMap.put("email", email);
                            userMap.put("photoURL", photoUrl);
                            db.collection("users").document(uid).set(userMap);
                            headerTitle.setText(googleName);
                        } else {
                            String savedName = doc.getString("username");
                            headerTitle.setText(savedName != null ? savedName : "Usuario");
                            db.collection("users").document(uid).update(
                                    "email", email,
                                    "photoURL", photoUrl
                            );
                        }
                        if (photoUrl != null) {
                            Glide.with(this)
                                    .load(photoUrl)
                                    .placeholder(R.drawable.circle_background)
                                    .into(headerImage);
                        } else {
                            headerImage.setImageResource(R.drawable.circle_background);
                        }
                    });

            headerTitle.setOnClickListener(v -> changeName());
            iconLapiz.setOnClickListener(v -> changeName());
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                0, 0
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();


        // 🔹 NavigationView listener
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_profile) {
                openProfile();
            }
            if (item.getItemId() == R.id.nav_Info) {
                new AlertDialog.Builder(this)
                        .setTitle("About")
                        .setMessage("App developed by: Rubén Robles Berlanga\nVersion: 1.0.0\n© 2025 WatchGuide")
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
            if (item.getItemId() == R.id.nav_theme) {
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_selector_theme, null);
                Spinner spinner = dialogView.findViewById(R.id.spinnerTemas);

                String[] temas = {"Naruto", "One Piece", "Bleach", "Dragon Ball Z", "Demon Slayer"};
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, temas);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);

                new AlertDialog.Builder(this)
                        .setTitle("Personalizar Tema")
                        .setView(dialogView)
                        .setPositiveButton("Aplicar", (dialog, which) -> {
                            int pos = spinner.getSelectedItemPosition();
                            int nuevoTema;
                            switch (pos) {
                                case 0: nuevoTema = R.style.TemaNaruto; break;
                                case 1: nuevoTema = R.style.TemaOnePiece; break;
                                case 2: nuevoTema = R.style.TemaBleach; break;
                                case 3: nuevoTema = R.style.TemaDragonBall; break;
                                case 4: nuevoTema = R.style.TemaKimetsu; break;
                                default: nuevoTema = R.style.TemaOnePiece; break;
                            }

                            getSharedPreferences("MisTemas", MODE_PRIVATE)
                                    .edit()
                                    .putInt("tema", nuevoTema)
                                    .apply();

                            recreate();
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();

                return true;
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }


    private void changeName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        // Crear un EditText dentro de un AlertDialog
        EditText input = new EditText(this);
        input.setHint("Nuevo nombre de usuario");

        new AlertDialog.Builder(this)
                .setTitle("Cambiar nombre")
                .setMessage("Introduce tu nuevo nombre de usuario:")
                .setView(input)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String newName = input.getText().toString().trim();

                    if (newName.isEmpty()) {
                        Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Guardar en Firestore
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .update("username", newName)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Nombre actualizado", Toast.LENGTH_SHORT).show();

                                // Actualizar el Navigation Drawer
                                NavigationView navigationView = findViewById(R.id.navigationView);
                                View headerView = navigationView.getHeaderView(0);
                                TextView headerName = headerView.findViewById(R.id.headerTitle);

                                headerName.setText(newName);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error al actualizar nombre", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }



    private void openProfile() {
        Intent intent = new Intent(this, ProfileActivity.class);
        startActivity(intent);
    }


    private void searchAnime(String query) {
        if (activeGenreId != -1) {
            // Si hay un filtro activo, buscar por género Y query
            api.searchAnimeWithGenre(query, activeGenreId).enqueue(new Callback<AnimeResponse>() {
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
        } else {
            // Búsqueda normal sin filtro
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
    }


    private void searchAnimeByGenre(int genreId) {
        api.getAnimeByGenre(genreId).enqueue(new Callback<AnimeResponse>() {
            @Override
            public void onResponse(Call<AnimeResponse> call, Response<AnimeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    animeList.clear();
                    animeList.addAll(response.body().data);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(MainActivity.this, "No results for this category", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AnimeResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
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

        // 🔹 Cargar los que sigues
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUid)
                .collection("following")
                .get()
                .addOnSuccessListener(snap -> {
                    userList.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String fUid = doc.getId();
                        String username = doc.getString("username");
                        if (username != null) userList.add(new UserItem(fUid, username));
                    }
                    userAdapter.notifyDataSetChanged();
                });

        // 🔹 Buscar usuarios localmente (case-insensitive)
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim().toLowerCase();

                FirebaseFirestore.getInstance()
                        .collection("users")
                        .get()
                        .addOnSuccessListener(snap -> {
                            userList.clear();
                            for (DocumentSnapshot doc : snap.getDocuments()) {
                                String fUid = doc.getId();
                                String username = doc.getString("username");
                                if (username == null || fUid.equals(currentUid)) continue;
                                if (username.toLowerCase().contains(query)) {
                                    userList.add(new UserItem(fUid, username));
                                }
                            }
                            userAdapter.notifyDataSetChanged();
                        });
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

package com.example.watchguide;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.watchguide.models.Anime;
import com.example.watchguide.models.AnimeResponse;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

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
    private Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchInput = findViewById(R.id.searchInput);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AnimeAdapter(animeList, this);
        recyclerView.setAdapter(adapter);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

// Cliente OkHttp con el interceptor
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

// Retrofit con cliente que loggea todo
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.jikan.moe/v4/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        api = retrofit.create(AnimeApi.class);





        searchAnime("naruto"); // <- aquí pones el término que quieras mostrar por defecto
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (searchRunnable != null) handler.removeCallbacks(searchRunnable);
                searchRunnable = () -> {
                    if (!query.isEmpty()) searchAnime(query);
                    else {
                        animeList.clear();
                        adapter.notifyDataSetChanged();
                    }
                };
                handler.postDelayed(searchRunnable, 500); // espera 500ms
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
            // antes y después se quedan vacíos
        });
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setItemBackgroundResource(android.R.color.transparent);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_fav) {
                showFavoritesDialog();
                return true;
            }
            return false;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                0, 0
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

// Click en items del drawer
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_favorites) {
                showFavoritesDialog();
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
                    Toast.makeText(MainActivity.this, "Sin resultados", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AnimeResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void showFavoritesDialog() {
        SharedPreferences prefs = getSharedPreferences("favorites", Context.MODE_PRIVATE);
        List<Anime> favList = new ArrayList<>();

        for (String key : prefs.getAll().keySet()) {
            if (key.endsWith("_title")) {
                String id = key.replace("_title", "");
                String title = prefs.getString(key, "");
                String image = prefs.getString(id + "_image", "");

                Anime anime = new Anime();
                anime.mal_id = Integer.parseInt(id);
                anime.title = title;
                anime.images = new Anime.Images();
                anime.images.jpg = new Anime.Images.JPG();
                anime.images.jpg.image_url = image;

                favList.add(anime);
            }
        }

        if (favList.isEmpty()) {
            Toast.makeText(this, "No tienes favoritos", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_favorites , null);
        RecyclerView favRecyclerView = dialogView.findViewById(R.id.recyclerViewFavs);
        favRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        FavoritesAdapter favAdapter = new FavoritesAdapter(favList, this);
        favRecyclerView.setAdapter(favAdapter);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Favoritos")
                .setView(dialogView)
                .setPositiveButton("Cerrar", null)
                .show();
    }

}
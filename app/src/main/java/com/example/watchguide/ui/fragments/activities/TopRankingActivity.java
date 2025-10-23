package com.example.watchguide.ui.fragments.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.watchguide.R;
import com.example.watchguide.data.api.AnimeApi;
import com.example.watchguide.models.Anime;
import com.example.watchguide.models.TopAnimeResponse;
import com.example.watchguide.ui.fragments.adapters.TopAnimeAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TopRankingActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TopAnimeAdapter adapter;
    private List<Anime> animeList = new ArrayList<>();
    private ProgressBar progressBar;
    private MaterialButton btnGoToTop1;
    private FloatingActionButton fabScrollToTop;

    // Local ranking
    private RecyclerView recyclerViewLocal;
    private TopAnimeAdapter adapterLocal;
    private List<Anime> animeListLocal = new ArrayList<>();
    private TextView textEmptyLocalRanking;
    private TabLayout tabLayout;
    private LinearLayout filterContainer;
    private ChipGroup chipGroupFilter;
    private int minRatingsFilter = 1; // Mínimo de valoraciones para el ranking local

    private int currentPage = 1;
    private int initialPage = 1;
    private int maxPage = 4; // Por defecto top 100
    private boolean isLoading = false;
    private boolean hasMorePages = true;
    private int highlightAnimeId = -1;
    private int animeRank = 0;
    private boolean startedFromMiddle = false;

    private AnimeApi api;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_ranking);

        // Obtener el ID y rank del anime a resaltar
        highlightAnimeId = getIntent().getIntExtra("highlightAnimeId", -1);
        animeRank = getIntent().getIntExtra("animeRank", 0);

        // Calcular maxPage dinámico según el rank
        if (animeRank > 100) {
            // Si el anime está fuera del top 100, calcular páginas necesarias + margen
            maxPage = (animeRank / 25) + 2; // +2 páginas de margen
        } else {
            maxPage = 4; // Top 100 por defecto
        }

        // Calcular página inicial según el rank
        if (animeRank > 25) {
            // Empezar 1 página antes para dar contexto
            initialPage = Math.max(1, (animeRank / 25));
            currentPage = initialPage;
            startedFromMiddle = true;
        } else {
            initialPage = 1;
            currentPage = 1;
            startedFromMiddle = false;
        }

        // Inicializar vistas
        ImageButton btnBack = findViewById(R.id.btnBack);
        recyclerView = findViewById(R.id.recyclerViewTopAnime);
        recyclerViewLocal = findViewById(R.id.recyclerViewLocalRanking);
        progressBar = findViewById(R.id.progressBarLoading);
        btnGoToTop1 = findViewById(R.id.btnGoToTop1);
        fabScrollToTop = findViewById(R.id.fabScrollToTop);
        textEmptyLocalRanking = findViewById(R.id.textEmptyLocalRanking);
        tabLayout = findViewById(R.id.tabLayout);
        filterContainer = findViewById(R.id.filterContainer);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);

        btnBack.setOnClickListener(v -> finish());

        // Inicializar Firestore
        db = FirebaseFirestore.getInstance();

        // Configurar listener para el filtro de ratings
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipAll) {
                    minRatingsFilter = 1;
                } else if (checkedId == R.id.chip3) {
                    minRatingsFilter = 3;
                } else if (checkedId == R.id.chip5) {
                    minRatingsFilter = 5;
                } else if (checkedId == R.id.chip10) {
                    minRatingsFilter = 10;
                }
                // Recargar el ranking local con el nuevo filtro
                loadLocalRanking();
            }
        });

        // Mostrar botón "Go to Top #1" si empezamos desde el medio
        if (startedFromMiddle) {
            btnGoToTop1.setVisibility(View.VISIBLE);
            btnGoToTop1.setOnClickListener(v -> resetToTop1());
        }

        // Configurar RecyclerView MAL
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new TopAnimeAdapter(animeList, this, highlightAnimeId);
        recyclerView.setAdapter(adapter);

        // Configurar RecyclerView Local
        LinearLayoutManager layoutManagerLocal = new LinearLayoutManager(this);
        recyclerViewLocal.setLayoutManager(layoutManagerLocal);
        adapterLocal = new TopAnimeAdapter(animeListLocal, this, -1);
        recyclerViewLocal.setAdapter(adapterLocal);

        // Configurar Retrofit
        setupRetrofit();

        // Configurar TabLayout
        tabLayout.addTab(tabLayout.newTab().setText("MAL Ranking"));
        tabLayout.addTab(tabLayout.newTab().setText("WatchGuide Ranking"));

        // Listener para cambio de tabs
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    // Tab MAL
                    recyclerView.setVisibility(View.VISIBLE);
                    recyclerViewLocal.setVisibility(View.GONE);
                    textEmptyLocalRanking.setVisibility(View.GONE);
                    filterContainer.setVisibility(View.GONE); // Ocultar filtro
                    // Mostrar/ocultar botón Go to Top 1 solo si empezó desde el medio
                    btnGoToTop1.setVisibility(startedFromMiddle ? View.VISIBLE : View.GONE);
                } else {
                    // Tab Local
                    recyclerView.setVisibility(View.GONE);
                    recyclerViewLocal.setVisibility(View.VISIBLE);
                    btnGoToTop1.setVisibility(View.GONE);
                    filterContainer.setVisibility(View.VISIBLE); // Mostrar filtro

                    // Cargar ranking local si aún no se ha cargado
                    if (animeListLocal.isEmpty()) {
                        loadLocalRanking();
                    }
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // FAB scroll to top listener
        fabScrollToTop.setOnClickListener(v -> {
            if (recyclerView.getVisibility() == View.VISIBLE) {
                recyclerView.smoothScrollToPosition(0);
            } else {
                recyclerViewLocal.smoothScrollToPosition(0);
            }
        });

        // Scroll listener para carga infinita y mostrar/ocultar FAB - MAL
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (manager != null) {
                    int visibleItemCount = manager.getChildCount();
                    int totalItemCount = manager.getItemCount();
                    int firstVisibleItemPosition = manager.findFirstVisibleItemPosition();

                    // Mostrar FAB si ha scrolleado más de 5 items
                    if (firstVisibleItemPosition > 5) {
                        fabScrollToTop.show();
                    } else {
                        fabScrollToTop.hide();
                    }

                    // Cargar más cuando llegue cerca del final
                    if (!isLoading && hasMorePages) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5
                                && firstVisibleItemPosition >= 0) {
                            loadTopAnime();
                        }
                    }
                }
            }
        });

        // Scroll listener para mostrar/ocultar FAB - Local
        recyclerViewLocal.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (manager != null) {
                    int firstVisibleItemPosition = manager.findFirstVisibleItemPosition();

                    // Mostrar FAB si ha scrolleado más de 5 items
                    if (firstVisibleItemPosition > 5) {
                        fabScrollToTop.show();
                    } else {
                        fabScrollToTop.hide();
                    }
                }
            }
        });

        // Cargar primera página
        loadTopAnime();
    }

    private void setupRetrofit() {
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
    }

    private void resetToTop1() {
        // Limpiar lista actual
        animeList.clear();
        adapter.notifyDataSetChanged();

        // Resetear variables
        currentPage = 1;
        initialPage = 1;
        hasMorePages = true;
        startedFromMiddle = false;

        // Ocultar botón "Go to Top #1"
        btnGoToTop1.setVisibility(View.GONE);

        // Cargar desde el top 1
        loadTopAnime();
    }

    private void loadTopAnime() {
        if (currentPage > maxPage) {
            hasMorePages = false;
            return;
        }

        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);

        api.getTopAnime(currentPage).enqueue(new Callback<TopAnimeResponse>() {
            @Override
            public void onResponse(Call<TopAnimeResponse> call, Response<TopAnimeResponse> response) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    List<Anime> newAnimes = response.body().data;
                    int startPosition = animeList.size();
                    animeList.addAll(newAnimes);
                    adapter.notifyItemRangeInserted(startPosition, newAnimes.size());

                    currentPage++;

                    // Verificar si hay más páginas
                    if (response.body().pagination != null) {
                        hasMorePages = response.body().pagination.has_next_page && currentPage <= maxPage;
                    }

                    // Si el anime resaltado está en esta página, hacer scroll hasta él
                    if (highlightAnimeId != -1 && startPosition == 0 && startedFromMiddle) {
                        // Primera carga desde el medio, hacer scroll al anime
                        recyclerView.post(() -> scrollToHighlightedAnime());
                    }
                } else {
                    Toast.makeText(TopRankingActivity.this, "Error loading top anime", Toast.LENGTH_SHORT).show();
                    Log.e("TopRanking", "Response not successful: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<TopAnimeResponse> call, Throwable t) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(TopRankingActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("TopRanking", "Error loading top anime", t);
            }
        });
    }

    private void scrollToHighlightedAnime() {
        for (int i = 0; i < animeList.size(); i++) {
            if (animeList.get(i).mal_id == highlightAnimeId) {
                final int position = i;
                recyclerView.post(() -> recyclerView.smoothScrollToPosition(position));
                break;
            }
        }
    }

    private void loadLocalRanking() {
        progressBar.setVisibility(View.VISIBLE);
        textEmptyLocalRanking.setVisibility(View.GONE);

        db.collection("anime")
                .whereGreaterThan("ratingCount", 0)
                .orderBy("ratingCount")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);

                    if (queryDocumentSnapshots.isEmpty()) {
                        textEmptyLocalRanking.setVisibility(View.VISIBLE);
                        recyclerViewLocal.setVisibility(View.GONE);
                        return;
                    }

                    // Lista temporal para filtrar y ordenar
                    List<Anime> tempList = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Long ratingCountLong = doc.getLong("ratingCount");
                            int ratingCount = ratingCountLong != null ? ratingCountLong.intValue() : 0;

                            // Aplicar filtro de mínimo de valoraciones
                            if (ratingCount < minRatingsFilter) {
                                continue;
                            }

                            Anime anime = new Anime();
                            anime.mal_id = Integer.parseInt(doc.getId());
                            anime.title = doc.getString("title");

                            // Obtener averageRating
                            Double avgRating = doc.getDouble("averageRating");
                            anime.score = avgRating != null ? avgRating : 0.0;

                            // Usar ratingCount como episodes para mostrar el total de ratings
                            anime.episodes = ratingCount;

                            // Imagen
                            String imageUrl = doc.getString("image_url");
                            if (imageUrl != null) {
                                anime.images = new Anime.Images();
                                anime.images.jpg = new Anime.Images.JPG();
                                anime.images.jpg.image_url = imageUrl;
                            }

                            anime.type = "WatchGuide"; // Indicador de que es del ranking local

                            tempList.add(anime);
                        } catch (Exception e) {
                            Log.e("TopRanking", "Error parsing anime: " + doc.getId(), e);
                        }
                    }

                    // Ordenar por averageRating descendente
                    tempList.sort((a1, a2) -> Double.compare(a2.score, a1.score));

                    // Limitar a 100 y asignar ranks
                    animeListLocal.clear();
                    int rank = 1;
                    for (int i = 0; i < Math.min(100, tempList.size()); i++) {
                        Anime anime = tempList.get(i);
                        anime.rank = rank++;
                        animeListLocal.add(anime);
                    }

                    adapterLocal.notifyDataSetChanged();

                    if (animeListLocal.isEmpty()) {
                        textEmptyLocalRanking.setVisibility(View.VISIBLE);
                        recyclerViewLocal.setVisibility(View.GONE);
                    } else {
                        textEmptyLocalRanking.setVisibility(View.GONE);
                        recyclerViewLocal.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading local ranking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("TopRanking", "Error loading local ranking", e);
                    textEmptyLocalRanking.setVisibility(View.VISIBLE);
                    recyclerViewLocal.setVisibility(View.GONE);
                });
    }
}

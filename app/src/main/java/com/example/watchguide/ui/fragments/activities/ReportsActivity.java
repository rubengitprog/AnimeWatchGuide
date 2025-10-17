package com.example.watchguide.ui.fragments.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.watchguide.R;
import com.example.watchguide.models.Report;
import com.example.watchguide.ui.fragments.adapters.ReportAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ReportsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewReports;
    private ReportAdapter reportAdapter;
    private List<Report> reportList = new ArrayList<>();
    private TextView textNoReports;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        // Configurar toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Inicializar vistas
        recyclerViewReports = findViewById(R.id.recyclerViewReports);
        textNoReports = findViewById(R.id.textNoReports);

        recyclerViewReports.setLayoutManager(new LinearLayoutManager(this));
        reportAdapter = new ReportAdapter(reportList, this);
        recyclerViewReports.setAdapter(reportAdapter);

        // Cargar reportes pendientes
        loadReports();
    }

    private void loadReports() {
        FirebaseFirestore.getInstance()
                .collection("reports")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    reportList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Report report = doc.toObject(Report.class);
                        if (report != null) {
                            report.reportId = doc.getId();
                            reportList.add(report);
                        }
                    }

                    if (reportList.isEmpty()) {
                        textNoReports.setVisibility(View.VISIBLE);
                        recyclerViewReports.setVisibility(View.GONE);
                    } else {
                        textNoReports.setVisibility(View.GONE);
                        recyclerViewReports.setVisibility(View.VISIBLE);
                    }

                    reportAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    textNoReports.setText(R.string.error_loading_reports, TextView.BufferType.valueOf(e.getMessage()));
                    textNoReports.setVisibility(View.VISIBLE);
                });
    }
}

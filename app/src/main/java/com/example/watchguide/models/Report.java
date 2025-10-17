package com.example.watchguide.models;

public class Report {
    public String reportId;
    public String reportedBy; // UID del usuario que reporta
    public String reportedByUsername; // Nombre del usuario que reporta
    public String contentType; // "review"
    public String contentId; // ID de la review reportada
    public String reason; // "spam", "offensive", "inappropriate", "other"
    public String description; // Descripción adicional
    public long timestamp;
    public String status; // "pending", "resolved", "dismissed"

    public Report() {
        // Constructor vacío requerido por Firestore
    }

    public Report(String reportedBy, String reportedByUsername, String contentType, String contentId,
                  String reason, String description, long timestamp, String status) {
        this.reportedBy = reportedBy;
        this.reportedByUsername = reportedByUsername;
        this.contentType = contentType;
        this.contentId = contentId;
        this.reason = reason;
        this.description = description;
        this.timestamp = timestamp;
        this.status = status;
    }
}

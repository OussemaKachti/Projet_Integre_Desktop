package com.esprit.models;

import java.time.LocalDateTime;

public class Participant {
    private int id;
    private int user_id;
    private int club_id;
    private LocalDateTime date_request;
    private String statut;
    private String description;

    // Constructeurs
    public Participant() {
        this.date_request = LocalDateTime.now();
        this.statut = "en_attente";
    }

    public Participant(int user_id, int club_id, String description) {
        this.user_id = user_id;
        this.club_id = club_id;
        this.description = description;
        this.date_request = LocalDateTime.now();
        this.statut = "en_attente";
    }

    public Participant(int user_id, int club_id, String description, String statut) {
        this.user_id = user_id;
        this.club_id = club_id;
        this.description = description;
       // this.date_request = LocalDateTime.now();
        this.statut = "en_attente";
    }

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public int getClub_id() {
        return club_id;
    }

    public void setClub_id(int club_id) {
        this.club_id = club_id;
    }

    public LocalDateTime getDate_request() {
        return date_request;
    }

    public void setDate_request(LocalDateTime date_request) {
        this.date_request = date_request;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    @Override
    public String toString() {
        return "ID: " + id + ", User ID: " + user_id + ", Club ID: " + club_id + ", Statut: " + statut;
    }
}

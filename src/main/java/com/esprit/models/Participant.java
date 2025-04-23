package com.esprit.models;

import java.time.LocalDateTime;

public class Participant {
    private int id;
    private int user_id;
    private int club_id;
    private LocalDateTime date_request;
    private String statut;
    private String description;
    private Club club;  // Association to Club (new field)

    // Default constructor
    public Participant() {
        this.date_request = LocalDateTime.now();
        this.statut = "en_attente";  // default statut
    }

    // Constructor with user_id, club_id, and description, and automatic default values for statut and date_request
    public Participant(int user_id, int club_id, String description) {
        this.user_id = user_id;
        this.club_id = club_id;
        this.description = description;
        this.date_request = LocalDateTime.now();  // Automatically set to current time
        this.statut = "en_attente";  // Default statut
    }

    // Constructor with user_id, club_id, description, and statut
    public Participant(int user_id, int club_id, String description, String statut) {
        this.user_id = user_id;
        this.club_id = club_id;
        this.description = description;
        this.date_request = LocalDateTime.now();  // Automatically set to current time
        this.statut = statut != null ? statut : "en_attente";  // If statut is not provided, default to "en_attente"
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

    // New method to get the club name
    public String getClubName() {
        return (club != null) ? club.getNomC() : "Unknown";  // Return the club's name if available
    }

    // Set the Club object
    public void setClub(Club club) {
        this.club = club;
    }

    // Override toString method to display Participant details
    @Override
    public String toString() {
        return "ID: " + id + ", User ID: " + user_id + ", Club ID: " + club_id + ", Statut: " + statut;
    }

    public Object getNomC() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getNomC'");
    }
}

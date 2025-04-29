package com.esprit.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Participant {
    private int id;
    private int user_id;
    private int club_id;
    private LocalDateTime date_request;
    private String statut;
    private String description;
    private String name;  // New field for participant's name
    private Club club;   // Association to Club

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Club getClub() {
        return club;
    }

    public void setClub(Club club) {
        this.club = club;
    }

    // Method to get the participant's name as a StringProperty for JavaFX
    public StringProperty nameProperty() {
        return new SimpleStringProperty(name != null ? name : "Unknown");
    }

    // Method to get the club name as a StringProperty for JavaFX
    public StringProperty clubNameProperty() {
        return new SimpleStringProperty(club != null ? club.getNomC() : "Unknown");
    }

    // Method to get the date_request as a StringProperty for JavaFX
    public StringProperty dateRequestProperty() {
        return new SimpleStringProperty(date_request != null ? date_request.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "Unknown");
    }

    // Override toString method to display Participant details
    @Override
    public String toString() {
        return "ID: " + id + ", User ID: " + user_id + ", Club ID: " + club_id + ", Statut: " + statut + ", Name: " + name;
    }
}

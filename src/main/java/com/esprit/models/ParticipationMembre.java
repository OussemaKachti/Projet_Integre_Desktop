package com.esprit.models;

import java.time.LocalDateTime;

/**
 * Modèle pour la participation d'un membre à un club
 * Équivalent de l'entité ParticipationMembre dans Symfony
 */
public class ParticipationMembre {
    private Integer id;
    private LocalDateTime dateRequest;
    private String statut; // Valeurs possibles : enAttente, accepte, refuse
    private User user;
    private Club club;
    private String description;
    
    public ParticipationMembre() {
        this.dateRequest = LocalDateTime.now();
        this.statut = "enAttente";
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getDateRequest() {
        return dateRequest;
    }

    public void setDateRequest(LocalDateTime dateRequest) {
        this.dateRequest = dateRequest;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Club getClub() {
        return club;
    }

    public void setClub(Club club) {
        this.club = club;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "ParticipationMembre{" +
                "id=" + id +
                ", statut='" + statut + '\'' +
                ", user=" + (user != null ? user.getFullName() : "null") +
                ", club=" + (club != null ? club.getNom() : "null") +
                '}';
    }
} 
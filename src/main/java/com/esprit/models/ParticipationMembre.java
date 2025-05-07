package com.esprit.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Modèle pour la participation d'un membre à un club
 * Équivalent de l'entité ParticipationMembre dans Symfony
 */
@Entity
@Table(name = "participation_membre")
public class ParticipationMembre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "date_request")
    private LocalDateTime dateRequest;
    
    @Column(name = "statut")
    private String statut; // Valeurs possibles : enAttente, accepte, refuse
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "club_id")
    private Club club;
    
    @Column(name = "description")
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
<<<<<<< HEAD
                ", club=" + (club != null ? club.getNom() : "null") +
=======
                ", club=" + (club != null ? club.getNomC() : "null") +
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
                '}';
    }
} 
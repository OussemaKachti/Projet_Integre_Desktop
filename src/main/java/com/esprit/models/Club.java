package com.esprit.models;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "club")
public class Club {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_creation")
    private LocalDate dateCreation;
    
    @Column(name = "logo")
    private String logo;
    
    @Column(name = "points")
    private int points;

    public Club() {
    }

    public Club(int id, String nom, String description, LocalDate dateCreation) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.dateCreation = dateCreation;
    }
    
    public Club(int id, String nom, String description, LocalDate dateCreation, String logo, int points) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.dateCreation = dateCreation;
        this.logo = logo;
        this.points = points;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }
    
    // Method needed by other parts of the application
    public String getNomC() {
        return nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDate dateCreation) {
        this.dateCreation = dateCreation;
    }
    
    public String getLogo() {
        return logo;
    }
    
    public void setLogo(String logo) {
        this.logo = logo;
    }
    
    public int getPoints() {
        return points;
    }
    
    public void setPoints(int points) {
        this.points = points;
    }

    @Override
    public String toString() {
        return "Club{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", dateCreation=" + dateCreation +
                ", logo='" + logo + '\'' +
                ", points=" + points +
                '}';
    }
}
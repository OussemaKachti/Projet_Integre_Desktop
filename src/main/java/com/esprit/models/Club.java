package com.esprit.models;

public class Club {
    private int id, president_id, points;
    private String nom_c, statuts, description, image;

    // Constructeur
    public Club(int id, int president_id, int points, String nom_c, String statuts, String description, String image) {
        this.id = id;
        this.president_id = president_id;
        this.points = points;
        this.nom_c = nom_c;
        this.statuts = statuts;
        this.description = description;
        this.image = image;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPresident_id() {
        return president_id;
    }

    public void setPresident_id(int president_id) {
        this.president_id = president_id;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getNom_c() {
        return nom_c;
    }

    public void setNom_c(String nom_c) {
        this.nom_c = nom_c;
    }

    public String getStatuts() {
        return statuts;
    }

    public void setStatuts(String statuts) {
        this.statuts = statuts;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    // Méthode toString pour affichage dans la ComboBox
    @Override
    public String toString() {
        return nom_c; // Affichage du nom du club dans la ComboBox
    }
}

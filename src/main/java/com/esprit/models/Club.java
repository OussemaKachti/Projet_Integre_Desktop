package com.esprit.models;

public class Club {

    private int id; // id Primaire
    private int presidentId; // president_id Index
    private String nomC; // nom_c
    private String description; // description
    private String status; // status
    private String image; // image
    private int points; // points

    // Constructeur par défaut
    public Club() {}

    // Constructeur avec tous les paramètres
    public Club(int id, int presidentId, String nomC, String description, String status, String image, int points) {
        this.id = id;
        this.presidentId = presidentId;
        this.nomC = nomC;
        this.description = description;
        this.status = status;
        this.image = image;
        this.points = points;
    }

    // Getters et Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPresidentId() {
        return presidentId;
    }

    public void setPresidentId(int presidentId) {
        this.presidentId = presidentId;
    }

    public String getNomC() {
        return nomC;
    }

    public void setNomC(String nomC) {
        this.nomC = nomC;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    @Override
    public String toString() {
        return "Club [id=" + id + ", presidentId=" + presidentId + ", nomC=" + nomC + ", description=" + description
                + ", status=" + status + ", image=" + image + ", points=" + points + "]";
    }
}

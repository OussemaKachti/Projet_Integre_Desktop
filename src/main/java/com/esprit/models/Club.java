package com.esprit.models;

import javafx.beans.property.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Club {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty nomC = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty logo = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> dateCreation = new SimpleObjectProperty<>();
    private final ObjectProperty<User> president = new SimpleObjectProperty<>();

    public int getPoints() {
        return points.get();
    }

    public IntegerProperty pointsProperty() {
        return points;
    }
    public void setPoints(int points) {
        this.points.set(points);
    }

    private final IntegerProperty points = new SimpleIntegerProperty();
    private final StringProperty status = new SimpleStringProperty();
    private List<User> membres = new ArrayList<>();
    private List<Sondage> sondages = new ArrayList<>();

    public Club() {
        this.dateCreation.set(LocalDateTime.now());
    }

    // Getters and Setters with Properties
    public int getId() {
        return id.get();
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public String getNomC() {
        return nomC.get();
    }

    public StringProperty nomCProperty() {
        return nomC;
    }

    public void setNomC(String nomC) {
        this.nomC.set(nomC);
    }

    public String getDescription() {
        return description.get();
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    public String getLogo() {
        return logo.get();
    }

    public StringProperty logoProperty() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo.set(logo);
    }

    public LocalDateTime getDateCreation() {
        return dateCreation.get();
    }

    public ObjectProperty<LocalDateTime> dateCreationProperty() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime date) {
        this.dateCreation.set(date);
    }

    public User getPresident() {
        return president.get();
    }

    public ObjectProperty<User> presidentProperty() {
        return president;
    }

    public void setPresident(User president) {
        this.president.set(president);
    }

    public String getStatus() {
        return status.get();
    }

    public StringProperty statusProperty() {
        return status;
    }

    public void setStatus(String status) {
        this.status.set(status);
    }

    // List management
    public List<User> getMembres() {
        return membres;
    }

    public void setMembres(List<User> membres) {
        this.membres = membres;
    }

    public List<Sondage> getSondages() {
        return sondages;
    }

    public void setSondages(List<Sondage> sondages) {
        this.sondages = sondages;
    }

    public void addMembre(User membre) {
        if (!this.membres.contains(membre)) {
            this.membres.add(membre);
        }
    }

    public void addSondage(Sondage sondage) {
        if (!this.sondages.contains(sondage)) {
            this.sondages.add(sondage);
            sondage.setClub(this);
        }
    }
}
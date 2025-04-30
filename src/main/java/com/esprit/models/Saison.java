package com.esprit.models;

import javafx.beans.property.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Saison {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty nomSaison = new SimpleStringProperty();
    private final StringProperty descSaison = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> dateFin = new SimpleObjectProperty<>();
    private final StringProperty image = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> updatedAt = new SimpleObjectProperty<>();

    private List<Competition> competitions = new ArrayList<>();

    public Saison() {
        // Default constructor
    }

    // --- ID ---
    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    // --- nomSaison ---
    public String getNomSaison() {
        return nomSaison.get();
    }

    public void setNomSaison(String nomSaison) {
        this.nomSaison.set(nomSaison);
    }

    public StringProperty nomSaisonProperty() {
        return nomSaison;
    }

    // --- descSaison ---
    public String getDescSaison() {
        return descSaison.get();
    }

    public void setDescSaison(String descSaison) {
        this.descSaison.set(descSaison);
    }

    public StringProperty descSaisonProperty() {
        return descSaison;
    }

    // --- dateFin ---
    public LocalDate getDateFin() {
        return dateFin.get();
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin.set(dateFin);
    }

    public ObjectProperty<LocalDate> dateFinProperty() {
        return dateFin;
    }

    // --- image ---
    public String getImage() {
        return image.get();
    }

    public void setImage(String image) {
        this.image.set(image);
        if (image != null && !image.isEmpty()) {
            this.setUpdatedAt(LocalDate.now());
        }
    }

    public StringProperty imageProperty() {
        return image;
    }

    // --- updatedAt ---
    public LocalDate getUpdatedAt() {
        return updatedAt.get();
    }

    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt.set(updatedAt);
    }

    public ObjectProperty<LocalDate> updatedAtProperty() {
        return updatedAt;
    }

    // --- Competitions ---
    public List<Competition> getCompetitions() {
        return competitions;
    }

    public void setCompetitions(List<Competition> competitions) {
        this.competitions = competitions;
    }

    public void addCompetition(Competition competition) {
        if (!this.competitions.contains(competition)) {
            this.competitions.add(competition);
            competition.setSaisonId(this);
        }
    }

    public void removeCompetition(Competition competition) {
        this.competitions.remove(competition);
        if (competition.getSaisonId() == this) {
            competition.setSaisonId(null);
        }
    }
    @Override
    public String toString() {
        return "Saison{" +
                "id=" + id +
                ", nomSaison='" + nomSaison + '\'' +
                ", descSaison='" + descSaison + '\'' +
                ", dateFin=" + dateFin +
                ", image='" + image + '\'' +
                '}';
    }
}
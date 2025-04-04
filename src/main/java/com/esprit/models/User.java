package com.esprit.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.esprit.models.enums.RoleEnum;

import javafx.beans.property.*;

public class User {
    private final IntegerProperty id = new SimpleIntegerProperty(this, "id");
    private final StringProperty prenom = new SimpleStringProperty(this, "prenom");
    private final StringProperty nom = new SimpleStringProperty(this, "nom");
    private final StringProperty email = new SimpleStringProperty(this, "email");
    private final StringProperty password = new SimpleStringProperty(this, "password");
    private final StringProperty tel = new SimpleStringProperty(this, "tel");
    private final StringProperty profilePicture = new SimpleStringProperty(this, "profilePicture");
    private final ObjectProperty<RoleEnum> role = new SimpleObjectProperty<>(this, "role");
    private final StringProperty status = new SimpleStringProperty(this, "status");
    private final BooleanProperty isVerified = new SimpleBooleanProperty(this, "isVerified");
    private final StringProperty confirmationToken = new SimpleStringProperty(this, "confirmationToken");
    private final ObjectProperty<LocalDateTime> confirmationTokenExpiresAt = new SimpleObjectProperty<>(this,
            "confirmationTokenExpiresAt");
    private final ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>(this, "createdAt");
    private final ObjectProperty<LocalDateTime> lastLoginAt = new SimpleObjectProperty<>(this, "lastLoginAt");
    private final IntegerProperty warningCount = new SimpleIntegerProperty(this, "warningCount");

    // Collections
    private List<Sondage> sondages = new ArrayList<>();
    // private List<ParticipationMembre> participations = new ArrayList<>();
    private List<Commentaire> commentaires = new ArrayList<>();
    // private List<Like> likes = new ArrayList<>();
    // private List<ParticipationEvent> participationEvents = new ArrayList<>();
    // private List<Commande> commandes = new ArrayList<>();

    // Constants
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_DISABLED = "disabled";

    // Constructor
    public User() {
        this.createdAt.set(LocalDateTime.now());
        this.warningCount.set(0);
        this.status.set(STATUS_ACTIVE);
        this.isVerified.set(false);
    }

    // Getters and Setters with Property methods
    public int getId() {
        return id.get();
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public String getPrenom() {
        return prenom.get();
    }

    public StringProperty prenomProperty() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom.set(prenom);
    }

    public String getNom() {
        return nom.get();
    }

    public StringProperty nomProperty() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom.set(nom);
    }

    public String getEmail() {
        return email.get();
    }

    public StringProperty emailProperty() {
        return email;
    }

    public void setEmail(String email) {
        this.email.set(email);
    }

    public String getPassword() {
        return password.get();
    }

    public StringProperty passwordProperty() {
        return password;
    }

    public void setPassword(String password) {
        this.password.set(password);
    }

    public String getTel() {
        return tel.get();
    }

    public StringProperty telProperty() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel.set(tel);
    }

    public String getProfilePicture() {
        return profilePicture.get();
    }

    public StringProperty profilePictureProperty() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture.set(profilePicture);
    }

    public RoleEnum getRole() {
        return role.get();
    }

    public ObjectProperty<RoleEnum> roleProperty() {
        return role;
    }

    public void setRole(RoleEnum role) {
        this.role.set(role);
    }

    // Collections getters and setters
    public List<Sondage> getSondages() {
        return sondages;
    }

    public void setSondages(List<Sondage> sondages) {
        this.sondages = sondages;
    }

    // public List<ParticipationMembre> getParticipations() {
    // return participations;
    // }

    // public void setParticipations(List<ParticipationMembre> participations) {
    // this.participations = participations;
    // }

    // Utility methods
    public String getFullName() {
        return getNom() + " " + getPrenom();
    }

    public boolean isAdmin() {
        return getRole() == RoleEnum.ADMINISTRATEUR;
    }

    // public boolean isActive() {
    // return getStatus().equals(STATUS_ACTIVE);
    // }

    public void incrementWarningCount() {
        this.warningCount.set(this.warningCount.get() + 1);
    }

    public boolean hasReachedMaxWarnings(int maxWarnings) {
        return this.warningCount.get() >= maxWarnings;
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
package com.esprit.models;

import jakarta.persistence.*;
import javafx.beans.property.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "club")
public class Club {
    // JavaFX properties for UI binding
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty nom = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty logo = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> dateCreation = new SimpleObjectProperty<>();
    private final ObjectProperty<User> president = new SimpleObjectProperty<>();
    private final StringProperty status = new SimpleStringProperty();
    
    // JPA relationships
    private List<User> membres = new ArrayList<>();
    
    @OneToMany(mappedBy = "club", fetch = FetchType.EAGER)
    private List<Sondage> sondages = new ArrayList<>();

    public Club() {
        this.dateCreation.set(LocalDateTime.now());
    }

    // Getters and Setters with Properties
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getId() {
        return id.get();
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public void setId(int id) {
        this.id.set(id);
    }

    @Column(name = "nom")
    public String getNom() {
        return nom.get();
    }

    public StringProperty nomProperty() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom.set(nom);
    }

    @Column(name = "description")
    public String getDescription() {
        return description.get();
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    @Column(name = "logo")
    public String getLogo() {
        return logo.get();
    }

    public StringProperty logoProperty() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo.set(logo);
    }

    @Column(name = "date_creation")
    public LocalDateTime getDateCreation() {
        return dateCreation.get();
    }

    public ObjectProperty<LocalDateTime> dateCreationProperty() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime date) {
        this.dateCreation.set(date);
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "president_id")
    public User getPresident() {
        return president.get();
    }

    public ObjectProperty<User> presidentProperty() {
        return president;
    }

    public void setPresident(User president) {
        this.president.set(president);
    }

    @Column(name = "status")
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
    @Transient
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
    
    @Override
    public String toString() {
        return this.getNom();
    }
}
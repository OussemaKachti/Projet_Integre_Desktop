package com.esprit.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

@Entity
@Table(name = "club")
public class Club {
    // JPA fields
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;


    @Column(name = "nom")
    private String nom;
    @Column(name = "nom_c")
    private String nomC;
    @Column(name = "nom_c")
    private String nomC;
    @Column(name = "description")
    private String description;


    @Column(name = "logo")
    private String logo;


    @Column(name = "date_creation")
    private LocalDateTime dateCreation;


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "president_id")
    private User president;

    @Column(name = "president_id", insertable = false, updatable = false)
    private int presidentId;

    @Column(name = "status")
    private String status;
    @Column(name = "image")
    private String image;

    @Column(name = "points")
    private int points;

    @Column(name = "image")
    private String image;

    @Column(name = "points")
    private int points;

    // JavaFX properties for UI binding
    @Transient
    private final IntegerProperty idProperty = new SimpleIntegerProperty();


    @Transient
    private final StringProperty nomCProperty = new SimpleStringProperty();

    @Transient
    private final StringProperty descriptionProperty = new SimpleStringProperty();


    @Transient
    private final StringProperty logoProperty = new SimpleStringProperty();


    @Transient
    private final ObjectProperty<LocalDateTime> dateCreationProperty = new SimpleObjectProperty<>();


    @Transient
    private final ObjectProperty<User> presidentProperty = new SimpleObjectProperty<>();


    @Transient
    private final StringProperty statusProperty = new SimpleStringProperty();


    // JPA relationships
    @Transient
    private List<User> membres = new ArrayList<>();


    @OneToMany(mappedBy = "club", fetch = FetchType.EAGER)
    private List<Sondage> sondages = new ArrayList<>();

    public Club() {
        this.dateCreation = LocalDateTime.now();
        this.dateCreationProperty.set(LocalDateTime.now());
    }

    public Club(int id, int presidentId, String nomC, String description, String status, String image, int points) {
        this.id = id;
        this.presidentId = presidentId;
        this.nomC = nomC;
        this.description = description;
        this.status = status;
        this.image = image;
        this.points = points;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
        this.idProperty.set(id);
    }

    // public String getNom() {
    //     return nom;
    // }

    // public void setNom(String nom) {
    //     this.nom = nom;
    //     this.nomCProperty.set(nom);
    // }

    public String getNomC() {
        return nomC;
    }

    public int getPresidentId() {
        return presidentId;
    }

    public void setPresidentId(int presidentId) {
        this.presidentId = presidentId;
    }

    public void setNomC(String nomC) {
        this.nomC = nomC;
        this.nomCProperty.set(nom);

    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.descriptionProperty.set(description);
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
        this.logoProperty.set(logo);
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime date) {
        this.dateCreation = date;
        this.dateCreationProperty.set(date);
    }

    public User getPresident() {
        return president;
    }

    public void setPresident(User president) {
        this.president = president;
        this.presidentProperty.set(president);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.statusProperty.set(status);
    }


    // Property accessors for JavaFX
    public IntegerProperty idProperty() {
        idProperty.set(id);
        return idProperty;
    }

    public StringProperty nomCProperty() {
        nomCProperty.set(nomC);
        return nomCProperty;
    }


    public StringProperty descriptionProperty() {
        descriptionProperty.set(description);
        return descriptionProperty;
    }


    public StringProperty logoProperty() {
        logoProperty.set(logo);
        return logoProperty;
    }


    public ObjectProperty<LocalDateTime> dateCreationProperty() {
        dateCreationProperty.set(dateCreation);
        return dateCreationProperty;
    }


    public ObjectProperty<User> presidentProperty() {
        presidentProperty.set(president);
        return presidentProperty;
    }


    public StringProperty statusProperty() {
        statusProperty.set(status);
        return statusProperty;
    }

    // List management
    public List<User> getMembres() {
        return membres;
    }

    // List management
    public List<User> getMembres() {
        return membres;
    }

    public void setMembres(List<User> membres) {
        this.membres = membres;
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

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void addSondage(Sondage sondage) {
        if (!this.sondages.contains(sondage)) {
            this.sondages.add(sondage);
            sondage.setClub(this);
        }
    }


    @Override
    public String toString() {
        return this.getNomC();
    }
}
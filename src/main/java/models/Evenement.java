package models;

import java.util.Date;

public class Evenement {
    private int id, club_id, categorie_id;
    private String nom_event , type , desc_event , image_description,lieux;
    private Date start_date;
    private Date end_date;


    // Constructeur par défaut
    public Evenement() {}

    // Constructeur avec paramètres
    public Evenement(int id, int club_id, int categorie_id, String nom_event, String type, String desc_event,
                     String image_description, Date start_date, Date end_date, String lieux) {
        this.id = id;
        this.club_id = club_id;
        this.categorie_id = categorie_id;
        this.nom_event = nom_event;
        this.type = type;
        this.desc_event = desc_event;
        this.image_description = image_description;
        this.start_date = start_date;
        this.end_date = end_date;
        this.lieux = lieux;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getClub_id() {
        return club_id;
    }

    public void setClub_id(int club_id) {
        this.club_id = club_id;
    }

    public int getCategorie_id() {
        return categorie_id;
    }

    public void setCategorie_id(int categorie_id) {
        this.categorie_id = categorie_id;
    }

    public String getNom_event() {
        return nom_event;
    }

    public void setNom_event(String nom_event) {
        this.nom_event = nom_event;
    }

    public String getDesc_event() {
        return desc_event;
    }

    public void setDesc_event(String desc_event) {
        this.desc_event = desc_event;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getImage_description() {
        return image_description;
    }

    public void setImage_description(String image_description) {
        this.image_description = image_description;
    }

    public Date getStart_date() {
        return start_date;
    }

    public void setStart_date(Date start_date) {
        this.start_date = start_date;
    }

    public Date getEnd_date() {
        return end_date;
    }

    public void setEnd_date(Date end_date) {
        this.end_date = end_date;
    }

    public String getLieux() {
        return lieux;
    }

    public void setLieux(String lieux) {
        this.lieux = lieux;
    }
}

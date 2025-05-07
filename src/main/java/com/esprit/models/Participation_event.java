package com.esprit.models;

import java.util.Date;

public class Participation_event {
    private Long user_id;
    private Long evenement_id;
    private Date dateparticipation;

    // Constructeur par défaut
    public Participation_event() {
    }

    // Constructeur avec paramètres
    public Participation_event(Long user_id, Long evenement_id, Date dateparticipation) {
        this.user_id = user_id;
        this.evenement_id = evenement_id;
        this.dateparticipation = dateparticipation;
    }

    // Getters et Setters
    public Long getUser_id() {
        return user_id;
    }

    public void setUser_id(Long user_id) {
        this.user_id = user_id;
    }

    public Long getEvenement_id() {
        return evenement_id;
    }

    public void setEvenement_id(Long evenement_id) {
        this.evenement_id = evenement_id;
    }

    public Date getDateparticipation() {
        return dateparticipation;
    }

    public void setDateparticipation(Date dateparticipation) {
        this.dateparticipation = dateparticipation;
    }

    @Override
    public String toString() {
        return "Participation_event{" +
                "user_id=" + user_id +
                ", evenement_id=" + evenement_id +
                ", dateparticipation=" + dateparticipation +
                '}';
    }
}
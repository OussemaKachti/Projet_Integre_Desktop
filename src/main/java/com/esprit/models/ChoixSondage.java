 package com.esprit.models;

import java.util.ArrayList;
import java.util.List;

public class ChoixSondage {
    private Integer id;
    private String contenu;
    private Sondage sondage;
    private List<Reponse> reponses;

    public ChoixSondage() {
        this.reponses = new ArrayList<>();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public Sondage getSondage() {
        return sondage;
    }

    public void setSondage(Sondage sondage) {
        this.sondage = sondage;
    }

    public List<Reponse> getReponses() {
        return reponses;
    }

    public void setReponses(List<Reponse> reponses) {
        this.reponses = reponses;
    }

    public void addReponse(Reponse reponse) {
        if (!this.reponses.contains(reponse)) {
            this.reponses.add(reponse);
            reponse.setChoixSondage(this);
        }
    }

    @Override
    public String toString() {
        return this.contenu;
    }
}
package com.esprit.models;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ChoixSondage {
    private Integer id;
    private String contenu;
    private Sondage sondage;
    private List<Reponse> reponses;
    
    // Constantes pour les validations
    private static final int LONGUEUR_MINIMALE = 2;
    private static final int LONGUEUR_MAXIMALE = 100;
    private static final Pattern PATTERN_CONTENU_VALIDE = Pattern.compile("^\\S.{0,98}\\S$");

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
    
    /**
     * Vérifie si le contenu du choix est trop court
     * @return true si le contenu est trop court, false sinon
     */
    public boolean estTropCourt() {
        return contenu == null || contenu.trim().length() < LONGUEUR_MINIMALE;
    }
    
    /**
     * Vérifie si le contenu du choix est trop long
     * @return true si le contenu est trop long, false sinon
     */
    public boolean estTropLong() {
        return contenu != null && contenu.length() > LONGUEUR_MAXIMALE;
    }
    
    /**
     * Vérifie si le contenu du choix est vide ou contient uniquement des espaces
     * @return true si le contenu est vide, false sinon
     */
    public boolean estVide() {
        return contenu == null || contenu.trim().isEmpty();
    }
    
    /**
     * Vérifie si le contenu du choix est valide (non vide, longueur correcte)
     * @return true si le contenu est invalide, false sinon
     */
    public boolean estContenuInvalide() {
        return estVide() || estTropCourt() || estTropLong() || !PATTERN_CONTENU_VALIDE.matcher(contenu).matches();
    }

    @Override
    public String toString() {
        return this.contenu;
    }
}
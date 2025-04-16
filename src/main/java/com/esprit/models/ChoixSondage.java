package com.esprit.models;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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

@Entity
@Table(name = "choix_sondage")
public class ChoixSondage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "contenu")
    private String contenu;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sondage_id")
    private Sondage sondage;
    
    @OneToMany(mappedBy = "choixSondage", fetch = FetchType.LAZY)
    private List<Reponse> reponses;
    
    // Constantes de validation
    @Transient
    private static final int LONGUEUR_MINIMALE = 2;
    @Transient
    private static final int LONGUEUR_MAXIMALE = 100;
    @Transient
    private static final Pattern PATTERN_CONTENU_VALIDE = Pattern.compile("^[a-zA-Z0-9\\s\\p{Punct}]+$");

    public ChoixSondage() {
        this.reponses = new ArrayList<>();
    }

    public ChoixSondage(String contenu) {
        this();
        this.contenu = contenu;
    }

    // Getters and Setters
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
     * @return true si le contenu est trop court
     */
    public boolean estTropCourt() {
        return contenu == null || contenu.trim().length() < LONGUEUR_MINIMALE;
    }
    
    /**
     * Vérifie si le contenu du choix est trop long
     * @return true si le contenu est trop long
     */
    public boolean estTropLong() {
        return contenu != null && contenu.length() > LONGUEUR_MAXIMALE;
    }
    
    /**
     * Vérifie si le contenu du choix est vide
     * @return true si le contenu est vide
     */
    public boolean estVide() {
        return contenu == null || contenu.trim().isEmpty();
    }
    
    /**
     * Vérifie si le contenu du choix est invalide (caractères non autorisés)
     * @return true si le contenu est invalide
     */
    public boolean estContenuInvalide() {
        return contenu == null || !PATTERN_CONTENU_VALIDE.matcher(contenu).matches();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChoixSondage autre = (ChoixSondage) obj;
        return id != null && id.equals(autre.getId());
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return this.contenu;
    }
}
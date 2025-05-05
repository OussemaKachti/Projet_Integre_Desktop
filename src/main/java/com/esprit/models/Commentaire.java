package com.esprit.models;

import java.time.LocalDate;
import java.util.Arrays;
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
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "commentaire")
public class Commentaire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "contenu_comment")
    private String contenuComment;
    
    @Column(name = "date_comment")
    private LocalDate dateComment;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sondage_id")
    private Sondage sondage;

    // Liste de mots inappropriés à filtrer
    @Transient
    private static final List<String> MOTS_INAPPROPRIES = Arrays.asList(
            "insulte", "grossier", "offensive", "vulgar", "idiot");

    // Regex pour vérifier si le contenu contient au moins un mot
    @Transient
    private static final Pattern PATTERN_CONTENU_VALIDE = Pattern.compile("^.*[a-zA-Z0-9]+.*$");

    public Commentaire() {
        this.dateComment = LocalDate.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContenuComment() {
        return contenuComment;
    }

    public void setContenuComment(String contenuComment) {
        this.contenuComment = contenuComment;
    }

    public LocalDate getDateComment() {
        return dateComment;
    }

    public void setDateComment(LocalDate dateComment) {
        this.dateComment = dateComment;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Sondage getSondage() {
        return sondage;
    }

    public void setSondage(Sondage sondage) {
        this.sondage = sondage;
    }

    /**
     * Vérifie si le commentaire est vide ou trop court
     * 
     * @return true si le commentaire est invalide, false sinon
     */
    public boolean estTropCourt() {
        return contenuComment == null || contenuComment.trim().length() < 2;
    }

    /**
     * Vérifie si le commentaire contient des mots inappropriés
     * 
     * @return true si le commentaire contient des mots inappropriés, false sinon
     */
    public boolean contientMotsInappropries() {
        if (contenuComment == null)
            return false;

        String contenuLowerCase = contenuComment.toLowerCase();
        for (String mot : MOTS_INAPPROPRIES) {
            if (contenuLowerCase.contains(mot.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vérifie si le commentaire a un format valide (contient au moins un caractère
     * alphanumérique)
     * 
     * @return true si le format est valide, false sinon
     */
    public boolean estFormatInvalide() {
        return contenuComment == null || !PATTERN_CONTENU_VALIDE.matcher(contenuComment).matches();
    }

    /**
     * Vérifie si le commentaire contient trop de majuscules (considéré comme crier)
     * 
     * @return true si le commentaire contient plus de 50% de majuscules, false
     *         sinon
     */
    public boolean contientTropDeMajuscules() {
        if (contenuComment == null || contenuComment.trim().isEmpty())
            return false;

        int countUpperCase = 0;
        int countLowerCase = 0;

        for (char c : contenuComment.toCharArray()) {
            if (Character.isUpperCase(c)) {
                countUpperCase++;
            } else if (Character.isLowerCase(c)) {
                countLowerCase++;
            }
        }

        // Si plus de 50% des caractères alphabétiques sont en majuscules
        return countUpperCase + countLowerCase > 0 &&
                (double) countUpperCase / (countUpperCase + countLowerCase) > 0.5;
    }
}
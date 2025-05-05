package com.esprit.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.persistence.CascadeType;
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
@Table(name = "sondage")
public class Sondage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "question")
    private String question;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "club_id")
    private Club club;
    
    @OneToMany(mappedBy = "sondage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ChoixSondage> choix;
    
    @OneToMany(mappedBy = "sondage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Commentaire> commentaires;
    
    @OneToMany(mappedBy = "sondage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Reponse> reponses;
    
    // Regex pour vérifier la longueur minimale de la question
    @Transient
    private static final Pattern PATTERN_QUESTION_VALIDE = Pattern.compile("^.{5,}$");
    // Nombre minimal de choix requis
    @Transient
    private static final int NOMBRE_MINIMAL_CHOIX = 2;

    public Sondage() {
        this.createdAt = LocalDateTime.now();
        this.choix = new ArrayList<>();
        this.commentaires = new ArrayList<>();
        this.reponses = new ArrayList<>();
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Club getClub() {
        return club;
    }

    public void setClub(Club club) {
        this.club = club;
    }

    public List<ChoixSondage> getChoix() {
        return choix;
    }

    public void setChoix(List<ChoixSondage> choix) {
        this.choix = choix;
    }

    public List<Commentaire> getCommentaires() {
        return commentaires;
    }

    public void setCommentaires(List<Commentaire> commentaires) {
        this.commentaires = commentaires;
    }

    public List<Reponse> getReponses() {
        return reponses;
    }

    public void setReponses(List<Reponse> reponses) {
        this.reponses = reponses;
    }

    public void addChoix(ChoixSondage choix) {
        if (!this.choix.contains(choix)) {
            this.choix.add(choix);
            choix.setSondage(this);
        }
    }

    public void addCommentaire(Commentaire commentaire) {
        if (!this.commentaires.contains(commentaire)) {
            this.commentaires.add(commentaire);
            commentaire.setSondage(this);
        }
    }

    public void addReponse(Reponse reponse) {
        if (!this.reponses.contains(reponse)) {
            this.reponses.add(reponse);
            reponse.setSondage(this);
        }
    }
    
    /**
     * Vérifie si la question du sondage est invalide (trop courte)
     * @return true si la question est invalide
     */
    public boolean estQuestionInvalide() {
        return question == null || !PATTERN_QUESTION_VALIDE.matcher(question).matches();
    }
    
    /**
     * Vérifie si la question se termine par un point d'interrogation
     * @return true si la question ne se termine pas par un point d'interrogation
     */
    public boolean questionSansPointInterrogation() {
        return question == null || !question.trim().endsWith("?");
    }
    
    /**
     * Vérifie si le nombre de choix est insuffisant
     * @return true si le nombre de choix est insuffisant
     */
    public boolean nombreChoixInsuffisant() {
        return choix == null || choix.size() < NOMBRE_MINIMAL_CHOIX;
    }
    
    /**
     * Vérifie si des choix sont dupliqués
     * @return true si des choix sont dupliqués
     */
    public boolean contientChoixDupliques() {
        if (choix == null || choix.isEmpty()) {
            return false;
        }
        
        Set<String> contenus = new HashSet<>();
        for (ChoixSondage choixSondage : choix) {
            String contenu = choixSondage.getContenu();
            if (contenu != null) {
                if (contenus.contains(contenu.toLowerCase())) {
                    return true;
                }
                contenus.add(contenu.toLowerCase());
            }
        }
        
        return false;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Sondage other = (Sondage) obj;
        return id != null && id.equals(other.getId());
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "Sondage [id=" + id + ", question=" + question + "]";
    }
}
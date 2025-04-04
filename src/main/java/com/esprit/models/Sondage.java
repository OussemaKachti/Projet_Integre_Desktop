package com.esprit.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Sondage {
    private Integer id;
    private String question;
    private LocalDateTime createdAt;
    private User user;
    private Club club;
    private List<ChoixSondage> choix;
    private List<Commentaire> commentaires;
    private List<Reponse> reponses;

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
}
package com.esprit.models;

import java.time.LocalDateTime;

public class Commentaire {
    private Integer id;
    private String contenuComment;
    private LocalDateTime dateComment;
    private User user;
    private Sondage sondage;

    public Commentaire() {
        this.dateComment = LocalDateTime.now();
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

    public LocalDateTime getDateComment() {
        return dateComment;
    }

    public void setDateComment(LocalDateTime dateComment) {
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
}
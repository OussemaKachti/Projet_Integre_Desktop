package com.esprit.models;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class Reponse {
    private Integer id;
    private LocalDate dateReponse;
    private User user;
    private ChoixSondage choixSondage;
    private Sondage sondage;

    public Reponse() {
        this.dateReponse = LocalDate.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDate getDateReponse() {
        return dateReponse;
    }

    public void setDateReponse(LocalDate dateReponse) {
        this.dateReponse = dateReponse;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ChoixSondage getChoixSondage() {
        return choixSondage;
    }

    public void setChoixSondage(ChoixSondage choixSondage) {
        this.choixSondage = choixSondage;
    }

    public Sondage getSondage() {
        return sondage;
    }

    public void setSondage(Sondage sondage) {
        this.sondage = sondage;
    }

    public Map<String, Object> getPollResults(Sondage sondage) {
        int totalVotes = sondage.getReponses().size();
        Map<String, Object> results = new HashMap<>();

        for (ChoixSondage choix : sondage.getChoix()) {
            long choixVotes = sondage.getReponses().stream()
                    .filter(r -> r.getChoixSondage().equals(choix))
                    .count();

            double percentage = totalVotes > 0 ? (choixVotes * 100.0) / totalVotes : 0;
            String color = getColorByPercentage(percentage);

            results.put("choix", choix.getContenu());
            results.put("percentage", percentage);
            results.put("color", color);
        }

        return results;
    }

    private String getColorByPercentage(double percentage) {
        if (percentage <= 20) return "#e74c3c";
        else if (percentage <= 40) return "#f39c12";
        else if (percentage <= 60) return "#f1c40f";
        else if (percentage <= 80) return "#2ecc71";
        else return "#3498db";
    }
}
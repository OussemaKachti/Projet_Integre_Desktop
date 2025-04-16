package com.esprit.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reponse")
public class Reponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "date_reponse")
    private LocalDateTime dateReponse;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "choix_id")
    private ChoixSondage choixSondage;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sondage_id")
    private Sondage sondage;
    
    public Reponse() {
        this.dateReponse = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getDateReponse() {
        return dateReponse;
    }

    public void setDateReponse(LocalDateTime dateReponse) {
        this.dateReponse = dateReponse;
    }

    public void setDateReponse(LocalDate dateReponse) {
        // Convert LocalDate to LocalDateTime at start of day
        this.dateReponse = dateReponse != null ? dateReponse.atStartOfDay() : LocalDateTime.now();
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
    
    /**
     * Calcule les résultats d'un sondage
     * @param sondage Le sondage pour lequel calculer les résultats
     * @return Une map contenant pour chaque choix: le contenu, le pourcentage de votes et une couleur
     */
    public static Map<String, Object> getPollResults(Sondage sondage) {
        Map<String, Object> results = new HashMap<>();
        List<ChoixSondage> choices = sondage.getChoix();
        List<Reponse> responses = sondage.getReponses();
        
        int totalVotes = responses.size();
        
        for (ChoixSondage choice : choices) {
            // Compter les votes pour ce choix
            long votesForChoice = responses.stream()
                    .filter(r -> r.getChoixSondage().getId().equals(choice.getId()))
                    .count();
            
            // Calculer le pourcentage
            double percentage = totalVotes > 0 ? (double) votesForChoice / totalVotes * 100 : 0;
            
            // Ajouter les informations au résultat
            Map<String, Object> choiceResult = new HashMap<>();
            choiceResult.put("content", choice.getContenu());
            choiceResult.put("percentage", Math.round(percentage));
            choiceResult.put("color", getColorByPercentage(percentage));
            
            results.put(choice.getId().toString(), choiceResult);
        }
        
        return results;
    }
    
    /**
     * Attribue une couleur en fonction du pourcentage de votes
     * @param percentage Le pourcentage de votes
     * @return Un code de couleur
     */
    private static String getColorByPercentage(double percentage) {
        if (percentage >= 70) return "#4CAF50"; // Vert pour popularité élevée
        if (percentage >= 40) return "#FFC107"; // Jaune pour popularité moyenne
        return "#F44336"; // Rouge pour popularité faible
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Reponse other = (Reponse) obj;
        return id != null && id.equals(other.getId());
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "Reponse [id=" + id + ", user=" + (user != null ? user.getFirstName() : "null") + 
               ", choix=" + (choixSondage != null ? choixSondage.getContenu() : "null") + "]";
    }
}
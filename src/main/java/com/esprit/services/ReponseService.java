package com.esprit.services;

import com.esprit.models.Reponse;
import com.esprit.models.ChoixSondage;
import com.esprit.models.Sondage;
import com.esprit.models.User;
import com.esprit.utils.DataSource;
import com.esprit.utils.DatabaseConnection;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReponseService {
    private Connection connection;

    public ReponseService() {
        connection = DataSource.getInstance().getCnx();
    }

    /**
     * Ajoute un vote (méthode déjà existante)
     */
    public void addVote(Reponse reponse) throws SQLException {
        // Vérifier si l'utilisateur a déjà voté
        if (hasUserVoted(reponse.getUser().getId(), reponse.getSondage().getId())) {
            // Supprimer l'ancien vote
            deleteUserVote(reponse.getUser().getId(), reponse.getSondage().getId());
        }

        String query = "INSERT INTO reponse (date_reponse, user_id, choix_sondage_id, sondage_id) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            // Conversion de LocalDate en Timestamp pour la BDD
            LocalDate date = reponse.getDateReponse();
            pst.setTimestamp(1, date != null ? Timestamp.valueOf(date.atStartOfDay()) : Timestamp.valueOf(LocalDate.now().atStartOfDay()));
            
            pst.setInt(2, reponse.getUser().getId());
            pst.setInt(3, reponse.getChoixSondage().getId());
            pst.setInt(4, reponse.getSondage().getId());

            pst.executeUpdate();
        }
    }
    
    /**
     * Ajoute une réponse à un sondage
     */
    public void add(Reponse reponse) throws SQLException {
        String query = "INSERT INTO reponse (date_reponse, user_id, choix_sondage_id, sondage_id) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pst = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            // Conversion de LocalDate en Timestamp pour la BDD
            LocalDate date = reponse.getDateReponse();
            pst.setTimestamp(1, date != null ? Timestamp.valueOf(date.atStartOfDay()) : Timestamp.valueOf(LocalDate.now().atStartOfDay()));
            
            pst.setInt(2, reponse.getUser().getId());
            pst.setInt(3, reponse.getChoixSondage().getId());
            pst.setInt(4, reponse.getSondage().getId());

            pst.executeUpdate();
            
            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                reponse.setId(rs.getInt(1));
            }
        }
    }
    
    /**
     * Met à jour une réponse existante
     */
    public void update(Reponse reponse) throws SQLException {
        String query = "UPDATE reponse SET date_reponse = ?, choix_sondage_id = ? WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            // Conversion de LocalDate en Timestamp pour la BDD
            LocalDate date = reponse.getDateReponse();
            pst.setTimestamp(1, date != null ? Timestamp.valueOf(date.atStartOfDay()) : Timestamp.valueOf(LocalDate.now().atStartOfDay()));
            
            pst.setInt(2, reponse.getChoixSondage().getId());
            pst.setInt(3, reponse.getId());
            
            pst.executeUpdate();
        }
    }
    
    /**
     * Supprime une réponse par son ID
     */
    public void delete(Integer id) throws SQLException {
        String query = "DELETE FROM reponse WHERE id = ?";
        
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    /**
     * Supprime la réponse d'un utilisateur pour un sondage (méthode déjà existante)
     */
    public void deleteUserVote(int userId, int sondageId) throws SQLException {
        String query = "DELETE FROM reponse WHERE user_id = ? AND sondage_id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, sondageId);
            pst.executeUpdate();
        }
    }

    /**
     * Vérifie si un utilisateur a déjà voté pour un sondage (méthode déjà existante)
     */
    public boolean hasUserVoted(int userId, int sondageId) throws SQLException {
        String query = "SELECT COUNT(*) FROM reponse WHERE user_id = ? AND sondage_id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, sondageId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }
    
    /**
     * Récupère la réponse d'un utilisateur pour un sondage
     */
    public Reponse getUserResponseForPoll(int userId, int sondageId) throws SQLException {
        String query = "SELECT * FROM reponse WHERE user_id = ? AND sondage_id = ?";
        
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, sondageId);
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToReponse(rs);
            }
        }
        
        return null;
    }

    /**
     * Récupère les résultats d'un sondage (méthode déjà existante)
     */
    public Map<String, Object> getPollResults(int sondageId) throws SQLException {
        Map<String, Object> results = new HashMap<>();
        String query = """
                SELECT cs.contenu, COUNT(r.id) as votes,
                (COUNT(r.id) * 100.0 / (SELECT COUNT(*) FROM reponse WHERE sondage_id = ?)) as percentage
                FROM choix_sondage cs
                LEFT JOIN reponse r ON cs.id = r.choix_sondage_id
                WHERE cs.sondage_id = ?
                GROUP BY cs.id, cs.contenu
                """;

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, sondageId);
            pst.setInt(2, sondageId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Map<String, Object> choixResult = new HashMap<>();
                double percentage = rs.getDouble("percentage");
                choixResult.put("choix", rs.getString("contenu"));
                choixResult.put("votes", rs.getInt("votes"));
                choixResult.put("percentage", Math.round(percentage * 100.0) / 100.0);
                choixResult.put("color", getColorByPercentage(percentage));

                results.put(rs.getString("contenu"), choixResult);
            }
        }
        return results;
    }

    /**
     * Obtient une couleur en fonction du pourcentage (méthode déjà existante)
     */
    private String getColorByPercentage(double percentage) {
        if (percentage <= 20)
            return "#e74c3c";
        else if (percentage <= 40)
            return "#f39c12";
        else if (percentage <= 60)
            return "#f1c40f";
        else if (percentage <= 80)
            return "#2ecc71";
        else
            return "#3498db";
    }

    /**
     * Récupère toutes les réponses d'un sondage (méthode déjà existante renommée)
     */
    public ObservableList<Reponse> getBySondage(int sondageId) throws SQLException {
        ObservableList<Reponse> reponses = FXCollections.observableArrayList();
        String query = "SELECT * FROM reponse WHERE sondage_id = ? ORDER BY date_reponse DESC";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, sondageId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                reponses.add(mapResultSetToReponse(rs));
            }
        }
        return reponses;
    }
    
    /**
     * Compte le nombre de votes pour un choix spécifique
     * @param choixId ID du choix dont on veut compter les votes
     * @return Le nombre de votes pour ce choix
     */
    public int getVotesByChoix(int choixId) throws SQLException {
        String query = "SELECT COUNT(*) FROM reponse WHERE choix_sondage_id = ?";
        
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, choixId);
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        
        return 0;
    }

    /**
     * Convertit un ResultSet en objet Reponse
     */
    private Reponse mapResultSetToReponse(ResultSet rs) throws SQLException {
        Reponse reponse = new Reponse();
        reponse.setId(rs.getInt("id"));
        
        // Conversion de Timestamp en LocalDate
        Timestamp timestamp = rs.getTimestamp("date_reponse");
        reponse.setDateReponse(timestamp.toLocalDateTime().toLocalDate());

        // Charger les relations
        UserService userService = new UserService();
        ChoixSondageService choixService = new ChoixSondageService();
        SondageService sondageService = new SondageService();

        reponse.setUser(userService.getById(rs.getInt("user_id")));
        reponse.setChoixSondage(choixService.getById(rs.getInt("choix_sondage_id")));
        reponse.setSondage(sondageService.getById(rs.getInt("sondage_id")));

        return reponse;
    }

    /**
     * Get user's response for a specific poll
     */
    public ChoixSondage getUserResponse(int userId, int sondageId) throws SQLException {
        String query = "SELECT cs.* FROM reponse r " +
                       "JOIN choix_sondage cs ON r.choix_sondage_id = cs.id " +
                       "WHERE r.user_id = ? AND r.sondage_id = ?";
        
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, sondageId);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    ChoixSondage choix = new ChoixSondage();
                    choix.setId(rs.getInt("id"));
                    choix.setContenu(rs.getString("contenu"));
                    
                    Sondage sondage = new Sondage();
                    sondage.setId(sondageId);
                    choix.setSondage(sondage);
                    
                    return choix;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Add a new vote
     */
    public void addVote(int userId, int sondageId, int choixId) throws SQLException {
        String query = "INSERT INTO reponse (user_id, sondage_id, choix_sondage_id, date_reponse) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, sondageId);
            pst.setInt(3, choixId);
            pst.setDate(4, Date.valueOf(LocalDate.now()));
            
            pst.executeUpdate();
        }
    }
    
    /**
     * Update a user's existing vote
     */
    public void updateUserVote(int userId, int sondageId, int newChoixId) throws SQLException {
        String query = "UPDATE reponse SET choix_sondage_id = ?, date_reponse = ? WHERE user_id = ? AND sondage_id = ?";
        
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, newChoixId);
            pst.setDate(2, Date.valueOf(LocalDate.now()));
            pst.setInt(3, userId);
            pst.setInt(4, sondageId);
            
            pst.executeUpdate();
        }
    }
    
    /**
     * Get total votes for a poll
     */
    public int getTotalVotesForPoll(int sondageId) throws SQLException {
        String query = "SELECT COUNT(*) FROM reponse WHERE sondage_id = ?";
        
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, sondageId);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        return 0;
    }
}
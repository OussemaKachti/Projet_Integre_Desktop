package com.esprit.services;

import com.esprit.models.Reponse;
import com.esprit.models.ChoixSondage;
import com.esprit.models.Sondage;
import com.esprit.models.User;
import com.esprit.utils.DataSource;
import com.esprit.utils.DatabaseConnection;
import com.esprit.utils.DataSource;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class ReponseService {
    private Connection connection;

    public ReponseService() {
        connection = DataSource.getInstance().getCnx();
    }

    public void addVote(Reponse reponse) throws SQLException {
        // Vérifier si l'utilisateur a déjà voté
        if (hasUserVoted(reponse.getUser().getId(), reponse.getSondage().getId())) {
            // Supprimer l'ancien vote
            deleteUserVote(reponse.getUser().getId(), reponse.getSondage().getId());
        }

        String query = "INSERT INTO reponse (date_reponse, user_id, choix_sondage_id, sondage_id) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setTimestamp(1, Timestamp.valueOf(reponse.getDateReponse()));
            pst.setInt(2, reponse.getUser().getId());
            pst.setInt(3, reponse.getChoixSondage().getId());
            pst.setInt(4, reponse.getSondage().getId());

            pst.executeUpdate();
        }
    }

    public void deleteUserVote(int userId, int sondageId) throws SQLException {
        String query = "DELETE FROM reponse WHERE user_id = ? AND sondage_id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, sondageId);
            pst.executeUpdate();
        }
    }

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

    public ObservableList<Reponse> getReponsesBySondage(int sondageId) throws SQLException {
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

    private Reponse mapResultSetToReponse(ResultSet rs) throws SQLException {
        Reponse reponse = new Reponse();
        reponse.setDateReponse(rs.getTimestamp("date_reponse").toLocalDateTime());

        // Charger les relations
        UserService userService = new UserService();
        ChoixSondageService choixService = new ChoixSondageService();
        SondageService sondageService = new SondageService();

        reponse.setUser(userService.getById(rs.getInt("user_id")));
        reponse.setChoixSondage(choixService.getById(rs.getInt("choix_sondage_id")));
        reponse.setSondage(sondageService.getById(rs.getInt("sondage_id")));

        return reponse;
    }

    public Reponse getUserResponse(int id, Integer id2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUserResponse'");
    }

    public void delete(Integer id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    public void add(Reponse reponse) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'add'");
    }
}
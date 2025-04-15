package com.esprit.services;

import com.esprit.models.ChoixSondage;
import com.esprit.models.Sondage;
import com.esprit.utils.DatabaseConnection;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.esprit.utils.DataSource;

import java.sql.*;

public class ChoixSondageService {
    private Connection connection;

    public ChoixSondageService() {
        connection = DataSource.getInstance().getCnx();
    }

    public void add(ChoixSondage choix) throws SQLException {
        String query = "INSERT INTO choix_sondage (contenu, sondage_id) VALUES (?, ?)";

        try (PreparedStatement pst = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, choix.getContenu());
            pst.setInt(2, choix.getSondage().getId());

            pst.executeUpdate();

            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                choix.setId(rs.getInt(1));
            }
        }
    }

    public void addMultipleChoix(ObservableList<ChoixSondage> choix, int sondageId) throws SQLException {
        String query = "INSERT INTO choix_sondage (contenu, sondage_id) VALUES (?, ?)";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            for (ChoixSondage c : choix) {
                pst.setString(1, c.getContenu());
                pst.setInt(2, sondageId);
                pst.addBatch();
            }
            pst.executeBatch();
        }
    }

    public void update(ChoixSondage choix) throws SQLException {
        String query = "UPDATE choix_sondage SET contenu = ? WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, choix.getContenu());
            pst.setInt(2, choix.getId());
            pst.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        // Vérifier d'abord s'il y a des réponses associées
        if (hasResponses(id)) {
            throw new SQLException("Cannot delete choice: It has associated responses");
        }

        String query = "DELETE FROM choix_sondage WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    public ChoixSondage getById(int id) throws SQLException {
        String query = "SELECT * FROM choix_sondage WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return mapResultSetToChoixSondage(rs);
            }
        }
        return null;
    }

    public ObservableList<ChoixSondage> getBySondage(int sondageId) throws SQLException {
        ObservableList<ChoixSondage> choix = FXCollections.observableArrayList();
        String query = "SELECT * FROM choix_sondage WHERE sondage_id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, sondageId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                choix.add(mapResultSetToChoixSondage(rs));
            }
        }
        return choix;
    }

    private boolean hasResponses(int choixId) throws SQLException {
        String query = "SELECT COUNT(*) FROM reponse WHERE choix_sondage_id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, choixId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private ChoixSondage mapResultSetToChoixSondage(ResultSet rs) throws SQLException {
        ChoixSondage choix = new ChoixSondage();
        choix.setId(rs.getInt("id"));
        choix.setContenu(rs.getString("contenu"));

        // Charger le sondage associé
        SondageService sondageService = new SondageService();
        choix.setSondage(sondageService.getById(rs.getInt("sondage_id")));

        return choix;
    }

    // Méthodes utilitaires
    public int getResponseCount(int choixId) throws SQLException {
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

    public double getResponsePercentage(int choixId, int totalResponses) throws SQLException {
        if (totalResponses == 0)
            return 0.0;

        int responses = getResponseCount(choixId);
        return (responses * 100.0) / totalResponses;
    }
}
package com.esprit.services;

import com.esprit.models.Sondage;
import com.esprit.utils.DatabaseConnection;
import com.esprit.models.ChoixSondage;
import com.esprit.utils.DataSource;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SondageService {
    private Connection connection;
    private static SondageService instance;

    public SondageService() {
        connection = DataSource.getInstance().getCnx();
    }

    public static SondageService getInstance() {
        if (instance == null) {
            instance = new SondageService();
        }
        return instance;
    }

    public void add(Sondage sondage) throws SQLException {
        String query = "INSERT INTO sondage (question, created_at, user_id, club_id) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pst = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, sondage.getQuestion());
            pst.setTimestamp(2, Timestamp.valueOf(sondage.getCreatedAt()));
            pst.setInt(3, sondage.getUser().getId());
            pst.setInt(4, sondage.getClub().getId());

            pst.executeUpdate();

            // Récupérer l'ID généré
            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                sondage.setId(rs.getInt(1));

                // Ajouter les choix
                for (ChoixSondage choix : sondage.getChoix()) {
                    addChoix(choix, sondage.getId());
                }
            }
        }
    }

    public void update(Sondage sondage) throws SQLException {
        String query = "UPDATE sondage SET question = ? WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, sondage.getQuestion());
            pst.setInt(2, sondage.getId());
            pst.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String query = "DELETE FROM sondage WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    public Sondage getById(int id) throws SQLException {
        String query = "SELECT * FROM sondage WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return mapResultSetToSondage(rs);
            }
        }
        return null;
    }

    public ObservableList<Sondage> getAll() throws SQLException {
        ObservableList<Sondage> sondages = FXCollections.observableArrayList();
        String query = "SELECT * FROM sondage ORDER BY created_at DESC";

        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                sondages.add(mapResultSetToSondage(rs));
            }
        }
        return sondages;
    }

    private Sondage mapResultSetToSondage(ResultSet rs) throws SQLException {
        Sondage sondage = new Sondage();
        sondage.setId(rs.getInt("id"));
        sondage.setQuestion(rs.getString("question"));
        sondage.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        // Charger l'utilisateur et le club
        UserService userService = new UserService();
        ClubService clubService = new ClubService();
        sondage.setUser(userService.getById(rs.getInt("user_id")));
        sondage.setClub(clubService.getById(rs.getInt("club_id")));

        // Charger les choix
        loadChoix(sondage);

        return sondage;
    }

    private void loadChoix(Sondage sondage) throws SQLException {
        String query = "SELECT * FROM choix_sondage WHERE sondage_id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, sondage.getId());
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                ChoixSondage choix = new ChoixSondage();
                choix.setId(rs.getInt("id"));
                choix.setContenu(rs.getString("contenu"));
                choix.setSondage(sondage);
                sondage.getChoix().add(choix);
            }
        }
    }

    private void addChoix(ChoixSondage choix, int sondageId) throws SQLException {
        String query = "INSERT INTO choix_sondage (contenu, sondage_id) VALUES (?, ?)";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, choix.getContenu());
            pst.setInt(2, sondageId);
            pst.executeUpdate();
        }
    }

    /**
     * Récupère tous les sondages d'un club spécifique
     */
    public List<Sondage> getByClub(int clubId) throws SQLException {
        List<Sondage> sondages = new ArrayList<>();
        String query = "SELECT * FROM sondage WHERE club_id = ? ORDER BY created_at DESC";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, clubId);
            
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    sondages.add(mapResultSetToSondage(rs));
                }
            }
        }
        
        return sondages;
    }
}
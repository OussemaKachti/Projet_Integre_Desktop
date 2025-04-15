package com.esprit.services;

import com.esprit.models.Saison;
import com.esprit.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SaisonService {
    private Connection connection;

    public SaisonService() {
        connection = DatabaseConnection.getInstance().getCnx();
    }

    public List<Saison> getAll() throws SQLException {
        List<Saison> saisons = new ArrayList<>();
        String query = "SELECT * FROM saison";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Saison saison = mapResultSetToSaison(rs);
                saisons.add(saison);
            }
        }

        return saisons;
    }

    public Saison getById(int id) throws SQLException {
        String query = "SELECT * FROM saison WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSaison(rs);
                }
            }
        }

        return null;
    }

    public void add(Saison saison) throws SQLException {
        String query = "INSERT INTO saison (nom_saison, desc_saison, date_fin, image) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, saison.getNomSaison());
            pstmt.setString(2, saison.getDescSaison());
            pstmt.setDate(3, Date.valueOf(saison.getDateFin()));
            pstmt.setString(4, saison.getImage());

            pstmt.executeUpdate();

            // Get the generated ID
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    saison.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public void update(Saison saison) throws SQLException {
        String query = "UPDATE saison SET nom_saison = ?, desc_saison = ?, date_fin = ?, image = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, saison.getNomSaison());
            pstmt.setString(2, saison.getDescSaison());
            pstmt.setDate(3, Date.valueOf(saison.getDateFin()));
            pstmt.setString(4, saison.getImage());
            pstmt.setInt(5, saison.getId());

            pstmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String query = "DELETE FROM saison WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    private Saison mapResultSetToSaison(ResultSet rs) throws SQLException {
        Saison saison = new Saison();
        saison.setId(rs.getInt("id"));
        saison.setNomSaison(rs.getString("nom_saison"));
        saison.setDescSaison(rs.getString("desc_saison"));
        saison.setDateFin(rs.getDate("date_fin").toLocalDate());
        saison.setImage(rs.getString("image"));
        return saison;
    }
}
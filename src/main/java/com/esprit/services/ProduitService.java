package com.esprit.services;

import com.esprit.models.Club;
import com.esprit.models.Produit;
import com.esprit.utils.DataSource;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProduitService {

    private Connection conn;
    private static ProduitService instance;

    // Private constructor to prevent direct instantiation
    private ProduitService() {
        this.conn = DataSource.getInstance().getCnx(); // Assuming DataSource is a class that manages the DB connection
    }

    // Singleton instance method
    public static ProduitService getInstance() {
        if (instance == null) {
            instance = new ProduitService();
        }
        return instance;
    }


    public void insertProduit(Produit produit) throws SQLException {
        String sql = "INSERT INTO produit (nom_prod, desc_prod, prix, img_prod, created_at, quantity, club_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, produit.getNomProd());
            stmt.setString(2, produit.getDescProd());
            stmt.setFloat(3, produit.getPrix());
            stmt.setString(4, produit.getImgProd());
            stmt.setTimestamp(5, Timestamp.valueOf(produit.getCreatedAt()));
            stmt.setString(6, produit.getQuantity());
            stmt.setInt(7, produit.getClub() != null ? produit.getClub().getId() : null);

            stmt.executeUpdate();

            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                produit.setId(generatedKeys.getInt(1));
            }
        }
    }

    public List<Produit> selectAllProduits() throws SQLException {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT * FROM produit";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Produit produit = new Produit();
                produit.setId(rs.getInt("id"));
                produit.setNomProd(rs.getString("nom_prod"));
                produit.setDescProd(rs.getString("desc_prod"));
                produit.setPrix(rs.getFloat("prix"));
                produit.setImgProd(rs.getString("img_prod"));
                produit.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                produit.setQuantity(rs.getString("quantity"));

                int clubId = rs.getInt("club_id");
                if (!rs.wasNull()) {
                    Club club = new Club();
                    club.setId(clubId);
                    produit.setClub(club);
                }

                produits.add(produit);
            }
        }
        return produits;
    }
    public List<Produit> getAll() throws SQLException {
        return selectAllProduits(); // Calls the existing method to get all products
    }

    public void updateProduit(Produit produit) throws SQLException {
        String sql = "UPDATE produit SET nom_prod = ?, desc_prod = ?, prix = ?, img_prod = ?, created_at = ?, quantity = ?, club_id = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, produit.getNomProd());
            stmt.setString(2, produit.getDescProd());
            stmt.setFloat(3, produit.getPrix());
            stmt.setString(4, produit.getImgProd());
            stmt.setTimestamp(5, Timestamp.valueOf(produit.getCreatedAt()));
            stmt.setString(6, produit.getQuantity());
            stmt.setInt(7, produit.getClub() != null ? produit.getClub().getId() : null);
            stmt.setInt(8, produit.getId());
            stmt.executeUpdate();
        }
    }

    public void deleteProduit(int id) throws SQLException {
        String sql = "DELETE FROM produit WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public Produit getProduitById(int id) throws SQLException {
        String sql = "SELECT * FROM produit WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Produit produit = new Produit();
                produit.setId(rs.getInt("id"));
                produit.setNomProd(rs.getString("nom_prod"));
                produit.setDescProd(rs.getString("desc_prod"));
                produit.setPrix(rs.getFloat("prix"));
                produit.setImgProd(rs.getString("img_prod"));
                produit.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                produit.setQuantity(rs.getString("quantity"));

                int clubId = rs.getInt("club_id");
                if (!rs.wasNull()) {
                    Club club = new Club();
                    club.setId(clubId);
                    produit.setClub(club);
                }
                return produit;
            }
        }
        return null;
    }

    public List<Produit> getProduitsByClub(int clubId) throws SQLException {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT * FROM produit WHERE club_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clubId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Produit produit = new Produit();
                produit.setId(rs.getInt("id"));
                produit.setNomProd(rs.getString("nom_prod"));
                produit.setDescProd(rs.getString("desc_prod"));
                produit.setPrix(rs.getFloat("prix"));
                produit.setImgProd(rs.getString("img_prod"));
                produit.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                produit.setQuantity(rs.getString("quantity"));

                Club club = new Club();
                club.setId(clubId);
                produit.setClub(club);

                produits.add(produit);
            }
        }
        return produits;
    }
}

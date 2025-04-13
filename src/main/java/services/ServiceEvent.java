package services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.Evenement;
import utils.DataSource;

public class ServiceEvent {
    private Connection conn;

    public ServiceEvent() {
        conn = DataSource.getInstance().getCnx();
    }

    public static void ajouter(Evenement e) throws SQLException {
        Connection conn = DataSource.getInstance().getCnx();
        String query = "INSERT INTO evenement (nom_event, type, desc_event, image_description, lieux, club_id, categorie_id, start_date, end_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, e.getNom_event());

            // Vérifier et gérer 'type' pour ne pas insérer de null
            String type = e.getType();
            if (type == null || type.isEmpty()) {
                type = "default_type";  // Fournir une valeur par défaut si 'type' est null
            }
            pst.setString(2, type);  // Insérer le 'type'

            pst.setString(3, e.getDesc_event());
            pst.setString(4, e.getImage_description());
            pst.setString(5, e.getLieux());
            pst.setInt(6, e.getClub_id());
            pst.setInt(7, e.getCategorie_id());
            pst.setDate(8, new java.sql.Date(e.getStart_date().getTime()));
            pst.setDate(9, new java.sql.Date(e.getEnd_date().getTime()));

            pst.executeUpdate();
        }
    }


    public ObservableList<String> getAllCategoriesNames() {
        ObservableList<String> categories = FXCollections.observableArrayList();

        try {
            String query = "SELECT nom_cat FROM categorie";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);

            while (rs.next()) {
                categories.add(rs.getString("nom_cat"));
            }
        } catch (SQLException ex) {
            System.err.println("Erreur lors de la récupération des catégories: " + ex.getMessage());
        }

        return categories;
    }

    public ObservableList<String> getAllClubsNames() {
        ObservableList<String> clubs = FXCollections.observableArrayList();

        try {
            String query = "SELECT nom_c FROM club";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);

            while (rs.next()) {
                clubs.add(rs.getString("nom_c"));
            }
        } catch (SQLException ex) {
            System.err.println("Erreur lors de la récupération des clubs: " + ex.getMessage());
        }

        return clubs;
    }

    public int getClubIdByName(String clubName) {
        int clubId = -1;

        try {
            String query = "SELECT id FROM club WHERE nom_c = ?";
            PreparedStatement pst = conn.prepareStatement(query);
            pst.setString(1, clubName);

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                clubId = rs.getInt("id");
            }
        } catch (SQLException ex) {
            System.err.println("Erreur lors de la récupération de l'ID du club: " + ex.getMessage());
        }

        return clubId;
    }

    public int getCategorieIdByName(String categorieName) {
        int categorieId = -1;

        try {
            String query = "SELECT id FROM categorie WHERE nom_cat = ?";
            PreparedStatement pst = conn.prepareStatement(query);
            pst.setString(1, categorieName);

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                categorieId = rs.getInt("id");
            }
        } catch (SQLException ex) {
            System.err.println("Erreur lors de la récupération de l'ID de la catégorie: " + ex.getMessage());
        }

        return categorieId;
    }
    public String getClubNameById(int clubId) {
        String clubName = null;
        try {
            String query = "SELECT nom_c FROM club WHERE id = ?";
            PreparedStatement pst = conn.prepareStatement(query);
            pst.setInt(1, clubId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                clubName = rs.getString("nom_c");
            }
        } catch (SQLException ex) {
            System.err.println("Erreur lors de la récupération du nom du club: " + ex.getMessage());
        }
        return clubName;
    }

    public String getCategoryNameById(int categoryId) {
        String categoryName = null;
        try {
            String query = "SELECT nom_cat FROM categorie WHERE id = ?";
            PreparedStatement pst = conn.prepareStatement(query);
            pst.setInt(1, categoryId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                categoryName = rs.getString("nom_cat");
            }
        } catch (SQLException ex) {
            System.err.println("Erreur lors de la récupération du nom de la catégorie: " + ex.getMessage());
        }
        return categoryName;
    }
}
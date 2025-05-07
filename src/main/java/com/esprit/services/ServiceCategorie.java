package com.esprit.services;

import com.esprit.models.Categorie;
import com.esprit.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServiceCategorie implements IService<Categorie> {

    private Connection connection;

    public ServiceCategorie() {
        connection = DataSource.getInstance().getCnx(); // Initialisation de la connexion
    }

    @Override
    public void ajouter(Categorie categorie) throws SQLException {
        String req = "INSERT INTO categorie (nom_cat) VALUES (?)";
        PreparedStatement preparedStatement = connection.prepareStatement(req);
        preparedStatement.setString(1, categorie.getNom_cat()); // Ajout du nom de la catégorie
        preparedStatement.executeUpdate();
        System.out.println("Catégorie ajoutée");
    }

    @Override
    public void modifier(Categorie categorie) throws SQLException {
        // Exemple de modification (à adapter si nécessaire)
        String req = "UPDATE categorie SET nom_cat = ? WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(req);
        preparedStatement.setString(1, categorie.getNom_cat());
        preparedStatement.setInt(2, categorie.getId());
        preparedStatement.executeUpdate();
        System.out.println("Catégorie modifiée");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM categorie WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(req);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();
        System.out.println("Catégorie supprimée");
    }

    @Override
    public List<Categorie> afficher() throws SQLException {
        List<Categorie> categories = new ArrayList<>();
        String req = "SELECT * FROM categorie";
        PreparedStatement preparedStatement = connection.prepareStatement(req);
        ResultSet rs = preparedStatement.executeQuery();

        while (rs.next()) {
            int id = rs.getInt("id");
            String nom = rs.getString("nom_cat");
            Categorie categorie = new Categorie(id, nom);
            categories.add(categorie);
        }

        return categories;
    }

    // Méthode pour obtenir les statistiques d'utilisation des catégories
    public List<CategoryUsage> getCategoriesUsageStats() throws SQLException {
        List<CategoryUsage> stats = new ArrayList<>();

        // Cette requête compte combien d'événements sont associés à chaque catégorie
        String req = "SELECT c.id, c.nom_cat, COUNT(e.id) as count " +
                "FROM categorie c " +
                "LEFT JOIN evenement e ON c.id = e.categorie_id " + // Changement de table club à evenement
                "GROUP BY c.id, c.nom_cat " +
                "ORDER BY count DESC";

        PreparedStatement preparedStatement = connection.prepareStatement(req);
        ResultSet rs = preparedStatement.executeQuery();

        while (rs.next()) {
            int id = rs.getInt("id");
            String nom = rs.getString("nom_cat");
            int count = rs.getInt("count");
            stats.add(new CategoryUsage(id, nom, count));
        }

        return stats;
    }

    // Classe pour stocker les données d'utilisation des catégories
    public static class CategoryUsage {
        private int id;
        private String name;
        private int count;

        public CategoryUsage(int id, String name, int count) {
            this.id = id;
            this.name = name;
            this.count = count;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getCount() {
            return count;
        }
    }
}
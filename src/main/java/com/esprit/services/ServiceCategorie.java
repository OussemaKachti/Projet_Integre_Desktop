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
        connection = DataSource.getInstance().getCnx(); // Correct method from DataSource
    }

    @Override
    public void add(Categorie categorie) throws SQLException {
        String req = "INSERT INTO categorie (nom_cat) VALUES (?)";
        PreparedStatement preparedStatement = connection.prepareStatement(req);
        preparedStatement.setString(1, categorie.getNom_cat()); // Ajout du nom de la catégorie
        preparedStatement.executeUpdate();
        System.out.println("Catégorie ajoutée");
    }

    @Override
    public void update(Categorie categorie) throws SQLException {
        // Exemple de modification (à adapter si nécessaire)
        String req = "UPDATE categorie SET nom_cat = ? WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(req);
        preparedStatement.setString(1, categorie.getNom_cat());
        preparedStatement.setInt(2, categorie.getId());
        preparedStatement.executeUpdate();
        System.out.println("Catégorie modifiée");
    }

    @Override
    public void delete(int id) throws SQLException {
        String req = "DELETE FROM categorie WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(req);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();
        System.out.println("Catégorie supprimée");
    }

    @Override
    public List<Categorie> getAll() throws SQLException {
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
    
    @Override
    public Categorie findById(int id) throws SQLException {
        String req = "SELECT * FROM categorie WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(req);
        preparedStatement.setInt(1, id);
        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            String nom = rs.getString("nom_cat");
            return new Categorie(id, nom);
        }
        
        return null;
    }
}
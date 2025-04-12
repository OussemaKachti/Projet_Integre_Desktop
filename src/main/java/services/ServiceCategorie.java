package services;

import models.Categorie;
import utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
        // Exemple de méthode d'affichage (à compléter si nécessaire)
        // Retourne une liste des catégories
        return null;
    }
}

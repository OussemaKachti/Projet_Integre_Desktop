package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import models.Categorie;
import services.ServiceCategorie;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class AjouterCategorie implements Initializable {

    @FXML
    private TextField nomcattf;

    @FXML
    private Button ajoutercat;

    @FXML
    private TableView<Categorie> categoriesTableView;

    @FXML
    private TableColumn<Categorie, Integer> idColumn;

    @FXML
    private TableColumn<Categorie, String> nameColumn;

    private ServiceCategorie serviceCategorie;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize the service
        serviceCategorie = new ServiceCategorie();

        // Configure table columns
        setupTableColumns();

        // Load categories from database
        loadCategories();
    }

    private void setupTableColumns() {
        // Associate table columns with Categorie properties
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("nom_cat"));

        // Set table properties
        categoriesTableView.setPlaceholder(new javafx.scene.control.Label("Aucune catégorie disponible"));

        // Optional: Add row highlighting on hover and selection styles
        categoriesTableView.getStyleClass().add("table-view");
    }

    // Méthode pour charger les catégories depuis la base de données
    private void loadCategories() {
        try {
            // Get categories from database
            List<Categorie> categories = serviceCategorie.afficher();

            // Convert to ObservableList for TableView
            ObservableList<Categorie> observableCategories = FXCollections.observableArrayList(categories);

            // Set items to TableView
            categoriesTableView.setItems(observableCategories);

        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les catégories: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
    }

    // Cette méthode sera appelée lorsque l'utilisateur clique sur "Back"
    @FXML
    void handleBack(ActionEvent event) {
        try {
            // Charger la page précédente
            Parent root = FXMLLoader.load(getClass().getResource("/views/ListeEvents.fxml"));
            Scene scene = new Scene(root);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible de charger la page précédente.", AlertType.ERROR);
            e.printStackTrace();
        }
    }

    // Cette méthode sera appelée lorsqu'on clique sur le bouton ajouter
    @FXML
    void insererCategorie(ActionEvent event) {
        String nomcattfText = nomcattf.getText().trim(); // Supprimer les espaces inutiles

        // Vérification si le champ nom_cat est vide
        if (nomcattfText.isEmpty()) {
            showAlert("Erreur", "Le nom de la catégorie ne peut pas être vide.", AlertType.ERROR);
            return;
        }

        try {
            // Création de l'objet Categorie
            Categorie categorie = new Categorie(nomcattfText);

            // Appel au service pour ajouter la catégorie
            serviceCategorie.ajouter(categorie);

            // Notification de succès
            showAlert("Succès", "La catégorie a été ajoutée avec succès.", AlertType.INFORMATION);

            // Réinitialisation du champ après ajout
            nomcattf.clear();

            // Recharger la liste pour afficher la nouvelle catégorie
            loadCategories();

        } catch (SQLException e) {
            // Affichage de l'erreur en cas d'échec
            showAlert("Erreur", "Une erreur est survenue lors de l'ajout de la catégorie: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
    }

    // Méthode pour afficher les alertes
    private void showAlert(String title, String message, AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
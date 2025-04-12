package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Categorie;
import services.ServiceCategorie;

import java.io.IOException;
import java.sql.SQLException;

public class AjouterCategorie {

    @FXML
    private TextField nomcattf;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private Button ajoutercat;

    // Cette méthode sera appelée lorsque l'utilisateur clique sur "Back"
    @FXML
    void handleBack(ActionEvent event) {
        try {
            // Charger la page précédente (remplacez "PreviousPage.fxml" par le nom de votre page précédente)
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

    // Cette méthode sera appelée lorsque l'utilisateur clique sur "Cancel"
    @FXML
    void handleCancel(ActionEvent event) {
        // Réinitialiser les champs
        nomcattf.clear();
        if (descriptionArea != null) {
            descriptionArea.clear();
        }

        // Alternative: retourner à la page précédente
        handleBack(event);
    }

    // Cette méthode sera appelée lorsqu'on clique sur le bouton ajouter
    @FXML
    void insererCategorie(ActionEvent event) {
        String nomcattfText = nomcattf.getText().trim(); // Supprimer les espaces inutiles

        // Récupérer la description (si le champ existe)
        String description = "";
        if (descriptionArea != null) {
            description = descriptionArea.getText().trim();
        }

        // Vérification si le champ nom_cat est vide
        if (nomcattfText.isEmpty()) {
            showAlert("Erreur", "Le nom de la catégorie ne peut pas être vide.", AlertType.ERROR);
            return;
        }

        ServiceCategorie sc = new ServiceCategorie();
        try {
            // Création de l'objet Categorie
            // Si votre classe Categorie possède un constructeur avec description, utilisez-le
            Categorie categorie = new Categorie(nomcattfText);

            // Si vous avez un setter pour la description et que vous souhaitez l'utiliser
            // categorie.setDescription(description);

            // Appel au service pour ajouter la catégorie
            sc.ajouter(categorie);

            // Notification de succès
            showAlert("Succès", "La catégorie a été ajoutée avec succès.", AlertType.INFORMATION);

            // Réinitialisation des champs après ajout
            nomcattf.clear();
            if (descriptionArea != null) {
                descriptionArea.clear();
            }

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
        alert.setHeaderText(null); // On peut définir un en-tête si nécessaire
        alert.setContentText(message);
        alert.showAndWait();
    }
}
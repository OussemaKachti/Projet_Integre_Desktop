package com.esprit.controllers.crud;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ProduitFormController {
    @FXML private TextField txtNom;
    @FXML private TextField txtPrix;
    @FXML private TextField txtQuantity;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtImage;
    @FXML private ComboBox<String> comboClub;
    @FXML private Button btnSave;
    @FXML private Button btnUpdate;
    @FXML private Button btnCancel;

    @FXML
    public void initialize() {
        // Initialisation si nécessaire
    }

    @FXML
    private void saveProduit() {
        // Ajouter produit
    }

    @FXML
    private void updateProduit() {
        // Mettre à jour produit
    }

    @FXML
    private void resetForm() {
        txtNom.clear();
        txtPrix.clear();
        txtQuantity.clear();
        txtDescription.clear();
        txtImage.clear();
        comboClub.getSelectionModel().clearSelection();
    }

    @FXML
    private void browseImage() {
        // Gestion du fichier image
    }
}

package com.esprit.controllers.crud;

import com.esprit.models.Produit;
import com.esprit.services.ProduitService;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.beans.property.SimpleStringProperty;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.Optional;

/**
 * Contrôleur pour la gestion des produits (CRUD)
 */
public class ProduitController implements Initializable {

    // Composants de l'interface utilisateur
    @FXML private TableView<Produit> tableProduits;
    @FXML private TableColumn<Produit, String> colNom;
    @FXML private TableColumn<Produit, String> colPrix;
    @FXML private TableColumn<Produit, String> colDescription;
    @FXML private TableColumn<Produit, String> colActions;

    @FXML private TextField txtNom;
    @FXML private TextField txtPrix;
    @FXML private TextField txtDescription;
    @FXML private Button btnSave;
    @FXML private Button btnUpdate;
    @FXML private Button btnCancel;

    // Services
    private final ProduitService produitService;

    // État du contrôleur
    private Produit currentProduit;
    private boolean editMode = false;

    /**
     * Constructeur
     */
    public ProduitController() {
        this.produitService = ProduitService.getInstance();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupButtons();
        loadProduits();
    }

    /**
     * Configure les colonnes du tableau
     */
    private void setupTable() {
        colNom.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getNomProd()));
        colPrix.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getPrix())));
        colDescription.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDescProd()));

        setupActionsColumn();
    }

    /**
     * Configure la colonne des actions
     */
    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<Produit, String>() {
            private final Button editButton = new Button("Modifier");
            private final Button deleteButton = new Button("Supprimer");

            {
                editButton.setOnAction(e -> {
                    Produit produit = getTableView().getItems().get(getIndex());
                    editProduit(produit);
                });

                deleteButton.setOnAction(e -> {
                    Produit produit = getTableView().getItems().get(getIndex());
                    deleteProduit(produit);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, editButton, deleteButton);
                    buttons.setPadding(new Insets(0, 0, 0, 5));
                    setGraphic(buttons);
                }
            }
        });
    }

    /**
     * Configure les événements des boutons
     */
    private void setupButtons() {
        btnSave.setOnAction(e -> saveProduit());
        btnUpdate.setOnAction(e -> updateProduit());
        btnCancel.setOnAction(e -> resetForm());

        // Masquer le bouton de mise à jour initialement
        btnUpdate.setVisible(false);
    }

    /**
     * Charge tous les produits dans le tableau
     */
    private void loadProduits() {
        try {
            // Conversion explicite en ObservableList
            ObservableList<Produit> produits = FXCollections.observableArrayList(produitService.getAll());
            tableProduits.setItems(produits);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du chargement des produits", e.getMessage());
        }
    }

    /**
     * Sauvegarde un produit
     */
    @FXML
    private void saveProduit() {
        try {
            // Validation
            if (txtNom.getText().isEmpty() || txtPrix.getText().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Nom et prix requis", "Veuillez saisir un nom et un prix.");
                return;
            }

            // Créer un produit
            Produit produit = new Produit();
            produit.setNomProd(txtNom.getText());
            produit.setPrix((float) Double.parseDouble(txtPrix.getText()));
            produit.setDescProd(txtDescription.getText());

            // Sauvegarder le produit
            produitService.insertProduit(produit);

            // Réinitialiser le formulaire et recharger les données
            resetForm();
            loadProduits();

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Produit créé", "Le produit a été créé avec succès.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la création du produit", e.getMessage());
        }
    }

    /**
     * Met à jour un produit existant
     */
    @FXML
    private void updateProduit() {
        if (currentProduit == null) {
            return;
        }

        try {
            // Validation
            if (txtNom.getText().isEmpty() || txtPrix.getText().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Nom et prix requis", "Veuillez saisir un nom et un prix.");
                return;
            }

            // Mettre à jour le produit
            currentProduit.setNomProd(txtNom.getText());
            currentProduit.setPrix((float) Double.parseDouble(txtPrix.getText()));
            currentProduit.setDescProd(txtDescription.getText());

            // Sauvegarder les changements
            produitService.updateProduit(currentProduit);

            // Réinitialiser le formulaire et recharger les données
            resetForm();
            loadProduits();

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Produit modifié", "Le produit a été modifié avec succès.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification du produit", e.getMessage());
        }
    }

    /**
     * Prépare le formulaire pour modifier un produit existant
     */
    private void editProduit(Produit produit) {
        currentProduit = produit;
        txtNom.setText(produit.getNomProd());
        txtPrix.setText(String.valueOf(produit.getPrix()));
        txtDescription.setText(produit.getDescProd());

        // Passer en mode édition
        editMode = true;
        btnSave.setVisible(false);
        btnUpdate.setVisible(true);
    }

    /**
     * Supprime un produit
     */
    private void deleteProduit(Produit produit) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmation de suppression");
        confirmDialog.setHeaderText("Supprimer le produit");
        confirmDialog.setContentText("Êtes-vous sûr de vouloir supprimer ce produit ?");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                produitService.deleteProduit(produit.getId());
                loadProduits();

                showAlert(Alert.AlertType.INFORMATION, "Succès", "Produit supprimé", "Le produit a été supprimé avec succès.");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression", e.getMessage());
            }
        }
    }

    /**
     * Réinitialise le formulaire
     */
    private void resetForm() {
        txtNom.clear();
        txtPrix.clear();
        txtDescription.clear();

        // Réinitialiser l'état
        currentProduit = null;
        editMode = false;

        // Réinitialiser les boutons
        btnSave.setVisible(true);
        btnUpdate.setVisible(false);
    }

    /**
     * Affiche une boîte de dialogue
     */
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

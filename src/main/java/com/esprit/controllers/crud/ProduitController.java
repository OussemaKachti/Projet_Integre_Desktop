package com.esprit.controllers.crud;

import com.esprit.ProduitApp;
import com.esprit.models.Club;
import com.esprit.models.Produit;
import com.esprit.services.ClubService;
import com.esprit.services.ProduitService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.beans.property.SimpleStringProperty;

import javafx.event.ActionEvent;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.Optional;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Contrôleur pour la gestion des produits (CRUD)
 */
public class ProduitController implements Initializable {

    // Composants de l'interface utilisateur - AJOUT DES NOUVEAUX CHAMPS
    @FXML private TableView<Produit> tableProduits;
    @FXML private TableColumn<Produit, String> colNom;
    @FXML private TableColumn<Produit, String> colPrix;
    @FXML private TableColumn<Produit, String> colDescription;
    @FXML private TableColumn<Produit, String> colQuantity;
    @FXML private TableColumn<Produit, String> colImage;
    @FXML private TableColumn<Produit, String> colClub;
    @FXML private TableColumn<Produit, String> colActions;

    @FXML private TextField txtNom;
    @FXML private TextField txtPrix;
    @FXML private TextField txtQuantity;
    @FXML private TextField txtImage;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<Club> comboClub;
    @FXML private Button btnSave;
    @FXML private Button btnUpdate;
    @FXML private Button btnCancel;
    @FXML private Button btnBrowse;

    // Services
    private final ProduitService produitService;
    private ClubService clubService; // Ajout du service pour les clubs

    // État du contrôleur
    private Produit currentProduit;
    private boolean editMode = false;

    /**
     * Constructeur
     */
    public ProduitController() {
        this.produitService = ProduitService.getInstance();
        try {
            this.clubService = ClubService.getInstance(); // Initialisez votre service de club ici
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation du service de club: " + e.getMessage());
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupButtons();
        loadProduits();
        loadClubs(); // Charger les clubs dans le ComboBox
    }

    /**
     * Charge les clubs dans le ComboBox
     */
    private void loadClubs() {
        try {
            if (clubService != null) {
                ObservableList<Club> clubs = FXCollections.observableArrayList(clubService.getAll());
                comboClub.setItems(clubs);

                // Définissez comment les clubs seront affichés dans le ComboBox
                comboClub.setCellFactory(cell -> new ListCell<Club>() {
                    @Override
                    protected void updateItem(Club club, boolean empty) {
                        super.updateItem(club, empty);
                        if (empty || club == null) {
                            setText(null);
                        } else {
                            setText(club.getNomC()); // Utilisez la propriété appropriée du club
                        }
                    }
                });

                comboClub.setButtonCell(new ListCell<Club>() {
                    @Override
                    protected void updateItem(Club club, boolean empty) {
                        super.updateItem(club, empty);
                        if (empty || club == null) {
                            setText(null);
                        } else {
                            setText(club.getNomC()); // Utilisez la propriété appropriée du club
                        }
                    }
                });
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du chargement des clubs", e.getMessage());
        }
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

        // Ajout des nouvelles colonnes
        colQuantity.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getQuantity()));
        colImage.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getImgProd()));
        colClub.setCellValueFactory(cellData -> {
            Club club = cellData.getValue().getClub();
            return new SimpleStringProperty(club != null ? club.getNomC() : "");
        });

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

            try {
                produit.setPrix(Float.parseFloat(txtPrix.getText()));
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Format de prix invalide", "Veuillez saisir un nombre valide pour le prix.");
                return;
            }

            produit.setDescProd(txtDescription.getText());

            // Ajouter les nouveaux champs
            produit.setQuantity(txtQuantity.getText());

            // Gérer l'image sélectionnée
            if (txtImage.getText() != null && !txtImage.getText().isEmpty()) {
                String imagePath = txtImage.getText();
                String fileName = new File(imagePath).getName();
                String targetDirectory = "src/main/resources/images/";
                File targetDir = new File(targetDirectory);

                if (!targetDir.exists()) {
                    targetDir.mkdirs();
                }

                File sourceFile = new File(imagePath);
                File targetFile = new File(targetDir, fileName);

                try {
                    Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    produit.setImgProd("images/" + fileName);
                } catch (IOException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la copie de l'image", e.getMessage());
                    return;
                }
            }

            // Ajouter le club sélectionné
            Club selectedClub = comboClub.getValue();
            if (selectedClub != null) {
                produit.setClub(selectedClub);
            }

            // Sauvegarder le produit
            produitService.insertProduit(produit);

            // Réinitialiser le formulaire et recharger les données
            resetForm();
            loadProduits();

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Produit créé", "Le produit a été créé avec succès.");
        } catch (SQLException e) {
            e.printStackTrace(); // Pour voir l'erreur dans la console
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

            try {
                currentProduit.setPrix(Float.parseFloat(txtPrix.getText()));
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Format de prix invalide", "Veuillez saisir un nombre valide pour le prix.");
                return;
            }

            currentProduit.setDescProd(txtDescription.getText());
            currentProduit.setQuantity(txtQuantity.getText());

            // Gérer l'image sélectionnée
            if (txtImage.getText() != null && !txtImage.getText().isEmpty()) {
                String imagePath = txtImage.getText();
                String fileName = new File(imagePath).getName();
                String targetDirectory = "src/main/resources/images/";
                File targetDir = new File(targetDirectory);

                if (!targetDir.exists()) {
                    targetDir.mkdirs();
                }

                File sourceFile = new File(imagePath);
                File targetFile = new File(targetDir, fileName);

                try {
                    Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    currentProduit.setImgProd("images/" + fileName);
                } catch (IOException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la copie de l'image", e.getMessage());
                    return;
                }
            }

            // Mettre à jour le club
            Club selectedClub = comboClub.getValue();
            if (selectedClub != null) {
                currentProduit.setClub(selectedClub);
            }

            // Sauvegarder les changements
            produitService.updateProduit(currentProduit);

            // Réinitialiser le formulaire et recharger les données
            resetForm();
            loadProduits();

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Produit modifié", "Le produit a été modifié avec succès.");
        } catch (SQLException e) {
            e.printStackTrace(); // Pour voir l'erreur dans la console
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
        txtQuantity.setText(produit.getQuantity());
        txtImage.setText(produit.getImgProd());

        // Sélectionner le club du produit
        if (produit.getClub() != null) {
            for (Club club : comboClub.getItems()) {
                if (club.getId() == produit.getClub().getId()) {
                    comboClub.setValue(club);
                    break;
                }
            }
        }

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
                e.printStackTrace(); // Pour voir l'erreur dans la console
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression", e.getMessage());
            }
        }
    }

    /**
     * Réinitialise le formulaire
     */
    @FXML
    public void resetForm() {
        txtNom.clear();
        txtPrix.clear();
        txtDescription.clear();
        txtQuantity.clear();
        txtImage.clear();
        comboClub.setValue(null);

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

    @FXML
    private void goToCommande(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/produit/Commandeview.fxml"));
            Parent root = loader.load();

            // Remplace la scène actuelle
            if (ProduitApp.getPrimaryStage() != null) {
                ProduitApp.getPrimaryStage().getScene().setRoot(root);
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation impossible",
                        "Impossible d'accéder à la fenêtre principale de l'application.");
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de navigation",
                    "Impossible de charger la vue des commandes: " + e.getMessage());
        }
    }

    @FXML
    private void browseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(txtImage.getScene().getWindow());
        if (selectedFile != null) {
            txtImage.setText(selectedFile.getAbsolutePath());
        }
    }
    /**
     * Ouvre la vue d'affichage des produits en cartes
     */

    @FXML
    private void showProductGrid(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/produit/Produit_card.fxml"));
            Parent root = loader.load();

            // Remplace la scène actuelle
            if (ProduitApp.getPrimaryStage() != null) {
                ProduitApp.getPrimaryStage().getScene().setRoot(root);
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation impossible",
                        "Impossible d'accéder à la fenêtre principale de l'application.");
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de navigation",
                    "Impossible de charger la vue grille des produits : " + e.getMessage());
        }
    }
    /**
     * Valide les champs du formulaire produit
     * @return true si tout est valide, false sinon
     */
    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (txtNom.getText().isEmpty()) {
            errors.append("- Le nom du produit est requis.\n");
        }

        if (txtPrix.getText().isEmpty()) {
            errors.append("- Le prix est requis.\n");
        } else {
            try {
                float prix = Float.parseFloat(txtPrix.getText());
                if (prix < 0) {
                    errors.append("- Le prix ne peut pas être négatif.\n");
                }
            } catch (NumberFormatException e) {
                errors.append("- Le prix doit être un nombre valide.\n");
            }
        }

        if (txtDescription.getText().isEmpty()) {
            errors.append("- La description est requise.\n");
        }

        if (txtQuantity.getText().isEmpty()) {
            errors.append("- La quantité est requise.\n");
        } else {
            try {
                int qte = Integer.parseInt(txtQuantity.getText());
                if (qte < 0) {
                    errors.append("- La quantité ne peut pas être négative.\n");
                }
            } catch (NumberFormatException e) {
                errors.append("- La quantité doit être un entier.\n");
            }
        }

        if (comboClub.getValue() == null) {
            errors.append("- Un club doit être sélectionné.\n");
        }

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation", "Veuillez corriger les erreurs suivantes :", errors.toString());
            return false;
        }

        return true;
    }



}
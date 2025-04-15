package com.esprit.controllers.crud;

import com.esprit.models.Sondage;
import com.esprit.models.ChoixSondage;
import com.esprit.models.Club;
import com.esprit.models.User;
import com.esprit.services.SondageService;
import com.esprit.services.ChoixSondageService;
import com.esprit.services.UserService;
import com.esprit.services.ClubService;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.beans.property.SimpleStringProperty;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

/**
 * Contrôleur pour la gestion des sondages (CRUD)
 */
public class SondageController implements Initializable {
    
    // Composants de l'interface utilisateur
    @FXML private TableView<Sondage> tableSondages;
    @FXML private TableColumn<Sondage, String> colQuestion;
    @FXML private TableColumn<Sondage, String> colDate;
    @FXML private TableColumn<Sondage, String> colClub;
    @FXML private TableColumn<Sondage, String> colActions;
    
    @FXML private TextField txtQuestion;
    @FXML private VBox choicesContainer;
    @FXML private Button btnAddChoice;
    @FXML private Button btnSave;
    @FXML private Button btnUpdate;
    @FXML private Button btnCancel;
    
    // Services
    private final SondageService sondageService;
    private final ChoixSondageService choixService;
    private final UserService userService;
    private final ClubService clubService;
    
    // État du contrôleur
    private Sondage currentSondage;
    private List<TextField> choiceFields = new ArrayList<>();
    private boolean editMode = false;
    
    /**
     * Constructeur
     */
    public SondageController() {
        this.sondageService = SondageService.getInstance();
        this.choixService = new ChoixSondageService();
        this.userService = new UserService();
        this.clubService = new ClubService();
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupButtons();
        loadSondages();
        
        // Ajouter deux champs de choix initiaux
        addChoiceField();
        addChoiceField();
    }
    
    /**
     * Configure les colonnes du tableau
     */
    private void setupTable() {
        colQuestion.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getQuestion()));
            
        colDate.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCreatedAt().toString()));
            
        colClub.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getClub() != null ? 
                cellData.getValue().getClub().getNom() : ""));
                
        setupActionsColumn();
    }
    
    /**
     * Configure la colonne des actions
     */
    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<Sondage, String>() {
            private final Button editButton = new Button("Modifier");
            private final Button deleteButton = new Button("Supprimer");
            private final Button viewButton = new Button("Voir");
            
            {
                editButton.setOnAction(e -> {
                    Sondage sondage = getTableView().getItems().get(getIndex());
                    editSondage(sondage);
                });
                
                deleteButton.setOnAction(e -> {
                    Sondage sondage = getTableView().getItems().get(getIndex());
                    deleteSondage(sondage);
                });
                
                viewButton.setOnAction(e -> {
                    Sondage sondage = getTableView().getItems().get(getIndex());
                    viewSondageDetails(sondage);
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, viewButton, editButton, deleteButton);
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
        btnAddChoice.setOnAction(e -> addChoiceField());
        btnSave.setOnAction(e -> saveSondage());
        btnUpdate.setOnAction(e -> updateSondage());
        btnCancel.setOnAction(e -> resetForm());
        
        // Masquer le bouton de mise à jour initialement
        btnUpdate.setVisible(false);
    }
    
    /**
     * Charge tous les sondages dans le tableau
     */
    private void loadSondages() {
        try {
            ObservableList<Sondage> sondages = sondageService.getAll();
            tableSondages.setItems(sondages);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors du chargement des sondages", e.getMessage());
        }
    }
    
    /**
     * Ajoute un champ de saisie pour un choix
     */
    @FXML
    private void addChoiceField() {
        TextField choiceField = new TextField();
        choiceField.setPromptText("Option " + (choiceFields.size() + 1));
        
        Button removeButton = new Button("X");
        removeButton.setOnAction(e -> removeChoiceField(choiceField));
        
        HBox choiceBox = new HBox(5, choiceField, removeButton);
        choiceBox.setPadding(new Insets(5, 0, 0, 0));
        
        choiceFields.add(choiceField);
        choicesContainer.getChildren().add(choiceBox);
    }
    
    /**
     * Supprime un champ de saisie pour un choix
     */
    private void removeChoiceField(TextField field) {
        int index = choiceFields.indexOf(field);
        if (index != -1) {
            choiceFields.remove(index);
            choicesContainer.getChildren().remove(index);
        }
    }
    
    /**
     * Crée ou met à jour un sondage
     */
    @FXML
    private void saveSondage() {
        try {
            // Validation
            if (txtQuestion.getText().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", 
                         "Question requise", "Veuillez saisir une question.");
                return;
            }
            
            boolean hasValidChoice = false;
            for (TextField field : choiceFields) {
                if (!field.getText().isEmpty()) {
                    hasValidChoice = true;
                    break;
                }
            }
            
            if (!hasValidChoice) {
                showAlert(Alert.AlertType.ERROR, "Erreur", 
                         "Choix requis", "Veuillez ajouter au moins un choix.");
                return;
            }
            
            // Récupérer l'utilisateur statique avec ID=2
            User user = userService.getById(2);
            if (user == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", 
                         "Utilisateur non trouvé", "L'utilisateur avec ID=1 n'existe pas.");
                return;
            }
            
            // Récupérer le club où l'utilisateur est président
            Club club = clubService.findByPresident(user.getId());
            if (club == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", 
                         "Club non trouvé", "L'utilisateur n'est pas président d'un club.");
                return;
            }
            
            // Créer le sondage
            Sondage sondage = new Sondage();
            sondage.setQuestion(txtQuestion.getText());
            sondage.setCreatedAt(LocalDateTime.now());
            sondage.setUser(user);
            sondage.setClub(club);
            
            // Ajouter les choix
            for (TextField field : choiceFields) {
                if (!field.getText().isEmpty()) {
                    ChoixSondage choix = new ChoixSondage();
                    choix.setContenu(field.getText());
                    sondage.addChoix(choix);
                }
            }
            
            // Sauvegarder le sondage
            sondageService.add(sondage);
            
            // Réinitialiser le formulaire et recharger les données
            resetForm();
            loadSondages();
            
            showAlert(Alert.AlertType.INFORMATION, "Succès", 
                     "Sondage créé", "Le sondage a été créé avec succès.");
                     
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors de la création du sondage", e.getMessage());
        }
    }
    
    /**
     * Met à jour un sondage existant
     */
    @FXML
    private void updateSondage() {
        if (currentSondage == null) {
            return;
        }
        
        try {
            // Validation
            if (txtQuestion.getText().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", 
                         "Question requise", "Veuillez saisir une question.");
                return;
            }
            
            // Mettre à jour le sondage
            currentSondage.setQuestion(txtQuestion.getText());
            
            // Sauvegarder les changements
            sondageService.update(currentSondage);
            
            // Réinitialiser le formulaire et recharger les données
            resetForm();
            loadSondages();
            
            showAlert(Alert.AlertType.INFORMATION, "Succès", 
                     "Sondage modifié", "Le sondage a été modifié avec succès.");
                     
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors de la modification du sondage", e.getMessage());
        }
    }
    
    /**
     * Prépare le formulaire pour modifier un sondage existant
     */
    private void editSondage(Sondage sondage) {
        currentSondage = sondage;
        txtQuestion.setText(sondage.getQuestion());
        
        // Passer en mode édition
        editMode = true;
        btnSave.setVisible(false);
        btnUpdate.setVisible(true);
        
        // Désactiver l'édition des choix pour un sondage existant
        choicesContainer.setDisable(true);
        btnAddChoice.setDisable(true);
    }
    
    /**
     * Supprime un sondage
     */
    private void deleteSondage(Sondage sondage) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmation de suppression");
        confirmDialog.setHeaderText("Supprimer le sondage");
        confirmDialog.setContentText("Êtes-vous sûr de vouloir supprimer ce sondage ?");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                sondageService.delete(sondage.getId());
                loadSondages();
                
                showAlert(Alert.AlertType.INFORMATION, "Succès", 
                         "Sondage supprimé", "Le sondage a été supprimé avec succès.");
                         
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", 
                         "Erreur lors de la suppression", e.getMessage());
            }
        }
    }
    
    /**
     * Affiche les détails d'un sondage
     */
    private void viewSondageDetails(Sondage sondage) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails du sondage");
        dialog.setHeaderText(sondage.getQuestion());
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        // Informations sur le sondage
        content.getChildren().add(new Label("Date de création: " + sondage.getCreatedAt()));
        content.getChildren().add(new Label("Club: " + 
            (sondage.getClub() != null ? sondage.getClub().getNom() : "N/A")));
        content.getChildren().add(new Label("Créé par: " + 
            (sondage.getUser() != null ? sondage.getUser().getLastName() + " " + 
                                       sondage.getUser().getFirstName() : "N/A")));
        
        content.getChildren().add(new Separator());
        content.getChildren().add(new Label("Choix:"));
        
        // Liste des choix
        for (ChoixSondage choix : sondage.getChoix()) {
            content.getChildren().add(new Label("• " + choix.getContenu()));
        }
        
        // Bouton de fermeture
        ButtonType closeButton = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);
        
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }
    
    /**
     * Réinitialise le formulaire
     */
    private void resetForm() {
        txtQuestion.clear();
        choiceFields.clear();
        choicesContainer.getChildren().clear();
        
        // Ajouter deux champs de choix initiaux
        addChoiceField();
        addChoiceField();
        
        // Réinitialiser l'état
        currentSondage = null;
        editMode = false;
        
        // Réinitialiser les boutons
        btnSave.setVisible(true);
        btnUpdate.setVisible(false);
        
        // Réactiver les choix
        choicesContainer.setDisable(false);
        btnAddChoice.setDisable(false);
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
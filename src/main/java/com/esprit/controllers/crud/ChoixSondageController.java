package com.esprit.controllers.crud;

import com.esprit.models.ChoixSondage;
import com.esprit.models.Sondage;
import com.esprit.services.ChoixSondageService;
import com.esprit.services.SondageService;
import com.esprit.services.ReponseService;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.Optional;

/**
 * Contrôleur pour la gestion des choix de sondage (CRUD)
 */
public class ChoixSondageController implements Initializable {
    
    // Composants de l'interface utilisateur
    @FXML private ComboBox<Sondage> cbSondages;
    @FXML private TableView<ChoixSondage> tableChoix;
    @FXML private TableColumn<ChoixSondage, String> colContenu;
    @FXML private TableColumn<ChoixSondage, Integer> colReponses;
    @FXML private TableColumn<ChoixSondage, String> colActions;
    
    @FXML private TextField txtContenu;
    @FXML private Button btnSave;
    @FXML private Button btnUpdate;
    @FXML private Button btnCancel;
    
    // Services
    private final ChoixSondageService choixService;
    private final SondageService sondageService;
    private final ReponseService reponseService;
    
    // État du contrôleur
    private ChoixSondage currentChoix;
    private boolean editMode = false;
    
    /**
     * Constructeur
     */
    public ChoixSondageController() {
        this.choixService = new ChoixSondageService();
        this.sondageService = SondageService.getInstance();
        this.reponseService = new ReponseService();
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupSondageComboBox();
        setupTable();
        setupButtons();
    }
    
    /**
     * Configure la liste déroulante des sondages
     */
    private void setupSondageComboBox() {
        try {
            ObservableList<Sondage> sondages = sondageService.getAll();
            cbSondages.setItems(sondages);
            
            // Configurer l'affichage des éléments de la liste
            cbSondages.setCellFactory(lv -> new ListCell<Sondage>() {
                @Override
                protected void updateItem(Sondage sondage, boolean empty) {
                    super.updateItem(sondage, empty);
                    if (empty || sondage == null) {
                        setText(null);
                    } else {
                        setText(sondage.getQuestion());
                    }
                }
            });
            
            // Configurer l'affichage de l'élément sélectionné
            cbSondages.setButtonCell(new ListCell<Sondage>() {
                @Override
                protected void updateItem(Sondage sondage, boolean empty) {
                    super.updateItem(sondage, empty);
                    if (empty || sondage == null) {
                        setText("Sélectionner un sondage");
                    } else {
                        setText(sondage.getQuestion());
                    }
                }
            });
            
            // Gérer le changement de sélection
            cbSondages.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    loadChoixBySondage(newVal);
                } else {
                    tableChoix.getItems().clear();
                }
            });
            
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors du chargement des sondages", e.getMessage());
        }
    }
    
    /**
     * Configure les colonnes du tableau
     */
    private void setupTable() {
        colContenu.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getContenu()));
            
        // Colonne pour afficher le nombre de réponses
        colReponses.setCellValueFactory(cellData -> {
            try {
                int count = choixService.getResponseCount(cellData.getValue().getId());
                return javafx.beans.property.SimpleIntegerProperty.valueOf(count).asObject();
            } catch (SQLException e) {
                return javafx.beans.property.SimpleIntegerProperty.valueOf(0).asObject();
            }
        });
        
        setupActionsColumn();
    }
    
    /**
     * Configure la colonne des actions
     */
    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<ChoixSondage, String>() {
            private final Button editButton = new Button("Modifier");
            private final Button deleteButton = new Button("Supprimer");
            
            {
                editButton.setOnAction(e -> {
                    ChoixSondage choix = getTableView().getItems().get(getIndex());
                    editChoix(choix);
                });
                
                deleteButton.setOnAction(e -> {
                    ChoixSondage choix = getTableView().getItems().get(getIndex());
                    deleteChoix(choix);
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty) {
                    setGraphic(null);
                } else {
                    try {
                        ChoixSondage choix = getTableView().getItems().get(getIndex());
                        HBox buttons = new HBox(5);
                        
                        // Vérifier si le choix a des réponses
                        boolean hasResponses = choixService.getResponseCount(choix.getId()) > 0;
                        
                        buttons.getChildren().add(editButton);
                        
                        // Désactiver le bouton de suppression si le choix a des réponses
                        deleteButton.setDisable(hasResponses);
                        buttons.getChildren().add(deleteButton);
                        
                        buttons.setPadding(new Insets(0, 0, 0, 5));
                        setGraphic(buttons);
                        
                    } catch (SQLException | IndexOutOfBoundsException e) {
                        setGraphic(null);
                    }
                }
            }
        });
    }
    
    /**
     * Configure les événements des boutons
     */
    private void setupButtons() {
        btnSave.setOnAction(e -> saveChoix());
        btnUpdate.setOnAction(e -> updateChoix());
        btnCancel.setOnAction(e -> resetForm());
        
        // Masquer le bouton de mise à jour initialement
        btnUpdate.setVisible(false);
    }
    
    /**
     * Charge les choix d'un sondage
     */
    private void loadChoixBySondage(Sondage sondage) {
        try {
            ObservableList<ChoixSondage> choix = choixService.getBySondage(sondage.getId());
            tableChoix.setItems(choix);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors du chargement des choix", e.getMessage());
        }
    }
    
    /**
     * Crée un nouveau choix
     */
    @FXML
    private void saveChoix() {
        Sondage selectedSondage = cbSondages.getSelectionModel().getSelectedItem();
        
        if (selectedSondage == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Sondage requis", "Veuillez sélectionner un sondage.");
            return;
        }
        
        if (txtContenu.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Contenu requis", "Veuillez saisir le contenu du choix.");
            return;
        }
        
        try {
            ChoixSondage choix = new ChoixSondage();
            choix.setContenu(txtContenu.getText());
            choix.setSondage(selectedSondage);
            
            choixService.add(choix);
            
            resetForm();
            loadChoixBySondage(selectedSondage);
            
            showAlert(Alert.AlertType.INFORMATION, "Succès", 
                     "Choix créé", "Le choix a été créé avec succès.");
                     
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors de la création du choix", e.getMessage());
        }
    }
    
    /**
     * Met à jour un choix existant
     */
    @FXML
    private void updateChoix() {
        if (currentChoix == null) {
            return;
        }
        
        if (txtContenu.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Contenu requis", "Veuillez saisir le contenu du choix.");
            return;
        }
        
        try {
            currentChoix.setContenu(txtContenu.getText());
            
            choixService.update(currentChoix);
            
            resetForm();
            loadChoixBySondage(currentChoix.getSondage());
            
            showAlert(Alert.AlertType.INFORMATION, "Succès", 
                     "Choix modifié", "Le choix a été modifié avec succès.");
                     
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors de la modification du choix", e.getMessage());
        }
    }
    
    /**
     * Prépare le formulaire pour modifier un choix existant
     */
    private void editChoix(ChoixSondage choix) {
        currentChoix = choix;
        txtContenu.setText(choix.getContenu());
        
        // Passer en mode édition
        editMode = true;
        btnSave.setVisible(false);
        btnUpdate.setVisible(true);
        
        // Désactiver la sélection du sondage
        cbSondages.setDisable(true);
    }
    
    /**
     * Supprime un choix
     */
    private void deleteChoix(ChoixSondage choix) {
        try {
            // Vérifier si le choix a des réponses
            int responseCount = choixService.getResponseCount(choix.getId());
            if (responseCount > 0) {
                showAlert(Alert.AlertType.ERROR, "Erreur", 
                         "Impossible de supprimer", "Ce choix a déjà des réponses (" + 
                         responseCount + ").");
                return;
            }
            
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Confirmation de suppression");
            confirmDialog.setHeaderText("Supprimer le choix");
            confirmDialog.setContentText("Êtes-vous sûr de vouloir supprimer ce choix ?");
            
            Optional<ButtonType> result = confirmDialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                choixService.delete(choix.getId());
                
                loadChoixBySondage(choix.getSondage());
                
                showAlert(Alert.AlertType.INFORMATION, "Succès", 
                         "Choix supprimé", "Le choix a été supprimé avec succès.");
            }
            
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors de la suppression", e.getMessage());
        }
    }
    
    /**
     * Réinitialise le formulaire
     */
    private void resetForm() {
        txtContenu.clear();
        
        // Réinitialiser l'état
        currentChoix = null;
        editMode = false;
        
        // Réinitialiser les boutons
        btnSave.setVisible(true);
        btnUpdate.setVisible(false);
        
        // Réactiver la sélection du sondage
        cbSondages.setDisable(false);
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
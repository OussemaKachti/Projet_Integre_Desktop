package com.esprit.controllers.crud;

import com.esprit.models.Reponse;
import com.esprit.models.Sondage;
import com.esprit.models.ChoixSondage;
import com.esprit.models.User;
import com.esprit.services.ReponseService;
import com.esprit.services.SondageService;
import com.esprit.services.ChoixSondageService;
import com.esprit.services.UserService;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.Map;
import java.util.Optional;

/**
 * Contrôleur pour la gestion des réponses aux sondages
 */
public class ReponseController implements Initializable {
    
    // Composants de l'interface utilisateur
    @FXML private ComboBox<Sondage> cbSondages;
    @FXML private VBox choicesContainer;
    @FXML private Button btnVote;
    @FXML private Button btnCancel;
    
    @FXML private TableView<Reponse> tableReponses;
    @FXML private TableColumn<Reponse, String> colUser;
    @FXML private TableColumn<Reponse, String> colChoix;
    @FXML private TableColumn<Reponse, String> colDate;
    @FXML private TableColumn<Reponse, String> colActions;
    
    @FXML private PieChart chartResults;
    @FXML private BarChart<String, Number> barChartResults;
    
    // Services
    private final ReponseService reponseService;
    private final SondageService sondageService;
    private final ChoixSondageService choixService;
    private final UserService userService;
    
    // État du contrôleur
    private ToggleGroup choixToggleGroup;
    private Sondage currentSondage;
    
    /**
     * Constructeur
     */
    public ReponseController() {
        this.reponseService = new ReponseService();
        this.sondageService = SondageService.getInstance();
        this.choixService = new ChoixSondageService();
        this.userService = new UserService();
        this.choixToggleGroup = new ToggleGroup();
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
                    loadSondageData(newVal);
                } else {
                    clearSondageData();
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
        colUser.setCellValueFactory(cellData -> {
            User user = cellData.getValue().getUser();
            return new SimpleStringProperty(user != null ? 
                user.getLastName() + " " + user.getFirstName() : "Anonyme");
        });
        
        colChoix.setCellValueFactory(cellData -> {
            ChoixSondage choix = cellData.getValue().getChoixSondage();
            return new SimpleStringProperty(choix != null ? choix.getContenu() : "");
        });
        
        colDate.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDateReponse()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        
        setupActionsColumn();
    }
    
    /**
     * Configure la colonne des actions
     */
    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<Reponse, String>() {
            private final Button deleteButton = new Button("Supprimer");
            
            {
                deleteButton.setOnAction(e -> {
                    Reponse reponse = getTableView().getItems().get(getIndex());
                    deleteReponse(reponse);
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty) {
                    setGraphic(null);
                } else {
                    // Vérifier si l'utilisateur courant est l'auteur de la réponse ou un admin
                    Reponse reponse = getTableView().getItems().get(getIndex());
                    User currentUser = null;
                    currentUser = userService.getById(1);
                    
                    boolean canDelete = (reponse.getUser() != null && 
                                      reponse.getUser().getId() == currentUser.getId());
                    
                    deleteButton.setDisable(!canDelete);
                    
                    HBox buttons = new HBox(5, deleteButton);
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
        btnVote.setOnAction(e -> saveReponse());
        btnCancel.setOnAction(e -> resetForm());
    }
    
    /**
     * Charge les données d'un sondage
     */
    private void loadSondageData(Sondage sondage) {
        currentSondage = sondage;
        
        // Charger les choix du sondage
        loadChoix(sondage);
        
        // Charger les réponses du sondage
        loadReponses(sondage);
        
        // Afficher les résultats
        updateCharts(sondage);
    }
    
    /**
     * Charge les choix d'un sondage
     */
    private void loadChoix(Sondage sondage) {
        choicesContainer.getChildren().clear();
        choixToggleGroup = new ToggleGroup();
        
        try {
            ObservableList<ChoixSondage> choix = choixService.getBySondage(sondage.getId());
            
            for (ChoixSondage c : choix) {
                RadioButton rb = new RadioButton(c.getContenu());
                rb.setToggleGroup(choixToggleGroup);
                rb.setUserData(c);
                choicesContainer.getChildren().add(rb);
            }
            
            // Vérifier si l'utilisateur a déjà voté
            try {
                User currentUser = userService.getById(1); // Utilisateur statique ID=1
                Reponse existingReponse = reponseService.getUserResponseForPoll(currentUser.getId(), sondage.getId());
                
                if (existingReponse != null) {
                    // L'utilisateur a déjà voté, sélectionner son choix
                    for (int i = 0; i < choicesContainer.getChildren().size(); i++) {
                        if (choicesContainer.getChildren().get(i) instanceof RadioButton) {
                            RadioButton rb = (RadioButton) choicesContainer.getChildren().get(i);
                            ChoixSondage choixData = (ChoixSondage) rb.getUserData();
                            
                            if (choixData.getId().equals(existingReponse.getChoixSondage().getId())) {
                                rb.setSelected(true);
                                break;
                            }
                        }
                    }
                    
                    btnVote.setText("Modifier mon vote");
                } else {
                    btnVote.setText("Voter");
                }
            } catch (SQLException e) {
                // Ignorer l'erreur et continuer
                e.printStackTrace();
                btnVote.setText("Voter");
            }
            
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors du chargement des choix", e.getMessage());
        }
    }
    
    /**
     * Charge les réponses d'un sondage
     */
    private void loadReponses(Sondage sondage) {
        try {
            ObservableList<Reponse> reponses = reponseService.getBySondage(sondage.getId());
            tableReponses.setItems(reponses);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors du chargement des réponses", e.getMessage());
        }
    }
    
    /**
     * Met à jour les graphiques avec les résultats du sondage
     */
    private void updateCharts(Sondage sondage) {
        try {
            // Récupérer les résultats du sondage
            Map<String, Object> results = reponseService.getPollResults(sondage.getId());
            
            // Mettre à jour le graphique en camembert
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
            for (ChoixSondage choix : sondage.getChoix()) {
                int count = 0;
                try {
                    count = choixService.getResponseCount(choix.getId());
                } catch (SQLException e) {
                    // Ignorer l'erreur et continuer
                }
                
                if (count > 0) {
                    pieChartData.add(new PieChart.Data(choix.getContenu(), count));
                }
            }
            
            chartResults.setData(pieChartData);
            chartResults.setTitle("Résultats du sondage");
            
            // Mettre à jour le graphique à barres
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Réponses");
            
            for (ChoixSondage choix : sondage.getChoix()) {
                int count = 0;
                try {
                    count = choixService.getResponseCount(choix.getId());
                } catch (SQLException e) {
                    // Ignorer l'erreur et continuer
                }
                
                series.getData().add(new XYChart.Data<>(choix.getContenu(), count));
            }
            
            barChartResults.getData().clear();
            barChartResults.getData().add(series);
            barChartResults.setTitle("Réponses par choix");
            
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors du chargement des résultats", e.getMessage());
        }
    }
    
    /**
     * Crée ou met à jour une réponse
     */
    @FXML
    private void saveReponse() {
        if (currentSondage == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Sondage requis", "Veuillez sélectionner un sondage.");
            return;
        }
        
        RadioButton selectedRb = (RadioButton) choixToggleGroup.getSelectedToggle();
        if (selectedRb == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Choix requis", "Veuillez sélectionner un choix.");
            return;
        }
        
        ChoixSondage selectedChoix = (ChoixSondage) selectedRb.getUserData();
        
        try {
            // Récupérer l'utilisateur statique
            User currentUser = userService.getById(1);
            if (currentUser == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", 
                         "Utilisateur non trouvé", "L'utilisateur avec ID=1 n'existe pas.");
                return;
            }
            
            // Vérifier si l'utilisateur a déjà voté
            Reponse existingReponse = reponseService.getUserResponseForPoll(currentUser.getId(), currentSondage.getId());
            
            if (existingReponse != null) {
                // Mettre à jour la réponse existante
                existingReponse.setChoixSondage(selectedChoix);
                existingReponse.setDateReponse(LocalDate.now());
                
                reponseService.update(existingReponse);
                
                showAlert(Alert.AlertType.INFORMATION, "Succès", 
                         "Vote modifié", "Votre vote a été modifié avec succès.");
            } else {
                // Créer une nouvelle réponse
                Reponse reponse = new Reponse();
                reponse.setUser(currentUser);
                reponse.setSondage(currentSondage);
                reponse.setChoixSondage(selectedChoix);
                reponse.setDateReponse(LocalDate.now());
                
                reponseService.add(reponse);
                
                showAlert(Alert.AlertType.INFORMATION, "Succès", 
                         "Vote enregistré", "Votre vote a été enregistré avec succès.");
            }
            
            // Recharger les données
            loadReponses(currentSondage);
            updateCharts(currentSondage);
            
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors de l'enregistrement du vote", e.getMessage());
        }
    }
    
    /**
     * Supprime une réponse
     */
    private void deleteReponse(Reponse reponse) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmation de suppression");
        confirmDialog.setHeaderText("Supprimer votre vote");
        confirmDialog.setContentText("Êtes-vous sûr de vouloir supprimer votre vote ?");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                reponseService.delete(reponse.getId());
                
                loadReponses(currentSondage);
                updateCharts(currentSondage);
                
                showAlert(Alert.AlertType.INFORMATION, "Succès", 
                         "Vote supprimé", "Votre vote a été supprimé avec succès.");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", 
                         "Erreur lors de la suppression", e.getMessage());
            }
        }
    }
    
    /**
     * Efface les données du sondage
     */
    private void clearSondageData() {
        choicesContainer.getChildren().clear();
        tableReponses.getItems().clear();
        chartResults.getData().clear();
        barChartResults.getData().clear();
        currentSondage = null;
    }
    
    /**
     * Réinitialise le formulaire
     */
    private void resetForm() {
        if (choixToggleGroup != null) {
            choixToggleGroup.selectToggle(null);
        }
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
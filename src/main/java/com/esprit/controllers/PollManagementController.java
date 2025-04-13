package com.esprit.controllers;

import com.esprit.models.ChoixSondage;
import com.esprit.models.Club;
import com.esprit.models.Sondage;
import com.esprit.models.User;
import com.esprit.services.ClubService;
import com.esprit.services.SondageService;
import com.esprit.services.UserService;
import com.esprit.utils.AlertUtils;
import com.esprit.utils.NavigationManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Contrôleur pour la gestion des sondages
 */
public class PollManagementController implements Initializable {

    @FXML private TableView<Sondage> pollsTable;
    @FXML private TableColumn<Sondage, String> questionColumn;
    @FXML private TableColumn<Sondage, String> optionsColumn;
    @FXML private TableColumn<Sondage, String> dateColumn;
    @FXML private TableColumn<Sondage, String> actionsColumn;
    @FXML private Button backButton;
    @FXML private Button searchButton;
    @FXML private TextField searchField;
    @FXML private Pane toastContainer;
    
    private final SondageService sondageService = SondageService.getInstance();
    private final ClubService clubService = new ClubService();
    private final UserService userService = new UserService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    private User currentUser;
    private Scene previousScene;
    private ObservableList<Sondage> allPolls;
    private FilteredList<Sondage> filteredPolls;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            // Récupérer l'utilisateur actuel (pour les tests, ID=2)
            currentUser = userService.getById(2);
            
            // Configure table appearance
            pollsTable.setFixedCellSize(50);
            pollsTable.setPlaceholder(new Label("No polls available"));
            
            // Configurer les colonnes du tableau
            setupTableColumns();
            
            // Configurer la recherche
            setupSearch();
            
            // Charger les sondages
            loadPolls("all");
            
            // Configurer les boutons
            backButton.setOnAction(e -> navigateBack());
            
            // Configurer l'animation du toast
            setupToast();
            
            // Set stage to maximized mode after a small delay to ensure UI is fully loaded
            javafx.application.Platform.runLater(() -> {
                Stage stage = (Stage) pollsTable.getScene().getWindow();
                if (stage != null) {
                    stage.setMaximized(true);
                }
            });
            
        } catch (SQLException e) {
            AlertUtils.showError("Erreur d'initialisation", "Une erreur est survenue: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Configure les colonnes du tableau
     */
    private void setupTableColumns() {
        // Colonne Question
        questionColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getQuestion()));
        
        // Colonne Options
        optionsColumn.setCellValueFactory(cellData -> {
            try {
                // Get the options for this poll
                List<ChoixSondage> options = sondageService.getChoixBySondage(cellData.getValue().getId());
                
                // Format them as a comma-separated string
                return new SimpleStringProperty(options.stream()
                    .map(ChoixSondage::getContenu)
                    .collect(Collectors.joining(", ")));
            } catch (SQLException e) {
                e.printStackTrace();
                return new SimpleStringProperty("Error loading options");
            }
        });
        
        // Colonne Date
        dateColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCreatedAt().format(dateFormatter)));
        
        // Colonne Actions (boutons Modifier et Supprimer)
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox actionsBox = new HBox(10, editButton, deleteButton);
            
            {
                // Set explicit styles for better visibility
                editButton.getStyleClass().add("edit-button");
                deleteButton.getStyleClass().add("delete-button");
                
                editButton.setPrefWidth(65);
                deleteButton.setPrefWidth(65);
                
                editButton.setPrefHeight(25);
                deleteButton.setPrefHeight(25);
                
                actionsBox.setAlignment(javafx.geometry.Pos.CENTER);
                actionsBox.getStyleClass().add("actions-box");
                actionsBox.setPadding(new javafx.geometry.Insets(2, 0, 2, 0));
                
                // Configure the edit button's action
                editButton.setOnAction(e -> {
                    Sondage sondage = getTableView().getItems().get(getIndex());
                    openPollModal(sondage);
                });
                
                // Configure the delete button's action
                deleteButton.setOnAction(e -> {
                    Sondage sondage = getTableView().getItems().get(getIndex());
                    confirmDeletePoll(sondage);
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(actionsBox);
                }
            }
        });
    }
    
    /**
     * Configure la recherche
     */
    private void setupSearch() {
        searchButton.setOnAction(e -> performSearch());
        
        // Enable search on Enter key
        searchField.setOnAction(e -> performSearch());
    }
    
    /**
     * Exécute la recherche dans les sondages
     */
    private void performSearch() {
        String searchTerm = searchField.getText().toLowerCase().trim();
        
        if (searchTerm.isEmpty()) {
            pollsTable.setItems(allPolls);
        } else {
            filteredPolls = new FilteredList<>(allPolls, sondage -> 
                sondage.getQuestion().toLowerCase().contains(searchTerm));
            pollsTable.setItems(filteredPolls);
        }
    }
    
    /**
     * Configure l'animation du toast
     */
    private void setupToast() {
        toastContainer.setVisible(false);
    }
    
    /**
     * Affiche un toast avec un message
     */
    private void showToast(String message, String type) {
        Label toastText = (Label) ((HBox) toastContainer.getChildren().get(0)).getChildren().get(0);
        toastText.setText(message);
        
        HBox toastBox = (HBox) toastContainer.getChildren().get(0);
        toastBox.getStyleClass().removeAll("toast-success", "toast-error");
        toastBox.getStyleClass().add("error".equals(type) ? "toast-error" : "toast-success");
        
        toastContainer.setVisible(true);
        
        // Hide the toast after 3 seconds
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                javafx.application.Platform.runLater(() -> toastContainer.setVisible(false));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
     * Charge les sondages dans le tableau avec filtre optionnel
     */
    private void loadPolls(String clubFilter) throws SQLException {
        ObservableList<Sondage> polls;
        
        try {
            if ("all".equals(clubFilter)) {
                // Si on récupère déjà une ObservableList, pas besoin de conversion
                polls = sondageService.getAll();
            } else {
                // Conversion de List en ObservableList
                List<Sondage> sondagesList = sondageService.getByClub(clubFilter);
                polls = FXCollections.observableArrayList(sondagesList);
            }
            
            this.allPolls = polls;
            
            // Set placeholder text for empty table
            if (polls.isEmpty()) {
                pollsTable.setPlaceholder(new Label("No polls found for the selected criteria"));
            } else {
                pollsTable.setPlaceholder(new Label("No polls available"));
            }
            
            pollsTable.setItems(allPolls);
        } catch (SQLException e) {
            AlertUtils.showError("Error", "Failed to load polls: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Ouvre la fenêtre modale pour créer ou modifier un sondage
     */
    private void openPollModal(Sondage sondage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/EditPollModal.fxml"));
            VBox modalContent = loader.load();
            
            // Create scene first before setting the stage
            Scene modalScene = new Scene(modalContent);
            
            // Add stylesheet to scene
            modalScene.getStylesheets().add(getClass().getResource("/com/esprit/styles/poll-management-style.css").toExternalForm());
            
            Stage modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.setTitle(sondage == null ? "Create Poll" : "Edit Poll");
            
            // Set scene to stage before passing it to the controller
            modalStage.setScene(modalScene);
            
            EditPollModalController controller = loader.getController();
            controller.setModalStage(modalStage);
            
            try {
                if (sondage == null) {
                    controller.setCreateMode(currentUser);
                } else {
                    Sondage refreshedSondage = sondageService.getById(sondage.getId());
                    if (refreshedSondage != null) {
                        controller.setEditMode(refreshedSondage, currentUser);
                    } else {
                        controller.setEditMode(sondage, currentUser);
                    }
                }
                
                controller.setOnSaveHandler(() -> {
                    try {
                        loadPolls("all");
                        showCustomAlert("Success", "Poll operation completed successfully!", "success");
                    } catch (SQLException e) {
                        showCustomAlert("Error", "Unable to reload polls: " + e.getMessage(), "error");
                    }
                });
                
                // Show the modal window
                modalStage.showAndWait();
                
            } catch (SQLException e) {
                showCustomAlert("Error", "Failed to prepare poll data: " + e.getMessage(), "error");
                e.printStackTrace();
            }
            
        } catch (IOException e) {
            showCustomAlert("Error", "Unable to open modal window: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }
    
    /**
     * Demande confirmation avant de supprimer un sondage
     */
    private void confirmDeletePoll(Sondage sondage) {
        boolean confirm = showCustomConfirmDialog(
            "Delete Poll",
            "Are you sure you want to delete this poll?",
            "This action cannot be undone. All responses and comments will also be deleted."
        );
        
        if (confirm) {
            try {
                deletePollWithDependencies(sondage.getId());
                showCustomAlert("Success", "Poll deleted successfully!", "success");
                loadPolls("all");
            } catch (SQLException e) {
                showCustomAlert("Error", "Unable to delete poll: " + e.getMessage(), "error");
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Delete a poll and all its dependencies to avoid foreign key constraint violations
     */
    private void deletePollWithDependencies(int pollId) throws SQLException {
        // Get all responses/votes for this poll and delete them first
        try {
            // Delete comments related to the poll
            sondageService.deleteCommentsByPollId(pollId);
            
            // Delete responses/votes related to the poll
            sondageService.deleteResponsesByPollId(pollId);
            
            // Delete poll options
            sondageService.deleteOptionsByPollId(pollId);
            
            // Finally delete the poll itself
            sondageService.delete(pollId);
        } catch (SQLException e) {
            AlertUtils.showError("Error", "Could not delete poll dependencies: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Définit la scène précédente pour la navigation
     */
    public void setPreviousScene(Scene scene) {
        this.previousScene = scene;
    }
    
    /**
     * Filtre les sondages pour afficher uniquement ceux d'un club spécifique
     * @param clubName Nom du club à filtrer
     */
    public void filterByClub(String clubName) {
        // Ne filtre plus par club directement, mais utilise la recherche
        if (clubName != null && !clubName.isEmpty() && !"all".equals(clubName)) {
            try {
                // Charge d'abord tous les sondages
                loadPolls("all");
                
                // Filtre les sondages du club spécifié
                filteredPolls = new FilteredList<>(allPolls, sondage -> 
                    sondage.getClub().getNom().equals(clubName));
                
                pollsTable.setItems(filteredPolls);
            } catch (SQLException e) {
                AlertUtils.showError("Erreur", "Impossible de charger les sondages du club: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Retourne à la vue précédente
     */
    private void navigateBack() {
        if (previousScene != null) {
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(previousScene);
            
            // Maximiser la fenêtre précédente
            stage.setMaximized(true);
        } else {
            AlertUtils.showWarning("Navigation", "Impossible de revenir à la vue précédente.");
        }
    }

    private void showCustomAlert(String title, String message, String type) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Set the alert type
        ButtonType buttonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().add(buttonType);
        
        // Apply custom styling
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/com/esprit/styles/alert-style.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-alert");
        
        // Add specific style class based on alert type
        switch (type.toLowerCase()) {
            case "success":
                dialogPane.getStyleClass().add("custom-alert-success");
                break;
            case "warning":
                dialogPane.getStyleClass().add("custom-alert-warning");
                break;
            case "error":
                dialogPane.getStyleClass().add("custom-alert-error");
                break;
        }
        
        // Style the button
        Button okButton = (Button) dialogPane.lookupButton(buttonType);
        okButton.getStyleClass().add("custom-alert-button");
        
        alert.showAndWait();
    }

    private boolean showCustomConfirmDialog(String title, String message, String details) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.setContentText(details);
        
        // Set custom buttons
        ButtonType deleteButton = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(deleteButton, cancelButton);
        
        // Apply custom styling
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/com/esprit/styles/alert-style.css").toExternalForm());
        dialogPane.getStyleClass().addAll("custom-alert", "custom-alert-warning");
        
        // Style the buttons
        Button confirmButton = (Button) dialogPane.lookupButton(deleteButton);
        confirmButton.getStyleClass().addAll("custom-alert-button", "confirm-button");
        
        Button cancelBtn = (Button) dialogPane.lookupButton(cancelButton);
        cancelBtn.getStyleClass().addAll("custom-alert-button", "cancel-button");
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == deleteButton;
    }
}
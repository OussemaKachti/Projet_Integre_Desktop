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
import com.esprit.utils.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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

    @FXML private ListView<Sondage> pollsListView;
    @FXML private Button backButton;
    @FXML private Button searchButton;
    @FXML private TextField searchField;
    @FXML private Pane toastContainer;
    @FXML private HBox pageButtonsContainer;
    
    private final int ITEMS_PER_PAGE = 3;
    private int currentPage = 1;
    private int totalPages;
    
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
            // Get the logged-in user from SessionManager
            currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                AlertUtils.showError("Error", "No user is currently logged in.");
                return;
            }

            // Get the club where the user is president
            Club userClub = clubService.findByPresident(currentUser.getId());
            if (userClub == null) {
                AlertUtils.showError("Access Denied", "You must be a club president to access this view.");
                navigateBack();
                return;
            }
            
            // Configure list appearance
            pollsListView.setPlaceholder(new Label("No polls available"));
            
            // Setup ListView with custom cell factory
            setupListView();
            
            // Configure search
            setupSearch();
            
            // Load polls for the user's club only
            loadPolls(userClub.getId());
            
            // Configure buttons
            backButton.setOnAction(e -> navigateBack());
            
            // Configure toast animation
            setupToast();
            
            // Set stage to maximized mode after a small delay to ensure UI is fully loaded
            javafx.application.Platform.runLater(() -> {
                Stage stage = (Stage) pollsListView.getScene().getWindow();
                if (stage != null) {
                    stage.setMaximized(true);
                }
            });
            
        } catch (SQLException e) {
            AlertUtils.showError("Initialization Error", "An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Configure ListView with custom cell factory
     */
    private void setupListView() {
        pollsListView.setCellFactory(listView -> new ListCell<Sondage>() {
            private final HBox rowContainer = new HBox();
            private final Label questionLabel = new Label();
            private final Label optionsLabel = new Label();
            private final Label dateLabel = new Label();
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox actionsBox = new HBox(10, editButton, deleteButton);
            
            {
                // Configure container to look like a table row
                rowContainer.setAlignment(Pos.CENTER_LEFT);
                rowContainer.setPadding(new Insets(0, 5, 0, 5));
                
                // Question column
                questionLabel.setPrefWidth(500);
                questionLabel.setMaxWidth(500);
                questionLabel.setWrapText(true);
                questionLabel.getStyleClass().add("table-cell-question");
                
                // Options column
                optionsLabel.setPrefWidth(400);
                optionsLabel.setMaxWidth(400);
                optionsLabel.setWrapText(true);
                optionsLabel.getStyleClass().add("table-cell-options");
                
                // Date column
                dateLabel.setPrefWidth(180);
                dateLabel.setMaxWidth(180);
                dateLabel.getStyleClass().add("table-cell-date");
                
                // Configure action buttons to match the image
                editButton.getStyleClass().add("edit-button");
                editButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
                
                deleteButton.getStyleClass().add("delete-button");
                deleteButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");

                // Actions column
                actionsBox.setAlignment(Pos.CENTER);
                actionsBox.setPrefWidth(180);
                actionsBox.setMaxWidth(180);
                actionsBox.getStyleClass().add("table-cell-actions");
                
                // Add all elements to the row container with exact spacing as in image
                rowContainer.getChildren().addAll(questionLabel, optionsLabel, dateLabel, actionsBox);
                rowContainer.setMinHeight(40);
                rowContainer.setMaxHeight(40);
            }
            
            @Override
            protected void updateItem(Sondage poll, boolean empty) {
                super.updateItem(poll, empty);
                
                if (empty || poll == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Set question text
                    questionLabel.setText(poll.getQuestion());
                    
                    // Set options text (without prefix, just like in the image)
                    try {
                        List<ChoixSondage> options = sondageService.getChoixBySondage(poll.getId());
                        String optionsText = options.stream()
                            .map(ChoixSondage::getContenu)
                            .collect(Collectors.joining(", "));
                        optionsLabel.setText(optionsText);
                    } catch (SQLException e) {
                        optionsLabel.setText("Error loading options");
                    }
                    
                    // Set date text in the format from the image
                    dateLabel.setText(poll.getCreatedAt().format(dateFormatter));
                    
                    // Configure the edit button's action
                    editButton.setOnAction(e -> openPollModal(poll));
                    
                    // Configure the delete button's action
                    deleteButton.setOnAction(e -> confirmDeletePoll(poll));
                    
                    setGraphic(rowContainer);
                    
                    // Set row style based on even/odd to match the image
                    if (getIndex() % 2 == 0) {
                        rowContainer.setStyle("-fx-background-color: white;");
                    } else {
                        rowContainer.setStyle("-fx-background-color: #f8f9fa;");
                    }
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
            filteredPolls = null;
        } else {
            filteredPolls = new FilteredList<>(this.allPolls, sondage -> 
                sondage.getQuestion().toLowerCase().contains(searchTerm));
        }
        
        // Reset to first page and update pagination
        currentPage = 1;
        totalPages = (int) Math.ceil((double) (filteredPolls != null ? filteredPolls.size() : allPolls.size()) / ITEMS_PER_PAGE);
        updatePagination();
        showCurrentPage();
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
     * Charge les sondages dans la liste avec filtre optionnel
     */
   private void loadPolls(int clubId) throws SQLException {
    try {
        // Get polls for the specific club
        List<Sondage> sondagesList = sondageService.getAll().stream()
            .filter(sondage -> sondage.getClub() != null && sondage.getClub().getId() == clubId)
            .collect(Collectors.toList());
        
        this.allPolls = FXCollections.observableArrayList(sondagesList);
        
        // Calculate total pages
        totalPages = (int) Math.ceil((double) allPolls.size() / ITEMS_PER_PAGE);
        
        // Update pagination
        updatePagination();
        
        // Show current page
        showCurrentPage();
        
    } catch (SQLException e) {
        AlertUtils.showError("Error", "Failed to load polls: " + e.getMessage());
        throw e;
    }
}
    
    private void updatePagination() {
        pageButtonsContainer.getChildren().clear();
        
        for (int i = 1; i <= totalPages; i++) {
            Button pageButton = new Button(String.valueOf(i));
            pageButton.getStyleClass().add("page-button");
            
            if (i == currentPage) {
                pageButton.getStyleClass().add("page-button-active");
            }
            
            final int pageNumber = i;
            pageButton.setOnAction(e -> {
                currentPage = pageNumber;
                updatePagination();
                showCurrentPage();
            });
            
            pageButtonsContainer.getChildren().add(pageButton);
        }
    }
    
    private void showCurrentPage() {
        ObservableList<Sondage> currentList = filteredPolls != null ? filteredPolls : allPolls;
        int fromIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, currentList.size());
        
        if (fromIndex >= currentList.size()) {
            pollsListView.setItems(FXCollections.observableArrayList());
        } else {
            ObservableList<Sondage> currentPageItems = FXCollections.observableArrayList(
                currentList.subList(fromIndex, toIndex)
            );
            pollsListView.setItems(currentPageItems);
        }
        
        if (currentList.isEmpty()) {
            pollsListView.setPlaceholder(new Label("No polls found for your club"));
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
            // Get the user's club
            Club userClub = clubService.findByPresident(currentUser.getId());
            if (userClub == null) {
                throw new IllegalStateException("User is not a president of any club");
            }
            
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
                    loadPolls(userClub.getId()); // Use userClub.getId() instead of currentUser.getClub()
                    // showCustomAlert("Success", "Poll operation completed successfully!", "success");
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
                
                // Get the user's club using ClubService
                Club userClub = clubService.findByPresident(currentUser.getId());
                if (userClub != null) {
                    loadPolls(userClub.getId());
                } else {
                    showCustomAlert("Error", "Could not find user's club", "error");
                }
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
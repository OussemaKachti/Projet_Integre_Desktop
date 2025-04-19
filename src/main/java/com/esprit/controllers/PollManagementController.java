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
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
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

    @FXML private Button backButton;
    @FXML private Button searchButton;
    @FXML private TextField searchField;
    @FXML private Pane toastContainer;
    @FXML private HBox pageButtonsContainer;
    
    // Navbar components
    @FXML private StackPane clubsContainer;
    @FXML private Button clubsButton;
    @FXML private VBox clubsDropdown;
    @FXML private HBox clubPollsItem;
    @FXML private Label clubPollsLabel;
    @FXML private StackPane userProfileContainer;
    @FXML private ImageView userProfilePic;
    @FXML private Label userNameLabel;
    @FXML private VBox profileDropdown;
    
    // New UI components
    @FXML private VBox pollsTableContent;
    @FXML private StackPane emptyStateContainer;
    
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

            // Set user name in navbar
            if (userNameLabel != null) {
                userNameLabel.setText(currentUser.getFirstName() + " " + currentUser.getLastName());
            }

            // Initially hide the dropdowns
            if (profileDropdown != null) {
                profileDropdown.setVisible(false);
                profileDropdown.setManaged(false);
            }
            if (clubsDropdown != null) {
                clubsDropdown.setVisible(false);
                clubsDropdown.setManaged(false);
            }
            
            // Get the club where the user is president
            Club userClub = clubService.findByPresident(currentUser.getId());
            if (userClub == null) {
                AlertUtils.showError("Access Denied", "You must be a club president to access this view.");
                navigateBack();
                return;
            }
            
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
                Stage stage = (Stage) backButton.getScene().getWindow();
                if (stage != null) {
                    stage.setMaximized(true);
                }
            });
            
        } catch (SQLException e) {
            AlertUtils.showError("Initialization Error", "An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Navigation methods for navbar
    
    @FXML
    public void showProfileDropdown() {
        if (profileDropdown != null) {
            profileDropdown.setVisible(true);
            profileDropdown.setManaged(true);
        }
    }
    
    @FXML
    public void hideProfileDropdown() {
        if (profileDropdown != null) {
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
        }
    }
    
    @FXML
    public void showClubsDropdown() {
        if (clubsDropdown != null) {
            clubsDropdown.setVisible(true);
            clubsDropdown.setManaged(true);
        }
    }
    
    @FXML
    public void hideClubsDropdown() {
        if (clubsDropdown != null) {
            clubsDropdown.setVisible(false);
            clubsDropdown.setManaged(false);
        }
    }
    
    @FXML
    public void navigateToHome() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/home.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.setScene(scene);
        stage.setMaximized(true);
    }
    
    @FXML
    public void navigateToPolls() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/SondageView.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.setScene(scene);
        stage.setMaximized(true);
    }
    
    @FXML
    public void navigateToProfile() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/profile.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.setScene(scene);
        stage.setMaximized(true);
    }
    
    @FXML
    public void handleLogout() throws IOException {
        // Clear the session
        SessionManager.getInstance().clearSession();
        
        // Navigate to login screen
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/login.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.setScene(scene);
    }
    
    @FXML
    public void navigateToEvents() throws IOException {
        // Navigate to events page
        AlertUtils.showInformation("Navigation", "Events page is not yet implemented.");
    }
    
    @FXML
    public void navigateToProducts() throws IOException {
        // Navigate to products page
        AlertUtils.showInformation("Navigation", "Products page is not yet implemented.");
    }
    
    @FXML
    public void navigateToCompetition() throws IOException {
        // Navigate to competition page
        AlertUtils.showInformation("Navigation", "Competition page is not yet implemented.");
    }
    
    @FXML
    public void navigateToContact() throws IOException {
        // Navigate to contact page
        AlertUtils.showInformation("Navigation", "Contact page is not yet implemented.");
    }
    
    /**
     * Builds and adds a table row for a single poll
     */
    private void addPollRow(Sondage poll, int index) {
        // Create the main row container
        HBox rowContainer = new HBox();
        rowContainer.getStyleClass().add("modern-table-row");
        
        // Add alternating row styles
        if (index % 2 == 0) {
            rowContainer.getStyleClass().add("modern-table-row-even");
        } else {
            rowContainer.getStyleClass().add("modern-table-row-odd");
        }
        
        // Create the question cell
        Label questionLabel = new Label(poll.getQuestion());
        questionLabel.setPrefWidth(450);
        questionLabel.setMaxWidth(450);
        questionLabel.setWrapText(true);
        questionLabel.getStyleClass().add("modern-table-cell");
        questionLabel.getStyleClass().add("question-cell");
        
        // Create the options cell
        Label optionsLabel = new Label();
        optionsLabel.setPrefWidth(350);
        optionsLabel.setMaxWidth(350);
        optionsLabel.setWrapText(true);
        optionsLabel.getStyleClass().add("modern-table-cell");
        optionsLabel.getStyleClass().add("options-cell");
        
        // Load options
        try {
            List<ChoixSondage> options = sondageService.getChoixBySondage(poll.getId());
            String optionsText = options.stream()
                .map(ChoixSondage::getContenu)
                .collect(Collectors.joining(", "));
            optionsLabel.setText(optionsText);
        } catch (SQLException e) {
            optionsLabel.setText("Error loading options");
        }
        
        // Create the date cell
        Label dateLabel = new Label(poll.getCreatedAt().format(dateFormatter));
        dateLabel.setPrefWidth(180);
        dateLabel.setMaxWidth(180);
        dateLabel.getStyleClass().add("modern-table-cell");
        dateLabel.getStyleClass().add("date-cell");
        
        // Create the actions cell
        HBox actionsBox = new HBox(10);
        actionsBox.setAlignment(Pos.CENTER);
        actionsBox.setPrefWidth(180);
        actionsBox.setMaxWidth(180);
        actionsBox.getStyleClass().add("modern-table-cell");
        actionsBox.getStyleClass().add("actions-cell");
        
        // Create action buttons
        Button editButton = new Button("Edit");
        editButton.getStyleClass().add("edit-button");
        
        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("delete-button");
        
        // Set button actions
        editButton.setOnAction(e -> openPollModal(poll));
        deleteButton.setOnAction(e -> confirmDeletePoll(poll));
        
        // Add buttons to actions box
        actionsBox.getChildren().addAll(editButton, deleteButton);
        
        // Add all elements to row
        rowContainer.getChildren().addAll(questionLabel, optionsLabel, dateLabel, actionsBox);
        
        // Add hover effect - using JavaFX fade transitions for subtle effect
        rowContainer.setOnMouseEntered(e -> {
            rowContainer.getStyleClass().add("modern-table-row-hover");
        });
        
        rowContainer.setOnMouseExited(e -> {
            rowContainer.getStyleClass().remove("modern-table-row-hover");
        });
        
        // Add row to table content
        pollsTableContent.getChildren().add(rowContainer);
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
        
        // Show visual feedback for search results
        if (filteredPolls != null && filteredPolls.isEmpty()) {
            emptyStateContainer.setVisible(true);
            emptyStateContainer.setManaged(true);
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
     * Charge les sondages dans la liste avec filtre optionnel
     */
    private void loadPolls(int clubId) throws SQLException {
        try {
            // Clear existing content
            pollsTableContent.getChildren().clear();
            
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
            
            // Show/hide empty state
            emptyStateContainer.setVisible(allPolls.isEmpty());
            emptyStateContainer.setManaged(allPolls.isEmpty());
            
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
        
        // Clear existing content
        pollsTableContent.getChildren().clear();
        
        if (fromIndex >= currentList.size()) {
            // No items to display
            emptyStateContainer.setVisible(true);
            emptyStateContainer.setManaged(true);
        } else {
            // Create and add rows for current page items
            List<Sondage> currentPageItems = currentList.subList(fromIndex, toIndex);
            
            for (int i = 0; i < currentPageItems.size(); i++) {
                addPollRow(currentPageItems.get(i), i);
            }
            
            emptyStateContainer.setVisible(false);
            emptyStateContainer.setManaged(false);
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
            
            // If we're returning to SondageView, force a reload by creating a new instance
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/SondageView.fxml"));
                Scene scene = new Scene(loader.load());
                stage.setScene(scene);
                stage.setMaximized(true);
            } catch (IOException e) {
                // Fallback to the previous scene if loading fails
                stage.setScene(previousScene);
                stage.setMaximized(true);
                e.printStackTrace();
                AlertUtils.showError("Navigation Error", "Failed to reload view: " + e.getMessage());
            }
        } else {
            // Try to navigate to SondageView directly if previous scene is null
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/SondageView.fxml"));
                Scene scene = new Scene(loader.load());
                Stage stage = (Stage) backButton.getScene().getWindow();
                stage.setScene(scene);
                stage.setMaximized(true);
            } catch (IOException e) {
                e.printStackTrace();
                AlertUtils.showInformation("Navigation", "Impossible de revenir à la vue précédente.");
            }
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
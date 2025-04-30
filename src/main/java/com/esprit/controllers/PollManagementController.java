package com.esprit.controllers;

import com.esprit.models.ChoixSondage;
import com.esprit.models.Club;
import com.esprit.models.Commentaire;
import com.esprit.models.Sondage;
import com.esprit.models.User;
import com.esprit.services.ChoixSondageService;
import com.esprit.services.ClubService;
import com.esprit.services.CommentaireService;
import com.esprit.services.OpenAIService;
import com.esprit.services.ReponseService;
import com.esprit.services.SondageService;
import com.esprit.services.UserService;
import com.esprit.utils.AlertUtils;
import com.esprit.utils.NavigationManager;
import com.esprit.utils.SessionManager;

import javafx.application.Platform;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import java.util.concurrent.CompletableFuture;

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
    
    // Nouveaux FXML components pour le leaderboard
    @FXML private VBox leaderboardContainer;
    @FXML private Button refreshLeaderboardButton;
    @FXML private HBox participationStatsContainer;
    @FXML private Label totalVotesLabel;
    @FXML private Label uniqueParticipantsLabel;
    @FXML private Label mostPopularPollLabel;
    @FXML private Label mostPopularPollVotesLabel;
    @FXML private VBox leaderboardContent;
    
    private final int ITEMS_PER_PAGE = 3;
    private int currentPage = 1;
    private int totalPages;
    
    private final SondageService sondageService = SondageService.getInstance();
    private final ClubService clubService = new ClubService();
    private final UserService userService = new UserService();
    private final ReponseService reponseService = new ReponseService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    private User currentUser;
    private Club currentClub;
    private Scene previousScene;
    private ObservableList<Sondage> allPolls;
    private FilteredList<Sondage> filteredPolls;
    private List<Map<String, Object>> leaderboardData;
    private Map<String, Object> participationStats;
    private OpenAIService openAIService;
    private static final String[] MEDAL_ICONS = {"🥇", "🥈", "🥉"};
    private static final int TOP_USERS_LIMIT = 10;
    
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
            currentClub = clubService.findByPresident(currentUser.getId());
            if (currentClub == null) {
                AlertUtils.showError("Access Denied", "You must be a club president to access this view.");
                navigateBack();
                return;
            }
            
            // Configure search
            setupSearch();
            
            // Load polls for the user's club only
            loadPolls(currentClub.getId());
            
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
            
            // Configuration de l'interface
            setupLeaderboard();
            setupEventHandlers();
            
            // Charger les données
            loadLeaderboardData();
            
            // Initialiser l'OpenAI Service
            try {
                openAIService = new OpenAIService();
            } catch (Exception e) {
                System.err.println("Error initializing OpenAI service: " + e.getMessage());
            }
            
            // Mettre à jour les informations utilisateur
            updateUserInfo();
            
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
     * Configure toast animation
     */
    private void setupToast() {
        // Configure toast animation for the toast container
        toastContainer.setVisible(false);
        toastContainer.setOpacity(0);
    }

    /**
     * Set up event handlers for UI elements
     */
    private void setupEventHandlers() {
        // Set up search button event
        if (searchButton != null) {
            searchButton.setOnAction(e -> performSearch());
        }
        
        // Set up search field enter key event
        if (searchField != null) {
            searchField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    performSearch();
                }
            });
        }
        
        // Set up refresh button event
        if (refreshLeaderboardButton != null) {
            refreshLeaderboardButton.setOnAction(e -> refreshLeaderboard());
        }
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

    /**
     * Configure le tableau de classement et les statistiques de participation
     */
    private void setupLeaderboard() {
        // Initialisation des statistiques
        totalVotesLabel.setText("0");
        uniqueParticipantsLabel.setText("0");
        mostPopularPollLabel.setText("No poll data available yet");
        mostPopularPollVotesLabel.setText("0 votes");
        
        // Vider le contenu du leaderboard
        leaderboardContent.getChildren().clear();
        
        // Créer dynamiquement une icône de rafraîchissement pour le bouton
        Label refreshIcon = new Label("⟳");
        refreshIcon.setStyle("-fx-font-size: 16px; -fx-text-fill: #555555;");
        refreshLeaderboardButton.setGraphic(refreshIcon);
    }
    
    /**
     * Charge les données du tableau de classement depuis la base de données
     */
    private void loadLeaderboardData() {
        if (currentClub == null) {
            return;
        }
        
        try {
            // Afficher un indicateur de chargement
            showLoadingState();
            
            // Chargement asynchrone pour ne pas bloquer l'interface utilisateur
            CompletableFuture.runAsync(() -> {
                try {
                    // Récupérer les statistiques de participation
                    Map<String, Object> stats = reponseService.getParticipationStatsByClub(currentClub.getId());
                    
                    // Récupérer les utilisateurs les plus actifs
                    List<Map<String, Object>> topUsers = reponseService.getTopRespondentsByClub(currentClub.getId(), TOP_USERS_LIMIT);
                    
                    // Enrichir les données avec l'OpenAI si disponible
                    if (openAIService != null && !topUsers.isEmpty()) {
                        enrichLeaderboardData(topUsers);
                    }
                    
                    // Mettre à jour l'interface sur le thread JavaFX
                    Platform.runLater(() -> {
                        updateParticipationStats(stats);
                        updateLeaderboardTable(topUsers);
                        hideLoadingState();
                    });
                    
                } catch (SQLException e) {
                    e.printStackTrace();
                    // Afficher l'erreur sur le thread JavaFX
                    Platform.runLater(() -> {
                        showAlert("Error", "Failed to load leaderboard data: " + e.getMessage(), "error");
                        hideLoadingState();
                    });
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load leaderboard data: " + e.getMessage(), "error");
            hideLoadingState();
        }
    }
    
    /**
     * Enrichit les données du leaderboard avec des descriptions générées par l'IA
     */
    private void enrichLeaderboardData(List<Map<String, Object>> topUsers) {
        try {
            // Préparer une liste des utilisateurs pour l'API
            List<String> userDescriptions = new ArrayList<>();
            for (Map<String, Object> user : topUsers) {
                String name = user.get("firstName") + " " + user.get("lastName");
                int voteCount = (int) user.get("voteCount");
                userDescriptions.add(name + " - " + voteCount + " votes");
            }
            
            // Générer des badges spéciaux pour les utilisateurs
            String prompt = "Generate creative, short (max 3 words) badge titles for these users based on their poll participation. " + 
                            "Format each badge as 'Badge Title'. Examples: 'Survey Champion', 'Voting Expert', 'Feedback Guru'.\n\n" +
                            String.join("\n", userDescriptions);
            
            // Appeler l'API OpenAI
            String response = openAIService.summarizeComments(convertToCommentaireList(prompt));
            
            // Analyser la réponse
            String[] badges = response.split("\n");
            for (int i = 0; i < Math.min(badges.length, topUsers.size()); i++) {
                String badge = badges[i].trim();
                // Nettoyer le badge (enlever numérotation, tirets, etc.)
                badge = badge.replaceAll("^\\d+\\.\\s*", "").replaceAll("^-\\s*", "");
                if (badge.startsWith("\"") && badge.endsWith("\"")) {
                    badge = badge.substring(1, badge.length() - 1);
                }
                if (badge.startsWith("'") && badge.endsWith("'")) {
                    badge = badge.substring(1, badge.length() - 1);
                }
                
                // Ajouter le badge au user
                topUsers.get(i).put("badge", badge);
            }
            
        } catch (Exception e) {
            System.err.println("Error enriching leaderboard data: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to convert a String to a Commentaire object for OpenAI processing
     */
    private List<Commentaire> convertToCommentaireList(String content) {
        Commentaire commentaire = new Commentaire();
        commentaire.setContenuComment(content);
        commentaire.setDateComment(LocalDate.now());
        return Collections.singletonList(commentaire);
    }
    
    /**
     * Met à jour les statistiques de participation
     */
    private void updateParticipationStats(Map<String, Object> stats) {
        // Mettre à jour les labels de statistiques
        int totalVotes = (int) stats.getOrDefault("totalVotes", 0);
        int uniqueParticipants = (int) stats.getOrDefault("uniqueParticipants", 0);
        
        totalVotesLabel.setText(String.valueOf(totalVotes));
        uniqueParticipantsLabel.setText(String.valueOf(uniqueParticipants));
        
        // Mettre à jour les informations sur le sondage le plus populaire
        Map<String, Object> mostPopularPoll = (Map<String, Object>) stats.get("mostPopularPoll");
        if (mostPopularPoll != null) {
            String question = (String) mostPopularPoll.get("question");
            int voteCount = (int) mostPopularPoll.get("voteCount");
            
            mostPopularPollLabel.setText(question);
            mostPopularPollVotesLabel.setText(voteCount + " votes");
        } else {
            mostPopularPollLabel.setText("No poll data available yet");
            mostPopularPollVotesLabel.setText("0 votes");
        }
    }
    
    /**
     * Met à jour le tableau de classement avec les données des utilisateurs
     */
    private void updateLeaderboardTable(List<Map<String, Object>> users) {
        // Vider le contenu existant
        leaderboardContent.getChildren().clear();
        
        if (users.isEmpty()) {
            // Afficher un message si aucun utilisateur
            VBox emptyState = new VBox();
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(30));
            emptyState.setSpacing(10);
            
            Label emptyLabel = new Label("No participation data available");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");
            
            emptyState.getChildren().add(emptyLabel);
            leaderboardContent.getChildren().add(emptyState);
            return;
        }
        
        // Créer une ligne pour chaque utilisateur
        for (int i = 0; i < users.size(); i++) {
            Map<String, Object> user = users.get(i);
            int rank = (int) user.get("rank");
            
            // Créer une ligne pour l'utilisateur
            HBox row = createUserRow(user, i % 2 == 0);
            leaderboardContent.getChildren().add(row);
        }
    }
    
    /**
     * Crée une ligne pour un utilisateur dans le tableau de classement
     */
    private HBox createUserRow(Map<String, Object> user, boolean isEven) {
        HBox row = new HBox();
        row.getStyleClass().add("leaderboard-row");
        if (isEven) {
            row.getStyleClass().add("leaderboard-row-highlight");
        }
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(10);
        
        // Récupérer les données de l'utilisateur
        int rank = (int) user.get("rank");
        String firstName = (String) user.get("firstName");
        String lastName = (String) user.get("lastName");
        String profilePic = (String) user.get("profilePicture");
        int voteCount = (int) user.get("voteCount");
        String badge = (String) user.getOrDefault("badge", null);
        
        // Créer l'affichage du rang
        Label rankLabel = new Label(String.valueOf(rank));
        rankLabel.getStyleClass().add("leaderboard-rank");
        if (rank <= 3) {
            rankLabel.getStyleClass().add("leaderboard-rank-" + rank);
        }
        
        // Afficher une médaille pour les 3 premiers
        Label medalLabel = new Label("");
        medalLabel.getStyleClass().add("leaderboard-medal");
        if (rank <= 3) {
            medalLabel.setText(MEDAL_ICONS[rank - 1]);
        }
        
        // Créer l'avatar de l'utilisateur
        ImageView avatar = new ImageView();
        avatar.setFitHeight(40);
        avatar.setFitWidth(40);
        avatar.getStyleClass().add("leaderboard-avatar");
        
        // Charger l'image de profil
        try {
            if (profilePic != null && !profilePic.isEmpty()) {
                File profileImageFile = new File("uploads/profiles/" + profilePic);
                if (profileImageFile.exists()) {
                    Image profileImage = new Image(profileImageFile.toURI().toString());
                    avatar.setImage(profileImage);
                } else {
                    avatar.setImage(new Image(getClass().getResourceAsStream("/com/esprit/images/default-profile.png")));
                }
            } else {
                avatar.setImage(new Image(getClass().getResourceAsStream("/com/esprit/images/default-profile.png")));
            }
            
            // Rendre l'avatar circulaire
            Circle clip = new Circle(20, 20, 20);
            avatar.setClip(clip);
            
        } catch (Exception e) {
            System.err.println("Error loading profile image: " + e.getMessage());
            try {
                avatar.setImage(new Image(getClass().getResourceAsStream("/com/esprit/images/default-profile.png")));
            } catch (Exception ex) {
                System.err.println("Error loading default profile image: " + ex.getMessage());
            }
        }
        
        // Information de l'utilisateur
        VBox userInfo = new VBox(2);
        userInfo.getStyleClass().add("leaderboard-user-info");
        
        Label userName = new Label(firstName + " " + lastName);
        userName.getStyleClass().add("leaderboard-username");
        
        userInfo.getChildren().add(userName);
        
        // Ajouter un badge si disponible
        if (badge != null && !badge.isEmpty()) {
            Label badgeLabel = new Label(badge);
            badgeLabel.getStyleClass().addAll("leaderboard-badge");
            
            // Différentes couleurs selon le rang
            if (rank == 1) {
                badgeLabel.getStyleClass().add("leaderboard-badge-gold");
            } else if (rank == 2) {
                badgeLabel.getStyleClass().add("leaderboard-badge-silver");
            } else if (rank == 3) {
                badgeLabel.getStyleClass().add("leaderboard-badge-bronze");
            } else {
                badgeLabel.getStyleClass().add("leaderboard-badge-participant");
            }
            
            userInfo.getChildren().add(badgeLabel);
        }
        
        // Nombre de votes
        Label votes = new Label(voteCount + " votes");
        votes.getStyleClass().add("leaderboard-votes");
        
        // Spacer pour pousser les votes à droite
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Ajouter tous les éléments à la ligne
        row.getChildren().addAll(rankLabel, medalLabel, avatar, userInfo, spacer, votes);
        
        return row;
    }
    
    /**
     * Affiche un état de chargement pendant le chargement des données
     */
    private void showLoadingState() {
        // Créer un indicateur de chargement
        VBox loadingBox = new VBox();
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(30));
        loadingBox.setSpacing(15);
        loadingBox.setId("loadingBox");
        
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(40, 40);
        
        Label loadingLabel = new Label("Loading leaderboard data...");
        loadingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");
        
        loadingBox.getChildren().addAll(progressIndicator, loadingLabel);
        
        // Vider et ajouter
        leaderboardContent.getChildren().clear();
        leaderboardContent.getChildren().add(loadingBox);
    }
    
    /**
     * Cache l'état de chargement
     */
    private void hideLoadingState() {
        // Enlever l'indicateur de chargement s'il existe
        leaderboardContent.getChildren().removeIf(node -> node.getId() != null && node.getId().equals("loadingBox"));
    }
    
    /**
     * Rafraîchit les données du tableau de classement
     */
    @FXML
    public void refreshLeaderboard() {
        loadLeaderboardData();
    }
    
    /**
     * Met à jour les informations de l'utilisateur dans l'interface
     */
    private void updateUserInfo() {
        if (currentUser != null) {
            userNameLabel.setText(currentUser.getFirstName() + " " + currentUser.getLastName());
            
            // Charger l'image de profil
            try {
                String profilePic = currentUser.getProfilePicture();
                if (profilePic != null && !profilePic.isEmpty()) {
                    File profileImageFile = new File("uploads/profiles/" + profilePic);
                    if (profileImageFile.exists()) {
                        Image profileImage = new Image(profileImageFile.toURI().toString());
                        userProfilePic.setImage(profileImage);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error loading profile image: " + e.getMessage());
            }
        }
    }

    /**
     * Affiche une alerte personnalisée
     */
    private void showAlert(String title, String message, String type) {
        Alert alert = new Alert(
            "error".equals(type) ? Alert.AlertType.ERROR : 
            "warning".equals(type) ? Alert.AlertType.WARNING : 
            Alert.AlertType.INFORMATION
        );
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
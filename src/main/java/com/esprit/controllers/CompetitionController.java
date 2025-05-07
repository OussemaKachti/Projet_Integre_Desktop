package com.esprit.controllers;

import com.esprit.models.Competition;
import com.esprit.models.Saison;
import com.esprit.models.enums.GoalTypeEnum;
import com.esprit.services.AIContentGeneratorService;
import com.esprit.services.CompetitionService;
import com.esprit.services.SaisonService;

import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.kordamp.ikonli.javafx.FontIcon;

public class CompetitionController {

    private final CompetitionService competitionService = new CompetitionService();
    private final SaisonService saisonService = new SaisonService();
    private Competition selectedCompetition;
    private boolean isEditMode = false;
    private int currentPage = 1;
    private int itemsPerPage = 5;
    private int totalPages = 1;
    private final AIContentGeneratorService aiService = new AIContentGeneratorService();

    // Main components
    @FXML private BorderPane contentArea;
    @FXML private VBox missionListContainer;
    @FXML private VBox missionFormContainer;
    @FXML private StackPane formOverlay;
    @FXML private StackPane contentStackPane;

    // Header and Stats components
    @FXML private Label contentTitle;
    @FXML private Label dateLabel;
    @FXML private Label totalMissionsLabel;
    @FXML private Label activeMissionsLabel;
    @FXML private Label completedMissionsLabel;
    @FXML private Label adminNameLabel;

    // Search components
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button statisticsButton;
    @FXML private Button seasonManagementButton;

    // Filter components
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<Saison> seasonFilter;

    // Form fields
    @FXML private TextField missionTitleField;
    @FXML private TextArea missionDescField;
    @FXML private TextField pointsField;
    @FXML private DatePicker startDateField;
    @FXML private DatePicker endDateField;
    @FXML private TextField goalValueField;
    @FXML private ComboBox<GoalTypeEnum> goalTypeComboBox;
    @FXML private ComboBox<Saison> saisonComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private Label formTitleLabel;
    @FXML private Label paginationInfoLabel;
    @FXML private Button generateButton;


    // Buttons
    @FXML private Button saveButton;
    @FXML private Button addButton;
    @FXML private Button cancelButton;
    @FXML private Button closeFormButton;
    @FXML private Button prevPageButton;
    @FXML private Button nextPageButton;

    // Navigation buttons
    @FXML private Button userManagementButton;
    @FXML private Button clubManagementButton;
    @FXML private Button eventManagementButton;
    @FXML private Button productOrdersButton;
    @FXML private Button competitionButton;
    @FXML private Button surveyButton;
    @FXML private Button profileButton;
    @FXML private Button logoutButton;

    // Pagination labels
    @FXML private Label currentPageLabel;
    @FXML private Label totalPagesLabel;

    @FXML
    public void initialize() {
        try {

            // On initialization, update all competition statuses based on current date
            competitionService.updateAllStatuses();

            // Set current date in header
            LocalDate now = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
            dateLabel.setText("Today: " + now.format(formatter));

            // Set admin name
            adminNameLabel.setText("Admin User"); // Replace with actual admin name if available

            // Setup filters
            setupFilters();

            // Load all competitions
            loadAllCompetitions();

            // Update stats
            updateStats();

            // Set up button actions
            setupButtonActions();

            // Initialize dropdown values
            initializeComboBoxes();

            // Set up pagination
            setupPagination();

            // Apply custom styling
            applyCustomStyling();

            // Ensure form is properly styled when shown
            Platform.runLater(() -> {
                System.out.println("Applying styles to form elements");
                applyStylesToForm();
                // Hide or disable the status field in the form
                hideStatusField();

            });
        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Initialization Error",
                    "Failed to initialize mission management", e.getMessage());
        }
    }
    /**
     * Hide or disable the status field in the form since status
     * will now be automatically calculated based on dates
     */
    private void hideStatusField() {
        // Find the status field's parent container (likely a GridPane or similar layout)
        GridPane gridPane = null;
        for (Node node : missionFormContainer.getChildren()) {
            if (node instanceof GridPane) {
                gridPane = (GridPane) node;
                break;
            }
        }

        if (gridPane != null) {
            // Find and hide status label and combobox
            for (Node node : gridPane.getChildren()) {
                if (node instanceof Label) {
                    Label label = (Label) node;
                    if (label.getText() != null && label.getText().toLowerCase().contains("status")) {
                        label.setVisible(false);
                        label.setManaged(false);
                    }
                }
            }

            // Hide status combobox
            statusComboBox.setVisible(false);
            statusComboBox.setManaged(false);


        }
    }

    /**
     * Modified createMissionCard method to display and style status based on calculated value
     */

    private void setupFilters() {
        try {
            // Setup status filter
            statusFilter.getItems().clear();
            statusFilter.getItems().addAll("All Status", "Active", "Inactive");
            statusFilter.setValue("All Status");

            // Setup season filter
            seasonFilter.getItems().clear();

            // Create a "All Seasons" option
            Saison allSeasons = new Saison();
            allSeasons.setId(-1);
            allSeasons.setNomSaison("All Seasons");

            seasonFilter.getItems().add(allSeasons);

            // Get all seasons to populate filter
            List<Saison> saisons = saisonService.getAll();
            seasonFilter.getItems().addAll(saisons);

            // Set up readable names for saisons
            seasonFilter.setConverter(new StringConverter<Saison>() {
                @Override
                public String toString(Saison saison) {
                    return saison == null ? "" : saison.getNomSaison();
                }

                @Override
                public Saison fromString(String string) {
                    return null; // Not needed for combo box
                }
            });

            seasonFilter.setValue(allSeasons);

            // Set filter actions
            statusFilter.setOnAction(e -> filterCompetitions());
            seasonFilter.setOnAction(e -> filterCompetitions());
        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Filter Setup Error",
                    "Could not initialize filters", e.getMessage());
        }
    }

    private void filterCompetitions() {
        try {
            String statusValue = statusFilter.getValue();
            Saison selectedSeason = seasonFilter.getValue();

            // Reset to first page when filtering (this is correct behavior)
            currentPage = 1;
            currentPageLabel.setText("1");

            // Load filtered competitions
            loadFilteredCompetitions(statusValue, selectedSeason);

            // Update the pagination info
            updatePaginationInfo();
        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Filter Error",
                    "Could not apply filters", e.getMessage());
        }
    }

    private void loadFilteredCompetitions(String status, Saison season) throws SQLException {
        // Get all competitions first
        List<Competition> allCompetitions = competitionService.getAll();
        List<Competition> filteredCompetitions = new ArrayList<>(allCompetitions);

        // Filter by status if not "All Status"
        if (!"All Status".equals(status)) {
            String statusToFilter = "Active".equals(status) ? "activated" : "deactivated";
            filteredCompetitions = filteredCompetitions.stream()
                    .filter(c -> statusToFilter.equals(c.getStatus()))
                    .collect(Collectors.toList());
        }

        // Filter by season if not "All Seasons"
        if (season != null && season.getId() != -1) {
            filteredCompetitions = filteredCompetitions.stream()
                    .filter(c -> c.getSaisonId() != null && c.getSaisonId().getId() == season.getId())
                    .collect(Collectors.toList());
        }

        // Calculate total pages for filtered results
        totalPages = (int) Math.ceil((double) filteredCompetitions.size() / itemsPerPage);
        totalPages = totalPages == 0 ? 1 : totalPages; // Ensure at least 1 page
        totalPagesLabel.setText(String.valueOf(totalPages));

        // Update pagination controls
        prevPageButton.setDisable(currentPage <= 1);
        nextPageButton.setDisable(currentPage >= totalPages);

        // Calculate start and end index for current page
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, filteredCompetitions.size());

        // Get paged subset of filtered data
        List<Competition> pagedFilteredCompetitions =
                filteredCompetitions.isEmpty() ? new ArrayList<>() :
                        filteredCompetitions.subList(startIndex, endIndex);

        // Update the UI with filtered and paged data
        updateCompetitionList(pagedFilteredCompetitions);

        // Update the pagination info text
        paginationInfoLabel.setText(String.format("Showing %d to %d of %d missions",
                filteredCompetitions.isEmpty() ? 0 : startIndex + 1,
                endIndex,
                filteredCompetitions.size()));
    }

    private void updateStats() throws SQLException {
        // Count total competitions
        List<Competition> allCompetitions = competitionService.getAll();
        totalMissionsLabel.setText(String.valueOf(allCompetitions.size()));

        // Count active competitions
        long activeCount = allCompetitions.stream()
                .filter(c -> "activated".equals(c.getStatus()))
                .count();
        activeMissionsLabel.setText(String.valueOf(activeCount));

        // Count completed/inactive competitions
        long inactiveCount = allCompetitions.stream()
                .filter(c -> "deactivated".equals(c.getStatus()))
                .count();
        completedMissionsLabel.setText(String.valueOf(inactiveCount));
    }

    private void setupPagination() {
        // Set initial pagination state
        currentPageLabel.setText(String.valueOf(currentPage));
        totalPagesLabel.setText(String.valueOf(totalPages));

        // Setup pagination buttons
        prevPageButton.setDisable(currentPage <= 1);
        nextPageButton.setDisable(currentPage >= totalPages);
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            currentPageLabel.setText(String.valueOf(currentPage)); // Add this line
            try {
                loadPagedCompetitions();
                updatePaginationControls();
            } catch (SQLException e) {
                showAlert(AlertType.ERROR, "Pagination Error",
                        "Failed to load previous page", e.getMessage());
            }
        }
    }

    @FXML
    private void handleNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            currentPageLabel.setText(String.valueOf(currentPage)); // Add this line
            try {
                loadPagedCompetitions();
                updatePaginationControls();
            } catch (SQLException e) {
                showAlert(AlertType.ERROR, "Pagination Error",
                        "Failed to load next page", e.getMessage());
            }
        }
    }

    private void updatePaginationControls() {
        currentPageLabel.setText(String.valueOf(currentPage));
        prevPageButton.setDisable(currentPage <= 1);
        nextPageButton.setDisable(currentPage >= totalPages);
    }

    private void loadPagedCompetitions() throws SQLException {
        // Don't call filterCompetitions() here as it resets currentPage to 1
        // Instead, directly apply the current filters with the current page
        String statusValue = statusFilter.getValue();
        Saison selectedSeason = seasonFilter.getValue();

        // Load filtered competitions without resetting the page
        loadFilteredCompetitions(statusValue, selectedSeason);
    }

    private void updateCompetitionList(List<Competition> competitions) {
        // Clear existing items
        missionListContainer.getChildren().clear();

        if (competitions.isEmpty()) {
            Label emptyLabel = new Label("No missions found.");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6c757d;");
            emptyLabel.getStyleClass().add("no-missions-placeholder");
            emptyLabel.setMaxWidth(Double.MAX_VALUE);
            emptyLabel.setAlignment(Pos.CENTER);
            missionListContainer.getChildren().add(emptyLabel);
            return;
        }

        // Create a card for each competition
        for (Competition competition : competitions) {
            missionListContainer.getChildren().add(createMissionCard(competition));
        }
    }

    private void applyCustomStyling() {
        // Style date pickers
        startDateField.getEditor().setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; -fx-padding: 8px; -fx-font-size: 14px;");
        endDateField.getEditor().setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; -fx-padding: 8px; -fx-font-size: 14px;");

        // Add style for combo boxes
        goalTypeComboBox.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; -fx-border-radius: 5px; -fx-background-radius: 5px;");
        saisonComboBox.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; -fx-border-radius: 5px; -fx-background-radius: 5px;");
        statusComboBox.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; -fx-border-radius: 5px; -fx-background-radius: 5px;");
    }

    private void applyStylesToForm() {
        // Direct styling for form overlay
        formOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-alignment: center;");

        // Direct styling for form container
        missionFormContainer.setStyle("-fx-background-color: white; -fx-padding: 25px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 8); " +
                "-fx-background-radius: 10px; -fx-max-width: 550px; -fx-min-width: 550px; " +
                "-fx-alignment: center; -fx-border-color: #e0e0e0; -fx-border-radius: 10px; " +
                "-fx-border-width: 1px; -fx-spacing: 20;");

        // Style the form header
        for (javafx.scene.Node node : missionFormContainer.getChildren()) {
            if (node instanceof HBox && node.getStyleClass().contains("form-header")) {
                node.setStyle("-fx-padding: 0 0 15px 0; -fx-border-color: transparent transparent #eaeaea transparent; " +
                        "-fx-border-width: 0 0 1px 0; -fx-alignment: center-left;");
                break;
            }
        }

        // Style form title
        formTitleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2d3748;");

        // Style close button
        closeFormButton.setStyle("-fx-background-color: transparent; -fx-background-radius: 50%; -fx-padding: 5px;");

        // Apply styles to GridPane cells
        GridPane gridPane = null;
        for (javafx.scene.Node node : missionFormContainer.getChildren()) {
            if (node instanceof GridPane) {
                gridPane = (GridPane) node;
                break;
            }
        }

        if (gridPane != null) {
            for (javafx.scene.Node node : gridPane.getChildren()) {
                if (node instanceof Label && node.getStyleClass().contains("form-label")) {
                    node.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568; -fx-font-size: 14px;");
                }
            }
        }

        // Style form controls
        missionTitleField.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 5px; -fx-background-radius: 5px; -fx-padding: 8px; -fx-font-size: 14px;");

        missionDescField.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 5px; -fx-background-radius: 5px; -fx-padding: 8px; -fx-font-size: 14px;");

        pointsField.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 5px; -fx-background-radius: 5px; -fx-padding: 8px; -fx-font-size: 14px;");

        goalValueField.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 5px; -fx-background-radius: 5px; -fx-padding: 8px; -fx-font-size: 14px;");

        // Style date pickers
        startDateField.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 5px; -fx-background-radius: 5px;");
        startDateField.getEditor().setStyle("-fx-background-color: #f8f9fa; -fx-padding: 8px; -fx-font-size: 14px;");

        endDateField.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 5px; -fx-background-radius: 5px;");
        endDateField.getEditor().setStyle("-fx-background-color: #f8f9fa; -fx-padding: 8px; -fx-font-size: 14px;");

        // Style combo boxes
        goalTypeComboBox.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 5px; -fx-background-radius: 5px; -fx-padding: 4px;");

        saisonComboBox.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 5px; -fx-background-radius: 5px; -fx-padding: 4px;");

        statusComboBox.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 5px; -fx-background-radius: 5px; -fx-padding: 4px;");

        // Style buttons
        saveButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 5px; -fx-padding: 10px 20px; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);");

        cancelButton.setStyle("-fx-background-color: transparent; -fx-border-color: #6c757d; " +
                "-fx-text-fill: #6c757d; -fx-border-radius: 5px; -fx-background-radius: 5px; " +
                "-fx-padding: 8px 15px; -fx-cursor: hand;");
        // In applyStylesToForm() method, add this after styling other buttons:
        if (generateButton != null) {
            generateButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-background-radius: 5px; -fx-padding: 8px 15px; -fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);");
        }
        // Add hover effects
        setupButtonHoverEffects();
    }

    private void setupButtonHoverEffects() {
        // Save button hover effect
        saveButton.setOnMouseEntered(e ->
                saveButton.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-background-radius: 5px; -fx-padding: 10px 20px; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);"));

        saveButton.setOnMouseExited(e ->
                saveButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-background-radius: 5px; -fx-padding: 10px 20px; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);"));

        // Cancel button hover effect
        cancelButton.setOnMouseEntered(e ->
                cancelButton.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #6c757d; " +
                        "-fx-text-fill: #6c757d; -fx-border-radius: 5px; -fx-background-radius: 5px; " +
                        "-fx-padding: 8px 15px; -fx-cursor: hand;"));

        cancelButton.setOnMouseExited(e ->
                cancelButton.setStyle("-fx-background-color: transparent; -fx-border-color: #6c757d; " +
                        "-fx-text-fill: #6c757d; -fx-border-radius: 5px; -fx-background-radius: 5px; " +
                        "-fx-padding: 8px 15px; -fx-cursor: hand;"));

        // In setupButtonHoverEffects(), add:
        if (generateButton != null) {
            generateButton.setOnMouseEntered(e ->
                    generateButton.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; " +
                            "-fx-background-radius: 5px; -fx-padding: 8px 15px; -fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);"));

            generateButton.setOnMouseExited(e ->
                    generateButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; " +
                            "-fx-background-radius: 5px; -fx-padding: 8px 15px; -fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);"));
        }

        // Close button hover effect
        closeFormButton.setOnMouseEntered(e ->
                closeFormButton.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 50%; -fx-padding: 5px;"));

        closeFormButton.setOnMouseExited(e ->
                closeFormButton.setStyle("-fx-background-color: transparent; -fx-background-radius: 50%; -fx-padding: 5px;"));
    }

    private void setupButtonActions() {
        // Add new competition button
        addButton.setOnAction(event -> showAddForm());

        generateButton.setOnAction(event -> handleAIGeneration());
        // Close form button
        closeFormButton.setOnAction(event -> hideForm());

        // Cancel button
        cancelButton.setOnAction(event -> hideForm());

        // Save button
        saveButton.setOnAction(event -> {
            try {
                if (isEditMode) {
                    updateCompetition();
                } else {
                    createCompetition();
                }
            } catch (SQLException e) {
                showAlert(AlertType.ERROR, "Database Error",
                        "Could not save mission", e.getMessage());
            }
        });

        // Search button
        searchButton.setOnAction(event -> handleSearch());
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().trim().toLowerCase();

        try {
            if (searchTerm.isEmpty()) {
                // If search is empty, load all competitions
                loadAllCompetitions();
                return;
            }

            // Get all competitions and filter by search term
            List<Competition> allCompetitions = competitionService.getAll();
            List<Competition> filteredCompetitions = allCompetitions.stream()
                    .filter(comp ->
                            comp.getNomComp().toLowerCase().contains(searchTerm) ||
                                    comp.getDescComp().toLowerCase().contains(searchTerm))
                    .collect(Collectors.toList());

            updateCompetitionList(filteredCompetitions);

            // Update pagination info
            paginationInfoLabel.setText(String.format("Showing %d results for '%s'",
                    filteredCompetitions.size(), searchTerm));

        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Search Error",
                    "Failed to search missions", e.getMessage());
        }
    }

    private void initializeComboBoxes() throws SQLException {
        // Initialize goal type combo box
        goalTypeComboBox.getItems().addAll(GoalTypeEnum.values());

        // Set up readable names for goal types
        goalTypeComboBox.setConverter(new StringConverter<GoalTypeEnum>() {
            @Override
            public String toString(GoalTypeEnum goalType) {
                if (goalType == null) return "";
                switch (goalType) {
                    case EVENT_COUNT: return "Event Count";
                    case EVENT_LIKES: return "Event Likes";
                    case MEMBER_COUNT: return "Member Count";
                    default: return goalType.toString();
                }
            }

            @Override
            public GoalTypeEnum fromString(String string) {
                return null; // Not needed for combo box
            }
        });

        // Initialize status combo box
        statusComboBox.getItems().addAll("activated", "deactivated");

        // Initialize saison combo box
        List<Saison> saisons = saisonService.getAll();
        saisonComboBox.getItems().addAll(saisons);

        // Set up readable names for saisons
        saisonComboBox.setConverter(new StringConverter<Saison>() {
            @Override
            public String toString(Saison saison) {
                return saison == null ? "" : saison.getNomSaison();
            }

            @Override
            public Saison fromString(String string) {
                return null; // Not needed for combo box
            }
        });
    }

    private void loadAllCompetitions() throws SQLException {
        // Get all competitions from service
        List<Competition> competitions = competitionService.getAll();

        // Calculate total pages
        totalPages = (int) Math.ceil((double) competitions.size() / itemsPerPage);
        totalPages = totalPages == 0 ? 1 : totalPages; // Ensure at least 1 page
        totalPagesLabel.setText(String.valueOf(totalPages));

        // Load paged data
        loadPagedCompetitions();
    }

    private HBox createMissionCard(Competition competition) {
        // Make sure status is up to date before displaying
        competition.updateStatus();

        HBox card = new HBox();
        card.getStyleClass().add("mission-card");
        card.setPrefWidth(Region.USE_COMPUTED_SIZE);
        card.setPrefHeight(Region.USE_COMPUTED_SIZE);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setSpacing(15);
        card.setPadding(new Insets(15));

        // Add hover effect to card
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 1); -fx-border-color: #f0f0f0; -fx-border-radius: 10;");

        // Mission icon
        FontIcon icon = new FontIcon("mdi-trophy");
        icon.setIconSize(32);
        icon.setIconColor(Color.valueOf("#ffb400"));
        icon.getStyleClass().add("mission-icon");

        // Create a container for the icon with padding
        StackPane iconContainer = new StackPane(icon);
        iconContainer.setPadding(new Insets(0, 10, 0, 0));
        iconContainer.setMinWidth(50);

        // Mission content
        VBox content = new VBox(5);
        content.setStyle("-fx-padding: 0 10 0 10;");
        content.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(content, Priority.ALWAYS);

        // Mission header with title and points
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(10);

        Label titleLabel = new Label(competition.getNomComp());
        titleLabel.getStyleClass().add("mission-title");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #333333;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        // Points badge
        Label pointsLabel = new Label(competition.getPoints() + " Points");
        pointsLabel.getStyleClass().add("points-badge");
        pointsLabel.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-padding: 3px 8px; -fx-background-radius: 10px;");

        header.getChildren().addAll(titleLabel, pointsLabel);

        // Mission description
        Label descLabel = new Label(competition.getDescComp());
        descLabel.getStyleClass().add("mission-desc");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(Double.MAX_VALUE);
        descLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");

        // Mission details section
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(15);
        detailsGrid.setVgap(8);
        detailsGrid.setPadding(new Insets(10, 0, 10, 0));

        // Goal info
        Label goalLabel = new Label("Goal:");
        goalLabel.getStyleClass().add("detail-label");
        detailsGrid.add(goalLabel, 0, 0);

        Label goalValueLabel = new Label(String.valueOf(competition.getGoalValue()));
        goalValueLabel.getStyleClass().add("detail-value");
        detailsGrid.add(goalValueLabel, 1, 0);

        // Goal Type info
        Label goalTypeLabel = new Label("Goal Type:");
        goalTypeLabel.getStyleClass().add("detail-label");
        detailsGrid.add(goalTypeLabel, 0, 1);

        Label goalTypeValueLabel = new Label(formatGoalType(competition.getGoalType()));
        goalTypeValueLabel.getStyleClass().add("detail-value");
        detailsGrid.add(goalTypeValueLabel, 1, 1);


        // Status info
        Label statusLabel = new Label("Status:");
        statusLabel.getStyleClass().add("detail-label");
        detailsGrid.add(statusLabel, 0, 2);

        HBox statusContainer = new HBox();
        statusContainer.setAlignment(Pos.CENTER_LEFT);

        Label statusValueLabel = new Label(competition.getStatus());
        statusValueLabel.getStyleClass().addAll("status-badge",
                "activated".equals(competition.getStatus()) ? "status-active" : "status-inactive");

        // Circle indicator next to status for clearer visual indicator
        Circle statusIndicator = new Circle(5);
        statusIndicator.setFill("activated".equals(competition.getStatus()) ? Color.GREEN : Color.RED);
        statusIndicator.setStroke(Color.TRANSPARENT);

        statusContainer.getChildren().addAll(statusIndicator, statusValueLabel);
        statusContainer.setSpacing(5);

        // Add to details grid
        detailsGrid.add(statusContainer, 1, 2);

        // Date info
        Label dateLabel = new Label("Dates:");
        dateLabel.getStyleClass().add("detail-label");
        detailsGrid.add(dateLabel, 0, 3);

        String dateStr = "";
        if (competition.getStartDate() != null) {
            dateStr = formatDate(competition.getStartDate());
            if (competition.getEndDate() != null) {
                dateStr += " to " + formatDate(competition.getEndDate());
            }
        }

        Label dateValueLabel = new Label(dateStr);
        dateValueLabel.getStyleClass().add("detail-value");
        detailsGrid.add(dateValueLabel, 1, 3);

        // Add header, description and details to content container
        content.getChildren().addAll(header, descLabel, detailsGrid);

        // Action buttons
        VBox actions = new VBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setMinWidth(120);

        Button editButton = new Button("Edit");
        editButton.setGraphic(new FontIcon("mdi-pencil"));
        editButton.getStyleClass().add("edit-button");
        editButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 5px; -fx-padding: 8px 15px;");
        editButton.setPadding(new Insets(8, 12, 8, 12));
        editButton.setOnAction(e -> showEditForm(competition));

        // Add hover effect
        editButton.setOnMouseEntered(e -> editButton.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 5px; -fx-padding: 8px 15px;"));
        editButton.setOnMouseExited(e -> editButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 5px; -fx-padding: 8px 15px;"));

        Button deleteButton = new Button("Delete");
        deleteButton.setGraphic(new FontIcon("mdi-delete"));
        deleteButton.getStyleClass().add("delete-button");
        deleteButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-background-radius: 5px; -fx-padding: 8px 15px;");
        deleteButton.setPadding(new Insets(8, 12, 8, 12));
        deleteButton.setOnAction(e -> {
            try {
                deleteCompetition(competition);
            } catch (SQLException ex) {
                showAlert(AlertType.ERROR, "Delete Error",
                        "Could not delete mission", ex.getMessage());
            }
        });

        // Add hover effect
        deleteButton.setOnMouseEntered(e -> deleteButton.setStyle("-fx-background-color: #bb2d3b; -fx-text-fill: white; -fx-background-radius: 5px; -fx-padding: 8px 15px;"));
        deleteButton.setOnMouseExited(e -> deleteButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-background-radius: 5px; -fx-padding: 8px 15px;"));

        actions.getChildren().addAll(editButton, deleteButton);

        // Add all components to card
        card.getChildren().addAll(iconContainer, content, actions);

        // Add hover effect to the entire card
        card.setOnMouseEntered(e ->
                card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2); -fx-border-color: #e6e6e6; -fx-border-radius: 10;"));
        card.setOnMouseExited(e ->
                card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 1); -fx-border-color: #f0f0f0; -fx-border-radius: 10;"));

        return card;
    }

    private String formatGoalType(GoalTypeEnum goalType) {
        if (goalType == null) return "Unknown";

        switch (goalType) {
            case EVENT_COUNT: return "Event Count";
            case EVENT_LIKES: return "Event Likes";
            case MEMBER_COUNT: return "Member Count";
            default: return goalType.toString();
        }
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.toLocalDate().toString();
    }

    private void showAddForm() {
        isEditMode = false;
        formTitleLabel.setText("Add Mission");
        clearForm();
        showForm();
    }

    private void showEditForm(Competition competition) {
        isEditMode = true;
        formTitleLabel.setText("Edit Mission");
        selectedCompetition = competition;

        // Populate form fields
        missionTitleField.setText(competition.getNomComp());
        missionDescField.setText(competition.getDescComp());
        pointsField.setText(String.valueOf(competition.getPoints()));

        if (competition.getStartDate() != null) {
            startDateField.setValue(competition.getStartDate().toLocalDate());
        }

        if (competition.getEndDate() != null) {
            endDateField.setValue(competition.getEndDate().toLocalDate());
        }

        goalValueField.setText(String.valueOf(competition.getGoalValue()));
        goalTypeComboBox.setValue(competition.getGoalType());
        saisonComboBox.setValue(competition.getSaisonId());
        statusComboBox.setValue(competition.getStatus());

        showForm();
    }

    private void showForm() {
        // Apply styles again to ensure everything is styled properly
        applyStylesToForm();

        // Show form and overlay
        formOverlay.setVisible(true);
        formOverlay.setManaged(true);

        // Create fade-in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), formOverlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        // Create scale animation for the form
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), missionFormContainer);
        scaleIn.setFromX(0.95);
        scaleIn.setFromY(0.95);
        scaleIn.setToX(1);
        scaleIn.setToY(1);

        // Play animations
        fadeIn.play();
        scaleIn.play();
    }

    private void hideForm() {
        // Create fade-out animation
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), formOverlay);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        // Create scale-out animation
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(200), missionFormContainer);
        scaleOut.setFromX(1);
        scaleOut.setFromY(1);
        scaleOut.setToX(0.95);
        scaleOut.setToY(0.95);

        // When animation finishes, hide the overlay
        fadeOut.setOnFinished(event -> {
            formOverlay.setVisible(false);
            formOverlay.setManaged(false);
            clearForm();
        });

        // Play animations
        fadeOut.play();
        scaleOut.play();
    }

    private void clearForm() {
        missionTitleField.clear();
        missionDescField.clear();
        pointsField.clear();
        startDateField.setValue(null);
        endDateField.setValue(null);
        goalValueField.clear();
        goalTypeComboBox.getSelectionModel().clearSelection();
        saisonComboBox.getSelectionModel().clearSelection();
        statusComboBox.getSelectionModel().clearSelection();
        selectedCompetition = null;
    }

    private void createCompetition() throws SQLException {
        // Check if AI generation is needed before validation
        if ((missionTitleField.getText().trim().isEmpty() || missionDescField.getText().trim().isEmpty()) &&
                goalTypeComboBox.getValue() != null &&
                !goalValueField.getText().isEmpty() &&
                !pointsField.getText().isEmpty()) {

            try {
                int goalValue = Integer.parseInt(goalValueField.getText());
                int points = Integer.parseInt(pointsField.getText());
                GoalTypeEnum goalType = goalTypeComboBox.getValue();
                Saison saison = saisonComboBox.getValue();

                // Show loading indicator
                Platform.runLater(() -> {
                    missionTitleField.setPromptText("Generating...");
                    missionDescField.setPromptText("Generating...");
                });

                // Generate content
                AIContentGeneratorService.GeneratedContent content =
                        aiService.generateMissionContent(goalType, goalValue, points, saison);

                // Update UI with generated content
                Platform.runLater(() -> {
                    if (missionTitleField.getText().trim().isEmpty()) {
                        missionTitleField.setText(content.title);
                    }
                    if (missionDescField.getText().trim().isEmpty()) {
                        missionDescField.setText(content.description);
                    }

                    // Reset prompts
                    missionTitleField.setPromptText("Enter mission title");
                    missionDescField.setPromptText("Enter mission description");

                    // Show a dialog to let user review generated content
                    Alert reviewAlert = new Alert(AlertType.INFORMATION);
                    reviewAlert.setTitle("AI Generated Content");
                    reviewAlert.setHeaderText("AI has generated content for your mission");
                    reviewAlert.setContentText("Please review the generated title and description. You can modify them if needed before saving.");

                    // Style the dialog
                    DialogPane dialogPane = reviewAlert.getDialogPane();
                    dialogPane.setStyle(
                            "-fx-background-color: white;" +
                                    "-fx-border-color: #0dcaf0;" +
                                    "-fx-border-width: 1px;" +
                                    "-fx-border-radius: 8px;" +
                                    "-fx-background-radius: 8px;" +
                                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
                    );

                    Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
                    okButton.setStyle(
                            "-fx-background-color: #0dcaf0;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-background-radius: 4px;" +
                                    "-fx-font-weight: bold;"
                    );

                    reviewAlert.showAndWait();
                });

                return; // Let user review and manually click save again

            } catch (NumberFormatException e) {
                // Invalid number format in fields - continue to validation
            } catch (Exception e) {
                showAlert(AlertType.WARNING, "AI Generation Failed",
                        "Could not generate content", "Please enter the title and description manually.");

                // Reset prompts
                missionTitleField.setPromptText("Enter mission title");
                missionDescField.setPromptText("Enter mission description");
            }
        }

        // Validate form
        if (!validateForm()) {
            return;
        }

        Competition newCompetition = new Competition();

        // Set competition properties from form
        newCompetition.setNomComp(missionTitleField.getText());
        newCompetition.setDescComp(missionDescField.getText());
        newCompetition.setPoints(Integer.parseInt(pointsField.getText()));

        if (startDateField.getValue() != null) {
            newCompetition.setStartDate(LocalDateTime.of(startDateField.getValue(), LocalTime.MIDNIGHT));
        }

        if (endDateField.getValue() != null) {
            newCompetition.setEndDate(LocalDateTime.of(endDateField.getValue(), LocalTime.of(23, 59, 59)));
        }

        newCompetition.setGoalValue(Integer.parseInt(goalValueField.getText()));
        newCompetition.setGoalType(goalTypeComboBox.getValue());
        newCompetition.setSaisonId(saisonComboBox.getValue());

        competitionService.add(newCompetition);
        showAlert(AlertType.INFORMATION, "Mission Created",
                "The mission was created successfully", "");
        hideForm();
        loadAllCompetitions();
        updateStats();
    }

    private void updateCompetition() throws SQLException {
        // Validate form
        if (!validateForm()) {
            return;
        }

        // Set competition properties from form
        selectedCompetition.setNomComp(missionTitleField.getText());
        selectedCompetition.setDescComp(missionDescField.getText());
        selectedCompetition.setPoints(Integer.parseInt(pointsField.getText()));

        if (startDateField.getValue() != null) {
            selectedCompetition.setStartDate(LocalDateTime.of(startDateField.getValue(), LocalTime.MIDNIGHT));
        } else {
            selectedCompetition.setStartDate(null);
        }

        if (endDateField.getValue() != null) {
            selectedCompetition.setEndDate(LocalDateTime.of(endDateField.getValue(), LocalTime.of(23, 59, 59)));
        } else {
            selectedCompetition.setEndDate(null);
        }

        selectedCompetition.setGoalValue(Integer.parseInt(goalValueField.getText()));
        selectedCompetition.setGoalType(goalTypeComboBox.getValue());
        selectedCompetition.setSaisonId(saisonComboBox.getValue());

        competitionService.update(selectedCompetition);
        showAlert(AlertType.INFORMATION, "Mission Updated",
                "The mission was updated successfully", "");
        hideForm();
        loadAllCompetitions();
        updateStats();
    }

    private boolean validateForm() {
        StringBuilder errorMessage = new StringBuilder();

        if (missionTitleField.getText() == null || missionTitleField.getText().trim().isEmpty()) {
            errorMessage.append("• Please enter a mission title.\n");
        }

        // Validate points (must be a positive integer)
        try {
            int points = Integer.parseInt(pointsField.getText());
            if (points <= 0) {
                errorMessage.append("• Points must be a positive number.\n");
            }
        } catch (NumberFormatException e) {
            errorMessage.append("• Please enter a valid number for points.\n");
        }

        // Validate goal value
        try {
            int goalValue = Integer.parseInt(goalValueField.getText());
            if (goalValue <= 0) {
                errorMessage.append("• Goal value must be a positive number.\n");
            }
        } catch (NumberFormatException e) {
            errorMessage.append("• Please enter a valid number for goal value.\n");
        }

        // Validate that start date is before end date if both are set
        if (startDateField.getValue() != null && endDateField.getValue() != null) {
            if (startDateField.getValue().isAfter(endDateField.getValue())) {
                errorMessage.append("• Start date must be before end date.\n");
            }
        }
        // Validate dates are provided
        if (startDateField.getValue() == null) {
            errorMessage.append("• Please select a start date.\n");
        }

        if (endDateField.getValue() == null) {
            errorMessage.append("• Please select an end date.\n");
        }
        // Validate required dropdowns
        if (goalTypeComboBox.getValue() == null) {
            errorMessage.append("• Please select a goal type.\n");
        }

        if (saisonComboBox.getValue() == null) {
            errorMessage.append("• Please select a season.\n");
        }



        if (errorMessage.length() > 0) {
            String title = "Form Validation";
            String header = "Please correct the following issues:";
            String content = errorMessage.toString();

            // Create custom styled alert for validation errors
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);

            // Style the dialog pane
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-border-color: #ffc107;" +
                            "-fx-border-width: 1px;" +
                            "-fx-border-radius: 8px;" +
                            "-fx-background-radius: 8px;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
            );

            // Set custom button style
            Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
            okButton.setStyle(
                    "-fx-background-color: #ffc107;" +
                            "-fx-text-fill: white;" +
                            "-fx-background-radius: 4px;" +
                            "-fx-font-weight: bold;"
            );

            alert.showAndWait();
            return false;
        }

        return true;
    }

    private void deleteCompetition(Competition competition) throws SQLException {
        if (competition == null) {
            showAlert(AlertType.WARNING, "No Selection",
                    "Please select a mission to delete", "");
            return;
        }

        // Confirm deletion
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Are you sure you want to delete this mission?");
        confirmAlert.setContentText("Mission: " + competition.getNomComp());

        // Style the dialog pane
        DialogPane dialogPane = confirmAlert.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #dc3545;" +
                        "-fx-border-width: 1px;" +
                        "-fx-border-radius: 8px;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
        );

        Button yesButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        yesButton.setStyle(
                "-fx-background-color: #dc3545;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 4px;" +
                        "-fx-font-weight: bold;"
        );

        Button noButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        noButton.setStyle(
                "-fx-background-color: #6c757d;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 4px;" +
                        "-fx-font-weight: bold;"
        );

        if (confirmAlert.showAndWait().get() == ButtonType.OK) {
            competitionService.delete(competition.getId());
            showAlert(AlertType.INFORMATION, "Mission Deleted",
                    "The mission was deleted successfully", "");
            loadAllCompetitions();
            updateStats();
        }
    }

    // Update the pagination info
    private void updatePaginationInfo() {
        try {
            // Get filtered count based on current filters
            String statusValue = statusFilter.getValue();
            Saison selectedSeason = seasonFilter.getValue();

            List<Competition> allCompetitions = competitionService.getAll();
            List<Competition> filteredCompetitions = new ArrayList<>(allCompetitions);

            // Apply status filter
            if (!"All Status".equals(statusValue)) {
                String statusToFilter = "Active".equals(statusValue) ? "activated" : "deactivated";
                filteredCompetitions = filteredCompetitions.stream()
                        .filter(c -> statusToFilter.equals(c.getStatus()))
                        .collect(Collectors.toList());
            }

            // Apply season filter
            if (selectedSeason != null && selectedSeason.getId() != -1) {
                filteredCompetitions = filteredCompetitions.stream()
                        .filter(c -> c.getSaisonId() != null && c.getSaisonId().getId() == selectedSeason.getId())
                        .collect(Collectors.toList());
            }

            // Calculate start and end index for current page
            int startIndex = (currentPage - 1) * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, filteredCompetitions.size());

            // Update pagination info
            if (filteredCompetitions.isEmpty()) {
                paginationInfoLabel.setText("No missions found");
            } else {
                paginationInfoLabel.setText(String.format("Showing %d to %d of %d missions",
                        startIndex + 1, endIndex, filteredCompetitions.size()));
            }
        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Pagination Error",
                    "Could not update pagination information", e.getMessage());
        }
    }

    // Navigation methods for sidebar
    @FXML
    public void showUserManagement() {
        try {
            navigateTo("/com/esprit/views/adminDashboard.fxml");
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Navigation Error",
                    "Could not navigate to User Management", e.getMessage());
        }
    }

    @FXML
    public void showClubManagement() {
        try {
            navigateTo("/com/esprit/views/adminClubs.fxml");
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Navigation Error",
                    "Could not navigate to Club Management", e.getMessage());
        }
    }

    @FXML
    public void showEventManagement() {
        try {
            navigateTo("/com/esprit/views/adminEvents.fxml");
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Navigation Error",
                    "Could not navigate to Event Management", e.getMessage());
        }
    }

    @FXML
    public void showProductOrders() {
        try {
            navigateTo("/com/esprit/views/adminProducts.fxml");
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Navigation Error",
                    "Could not navigate to Products & Orders", e.getMessage());
        }
    }

    @FXML
    public void showCompetition() {
        // Already on this page, no navigation needed
    }

    @FXML
    public void showSurvey() {
        try {
            navigateTo("/com/esprit/views/adminSurvey.fxml");
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Navigation Error",
                    "Could not navigate to Survey Management", e.getMessage());
        }
    }

    @FXML
    public void navigateToProfile() {
        try {
            navigateTo("/com/esprit/views/adminProfile.fxml");
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Navigation Error",
                    "Could not navigate to Profile", e.getMessage());
        }
    }

    @FXML
    public void handleLogout() {
        // Confirm logout
        Alert confirmLogout = new Alert(AlertType.CONFIRMATION);
        confirmLogout.setTitle("Confirm Logout");
        confirmLogout.setHeaderText("Are you sure you want to logout?");
        confirmLogout.setContentText("Your current session will be closed.");

        // Style the confirmation dialog
        DialogPane dialogPane = confirmLogout.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #dce3f0;" +
                        "-fx-border-width: 1px;" +
                        "-fx-border-radius: 8px;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
        );

        if (confirmLogout.showAndWait().get() == ButtonType.OK) {
            try {
                // Navigate to login screen
                navigateTo("/com/esprit/views/login.fxml");
            } catch (IOException e) {
                showAlert(AlertType.ERROR, "Logout Error",
                        "Could not navigate to login screen", e.getMessage());
            }
        }
    }

    private void navigateTo(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();
        Scene scene = contentArea.getScene();
        scene.setRoot(root);
    }

    private void showAlert(AlertType alertType, String title, String message, String details) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);

        if (details != null && !details.isEmpty()) {
            alert.setContentText(message + "\n\nDetails: " + details);
        } else {
            alert.setContentText(message);
        }

        // Style the dialog pane based on alert type
        DialogPane dialogPane = alert.getDialogPane();
        String backgroundColor = "#ffffff";
        String borderColor = "#dce3f0";

        switch (alertType) {
            case ERROR:
                borderColor = "#dc3545";
                break;
            case WARNING:
                borderColor = "#ffc107";
                break;
            case INFORMATION:
                borderColor = "#0dcaf0";
                break;
            case CONFIRMATION:
                borderColor = "#20c997";
                break;
            default:
                break;
        }

        dialogPane.setStyle(
                "-fx-background-color: " + backgroundColor + ";" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-width: 1px;" +
                        "-fx-border-radius: 8px;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
        );

        // Style the content text
        Label content = (Label) dialogPane.lookup(".content");
        if (content != null) {
            content.setStyle(
                    "-fx-font-size: 14px;" +
                            "-fx-text-fill: #4b5c7b;" +
                            "-fx-padding: 10 0 10 0;"
            );
        }

        // Style the buttons
        dialogPane.getButtonTypes().forEach(buttonType -> {
            Button button = (Button) dialogPane.lookupButton(buttonType);
            String buttonColor = "#4a90e2";
            String textColor = "white";

            switch (alertType) {
                case ERROR:
                    buttonColor = "#dc3545";
                    break;
                case WARNING:
                    buttonColor = "#ffc107";
                    break;
                case INFORMATION:
                    buttonColor = "#0dcaf0";
                    break;
                case CONFIRMATION:
                    if (buttonType == ButtonType.OK) {
                        buttonColor = "#20c997";
                    } else {
                        buttonColor = "#6c757d";
                    }
                    break;
                default:
                    break;
            }
            final String finalButtonColor = buttonColor;
            button.setStyle(
                    "-fx-background-color: " + finalButtonColor + ";" +
                            "-fx-text-fill: " + textColor + ";" +
                            "-fx-background-radius: 4px;" +
                            "-fx-padding: 8 15;" +
                            "-fx-font-weight: bold;" +
                            "-fx-cursor: hand;"
            );

            // Add hover effect
            button.setOnMouseEntered(e -> {
                // Darken the button color when hovered
                button.setStyle(
                        "-fx-background-color: derive(" + finalButtonColor + ", -10%);" +
                                "-fx-text-fill: " + textColor + ";" +
                                "-fx-background-radius: 4px;" +
                                "-fx-padding: 8 15;" +
                                "-fx-font-weight: bold;" +
                                "-fx-cursor: hand;"
                );
            });

            button.setOnMouseExited(e -> {
                // Restore original color when not hovered
                button.setStyle(
                        "-fx-background-color: " + finalButtonColor + ";" +
                                "-fx-text-fill: " + textColor + ";" +
                                "-fx-background-radius: 4px;" +
                                "-fx-padding: 8 15;" +
                                "-fx-font-weight: bold;" +
                                "-fx-cursor: hand;"
                );
            });
        });

        alert.showAndWait();
    }

    @FXML
    private void showStatistics() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/CompetitionStatistics.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setTitle("Gamification Statistics Dashboard");
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Navigation Error",
                    "Could not open statistics dashboard", e.getMessage());
        }
    }

    @FXML
    private void showSeasonManagement() {
        try {
            navigateTo("/com/esprit/views/AdminSaisons.fxml");
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Navigation Error",
                    "Could not navigate to Season Management", e.getMessage());
        }
    }
    private void handleAIGeneration() {
        // Check if essential fields are filled but name/description are empty
        if (goalTypeComboBox.getValue() != null &&
                !goalValueField.getText().isEmpty() &&
                !pointsField.getText().isEmpty() &&
                (missionTitleField.getText().trim().isEmpty() || missionDescField.getText().trim().isEmpty())) {

            try {
                int goalValue = Integer.parseInt(goalValueField.getText());
                int points = Integer.parseInt(pointsField.getText());
                GoalTypeEnum goalType = goalTypeComboBox.getValue();
                Saison saison = saisonComboBox.getValue();

                // Show loading indicator (optional)
                Platform.runLater(() -> {
                    missionTitleField.setPromptText("Generating...");
                    missionDescField.setPromptText("Generating...");
                });

                // Generate content in background thread
                new Thread(() -> {
                    try {
                        AIContentGeneratorService.GeneratedContent content =
                                aiService.generateMissionContent(goalType, goalValue, points, saison);

                        // Update UI on JavaFX thread
                        Platform.runLater(() -> {
                            if (missionTitleField.getText().trim().isEmpty()) {
                                missionTitleField.setText(content.title);
                            }
                            if (missionDescField.getText().trim().isEmpty()) {
                                missionDescField.setText(content.description);
                            }

                            // Reset prompts
                            missionTitleField.setPromptText("Enter mission title");
                            missionDescField.setPromptText("Enter mission description");
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            showAlert(AlertType.WARNING, "AI Generation Failed",
                                    "Could not generate content", "Using default values instead.");

                            // Reset prompts
                            missionTitleField.setPromptText("Enter mission title");
                            missionDescField.setPromptText("Enter mission description");
                        });
                    }
                }).start();

            } catch (NumberFormatException e) {
                // Invalid number format in fields
            }
        }
    }
}
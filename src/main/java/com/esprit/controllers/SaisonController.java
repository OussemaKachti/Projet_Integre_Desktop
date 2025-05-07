package com.esprit.controllers;

import com.esprit.models.Saison;
import com.esprit.services.SaisonService;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class SaisonController {

    private final SaisonService saisonService = new SaisonService();
    private Saison selectedSaison;
    private File selectedImageFile;
    private String uploadDirectory = "uploads/images/";
    private boolean isEditMode = false;
    private int currentPage = 1;
    private int itemsPerPage = 5;
    private int totalPages = 1;

    // Main components
    @FXML private BorderPane contentArea;
    @FXML private VBox seasonListContainer;
    @FXML private VBox seasonFormContainer;
    @FXML private StackPane contentStackPane;

    // Header and Stats components
    @FXML private Label contentTitle;
    @FXML private Label dateLabel;
    @FXML private Label totalSeasonsLabel;
    @FXML private Label activeSeasonsLabel;
    @FXML private Label upcomingSeasonsLabel;
    @FXML private Label adminNameLabel;

    // Search components
    @FXML private TextField searchField;
    @FXML private Button searchButton;

    // Filter components
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> yearFilter;

    // Form fields
    @FXML private TextField saisonNameField;
    @FXML private TextArea saisonDescField;
    @FXML private DatePicker saisonDateField;
    @FXML private ImageView imagePreview;
    @FXML private Label selectedImageLabel;
    @FXML private Label formTitleLabel;
    @FXML private Label paginationInfoLabel;

    // Buttons
    @FXML private Button saveButton;
    @FXML private Button addButton;
    @FXML private Button cancelButton;
    @FXML private Button closeFormButton;
    @FXML private Button chooseImageButton;
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
    @FXML private Button CompetitionManagementButton;

    // Pagination labels
    @FXML private Label currentPageLabel;
    @FXML private Label totalPagesLabel;
    @FXML private StackPane formOverlay;

    @FXML
    public void initialize() {
        try {
            // Set current date in header
            LocalDate now = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
            dateLabel.setText("Today: " + now.format(formatter));

            // Set admin name
            adminNameLabel.setText("Admin User"); // Replace with actual admin name if available

            // Initialize directory for image uploads
            initializeUploadDirectory();

            // Setup filters
            setupFilters();

            // Load all seasons and update stats
            loadAllSeasons();
            updateStats();

            // Set up button actions
            setupButtonActions();

            // Set up pagination
            setupPagination();

            // Apply custom styling
            applyCustomStyling();
        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Initialization Error",
                    "Failed to initialize the season management", e.getMessage());
        }
        // Delay applying styles until JavaFX rendering is complete
        Platform.runLater(() -> {
            System.out.println("Applying styles to form elements");
            applyStylesToForm();
        });
    }
    private void applyStylesToForm() {
        // Direct styling for form overlay
        formOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-alignment: center;");

        // Direct styling for form container
        seasonFormContainer.setStyle("-fx-background-color: white; -fx-padding: 25px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 8); " +
                "-fx-background-radius: 10px; -fx-max-width: 550px; -fx-min-width: 550px;-fx-max-height: 600px; " +
                "-fx-alignment: center; -fx-border-color: #e0e0e0; -fx-border-radius: 10px; " +
                "-fx-border-width: 1px; -fx-spacing: 20;");

        // Style the form header
        for (javafx.scene.Node node : seasonFormContainer.getChildren()) {
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
        for (javafx.scene.Node node : seasonFormContainer.getChildren()) {
            if (node instanceof GridPane) {
                gridPane = (GridPane) node;
                break;
            }
        }

        if (gridPane != null) {
            for (javafx.scene.Node node : gridPane.getChildren()) {
                if (node instanceof Label) {
                    node.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568; -fx-font-size: 14px;");
                }
            }
        }

        // Style form controls
        saisonNameField.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 5px; -fx-background-radius: 5px; -fx-padding: 8px; -fx-font-size: 14px;");

        saisonDescField.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 5px; -fx-background-radius: 5px; -fx-padding: 8px; -fx-font-size: 14px;");

        // Style the date picker
        saisonDateField.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 5px; -fx-background-radius: 5px;");

        // Style the editor inside DatePicker
        saisonDateField.getEditor().setStyle("-fx-background-color: #f8f9fa; -fx-padding: 8px; -fx-font-size: 14px;");

        // Style image preview
        imagePreview.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; " +
                "-fx-border-radius: 8px; -fx-padding: 5px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");

        // Style the file label
        selectedImageLabel.setStyle("-fx-text-fill: #6c757d; -fx-padding: 5px 0;");

        // Style buttons
        saveButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 5px; -fx-padding: 10px 20px; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);");

        cancelButton.setStyle("-fx-background-color: transparent; -fx-border-color: #6c757d; " +
                "-fx-text-fill: #6c757d; -fx-border-radius: 5px; -fx-background-radius: 5px; " +
                "-fx-padding: 8px 15px; -fx-cursor: hand;");

        chooseImageButton.setStyle("-fx-background-color: transparent; -fx-border-color: #3b82f6; " +
                "-fx-text-fill: #3b82f6; -fx-border-radius: 5px; -fx-background-radius: 5px; " +
                "-fx-padding: 8px 15px; -fx-cursor: hand;");

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

        // Choose image button hover effect
        chooseImageButton.setOnMouseEntered(e ->
                chooseImageButton.setStyle("-fx-background-color: #f0f7ff; -fx-border-color: #3b82f6; " +
                        "-fx-text-fill: #3b82f6; -fx-border-radius: 5px; -fx-background-radius: 5px; " +
                        "-fx-padding: 8px 15px; -fx-cursor: hand;"));

        chooseImageButton.setOnMouseExited(e ->
                chooseImageButton.setStyle("-fx-background-color: transparent; -fx-border-color: #3b82f6; " +
                        "-fx-text-fill: #3b82f6; -fx-border-radius: 5px; -fx-background-radius: 5px; " +
                        "-fx-padding: 8px 15px; -fx-cursor: hand;"));

        // Close button hover effect
        closeFormButton.setOnMouseEntered(e ->
                closeFormButton.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 50%; -fx-padding: 5px;"));

        closeFormButton.setOnMouseExited(e ->
                closeFormButton.setStyle("-fx-background-color: transparent; -fx-background-radius: 50%; -fx-padding: 5px;"));
    }

    private void setupFilters() {
        try {
            // Setup status filter
            statusFilter.getItems().clear();
            statusFilter.getItems().addAll("All Status", "Active", "Upcoming", "Completed");
            statusFilter.setValue("All Status");

            // Setup year filter
            yearFilter.getItems().clear();
            yearFilter.getItems().add("All Years");

            // Get all seasons to populate year filter
            List<Saison> allSeasons = saisonService.getAll();
            Set<Integer> yearSet = new HashSet<>();

            // Extract years from season end dates
            for (Saison saison : allSeasons) {
                if (saison.getDateFin() != null) {
                    yearSet.add(saison.getDateFin().getYear());
                }
            }

            // Add current and next year if not already included
            LocalDate now = LocalDate.now();
            yearSet.add(now.getYear());
            yearSet.add(now.getYear() + 1);

            // Convert to sorted list and add to combo box
            List<Integer> years = new ArrayList<>(yearSet);
            Collections.sort(years);
            for (Integer year : years) {
                yearFilter.getItems().add(String.valueOf(year));
            }

            yearFilter.setValue("All Years");

            // Set filter actions
            statusFilter.setOnAction(e -> filterSeasons());
            yearFilter.setOnAction(e -> filterSeasons());
        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Filter Setup Error",
                    "Could not initialize filters", e.getMessage());
        }
    }

    private void filterSeasons() {
        try {
            String statusValue = statusFilter.getValue();
            String yearValue = yearFilter.getValue();

            // Reset to first page when filtering
            currentPage = 1;
            currentPageLabel.setText("1");

            // Load filtered seasons
            loadFilteredSeasons(statusValue, yearValue);

            // Update the pagination info label
            updatePaginationInfo();
        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Filter Error",
                    "Could not apply filters", e.getMessage());
        }
    }

    private void loadFilteredSeasons(String status, String year) throws SQLException {
        // Get all seasons first
        List<Saison> allSeasons = saisonService.getAll();
        List<Saison> filteredSeasons = new ArrayList<>(allSeasons);

        LocalDate now = LocalDate.now();

        // Filter by status if not "All Status"
        if (!"All Status".equals(status)) {
            switch (status) {
                case "Active":
                    // Active seasons: end date is in the future but within next month
                    filteredSeasons = filteredSeasons.stream()
                            .filter(s -> s.getDateFin() != null
                                    && s.getDateFin().isAfter(now)
                                    && s.getDateFin().isBefore(now.plusMonths(1)))
                            .collect(Collectors.toList());
                    break;
                case "Upcoming":
                    // Upcoming seasons: end date is more than a month in the future
                    filteredSeasons = filteredSeasons.stream()
                            .filter(s -> s.getDateFin() != null
                                    && s.getDateFin().isAfter(now.plusMonths(1)))
                            .collect(Collectors.toList());
                    break;
                case "Completed":
                    // Completed seasons: end date is in the past
                    filteredSeasons = filteredSeasons.stream()
                            .filter(s -> s.getDateFin() != null
                                    && s.getDateFin().isBefore(now))
                            .collect(Collectors.toList());
                    break;
            }
        }

        // Filter by year if not "All Years"
        if (!"All Years".equals(year)) {
            int selectedYear = Integer.parseInt(year);
            filteredSeasons = filteredSeasons.stream()
                    .filter(s -> s.getDateFin() != null
                            && s.getDateFin().getYear() == selectedYear)
                    .collect(Collectors.toList());
        }

        // Calculate total pages for filtered results
        totalPages = (int) Math.ceil((double) filteredSeasons.size() / itemsPerPage);
        totalPages = totalPages == 0 ? 1 : totalPages; // Ensure at least 1 page
        totalPagesLabel.setText(String.valueOf(totalPages));

        // Update pagination controls
        prevPageButton.setDisable(currentPage <= 1);
        nextPageButton.setDisable(currentPage >= totalPages);

        // Calculate start and end index for current page
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, filteredSeasons.size());

        // Get paged subset of filtered data
        List<Saison> pagedFilteredSeasons =
                filteredSeasons.isEmpty() ? new ArrayList<>() :
                        filteredSeasons.subList(startIndex, endIndex);

        // Update the UI with filtered and paged data
        updateSeasonList(pagedFilteredSeasons);

        // Update the pagination info text
        paginationInfoLabel.setText(String.format("Showing %d to %d of %d seasons",
                filteredSeasons.isEmpty() ? 0 : startIndex + 1,
                endIndex,
                filteredSeasons.size()));
    }
    private void updatePaginationInfo() {
        try {
            // Get filtered count based on current filters
            String statusValue = statusFilter.getValue();
            String yearValue = yearFilter.getValue();

            List<Saison> allSeasons = saisonService.getAll();
            List<Saison> filteredSeasons = new ArrayList<>(allSeasons);

            LocalDate now = LocalDate.now();

            // Apply status filter
            if (!"All Status".equals(statusValue)) {
                switch (statusValue) {
                    case "Active":
                        filteredSeasons = filteredSeasons.stream()
                                .filter(s -> s.getDateFin() != null
                                        && s.getDateFin().isAfter(now)
                                        && s.getDateFin().isBefore(now.plusMonths(1)))
                                .collect(Collectors.toList());
                        break;
                    case "Upcoming":
                        filteredSeasons = filteredSeasons.stream()
                                .filter(s -> s.getDateFin() != null
                                        && s.getDateFin().isAfter(now.plusMonths(1)))
                                .collect(Collectors.toList());
                        break;
                    case "Completed":
                        filteredSeasons = filteredSeasons.stream()
                                .filter(s -> s.getDateFin() != null
                                        && s.getDateFin().isBefore(now))
                                .collect(Collectors.toList());
                        break;
                }
            }

            // Apply year filter
            if (!"All Years".equals(yearValue)) {
                int selectedYear = Integer.parseInt(yearValue);
                filteredSeasons = filteredSeasons.stream()
                        .filter(s -> s.getDateFin() != null
                                && s.getDateFin().getYear() == selectedYear)
                        .collect(Collectors.toList());
            }

            // Calculate start and end index for current page
            int startIndex = (currentPage - 1) * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, filteredSeasons.size());

            // Update pagination info
            if (filteredSeasons.isEmpty()) {
                paginationInfoLabel.setText("No seasons found");
            } else {
                paginationInfoLabel.setText(String.format("Showing %d to %d of %d seasons",
                        startIndex + 1, endIndex, filteredSeasons.size()));
            }
        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Pagination Error",
                    "Could not update pagination information", e.getMessage());
        }
    }

    private void updateStats() throws SQLException {
        // Count total seasons
        List<Saison> allSeasons = saisonService.getAll();
        totalSeasonsLabel.setText(String.valueOf(allSeasons.size()));

        // Count active seasons (those with end date in the future)
        LocalDate now = LocalDate.now();
        long activeCount = allSeasons.stream()
                .filter(s -> s.getDateFin() != null && s.getDateFin().isAfter(now))
                .count();
        activeSeasonsLabel.setText(String.valueOf(activeCount));

        // Count upcoming seasons (placeholder logic - define what makes a season "upcoming")
        // This is just an example - modify according to your business logic
        long upcomingCount = allSeasons.stream()
                .filter(s -> s.getDateFin() != null && s.getDateFin().isAfter(now.plusMonths(1)))
                .count();
        upcomingSeasonsLabel.setText(String.valueOf(upcomingCount));
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
            try {
                loadPagedSeasons();
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
            try {
                loadPagedSeasons();
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

    private void loadPagedSeasons() throws SQLException {
        List<Saison> allSeasons = saisonService.getAll();

        // Calculate total pages
        totalPages = (int) Math.ceil((double) allSeasons.size() / itemsPerPage);
        totalPagesLabel.setText(String.valueOf(totalPages));

        // Calculate start and end index for the current page
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allSeasons.size());

        // Get sublist for current page
        List<Saison> pageItems = allSeasons.subList(startIndex, endIndex);

        // Update the UI with paged seasons
        updateSeasonList(pageItems);

        // Update pagination info label
        paginationInfoLabel.setText(String.format("Showing %d to %d of %d seasons",
                startIndex + 1, endIndex, allSeasons.size()));
    }

    private void updateSeasonList(List<Saison> seasons) {
        // Clear existing items
        seasonListContainer.getChildren().clear();

        if (seasons.isEmpty()) {
            Label emptyLabel = new Label("No seasons found.");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6c757d;");
            emptyLabel.getStyleClass().add("no-seasons-placeholder");
            emptyLabel.setMaxWidth(Double.MAX_VALUE);
            emptyLabel.setAlignment(Pos.CENTER);
            seasonListContainer.getChildren().add(emptyLabel);
            return;
        }

        // Create a card for each season
        for (Saison saison : seasons) {
            seasonListContainer.getChildren().add(createSeasonCard(saison));
        }
    }

    private void applyCustomStyling() {
        // Add specific styling for form elements
        saisonDateField.getEditor().setStyle("-fx-background-color: #F3F4F6; -fx-border-color: #e4e9f0;");

        // Style the imagePreview
        imagePreview.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e4e9f0; -fx-border-radius: 4px;");

        // Add drop shadow to season form container
        seasonFormContainer.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 4);");
    }

    private void setupButtonActions() {
        // Add new season button
        addButton.setOnAction(event -> showAddForm());

        // Close form button
        closeFormButton.setOnAction(event -> hideForm());

        // Cancel button
        cancelButton.setOnAction(event -> hideForm());

        // Save button
        saveButton.setOnAction(event -> {
            try {
                if (isEditMode) {
                    updateSaison();
                } else {
                    createSaison();
                }
            } catch (SQLException e) {
                showAlert(AlertType.ERROR, "Database Error",
                        "Could not save season", e.getMessage());
            }
        });

        // Choose image button
        chooseImageButton.setOnAction(event -> chooseImage());

        // Search button
        searchButton.setOnAction(event -> handleSearch());
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().trim().toLowerCase();

        try {
            if (searchTerm.isEmpty()) {
                // If search is empty, load all seasons
                loadAllSeasons();
                return;
            }

            // Get all seasons and filter by search term
            List<Saison> allSeasons = saisonService.getAll();
            List<Saison> filteredSeasons = allSeasons.stream()
                    .filter(saison ->
                            saison.getNomSaison().toLowerCase().contains(searchTerm) ||
                                    saison.getDescSaison().toLowerCase().contains(searchTerm))
                    .toList();

            updateSeasonList(filteredSeasons);

            // Update pagination info
            paginationInfoLabel.setText(String.format("Showing %d results for '%s'",
                    filteredSeasons.size(), searchTerm));

        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Search Error",
                    "Failed to search seasons", e.getMessage());
        }
    }

    private void initializeUploadDirectory() {
        // Create the uploads directory if it doesn't exist
        try {
            Path path = Paths.get(uploadDirectory);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Directory Error",
                    "Could not create upload directory", e.getMessage());
        }
    }

    private void loadAllSeasons() throws SQLException {
        // Get all seasons from service
        List<Saison> saisons = saisonService.getAll();

        // Calculate total pages
        totalPages = (int) Math.ceil((double) saisons.size() / itemsPerPage);
        totalPagesLabel.setText(String.valueOf(totalPages));

        // Load paged data
        loadPagedSeasons();
    }

    private HBox createSeasonCard(Saison saison) {
        HBox card = new HBox();
        card.getStyleClass().add("season-card");
        card.setPrefWidth(Region.USE_COMPUTED_SIZE);
        card.setPrefHeight(Region.USE_COMPUTED_SIZE);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setSpacing(15);
        card.setPadding(new Insets(15));

        // Add hover effect to card
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 1); -fx-border-color: #f0f0f0; -fx-border-radius: 10;");

        // Season icon
        FontIcon icon = new FontIcon("mdi-calendar-clock");
        icon.setIconSize(32);
        icon.setIconColor(javafx.scene.paint.Color.web("#1e90ff"));
        icon.getStyleClass().add("season-icon");

        // Create a container for the icon with padding
        StackPane iconContainer = new StackPane(icon);
        iconContainer.setPadding(new Insets(0, 10, 0, 0));
        iconContainer.setMinWidth(50);

        // Image view for season image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(120);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(true);

        // Add border and rounded corners to image
        imageView.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e4e9f0; -fx-border-radius: 8px;");

        // Load image if available
        if (saison.getImage() != null && !saison.getImage().isEmpty()) {
            try {
                File imageFile = new File(uploadDirectory + saison.getImage());
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    imageView.setImage(image);
                } else {
                    // Set placeholder
                    setImagePlaceholder(imageView);
                }
            } catch (Exception e) {
                setImagePlaceholder(imageView);
                System.err.println("Error loading image: " + e.getMessage());
            }
        } else {
            setImagePlaceholder(imageView);
        }

        // Season content
        VBox content = new VBox(5);
        content.setStyle("-fx-padding: 0 10 0 10;");
        content.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label titleLabel = new Label(saison.getNomSaison());
        titleLabel.getStyleClass().add("season-title");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #333333;");

        Label descLabel = new Label(saison.getDescSaison());
        descLabel.getStyleClass().add("season-desc");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(Double.MAX_VALUE);
        descLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");

        HBox dateBox = new HBox();
        dateBox.setAlignment(Pos.CENTER_LEFT);
        dateBox.setPadding(new Insets(5, 0, 0, 0));

        Label dateLabel = new Label();
        if (saison.getDateFin() != null) {
            dateLabel.setText("End Date: " + saison.getDateFin().toString());
        } else {
            dateLabel.setText("No end date specified");
        }
        dateLabel.getStyleClass().add("date-badge");
        dateLabel.setStyle("-fx-background-color: #f0f4f8; -fx-padding: 5 10; -fx-background-radius: 4; -fx-text-fill: #4a6790;");

        dateBox.getChildren().add(dateLabel);

        // Status indicator
        HBox statusBox = new HBox();
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.setPadding(new Insets(5, 0, 0, 0));
        statusBox.setSpacing(5);

        Circle statusCircle = new Circle(4);
        Label statusLabel = new Label();

        // Set status based on end date
        LocalDate now = LocalDate.now();
        if (saison.getDateFin() == null) {
            statusCircle.setFill(javafx.scene.paint.Color.GRAY);
            statusLabel.setText("No Date");
        } else if (saison.getDateFin().isBefore(now)) {
            statusCircle.setFill(javafx.scene.paint.Color.RED);
            statusLabel.setText("Completed");
        } else if (saison.getDateFin().isAfter(now.plusMonths(1))) {
            statusCircle.setFill(javafx.scene.paint.Color.ORANGE);
            statusLabel.setText("Upcoming");
        } else {
            statusCircle.setFill(javafx.scene.paint.Color.GREEN);
            statusLabel.setText("Active");
        }

        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        statusBox.getChildren().addAll(statusCircle, statusLabel);

        content.getChildren().addAll(titleLabel, descLabel, dateBox, statusBox);

        // Action buttons
        VBox actions = new VBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setMinWidth(120);

        Button editButton = new Button("Edit");
        editButton.setGraphic(new FontIcon("mdi-pencil"));
        editButton.getStyleClass().add("edit-button");
        editButton.setStyle("-fx-background-color: #1e90ff; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 15;-fx-min-width: 90px;");
        editButton.setPadding(new Insets(8, 12, 8, 12));
        editButton.setOnAction(e -> showEditForm(saison));

        // Add hover effect
        editButton.setOnMouseEntered(e -> editButton.setStyle("-fx-background-color: #0d6efd; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 15;"));
        editButton.setOnMouseExited(e -> editButton.setStyle("-fx-background-color: #1e90ff; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 15;"));

        Button deleteButton = new Button("Delete");
        deleteButton.setGraphic(new FontIcon("mdi-delete"));
        deleteButton.getStyleClass().add("delete-button");
        deleteButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 15;");
        deleteButton.setPadding(new Insets(8, 12, 8, 12));
        deleteButton.setOnAction(e -> {
            try {
                deleteSaison(saison);
            } catch (SQLException ex) {
                showAlert(AlertType.ERROR, "Delete Error",
                        "Could not delete season", ex.getMessage());
            }
        });

        // Add hover effect
        deleteButton.setOnMouseEntered(e -> deleteButton.setStyle("-fx-background-color: #bb2d3b; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 15;"));
        deleteButton.setOnMouseExited(e -> deleteButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 15;"));

        actions.getChildren().addAll(editButton, deleteButton);

        // Add all components to card
        card.getChildren().addAll(iconContainer, imageView, content, actions);

        // Add hover effect to the entire card
        card.setOnMouseEntered(e ->
                card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2); -fx-border-color: #e6e6e6; -fx-border-radius: 10;"));
        card.setOnMouseExited(e ->
                card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 1); -fx-border-color: #f0f0f0; -fx-border-radius: 10;"));

        return card;
    }

    private void setImagePlaceholder(ImageView imageView) {
        // Create a placeholder rectangle with gradient background
        Rectangle placeholder = new Rectangle(120, 80);
        placeholder.setArcWidth(8);
        placeholder.setArcHeight(8);
        placeholder.setFill(javafx.scene.paint.Color.LIGHTGRAY);

        // Create FontIcon for placeholder
        FontIcon placeholderIcon = new FontIcon("mdi-image");
        placeholderIcon.setIconSize(32);
        placeholderIcon.setIconColor(javafx.scene.paint.Color.WHITE);

        // Create a StackPane to hold both
        StackPane placeholderPane = new StackPane(placeholder, placeholderIcon);

        // Convert to Image for the ImageView
        // This approach is a workaround since we can't directly set a Node as the content of ImageView
        // In a real app, you'd use a different approach like using a StackPane instead of ImageView
        imageView.setImage(null); // Clear existing image
    }

    private void showAddForm() {
        isEditMode = false;
        formTitleLabel.setText("Add Season");
        clearForm();
        showForm();
    }

    private void showEditForm(Saison saison) {
        isEditMode = true;
        formTitleLabel.setText("Edit Season");
        selectedSaison = saison;

        // Populate form fields
        saisonNameField.setText(saison.getNomSaison());
        saisonDescField.setText(saison.getDescSaison());

        if (saison.getDateFin() != null) {
            saisonDateField.setValue(saison.getDateFin());
        } else {
            saisonDateField.setValue(null);
        }

        // Load image if available
        if (saison.getImage() != null && !saison.getImage().isEmpty()) {
            try {
                File imageFile = new File(uploadDirectory + saison.getImage());
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    imagePreview.setImage(image);
                    selectedImageLabel.setText(saison.getImage());
                }
            } catch (Exception e) {
                System.err.println("Error loading image: " + e.getMessage());
            }
        } else {
            imagePreview.setImage(null);
            selectedImageLabel.setText("No file selected");
        }

        showForm();
    }

    private void showForm() {

        applyStylesToForm();
        // Show both the overlay and the form
        formOverlay.setVisible(true);
        formOverlay.setManaged(true);
        seasonFormContainer.setVisible(true);
        seasonFormContainer.setManaged(true);

        // Add a fade-in animation for better UX
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), formOverlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);


        // Optional scale animation for the form
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), seasonFormContainer);
        scaleIn.setFromX(0.95);
        scaleIn.setFromY(0.95);
        scaleIn.setToX(1);
        scaleIn.setToY(1);


        scaleIn.play();
        fadeIn.play();
    }

    private void hideForm() {
        // Create fade-out animation
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), formOverlay);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        // Scale out animation
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(200), seasonFormContainer);
        scaleOut.setFromX(1);
        scaleOut.setFromY(1);
        scaleOut.setToX(0.95);
        scaleOut.setToY(0.95);

        // When animation finishes, hide the form
        fadeOut.setOnFinished(event -> {
            formOverlay.setVisible(false);
            formOverlay.setManaged(false);
            seasonFormContainer.setVisible(false);
            seasonFormContainer.setManaged(false);
            clearForm();
        });

        // Play animations
        fadeOut.play();
        scaleOut.play();
    }

    private void clearForm() {
        saisonNameField.clear();
        saisonDescField.clear();
        saisonDateField.setValue(null);
        selectedImageFile = null;
        imagePreview.setImage(null);
        selectedImageLabel.setText("No file selected");
        selectedSaison = null;
    }

    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(addButton.getScene().getWindow());
        if (selectedFile != null) {
            selectedImageFile = selectedFile;
            selectedImageLabel.setText(selectedFile.getName());

            // Show image preview
            try {
                Image image = new Image(selectedFile.toURI().toString());
                imagePreview.setImage(image);
            } catch (Exception e) {
                showAlert(AlertType.ERROR, "Image Error",
                        "Could not load image preview", e.getMessage());
            }
        }
    }

    private void createSaison() throws SQLException {
        // Validate form first
        if (!validateForm()) {
            return;
        }

        String saisonName = saisonNameField.getText();
        String saisonDesc = saisonDescField.getText();
        LocalDate endDate = saisonDateField.getValue();

        Saison newSaison = new Saison();
        newSaison.setNomSaison(saisonName);
        newSaison.setDescSaison(saisonDesc);

        if (endDate != null) {
            newSaison.setDateFin(endDate);
        }

        // Handle image upload
        if (selectedImageFile != null) {
            String fileName = saveImage(selectedImageFile);
            newSaison.setImage(fileName);
        }

        saisonService.add(newSaison);

        // Show success alert
        showAlert(AlertType.INFORMATION, "Season Created",
                "The season was created successfully.", "");

        hideForm();
        loadAllSeasons();
        updateStats();
    }

    private void updateSaison() throws SQLException {
        if (selectedSaison == null) {
            showAlert(AlertType.WARNING, "No Selection",
                    "Please select a season to update.", "");
            return;
        }

        // Validate form first
        if (!validateForm()) {
            return;
        }

        String saisonName = saisonNameField.getText();
        String saisonDesc = saisonDescField.getText();
        LocalDate endDate = saisonDateField.getValue();

        selectedSaison.setNomSaison(saisonName);
        selectedSaison.setDescSaison(saisonDesc);

        if (endDate != null) {
            selectedSaison.setDateFin(endDate);
        } else {
            selectedSaison.setDateFin(null);
        }

        // Handle image upload
        if (selectedImageFile != null) {
            String fileName = saveImage(selectedImageFile);
            selectedSaison.setImage(fileName);
        }

        saisonService.update(selectedSaison);

        // Show success alert
        showAlert(AlertType.INFORMATION, "Season Updated",
                "The season was updated successfully.", "");

        hideForm();
        loadAllSeasons();
        updateStats();
    }

    private void deleteSaison(Saison saison) throws SQLException {
        if (saison == null) {
            showAlert(AlertType.WARNING, "No Selection",
                    "Please select a season to delete.", "");
            return;
        }

        // Confirm deletion
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Are you sure you want to delete the season '" + saison.getNomSaison() + "'?");

        // Style the confirmation dialog
        DialogPane dialogPane = confirmAlert.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #dce3f0;" +
                        "-fx-border-width: 1px;" +
                        "-fx-border-radius: 8px;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
        );

        if (confirmAlert.showAndWait().get() == ButtonType.OK) {
            // Delete the image file if it exists
            if (saison.getImage() != null && !saison.getImage().isEmpty()) {
                try {
                    Files.deleteIfExists(Paths.get(uploadDirectory + saison.getImage()));
                } catch (IOException e) {
                    System.err.println("Error deleting image file: " + e.getMessage());
                }
            }

            saisonService.delete(saison.getId());

            // Show success alert
            showAlert(AlertType.INFORMATION, "Season Deleted",
                    "The season was deleted successfully.", "");

            loadAllSeasons();
            updateStats();
        }
    }

    private String saveImage(File file) {
        String fileName = UUID.randomUUID().toString() + getFileExtension(file.getName());
        Path targetPath = Paths.get(uploadDirectory + fileName);

        try {
            Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "File Error",
                    "Could not save image", e.getMessage());
            return null;
        }
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex);
        }
        return "";
    }

    private boolean validateForm() {
        StringBuilder errorMessage = new StringBuilder();

        // Validate saison name (required field)
        if (saisonNameField.getText() == null || saisonNameField.getText().trim().isEmpty()) {
            errorMessage.append("• Please enter a valid season name.\n");
        }

        // Validate description (required field for our implementation)
        if (saisonDescField.getText() == null || saisonDescField.getText().trim().isEmpty()) {
            errorMessage.append("• Please enter a season description.\n");
        }

        // Validate date (not mandatory but if provided, should be valid)
        if (saisonDateField.getValue() != null) {
            // Check if date is in the past
            if (saisonDateField.getValue().isBefore(LocalDate.now())) {
                // This is just a warning, not an error
                errorMessage.append("• Warning: End date is in the past.\n");
            }
        }

        // Show validation errors if any
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

            // Check if there are actual errors (not just warnings)
            return !errorMessage.toString().contains("Please enter");
        }

        return true;
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
            navigateTo("/com/esprit/views/AdminEvent.fxml");
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
    private void showCompetitionManagement() {
        try {
            navigateTo("/com/esprit/views/AdminCompetition.fxml");
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Navigation Error",
                    "Could not navigate to Season Management", e.getMessage());
        }
    }
}
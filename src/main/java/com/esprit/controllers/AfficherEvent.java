package com.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import com.esprit.models.Evenement;
import com.esprit.services.ServiceEvent;
import com.esprit.MainApp;
import com.esprit.models.User;
import com.esprit.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import com.esprit.utils.DataSource;
import javafx.stage.Stage;

public class AfficherEvent implements Initializable {

    @FXML
    private Button addNewEventButton;

    @FXML
    private Button backButton;

    @FXML
    private Button refreshButton;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> categoryFilter;

    @FXML
    private ComboBox<String> clubFilter;

    @FXML
    private ComboBox<String> dateFilter;

    @FXML
    private FlowPane eventsContainer;

    @FXML
    private Label totalEventsLabel;

    // Add these new FXML components for pagination
    @FXML
    private Button prevPageButton;

    @FXML
    private Button nextPageButton;

    @FXML
    private Label pageInfoLabel;

    // User Profile Section - Added components
    @FXML
    private StackPane userProfileContainer;

    @FXML
    private ImageView userProfilePic;

    @FXML
    private Label userNameLabel;

    @FXML
    private VBox profileDropdown;

    private ServiceEvent serviceEvent;
    private List<Evenement> allEvents;
    private User currentUser; // Added for user profile

    // Pagination properties
    private int currentPage = 1;
    private final int ITEMS_PER_PAGE = 6; // Number of events to show per page
    private int totalPages = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialize service
        serviceEvent = new ServiceEvent();

        // Load user profile data - Added for user profile integration
        initializeUserProfile();

        // Setup filters before loading events
        setupFilters();

        // Load events from database
        loadEvents();

        // Set up pagination buttons
        setupPagination();

        // Update total events count
        updateEventCounter();

        // Add listeners for filters
        addFilterListeners();
        boolean isPresident = SessionManager.getInstance().hasRole("PRESIDENT_CLUB");

        // Rendre le bouton visible seulement si l'utilisateur est un président
        addNewEventButton.setVisible(isPresident);
        addNewEventButton.setManaged(isPresident);

        // Set up search field action
        searchField.setOnAction(e -> handleSearch());

        // Setup refresh button action
        if (refreshButton != null) {
            refreshButton.setOnAction(e -> handleRefresh());
        }
        setupCalendarButton();
    }

    /**
     * Initialize user profile data from session
     * Added method from HomeController integration
     */
    private void initializeUserProfile() {
        // Get current user from session
        currentUser = SessionManager.getInstance().getCurrentUser();

        if (currentUser != null) {
            // Set user name
            userNameLabel.setText(currentUser.getFirstName() + " " + currentUser.getLastName());

            // Load profile picture
            String profilePicture = currentUser.getProfilePicture();
            if (profilePicture != null && !profilePicture.isEmpty()) {
                try {
                    File imageFile = new File("uploads/profiles/" + profilePicture);
                    if (imageFile.exists()) {
                        Image image = new Image(imageFile.toURI().toString());
                        userProfilePic.setImage(image);
                    } else {
                        loadDefaultProfilePic();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    loadDefaultProfilePic();
                }
            } else {
                loadDefaultProfilePic();
            }

            // Apply circular clip to profile picture
            double radius = 22.5; // Match the style
            userProfilePic.setClip(new javafx.scene.shape.Circle(radius, radius, radius));

            // Initially hide the dropdown
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
        }
    }

    /**
     * Load default profile picture
     * Added method from HomeController integration
     */
    private void loadDefaultProfilePic() {
        try {
            Image defaultImage = new Image(getClass().getResourceAsStream("/com/esprit/images/default-profile.png"));
            userProfilePic.setImage(defaultImage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Show profile dropdown menu
     * Added method from HomeController integration
     */
    @FXML
    private void showProfileDropdown() {
        profileDropdown.setVisible(true);
        profileDropdown.setManaged(true);
    }

    /**
     * Hide profile dropdown menu
     * Added method from HomeController integration
     */
    @FXML
    private void hideProfileDropdown() {
        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);
    }

    /**
     * Navigate to user profile view
     * Added method from HomeController integration
     */
    @FXML
    private void navigateToProfile() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Profile.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }
    @FXML
    private Button navButton; // Injection du bouton

    @FXML
    private void navigateToHome1() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Home.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) navButton.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToProducts() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/produit/ProduitView.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToCompetition() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Competition.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    /**
     * Handle user logout
     * Added method from HomeController integration
     */
    @FXML
    private void handleLogout() throws IOException {
        // Clear the session
        SessionManager.getInstance().clearSession();

        // Navigate to login page
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    /**
     * Navigate to contact page
     * Added method from HomeController integration
     */
    @FXML
    private void navigateToContact() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Contact.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    /**
     * Navigate to home page
     * Added method from HomeController integration
     */
    @FXML
    private void navigateToHome() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Home.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    /**
     * Handle refresh button action - resets filters and reloads all events
     */
    @FXML
    private void handleRefresh() {
        // Reset all filters to default values
        searchField.clear();
        categoryFilter.setValue("All Categories");
        clubFilter.setValue("All Clubs");
        dateFilter.setValue("All Dates");

        // Reset to first page
        currentPage = 1;

        // Reload all events from database
        allEvents = loadAllEvents();

        // Display the first page
        displayCurrentPage();

        // Update counter
        updateEventCounter();

        // Update pagination controls
        updatePaginationControls();
    }

    private void setupPagination() {
        // Configure the pagination buttons
        prevPageButton.setOnAction(event -> {
            if (currentPage > 1) {
                currentPage--;
                displayCurrentPage();
            }
        });

        nextPageButton.setOnAction(event -> {
            if (currentPage < totalPages) {
                currentPage++;
                displayCurrentPage();
            }
        });

        // Initial button states
        updatePaginationControls();
    }

    private void updatePaginationControls() {
        // Calculate total pages
        if (allEvents != null) {
            totalPages = (int) Math.ceil((double) allEvents.size() / ITEMS_PER_PAGE);
        }

        // Update page info label
        pageInfoLabel.setText("Page " + currentPage + " of " + totalPages);

        // Enable/disable buttons based on current page
        prevPageButton.setDisable(currentPage <= 1);
        nextPageButton.setDisable(currentPage >= totalPages);
    }

    private void displayCurrentPage() {
        if (allEvents == null || allEvents.isEmpty()) {
            eventsContainer.getChildren().clear();
            return;
        }

        // Calculate start and end indices for current page
        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allEvents.size());

        // Get events for current page
        List<Evenement> currentPageEvents = allEvents.subList(startIndex, endIndex);

        // Display the current page of events
        displayEvents(currentPageEvents);

        // Update pagination controls
        updatePaginationControls();
    }

    @FXML
    private void handleAddNewEvent() {
        // Vérification supplémentaire du rôle avant d'ouvrir la vue d'ajout
        if (SessionManager.getInstance().hasRole("PRESIDENT_CLUB")) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/AjouterEvent.fxml"));
                Parent root = loader.load();
                addNewEventButton.getScene().setRoot(root);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Afficher une alerte pour informer l'utilisateur
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Accès restreint");
            alert.setHeaderText("Permission refusée");
            alert.setContentText("Seuls les présidents de club peuvent ajouter des événements.");
            alert.showAndWait();
        }
    }


    private void loadEvents() {
        try {
            Connection conn = DataSource.getInstance().getCnx();
            String query = "SELECT * FROM evenement ORDER BY start_date DESC";

            PreparedStatement pst = conn.prepareStatement(query);
            ResultSet rs = pst.executeQuery();

            allEvents = new ArrayList<>();

            while (rs.next()) {
                Evenement event = new Evenement();
                event.setId(rs.getInt("id"));
                event.setNom_event(rs.getString("nom_event"));
                event.setType(rs.getString("type"));
                event.setDesc_event(rs.getString("desc_event"));
                event.setImage_description(rs.getString("image_description"));
                event.setLieux(rs.getString("lieux"));
                event.setClub_id(rs.getInt("club_id"));
                event.setCategorie_id(rs.getInt("categorie_id"));
                event.setStart_date(rs.getDate("start_date"));
                event.setEnd_date(rs.getDate("end_date"));

                allEvents.add(event);
            }

            // Instead of showing all events at once, show the first page
            displayCurrentPage();

        } catch (SQLException ex) {
            System.err.println("Error loading events: " + ex.getMessage());
        }
    }

    private void displayEvents(List<Evenement> events) {
        eventsContainer.getChildren().clear();

        for (Evenement event : events) {
            VBox eventCard = createEventCard(event);
            eventsContainer.getChildren().add(eventCard);
        }
    }

    private VBox createEventCard(Evenement event) {
        // Récupérer les noms du club et de la catégorie à partir des IDs
        String categoryName = serviceEvent.getCategoryNameById(event.getCategorie_id());
        String clubName = serviceEvent.getClubNameById(event.getClub_id());

        // Create main card container
        VBox eventCard = new VBox();
        eventCard.setPrefWidth(300);
        eventCard.setPrefHeight(380);
        eventCard.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-border-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 3);");

        // Create image container
        StackPane imageContainer = new StackPane();
        ImageView imageView = new ImageView();
        imageView.setFitHeight(180);
        imageView.setFitWidth(300);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 15 15 0 0;");

        // Try to load image from path or use default
        try {
            if (event.getImage_description() != null && !event.getImage_description().isEmpty()) {
                File imageFile = new File(event.getImage_description());
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    imageView.setImage(image);
                } else {
                    // Default image if file not found
                    imageView.setImage(new Image("/images/default-event.png"));
                }
            } else {
                // Default image if no path
                imageView.setImage(new Image("/images/default-event.png"));
            }
        } catch (Exception ex) {
            System.err.println("Error loading image: " + ex.getMessage());
            // Use default image on error
            imageView.setImage(new Image("/images/default-event.png"));
        }

        // Create status label
        Label statusLabel = new Label();

        String eventType = event.getType(); //

        statusLabel.setText(eventType);

        if ("Closed".equalsIgnoreCase(eventType)) {
            statusLabel.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 5 10;");
        } else if ("Open".equalsIgnoreCase(eventType)) {
            statusLabel.setStyle("-fx-background-color: #040f71; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 5 10;");
        } else {
            statusLabel.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 5 10;");
        }

        StackPane.setAlignment(statusLabel, javafx.geometry.Pos.TOP_RIGHT);
        StackPane.setMargin(statusLabel, new javafx.geometry.Insets(10, 10, 0, 0));

        imageContainer.getChildren().addAll(imageView, statusLabel);

        // Create details container
        VBox detailsContainer = new VBox();
        detailsContainer.setStyle("-fx-padding: 15;");
        detailsContainer.setSpacing(8);

        // Category and club
        HBox categoryClubBox = new HBox();
        categoryClubBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        categoryClubBox.setSpacing(10);

        Label categoryLabel = new Label(categoryName);
        categoryLabel.setStyle("-fx-text-fill: #1e90ff;");
        categoryLabel.setFont(new javafx.scene.text.Font("Arial Bold", 12));

        Label clubLabel = new Label("• " + clubName);
        clubLabel.setStyle("-fx-text-fill: #666666;");
        clubLabel.setFont(new javafx.scene.text.Font("Arial", 12));

        categoryClubBox.getChildren().addAll(categoryLabel, clubLabel);

        // Event title
        Label titleLabel = new Label(event.getNom_event());
        titleLabel.setStyle("-fx-text-fill: #333333;");
        titleLabel.setFont(new javafx.scene.text.Font("Arial Bold", 16));

        // Event description (truncated)
        String description = event.getDesc_event();
        if (description.length() > 100) {
            description = description.substring(0, 97) + "...";
        }
        Label descriptionLabel = new Label(description);
        descriptionLabel.setStyle("-fx-text-fill: #555555; -fx-wrap-text: true;");
        descriptionLabel.setFont(new javafx.scene.text.Font("Arial", 13));

        // Format dates
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
        String dateRange;
        if (dateFormat.format(event.getStart_date()).equals(dateFormat.format(event.getEnd_date()))) {
            dateRange = dateFormat.format(event.getStart_date());
        } else {
            dateRange = dateFormat.format(event.getStart_date()) + " - " + dateFormat.format(event.getEnd_date());
        }

        // Date display
        HBox dateBox = new HBox();
        dateBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        dateBox.setSpacing(5);
        dateBox.setStyle("-fx-padding: 5 0 0 0;");

        Label dateIconLabel = new Label("📅");
        dateIconLabel.setStyle("-fx-text-fill: #666666;");
        dateIconLabel.setFont(new javafx.scene.text.Font("Arial", 13));

        Label dateTextLabel = new Label(dateRange);
        dateTextLabel.setStyle("-fx-text-fill: #666666;");
        dateTextLabel.setFont(new javafx.scene.text.Font("Arial", 13));

        dateBox.getChildren().addAll(dateIconLabel, dateTextLabel);

        // Location display
        HBox locationBox = new HBox();
        locationBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        locationBox.setSpacing(5);

        Label locationIconLabel = new Label("📍");
        locationIconLabel.setStyle("-fx-text-fill: #666666;");
        locationIconLabel.setFont(new javafx.scene.text.Font("Arial", 13));

        Label locationTextLabel = new Label(event.getLieux());
        locationTextLabel.setStyle("-fx-text-fill: #666666;");
        locationTextLabel.setFont(new javafx.scene.text.Font("Arial", 13));

        locationBox.getChildren().addAll(locationIconLabel, locationTextLabel);

        // Buttons
        HBox buttonsBox = new HBox();
        buttonsBox.setAlignment(javafx.geometry.Pos.CENTER);
        buttonsBox.setSpacing(10);
        buttonsBox.setStyle("-fx-padding: 10 0 0 0;");

        //  bouton viewDetailsButton
        Button viewDetailsButton = new Button("View Details");
        viewDetailsButton.setStyle("-fx-background-color: #1e90ff; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 15;");
        viewDetailsButton.setFont(new javafx.scene.text.Font("Arial Bold", 13));

        // Ajouter l'action pour le bouton "View Details"
        viewDetailsButton.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/DetailsEvent.fxml"));
                Parent root = loader.load();

                // Récupérer le contrôleur et passer l'événement sélectionné
                DetailsEvent detailsController = loader.getController();
                detailsController.setEventData(event);

                viewDetailsButton.getScene().setRoot(root);

            } catch (IOException ex) {
                System.err.println("Error loading DetailsEvent.fxml: " + ex.getMessage());
                ex.printStackTrace();
            }
        });


        // Disable registration if event is closed


        buttonsBox.getChildren().addAll(viewDetailsButton);

        // Add all elements to details container
        detailsContainer.getChildren().addAll(
                categoryClubBox,
                titleLabel,
                descriptionLabel,
                dateBox,
                locationBox,
                buttonsBox
        );

        // Add image and details to event card
        eventCard.getChildren().addAll(imageContainer, detailsContainer);

        return eventCard;
    }

    private void updateEventCounter() {
        if (totalEventsLabel != null && allEvents != null) {
            totalEventsLabel.setText("Total Events: " + allEvents.size());
        }
    }

    // Helper method to reload all events for when search is cleared
    private List<Evenement> loadAllEvents() {
        try {
            Connection conn = DataSource.getInstance().getCnx();
            String query = "SELECT * FROM evenement ORDER BY start_date DESC";
            PreparedStatement pst = conn.prepareStatement(query);
            ResultSet rs = pst.executeQuery();

            List<Evenement> events = new ArrayList<>();

            while (rs.next()) {
                Evenement event = new Evenement();
                event.setId(rs.getInt("id"));
                event.setNom_event(rs.getString("nom_event"));
                event.setType(rs.getString("type"));
                event.setDesc_event(rs.getString("desc_event"));
                event.setImage_description(rs.getString("image_description"));
                event.setLieux(rs.getString("lieux"));
                event.setClub_id(rs.getInt("club_id"));
                event.setCategorie_id(rs.getInt("categorie_id"));
                event.setStart_date(rs.getDate("start_date"));
                event.setEnd_date(rs.getDate("end_date"));

                events.add(event);
            }

            return events;

        } catch (SQLException ex) {
            System.err.println("Error loading events: " + ex.getMessage());
            return new ArrayList<>();
        }
    }

    private void setupFilters() {
        // Initialize ComboBox items if they're null
        if (categoryFilter.getItems() == null || categoryFilter.getItems().isEmpty()) {
            categoryFilter.getItems().add("All Categories");
            categoryFilter.setValue("All Categories");
        }

        if (clubFilter.getItems() == null || clubFilter.getItems().isEmpty()) {
            clubFilter.getItems().add("All Clubs");
            clubFilter.setValue("All Clubs");
        }

        if (dateFilter.getItems() == null || dateFilter.getItems().isEmpty()) {
            dateFilter.getItems().addAll(
                    "All Dates",
                    "Today",
                    "This Week",
                    "This Month",
                    "Upcoming",
                    "Past Events"
            );
            dateFilter.setValue("All Dates");
        }

        // Setup Category Filter
        try {
            Connection conn = DataSource.getInstance().getCnx();

            // Populate category filter
            List<String> categories = new ArrayList<>();
            categories.add("All Categories"); // Default option

            // Use your existing method to get categories, but check the column name
            String categoryQuery = "SELECT nom_cat FROM categorie ORDER BY nom_cat";
            PreparedStatement categoryPst = conn.prepareStatement(categoryQuery);
            ResultSet categoryRs = categoryPst.executeQuery();

            while (categoryRs.next()) {
                categories.add(categoryRs.getString("nom_cat"));
            }

            categoryFilter.getItems().clear();
            categoryFilter.getItems().addAll(categories);
            categoryFilter.setValue("All Categories");

            // Populate club filter
            List<String> clubs = new ArrayList<>();
            clubs.add("All Clubs"); // Default option

            // Use your existing method to get clubs, but check the column name
            String clubQuery = "SELECT nom_c FROM club ORDER BY nom_c";
            PreparedStatement clubPst = conn.prepareStatement(clubQuery);
            ResultSet clubRs = clubPst.executeQuery();

            while (clubRs.next()) {
                clubs.add(clubRs.getString("nom_c"));
            }

            clubFilter.getItems().clear();
            clubFilter.getItems().addAll(clubs);
            clubFilter.setValue("All Clubs");

        } catch (SQLException ex) {
            System.err.println("Error setting up filters: " + ex.getMessage());
        }
    }

    private void addFilterListeners() {
        // Add listeners to filters to apply filtering when they change
        categoryFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        clubFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        dateFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());

        // Add listener to search field for real-time filtering
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.isEmpty() && !oldValue.isEmpty()) {
                applyFilters(); // Only trigger when text is cleared
            }
        });
    }

    private void applyFilters() {
        // Get filter values
        String searchText = searchField.getText().toLowerCase().trim();
        String categoryValue = categoryFilter.getValue();
        String clubValue = clubFilter.getValue();
        String dateValue = dateFilter.getValue();

        // Load all events first
        List<Evenement> baseEvents = loadAllEvents();
        List<Evenement> filteredEvents = new ArrayList<>(baseEvents);

        // Apply category filter
        if (categoryValue != null && !categoryValue.equals("All Categories")) {
            // Use existing method in your ServiceEvent class
            int categoryId = serviceEvent.getCategorieIdByName(categoryValue);
            if (categoryId != -1) {
                filteredEvents.removeIf(event -> event.getCategorie_id() != categoryId);
            }
        }

        // Apply club filter
        if (clubValue != null && !clubValue.equals("All Clubs")) {
            // Use existing method in your ServiceEvent class
            int clubId = serviceEvent.getClubIdByName(clubValue);
            if (clubId != -1) {
                filteredEvents.removeIf(event -> event.getClub_id() != clubId);
            }
        }

        // Apply date filter
        if (dateValue != null && !dateValue.equals("All Dates")) {
            filteredEvents = filterEventsByDate(filteredEvents, dateValue);
        }

        // Apply search text if not empty
        if (!searchText.isEmpty()) {
            filteredEvents.removeIf(event ->
                    !event.getNom_event().toLowerCase().contains(searchText) &&
                            !event.getDesc_event().toLowerCase().contains(searchText) &&
                            !event.getLieux().toLowerCase().contains(searchText)
            );
        }

        // Update allEvents with filtered list
        allEvents = filteredEvents;

        // Reset to first page
        currentPage = 1;

        // Display filtered events
        if (filteredEvents.isEmpty()) {
            eventsContainer.getChildren().clear();
            Label noResultsLabel = new Label("No events found matching your criteria.");
            noResultsLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 16px;");
            eventsContainer.getChildren().add(noResultsLabel);

            // Update pagination for no results
            totalPages = 1;
            pageInfoLabel.setText("Page 0 of 0");
            prevPageButton.setDisable(true);
            nextPageButton.setDisable(true);
        } else {
            displayCurrentPage();
        }

        // Update event counter
        updateEventCounter();
    }

    private List<Evenement> filterEventsByDate(List<Evenement> events, String dateFilter) {
        List<Evenement> filteredEvents = new ArrayList<>();

        // Get current date
        java.util.Date currentDate = new java.util.Date();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(currentDate);

        // Apply date filtering
        switch(dateFilter) {
            case "Today":
                // Today's events
                for (Evenement event : events) {
                    java.util.Calendar eventCal = java.util.Calendar.getInstance();
                    eventCal.setTime(event.getStart_date());

                    if (cal.get(java.util.Calendar.YEAR) == eventCal.get(java.util.Calendar.YEAR) &&
                            cal.get(java.util.Calendar.DAY_OF_YEAR) == eventCal.get(java.util.Calendar.DAY_OF_YEAR)) {
                        filteredEvents.add(event);
                    }
                }
                break;

            case "This Week":
                // This week's events
                int currentWeek = cal.get(java.util.Calendar.WEEK_OF_YEAR);
                int currentYear = cal.get(java.util.Calendar.YEAR);

                for (Evenement event : events) {
                    java.util.Calendar eventCal = java.util.Calendar.getInstance();
                    eventCal.setTime(event.getStart_date());

                    if (currentYear == eventCal.get(java.util.Calendar.YEAR) &&
                            currentWeek == eventCal.get(java.util.Calendar.WEEK_OF_YEAR)) {
                        filteredEvents.add(event);
                    }
                }
                break;

            case "This Month":
                // This month's events
                int currentMonth = cal.get(java.util.Calendar.MONTH);
                int currentMonthYear = cal.get(java.util.Calendar.YEAR);

                for (Evenement event : events) {
                    java.util.Calendar eventCal = java.util.Calendar.getInstance();
                    eventCal.setTime(event.getStart_date());

                    if (currentMonthYear == eventCal.get(java.util.Calendar.YEAR) &&
                            currentMonth == eventCal.get(java.util.Calendar.MONTH)) {
                        filteredEvents.add(event);
                    }
                }
                break;

            case "Upcoming":
                // Future events (starting from today)
                for (Evenement event : events) {
                    if (event.getStart_date().compareTo(currentDate) >= 0) {
                        filteredEvents.add(event);
                    }
                }
                break;

            case "Past Events":
                // Past events (before today)
                for (Evenement event : events) {
                    if (event.getEnd_date().compareTo(currentDate) < 0) {
                        filteredEvents.add(event);
                    }
                }
                break;

            default:
                // Return all events if no valid filter
                return events;
        }

        return filteredEvents;
    }

    // Single search method to handle searching
    @FXML
    private void handleSearch() {
        // When user explicitly clicks search or presses Enter in search field, apply all filters
        applyFilters();
    }

    @FXML
    private Button calendarViewButton; // Add this to your FXML button declarations

    // Add this method to your initialization sequence in initialize()
    private void setupCalendarButton() {
        // Set up calendar view button
        if (calendarViewButton != null) {
            calendarViewButton.setOnAction(e -> switchToCalendarView());
        }
    }

    @FXML
    private void switchToCalendarView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/CalendarView.fxml"));
            Parent root = loader.load();
            calendarViewButton.getScene().setRoot(root);
        } catch (IOException ex) {
            System.err.println("Error loading CalendarView.fxml: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
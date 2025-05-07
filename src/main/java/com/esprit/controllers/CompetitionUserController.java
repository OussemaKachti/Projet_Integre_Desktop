package com.esprit.controllers;

import com.esprit.models.Club;
import com.esprit.models.Competition;
import com.esprit.models.Saison;
import com.esprit.services.ClubService;
import com.esprit.services.CompetitionService;
import com.esprit.services.SaisonService;

import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.esprit.MainApp;
import com.esprit.models.User;
import com.esprit.utils.SessionManager;
import javafx.fxml.Initializable;
import java.util.ResourceBundle;

public class CompetitionUserController implements Initializable{

    private final CompetitionService competitionService = new CompetitionService();
    private final SaisonService saisonService = new SaisonService();
    private final ClubService clubService = ClubService.getInstance();
    private User currentUser;
    // Main container
    @FXML private AnchorPane competitionUserPane;

    // Header components
    @FXML private ImageView userProfilePic;
    @FXML private Label userNameLabel;
    @FXML private StackPane userProfileContainer;
    @FXML private VBox profileDropdown;

    // Season navigation components
    @FXML private FlowPane seasonsContainer;
    @FXML private Button prevSeasonsButton;
    @FXML private Button nextSeasonsButton;
    @FXML private Label seasonPageIndicator;
    @FXML private VBox noSeasonsContainer;

    // Missions components
    @FXML private VBox missionsContainer;
    @FXML private VBox noMissionsContainer;
    @FXML private Label missionCountLabel;

    // Leaderboard components
    @FXML private TableView<Club> leaderboardTable;
    @FXML private TableColumn<Club, Integer> rankColumn;
    @FXML private TableColumn<Club, String> clubColumn;
    @FXML private TableColumn<Club, Integer> pointsColumn;
    @FXML private HBox topPerformersContainer;

    // Navigation and UI components
    @FXML private Button backButton;
    @FXML private Label seasonTitle;

    // State variables
    private Saison currentSeason;
    private List<Saison> allSeasons = new ArrayList<>();
    private List<List<Saison>> seasonPages = new ArrayList<>();
    private int currentSeasonPageIndex = 0;
    private final int SEASONS_PER_PAGE = 5;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
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

                            // Keep aspect ratio
                            userProfilePic.setPreserveRatio(true);
                            userProfilePic.setFitHeight(40);
                            userProfilePic.setFitWidth(40);
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
                javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(20, 20, 20);
                userProfilePic.setClip(clip);

                // Ensure dropdown is properly positioned and hidden initially
                javafx.application.Platform.runLater(() -> {
                    profileDropdown.setVisible(false);
                    profileDropdown.setManaged(false);
                    profileDropdown.toFront();

                    // Position the dropdown properly
                    userProfileContainer.layout(); // Force layout to calculate proper size
                    // Set mouse event handlers for better interaction
                    profileDropdown.setOnMouseEntered(e -> profileDropdown.setVisible(true));
                    profileDropdown.setOnMouseExited(e -> {
                        if (!userProfileContainer.isHover()) {
                            hideProfileDropdown();
                        }
                    });
                });
            }

            // Set up back button with icon
            setupBackButton();

            // Set up season navigation
            setupSeasonNavigation();

            // Load seasons
            loadSeasons();

            // Configure the leaderboard table
            configureLeaderboardTable();

            // Load leaderboard data
            loadLeaderboardData();

            // Load current season missions
            loadCurrentSeasonMissions();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Initialization Error", "Could not load competition data: " + e.getMessage());
        }

        // Apply fade-in animation to main container
        fadeInNode(competitionUserPane, 300);
    }

    private void loadDefaultProfilePic() {
        try {
            Image defaultImage = new Image(getClass().getResourceAsStream("/com/esprit/images/default-profile.png"));
            userProfilePic.setImage(defaultImage);
            userProfilePic.setPreserveRatio(true);
            userProfilePic.setFitHeight(40);
            userProfilePic.setFitWidth(40);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupUserProfile() {
        // Load user profile image (placeholder for now)
        try {
            URL profileImageUrl = getClass().getResource("/com/esprit/images/profile_placeholder.png");
            if (profileImageUrl != null) {
                userProfilePic.setImage(new Image(profileImageUrl.toExternalForm()));
            }

            // Set user name from session (placeholder for now)
            userNameLabel.setText("John Doe"); // Replace with actual user name from session
        } catch (Exception e) {
            System.err.println("Error loading profile image: " + e.getMessage());
        }
    }

    @FXML
    public void toggleProfileDropdown() {
        if (profileDropdown.isVisible()) {
            hideProfileDropdown();
        } else {
            showProfileDropdown();
        }
    }

    @FXML
    public void showProfileDropdown() {
        System.out.println("Showing profile dropdown"); // Debug message

        // Ensure dropdown is visible and positioned correctly
        profileDropdown.toFront();
        profileDropdown.setVisible(true);
        profileDropdown.setManaged(true);

        // Position the dropdown correctly
        double xOffset = 0;
        double yOffset = userProfileContainer.getHeight() + 5;
        profileDropdown.setTranslateX(xOffset);
        profileDropdown.setTranslateY(yOffset);
    }

    @FXML
    public void hideProfileDropdown() {
        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);
    }

    private void setupBackButton() {
        FontIcon backIcon = new FontIcon("fas-arrow-left");
        backIcon.setIconColor(Color.WHITE);
        backButton.setGraphic(backIcon);
        backButton.setOnAction(event -> navigateBack());
    }

    private void setupSeasonNavigation() {
        // Initially disable buttons until we know if we have multiple pages
        prevSeasonsButton.setDisable(true);
        nextSeasonsButton.setDisable(true);

        // Set up click handlers
        prevSeasonsButton.setOnAction(e -> showPreviousSeasons());
        nextSeasonsButton.setOnAction(e -> showNextSeasons());
    }

    @FXML
    public void navigateToHome() {
        navigate("/com/esprit/views/home.fxml");
    }

    @FXML
    public void navigateToClubs() {
        navigate("/com/esprit/views/clubs.fxml");
    }

    @FXML
    public void navigateToEvents() {
        navigate("/com/esprit/views/events.fxml");
    }

    @FXML
    public void navigateToProducts() {
        navigate("/com/esprit/views/products.fxml");
    }

    @FXML
    public void navigateToProfile() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Profile.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) competitionUserPane.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    public void navigateToContact() {
        navigate("/com/esprit/views/contact.fxml");
    }

    @FXML
    public void handleLogout() throws IOException {
        // Clear the session
        SessionManager.getInstance().clearSession();

        // Navigate to login page
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    private void navigateBack() {
        try {
            URL dashboardUrl = getClass().getResource("/com/esprit/views/dashboard.fxml");
            if (dashboardUrl == null) {
                throw new IOException("Cannot find dashboard.fxml resource");
            }

            navigate(dashboardUrl.getPath());
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not navigate back: " + e.getMessage());
        }
    }

    private void navigate(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) competitionUserPane.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not navigate to: " + fxmlPath + "\nError: " + e.getMessage());
        }
    }

    @FXML
    public void showPreviousSeasons() {
        if (currentSeasonPageIndex > 0) {
            currentSeasonPageIndex--;
            updateSeasonsPage();
        }
    }

    @FXML
    public void showNextSeasons() {
        if (currentSeasonPageIndex < seasonPages.size() - 1) {
            currentSeasonPageIndex++;
            updateSeasonsPage();
        }
    }

    private void updateSeasonsPage() {
        // Update page indicator
        seasonPageIndicator.setText((currentSeasonPageIndex + 1) + "/" + seasonPages.size());

        // Update button states
        prevSeasonsButton.setDisable(currentSeasonPageIndex == 0);
        nextSeasonsButton.setDisable(currentSeasonPageIndex >= seasonPages.size() - 1);

        // Display seasons for current page
        displaySeasons(seasonPages.get(currentSeasonPageIndex));
    }

    private void loadSeasons() throws SQLException {
        seasonsContainer.getChildren().clear();
        allSeasons.clear();
        seasonPages.clear();

        // Get all seasons
        List<Saison> seasons = saisonService.getAll();

        if (seasons.isEmpty()) {
            // Show empty state
            noSeasonsContainer.setVisible(true);
            noSeasonsContainer.setManaged(true);
            seasonPageIndicator.setText("0/0");
            prevSeasonsButton.setDisable(true);
            nextSeasonsButton.setDisable(true);
            return;
        } else {
            noSeasonsContainer.setVisible(false);
            noSeasonsContainer.setManaged(false);
        }

        // Sort seasons by end date (closest to end first)
        allSeasons = seasons.stream()
                .sorted(Comparator.comparing(Saison::getDateFin))
                .collect(Collectors.toList());

        // Determine current season (most recent active)
        if (!allSeasons.isEmpty()) {
            Optional<Saison> activeSeason = allSeasons.stream()
                    .filter(s -> s.getDateFin().isAfter(LocalDate.now()))
                    .min(Comparator.comparing(Saison::getDateFin));

            if (activeSeason.isPresent()) {
                currentSeason = activeSeason.get();
            } else {
                currentSeason = allSeasons.get(0);
            }

            // Update the title if seasonTitle exists
            if (seasonTitle != null) {
                seasonTitle.setText(currentSeason.getNomSaison());
            }
        }

        // Create pages of seasons
        for (int i = 0; i < allSeasons.size(); i += SEASONS_PER_PAGE) {
            int end = Math.min(i + SEASONS_PER_PAGE, allSeasons.size());
            seasonPages.add(allSeasons.subList(i, end));
        }

        // Set initial page
        currentSeasonPageIndex = 0;

        // Find which page contains the current season
        if (currentSeason != null) {
            for (int i = 0; i < seasonPages.size(); i++) {
                if (seasonPages.get(i).contains(currentSeason)) {
                    currentSeasonPageIndex = i;
                    break;
                }
            }
        }

        // Update page indicator
        seasonPageIndicator.setText((currentSeasonPageIndex + 1) + "/" + seasonPages.size());

        // Update button states
        prevSeasonsButton.setDisable(currentSeasonPageIndex == 0);
        nextSeasonsButton.setDisable(currentSeasonPageIndex >= seasonPages.size() - 1 || seasonPages.size() <= 1);

        // Display seasons for current page
        if (!seasonPages.isEmpty()) {
            displaySeasons(seasonPages.get(currentSeasonPageIndex));
        }
    }

    private void displaySeasons(List<Saison> seasons) {
        seasonsContainer.getChildren().clear();

        // Add seasons to container with delay for animation effect
        for (int i = 0; i < seasons.size(); i++) {
            VBox seasonCard = createSeasonCard(seasons.get(i));
            seasonsContainer.getChildren().add(seasonCard);

            // Apply fade-in animation with staggered delay
            fadeInNode(seasonCard, 100 * (i + 1));
        }
    }

    private VBox createSeasonCard(Saison season) {
        VBox card = new VBox();
        card.getStyleClass().addAll("season-card");
        card.setAlignment(Pos.CENTER);
        card.setSpacing(10);
        card.setPadding(new Insets(15));
        card.setPrefWidth(180);
        card.setOpacity(0); // Start invisible for animation

        // Create a container for the icon with circular background
        StackPane iconContainer = new StackPane();
        iconContainer.getStyleClass().add("season-icon-container");

        // Season icon
        ImageView seasonIcon = new ImageView();
        try {
            String imagePath = season.getImage();
            boolean imageLoaded = false;

            if (imagePath != null && !imagePath.isEmpty()) {
                // Try as absolute path
                File imageFile = new File(imagePath);
                if (imageFile.exists() && imageFile.isFile()) {
                    seasonIcon.setImage(new Image(imageFile.toURI().toString()));
                    imageLoaded = true;
                }

                // Try as resource path
                if (!imageLoaded && imagePath.startsWith("/")) {
                    URL resourceUrl = getClass().getResource(imagePath);
                    if (resourceUrl != null) {
                        Image img = new Image(resourceUrl.toExternalForm());
                        if (!img.isError()) {
                            seasonIcon.setImage(img);
                            imageLoaded = true;
                        }
                    }
                }

                // Try with application resources prefix
                if (!imageLoaded) {
                    String resourcePath = "/com/esprit/images/" + (imagePath.startsWith("/") ? imagePath.substring(1) : imagePath);
                    URL resourceUrl = getClass().getResource(resourcePath);
                    if (resourceUrl != null) {
                        Image img = new Image(resourceUrl.toExternalForm());
                        if (!img.isError()) {
                            seasonIcon.setImage(img);
                            imageLoaded = true;
                        }
                    }
                }

                // Try database folder convention if exists
                if (!imageLoaded && !imagePath.startsWith("/") && !imagePath.contains(":")) {
                    String dbImagePath = "/com/esprit/images/" + imagePath;
                    URL resourceUrl = getClass().getResource(dbImagePath);
                    if (resourceUrl != null) {
                        Image img = new Image(resourceUrl.toExternalForm());
                        if (!img.isError()) {
                            seasonIcon.setImage(img);
                            imageLoaded = true;
                        }
                    }
                }
            }

            // If all attempts failed, use default
            if (seasonIcon.getImage() == null || seasonIcon.getImage().isError()) {
                URL defaultImageUrl = getClass().getResource("/com/esprit/images/default.PNG");
                if (defaultImageUrl != null) {
                    seasonIcon.setImage(new Image(defaultImageUrl.toExternalForm()));
                } else {
                    System.err.println("Warning: Could not find default image resource");
                }
            }
        } catch (Exception e) {
            // Fall back to default image on error
            try {
                URL defaultImageUrl = getClass().getResource("/com/esprit/images/default.PNG");
                if (defaultImageUrl != null) {
                    seasonIcon.setImage(new Image(defaultImageUrl.toExternalForm()));
                } else {
                    System.err.println("Warning: Could not find default image resource: " + e.getMessage());
                }
            } catch (Exception ex) {
                System.err.println("Failed to load default image: " + ex.getMessage());
            }
        }

        seasonIcon.setFitHeight(40);
        seasonIcon.setFitWidth(40);
        seasonIcon.setPreserveRatio(true);

        iconContainer.getChildren().add(seasonIcon);

        // Season title
        Label titleLabel = new Label(season.getNomSaison());
        titleLabel.getStyleClass().add("season-title");

        // Season description
        Label descLabel = new Label(season.getDescSaison());
        descLabel.getStyleClass().add("season-description");
        descLabel.setWrapText(true);
        descLabel.setTextAlignment(TextAlignment.CENTER);

        // Season end date
        Label endDateLabel = new Label(formatEndDate(season.getDateFin()));
        endDateLabel.getStyleClass().add("season-date");

        // Active/Inactive badge
        Label statusBadge = new Label(season.getDateFin().isAfter(LocalDate.now()) ? "ACTIVE" : "PAST");
        statusBadge.getStyleClass().add("season-badge");
        if (!season.getDateFin().isAfter(LocalDate.now())) {
            statusBadge.getStyleClass().add("season-badge-past");
        }

        // Position the badge in the top-right corner
        StackPane badgeContainer = new StackPane();
        badgeContainer.setAlignment(Pos.TOP_RIGHT);
        badgeContainer.getChildren().add(statusBadge);
        StackPane.setMargin(statusBadge, new Insets(-20, -20, 0, 0));

        // If this is the current season, highlight it
        if (currentSeason != null && currentSeason.getId() == season.getId()) {
            card.getStyleClass().add("current-season");
        }

        // Make the card clickable to display its missions
        card.setOnMouseClicked(event -> {
            try {
                currentSeason = season;
                loadCurrentSeasonMissions();

                // Update selected styling
                for (Node node : seasonsContainer.getChildren()) {
                    node.getStyleClass().remove("current-season");
                }
                card.getStyleClass().add("current-season");

                if (seasonTitle != null) {
                    seasonTitle.setText(season.getNomSaison());
                }
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Could not load missions for the selected season: " + e.getMessage());
            }
        });

        // Add components to card (with badge overlay)
        VBox cardContent = new VBox(iconContainer, titleLabel, descLabel, endDateLabel);
        cardContent.setAlignment(Pos.CENTER);
        cardContent.setSpacing(10);

        StackPane cardWithBadge = new StackPane();
        cardWithBadge.getChildren().addAll(cardContent, badgeContainer);

        card.getChildren().add(cardWithBadge);
        return card;
    }

    private String formatEndDate(LocalDate date) {
        // Check if date is in the future or past and format accordingly
        if (date.isAfter(LocalDate.now())) {
            return "Ends " + date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        } else {
            return "Ended " + date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        }
    }

    private void loadCurrentSeasonMissions() throws SQLException {
        missionsContainer.getChildren().clear();

        if (currentSeason == null) {
            noMissionsContainer.setVisible(true);
            noMissionsContainer.setManaged(true);
            missionCountLabel.setText("(0)");
            return;
        } else {
            noMissionsContainer.setVisible(false);
            noMissionsContainer.setManaged(false);
        }

        // Get missions for the current season
        List<Competition> missions = competitionService.getAll().stream()
                .filter(m -> m.getSaisonId() != null && m.getSaisonId().getId() == currentSeason.getId())
                .collect(Collectors.toList());

        // Update mission count
        missionCountLabel.setText("(" + missions.size() + ")");

        if (missions.isEmpty()) {
            noMissionsContainer.setVisible(true);
            noMissionsContainer.setManaged(true);
            return;
        }

        for (int i = 0; i < missions.size(); i++) {
            HBox missionItem = createMissionItem(missions.get(i));
            missionsContainer.getChildren().add(missionItem);

            // Apply animation with staggered delay
            fadeInNode(missionItem, 100 * (i + 1));
        }
    }

    private HBox createMissionItem(Competition mission) {
        HBox item = new HBox();
        item.getStyleClass().add("mission-item");
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(15));
        item.setSpacing(15);
        item.setOpacity(0); // Start invisible for animation

        // Left side with mission icon
        StackPane iconPane = new StackPane();
        FontIcon missionIcon = new FontIcon("fas-trophy");
        missionIcon.setIconSize(20);
        missionIcon.setIconColor(Color.web("#4a90e2"));

        Circle iconBackground = new Circle(20);
        iconBackground.setFill(Color.web("#f0f7ff"));

        iconPane.getChildren().addAll(iconBackground, missionIcon);
        iconPane.setPadding(new Insets(0, 15, 0, 0));

        // Center with mission details
        VBox missionDetails = new VBox();
        missionDetails.setSpacing(5);
        HBox.setHgrow(missionDetails, Priority.ALWAYS);

        Label titleLabel = new Label(mission.getNomComp());
        titleLabel.getStyleClass().add("mission-title");

        Label descLabel = new Label(mission.getDescComp());
        descLabel.getStyleClass().add("mission-desc");
        descLabel.setWrapText(true);

        missionDetails.getChildren().addAll(titleLabel, descLabel);

        // Right side with points
        HBox pointsContainer = new HBox();
        pointsContainer.setAlignment(Pos.CENTER);
        pointsContainer.getStyleClass().add("points-container");

        Label pointsLabel = new Label(mission.getPoints() + " Points");
        pointsLabel.getStyleClass().add("points-label");

        pointsContainer.getChildren().add(pointsLabel);

        item.getChildren().addAll(iconPane, missionDetails, pointsContainer);
        return item;
    }

    private void configureLeaderboardTable() {
        // Configure rank column with custom cell factory
        rankColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(leaderboardTable.getItems().indexOf(cellData.getValue()) + 1).asObject());

        rankColumn.setCellFactory(column -> new TableCell<Club, Integer>() {
            @Override
            protected void updateItem(Integer rank, boolean empty) {
                super.updateItem(rank, empty);

                if (empty || rank == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(rank.toString());
                    getStyleClass().add("rank-cell");

                    // Add medal icons for top 3
                    if (rank == 1) {
                        FontIcon medalIcon = new FontIcon("fas-medal");
                        medalIcon.getStyleClass().add("medal-icon");
                        setGraphic(medalIcon);
                    } else if (rank == 2) {
                        FontIcon medalIcon = new FontIcon("fas-medal");
                        medalIcon.getStyleClass().add("medal-icon-silver");
                        setGraphic(medalIcon);
                    } else if (rank == 3) {
                        FontIcon medalIcon = new FontIcon("fas-medal");
                        medalIcon.getStyleClass().add("medal-icon-bronze");
                        setGraphic(medalIcon);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        // Configure club column with logo and name
        clubColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getNomC()));

        clubColumn.setCellFactory(column -> new TableCell<Club, String>() {
            @Override
            protected void updateItem(String clubName, boolean empty) {
                super.updateItem(clubName, empty);

                if (empty || clubName == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Create a container for club logo and name
                    HBox container = new HBox();
                    container.setAlignment(Pos.CENTER_LEFT);
                    container.setSpacing(10);

                    // Try to get club logo (placeholder for now)
                    ImageView logoView = new ImageView();
                    try {
                        Club club = getTableView().getItems().get(getIndex());
                        String logoPath = club.getLogo(); // Assuming Club has getLogo method

                        if (logoPath != null && !logoPath.isEmpty()) {
                            File logoFile = new File(logoPath);
                            if (logoFile.exists()) {
                                logoView.setImage(new Image(logoFile.toURI().toString()));
                            } else {
                                URL placeholderUrl = getClass().getResource("/com/esprit/images/club_placeholder.png");
                                if (placeholderUrl != null) {
                                    logoView.setImage(new Image(placeholderUrl.toExternalForm()));
                                }
                            }
                        } else {
                            URL placeholderUrl = getClass().getResource("/com/esprit/images/club_placeholder.png");
                            if (placeholderUrl != null) {
                                logoView.setImage(new Image(placeholderUrl.toExternalForm()));
                            }
                        }
                    } catch (Exception e) {
                        // Use a default club icon
                        FontIcon clubIcon = new FontIcon("fas-shield-alt");
                        clubIcon.setIconColor(Color.web("#4a90e2"));
                        clubIcon.setIconSize(16);
                        container.getChildren().add(clubIcon);
                    }

                    if (logoView.getImage() != null) {
                        logoView.setFitHeight(20);
                        logoView.setFitWidth(20);
                        logoView.setPreserveRatio(true);
                        container.getChildren().add(logoView);
                    }

                    // Add club name
                    Label nameLabel = new Label(clubName);
                    nameLabel.setStyle("-fx-font-weight: normal;");
                    container.getChildren().add(nameLabel);

                    setGraphic(container);
                    setText(null);
                }
            }
        });

        // Configure points column
        pointsColumn.setCellValueFactory(new PropertyValueFactory<>("points"));
        pointsColumn.setCellFactory(column -> new TableCell<Club, Integer>() {
            @Override
            protected void updateItem(Integer points, boolean empty) {
                super.updateItem(points, empty);

                if (empty || points == null) {
                    setText(null);
                } else {
                    setText(points.toString());
                    getStyleClass().add("points-cell");
                }
            }
        });

        // Style rows based on position
        leaderboardTable.setRowFactory(tv -> new TableRow<Club>() {
            @Override
            protected void updateItem(Club club, boolean empty) {
                super.updateItem(club, empty);

                getStyleClass().removeAll("rank-1", "rank-2", "rank-3");

                if (!empty && club != null) {
                    int position = getIndex() + 1;
                    if (position == 1) {
                        getStyleClass().add("rank-1");
                    } else if (position == 2) {
                        getStyleClass().add("rank-2");
                    } else if (position == 3) {
                        getStyleClass().add("rank-3");
                    }
                }
            }
        });
    }

    private void loadLeaderboardData() throws SQLException {
        List<Club> clubs = clubService.getAll();

        // Sort clubs by points (highest first)
        clubs.sort(Comparator.comparing(Club::getPoints).reversed());

        leaderboardTable.setItems(FXCollections.observableArrayList(clubs));

        // Update top performers section
        updateTopPerformers(clubs);
    }

    private void updateTopPerformers(List<Club> clubs) {
        topPerformersContainer.getChildren().clear();

        // Only display if we have enough clubs
        if (clubs.size() < 3) {
            return;
        }

        // Get top 3 clubs
        List<Club> topClubs = clubs.subList(0, Math.min(3, clubs.size()));

        // Create performer cards
        for (int i = 0; i < topClubs.size(); i++) {
            VBox performerCard = createTopPerformerCard(topClubs.get(i), i + 1);
            topPerformersContainer.getChildren().add(performerCard);
        }
    }

    private VBox createTopPerformerCard(Club club, int rank) {
        VBox card = new VBox();
        card.setAlignment(Pos.CENTER);
        card.setSpacing(5);
        card.getStyleClass().add("top-performer");

        if (rank == 1) {
            card.getStyleClass().add("top-performer-first");
        } else if (rank == 2) {
            card.getStyleClass().add("top-performer-second");
        } else if (rank == 3) {
            card.getStyleClass().add("top-performer-third");
        }

        // Avatar container with colored circle
        StackPane avatarContainer = new StackPane();
        avatarContainer.getStyleClass().add("top-performer-avatar-container");

        Circle avatarBg = new Circle(25);
        avatarBg.getStyleClass().add("top-performer-avatar-bg");

        // Club logo or default icon
        ImageView logoView = new ImageView();
        try {
            String logoPath = club.getLogo();
            if (logoPath != null && !logoPath.isEmpty()) {
                File logoFile = new File(logoPath);
                if (logoFile.exists()) {
                    logoView.setImage(new Image(logoFile.toURI().toString()));
                } else {
                    // Try resource path
                    URL resourceUrl = getClass().getResource(logoPath);
                    if (resourceUrl != null) {
                        logoView.setImage(new Image(resourceUrl.toExternalForm()));
                    } else {
                        // Use placeholder
                        URL placeholderUrl = getClass().getResource("/com/esprit/images/club_placeholder.png");
                        if (placeholderUrl != null) {
                            logoView.setImage(new Image(placeholderUrl.toExternalForm()));
                        }
                    }
                }
            } else {
                // Use placeholder
                URL placeholderUrl = getClass().getResource("/com/esprit/images/club_placeholder.png");
                if (placeholderUrl != null) {
                    logoView.setImage(new Image(placeholderUrl.toExternalForm()));
                }
            }
        } catch (Exception e) {
            // Use a default club icon if image loading fails
            FontIcon clubIcon = new FontIcon("fas-shield-alt");
            clubIcon.setIconColor(Color.web("#4a90e2"));
            clubIcon.setIconSize(24);
            avatarContainer.getChildren().addAll(avatarBg, clubIcon);
        }

        if (logoView.getImage() != null) {
            logoView.setFitHeight(30);
            logoView.setFitWidth(30);
            logoView.setPreserveRatio(true);
            avatarContainer.getChildren().addAll(avatarBg, logoView);
        } else if (avatarContainer.getChildren().isEmpty()) {
            // If no image and no icon added yet, add default icon
            FontIcon clubIcon = new FontIcon("fas-shield-alt");
            clubIcon.setIconColor(Color.web("#4a90e2"));
            clubIcon.setIconSize(24);
            avatarContainer.getChildren().addAll(avatarBg, clubIcon);
        }

        // Club name
        Label nameLabel = new Label(club.getNomC());
        nameLabel.getStyleClass().add("top-performer-name");

        // Rank label
        String rankText;
        switch (rank) {
            case 1:
                rankText = "1st Place";
                break;
            case 2:
                rankText = "2nd Place";
                break;
            case 3:
                rankText = "3rd Place";
                break;
            default:
                rankText = rank + "th Place";
        }

        Label rankLabel = new Label(rankText);
        rankLabel.getStyleClass().add("top-performer-rank");

        // Points label with subtle styling
        Label pointsLabel = new Label(club.getPoints() + " pts");
        pointsLabel.getStyleClass().add("top-performer-rank");

        card.getChildren().addAll(avatarContainer, nameLabel, rankLabel, pointsLabel);
        return card;
    }

    // Utility method for fade-in animation
    private void fadeInNode(Node node, int delayMs) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), node);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setDelay(Duration.millis(delayMs));
        fadeIn.play();
    }

    private void showAlert(Alert.AlertType alertType, String title, String headerText, String contentText) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);

        try {
            // Apply custom CSS with proper error handling
            DialogPane dialogPane = alert.getDialogPane();
            URL cssResource = getClass().getResource("/com/esprit/styles/uniclubs.css");
            if (cssResource != null) {
                String css = cssResource.toExternalForm();
                dialogPane.getStylesheets().add(css);
                dialogPane.getStyleClass().add("custom-alert");

                // Try to add icon if available
                URL iconResource = getClass().getResource("/com/esprit/images/unicorn.png");
                if (iconResource != null) {
                    Stage stage = (Stage) dialogPane.getScene().getWindow();
                    stage.getIcons().add(new Image(iconResource.toExternalForm()));
                }
            } else {
                System.err.println("Warning: Could not find CSS resource at /com/esprit/styles/uniclubs.css");
            }
        } catch (Exception e) {
            System.err.println("Failed to apply custom styling to alert: " + e.getMessage());
            e.printStackTrace();
        }

        // The alert will show with or without custom styling
        alert.showAndWait();
    }

    private void showAlert(Alert.AlertType alertType, String title, String contentText) {
        showAlert(alertType, title, null, contentText);
    }

    // Utility method to refresh the competition view
    @FXML
    public void refreshView() {
        try {
            loadSeasons();
            loadLeaderboardData();
            loadCurrentSeasonMissions();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Refresh Error",
                    "Could not refresh competition data: " + e.getMessage());
        }
    }

    /**
     * Navigate to the Mission Progress view
     * This improved method ensures the view is properly initialized and displayed
     */
    @FXML
    public void navigateToMissionProgress() {
        try {
            // Load the Mission Progress view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/MissionProgressView.fxml"));
            Parent missionProgressView = loader.load();

            // Get reference to the controller
            MissionProgressViewController controller = loader.getController();

            // Explicitly call refresh to ensure data is loaded
            controller.refreshData();

            // Create a new scene to avoid any container conflicts
            Scene scene = new Scene(missionProgressView);

            // Get CSS stylesheets from current scene and add them to new scene
            Scene currentScene = competitionUserPane.getScene();
            if (currentScene != null && currentScene.getStylesheets() != null) {
                scene.getStylesheets().addAll(currentScene.getStylesheets());
            }

            // Add specific stylesheet for mission progress if needed
            URL cssUrl = getClass().getResource("/com/esprit/styles/mission-progress.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            // Set the new scene to the stage
            Stage stage = (Stage) competitionUserPane.getScene().getWindow();
            stage.setScene(scene);

            // Log navigation
            System.out.println("Navigated to Mission Progress View");

        } catch (IOException e) {
            e.printStackTrace();
            // Display error to user
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Could not load Mission Progress view");
            alert.setContentText("An error occurred while trying to navigate to the Mission Progress view: " + e.getMessage());
            alert.showAndWait();
        }
    }
}
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
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CompetitionUserController {

    private final CompetitionService competitionService = new CompetitionService();
    private final SaisonService saisonService = new SaisonService();
    private final ClubService clubService = ClubService.getInstance();

    @FXML private AnchorPane competitionUserPane;
    @FXML private FlowPane seasonsContainer;
    @FXML private VBox missionsContainer;
    @FXML private TableView<Club> leaderboardTable;
    @FXML private TableColumn<Club, Integer> rankColumn;
    @FXML private TableColumn<Club, String> clubColumn;
    @FXML private TableColumn<Club, Integer> pointsColumn;
    @FXML private Button backButton;
    @FXML private Label seasonTitle;

    private Saison currentSeason;

    @FXML
    public void initialize() {
        try {
            // Set up back button with icon
            FontIcon backIcon = new FontIcon("fas-arrow-left");
            backIcon.setIconColor(Color.WHITE);
            backButton.setGraphic(backIcon);
            backButton.setOnAction(event -> navigateBack());

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

    private void navigateBack() {
        try {
            URL dashboardUrl = getClass().getResource("/com/esprit/views/dashboard.fxml");
            if (dashboardUrl == null) {
                throw new IOException("Cannot find dashboard.fxml resource");
            }
            
            FXMLLoader loader = new FXMLLoader(dashboardUrl);
            AnchorPane previousScene = loader.load();
            competitionUserPane.getChildren().setAll(previousScene);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not navigate back: " + e.getMessage());
        }
    }

    private void loadSeasons() throws SQLException {
        seasonsContainer.getChildren().clear();

        List<Saison> seasons = saisonService.getAll();

        // Determine current season (most recent)
        if (!seasons.isEmpty()) {
            currentSeason = seasons.stream()
                    .max(Comparator.comparing(Saison::getDateFin))
                    .orElse(seasons.get(0));

            // Update the title if seasonTitle exists
            if (seasonTitle != null) {
                seasonTitle.setText(currentSeason.getNomSaison());
            }
        }

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

        // If this is the current season, highlight it
        if (currentSeason != null && currentSeason.getId() == season.getId()) {
            card.getStyleClass().add("current-season");

            // Add current indicator
            Label currentLabel = new Label("CURRENT");
            currentLabel.setStyle("-fx-background-color: #4a90e2; -fx-text-fill: white; -fx-font-size: 10px; " +
                    "-fx-padding: 3px 8px; -fx-background-radius: 3px;");
            currentLabel.setTranslateX(50);
            currentLabel.setTranslateY(-85);
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

        card.getChildren().addAll(iconContainer, titleLabel, descLabel, endDateLabel);
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
            Label noMissionsLabel = new Label("No active season found.");
            noMissionsLabel.getStyleClass().add("no-data-label");
            missionsContainer.getChildren().add(noMissionsLabel);
            return;
        }

        // Get missions for the current season
        List<Competition> missions = competitionService.getAll().stream()
                .filter(m -> m.getSaisonId() != null && m.getSaisonId().getId() == currentSeason.getId())
                .collect(Collectors.toList());

        if (missions.isEmpty()) {
            Label noMissionsLabel = new Label("No missions available for the current season.");
            noMissionsLabel.getStyleClass().add("no-data-label");
            missionsContainer.getChildren().add(noMissionsLabel);
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
}
package com.esprit.controllers;

import com.esprit.models.ClubWithMissionProgress;
import com.esprit.models.MissionProgress;
import com.esprit.services.MissionProgressService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MissionProgressViewController implements Initializable, MissionProgressService.MissionCompletionListener {

    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox clubsContainer;
    @FXML
    private ScrollPane missionDetailScrollPane;
    @FXML
    private VBox missionDetailView;
    @FXML
    private VBox missionDetailsContainer;
    @FXML
    private VBox clubsView;
    @FXML
    private Label detailClubName;
    @FXML
    private Button backButton;
    @FXML
    private ChoiceBox<String> filterBox;
    @FXML
    private ChoiceBox<String> viewModeBox;
    @FXML
    private Button refreshButton;
    @FXML
    private Label statusLabel;

    private final MissionProgressService missionProgressService;
    private ObservableList<ClubWithMissionProgress> allClubsWithProgress;
    private ObservableList<ClubWithMissionProgress> filteredClubs;
    private Timeline autoRefreshTimeline;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public MissionProgressViewController() {
        missionProgressService = MissionProgressService.getInstance();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("MissionProgressViewController initialize method starting");

        // Register this controller as a listener for mission completions
        missionProgressService.addCompletionListener(this);

        // Configure scroll panes
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setPannable(true);

        if (missionDetailScrollPane != null) {
            missionDetailScrollPane.setFitToWidth(true);
        }

        // Ensure the main clubs view is visible initially
        if (clubsView != null) {
            clubsView.setVisible(true);
            clubsView.setManaged(true);
        }

        // Make sure mission detail view is hidden initially
        if (missionDetailScrollPane != null) {
            missionDetailScrollPane.setVisible(false);
            missionDetailScrollPane.setManaged(false);
        }

        // Initialize filter dropdown
        initializeFilterBox();

        // Initialize view mode dropdown
        initializeViewModeBox();

        // Set up back button for mission detail view
        backButton.setOnAction(event -> {
            System.out.println("Back button clicked");
            missionDetailScrollPane.setVisible(false);
            missionDetailScrollPane.setManaged(false);
            clubsView.setVisible(true);
            clubsView.setManaged(true);
        });

        // Set up refresh button
        refreshButton.setOnAction(event -> {
            System.out.println("Refresh button clicked");
            refreshData();
        });

        // Load initial data
        refreshData();

        // Set up auto-refresh timeline (every 30 seconds)
        autoRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(30), e -> {
                    System.out.println("Auto refresh triggered");
                    Platform.runLater(this::refreshData);
                }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();

        System.out.println("MissionProgressViewController initialize method complete");
    }

    private void initializeFilterBox() {
        filterBox.setItems(FXCollections.observableArrayList(
                "All Clubs",
                "Most Progress",
                "Least Progress",
                "Most Completed Missions"));

        filterBox.getSelectionModel().selectFirst();

        filterBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                System.out.println("Filter changed to: " + newValue);
                applyFilter(newValue);
            }
        });
    }

    private void initializeViewModeBox() {
        viewModeBox.setItems(FXCollections.observableArrayList(
                "Cards View",
                "Compact Cards"));

        viewModeBox.getSelectionModel().selectFirst();

        viewModeBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                System.out.println("View mode changed to: " + newValue);
                applyViewMode(newValue);
            }
        });
    }

    private void applyViewMode(String viewMode) {
        // Update the view mode based on selection
        if (filteredClubs != null) {
            displayClubs(filteredClubs);
        }
    }

    public void refreshData() {
        try {
            updateStatus("Refreshing data...");

            System.out.println("Refreshing mission progress data...");

            List<ClubWithMissionProgress> freshData = missionProgressService.getClubsWithActiveMissionProgress();

            System.out.println("Retrieved " + (freshData != null ? freshData.size() : 0) + " clubs with progress data");

            if (allClubsWithProgress == null) {
                allClubsWithProgress = FXCollections
                        .observableArrayList(freshData != null ? freshData : FXCollections.emptyObservableList());
                filteredClubs = FXCollections
                        .observableArrayList(freshData != null ? freshData : FXCollections.emptyObservableList());
            } else {
                allClubsWithProgress.setAll(freshData != null ? freshData : FXCollections.emptyObservableList());

                String currentFilter = filterBox.getSelectionModel().getSelectedItem();
                if (currentFilter != null) {
                    applyFilter(currentFilter);
                } else {
                    filteredClubs.setAll(freshData != null ? freshData : FXCollections.emptyObservableList());
                }
            }

            displayClubs(filteredClubs);

            missionProgressService.checkExpiredMissions();

            updateStatus("Data refreshed at " + LocalDateTime.now().format(dateFormatter));
        } catch (Exception e) {
            System.err.println("Error refreshing data: " + e.getMessage());
            e.printStackTrace();
            updateStatus("Error refreshing data: " + e.getMessage());
        }
    }

    private void applyFilter(String filterOption) {
        if (allClubsWithProgress == null || allClubsWithProgress.isEmpty()) {
            filteredClubs = FXCollections.observableArrayList();
            displayClubs(filteredClubs);
            return;
        }

        List<ClubWithMissionProgress> tempList = allClubsWithProgress.stream().collect(Collectors.toList());

        switch (filterOption) {
            case "Most Progress":
                tempList.sort(
                        (c1, c2) -> Double.compare(c2.getTotalProgressPercentage(), c1.getTotalProgressPercentage()));
                break;
            case "Least Progress":
                tempList.sort(
                        (c1, c2) -> Double.compare(c1.getTotalProgressPercentage(), c2.getTotalProgressPercentage()));
                break;
            case "Most Completed Missions":
                tempList.sort(
                        (c1, c2) -> Integer.compare(c2.getCompletedMissionsCount(), c1.getCompletedMissionsCount()));
                break;
            default:
                tempList.sort((c1, c2) -> c1.getNomC().compareToIgnoreCase(c2.getNomC()));
                break;
        }

        filteredClubs.setAll(tempList);
        displayClubs(filteredClubs);

        updateStatus("Filter applied: " + filterOption);
    }

    private void displayClubs(List<ClubWithMissionProgress> clubs) {
        clubsContainer.getChildren().clear();

        if (clubs.isEmpty()) {
            Label emptyLabel = new Label("No active missions available");
            emptyLabel.getStyleClass().add("empty-label");
            clubsContainer.getChildren().add(emptyLabel);
            return;
        }

        // Adjust spacing based on view mode
        String viewMode = viewModeBox.getSelectionModel().getSelectedItem();
        if ("Compact Cards".equals(viewMode)) {
            clubsContainer.setSpacing(10);
        } else {
            clubsContainer.setSpacing(20);
        }

        for (ClubWithMissionProgress clubWithProgress : clubs) {
            VBox clubCard = createClubCard(clubWithProgress);
            clubsContainer.getChildren().add(clubCard);
        }

        // Force layout update to ensure all content is visible
        Platform.runLater(() -> {
            clubsContainer.requestLayout();
            scrollPane.requestLayout();
            scrollPane.setVvalue(scrollPane.getVvalue()); // Force viewport update

            // Additional workaround for ScrollPane content visibility
            if (scrollPane.getViewportBounds() != null) {
                double contentHeight = clubsContainer.getBoundsInLocal().getHeight();
                if (contentHeight > scrollPane.getViewportBounds().getHeight()) {
                    clubsContainer.setMinHeight(contentHeight);
                }
            }
        });
    }

    private VBox createClubCard(ClubWithMissionProgress clubWithProgress) {
        VBox clubCard = new VBox();

        // Apply different styles based on view mode
        String viewMode = viewModeBox.getSelectionModel().getSelectedItem();
        if ("Compact Cards".equals(viewMode)) {
            clubCard.getStyleClass().add("compact-card");
            clubCard.setSpacing(8);
        } else {
            clubCard.getStyleClass().add("club-card");
            clubCard.setSpacing(10);
        }

        clubCard.setMaxWidth(Double.MAX_VALUE);

        // Club header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        // Adjust header spacing based on view mode
        if ("Compact Cards".equals(viewMode)) {
            header.setSpacing(10);
        } else {
            header.setSpacing(15);
        }

        // Club logo - smaller for compact view
        ImageView logoView = createClubLogoImageView(clubWithProgress);
        if ("Compact Cards".equals(viewMode)) {
            logoView.setFitWidth(40);
            logoView.setFitHeight(40);
        }
        header.getChildren().add(logoView);

        // Club name and points
        VBox clubInfo = new VBox(5);
        Label clubName = new Label(clubWithProgress.getNomC());
        clubName.getStyleClass().add("club-name");

        Label pointsLabel = new Label(clubWithProgress.getPoints() + " points total");
        pointsLabel.getStyleClass().add("points-label");

        clubInfo.getChildren().addAll(clubName, pointsLabel);
        header.getChildren().add(clubInfo);

        // Overall progress
        ProgressBar overallProgress = new ProgressBar(clubWithProgress.getTotalProgressPercentage() / 100.0);
        overallProgress.setPrefWidth(150);

        if (clubWithProgress.getTotalProgressPercentage() >= 90) {
            overallProgress.getStyleClass().add("progress-bar-completed");
        } else if (clubWithProgress.getTotalProgressPercentage() >= 50) {
            overallProgress.getStyleClass().add("progress-bar-halfway");
        } else {
            overallProgress.getStyleClass().add("progress-bar-starting");
        }

        Label progressPercent = new Label(String.format("%.1f%%", clubWithProgress.getTotalProgressPercentage()));
        progressPercent.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");

        HBox progressBox = new HBox(10);
        progressBox.setAlignment(Pos.CENTER_RIGHT);
        progressBox.getChildren().addAll(overallProgress, progressPercent);
        HBox.setHgrow(progressBox, Priority.ALWAYS);

        header.getChildren().add(progressBox);
        clubCard.getChildren().add(header);

        // Separator
        Separator separator = new Separator();
        clubCard.getChildren().add(separator);

        // Mission progress section
        boolean hasMissions = false;

        // In compact mode, show simplified mission info
        if ("Compact Cards".equals(viewMode)) {
            // Show only mission count and overall progress
            int activeMissionCount = (int) clubWithProgress.getMissionProgressList().stream()
                    .filter(mp -> mp.getCompetition() != null && "activated".equals(mp.getCompetition().getStatus()))
                    .count();

            Label missionsInfo = new Label(activeMissionCount + " active missions");
            missionsInfo.setStyle("-fx-font-size: 14px; -fx-text-fill: #424242;");
            clubCard.getChildren().add(missionsInfo);

            hasMissions = activeMissionCount > 0;
        } else {
            // Full mission details for regular view
            Label missionsHeader = new Label("Active Missions");
            missionsHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 5 0 5 0;");
            clubCard.getChildren().add(missionsHeader);

            // Add each mission progress
            for (MissionProgress missionProgress : clubWithProgress.getMissionProgressList()) {
                if (missionProgress.getCompetition() != null &&
                        "activated".equals(missionProgress.getCompetition().getStatus())) {

                    VBox missionBox = createMissionProgressBox(missionProgress);
                    clubCard.getChildren().add(missionBox);
                    clubCard.getChildren().add(new Separator());
                    hasMissions = true;
                }
            }

            if (!hasMissions) {
                Label noMissionsLabel = new Label("No active missions for this club");
                noMissionsLabel.getStyleClass().add("no-missions-label");
                clubCard.getChildren().add(noMissionsLabel);
            }
        }

        if (!hasMissions) {
            Label noMissionsLabel = new Label("No active missions for this club");
            noMissionsLabel.getStyleClass().add("no-missions-label");
            clubCard.getChildren().add(noMissionsLabel);
        }

        // Summary
        HBox summary = new HBox(20);
        summary.getStyleClass().add("summary-box");
        summary.setAlignment(Pos.CENTER_LEFT);

        Label completedMissions = new Label(clubWithProgress.getCompletedMissionsCount() +
                " completed missions");
        completedMissions.getStyleClass().add("completed-missions-label");

        Label earnedPoints = new Label(clubWithProgress.getEarnedPoints() + "/" +
                clubWithProgress.getTotalPossiblePoints() + " points earned");
        earnedPoints.getStyleClass().add("earned-points-label");

        summary.getChildren().addAll(completedMissions, earnedPoints);
        clubCard.getChildren().add(summary);

        // View details button
        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        Button viewDetailsButton = new Button("View Details");
        viewDetailsButton.getStyleClass().add("view-button");
        viewDetailsButton.setOnAction(event -> showMissionDetail(clubWithProgress));

        buttonBox.getChildren().add(viewDetailsButton);
        clubCard.getChildren().add(buttonBox);

        return clubCard;
    }

    private VBox createMissionProgressBox(MissionProgress missionProgress) {
        VBox missionBox = new VBox(5);
        missionBox.getStyleClass().add("mission-box");

        // Mission name and points
        HBox missionHeader = new HBox(10);
        missionHeader.setAlignment(Pos.CENTER_LEFT);

        Label missionName = new Label(missionProgress.getCompetition().getNomComp());
        missionName.getStyleClass().add("mission-name");

        Label pointsLabel = new Label("+" + missionProgress.getCompetition().getPoints() + " pts");
        pointsLabel.getStyleClass().add("mission-points-label");

        missionHeader.getChildren().addAll(missionName, pointsLabel);

        if (missionProgress.getIsCompleted()) {
            Label completed = new Label("COMPLETED");
            completed.getStyleClass().add("mission-completed-label");
            missionHeader.getChildren().add(completed);
        }

        missionBox.getChildren().add(missionHeader);

        // Mission description
        Text description = new Text(missionProgress.getCompetition().getDescComp());
        description.setWrappingWidth(550);
        missionBox.getChildren().add(description);

        // Progress bar section
        HBox progressSection = new HBox(10);
        progressSection.setAlignment(Pos.CENTER_LEFT);
        progressSection.setPadding(new Insets(5, 0, 0, 0));

        double progressPercentage = missionProgress.getCompetition().getGoalValue() > 0
                ? (double) missionProgress.getProgress() / missionProgress.getCompetition().getGoalValue() * 100.0
                : 0;

        ProgressBar progressBar = new ProgressBar(missionProgress.getCompetition().getGoalValue() > 0
                ? (double) missionProgress.getProgress() / missionProgress.getCompetition().getGoalValue()
                : 0);
        progressBar.setPrefWidth(300);

        if (missionProgress.getIsCompleted()) {
            progressBar.getStyleClass().add("progress-bar-completed");
        } else if (progressPercentage > 50) {
            progressBar.getStyleClass().add("progress-bar-halfway");
        } else {
            progressBar.getStyleClass().add("progress-bar-starting");
        }

        Label progressText = new Label(String.format("%d/%d (%d%%)",
                missionProgress.getProgress(),
                missionProgress.getCompetition().getGoalValue(),
                (int) progressPercentage));

        progressSection.getChildren().addAll(progressBar, progressText);
        missionBox.getChildren().add(progressSection);

        // Goal description
        String goalTypeText = "";
        switch (missionProgress.getCompetition().getGoalType()) {
            case EVENT_COUNT:
                goalTypeText = "Create " + missionProgress.getCompetition().getGoalValue() + " events";
                break;
            case EVENT_LIKES:
                goalTypeText = "Get " + missionProgress.getCompetition().getGoalValue() + " likes on events";
                break;
            case MEMBER_COUNT:
                goalTypeText = "Reach " + missionProgress.getCompetition().getGoalValue() + " members";
                break;
        }

        Label goalTypeLabel = new Label(goalTypeText);
        goalTypeLabel.getStyleClass().add("goal-type-label");
        missionBox.getChildren().add(goalTypeLabel);

        // Add deadline if available
        if (missionProgress.getCompetition().getEndDate() != null) {
            Label deadlineLabel = new Label("Deadline: " +
                    missionProgress.getCompetition().getEndDate().format(dateFormatter));
            deadlineLabel.getStyleClass().add("deadline-label");
            missionBox.getChildren().add(deadlineLabel);
        }

        return missionBox;
    }

    private void showMissionDetail(ClubWithMissionProgress club) {
        if (club == null) {
            System.out.println("Cannot show details for null club");
            return;
        }

        System.out.println("Showing mission details for club: " + club.getNomC());

        // Update club name in detail view
        detailClubName.setText(club.getNomC());

        // Clear previous mission details
        missionDetailsContainer.getChildren().clear();

        // Filter active missions for this club
        List<MissionProgress> activeMissions = club.getMissionProgressList().stream()
                .filter(mp -> mp.getCompetition() != null && "activated".equals(mp.getCompetition().getStatus()))
                .collect(Collectors.toList());

        System.out.println("Club has " + activeMissions.size() + " active missions");

        if (activeMissions.isEmpty()) {
            Label noMissionsLabel = new Label("No active missions for this club");
            noMissionsLabel.getStyleClass().add("no-missions-label");
            missionDetailsContainer.getChildren().add(noMissionsLabel);
        } else {
            for (MissionProgress missionProgress : activeMissions) {
                VBox missionDetailBox = createDetailedMissionBox(missionProgress);
                missionDetailsContainer.getChildren().add(missionDetailBox);
            }
        }

        // Switch views
        clubsView.setVisible(false);
        clubsView.setManaged(false);
        missionDetailScrollPane.setVisible(true);
        missionDetailScrollPane.setManaged(true);
    }

    private VBox createDetailedMissionBox(MissionProgress missionProgress) {
        VBox missionBox = new VBox(10);
        missionBox.getStyleClass().add("mission-box");
        missionBox.setPadding(new Insets(15));

        // Mission header
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label missionName = new Label(missionProgress.getCompetition().getNomComp());
        missionName.getStyleClass().add("mission-name");
        missionName.setStyle("-fx-font-size: 16px;");

        Label pointsLabel = new Label("+" + missionProgress.getCompetition().getPoints() + " pts");
        pointsLabel.getStyleClass().add("mission-points-label");

        if (missionProgress.getIsCompleted()) {
            Label completedLabel = new Label("COMPLETED");
            completedLabel.getStyleClass().add("mission-completed-label");
            headerBox.getChildren().addAll(missionName, pointsLabel, completedLabel);
        } else {
            headerBox.getChildren().addAll(missionName, pointsLabel);
        }

        missionBox.getChildren().add(headerBox);

        // Description
        Text description = new Text(missionProgress.getCompetition().getDescComp());
        description.setWrappingWidth(650);
        missionBox.getChildren().add(description);

        // Progress section
        HBox progressSection = new HBox(15);
        progressSection.setAlignment(Pos.CENTER_LEFT);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(400);
        double progress = missionProgress.getCompetition().getGoalValue() > 0
                ? (double) missionProgress.getProgress() / missionProgress.getCompetition().getGoalValue()
                : 0;
        progressBar.setProgress(progress);

        if (missionProgress.getIsCompleted()) {
            progressBar.getStyleClass().add("progress-bar-completed");
        } else if (progress > 0.5) {
            progressBar.getStyleClass().add("progress-bar-halfway");
        } else {
            progressBar.getStyleClass().add("progress-bar-starting");
        }

        Label progressText = new Label(String.format("%d/%d (%d%%)",
                missionProgress.getProgress(),
                missionProgress.getCompetition().getGoalValue(),
                (int) (progress * 100)));

        progressSection.getChildren().addAll(progressBar, progressText);
        missionBox.getChildren().add(progressSection);

        // Goal type and deadline
        String goalTypeText = "";
        switch (missionProgress.getCompetition().getGoalType()) {
            case EVENT_COUNT:
                goalTypeText = "Create " + missionProgress.getCompetition().getGoalValue() + " events";
                break;
            case EVENT_LIKES:
                goalTypeText = "Get " + missionProgress.getCompetition().getGoalValue() + " likes on events";
                break;
            case MEMBER_COUNT:
                goalTypeText = "Reach " + missionProgress.getCompetition().getGoalValue() + " members";
                break;
        }

        Label goalTypeLabel = new Label("Goal: " + goalTypeText);
        goalTypeLabel.getStyleClass().add("goal-type-label");
        missionBox.getChildren().add(goalTypeLabel);

        if (missionProgress.getCompetition().getEndDate() != null) {
            Label deadlineLabel = new Label("Deadline: " +
                    missionProgress.getCompetition().getEndDate().format(dateFormatter));
            deadlineLabel.getStyleClass().add("deadline-label");
            missionBox.getChildren().add(deadlineLabel);
        }

        return missionBox;
    }

    private ImageView createClubLogoImageView(ClubWithMissionProgress clubWithProgress) {
        ImageView logoView = new ImageView();
        logoView.setFitWidth(50);
        logoView.setFitHeight(50);
        logoView.setPreserveRatio(true);

        boolean logoLoaded = false;

        try {
            if (clubWithProgress.getLogo() != null && !clubWithProgress.getLogo().isEmpty()) {
                File logoFile = new File(clubWithProgress.getLogo());
                if (logoFile.exists()) {
                    Image logoImage = new Image(logoFile.toURI().toString());
                    if (!logoImage.isError()) {
                        logoView.setImage(logoImage);
                        logoLoaded = true;
                    }
                }

                if (!logoLoaded) {
                    InputStream is = getClass().getResourceAsStream(clubWithProgress.getLogo());
                    if (is != null) {
                        Image logoImage = new Image(is);
                        if (!logoImage.isError()) {
                            logoView.setImage(logoImage);
                            logoLoaded = true;
                        }
                    }
                }

                if (!logoLoaded) {
                    InputStream is = getClass().getResourceAsStream("/com/esprit/images/" + clubWithProgress.getLogo());
                    if (is != null) {
                        Image logoImage = new Image(is);
                        if (!logoImage.isError()) {
                            logoView.setImage(logoImage);
                            logoLoaded = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading club logo: " + e.getMessage());
        }

        if (!logoLoaded) {
            try {
                String[] defaultLogoPaths = {
                        "/com/esprit/images/default-club.png",
                        "/com/esprit/images/club_placeholder.png",
                        "/com/esprit/images/default.png"
                };

                for (String path : defaultLogoPaths) {
                    InputStream is = getClass().getResourceAsStream(path);
                    if (is != null) {
                        Image defaultLogo = new Image(is);
                        logoView.setImage(defaultLogo);
                        logoLoaded = true;
                        break;
                    }
                }
            } catch (Exception e) {
                System.out.println("Error loading default logo: " + e.getMessage());
            }
        }

        if (!logoLoaded) {
            // Create a placeholder
            Region placeholder = new Region();
            placeholder.setPrefSize(50, 50);
            placeholder.setStyle("-fx-background-color: #2196F3; -fx-background-radius: 25;");
            logoView.setImage(null);
        }

        return logoView;
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    @Override
    public void onMissionCompleted(MissionProgress missionProgress) {
        Platform.runLater(() -> {
            System.out.println("Mission completed: " + missionProgress.getCompetition().getNomComp());
            refreshData();
        });
    }

    public void onClose() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
        missionProgressService.removeCompletionListener(this);
        updateStatus("View closed");
    }
}
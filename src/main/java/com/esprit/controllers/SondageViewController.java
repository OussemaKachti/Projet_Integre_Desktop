package com.esprit.controllers;

import com.esprit.models.Commentaire;
import com.esprit.models.ParticipationMembre;
import com.esprit.models.Sondage;
import com.esprit.models.ChoixSondage;
import com.esprit.models.Reponse;
import com.esprit.models.User;
import com.esprit.models.Club;
import com.esprit.services.CommentaireService;
import com.esprit.services.SondageService;
import com.esprit.services.ChoixSondageService;
import com.esprit.services.ReponseService;
import com.esprit.services.UserService;
import com.esprit.services.ClubService;
import com.esprit.services.ParticipationMembreService;
import com.esprit.utils.AlertUtils;
import com.esprit.utils.SessionManager;
import com.esprit.utils.NavigationManager;
import com.esprit.utils.EmailService;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import com.esprit.MainApp;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;
import java.util.HashSet;
import javafx.scene.Node;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.HashMap;

import javafx.concurrent.Task;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import java.util.Map;
import java.util.LinkedHashMap;
import com.esprit.services.AiService;
// import java.awt.Desktop;
import java.net.URI;
import javafx.scene.layout.GridPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import java.io.InputStream;

public class SondageViewController implements Initializable {

    // Références aux éléments FXML
    @FXML
    private TabPane tabPane;
    @FXML
    private VBox sondagesContainer;
    @FXML
    private TextArea commentTextArea;
    @FXML
    private ComboBox<String> filterClubComboBox;

    // Éléments du formulaire de création de sondage
    @FXML
    private TextField pollQuestionField;
    @FXML
    private Label questionErrorLabel;
    @FXML
    private Label optionsErrorLabel;
    @FXML
    private VBox pollOptionsContainer;
    @FXML
    private TextField option1Field;
    @FXML
    private TextField option2Field;
    @FXML
    private Button addOptionButton;
    @FXML
    private Button createPollButton;
    @FXML
    private Button viewAllPollsButton;

    // Services
    private final SondageService sondageService = new SondageService();
    private final CommentaireService commentaireService = new CommentaireService();
    private final ChoixSondageService choixService = new ChoixSondageService();
    private final ReponseService reponseService = new ReponseService();
    private final UserService userService = new UserService();
    private final ClubService clubService = new ClubService();
    private final EmailService emailService = EmailService.getInstance();
    private final ParticipationMembreService participationService = new ParticipationMembreService();

    // Variables d'état
    private Sondage currentSondage;
    private User currentUser;
    private final ObservableList<Sondage> sondagesList = FXCollections.observableArrayList();
    private final ObservableList<String> clubsList = FXCollections.observableArrayList();
    private int optionCount = 2; // Commence avec 2 options

    // Add these FXML field declarations at the top of the class with the other
    // declarations
    @FXML
    private StackPane userProfileContainer;

    @FXML
    private ImageView userProfilePic;

    @FXML
    private Label userNameLabel;

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

            // Load profile picture
            if (userProfilePic != null) {
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
                double radius = 20;
                userProfilePic.setClip(new javafx.scene.shape.Circle(radius, radius, radius));
            }

            // Check if user has admin role and add toxicity management button
            if (currentUser.getRole() != null && currentUser.getRole().equals("ADMINISTRATEUR")) {
                addToxicityManagementButton();
            }

            // Configure club filter
            setupClubFilter();

            // Apply CSS styles to components
            sondagesContainer.getStyleClass().add("polls-section");
            sondagesContainer.setSpacing(20);

            // Check if user is a club president
            Club userClub = clubService.findByPresident(currentUser.getId());
            boolean isPresident = userClub != null;

            // Show/hide elements based on user role
            VBox pollCreationContainer = (VBox) viewAllPollsButton.getParent().lookup(".poll-creation-container");
            if (pollCreationContainer != null) {
                pollCreationContainer.setVisible(isPresident);
                pollCreationContainer.setManaged(isPresident);
            }

            viewAllPollsButton.setVisible(isPresident);
            viewAllPollsButton.setManaged(isPresident);

            // Configure View All Polls button if user is president
            if (isPresident) {
                viewAllPollsButton.setOnAction(e -> handleViewAllPolls());
            }

            // Load polls
            loadSondages("all");
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.showError("Initialization Error", "An error occurred: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("Error", "Failed to initialize view: " + e.getMessage());
        }
    }

    /**
     * Load default profile picture when user's profile picture is not available
     */
    private void loadDefaultProfilePic() {
        try {
            // Try to load from class resources
            InputStream stream = getClass().getResourceAsStream("/com/esprit/images/default-profile.png");
            
            // Check if stream is null and try alternative paths
            if (stream == null) {
                // Try different paths
                stream = getClass().getResourceAsStream("/images/default-profile.png");
                
                if (stream == null) {
                    // Last resort - create a simple default image
                    WritableImage defaultImg = new WritableImage(45, 45);
                    userProfilePic.setImage(defaultImg);
                    return;
                }
            }
            
            Image defaultImage = new Image(stream);
            userProfilePic.setImage(defaultImage);
        } catch (Exception e) {
            System.err.println("Failed to load default profile picture: " + e.getMessage());
            // Create a simple colored circle as fallback
            WritableImage defaultImg = new WritableImage(45, 45);
            userProfilePic.setImage(defaultImg);
        }
    }

    private void setupClubFilter() throws SQLException {
        // Ajouter l'option pour tous les clubs
        clubsList.add("all");

        // Ajouter tous les noms de clubs (correction de la méthode)
        List<com.esprit.models.Club> clubs = sondageService.getInstance().getAll().stream()
                .map(sondage -> sondage.getClub())
                .distinct()
                .toList();

        for (com.esprit.models.Club club : clubs) {
            clubsList.add(club.getNomC());
        }

        filterClubComboBox.setItems(clubsList);
        filterClubComboBox.getSelectionModel().selectFirst();

        // Configurer l'événement de changement
        filterClubComboBox.setOnAction(event -> {
            String selectedClub = filterClubComboBox.getValue();
            try {
                loadSondages(selectedClub);
            } catch (SQLException e) {
                e.printStackTrace();
                AlertUtils.showError("Error", "Error loading polls: " + e.getMessage());
            }
        });
    }

    private void loadSondages(String clubFilter) throws SQLException {
        // Vider le conteneur
        sondagesContainer.getChildren().clear();

        // Récupérer les sondages (filtrer par club si nécessaire)
        ObservableList<Sondage> sondages;
        if ("all".equals(clubFilter)) {
            sondages = sondageService.getInstance().getAll();
        } else {
            // Pour simplifier, on va filtre en mémoire plutôt que d'utiliser getByClub
            sondages = FXCollections.observableArrayList(
                    sondageService.getInstance().getAll().stream()
                            .filter(s -> s.getClub().getNomC().equals(clubFilter))
                            .toList());
        }

        // Créer les éléments d'interface pour chaque sondage
        for (Sondage sondage : sondages) {
            VBox sondageBox = createSondageBox(sondage);
            sondagesContainer.getChildren().add(sondageBox);
        }
    }

    /**
     * Creates a VBox containing a single sondage (poll) display
     */
    private VBox createSondageBox(Sondage sondage) throws SQLException {
        VBox sondageBox = new VBox(10);
        sondageBox.getStyleClass().add("sondage-box");
        sondageBox.setPadding(new Insets(20));

        // User avatar - create ImageView and load image from user's profilePicture
        ImageView avatar = new ImageView();
        avatar.setFitHeight(40);
        avatar.setFitWidth(40);
        avatar.setPreserveRatio(true);
        avatar.getStyleClass().add("comment-avatar");

        // Get user's profile picture path
        String profilePicPath = sondage.getUser().getProfilePicture();

        // Debug - print profile pic path to console
        System.out.println("User ID: " + sondage.getUser().getId() +
                ", Name: " + sondage.getUser().getFirstName() +
                ", Profile Pic Path: " + profilePicPath);

        try {
            // Check if user has a profile picture
            if (profilePicPath != null && !profilePicPath.isEmpty()) {
                // Use the same approach as CommentsModalController - look in uploads/profiles
                // directory
                File imageFile = new File("uploads/profiles/" + profilePicPath);
                System.out.println("Trying to load from: " + imageFile.getAbsolutePath());

                if (imageFile.exists()) {
                    System.out.println("File exists, loading image");
                    avatar.setImage(new Image(imageFile.toURI().toString()));
                } else {
                    System.out.println("File does not exist");

                    // If file doesn't exist in uploads, try the direct path
                    File directFile = new File(profilePicPath);
                    if (directFile.exists()) {
                        System.out.println("Direct file exists, loading image");
                        avatar.setImage(new Image(directFile.toURI().toString()));
                    } else {
                        System.out.println("Direct file does not exist either");

                        // Finally try as a resource path
                        String resourcePath = "/images/" + profilePicPath;
                        System.out.println("Trying resource path: " + resourcePath);

                        try {
                            if (getClass().getResourceAsStream(resourcePath) != null) {
                                avatar.setImage(new Image(getClass().getResourceAsStream(resourcePath)));
                                System.out.println("Successfully loaded from resources");
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to load from resources: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading profile image: " + e.getMessage());
            e.printStackTrace();
        }

        // User name
        Label userName = new Label(sondage.getUser().getFirstName() + " " + sondage.getUser().getLastName());
        userName.getStyleClass().add("user-name");

        // Date separator
        Label dateSeparator = new Label(" • ");
        dateSeparator.getStyleClass().add("date-separator");

        // Poll date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        Label pollDate = new Label(sondage.getCreatedAt().format(formatter));
        pollDate.getStyleClass().add("poll-date");

        // Add club label if available
        Label clubLabel = null;
        if (sondage.getClub() != null) {
            clubLabel = new Label(" • " + sondage.getClub().getNomC());
            clubLabel.getStyleClass().add("club-label");
        }

        // Create HBox for user info
        HBox userInfoBox = new HBox(10);
        userInfoBox.setAlignment(Pos.CENTER_LEFT);
        userInfoBox.getChildren().addAll(avatar, userName, dateSeparator, pollDate);
        if (clubLabel != null) {
            userInfoBox.getChildren().add(clubLabel);
        }
        userInfoBox.getStyleClass().add("user-info");

        // Add View Summary button
        Button viewSummaryButton = new Button("View Summary");
        viewSummaryButton.getStyleClass().add("view-summary-button");
        viewSummaryButton.setOnAction(e -> showCommentsSummary(sondage));
        userInfoBox.getChildren().add(viewSummaryButton);
        HBox.setMargin(viewSummaryButton, new Insets(0, 0, 0, 10));

        // Poll question
        Label questionLabel = new Label(sondage.getQuestion());
        questionLabel.getStyleClass().add("sondage-question");
        questionLabel.setWrapText(true);

        // Add to sondage box
        VBox pollHeader = new VBox(5);
        pollHeader.getChildren().addAll(userInfoBox, questionLabel);

        // Poll options with radio buttons for voting
        VBox optionsView = createPollOptionsView(sondage);
        sondageBox.getChildren().addAll(pollHeader, optionsView);

        // Add a section divider
        Region divider = new Region();
        divider.getStyleClass().add("section-divider");
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setOpacity(0.7);
        VBox.setMargin(divider, new Insets(10, 0, 5, 0));

        // Comment button container
        HBox commentButtonContainer = new HBox();
        commentButtonContainer.getStyleClass().add("comment-button-container");
        commentButtonContainer.setAlignment(Pos.CENTER_RIGHT);

        // Comment icon and button with counter
        HBox commentsButtonWithIcon = new HBox(5);
        commentsButtonWithIcon.setAlignment(Pos.CENTER);

        // Create comment icon
        Label commentIcon = new Label("💬");
        commentIcon.getStyleClass().add("comment-icon");

        // Comment button with counter
        int commentCount = getCommentCount(sondage.getId());
        Button commentsButton = new Button(commentCount + " Comments");
        commentsButton.getStyleClass().add("comments-button");
        commentsButton.setOnAction(e -> openCommentsModal(sondage));

        // Add icon to button
        commentsButtonWithIcon.getChildren().addAll(commentIcon, commentsButton);

        // Add comment button to container
        commentButtonContainer.getChildren().add(commentsButtonWithIcon);

        // Add comments section to sondage box
        sondageBox.getChildren().addAll(divider, commentButtonContainer);

        // Add comment form
        VBox commentForm = new VBox(10);
        commentForm.getStyleClass().add("comment-form");
        commentForm.getStyleClass().add("comment-form-container");

        // Comment form header with icon
        HBox commentFormHeader = new HBox(8);
        commentFormHeader.setAlignment(Pos.CENTER_LEFT);

        // Comment form title
        Label commentFormTitle = new Label("Write a comment");
        commentFormTitle.getStyleClass().add("comment-form-title");
        commentFormHeader.getChildren().add(commentFormTitle);

        // Create container for textarea and mic button
        HBox commentInputContainer = new HBox(5);
        commentInputContainer.setAlignment(Pos.CENTER);
        commentInputContainer.getStyleClass().add("comment-textarea-container");
        commentInputContainer.setFillHeight(true);

        // Comment textarea
        TextArea commentTextArea = new TextArea();
        commentTextArea.setPromptText("Share your thoughts...");
        commentTextArea.getStyleClass().add("comment-textarea");
        commentTextArea.setPrefHeight(70);
        commentTextArea.setWrapText(true);
        HBox.setHgrow(commentTextArea, Priority.ALWAYS);

        // Microphone button for voice input
        Button micButton = new Button("🎤");
        micButton.getStyleClass().add("voice-input-btn");
        micButton.setTooltip(new Tooltip("Click to speak your comment"));

        // Get speech recognition service and check availability

        // If speech recognition is not available, disable the button

        // Add components to the container
        commentInputContainer.getChildren().addAll(commentTextArea, micButton);

        // Label for comment validation errors
        Label commentErrorLabel = new Label();
        commentErrorLabel.getStyleClass().add("validation-error");
        commentErrorLabel.setVisible(false);
        commentErrorLabel.setWrapText(true);
        commentErrorLabel.setText("Comment cannot be empty.");

        // Add real-time validation to the textarea
        commentTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
            validateComment(newValue, commentErrorLabel, commentTextArea);
        });

        // Add comment button container
        HBox addCommentButtonBox = new HBox();
        addCommentButtonBox.setAlignment(Pos.CENTER_RIGHT);
        addCommentButtonBox.setPadding(new Insets(5, 0, 0, 0));

        // Add comment button with icon
        HBox postButtonWithIcon = new HBox(5);
        postButtonWithIcon.setAlignment(Pos.CENTER);

        // Add comment button
        Button addCommentButton = new Button("Post Comment");
        addCommentButton.getStyleClass().add("post-comment-button");
        addCommentButton.setOnAction(e -> {
            try {
                String content = commentTextArea.getText().trim();
                // Validate the comment before submitting
                if (!validateComment(content, commentErrorLabel, commentTextArea)) {
                    return;
                }

                addComment(sondage, content);
                commentTextArea.clear();
                commentErrorLabel.setVisible(false);
            } catch (SQLException ex) {
                ex.printStackTrace();
                AlertUtils.showError("Error", "Failed to post comment: " + ex.getMessage());
            }
        });

        // Add icon to button
        postButtonWithIcon.getChildren().add(addCommentButton);
        addCommentButtonBox.getChildren().add(postButtonWithIcon);

        // Add all elements to the comment form
        commentForm.getChildren().addAll(commentFormHeader, commentInputContainer, commentErrorLabel,
                addCommentButtonBox);

        // Add comment form to sondage box
        sondageBox.getChildren().add(commentForm);

        return sondageBox;
    }

    private HBox createResultRow(ChoixSondage option, int totalVotes) throws SQLException {
        HBox resultRow = new HBox();
        resultRow.getStyleClass().add("result-row");
        resultRow.getStyleClass().add("poll-option");
        resultRow.setAlignment(Pos.CENTER_LEFT);
        resultRow.setSpacing(10);
        resultRow.setPadding(new Insets(5));

        // Nombre de votes pour cette option
        int optionVotes = reponseService.getVotesByChoix(option.getId());

        // Calculer le pourcentage
        double percentage = totalVotes > 0 ? (double) optionVotes / totalVotes * 100 : 0;

        // Label pour le texte de l'option
        Label optionLabel = new Label(option.getContenu());
        optionLabel.getStyleClass().add("option-label");
        optionLabel.setPrefWidth(200); // Increased width for better readability

        // Créer un spacer pour pousser la barre de progression et le pourcentage à
        // droite
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Barre de progression
        ProgressBar progressBar = new ProgressBar(totalVotes > 0 ? (double) optionVotes / totalVotes : 0);
        progressBar.getStyleClass().add("option-progress");
        progressBar.setPrefWidth(220); // Match CSS width

        // Apply color class based on percentage range
        if (percentage <= 25.0) {
            progressBar.getStyleClass().add("progress-bar-low");
        } else if (percentage <= 50.0) {
            progressBar.getStyleClass().add("progress-bar-medium-low");
        } else if (percentage <= 75.0) {
            progressBar.getStyleClass().add("progress-bar-medium-high");
        } else {
            progressBar.getStyleClass().add("progress-bar-high");
        }

        // Label pour le pourcentage
        Label percentageLabel = new Label(String.format("%.1f%% (%d votes)", percentage, optionVotes));
        percentageLabel.getStyleClass().add("percentage-label");

        // Ajouter tous les éléments à la ligne
        resultRow.getChildren().addAll(optionLabel, spacer, progressBar, percentageLabel);

        return resultRow;
    }

    /**
     * Creates the poll option rows with radio buttons for voting
     */
    private VBox createPollOptionsView(Sondage sondage) throws SQLException {
        VBox optionsContainer = new VBox(10);
        optionsContainer.getStyleClass().add("poll-options");
        optionsContainer.setPadding(new Insets(10));

        // Create a toggle group for radio buttons
        ToggleGroup optionsGroup = new ToggleGroup();

        // Get all options for this poll
        List<ChoixSondage> options = choixService.getBySondage(sondage.getId());

        // Get total votes for percentage calculation
        int totalVotes = getTotalVotes(sondage.getId());

        // Check if the current user has already voted and what their choice was
        ChoixSondage userChoice = getUserChoice(sondage);

        // Create option rows with radio buttons and progress bars
        for (int i = 0; i < options.size(); i++) {
            ChoixSondage option = options.get(i);

            HBox optionRow = new HBox(10);
            optionRow.getStyleClass().add("poll-option");
            optionRow.setAlignment(Pos.CENTER_LEFT);

            // Radio button for option selection
            RadioButton optionRadio = new RadioButton(option.getContenu());
            optionRadio.getStyleClass().add("poll-option-radio");
            optionRadio.setToggleGroup(optionsGroup);
            optionRadio.setUserData(option.getId());

            // If user already voted for this option, select it
            if (userChoice != null && userChoice.getId() == option.getId()) {
                optionRadio.setSelected(true);
            }

            // Pane to push progress bar to the right
            Pane spacer = new Pane();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Get votes for this option
            int votes = reponseService.getVotesByChoix(option.getId());
            double percentage = totalVotes > 0 ? (votes * 100.0 / totalVotes) : 0;

            // Progress bar to show vote percentage
            ProgressBar optionProgress = new ProgressBar(percentage / 100);
            optionProgress.setPrefWidth(220); // Match CSS width

            // Apply color class based on percentage range
            if (percentage <= 25.0) {
                optionProgress.getStyleClass().add("progress-bar-low");
            } else if (percentage <= 50.0) {
                optionProgress.getStyleClass().add("progress-bar-medium-low");
            } else if (percentage <= 75.0) {
                optionProgress.getStyleClass().add("progress-bar-medium-high");
            } else {
                optionProgress.getStyleClass().add("progress-bar-high");
            }

            optionProgress.getStyleClass().add("option-progress");

            // Percentage label
            Label percentageLabel = new Label(String.format("%.1f%%", percentage));
            percentageLabel.getStyleClass().add("percentage-label");

            optionRow.getChildren().addAll(optionRadio, spacer, optionProgress, percentageLabel);
            optionsContainer.getChildren().add(optionRow);
        }

        // Create a container for the voting controls and user's choice
        HBox controlsContainer = new HBox();
        controlsContainer.setAlignment(Pos.CENTER);
        controlsContainer.setPrefWidth(Double.MAX_VALUE);

        // Left side - Voting buttons
        HBox buttonsBox = new HBox(10);
        buttonsBox.getStyleClass().add("vote-buttons-container");

        Button voteButton;
        if (userChoice == null) {
            // User hasn't voted yet
            voteButton = new Button("Submit Vote");
        } else {
            // User has already voted
            voteButton = new Button("Change Vote");
        }
        voteButton.getStyleClass().add("vote-button");
        voteButton.setOnAction(e -> handleVote(sondage, optionsGroup));

        buttonsBox.getChildren().add(voteButton);

        // Add delete vote button if user has already voted
        if (userChoice != null) {
            Button deleteVoteButton = new Button("Delete Vote");
            deleteVoteButton.getStyleClass().add("delete-vote-button");
            deleteVoteButton.setOnAction(e -> {
                try {
                    // Show confirmation dialog
                    boolean confirmed = showCustomConfirmDialog(
                            "Delete Vote",
                            "Are you sure you want to delete your vote?",
                            "This action cannot be undone.");

                    if (confirmed) {
                        // Delete user's vote
                        reponseService.deleteUserVote(currentUser.getId(), sondage.getId());

                        // Show confirmation
                        showToast("Your vote has been deleted successfully!", "success");

                        // Refresh the view
                        refreshData();
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showToast("Failed to delete vote: " + ex.getMessage(), "error");
                }
            });

            buttonsBox.getChildren().add(deleteVoteButton);
        }

        // Right side - User's choice if already voted
        HBox userChoiceContainer = new HBox();
        userChoiceContainer.getStyleClass().add("choice-status-container");
        HBox.setHgrow(userChoiceContainer, Priority.ALWAYS);

        if (userChoice != null) {
            HBox userChoiceBox = new HBox(10);
            userChoiceBox.getStyleClass().add("user-choice-box");
            userChoiceBox.setAlignment(Pos.CENTER_RIGHT);

            Label yourChoiceLabel = new Label("Your choice:");
            yourChoiceLabel.getStyleClass().add("your-choice-label");

            Label userChoiceLabel = new Label(userChoice.getContenu());
            userChoiceLabel.getStyleClass().add("user-choice");

            userChoiceBox.getChildren().addAll(yourChoiceLabel, userChoiceLabel);
            userChoiceContainer.getChildren().add(userChoiceBox);
        }

        // Add components to the container
        controlsContainer.getChildren().addAll(buttonsBox, userChoiceContainer);
        optionsContainer.getChildren().add(controlsContainer);

        return optionsContainer;
    }

    /**
     * Handle user vote for a poll
     */
    @FXML
    private void handleVote(Sondage sondage, ToggleGroup optionsGroup) {
        try {
            // Get the selected radio button
            RadioButton selectedOption = (RadioButton) optionsGroup.getSelectedToggle();

            // Validate selection
            if (selectedOption == null) {
                showToast("Please select an option to vote.", "warning");
                return;
            }

            // Get the choice ID from the selected radio button's user data
            int choixId = (int) selectedOption.getUserData();

            // Check if user already voted
            boolean hasVoted = reponseService.hasUserVoted(currentUser.getId(), sondage.getId());

            if (hasVoted) {
                // Confirm before updating vote
                boolean confirmed = showCustomConfirmDialog(
                        "Change Vote",
                        "Are you sure you want to change your vote?",
                        "Your previous vote will be replaced.");

                if (confirmed) {
                    // Update existing vote
                    reponseService.updateUserVote(currentUser.getId(), sondage.getId(), choixId);
                    showToast("Your vote has been updated successfully!", "success");

                    // Refresh the view
                    refreshData();
                }
            } else {
                // Add new vote
                reponseService.addVote(currentUser.getId(), sondage.getId(), choixId);
                showToast("Your vote has been recorded successfully!", "success");

                // Refresh the view
                refreshData();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showToast("Failed to record vote: " + e.getMessage(), "error");
        }
    }

    /**
     * Add a comment to a poll
     */
    private void addComment(Sondage sondage, String content) throws SQLException {
        try {
            // Check if user is banned from commenting
            if (commentaireService.isUserBannedFromCommenting(currentUser.getId())) {
                showCommentBannedDialog();
                return;
            }

            // Create a temporary label and validate comment
            Label tempLabel = new Label();
            if (!validateComment(content, tempLabel, null)) {
                showToast(tempLabel.getText(), "warning");
                return;
            }

            // Create and save the comment
            Commentaire commentaire = new Commentaire();
            commentaire.setSondage(sondage);
            commentaire.setUser(currentUser);
            commentaire.setContenuComment(content);
            commentaire.setDateComment(LocalDate.now());

            commentaireService.add(commentaire);

            showToast("Your comment has been added successfully!", "success");
            refreshData();
        } catch (SecurityException e) {
            // This is thrown when a user is banned
            showCommentBannedDialog();
        }
    }

    /**
     * Shows a beautifully styled dialog when a user is banned from commenting
     */
    private void showCommentBannedDialog() {
        // Create the dialog
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Account Restricted");

        // Create the content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");
        content.setAlignment(Pos.CENTER);

        // Add warning icon
        HBox iconContainer = new HBox();
        iconContainer.setAlignment(Pos.CENTER);
        Text warningIcon = new Text("⚠️");
        warningIcon.setStyle("-fx-font-size: 48px; -fx-fill: #F44336;");
        iconContainer.getChildren().add(warningIcon);

        // Add title
        Label titleLabel = new Label("Commenting Restricted");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #F44336;");

        // Add explanation
        VBox explanationBox = new VBox(10);
        explanationBox.setStyle("-fx-background-color: #FFF8E1; -fx-padding: 15px; -fx-background-radius: 5px;");

        Label explanationLabel = new Label(
                "Your account has been restricted from commenting due to multiple violations of our community guidelines.");
        explanationLabel.setWrapText(true);
        explanationLabel.setStyle("-fx-font-size: 14px;");

        Label detailsLabel = new Label(
                "This restriction occurs after receiving 3 warnings for posting inappropriate content.");
        detailsLabel.setWrapText(true);
        detailsLabel.setStyle("-fx-font-size: 14px;");

        explanationBox.getChildren().addAll(explanationLabel, detailsLabel);

        // Add contact info
        VBox contactBox = new VBox(10);
        contactBox.setStyle(
                "-fx-padding: 15px; -fx-background-radius: 5px; -fx-border-color: #E0E0E0; -fx-border-radius: 5px;");

        Label contactLabel = new Label(
                "If you believe this is an error or wish to appeal this decision, please contact our support team:");
        contactLabel.setWrapText(true);

        Hyperlink emailLink = new Hyperlink("support@uniclubs.com");
        emailLink.setStyle("-fx-font-size: 14px;");
        // emailLink.setOnAction(e -> {
        // // Open default mail client
        // try {
        // Desktop.getDesktop().mail(new
        // URI("mailto:support@uniclubs.com?subject=Comment%20Ban%20Appeal"));
        // } catch (Exception ex) {
        // showToast("Unable to open email client", "error");
        // }
        // });

        contactBox.getChildren().addAll(contactLabel, emailLink);

        // Add close button
        Button closeButton = new Button("Close");
        closeButton.setStyle(
                "-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 20;");
        closeButton.setOnAction(e -> dialog.close());

        // Add all to content
        content.getChildren().addAll(iconContainer, titleLabel, explanationBox, contactBox, closeButton);

        // Set the content and show
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);
        dialog.getDialogPane().setPrefWidth(400);

        dialog.showAndWait();
    }

    /**
     * Custom alert dialog with modern styling
     */
    private void showCustomAlert(String title, String message, String type) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.setResizable(false);

        // Create the dialog container
        VBox dialogVBox = new VBox(15);
        dialogVBox.getStyleClass().add("custom-alert");

        // Add type-specific class for styling
        if ("success".equals(type)) {
            dialogVBox.getStyleClass().add("custom-alert-success");
        } else if ("warning".equals(type)) {
            dialogVBox.getStyleClass().add("custom-alert-warning");
        } else if ("error".equals(type)) {
            dialogVBox.getStyleClass().add("custom-alert-error");
        }

        dialogVBox.setPadding(new Insets(20));

        // Create icon and title in a horizontal box
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        // Add appropriate icon based on alert type
        Label iconLabel = new Label();
        iconLabel.getStyleClass().add("custom-alert-icon");

        if ("success".equals(type)) {
            iconLabel.setText("✓");
        } else if ("warning".equals(type)) {
            iconLabel.setText("⚠");
        } else if ("error".equals(type)) {
            iconLabel.setText("✕");
        } else {
            iconLabel.setText("ℹ");
        }

        // Dialog title
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("title");

        headerBox.getChildren().addAll(iconLabel, titleLabel);

        // Dialog message
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("content");

        // OK button
        Button okButton = new Button("OK");
        okButton.getStyleClass().addAll("custom-alert-button", "custom-alert-button-ok");
        okButton.setOnAction(e -> {
            // Fade out animation before closing
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), dialogVBox);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> dialogStage.close());
            fadeOut.play();
        });

        // Button container
        HBox buttonsBox = new HBox();
        buttonsBox.getStyleClass().add("buttons-box");
        buttonsBox.getChildren().add(okButton);

        // Add all elements to dialog
        dialogVBox.getChildren().addAll(headerBox, messageLabel, buttonsBox);

        // Set up background with drop shadow
        StackPane rootPane = new StackPane();
        rootPane.getStyleClass().add("custom-alert-background");

        // Make background semi-transparent and clickable to dismiss
        Region overlay = new Region();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3);");
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), dialogVBox);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(event -> dialogStage.close());
                fadeOut.play();
            }
        });

        rootPane.getChildren().addAll(overlay, dialogVBox);

        // Create scene with transparent background
        Scene dialogScene = new Scene(rootPane);
        dialogScene.setFill(Color.TRANSPARENT);
        dialogScene.getStylesheets()
                .add(getClass().getResource("/com/esprit/styles/sondage-style.css").toExternalForm());

        // Set and show the dialog with animation
        dialogStage.setScene(dialogScene);

        // Center on screen
        dialogStage.setOnShown(e -> {
            dialogStage.setX((Screen.getPrimary().getVisualBounds().getWidth() - dialogScene.getWidth()) / 2);
            dialogStage.setY((Screen.getPrimary().getVisualBounds().getHeight() - dialogScene.getHeight()) / 2);

            // Play fade-in animation
            dialogVBox.setOpacity(0);
            dialogVBox.setScaleX(0.9);
            dialogVBox.setScaleY(0.9);

            ParallelTransition pt = new ParallelTransition();

            FadeTransition fadeIn = new FadeTransition(Duration.millis(350), dialogVBox);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(350), dialogVBox);
            scaleIn.setFromX(0.9);
            scaleIn.setFromY(0.9);
            scaleIn.setToX(1);
            scaleIn.setToY(1);

            pt.getChildren().addAll(fadeIn, scaleIn);
            pt.play();
        });

        dialogStage.showAndWait();
    }

    /**
     * Custom confirmation dialog with OK/Cancel buttons
     * 
     * @return true if confirmed, false if canceled
     */
    private boolean showCustomConfirmDialog(String title, String message, String details) {
        final boolean[] result = { false };

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.setResizable(false);

        // Create the dialog container
        VBox dialogVBox = new VBox(15);
        dialogVBox.getStyleClass().add("custom-alert");
        dialogVBox.setPadding(new Insets(25));

        // Create icon and title in a horizontal box
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        // Add appropriate icon for confirmation
        Label iconLabel = new Label("❓");
        iconLabel.getStyleClass().addAll("custom-alert-icon", "custom-alert-confirm-icon");

        // Dialog title
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("title");

        headerBox.getChildren().addAll(iconLabel, titleLabel);

        // Dialog message and details in a VBox
        VBox messageBox = new VBox(8);

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("content");

        Label detailsLabel = new Label(details);
        detailsLabel.setWrapText(true);
        detailsLabel.getStyleClass().add("details-content");

        messageBox.getChildren().addAll(messageLabel, detailsLabel);

        // Buttons
        Button confirmButton = new Button("Confirm");
        confirmButton.getStyleClass().addAll("custom-alert-button", "custom-alert-button-ok");
        confirmButton.setOnAction(e -> {
            // Fade out animation before closing
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), dialogVBox);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> {
                result[0] = true;
                dialogStage.close();
            });
            fadeOut.play();
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().addAll("custom-alert-button", "custom-alert-button-cancel");
        cancelButton.setOnAction(e -> {
            // Fade out animation before closing
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), dialogVBox);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> {
                result[0] = false;
                dialogStage.close();
            });
            fadeOut.play();
        });

        // Button container
        HBox buttonsBox = new HBox(15);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.getStyleClass().add("buttons-box");
        buttonsBox.getChildren().addAll(cancelButton, confirmButton);

        // Set up background with drop shadow
        StackPane rootPane = new StackPane();
        rootPane.getStyleClass().add("custom-alert-background");

        // Make background semi-transparent and clickable to dismiss
        Region overlay = new Region();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3);");
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), dialogVBox);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(event -> {
                    result[0] = false;
                    dialogStage.close();
                });
                fadeOut.play();
            }
        });

        // Add all elements to dialog
        dialogVBox.getChildren().addAll(headerBox, messageBox, buttonsBox);
        rootPane.getChildren().addAll(overlay, dialogVBox);

        // Create scene with transparent background
        Scene dialogScene = new Scene(rootPane);
        dialogScene.setFill(Color.TRANSPARENT);
        dialogScene.getStylesheets()
                .add(getClass().getResource("/com/esprit/styles/sondage-style.css").toExternalForm());

        // Set and show the dialog with animation
        dialogStage.setScene(dialogScene);

        // Center on screen
        dialogStage.setOnShown(e -> {
            dialogStage.setX((Screen.getPrimary().getVisualBounds().getWidth() - dialogScene.getWidth()) / 2);
            dialogStage.setY((Screen.getPrimary().getVisualBounds().getHeight() - dialogScene.getHeight()) / 2);

            // Play fade-in animation
            dialogVBox.setOpacity(0);
            dialogVBox.setScaleX(0.9);
            dialogVBox.setScaleY(0.9);

            ParallelTransition pt = new ParallelTransition();

            FadeTransition fadeIn = new FadeTransition(Duration.millis(350), dialogVBox);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(350), dialogVBox);
            scaleIn.setFromX(0.9);
            scaleIn.setFromY(0.9);
            scaleIn.setToX(1);
            scaleIn.setToY(1);

            pt.getChildren().addAll(fadeIn, scaleIn);
            pt.play();
        });

        dialogStage.showAndWait();

        return result[0];
    }

    @FXML
    private void handleCreatePoll() {
        // Reset validation error messages
        questionErrorLabel.setVisible(false);
        optionsErrorLabel.setVisible(false);

        String question = pollQuestionField.getText().trim();
        boolean hasError = false;

        // Validate question
        if (question.isEmpty()) {
            questionErrorLabel.setText("Question cannot be empty.");
            questionErrorLabel.setVisible(true);
            hasError = true;
        } else if (!question.endsWith("?")) {
            questionErrorLabel.setText("Question must end with a question mark (?).");
            questionErrorLabel.setVisible(true);
            hasError = true;
        } else if (question.length() < 5) {
            questionErrorLabel.setText("Question must be at least 5 characters long.");
            questionErrorLabel.setVisible(true);
            hasError = true;
        }

        // Collect options
        List<String> options = new ArrayList<>();
        boolean hasEmptyOption = false;
        boolean hasInvalidOption = false;
        Pattern validOptionPattern = Pattern.compile(".*[a-zA-Z0-9].*"); // Must contain at least one alphanumeric char

        for (Node node : pollOptionsContainer.getChildren()) {
            if (node instanceof TextField) {
                TextField optionField = (TextField) node;
                String optionText = optionField.getText().trim();
                if (optionText.isEmpty()) {
                    hasEmptyOption = true;
                } else if (!validOptionPattern.matcher(optionText).matches()) {
                    hasInvalidOption = true;
                } else {
                    options.add(optionText);
                }
            }
        }

        // Validate options
        if (hasEmptyOption) {
            optionsErrorLabel.setText("All options must have content.");
            optionsErrorLabel.setVisible(true);
            hasError = true;
        } else if (hasInvalidOption) {
            optionsErrorLabel.setText("Options must contain at least one letter or number.");
            optionsErrorLabel.setVisible(true);
            hasError = true;
        } else if (options.size() < 2) {
            optionsErrorLabel.setText("Please add at least 2 options for the poll.");
            optionsErrorLabel.setVisible(true);
            hasError = true;
        } else {
            // Check for duplicate options
            Set<String> uniqueOptions = new HashSet<>(
                    options.stream().map(String::toLowerCase).collect(Collectors.toList()));
            if (uniqueOptions.size() != options.size()) {
                optionsErrorLabel.setText("Duplicate options are not allowed.");
                optionsErrorLabel.setVisible(true);
                hasError = true;
            }
        }

        if (hasError) {
            return;
        }

        try {
            // Create poll object
            Sondage sondage = new Sondage();
            sondage.setQuestion(question);
            sondage.setUser(currentUser);

            // Find the club associated with the current user (president)
            Club userClub = clubService.findByPresident(currentUser.getId());
            if (userClub == null) {
                showToast("You must be a club president to create polls.", "error");
                return;
            }
            sondage.setClub(userClub);

            // Add options to the poll
            for (String optionText : options) {
                ChoixSondage choix = new ChoixSondage();
                choix.setContenu(optionText);
                sondage.addChoix(choix);
            }

            // Save the poll
            sondageService.add(sondage);

            // Send notification emails to club members
            sendEmailsToClubMembers(sondage, userClub);

            // Show success toast
            showToast("Poll created successfully!", "success");

            // Reset form
            resetPollForm();

            // Reload sondages properly - using "all" as filter to show all polls
            filterClubComboBox.getSelectionModel().select("all");
            loadSondages("all");

        } catch (SQLException e) {
            e.printStackTrace();
            showToast("An error occurred while creating the poll: " + e.getMessage(), "error");
        }
    }

    private void resetPollForm() {
        pollQuestionField.clear();

        // Supprimer toutes les options sauf les deux premières
        if (pollOptionsContainer.getChildren().size() > 2) {
            pollOptionsContainer.getChildren().remove(2, pollOptionsContainer.getChildren().size());
        }

        // Réinitialiser les deux premières options
        option1Field.clear();
        option2Field.clear();

        // Réinitialiser le compteur d'options
        optionCount = 2;
    }

    /**
     * Gère le clic sur le bouton "View All Polls" pour ouvrir la vue de gestion des
     * sondages
     * et afficher les sondages du club dont l'utilisateur courant est président
     */
    private void handleViewAllPolls() {
        try {
            if (currentUser != null) {
                Club userClub = clubService.findByPresident(currentUser.getId());

                // If user is a club president, open PollManagement view
                if (userClub != null) {
                    // Load PollManagement view
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/com/esprit/views/PollManagementView.fxml"));
                    Parent root = loader.load();

                    PollManagementController controller = loader.getController();
                    controller.setPreviousScene(viewAllPollsButton.getScene());

                    Scene scene = new Scene(root);
                    Stage stage = (Stage) viewAllPollsButton.getScene().getWindow();
                    stage.setScene(scene);

                    // Maximize window without fullscreen
                    stage.setMaximized(true);

                } else {
                    // If user is not a president, show alert
                    showToast(
                            "You must be a club president to access poll management.",
                            "warning");
                }
            } else {
                AlertUtils.showError("Error", "User session not found.");
            }
        } catch (SQLException | IOException e) {
            AlertUtils.showError("Error", "Could not open Poll Management: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get the option chosen by the current user for this poll, if any
     */
    private ChoixSondage getUserChoice(Sondage sondage) throws SQLException {
        return reponseService.getUserResponse(currentUser.getId(), sondage.getId());
    }

    /**
     * Get total number of votes for a poll
     */
    private int getTotalVotes(int pollId) throws SQLException {
        return reponseService.getTotalVotesForPoll(pollId);
    }

    /**
     * Get comment count for a poll
     */
    private int getCommentCount(int pollId) throws SQLException {
        return commentaireService.getBySondage(pollId).size();
    }

    private void openCommentsModal(Sondage sondage) {
        try {
            // Sauvegarder le sondage actuel
            currentSondage = sondage;

            // Obtenir l'URL du fichier FXML et la vérifier
            URL fxmlUrl = getClass().getResource("/com/esprit/views/CommentsModal.fxml");
            if (fxmlUrl == null) {
                AlertUtils.showError("Error", "Unable to locate CommentsModal.fxml. Please check the file path.");
                return;
            }

            // Charger la vue de la modale des commentaires
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            VBox commentsModal = loader.load();

            // Récupérer le contrôleur de la modale
            CommentsModalController controller = loader.getController();
            if (controller == null) {
                AlertUtils.showError("Error", "Unable to load CommentsModalController.");
                return;
            }

            // Configure the controller with required data
            controller.setSondage(sondage);
            controller.setParentController(this);
            controller.setCurrentUser(currentUser);

            // Initialize the modal content after setting necessary data
            controller.setupModalContent();

            // Créer une nouvelle scène et fenêtre pour la modale
            Scene scene = new Scene(commentsModal);

            // Charger le fichier CSS
            URL cssUrl = getClass().getResource("/com/esprit/styles/sondage-style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("Warning: Unable to load CSS file. The modal will appear without styling.");
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Comments - " + sondage.getQuestion());
            stage.setScene(scene);
            stage.setMinWidth(800);
            stage.setMinHeight(600);

            // Afficher la modale
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("Error", "Failed to open comments modal: " + e.getMessage());
        }
    }

    /**
     * Show a summary of all comments for a poll
     * 
     * @param sondage The poll to show comments summary for
     */
    private void showCommentsSummary(Sondage sondage) {
        try {
            // Get all comments for this poll
            CommentaireService commentaireService = new CommentaireService();
            ObservableList<Commentaire> comments = commentaireService.getBySondage(sondage.getId());

            if (comments.isEmpty()) {
                showToast("There are no comments to summarize for this poll.", "info");
                return;
            }

            // Show loading dialog
            Stage loadingStage = new Stage();
            loadingStage.initModality(Modality.APPLICATION_MODAL);
            loadingStage.initStyle(StageStyle.TRANSPARENT);

            VBox loadingBox = new VBox(10);
            loadingBox.setAlignment(Pos.CENTER);
            loadingBox.setPadding(new Insets(20));
            loadingBox.getStyleClass().add("loading-dialog");

            Label loadingLabel = new Label("Generating summary...");
            loadingLabel.getStyleClass().add("loading-label");

            ProgressIndicator progressIndicator = new ProgressIndicator();
            progressIndicator.setMaxSize(50, 50);

            loadingBox.getChildren().addAll(progressIndicator, loadingLabel);

            Scene loadingScene = new Scene(loadingBox);
            loadingScene.setFill(Color.TRANSPARENT);
            loadingScene.getStylesheets()
                    .add(getClass().getResource("/com/esprit/styles/sondage-style.css").toExternalForm());

            loadingStage.setScene(loadingScene);
            loadingStage.show();

            // Generate summary in a background task
            Task<String> summaryTask = new Task<String>() {
                @Override
                protected String call() throws Exception {
                    // Use the AI-powered summary service
                    return commentaireService.generateCommentsSummary(sondage.getId());
                }
            };

            summaryTask.setOnSucceeded(event -> {
                loadingStage.close();
                String summaryText = summaryTask.getValue();
                showSummaryDialog(sondage, summaryText);
            });

            summaryTask.setOnFailed(event -> {
                loadingStage.close();
                Throwable exception = summaryTask.getException();
                showToast("Failed to generate summary: " + exception.getMessage(), "error");
            });

            new Thread(summaryTask).start();

        } catch (SQLException e) {
            e.printStackTrace();
            showToast("Failed to load comments: " + e.getMessage(), "error");
        }
    }

    /**
     * Shows a dialog with the comments summary
     * 
     * @param sondage The poll
     * @param summary The generated summary text
     */
    private void showSummaryDialog(Sondage sondage, String summary) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Poll Insights: " + sondage.getQuestion());
        dialogStage.setMinWidth(900);
        dialogStage.setMinHeight(600);

        VBox dialogContainer = new VBox(15);
        dialogContainer.getStyleClass().add("summary-dialog");
        dialogContainer.setPadding(new Insets(20));

        // Header
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(StringUtils.truncate(sondage.getQuestion(), 50)); // Truncate title if needed
        titleLabel.getStyleClass().add("summary-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(800); // Ensure title has enough space

        headerBox.getChildren().addAll(titleLabel);

        // Split content into two columns
        HBox contentBox = new HBox(20);
        contentBox.setAlignment(Pos.TOP_CENTER);

        // Left column - Charts
        VBox chartsColumn = new VBox(25);
        chartsColumn.setPrefWidth(400);
        chartsColumn.setAlignment(Pos.TOP_CENTER);

        // Participation chart
        VBox participationBox = new VBox(10);
        participationBox.setAlignment(Pos.TOP_LEFT);

        Label participationTitle = new Label("USER PARTICIPATION");
        participationTitle.getStyleClass().add("chart-title");

        PieChart pieChart = createParticipationChart(sondage);
        pieChart.setPrefSize(350, 200);

        participationBox.getChildren().addAll(participationTitle, pieChart);

        // Sentiment analysis chart
        VBox sentimentBox = new VBox(10);
        sentimentBox.setAlignment(Pos.TOP_LEFT);

        Label sentimentTitle = new Label("SENTIMENT ANALYSIS");
        sentimentTitle.getStyleClass().add("chart-title");

        BarChart<String, Number> barChart = createSentimentChart(summary);
        barChart.setPrefSize(350, 200);

        sentimentBox.getChildren().addAll(sentimentTitle, barChart);

        chartsColumn.getChildren().addAll(participationBox, sentimentBox);

        // Right column - Metrics & Insights
        VBox metricsColumn = new VBox(25);
        metricsColumn.setPrefWidth(400);
        metricsColumn.setAlignment(Pos.TOP_CENTER);

        // Metrics section
        VBox metricsBox = new VBox(10);
        metricsBox.setAlignment(Pos.TOP_LEFT);

        Label metricsTitle = new Label("KEY METRICS");
        metricsTitle.getStyleClass().add("chart-title");

        // Metrics grid
        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(10);
        metricsGrid.setVgap(10);

        try {
            int commentCount = getCommentCount(sondage.getId());
            int userCount = countUniqueCommenters(sondage.getId());
            double avgLength = calculateAverageCommentLength(sondage.getId());
            LocalDate latestDate = getLatestCommentDate(sondage.getId());

            metricsGrid.add(createMetricCard("TOTAL COMMENTS", String.valueOf(commentCount), "💬", "#4B83CD"), 0, 0);
            metricsGrid.add(createMetricCard("UNIQUE USERS", String.valueOf(userCount), "👥", "#28a745"), 1, 0);
            metricsGrid.add(createMetricCard("AVG LENGTH", String.format("%.1f", avgLength), "📏", "#ffc107"), 0, 1);

            String dateString = latestDate != null ? latestDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                    : "No comments";

            metricsGrid.add(createMetricCard("LATEST UPDATE", dateString, "📅", "#dc3545"), 1, 1);
        } catch (SQLException e) {
            System.err.println("Error loading metrics: " + e.getMessage());
        }

        metricsBox.getChildren().addAll(metricsTitle, metricsGrid);

        // AI Insights section
        VBox insightsBox = new VBox(10);
        insightsBox.setAlignment(Pos.TOP_LEFT);

        Label insightsTitle = new Label("AI-GENERATED INSIGHTS");
        insightsTitle.getStyleClass().add("chart-title");

        // Process and truncate summary for shorter insights
        String processedSummary = processRawSummary(summary);
        String shortenedSummary = StringUtils.truncate(processedSummary, 200); // Limit to about 2-3 lines

        TextFlow insightsText = new TextFlow();
        insightsText.setMaxWidth(380);
        insightsText.setPrefHeight(150);

        Text summaryBullet = new Text("• Summary of comments:\n");
        summaryBullet.setStyle("-fx-font-weight: bold;");

        Text summaryText = new Text(shortenedSummary);

        insightsText.getChildren().addAll(summaryBullet, summaryText);

        VBox.setVgrow(insightsText, Priority.ALWAYS);
        insightsBox.getChildren().addAll(insightsTitle, insightsText);

        metricsColumn.getChildren().addAll(metricsBox, insightsBox);

        // Combine columns
        contentBox.getChildren().addAll(chartsColumn, metricsColumn);

        // Button row at bottom with more space
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(15, 0, 0, 0));

        Button exportButton = new Button("Export Insights");
        exportButton.getStyleClass().add("action-button");
        exportButton.setOnAction(e -> exportInsights(sondage, summary));

        Button closeButton = new Button("Close");
        closeButton.getStyleClass().add("cancel-button");
        closeButton.setOnAction(e -> dialogStage.close());

        buttonBox.getChildren().addAll(exportButton, closeButton);

        // Add all elements to dialog
        dialogContainer.getChildren().addAll(headerBox, contentBox, buttonBox);

        Scene scene = new Scene(dialogContainer);
        String cssPath = getClass().getResource("/com/esprit/styles/sondage-style.css").toExternalForm();
        scene.getStylesheets().add(cssPath);

        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    /**
     * Creates a modern, colorful metric card for the dashboard
     */
    private VBox createMetricCard(String label, String value, String icon, String color) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-border-radius: 8px; -fx-background-radius: 8px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 0); -fx-border-color: #e0e0e0; -fx-border-width: 1px;");

        // Icon with colored circle background
        StackPane iconContainer = new StackPane();
        iconContainer.setMinSize(40, 40);
        iconContainer.setMaxSize(40, 40);

        Circle iconBackground = new Circle(20);
        iconBackground.setFill(Color.web(color));
        iconBackground.setOpacity(0.15);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: " + color + ";");

        iconContainer.getChildren().addAll(iconBackground, iconLabel);

        // Metric value
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333333;");

        // Metric label
        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #777777;");

        card.getChildren().addAll(iconContainer, valueLabel, nameLabel);
        return card;
    }

    /**
     * Extract sentiment information from the AI summary
     */
    private Map<String, Double> extractSentiment(String summary) {
        Map<String, Double> sentiments = new LinkedHashMap<>();

        // Detect sentiment words in the summary text
        String lowerSummary = summary.toLowerCase();

        boolean hasPositive = lowerSummary.contains("positive") || lowerSummary.contains("favor") ||
                lowerSummary.contains("good") || lowerSummary.contains("great") ||
                lowerSummary.contains("like") || lowerSummary.contains("appreciate");

        boolean hasNegative = lowerSummary.contains("negative") || lowerSummary.contains("against") ||
                lowerSummary.contains("bad") || lowerSummary.contains("poor") ||
                lowerSummary.contains("dislike") || lowerSummary.contains("issue");

        boolean hasMixed = lowerSummary.contains("mixed") || lowerSummary.contains("both") ||
                lowerSummary.contains("varied") || lowerSummary.contains("diverse") ||
                lowerSummary.contains("some") || lowerSummary.contains("other");

        boolean hasNeutral = lowerSummary.contains("neutral") || lowerSummary.contains("unclear") ||
                lowerSummary.contains("undecided");

        // Simple algorithm to determine sentiment values
        double positive = 0.0;
        double negative = 0.0;
        double neutral = 15.0; // Baseline minimum

        // Adjust based on keywords
        if (hasPositive)
            positive += 45.0;
        if (hasNegative)
            negative += 30.0;
        if (hasMixed) {
            positive += 25.0;
            negative += 25.0;
        }
        if (hasNeutral)
            neutral += 25.0;

        // Ensure we have something to show even if no clear sentiment
        if (positive < 15.0)
            positive = 15.0;
        if (negative < 10.0)
            negative = 10.0;

        // Normalize to ensure total is 100%
        double total = positive + negative + neutral;
        positive = (positive / total) * 100;
        negative = (negative / total) * 100;
        neutral = (neutral / total) * 100;

        sentiments.put("Positive", positive);
        sentiments.put("Neutral", neutral);
        sentiments.put("Negative", negative);

        return sentiments;
    }

    /**
     * Process the raw summary to make it more concise and visually appealing
     */
    private String processRawSummary(String rawSummary) {
        // Remove any "Error:" prefix if present
        if (rawSummary.startsWith("Error:")) {
            return "Unable to generate summary. Please try again later.";
        }

        // Split into sentences
        String[] sentences = rawSummary.split("(?<=[.!?])\\s+");

        // If we have multiple sentences, format as bullet points
        if (sentences.length > 1) {
            StringBuilder formatted = new StringBuilder();

            // Add title if there's no clear one
            if (!rawSummary.toLowerCase().contains("summary") && !rawSummary.toLowerCase().contains("overview")) {
                formatted.append("Key Insights:\n\n");
            }

            // Add each sentence as a bullet point
            for (String sentence : sentences) {
                if (sentence.trim().isEmpty())
                    continue;
                formatted.append("• ").append(sentence.trim()).append("\n\n");
            }

            return formatted.toString();
        }

        // If it's just one sentence, return as is
        return rawSummary;
    }

    // Méthode appelée par d'autres contrôleurs pour rafraîchir les données
    public void refreshData() {
        try {
            loadSondages(filterClubComboBox.getValue());
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.showError("Error", "Error refreshing data: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddOption() {
        optionCount++;
        TextField newOptionField = new TextField();
        newOptionField.setPromptText("Option " + optionCount);
        newOptionField.getStyleClass().add("input-box");
        pollOptionsContainer.getChildren().add(newOptionField);
    }

    private boolean validateComment(String content, Label commentErrorLabel, TextArea commentTextArea) {
        if (content == null || content.trim().isEmpty()) {
            commentErrorLabel.setText("Comment cannot be empty.");
            commentErrorLabel.setVisible(true);
            return false;
        }

        // Check minimum length (2 characters)
        if (content.trim().length() < 2) {
            commentErrorLabel.setText("Comment must be at least 2 characters long.");
            commentErrorLabel.setVisible(true);
            return false;
        }

        // Check maximum length (20 characters)
        if (content.trim().length() > 20) {
            commentErrorLabel.setText("Comment is too long. Maximum 20 characters allowed.");
            commentErrorLabel.setVisible(true);
            return false;
        }

        // Check for inappropriate words
        List<String> inappropriateWords = Arrays.asList(
                "insulte", "grossier", "offensive", "vulgar", "idiot", "stupid");

        // Highlight inappropriate words in the textarea and show error message
        String lowercaseContent = content.toLowerCase();
        for (String word : inappropriateWords) {
            if (lowercaseContent.contains(word.toLowerCase())) {
                // Create a styled text version to highlight the inappropriate word
                String errorMessage = "Comment contains inappropriate word: \"" + word + "\"";
                commentErrorLabel.setText(errorMessage);
                commentErrorLabel.setVisible(true);

                // Mark the inappropriate word in the textarea by using CSS
                int startIndex = lowercaseContent.indexOf(word.toLowerCase());
                int endIndex = startIndex + word.length();

                // We can't directly style parts of TextArea, but we can show the validation
                // error
                return false;
            }
        }

        // Check for too many uppercase letters (more than 50% of alphabetic characters)
        int uppercaseCount = 0;
        int lowercaseCount = 0;

        for (char c : content.toCharArray()) {
            if (Character.isUpperCase(c)) {
                uppercaseCount++;
            } else if (Character.isLowerCase(c)) {
                lowercaseCount++;
            }
        }

        if (uppercaseCount + lowercaseCount > 0 &&
                (double) uppercaseCount / (uppercaseCount + lowercaseCount) > 0.5) {
            commentErrorLabel.setText("Too many uppercase letters. Please avoid shouting.");
            commentErrorLabel.setVisible(true);
            return false;
        }

        // If all validations pass, hide the error message and return true
        commentErrorLabel.setVisible(false);
        return true;
    }

    /**
     * Envoie des emails aux membres du club pour les informer du nouveau sondage
     * 
     * @param sondage le sondage créé
     * @param club    le club auquel appartient le sondage
     */
    private void sendEmailsToClubMembers(Sondage sondage, Club club) {
        try {
            // Récupérer les membres actifs du club (statut "accepte")
            List<ParticipationMembre> clubMembers = participationService.getParticipationsByClubAndStatut(
                    club.getId(), "accepte");

            // Préparer les options pour le template d'email
            String[] optionsArray = sondage.getChoix().stream()
                    .map(ChoixSondage::getContenu)
                    .toArray(String[]::new);

            int emailsSent = 0;
            int emailErrors = 0;

            // Journaliser le nombre de membres
            System.out.println("Sending emails to " + clubMembers.size() + " club members");

            // Envoyer un email à chaque membre
            for (ParticipationMembre participation : clubMembers) {
                User member = participation.getUser();

                // Vérifier que le membre a un email valide
                if (member != null && member.getEmail() != null && !member.getEmail().isEmpty()) {
                    // Créer le contenu HTML personnalisé
                    String emailContent = emailService.createNewPollEmailTemplate(
                            club.getNomC(),
                            member.getLastName(),
                            sondage.getQuestion(),
                            optionsArray);

                    // Envoyer l'email de manière asynchrone
                    emailService.sendEmailAsync(
                            member.getEmail(),
                            "Nouveau sondage dans votre club: " + club.getNomC(),
                            emailContent)
                            .thenAccept(success -> {
                                if (success) {
                                    System.out.println("Email sent successfully to: " + member.getEmail());
                                } else {
                                    System.err.println("Failed to send email to: " + member.getEmail());
                                }
                            });

                    emailsSent++;
                } else {
                    System.out.println("Member doesn't have a valid email: " +
                            (member != null ? member.getFullName() : "null"));
                    emailErrors++;
                }
            }

            // Afficher un résumé des emails envoyés
            if (emailsSent > 0) {
                showToast(
                        String.format("Notification emails sent to %d club members", emailsSent),
                        "success");
            }

        } catch (SQLException e) {
            System.err.println("Error sending notification emails: " + e.getMessage());
            e.printStackTrace();
            showToast(
                    "Failed to send notification emails to club members: " + e.getMessage(),
                    "warning");
        }
    }

    // Add these methods for the navbar
    @FXML
    public void navigateToHome() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/home.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) sondagesContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    public void navigateToPolls() throws IOException {
        // Already in polls view, do nothing or refresh
        refreshData();
    }

    @FXML
    public void navigateToMyClub() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Clubs.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) sondagesContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    public void navigateToClubs() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/ShowClubs.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) sondagesContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    public void navigateToProfile() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Profile.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) sondagesContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    public void handleLogout() throws IOException {
        // Clear the session
        SessionManager.getInstance().clearSession();

        // Navigate to login page
   FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/login.fxml"));
        Parent root = loader.load();
        
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        
        // Use the utility method for consistent setup
        MainApp.setupStage(stage, root, "Login - UNICLUBS",true);
    }

    @FXML
    public void navigateToEvents() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/AfficherEvent.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) sondagesContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    public void navigateToProducts() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/produit/ProduitView.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) sondagesContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToCompetition() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/UserCompetition.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    public void navigateToContact() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Contact.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) sondagesContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    /**
     * Shows a professional toast notification that automatically disappears after a
     * few seconds
     * 
     * @param message The message to display
     * @param type    The type of toast (success, error, warning)
     */
    private void showToast(String message, String type) {
        try {
            // Get the scene's root node
            StackPane root;
            if (sondagesContainer.getScene() != null && sondagesContainer.getScene().getRoot() instanceof StackPane) {
                root = (StackPane) sondagesContainer.getScene().getRoot();
            } else {
                // Create a new stack pane to overlay the toast
                root = new StackPane();
                if (sondagesContainer.getScene() != null) {
                    Scene currentScene = sondagesContainer.getScene();
                    // Create a new scene with StackPane as root that contains the original root
                    Node originalRoot = currentScene.getRoot();
                    root.getChildren().add(originalRoot);
                    currentScene.setRoot(root);
                } else {
                    return; // No scene available yet
                }
            }

            // Store the final reference to root for use in lambdas
            final StackPane finalRoot = root;

            // Create the toast container
            HBox toast = new HBox();
            toast.setMaxWidth(400);
            toast.setMaxHeight(70);
            toast.setPrefHeight(60);
            toast.setMinHeight(60);
            toast.setAlignment(Pos.CENTER_LEFT);
            toast.setSpacing(15);
            toast.setPadding(new Insets(15, 20, 15, 20));
            toast.setStyle("-fx-background-radius: 8; -fx-background-color: " +
                    (type.equals("success") ? "#4caf50" : type.equals("error") ? "#f44336" : "#ff9800") + ";" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 3);");

            // Create icon based on type
            Label icon = new Label();
            icon.setTextFill(Color.WHITE);
            icon.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

            if (type.equals("success")) {
                icon.setText("✓");
            } else if (type.equals("error")) {
                icon.setText("✖");
            } else {
                icon.setText("⚠");
            }

            // Create message text
            Label text = new Label(message);
            text.setTextFill(Color.WHITE);
            text.setStyle("-fx-font-size: 15px; -fx-font-weight: 500;");
            text.setWrapText(true);

            // Add elements to toast
            toast.getChildren().addAll(icon, text);

            // Position toast at the bottom center
            StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);
            StackPane.setMargin(toast, new Insets(0, 0, 100, 0));

            // Ensure toast is on top
            toast.toFront();

            // Store the final toast reference for lambdas
            final HBox finalToast = toast;

            // Scale and fade-in transition
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(180), finalToast);
            scaleIn.setFromX(0.8);
            scaleIn.setFromY(0.8);
            scaleIn.setToX(1);
            scaleIn.setToY(1);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(180), finalToast);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            ParallelTransition parallelIn = new ParallelTransition(scaleIn, fadeIn);

            // Fade-out transition
            FadeTransition fadeOut = new FadeTransition(Duration.millis(350), finalToast);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setDelay(Duration.seconds(3));
            fadeOut.setOnFinished(e -> {
                finalRoot.getChildren().remove(finalToast);
            });

            // Add toast to scene and play animations
            finalRoot.getChildren().add(finalToast);

            parallelIn.play();
            parallelIn.setOnFinished(e -> fadeOut.play());

        } catch (Exception e) {
            System.err.println("Error showing toast: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void showToxicityManagementPanel() {
        try {
            // Create the dashboard layout
            VBox dashboardRoot = new VBox(15);
            dashboardRoot.setPadding(new Insets(20));
            dashboardRoot.setStyle("-fx-background-color: white;");

            // Create title area
            HBox titleArea = new HBox();
            titleArea.setAlignment(Pos.CENTER_LEFT);

            ImageView warningIcon = new ImageView(
                    new Image(getClass().getResourceAsStream("/com/esprit/images/warning-icon.png")));
            if (warningIcon.getImage().isError()) {
                // Fallback if image not found
                warningIcon = createTextIcon("⚠️", 30, Color.ORANGE);
            } else {
                warningIcon.setFitWidth(30);
                warningIcon.setFitHeight(30);
            }

            Label titleLabel = new Label("AI Comment Moderation Dashboard");
            titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

            titleArea.getChildren().addAll(warningIcon, new Region() {
                {
                    setMinWidth(10);
                }
            }, titleLabel);

            // Create statistics area
            HBox statsArea = new HBox(20);
            statsArea.setAlignment(Pos.CENTER);
            statsArea.setPadding(new Insets(15));
            statsArea.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 10px;");

            // Get statistics from the CommentaireService
            CommentaireService commentaireService = new CommentaireService();
            int totalComments = commentaireService.getTotalComments();
            int flaggedComments = commentaireService.getFlaggedComments();
            int todayComments = commentaireService.getTodayComments();
            double flaggedPercentage = totalComments > 0 ? (flaggedComments * 100.0 / totalComments) : 0;

            VBox stat1 = createStatBox("Total Comments", String.valueOf(totalComments), "blue");
            VBox stat2 = createStatBox("Flagged Comments", String.valueOf(flaggedComments), "orange");
            VBox stat3 = createStatBox("Today's Comments", String.valueOf(todayComments), "green");
            VBox stat4 = createStatBox("Flagged %", String.format("%.1f%%", flaggedPercentage),
                    flaggedPercentage > 10 ? "red" : flaggedPercentage > 5 ? "orange" : "green");

            statsArea.getChildren().addAll(stat1, stat2, stat3, stat4);

            // Create flagged comments table
            TableView<Commentaire> commentsTable = new TableView<>();
            commentsTable.setPlaceholder(new Label("No flagged comments found."));
            commentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<Commentaire, String> userCol = new TableColumn<>("User");
            userCol.setCellValueFactory(data -> {
                User user = data.getValue().getUser();
                return new SimpleStringProperty(
                        user != null ? user.getFirstName() + " " + user.getLastName() : "Unknown");
            });

            TableColumn<Commentaire, String> pollCol = new TableColumn<>("Poll");
            pollCol.setCellValueFactory(data -> {
                Sondage sondage = data.getValue().getSondage();
                return new SimpleStringProperty(sondage != null ? sondage.getQuestion() : "Unknown");
            });

            TableColumn<Commentaire, String> contentCol = new TableColumn<>("Content");
            contentCol.setCellValueFactory(data -> {
                String content = data.getValue().getContenuComment();
                if (commentaireService.isContentFlaggedAsToxic(content)) {
                    // Extract original content if available in the database for admin view
                    return new SimpleStringProperty("[FLAGGED CONTENT]");
                } else {
                    return new SimpleStringProperty(content);
                }
            });
            contentCol.setPrefWidth(300);

            TableColumn<Commentaire, LocalDate> dateCol = new TableColumn<>("Date");
            dateCol.setCellValueFactory(new PropertyValueFactory<>("dateComment"));
            dateCol.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    if (empty || date == null) {
                        setText(null);
                    } else {
                        setText(date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
                    }
                }
            });

            TableColumn<Commentaire, Void> actionsCol = new TableColumn<>("Actions");
            actionsCol.setCellFactory(param -> new TableCell<>() {
                private final Button viewBtn = new Button("View");
                private final Button approveBtn = new Button("Approve");
                private final Button deleteBtn = new Button("Delete");

                {
                    viewBtn.setStyle("-fx-background-color: #4285F4; -fx-text-fill: white;");
                    approveBtn.setStyle("-fx-background-color: #0F9D58; -fx-text-fill: white;");
                    deleteBtn.setStyle("-fx-background-color: #DB4437; -fx-text-fill: white;");

                    viewBtn.setOnAction(event -> {
                        Commentaire comment = getTableView().getItems().get(getIndex());
                        showToxicityAnalysisDialog(comment);
                    });

                    approveBtn.setOnAction(event -> {
                        Commentaire comment = getTableView().getItems().get(getIndex());
                        approveComment(comment);
                        refreshFlaggedComments(commentsTable);
                    });

                    deleteBtn.setOnAction(event -> {
                        Commentaire comment = getTableView().getItems().get(getIndex());
                        try {
                            commentaireService.delete(comment.getId());
                            getTableView().getItems().remove(comment);
                            showToast("Comment deleted successfully", "success");
                        } catch (SQLException e) {
                            showToast("Error deleting comment: " + e.getMessage(), "error");
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        HBox buttons = new HBox(5);
                        buttons.getChildren().addAll(viewBtn, approveBtn, deleteBtn);
                        setGraphic(buttons);
                    }
                }
            });

            commentsTable.getColumns().addAll(userCol, pollCol, contentCol, dateCol, actionsCol);

            // Load flagged comments
            refreshFlaggedComments(commentsTable);

            // Create refresh button
            Button refreshButton = new Button("Refresh Comments");
            refreshButton.setStyle("-fx-background-color: #4285F4; -fx-text-fill: white;");
            refreshButton.setOnAction(e -> refreshFlaggedComments(commentsTable));

            // Add all components to the dashboard
            dashboardRoot.getChildren().addAll(
                    titleArea,
                    new Separator(),
                    statsArea,
                    new Label("Flagged Comments") {
                        {
                            setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
                        }
                    },
                    commentsTable,
                    refreshButton);

            VBox.setVgrow(commentsTable, Priority.ALWAYS);

            // Create and show the dialog
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Comment Moderation Dashboard");
            dialog.getDialogPane().setContent(dashboardRoot);
            dialog.getDialogPane().setPrefSize(900, 700);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            // Apply styles to dialog
            dialog.getDialogPane().setStyle("-fx-background-color: white;");

            dialog.showAndWait();

        } catch (SQLException e) {
            showToast("Error loading comment data: " + e.getMessage(), "error");
        }
    }

    /**
     * Shows a detailed toxicity analysis dialog for a comment
     */
    private void showToxicityAnalysisDialog(Commentaire comment) {
        try {
            // Get the original comment content by extracting from the warning message or
            // database
            String warningMessage = comment.getContenuComment();
            String originalContent = "[Original content not available]";

            // Create toxicity analysis
            com.esprit.utils.AiService aiService = new com.esprit.utils.AiService();
            JSONObject toxicityAnalysis;

            // If we can get the original content, analyze it
            // Otherwise, just create empty analysis
            if (originalContent.equals("[Original content not available]")) {
                // Create empty analysis result
                toxicityAnalysis = new JSONObject();
                toxicityAnalysis.put("isToxic", true);
                toxicityAnalysis.put("reason", "Comment was previously flagged as toxic");
                toxicityAnalysis.put("toxicWords", new JSONArray());

                JSONObject categories = new JSONObject();
                categories.put("profanity", new JSONArray());
                categories.put("offensive", new JSONArray());
                categories.put("discriminatory", new JSONArray());
                categories.put("threatening", new JSONArray());
                categories.put("formatting", new JSONArray());
                toxicityAnalysis.put("categories", categories);
            } else {
                toxicityAnalysis = aiService.analyzeToxicity(originalContent);
            }

            // Create the analysis dialog layout
            VBox root = new VBox(15);
            root.setPadding(new Insets(20));
            root.setStyle("-fx-background-color: white;");

            // Add title
            Label titleLabel = new Label("Toxicity Analysis");
            titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

            // Add user info
            User user = comment.getUser();
            HBox userInfo = new HBox(10);
            userInfo.setAlignment(Pos.CENTER_LEFT);

            Label userLabel = new Label("Comment by: ");
            userLabel.setStyle("-fx-font-weight: bold;");

            Label userName = new Label(user != null ? user.getFirstName() + " " + user.getLastName() : "Unknown User");

            Label emailLabel = new Label("Email: ");
            emailLabel.setStyle("-fx-font-weight: bold;");

            Label userEmail = new Label(user != null ? user.getEmail() : "unknown@email.com");

            userInfo.getChildren().addAll(userLabel, userName, new Region() {
                {
                    setMinWidth(20);
                }
            },
                    emailLabel, userEmail);

            // Add flagged comment section
            VBox commentBox = new VBox(5);
            commentBox.setStyle("-fx-background-color: #FFF3E0; -fx-padding: 10px; -fx-background-radius: 5px;");

            Label commentLabel = new Label("Flagged Comment Content:");
            commentLabel.setStyle("-fx-font-weight: bold;");

            TextArea commentArea = new TextArea(warningMessage);
            commentArea.setEditable(false);
            commentArea.setWrapText(true);
            commentArea.setPrefRowCount(3);
            commentArea.setStyle("-fx-control-inner-background: #FFF3E0; -fx-border-color: transparent;");

            commentBox.getChildren().addAll(commentLabel, commentArea);

            // Create analysis section
            VBox analysisBox = new VBox(10);
            analysisBox.setStyle("-fx-background-color: #E8F5E9; -fx-padding: 10px; -fx-background-radius: 5px;");

            Label analysisLabel = new Label("AI Analysis Results:");
            analysisLabel.setStyle("-fx-font-weight: bold;");

            // Main reason
            HBox reasonBox = new HBox(10);
            reasonBox.setAlignment(Pos.CENTER_LEFT);

            Label reasonLabel = new Label("Detection Reason:");
            reasonLabel.setStyle("-fx-font-weight: bold;");

            Label reasonValue = new Label(toxicityAnalysis.getString("reason"));
            reasonValue.setStyle("-fx-text-fill: #D32F2F;");

            reasonBox.getChildren().addAll(reasonLabel, reasonValue);

            // Categories section
            VBox categoriesBox = new VBox(5);
            categoriesBox.setStyle("-fx-background-color: white; -fx-padding: 10px; -fx-background-radius: 5px;");

            Label categoriesLabel = new Label("Detected Categories:");
            categoriesLabel.setStyle("-fx-font-weight: bold;");

            categoriesBox.getChildren().add(categoriesLabel);

            // Add each category
            JSONObject categories = toxicityAnalysis.getJSONObject("categories");
            String[] categoryNames = { "profanity", "offensive", "discriminatory", "threatening", "formatting" };
            String[] categoryDisplayNames = { "Profanity", "Offensive Language", "Discriminatory Language",
                    "Threatening Content", "Formatting Issues" };
            String[] categoryColors = { "#E57373", "#FFB74D", "#BA68C8", "#F44336", "#90CAF9" };

            boolean hasAnyDetection = false;

            for (int i = 0; i < categoryNames.length; i++) {
                JSONArray categoryItems = categories.getJSONArray(categoryNames[i]);
                if (categoryItems.length() > 0) {
                    hasAnyDetection = true;

                    HBox categoryBox = new HBox(10);
                    categoryBox.setAlignment(Pos.CENTER_LEFT);
                    categoryBox.setPadding(new Insets(5));

                    Label categoryNameLabel = new Label(categoryDisplayNames[i] + ": ");
                    categoryNameLabel.setStyle("-fx-font-weight: bold;");

                    // Create color indicator
                    Region colorIndicator = new Region();
                    colorIndicator.setMinSize(12, 12);
                    colorIndicator.setMaxSize(12, 12);
                    colorIndicator.setStyle("-fx-background-color: " + categoryColors[i] + "; " +
                            "-fx-background-radius: 6px;");

                    // Create detected items
                    StringBuilder itemsText = new StringBuilder();
                    for (int j = 0; j < categoryItems.length(); j++) {
                        itemsText.append(categoryItems.getString(j));
                        if (j < categoryItems.length() - 1) {
                            itemsText.append(", ");
                        }
                    }

                    Label itemsLabel = new Label(itemsText.toString());
                    itemsLabel.setStyle("-fx-text-fill: " + categoryColors[i] + "; -fx-font-weight: bold;");

                    categoryBox.getChildren().addAll(colorIndicator, categoryNameLabel, itemsLabel);
                    categoriesBox.getChildren().add(categoryBox);
                }
            }

            if (!hasAnyDetection) {
                Label noDetectionLabel = new Label("No specific categories detected");
                noDetectionLabel.setStyle("-fx-font-style: italic;");
                categoriesBox.getChildren().add(noDetectionLabel);
            }

            analysisBox.getChildren().addAll(analysisLabel, reasonBox, categoriesBox);

            // User action section
            HBox actionBox = new HBox(15);
            actionBox.setAlignment(Pos.CENTER);
            actionBox.setPadding(new Insets(10));

            Button approveBtn = new Button("Approve Comment");
            approveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            approveBtn.setPrefWidth(200);
            approveBtn.setOnAction(e -> {
                approveComment(comment);
                // Close the dialog by getting the Stage and calling close()
                ((Stage) approveBtn.getScene().getWindow()).close();
            });

            Button deleteBtn = new Button("Delete Comment");
            deleteBtn.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
            deleteBtn.setPrefWidth(200);
            deleteBtn.setOnAction(e -> {
                try {
                    CommentaireService commentaireService = new CommentaireService();
                    commentaireService.delete(comment.getId());
                    showToast("Comment deleted successfully", "success");
                    // Close the dialog
                    ((Stage) deleteBtn.getScene().getWindow()).close();
                } catch (SQLException ex) {
                    showToast("Error deleting comment: " + ex.getMessage(), "error");
                }
            });

            actionBox.getChildren().addAll(approveBtn, deleteBtn);

            // Add all components to root
            root.getChildren().addAll(
                    titleLabel,
                    new Separator(),
                    userInfo,
                    commentBox,
                    analysisBox,
                    actionBox);

            // Create and show dialog
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Comment Toxicity Analysis");
            dialog.getDialogPane().setContent(root);
            dialog.getDialogPane().setPrefSize(700, 600);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error showing analysis: " + e.getMessage(), "error");
        }
    }

    /**
     * Approves a previously flagged comment by restoring its original content
     */
    private void approveComment(Commentaire comment) {
        try {
            // This is a simplified implementation since we don't have access to original
            // content
            // In a real implementation, we would store the original content in a separate
            // column
            // For now, we'll just replace the warning message with a generic approved
            // message

            comment.setContenuComment("[Comment was reviewed and approved by moderator]");

            CommentaireService commentaireService = new CommentaireService();
            commentaireService.update(comment);

            showToast("Comment approved successfully", "success");
        } catch (SQLException e) {
            showToast("Error approving comment: " + e.getMessage(), "error");
        }
    }

    /**
     * Refreshes the flagged comments table with the latest data
     */
    private void refreshFlaggedComments(TableView<Commentaire> table) {
        try {
            CommentaireService commentaireService = new CommentaireService();
            ObservableList<Commentaire> allComments = commentaireService.getAllComments();

            // Filter only flagged comments
            ObservableList<Commentaire> flaggedComments = allComments.filtered(
                    comment -> commentaireService.isCommentFlaggedAsToxic(comment));

            table.setItems(flaggedComments);
        } catch (SQLException e) {
            showToast("Error refreshing comments: " + e.getMessage(), "error");
        }
    }

    /**
     * Creates a statistic box for the dashboard
     */
    private VBox createStatBox(String title, String value, String color) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(15));
        box.setMinWidth(150);
        box.setStyle("-fx-background-color: white; -fx-border-radius: 5px; " +
                "-fx-background-radius: 5px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 5);");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        Label valueLabel = new Label(value);
        String colorStyle;
        switch (color) {
            case "red":
                colorStyle = "#F44336";
                break;
            case "green":
                colorStyle = "#4CAF50";
                break;
            case "blue":
                colorStyle = "#2196F3";
                break;
            case "orange":
                colorStyle = "#FF9800";
                break;
            default:
                colorStyle = "#666666";
        }
        valueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + colorStyle + ";");

        box.getChildren().addAll(titleLabel, valueLabel);
        return box;
    }

    /**
     * Creates a text-based icon when image is not available
     */
    private ImageView createTextIcon(String text, double size, Color color) {
        Text textNode = new Text(text);
        textNode.setFont(Font.font("System", FontWeight.BOLD, size));
        textNode.setFill(color);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);

        WritableImage image = textNode.snapshot(params, null);
        return new ImageView(image);
    }

    /**
     * Adds a toxicity management button to the sidebar for admin users
     */
    private void addToxicityManagementButton() {
        try {
            // Find the sidebar VBox (the second child of the HBox in the ScrollPane)
            ScrollPane scrollPane = (ScrollPane) ((VBox) tabPane.getScene().getRoot()).getChildren().get(2);
            HBox contentHBox = (HBox) scrollPane.getContent();
            VBox sidebar = (VBox) contentHBox.getChildren().get(1);

            // Create a styled button for toxicity management
            Button toxicityButton = new Button("AI Comment Moderation");
            toxicityButton.getStyleClass().add("view-all-polls-button");
            toxicityButton.setPrefWidth(250.0);
            toxicityButton.setGraphic(createToxicityIcon());
            toxicityButton.setContentDisplay(ContentDisplay.LEFT);
            toxicityButton.setOnAction(e -> showToxicityManagementPanel());

            // Add a margin above the button
            VBox.setMargin(toxicityButton, new Insets(10, 0, 5, 0));

            // Add the button to the sidebar, right after the view all polls button
            sidebar.getChildren().add(3, toxicityButton);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to add toxicity management button: " + e.getMessage());
        }
    }

    /**
     * Creates an icon for the toxicity management button
     */
    private HBox createToxicityIcon() {
        HBox iconContainer = new HBox();
        iconContainer.setAlignment(Pos.CENTER);

        Text icon = new Text("⚠️");
        icon.setStyle("-fx-fill: #FF9800;");

        iconContainer.getChildren().add(icon);
        return iconContainer;
    }

    /**
     * Creates a PieChart showing participation distribution for a poll
     * 
     * @param sondage The poll to create the chart for
     * @return A configured PieChart
     */
    private PieChart createParticipationChart(Sondage sondage) {
        try {
            ObservableList<Commentaire> comments = commentaireService.getBySondage(sondage.getId());
            Map<String, Integer> userCommentCounts = new HashMap<>();

            for (Commentaire comment : comments) {
                String userName = comment.getUser().getFirstName();
                userCommentCounts.put(userName, userCommentCounts.getOrDefault(userName, 0) + 1);
            }

            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
            for (Map.Entry<String, Integer> entry : userCommentCounts.entrySet()) {
                pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
            }

            PieChart chart = new PieChart(pieChartData);
            chart.setTitle("");
            chart.setLabelsVisible(false);
            chart.setLegendVisible(true);
            chart.setLegendSide(Side.RIGHT);

            // Add tooltips to pie slices
            for (final PieChart.Data data : chart.getData()) {
                Tooltip tooltip = new Tooltip(String.format("%s: %d comments",
                        data.getName(), (int) data.getPieValue()));
                Tooltip.install(data.getNode(), tooltip);
            }

            return chart;
        } catch (SQLException e) {
            e.printStackTrace();
            return new PieChart();
        }
    }

    /**
     * Creates a BarChart showing sentiment analysis
     * 
     * @param summary The summary text to analyze
     * @return A configured BarChart
     */
    private BarChart<String, Number> createSentimentChart(String summary) {
        Map<String, Double> sentiments = extractSentiment(summary);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(0, 100, 20);
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
            @Override
            public String toString(Number object) {
                return object.intValue() + "%";
            }
        });

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("");
        chart.setLegendVisible(false);
        chart.setAnimated(true);

        // Hide grid lines for cleaner look
        chart.setHorizontalGridLinesVisible(false);
        chart.setVerticalGridLinesVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        sentiments.forEach((sentiment, value) -> {
            series.getData().add(new XYChart.Data<>(sentiment, value));
        });

        chart.getData().add(series);

        // Style the bars with different colors
        int i = 0;
        String[] colors = { "#4B83CD", "#47B39C", "#FFC107" };
        for (XYChart.Data<String, Number> data : series.getData()) {
            String color = colors[i % colors.length];
            data.getNode().setStyle("-fx-bar-fill: " + color + ";");
            i++;
        }

        return chart;
    }

    /**
     * Counts the number of unique users who commented on a poll
     * 
     * @param pollId The ID of the poll
     * @return Count of unique commenters
     */
    private int countUniqueCommenters(int pollId) throws SQLException {
        ObservableList<Commentaire> comments = commentaireService.getBySondage(pollId);
        Set<Integer> uniqueUserIds = new HashSet<>();

        for (Commentaire comment : comments) {
            uniqueUserIds.add(comment.getUser().getId());
        }

        return uniqueUserIds.size();
    }

    /**
     * Calculates the average comment length for a poll
     * 
     * @param pollId The ID of the poll
     * @return Average comment length
     */
    private double calculateAverageCommentLength(int pollId) throws SQLException {
        ObservableList<Commentaire> comments = commentaireService.getBySondage(pollId);

        if (comments.isEmpty()) {
            return 0;
        }

        int totalLength = 0;
        for (Commentaire comment : comments) {
            totalLength += comment.getContenuComment().length();
        }

        return (double) totalLength / comments.size();
    }

    /**
     * Gets the date of the most recent comment on a poll
     * 
     * @param pollId The ID of the poll
     * @return Date of the most recent comment
     */
    private LocalDate getLatestCommentDate(int pollId) throws SQLException {
        ObservableList<Commentaire> comments = commentaireService.getBySondage(pollId);

        if (comments.isEmpty()) {
            return null;
        }

        return comments.stream()
                .map(Commentaire::getDateComment)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    /**
     * Exports insights about a poll to a file or clipboard
     * 
     * @param sondage The poll
     * @param summary The summary text
     */
    private void exportInsights(Sondage sondage, String summary) {
        try {
            // Create content for export
            StringBuilder content = new StringBuilder();
            content.append("# Poll Insights: ").append(sondage.getQuestion()).append("\n\n");
            content.append("## Summary\n").append(processRawSummary(summary)).append("\n\n");

            // Add statistics
            content.append("## Statistics\n");
            content.append("- Total Comments: ").append(getCommentCount(sondage.getId())).append("\n");
            content.append("- Unique Commenters: ").append(countUniqueCommenters(sondage.getId())).append("\n");
            content.append("- Avg Comment Length: ")
                    .append(String.format("%.1f", calculateAverageCommentLength(sondage.getId()))).append("\n");

            // Copy to clipboard
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(content.toString());
            clipboard.setContent(clipboardContent);

            showToast("Insights copied to clipboard", "success");
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error exporting insights: " + e.getMessage(), "error");
        }
    }

    /**
     * Truncates a string to a specified length and adds ellipsis if needed
     */
    private static class StringUtils {
        public static String truncate(String text, int maxLength) {
            if (text == null || text.length() <= maxLength) {
                return text;
            }
            return text.substring(0, maxLength - 3) + "...";
        }
    }
}
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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import java.util.Set;
import java.util.HashSet;
import javafx.scene.Node;
import java.util.regex.Pattern;
import java.util.Arrays;
import javafx.concurrent.Task;

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
    private final ParticipationMembreService participationService = ParticipationMembreService.getInstance();

    // Variables d'état
    private Sondage currentSondage;
    private User currentUser;
    private final ObservableList<Sondage> sondagesList = FXCollections.observableArrayList();
    private final ObservableList<String> clubsList = FXCollections.observableArrayList();
    private int optionCount = 2; // Commence avec 2 options

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            // Récupérer l'utilisateur connecté
            currentUser = userService.getById(8); // Utilisateur par défaut pour les tests (ID 2)
            if (currentUser == null) {
                AlertUtils.showError("Error", "User with ID=2 does not exist in the database.");
                return;
            }

            // Configurer le filtre par club
            setupClubFilter();

            // Appliquer les styles CSS aux composants
            sondagesContainer.getStyleClass().add("polls-section");
            sondagesContainer.setSpacing(20);

            // Configurer le bouton View All Polls
            viewAllPollsButton.setOnAction(e -> handleViewAllPolls());

            // Charger les sondages
            loadSondages("all");
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.showError("Initialization Error", "An error occurred: " + e.getMessage());
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
            clubsList.add(club.getNom());
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
                            .filter(s -> s.getClub().getNom().equals(clubFilter))
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

        // // User info header
        // HBox userInfoBox = new HBox(10);
        // userInfoBox.getStyleClass().add("user-info");
        // userInfoBox.setPadding(new Insets(10));

        // User avatar
        ImageView avatar = new ImageView(new Image(getClass().getResourceAsStream("/images/user.png")));
        avatar.setFitHeight(40);
        avatar.setFitWidth(40);
        avatar.setPreserveRatio(true);
        avatar.getStyleClass().add("comment-avatar");

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
            clubLabel = new Label(" • " + sondage.getClub().getNom());
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

        // Comment form header with icon
        HBox commentFormHeader = new HBox(8);
        commentFormHeader.setAlignment(Pos.CENTER_LEFT);

        // // Create pen icon for comment form
        // Label penIcon = new Label("✏️");
        // penIcon.getStyleClass().add("comment-form-icon");

        // Comment form title
        Label commentFormTitle = new Label("Write a comment");
        commentFormTitle.getStyleClass().add("comment-form-title");

        // Add icon to comment form header
        // commentFormHeader.getChildren().addAll(penIcon, commentFormTitle);

        // Comment textarea
        TextArea commentTextArea = new TextArea();
        commentTextArea.setPromptText("Share your thoughts...");
        commentTextArea.getStyleClass().add("comment-textarea");
        commentTextArea.setPrefHeight(70);
        commentTextArea.setWrapText(true);

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

        // Create send icon
        // Label sendIcon = new Label("📤");
        // sendIcon.getStyleClass().add("send-icon");

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
        postButtonWithIcon.getChildren().addAll(addCommentButton);
        addCommentButtonBox.getChildren().add(postButtonWithIcon);

        // Add all elements to the comment form
        commentForm.getChildren().addAll(commentFormHeader, commentTextArea, commentErrorLabel, addCommentButtonBox);

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
                        showCustomAlert("Success", "Your vote has been deleted successfully.", "success");

                        // Refresh the view
                        refreshData();
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showCustomAlert("Error", "Failed to delete vote: " + ex.getMessage(), "error");
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
                showCustomAlert("Warning", "Please select an option to vote.", "warning");
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
                    showCustomAlert("Success", "Your vote has been updated successfully!", "success");

                    // Refresh the view
                    refreshData();
                }
            } else {
                // Add new vote
                reponseService.addVote(currentUser.getId(), sondage.getId(), choixId);
                showCustomAlert("Success", "Your vote has been recorded successfully!", "success");

                // Refresh the view
                refreshData();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showCustomAlert("Error", "Failed to record vote: " + e.getMessage(), "error");
        }
    }

    /**
     * Add a comment to a poll
     */
    private void addComment(Sondage sondage, String content) throws SQLException {
        // Create a temporary label and validate comment
        Label tempLabel = new Label();
        if (!validateComment(content, tempLabel, null)) {
            showCustomAlert("Warning", tempLabel.getText(), "warning");
            return;
        }

        // Create and save the comment
        Commentaire commentaire = new Commentaire();
        commentaire.setSondage(sondage);
        commentaire.setUser(currentUser);
        commentaire.setContenuComment(content);
        commentaire.setDateComment(LocalDate.now());

        commentaireService.add(commentaire);

        showCustomAlert("Success", "Your comment has been added successfully!", "success");
        refreshData();
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
                showCustomAlert("Error", "You must be a club president to create polls.", "error");
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

            // Reset form
            resetPollForm();

            // Show success popup
            showCustomAlert("Success", "Your poll has been created successfully!", "success");

            // Reload sondages properly - using "all" as filter to show all polls
            filterClubComboBox.getSelectionModel().select("all");
            loadSondages("all");

        } catch (SQLException e) {
            e.printStackTrace();
            showCustomAlert("Error", "An error occurred while creating the poll: " + e.getMessage(), "error");
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
            // Utiliser un utilisateur statique avec ID=2 pour les tests
            // au lieu de l'utilisateur de la session
            // if (currentUser != null) {
            User staticUser = userService.getById(2);
            Club userClub = clubService.findByPresident(staticUser.getId());

            // Si l'utilisateur est président d'un club, on ouvre la vue PollManagement
            if (userClub != null) {
                // Charger la vue PollManagement
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/PollManagementView.fxml"));
                Parent root = loader.load();

                PollManagementController controller = loader.getController();
                controller.setPreviousScene(viewAllPollsButton.getScene());

                Scene scene = new Scene(root);
                Stage stage = (Stage) viewAllPollsButton.getScene().getWindow();
                stage.setScene(scene);

                // Maximiser la fenêtre sans mode plein écran
                stage.setMaximized(true);

            } else {
                // Si l'utilisateur n'est pas président, afficher une alerte
                showCustomAlert(
                        "Access Restricted",
                        "You must be a club president to access poll management.",
                        "warning");
            }
            // } else {
            // AlertUtils.showError("Error", "User session not found.");
            // }
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
                showCustomAlert("Info", "There are no comments to summarize for this poll.", "info");
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
                    StringBuilder summary = new StringBuilder();
                    summary.append("Summary of comments for poll: ").append(sondage.getQuestion()).append("\n\n");

                    for (int i = 0; i < comments.size(); i++) {
                        Commentaire comment = comments.get(i);
                        summary.append(i + 1).append(". ")
                                .append(comment.getUser().getLastName()).append(" ")
                                .append(comment.getUser().getFirstName()).append(": ")
                                .append(comment.getContenuComment()).append(" (")
                                .append(comment.getDateComment()).append(")\n");
                    }

                    // Add basic statistics
                    summary.append("\n--- Statistics ---\n");
                    summary.append("Total Comments: ").append(comments.size()).append("\n");

                    // Count unique users
                    long uniqueUsers = comments.stream()
                            .map(c -> c.getUser().getId())
                            .distinct()
                            .count();
                    summary.append("Unique Commenters: ").append(uniqueUsers).append("\n");

                    // Delay for UX purposes, so loading dialog is visible
                    Thread.sleep(800);

                    return summary.toString();
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
                showCustomAlert("Error", "Failed to generate summary: " + exception.getMessage(), "error");
            });

            new Thread(summaryTask).start();

        } catch (SQLException e) {
            e.printStackTrace();
            showCustomAlert("Error", "Failed to load comments: " + e.getMessage(), "error");
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
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.setResizable(true);

        // Create dialog container
        VBox dialogVBox = new VBox(15);
        dialogVBox.getStyleClass().add("summary-dialog");
        dialogVBox.setPadding(new Insets(20));

        // Header with title and close button
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setSpacing(10);

        Label titleLabel = new Label("Comments Summary");
        titleLabel.getStyleClass().add("summary-title");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button closeButton = new Button("✕");
        closeButton.getStyleClass().add("close-button");
        closeButton.setOnAction(e -> dialogStage.close());

        headerBox.getChildren().addAll(titleLabel, closeButton);

        // Summary content area
        TextArea summaryArea = new TextArea(summary);
        summaryArea.setEditable(false);
        summaryArea.setWrapText(true);
        summaryArea.setPrefRowCount(20);
        summaryArea.setPrefColumnCount(60);
        summaryArea.getStyleClass().add("summary-text");

        // Add components to dialog
        dialogVBox.getChildren().addAll(headerBox, summaryArea);

        // Background with shadow
        StackPane rootPane = new StackPane();
        rootPane.getStyleClass().add("summary-dialog-background");

        // Semi-transparent overlay
        Region overlay = new Region();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3);");
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                dialogStage.close();
            }
        });

        rootPane.getChildren().addAll(overlay, dialogVBox);

        // Create scene
        Scene dialogScene = new Scene(rootPane);
        dialogScene.setFill(Color.TRANSPARENT);
        dialogScene.getStylesheets()
                .add(getClass().getResource("/com/esprit/styles/sondage-style.css").toExternalForm());

        dialogStage.setScene(dialogScene);
        dialogStage.setWidth(700);
        dialogStage.setHeight(500);

        // Center on screen and show with animation
        dialogStage.setOnShown(e -> {
            dialogStage.setX((Screen.getPrimary().getVisualBounds().getWidth() - dialogScene.getWidth()) / 2);
            dialogStage.setY((Screen.getPrimary().getVisualBounds().getHeight() - dialogScene.getHeight()) / 2);

            // Fade in animation
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), dialogVBox);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });

        dialogStage.show();
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
            commentErrorLabel.setText("Comment is too short. Minimum 2 characters required.");
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
                            club.getNom(),
                            member.getLastName(),
                            sondage.getQuestion(),
                            optionsArray);

                    // Envoyer l'email de manière asynchrone
                    emailService.sendEmailAsync(
                            member.getEmail(),
                            "Nouveau sondage dans votre club: " + club.getNom(),
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
                showCustomAlert(
                        "Emails Sent",
                        String.format("Notification emails sent to %d club members", emailsSent),
                        "success");
            }

        } catch (SQLException e) {
            System.err.println("Error sending notification emails: " + e.getMessage());
            e.printStackTrace();
            showCustomAlert(
                    "Email Notification Error",
                    "Failed to send notification emails to club members: " + e.getMessage(),
                    "warning");
        }
    }
}

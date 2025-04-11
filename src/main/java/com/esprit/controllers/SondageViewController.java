package com.esprit.controllers;

import com.esprit.models.Commentaire;
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
import com.esprit.utils.AlertUtils;
import com.esprit.utils.SessionManager;

import javafx.animation.FadeTransition;
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
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
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
            currentUser = userService.getById(2); // Utilisateur par défaut pour les tests (ID 2)
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
        
        // User info header
        HBox userInfoBox = new HBox(10);
        userInfoBox.getStyleClass().add("user-info");
        userInfoBox.setPadding(new Insets(10));
        
        // User avatar
        ImageView avatar = new ImageView(new Image(getClass().getResourceAsStream("/com/esprit/images/avatar.png")));
        avatar.setFitHeight(40);
        avatar.setFitWidth(40);
        avatar.setPreserveRatio(true);
        
        // User name
        Label userName = new Label(sondage.getUser().getNom() + " " + sondage.getUser().getPrenom());
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
        
        // Add all elements to user info box
        userInfoBox.getChildren().addAll(avatar, userName, dateSeparator, pollDate);
        if (clubLabel != null) {
            userInfoBox.getChildren().add(clubLabel);
        }
        
        // Poll question
        Label questionLabel = new Label(sondage.getQuestion());
        questionLabel.getStyleClass().add("sondage-question");
        questionLabel.setWrapText(true);
        
        // Add to sondage box
        sondageBox.getChildren().addAll(userInfoBox, questionLabel);
        
        // Poll options with radio buttons for voting
        VBox optionsView = createPollOptionsView(sondage);
        sondageBox.getChildren().add(optionsView);
        
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
        
        // Create pen icon for comment form
        Label penIcon = new Label("✏️");
        penIcon.getStyleClass().add("comment-form-icon");
        
        // Comment form title
        Label commentFormTitle = new Label("Write a comment");
        commentFormTitle.getStyleClass().add("comment-form-title");
        
        // Add icon to comment form header
        commentFormHeader.getChildren().addAll(penIcon, commentFormTitle);
        
        // Comment textarea
        TextArea commentTextArea = new TextArea();
        commentTextArea.setPromptText("Share your thoughts...");
        commentTextArea.getStyleClass().add("comment-textarea");
        commentTextArea.setPrefHeight(70);
        commentTextArea.setWrapText(true);
        
        // Add comment button container
        HBox addCommentButtonBox = new HBox();
        addCommentButtonBox.setAlignment(Pos.CENTER_RIGHT);
        addCommentButtonBox.setPadding(new Insets(5, 0, 0, 0));
        
        // Add comment button with icon
        HBox postButtonWithIcon = new HBox(5);
        postButtonWithIcon.setAlignment(Pos.CENTER);
        
        // Create send icon
        Label sendIcon = new Label("📤");
        sendIcon.getStyleClass().add("send-icon");
        
        // Add comment button
        Button addCommentButton = new Button("Post Comment");
        addCommentButton.getStyleClass().add("post-comment-button");
        addCommentButton.setOnAction(e -> {
            try {
                String content = commentTextArea.getText().trim();
                if (!content.isEmpty()) {
                    addComment(sondage, content);
                    commentTextArea.clear();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                AlertUtils.showError("Error", "Failed to post comment: " + ex.getMessage());
            }
        });
        
        // Add icon to button
        postButtonWithIcon.getChildren().addAll(sendIcon, addCommentButton);
        addCommentButtonBox.getChildren().add(postButtonWithIcon);
        
        // Add all elements to the comment form
        commentForm.getChildren().addAll(commentFormHeader, commentTextArea, addCommentButtonBox);
        
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
        optionLabel.setPrefWidth(150);

        // Créer un spacer pour pousser la barre de progression et le pourcentage à droite
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Barre de progression
        ProgressBar progressBar = new ProgressBar(totalVotes > 0 ? (double) optionVotes / totalVotes : 0);
        progressBar.getStyleClass().add("option-progress");
        progressBar.setPrefWidth(500);

        // Choisir une classe CSS pour la couleur de la barre en fonction de l'option
        String cssClass;
        String content = option.getContenu().toLowerCase();
        if (content.contains("java")) {
            cssClass = "progress-bar-java";
        } else if (content.contains("python")) {
            cssClass = "progress-bar-python";
        } else if (content.contains("javascript") || content.contains("js")) {
            cssClass = "progress-bar-javascript";
        } else if (content.contains("c#") || content.contains("csharp")) {
            cssClass = "progress-bar-csharp";
        } else if (content.contains("react")) {
            cssClass = "progress-bar-react";
        } else if (content.contains("angular")) {
            cssClass = "progress-bar-angular";
        } else if (content.contains("vue")) {
            cssClass = "progress-bar-vue";
        } else {
            // Choisir une couleur aléatoire parmi les disponibles
            String[] classes = { "progress-bar-java", "progress-bar-python", "progress-bar-javascript",
                    "progress-bar-csharp", "progress-bar-react", "progress-bar-angular", "progress-bar-vue" };
            cssClass = classes[option.getId() % classes.length];
        }
        progressBar.getStyleClass().add(cssClass);

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
        
        // Add user's current choice display if they've voted
        if (userChoice != null) {
            HBox userChoiceBox = new HBox(10);
            userChoiceBox.getStyleClass().add("user-choice-box");
            userChoiceBox.setAlignment(Pos.CENTER_LEFT);
            
            Label yourChoiceLabel = new Label("Your choice:");
            yourChoiceLabel.getStyleClass().add("your-choice-label");
            
            Label userChoiceLabel = new Label(userChoice.getContenu());
            userChoiceLabel.getStyleClass().add("user-choice");
            
            userChoiceBox.getChildren().addAll(yourChoiceLabel, userChoiceLabel);
            optionsContainer.getChildren().add(userChoiceBox);
        }
        
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
            optionProgress.setPrefWidth(150);
            optionProgress.getStyleClass().addAll("option-progress", "progress-bar-color-" + (i % 8));
            
            // Percentage label
            Label percentageLabel = new Label(String.format("%.1f%%", percentage));
            percentageLabel.getStyleClass().add("percentage-label");
            
            optionRow.getChildren().addAll(optionRadio, spacer, optionProgress, percentageLabel);
            optionsContainer.getChildren().add(optionRow);
        }
        
        // Add voting buttons
        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER_RIGHT);
        buttonsBox.setPadding(new Insets(10, 0, 0, 0));
        
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
        
        // Add delete vote button if user has already voted
        if (userChoice != null) {
            Button deleteVoteButton = new Button("Delete Vote");
            deleteVoteButton.getStyleClass().add("delete-vote-button");
            deleteVoteButton.setOnAction(e -> {
                try {
                    // Delete user's vote
                    reponseService.deleteUserVote(currentUser.getId(), sondage.getId());
                    
                    // Show confirmation
                    AlertUtils.showConfirmation("Vote", "Your vote has been deleted.");
                    
                    // Refresh the view
                    refreshData();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    AlertUtils.showError("Error", "Failed to delete vote: " + ex.getMessage());
                }
            });
            
            buttonsBox.getChildren().addAll(voteButton, deleteVoteButton);
        } else {
            buttonsBox.getChildren().add(voteButton);
        }
        
        optionsContainer.getChildren().add(buttonsBox);
        
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
                AlertUtils.showWarning("Voting", "Please select an option to vote.");
                return;
            }
            
            // Get the choice ID from the selected radio button's user data
            int choixId = (int) selectedOption.getUserData();
            
            // Check if user already voted
            boolean hasVoted = reponseService.hasUserVoted(currentUser.getId(), sondage.getId());
            
            if (hasVoted) {
                // Update existing vote
                reponseService.updateUserVote(currentUser.getId(), sondage.getId(), choixId);
                AlertUtils.showConfirmation("Vote", "Your vote has been updated!");
            } else {
                // Add new vote
                reponseService.addVote(currentUser.getId(), sondage.getId(), choixId);
                AlertUtils.showConfirmation("Vote", "Your vote has been recorded!");
            }
            
            // Refresh the view
            refreshData();
            
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.showError("Error", "Failed to record vote: " + e.getMessage());
        }
    }

    @FXML
    private void addComment() throws SQLException {
        if (currentSondage == null) {
            AlertUtils.showWarning("Comment", "Please select a poll before adding a comment.");
            return;
        }

        String commentText = commentTextArea.getText().trim();
        if (commentText.isEmpty()) {
            AlertUtils.showWarning("Comment", "Comment cannot be empty.");
            return;
        }

        // Créer et enregistrer le commentaire
        Commentaire commentaire = new Commentaire();
        commentaire.setSondage(currentSondage);
        commentaire.setUser(currentUser);
        commentaire.setContenuComment(commentText);
        commentaire.setDateComment(LocalDate.now());

        commentaireService.add(commentaire);

        AlertUtils.showConfirmation("Comment", "Your comment has been added successfully!");
        commentTextArea.clear();
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
            
            controller.setSondage(sondage);
            controller.setParentController(this);
            controller.setCurrentUser(currentUser);

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

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error details: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Cause: " + e.getCause().getMessage());
            }
            AlertUtils.showError("Error", "Unable to open comments window: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("Error", "Unexpected error: " + e.getMessage());
        }
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

    @FXML
    private void handleCreatePoll() {
        try {
            // Validation des entrées
            String question = pollQuestionField.getText().trim();
            if (question.isEmpty()) {
                AlertUtils.showError("Error", "Please enter a question for the poll.");
                return;
            }

            // Récupérer les options
            List<String> options = new ArrayList<>();
            for (int i = 0; i < pollOptionsContainer.getChildren().size(); i++) {
                if (pollOptionsContainer.getChildren().get(i) instanceof TextField) {
                    TextField optionField = (TextField) pollOptionsContainer.getChildren().get(i);
                    String optionText = optionField.getText().trim();
                    if (!optionText.isEmpty()) {
                        options.add(optionText);
                    }
                }
            }

            if (options.size() < 2) {
                AlertUtils.showError("Error", "Please provide at least 2 options for the poll.");
                return;
            }

            // Trouver le club de l'utilisateur
            Club userClub = clubService.findByPresident(currentUser.getId());
            if (userClub == null) {
                // Pour les tests, si l'utilisateur n'est pas président d'un club,
                // on prend le premier club disponible
                List<Club> clubs = clubService.getAll();
                if (clubs.isEmpty()) {
                    AlertUtils.showError("Error", "No club available. You must be associated with a club to create a poll.");
                    return;
                }
                userClub = clubs.get(0);
            }

            // Créer le sondage
            Sondage sondage = new Sondage();
            sondage.setQuestion(question);
            sondage.setUser(currentUser);
            sondage.setClub(userClub);
            sondage.setCreatedAt(LocalDateTime.now());

            // Ajouter les options au sondage
            for (String optionText : options) {
                ChoixSondage choix = new ChoixSondage();
                choix.setContenu(optionText);
                choix.setSondage(sondage);
                sondage.addChoix(choix);
            }

            // Enregistrer le sondage
            sondageService.add(sondage);

            // Réinitialiser le formulaire
            resetPollForm();

            // Recharger les sondages pour afficher le nouveau
            loadSondages(filterClubComboBox.getValue());

            AlertUtils.showSuccess("Success", "Poll created successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.showError("Error", "Failed to create poll: " + e.getMessage());
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
     * Gère le clic sur le bouton "View All Polls" pour ouvrir la vue de gestion des sondages
     * et afficher les sondages du club dont l'utilisateur courant est président
     */
    private void handleViewAllPolls() {
        try {
            // Trouver le club dont l'utilisateur est président
            Club userClub = clubService.findByPresident(currentUser.getId());
            if (userClub == null) {
                AlertUtils.showInfo("Information", "You are not the president of any club.");
                return;
            }
            
            // Charger la vue PollManagementView
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/PollManagementView.fxml"));
            Parent root = loader.load();
            
            // Récupérer le contrôleur
            PollManagementController controller = loader.getController();
            
            // Sauvegarder la scène actuelle pour pouvoir revenir
            Scene currentScene = viewAllPollsButton.getScene();
            controller.setPreviousScene(currentScene);
            
            // Définir le filtre pour afficher tous les sondages, puis naviguer vers la scène
            // On montrera tous les sondages plutôt que de filtrer par club pour éviter l'erreur
            Scene scene = new Scene(root);
            Stage stage = (Stage) viewAllPollsButton.getScene().getWindow();
            stage.setScene(scene);
            
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtils.showError("Error", "Error loading Poll Management view: " + e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.showError("Error", "Database error: " + e.getMessage());
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
    
    /**
     * Add a comment to a poll
     */
    private void addComment(Sondage sondage, String content) throws SQLException {
        if (content.isEmpty()) {
            AlertUtils.showWarning("Comment", "Comment cannot be empty.");
            return;
        }

        // Create and save the comment
        Commentaire commentaire = new Commentaire();
        commentaire.setSondage(sondage);
        commentaire.setUser(currentUser);
        commentaire.setContenuComment(content);
        commentaire.setDateComment(LocalDate.now());

        commentaireService.add(commentaire);

        AlertUtils.showInfo("Comment", "Your comment has been added successfully!");
        refreshData();
    }
}
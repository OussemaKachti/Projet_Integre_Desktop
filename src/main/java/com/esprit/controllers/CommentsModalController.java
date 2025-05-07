package com.esprit.controllers;

import com.esprit.models.Commentaire;
import com.esprit.models.Sondage;
import com.esprit.models.User;
import com.esprit.services.CommentaireService;
import com.esprit.utils.AlertUtils;
import com.esprit.utils.SessionManager;
import com.esprit.utils.TranslationService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import java.util.Arrays;
import java.io.File;

public class CommentsModalController implements Initializable {

    @FXML
    private Label modalTitle;
    @FXML
    private VBox commentsListContainer;
    @FXML
    private Button closeButton;
    @FXML
    private Button closeModalButton;

    private Sondage sondage;
    private User currentUser;
    private SondageViewController parentController;
    private final CommentaireService commentaireService = new CommentaireService();
    private final ObservableList<Commentaire> commentsList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Récupérer l'utilisateur connecté
        currentUser = SessionManager.getInstance().getCurrentUser(); //////////// AAAAAAAAAAAAAAA TESSSSSSSSTIIIIIIII
                                                                     //////////// /////////////////////////////
        if (currentUser == null) {
            // Utilisateur par défaut pour les tests
            currentUser = new User();
            currentUser.setId(2);
        }

        // Configure the close buttons safely
        if (closeButton != null) {
            closeButton.setOnAction(e -> closeModal());
        }

        if (closeModalButton != null) {
            closeModalButton.setOnAction(e -> closeModal());
        }

        // Set default title
        if (modalTitle != null) {
            modalTitle.setText("Commentaires");
        }

        // Style the comments container if available
        if (commentsListContainer != null) {
            commentsListContainer.setSpacing(12);
        }

        // Comments will be loaded after sondage is set
        // Don't try to load comments here as sondage is null
    }

    /**
     * This method is called after the modal is initialized and the sondage is set
     * It adds the comment section and loads the comments
     */
    public void setupModalContent() {
        if (commentsListContainer == null || sondage == null) {
            return;
        }

        try {
            // Set the title with the poll question
            if (modalTitle != null && sondage != null) {
                modalTitle.setText("Comments - " + sondage.getQuestion());
            }

            // Create the add comment section
            VBox addCommentSection = createAddCommentSection();

            // Get the root container
            Node scrollPane = commentsListContainer.getParent();
            if (scrollPane != null && scrollPane.getParent() != null) {
                VBox root = (VBox) scrollPane.getParent();

                // Add the comment section before the last element (close button)
                if (root.getChildren().size() > 1) {
                    root.getChildren().add(root.getChildren().size() - 1, addCommentSection);
                }
            }

            // Load comments
            loadComments();

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("Error", "Error initializing comments modal: " + e.getMessage());
        }
    }

    public void setSondage(Sondage sondage) {
        this.sondage = sondage;
        if (modalTitle != null) {
            modalTitle.setText("Comments - " + sondage.getQuestion());
        }
        loadComments();
    }

    public void setParentController(SondageViewController controller) {
        this.parentController = controller;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    @FXML
    public void loadComments() {
        if (commentsListContainer == null || sondage == null) {
            return; // Si les composants ne sont pas encore initialisés
        }

        commentsListContainer.getChildren().clear();

        try {
            // Récupérer les commentaires pour ce sondage
            ObservableList<Commentaire> comments = commentaireService.getBySondage(sondage.getId());

            if (comments.isEmpty()) {
                Label noCommentsLabel = new Label("No comments for this poll.");
                noCommentsLabel.getStyleClass().add("no-comments-label");
                commentsListContainer.getChildren().add(noCommentsLabel);
            } else {
                // Créer un élément d'interface pour chaque commentaire
                for (Commentaire comment : comments) {
                    VBox commentBox = createCommentBox(comment);
                    commentsListContainer.getChildren().add(commentBox);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.showError("Error", "Unable to load comments: " + e.getMessage());
        }
    }

    /**
     * Creates a comment box for a comment
     * 
     * @param comment The comment to create a box for
     * @return A VBox containing the comment UI
     */
    private VBox createCommentBox(Commentaire comment) {
        VBox commentBox = new VBox(5);
        commentBox.getStyleClass().add("comment-box");
        commentBox.setPadding(new Insets(10));

        // Comment header with user info
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        // User avatar
        ImageView avatar = createUserAvatar(comment.getUser());

        // User info
        VBox userInfoBox = new VBox();
        Label userName = new Label(comment.getUser().getFullName());
        userName.getStyleClass().add("comment-user");

        // Format date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        Label commentDate = new Label(comment.getDateComment().format(formatter));
        commentDate.getStyleClass().add("comment-date");

        userInfoBox.getChildren().addAll(userName, commentDate);
        headerBox.getChildren().addAll(avatar, userInfoBox);

        // Comment text
        Label commentText = new Label(comment.getContenuComment());
        commentText.getStyleClass().add("comment-text");
        commentText.setWrapText(true);
        commentText.setPadding(new Insets(5, 0, 5, 50));

        // Translation container (initially hidden)
        VBox translationContainer = new VBox(5);
        translationContainer.getStyleClass().add("translation-container");
        translationContainer.setVisible(false);
        translationContainer.setManaged(false);

        Label translatedContent = new Label();
        translatedContent.getStyleClass().add("translated-text");
        translatedContent.setWrapText(true);

        Label originalLanguageLabel = new Label();
        originalLanguageLabel.getStyleClass().add("language-label");

        Label translatedLanguageLabel = new Label();
        translatedLanguageLabel.getStyleClass().add("language-label");

        HBox languageLabels = new HBox(10);
        languageLabels.getChildren().addAll(originalLanguageLabel, translatedLanguageLabel);

        translationContainer.getChildren().addAll(languageLabels, translatedContent);

        // Check if comment is flagged as toxic
        boolean isToxic = comment.getContenuComment().startsWith("⚠️ Comment hidden");

        // Comment actions (only show for non-toxic comments and if the user is the
        // author)
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        actionBox.setPadding(new Insets(5, 0, 0, 50));

        // Add translate button
        Button translateButton = new Button("Translate");
        translateButton.getStyleClass().add("translate-button");
        translateButton.setOnAction(e -> {
            if (translationContainer.isVisible()) {
                translationContainer.setVisible(false);
                translationContainer.setManaged(false);
                translateButton.setText("Translate");
            } else {
                translateButton.setText("Hide Translation");
                translationContainer.setVisible(true);
                translationContainer.setManaged(true);

                // Get translation service instance
                TranslationService translationService = TranslationService.getInstance();

                // Call translation service
                translationService.detectAndTranslate(
                        comment.getContenuComment(),
                        translatedContent,
                        originalLanguageLabel,
                        translatedLanguageLabel);
            }
        });

        actionBox.getChildren().add(translateButton);

        if (!isToxic && currentUser != null && comment.getUser().getId() == currentUser.getId()) {
            // Edit components
            TextArea editTextArea = new TextArea(comment.getContenuComment());
            editTextArea.getStyleClass().add("edit-comment-textarea");
            editTextArea.setVisible(false);
            editTextArea.setWrapText(true);
            editTextArea.setPrefRowCount(3);

            // Edit button
            Button editButton = new Button("Edit");
            editButton.getStyleClass().add("edit-button");

            // Update button (initially hidden)
            Button updateButton = new Button("Update");
            updateButton.getStyleClass().add("update-button");
            updateButton.setVisible(false);

            // Delete button
            Button deleteButton = new Button("Delete");
            deleteButton.getStyleClass().add("delete-button");

            // Add event handlers
            editButton.setOnAction(e -> {
                commentText.setVisible(false);
                editTextArea.setVisible(true);
                errorLabel.setVisible(true);
                editButton.setVisible(false);
                updateButton.setVisible(true);
                validateEditComment(editTextArea.getText(), errorLabel, updateButton);
            });

            updateButton.setOnAction(e -> {
                try {
                    // Update comment in database
                    String newContent = editTextArea.getText();
                    if (newContent != null && !newContent.trim().isEmpty()) {
                        comment.setContenuComment(newContent);
                        commentaireService.update(comment);

                        // Update UI
                        commentText.setText(newContent);
                        commentText.setVisible(true);
                        editTextArea.setVisible(false);
                        editButton.setVisible(true);
                        updateButton.setVisible(false);

                        // Reset translation if visible
                        translationContainer.setVisible(false);
                        translationContainer.setManaged(false);
                        translateButton.setText("Translate");

                        // Refresh parent view
                        if (parentController != null) {
                            parentController.refreshData();
                        }
                    }
                } catch (SQLException ex) {
                    showCustomAlert("Error", "Failed to update comment: " + ex.getMessage(), "error");
                }
            });

            deleteButton.setOnAction(e -> {
                boolean confirmed = showConfirmDialog("Delete Comment",
                        "Are you sure you want to delete this comment?",
                        "This action cannot be undone.");

                if (confirmed) {
                    try {
                        commentaireService.delete(comment.getId());
                        // Remove comment box from UI
                        commentsListContainer.getChildren().remove(commentBox);
                        // Refresh parent view
                        if (parentController != null) {
                            parentController.refreshData();
                        }
                    } catch (SQLException ex) {
                        showCustomAlert("Error", "Failed to delete comment: " + ex.getMessage(), "error");
                    }
                }
            });

            actionBox.getChildren().addAll(editButton, deleteButton);
            commentBox.getChildren().addAll(headerBox, commentText, editTextArea, translationContainer, actionBox);
        } else {
            // For toxic comments or comments by other users, just show the content without
            // edit/delete actions
            if (isToxic) {
                // Add a moderation badge
                Label moderationLabel = new Label("Moderated Content");
                moderationLabel.setStyle("-fx-text-fill: #F44336; -fx-font-style: italic; -fx-font-size: 12px;");
                actionBox.getChildren().add(moderationLabel);
            }
            commentBox.getChildren().addAll(headerBox, commentText, translationContainer, actionBox);
        }

        return commentBox;
    }

    /**
     * Shows a custom styled alert
     */
    private void showCustomAlert(String title, String message, String type) {
        Stage alertStage = new Stage();
        alertStage.initModality(Modality.APPLICATION_MODAL);
        alertStage.setResizable(false);

        VBox alertContainer = new VBox(15);
        alertContainer.getStyleClass().add("custom-alert");
        alertContainer.getStyleClass().add("custom-alert-" + type);
        alertContainer.setPadding(new Insets(20));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("title");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("content");
        messageLabel.setWrapText(true);

        Button okButton = new Button("OK");
        okButton.getStyleClass().addAll("custom-alert-button", "custom-alert-button-ok");
        okButton.setOnAction(e -> alertStage.close());

        HBox buttonBox = new HBox();
        buttonBox.getStyleClass().add("buttons-box");
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().add(okButton);

        alertContainer.getChildren().addAll(titleLabel, messageLabel, buttonBox);

        Scene scene = new Scene(alertContainer);
        scene.getStylesheets().add(getClass().getResource("/com/esprit/styles/sondage-style.css").toExternalForm());

        alertStage.setScene(scene);
        alertStage.showAndWait();
    }

    /**
     * Shows a custom confirmation dialog
     */
    private boolean showCustomConfirmDialog(String title, String message, String details) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setResizable(false);

        VBox dialogContainer = new VBox(15);
        dialogContainer.getStyleClass().add("custom-alert");
        dialogContainer.setPadding(new Insets(25));

        // Header with icon and title
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("⚠");
        iconLabel.getStyleClass().add("custom-alert-confirm-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("title");

        headerBox.getChildren().addAll(iconLabel, titleLabel);

        // Message box
        VBox messageBox = new VBox(5);

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("content");
        messageLabel.setWrapText(true);

        Label detailsLabel = new Label(details);
        detailsLabel.getStyleClass().add("details-content");
        detailsLabel.setWrapText(true);

        messageBox.getChildren().addAll(messageLabel, detailsLabel);

        // Buttons
        boolean[] result = { false };

        Button confirmButton = new Button("Confirm");
        confirmButton.getStyleClass().addAll("custom-alert-button", "custom-alert-button-ok");
        confirmButton.setOnAction(e -> {
            result[0] = true;
            dialogStage.close();
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().addAll("custom-alert-button", "custom-alert-button-cancel");
        cancelButton.setOnAction(e -> {
            result[0] = false;
            dialogStage.close();
        });

        HBox buttonBox = new HBox(10);
        buttonBox.getStyleClass().add("buttons-box");
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().addAll(cancelButton, confirmButton);

        dialogContainer.getChildren().addAll(headerBox, messageBox, buttonBox);

        Scene scene = new Scene(dialogContainer);
        scene.getStylesheets().add(getClass().getResource("/com/esprit/styles/sondage-style.css").toExternalForm());

        dialogStage.setScene(scene);
        dialogStage.showAndWait();

        return result[0];
    }

    @FXML
    private void closeModal() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Create add comment section with validation
     */
    public VBox createAddCommentSection() {
        VBox addCommentContainer = new VBox(10);
        addCommentContainer.getStyleClass().add("add-comment-container");
        addCommentContainer.setPadding(new Insets(15));

        Label addCommentTitle = new Label("Add Your Comment");
        addCommentTitle.getStyleClass().add("add-comment-title");

        TextArea commentTextArea = new TextArea();
        commentTextArea.getStyleClass().add("add-comment-text-area");
        commentTextArea.setPromptText("Write your comment here...");
        commentTextArea.setPrefRowCount(3);

        Button addButton = new Button("Post Comment");
        addButton.getStyleClass().add("add-comment-button");

        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().add(addButton);

        addCommentContainer.getChildren().addAll(addCommentTitle, commentTextArea, buttonBox);

        // Set the action for the add button
        addButton.setOnAction(e -> {
            try {
                String content = commentTextArea.getText();

                // Validate comment content
                if (content == null || content.trim().isEmpty()) {
                    showCustomAlert("Warning", "Comment cannot be empty.", "warning");
                    return;
                }

                // Create new comment
                Commentaire comment = new Commentaire();
                comment.setContenuComment(content);
                comment.setDateComment(LocalDate.now());
                comment.setUser(currentUser);
                comment.setSondage(sondage);

                // Save to database
                commentaireService.add(comment);

                // Clear the text area
                commentTextArea.clear();

                // Show success message
                showCustomAlert("Success", "Comment added successfully!", "success");

                // Refresh comments
                loadComments();

                // Safely refresh parent view if available
                if (parentController != null) {
                    try {
                        parentController.refreshData();
                    } catch (Exception ex) {
                        // Log the error but don't show it to the user since the comment was
                        // successfully added
                        System.err.println("Error refreshing parent controller: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                showCustomAlert("Error", "Failed to add comment: " + ex.getMessage(), "error");
            }
        });

        return addCommentContainer;
    }

    /**
     * Creates a user avatar for display in comments
     * 
     * @param user The user to create an avatar for
     * @return An ImageView containing the user's avatar
     */
    private ImageView createUserAvatar(User user) {
        ImageView avatar = new ImageView();
        avatar.setFitHeight(40);
        avatar.setFitWidth(40);
        avatar.setPreserveRatio(true);
        avatar.getStyleClass().add("comment-avatar");

        // Load user profile image with fallback to default
        try {
            String profilePicPath = user.getProfilePicture();
            if (profilePicPath != null && !profilePicPath.isEmpty()) {
                File imageFile = new File("uploads/profiles/" + profilePicPath);
                if (imageFile.exists()) {
                    avatar.setImage(new Image(imageFile.toURI().toString()));
                } else {
                    // Fall back to default if file doesn't exist
                    avatar.setImage(new Image(getClass().getResourceAsStream("/com/esprit/images/user.png")));
                }
            } else {
                // Fall back to default if no profile pic
                avatar.setImage(new Image(getClass().getResourceAsStream("/com/esprit/images/user.png")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fall back to default on any error
            avatar.setImage(new Image(getClass().getResourceAsStream("/com/esprit/images/user.png")));
        }

        // Add drop shadow effect to avatar
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(3.0);
        dropShadow.setOffsetX(1.0);
        dropShadow.setOffsetY(1.0);
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.3));
        avatar.setEffect(dropShadow);

        // Make avatar circular
        double radius = avatar.getFitWidth() / 2;
        avatar.setClip(new javafx.scene.shape.Circle(radius, radius, radius));

        return avatar;
    }

    /**
     * Shows a confirmation dialog
     * 
     * @param title   The title of the dialog
     * @param message The main message
     * @param details Additional details
     * @return true if confirmed, false otherwise
     */
    private boolean showConfirmDialog(String title, String message, String details) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setResizable(false);
        dialogStage.setTitle(title);

        VBox dialogContainer = new VBox(15);
        dialogContainer.getStyleClass().add("custom-alert");
        dialogContainer.setPadding(new Insets(25));

        // Header with icon and title
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("⚠");
        iconLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #FF8C00;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        headerBox.getChildren().addAll(iconLabel, titleLabel);

        // Message box
        VBox messageBox = new VBox(10);

        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-font-size: 14px;");
        messageLabel.setWrapText(true);

        Label detailsLabel = new Label(details);
        detailsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #757575;");
        detailsLabel.setWrapText(true);

        messageBox.getChildren().addAll(messageLabel, detailsLabel);

        // Buttons
        boolean[] result = { false };

        Button confirmButton = new Button("Delete");
        confirmButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold;");
        confirmButton.setOnAction(e -> {
            result[0] = true;
            dialogStage.close();
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: #E0E0E0;");
        cancelButton.setOnAction(e -> {
            result[0] = false;
            dialogStage.close();
        });

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().addAll(cancelButton, confirmButton);

        dialogContainer.getChildren().addAll(headerBox, messageBox, buttonBox);

        Scene scene = new Scene(dialogContainer);
        if (getClass().getResource("/com/esprit/styles/sondage-style.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/com/esprit/styles/sondage-style.css").toExternalForm());
        }

        dialogStage.setScene(scene);
        dialogStage.showAndWait();

        return result[0];
    }
}
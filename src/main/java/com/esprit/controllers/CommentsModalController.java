package com.esprit.controllers;

import com.esprit.models.Commentaire;
import com.esprit.models.Sondage;
import com.esprit.models.User;
import com.esprit.services.CommentaireService;
import com.esprit.utils.AlertUtils;
import com.esprit.utils.SessionManager;

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
        currentUser = SessionManager.getInstance().getCurrentUser();        ////////////  AAAAAAAAAAAAAAA TESSSSSSSSTIIIIIIII /////////////////////////////
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

    private VBox createCommentBox(Commentaire comment) {
        // Main comment container
        VBox commentBox = new VBox();
        commentBox.getStyleClass().add("comment-box");
        commentBox.setPadding(new Insets(10));

        // Header with user info
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        // User avatar
        ImageView avatar = new ImageView();
        avatar.setFitHeight(40);
        avatar.setFitWidth(40);
        avatar.setPreserveRatio(true);
        avatar.getStyleClass().add("comment-avatar");
        
        // Load user profile image with fallback to default
        try {
            String profilePicPath = comment.getUser().getProfilePicture();
            if (profilePicPath != null && !profilePicPath.isEmpty()) {
                File imageFile = new File("uploads/profiles/" + profilePicPath);
                if (imageFile.exists()) {
                    avatar.setImage(new Image(imageFile.toURI().toString()));
                } else {
                    // Fall back to default if file doesn't exist
                    avatar.setImage(new Image(getClass().getResourceAsStream("/images/user.png")));
                }
            } else {
                // Fall back to default if no profile pic
                avatar.setImage(new Image(getClass().getResourceAsStream("/images/user.png")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fall back to default on any error
            avatar.setImage(new Image(getClass().getResourceAsStream("/images/user.png")));
        }
        
        // Add drop shadow effect to avatar
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(3.0);
        dropShadow.setOffsetX(1.0);
        dropShadow.setOffsetY(1.0);
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.3));
        avatar.setEffect(dropShadow);

        // User info container
        VBox userInfo = new VBox();

        // Username
        Label username = new Label(comment.getUser().getFirstName() + " " + comment.getUser().getLastName());
        username.getStyleClass().add("comment-user");

        // Comment date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        Label dateLabel = new Label(comment.getDateComment().format(formatter));
        dateLabel.getStyleClass().add("comment-date");

        userInfo.getChildren().addAll(username, dateLabel);
        headerBox.getChildren().addAll(avatar, userInfo);

        // Comment text
        Label commentText = new Label(comment.getContenuComment());
        commentText.getStyleClass().add("comment-text");
        commentText.setWrapText(true);
        VBox.setMargin(commentText, new Insets(5, 0, 5, 50));

        // Comment actions (Edit, Delete)
        HBox actionsBox = new HBox(10);
        actionsBox.getStyleClass().add("comment-actions");
        actionsBox.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(actionsBox, new Insets(5, 0, 0, 50));

        // Error message label (initially hidden)
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("validation-error");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        VBox.setMargin(errorLabel, new Insets(0, 0, 5, 50));

        // Edit button
        Button editButton = new Button("Edit");
        editButton.getStyleClass().add("edit-button");

        // Update button (initially hidden)
        Button updateButton = new Button("Update");
        updateButton.getStyleClass().add("update-button");
        updateButton.setVisible(false);
        updateButton.setDisable(true); // Initially disabled until valid input

        // Delete button
        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("delete-button");

        // Edit textarea (initially hidden)
        TextArea editTextArea = new TextArea(comment.getContenuComment());
        editTextArea.getStyleClass().add("edit-comment-textarea");
        editTextArea.setVisible(false);
        editTextArea.setPrefHeight(60);
        editTextArea.setPrefWidth(500);
        editTextArea.setPromptText("Edit your comment...");

        // Add real-time validation
        editTextArea.textProperty().addListener((obs, oldVal, newVal) -> {
            validateEditComment(newVal, errorLabel, updateButton);
        });

        // Check if current user is the comment author
        boolean isAuthor = currentUser != null && comment.getUser() != null &&
                comment.getUser().getId() == currentUser.getId();

        if (isAuthor) {
            // Edit button action
            editButton.setOnAction(e -> {
                commentText.setVisible(false);
                editTextArea.setVisible(true);
                errorLabel.setVisible(true);
                editButton.setVisible(false);
                updateButton.setVisible(true);
                validateEditComment(editTextArea.getText(), errorLabel, updateButton);
            });

            // Update button action
            updateButton.setOnAction(e -> {
                String newContent = editTextArea.getText();
                if (validateEditComment(newContent, errorLabel, updateButton)) {
                    updateComment(comment, newContent);
                }
            });

            // Delete button action
            deleteButton.setOnAction(e -> {
                deleteComment(comment);
            });

            actionsBox.getChildren().addAll(editTextArea, editButton, updateButton, deleteButton);
        }

        commentBox.getChildren().addAll(headerBox, commentText, errorLabel);

        // Only add the action box if the user is the author
        if (isAuthor) {
            commentBox.getChildren().add(actionsBox);
        }

        return commentBox;
    }

    /**
     * Validates the comment content and updates UI accordingly
     * 
     * @return true if the comment is valid, false otherwise
     */
    private boolean validateEditComment(String content, Label errorLabel, Button updateButton) {
        if (content == null || content.trim().isEmpty()) {
            errorLabel.setText("Le commentaire ne peut pas être vide");
            errorLabel.setVisible(true);
            updateButton.setDisable(true);
            return false;
        }

        if (content.trim().length() < 2) {
            errorLabel.setText("Le commentaire doit contenir au moins 2 caractères");
            errorLabel.setVisible(true);
            updateButton.setDisable(true);
            return false;
        }

        if (content.trim().length() > 20) {
            errorLabel.setText("Le commentaire ne peut pas dépasser 20 caractères");
            errorLabel.setVisible(true);
            updateButton.setDisable(true);
            return false;
        }

        // Check for inappropriate words
        List<String> inappropriateWords = Arrays.asList("insulte", "grossier", "offensive", "vulgar", "idiot",
                "stupid");
        String contentLower = content.toLowerCase();
        for (String word : inappropriateWords) {
            if (contentLower.contains(word)) {
                errorLabel.setText("Le commentaire contient des mots inappropriés");
                errorLabel.setVisible(true);
                updateButton.setDisable(true);
                return false;
            }
        }

        // If all validations pass
        errorLabel.setVisible(false);
        updateButton.setDisable(false);
        return true;
    }

    /**
     * Updates a comment after validation
     */
    private void updateComment(Commentaire comment, String newContent) {
        if (newContent == null || newContent.trim().isEmpty()) {
            showCustomAlert("Warning", "Comment cannot be empty.", "warning");
            return;
        }

        try {
            comment.setContenuComment(newContent);
            comment.setDateComment(LocalDate.now());
            commentaireService.update(comment);

            // Show success message
            showCustomAlert("Success", "Comment updated successfully!", "success");

            // Refresh comments
            loadComments();

            // Refresh parent view if available
            if (parentController != null) {
                parentController.refreshData();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showCustomAlert("Error", "Failed to update comment: " + e.getMessage(), "error");
        }
    }

    /**
     * Deletes a comment after confirmation
     */
    private void deleteComment(Commentaire comment) {
        if (showCustomConfirmDialog("Confirm Deletion",
                "Are you sure you want to delete this comment?",
                "This action cannot be undone.")) {
            try {
                commentaireService.delete(comment.getId(), currentUser.getId());

                // Show success message
                showCustomAlert("Success", "Comment deleted successfully!", "success");

                // Refresh comments
                loadComments();

                // Refresh parent view if available
                if (parentController != null) {
                    parentController.refreshData();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showCustomAlert("Error", "Failed to delete comment: " + e.getMessage(), "error");
            }
        }
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

                // Refresh parent view if available
                if (parentController != null) {
                    parentController.refreshData();
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                showCustomAlert("Error", "Failed to add comment: " + ex.getMessage(), "error");
            }
        });

        return addCommentContainer;
    }
}
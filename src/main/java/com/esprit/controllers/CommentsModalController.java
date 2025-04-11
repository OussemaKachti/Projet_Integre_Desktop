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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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

public class CommentsModalController implements Initializable {

    @FXML private Label modalTitle;
    @FXML private VBox commentsListContainer;
    @FXML private Button closeButton;
    @FXML private Button closeModalButton;
    
    private Sondage sondage;
    private User currentUser;
    private SondageViewController parentController;
    private final CommentaireService commentaireService = new CommentaireService();
    private final ObservableList<Commentaire> commentsList = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Récupérer l'utilisateur connecté
        currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            // Utilisateur par défaut pour les tests
            currentUser = new User();
            currentUser.setId(2);
        }
        
        // Configurer le bouton de fermeture
        if (closeButton != null) {
            closeButton.setOnAction(e -> closeModal());
        }
        
        // Configurer le titre modal
        if (modalTitle != null && sondage != null) {
            modalTitle.setText("Comments - " + sondage.getQuestion());
        }
        
        // Set modal title and configure close button action
        modalTitle.setText("Commentaires");
        closeButton.setOnAction(e -> ((Stage) closeButton.getScene().getWindow()).close());
        closeModalButton.setOnAction(e -> ((Stage) closeModalButton.getScene().getWindow()).close());
        
        // Style the modal
        commentsListContainer.setSpacing(12);
        
        // Load comments
        loadComments();
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
        ImageView avatar = new ImageView(new Image(getClass().getResourceAsStream("/com/esprit/images/avatar.png")));
        avatar.setFitHeight(40);
        avatar.setFitWidth(40);
        avatar.setPreserveRatio(true);
        avatar.getStyleClass().add("comment-avatar");
        
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
        Label username = new Label(comment.getUser().getNom() + " " + comment.getUser().getPrenom());
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
        
        // Edit textarea (initially hidden)
        TextArea editTextArea = new TextArea(comment.getContenuComment());
        editTextArea.getStyleClass().add("edit-comment-textarea");
        editTextArea.setVisible(false);
        editTextArea.setPrefHeight(60);
        editTextArea.setPrefWidth(500);
        editTextArea.setPromptText("Modifier votre commentaire...");
        
        // Edit button
        Button editButton = new Button("Modifier");
        editButton.getStyleClass().add("edit-button");
        
        // Update button (initially hidden)
        Button updateButton = new Button("Mettre à jour");
        updateButton.getStyleClass().add("update-button");
        updateButton.setVisible(false);
        
        // Delete button
        Button deleteButton = new Button("Supprimer");
        deleteButton.getStyleClass().add("delete-button");
        
        // Check if current user is the comment author
        boolean isAuthor = comment.getUser().getId() == 3; // Hardcoded user ID 3 for testing
        
        if (isAuthor) {
            // Edit button action
            editButton.setOnAction(e -> {
                commentText.setVisible(false);
                editTextArea.setVisible(true);
                editButton.setVisible(false);
                updateButton.setVisible(true);
            });
            
            // Update button action
            updateButton.setOnAction(e -> {
                updateComment(comment, editTextArea.getText(), commentText);
                commentText.setVisible(true);
                editTextArea.setVisible(false);
                editButton.setVisible(true);
                updateButton.setVisible(false);
            });
            
            // Delete button action
            deleteButton.setOnAction(e -> deleteComment(comment, commentBox));
            
            actionsBox.getChildren().addAll(editTextArea, editButton, updateButton, deleteButton);
        } else {
            // If not the author, don't show edit/delete buttons
            editButton.setVisible(false);
            deleteButton.setVisible(false);
        }
        
        commentBox.getChildren().addAll(headerBox, commentText, actionsBox);
        
        // Add hover effect
        commentBox.setOnMouseEntered(e -> {
            commentBox.setStyle("-fx-background-color: rgba(238, 239, 240, 0.7);");
        });
        
        commentBox.setOnMouseExited(e -> {
            commentBox.setStyle("-fx-background-color: rgba(248, 249, 250, 0.5);");
        });
        
        return commentBox;
    }
    
    private void updateComment(Commentaire comment, String newContent, Label commentText) {
        if (newContent == null || newContent.trim().isEmpty()) {
            AlertUtils.showWarning("Modification", "Comment cannot be empty.");
            return;
        }
        
        try {
            comment.setContenuComment(newContent.trim());
            // Mettre à jour la date lors de la modification
            comment.setDateComment(LocalDate.now());
            commentaireService.update(comment);
            
            AlertUtils.showConfirmation("Modification", "Comment updated successfully!");
            loadComments();
            
            // Rafraîchir le parent si nécessaire
            if (parentController != null) {
                parentController.refreshData();
            }
            
            commentText.setText(newContent);
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.showError("Error", "Unable to update comment: " + e.getMessage());
        }
    }
    
    private void deleteComment(Commentaire comment, VBox commentBox) {
        boolean confirm = AlertUtils.showConfirmation("Delete", 
                "Are you sure you want to delete this comment?");
        
        if (confirm) {
            try {
                commentaireService.delete(comment.getId());
                
                AlertUtils.showConfirmation("Delete", "Comment deleted successfully!");
                loadComments();
                
                // Rafraîchir le parent si nécessaire
                if (parentController != null) {
                    parentController.refreshData();
                }
                
                commentBox.getChildren().clear();
            } catch (SQLException e) {
                e.printStackTrace();
                AlertUtils.showError("Error", "Unable to delete comment: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void closeModal() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
} 
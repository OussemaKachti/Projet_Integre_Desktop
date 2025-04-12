package com.esprit.controllers;

import com.esprit.models.Commentaire;
import com.esprit.models.Sondage;
import com.esprit.models.ChoixSondage;
import com.esprit.services.SondageService;
import com.esprit.services.ChoixSondageService;
import com.esprit.services.CommentaireService;
import com.esprit.services.ReponseService;
import com.esprit.services.UserService;
import com.esprit.utils.AlertUtils;
import com.esprit.utils.NavigationManager;

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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.ResourceBundle;

public class PollDetailsController implements Initializable {

    @FXML private Label questionLabel;
    @FXML private Label clubLabel;
    @FXML private Label createdAtLabel;
    @FXML private VBox responseStatsContainer;
    @FXML private HBox voteCardsContainer;
    @FXML private TableView<Commentaire> commentsTable;
    @FXML private TableColumn<Commentaire, String> userColumn;
    @FXML private TableColumn<Commentaire, String> commentColumn;
    @FXML private TableColumn<Commentaire, String> createdAtColumn;
    @FXML private TableColumn<Commentaire, Void> actionsColumn;
    @FXML private StackPane noCommentsPane;
    @FXML private Button backButton;
    @FXML private Pagination commentsPagination;

    private Sondage currentSondage;
    private final SondageService sondageService = new SondageService();
    private final ChoixSondageService choixSondageService = new ChoixSondageService();
    private final ReponseService reponseService = new ReponseService();
    private final CommentaireService commentaireService = new CommentaireService();
    private final UserService userService = new UserService();
    
    // Stockage des commentaires pour la pagination
    private ObservableList<Commentaire> allComments = FXCollections.observableArrayList();
    private final int COMMENTS_PER_PAGE = 3;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("PollDetailsController: initializing...");
        // Configure the comments table
        setupCommentsTable();
        
        // Configure the pagination
        commentsPagination.setPageCount(1);
        commentsPagination.getStyleClass().clear();
        commentsPagination.getStyleClass().add("pagination");
        commentsPagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            updateCommentsTable(newIndex.intValue());
        });
    }

    public void setSondage(Sondage sondage) {
        System.out.println("Setting sondage: " + sondage.getId() + " - " + sondage.getQuestion());
        this.currentSondage = sondage;
        loadSondageDetails();
    }

    private void loadSondageDetails() {
        if (currentSondage == null) {
            System.err.println("Error: currentSondage is null");
            return;
        }

        try {
            // Load poll information
            questionLabel.setText(currentSondage.getQuestion());
            
            if (currentSondage.getClub() != null) {
                clubLabel.setText(currentSondage.getClub().getNom());
            } else {
                clubLabel.setText("N/A");
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            createdAtLabel.setText(currentSondage.getCreatedAt().format(formatter));

            // Load response statistics
            loadResponseStats();

            // Load vote distribution cards
            loadVoteDistributionCards();

            // Load comments
            loadComments();

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.showError("Error", "Failed to load poll details: " + e.getMessage());
        }
    }

    private void loadResponseStats() throws SQLException {
        responseStatsContainer.getChildren().clear();

        List<ChoixSondage> options = choixSondageService.getBySondage(currentSondage.getId());
        int totalVotes = 0;

        // Calculate total votes
        for (ChoixSondage option : options) {
            int votes = reponseService.getVotesByChoix(option.getId());
            totalVotes += votes;
        }

        // Create progress bars for each option
        for (ChoixSondage option : options) {
            int votes = reponseService.getVotesByChoix(option.getId());
            double percentage = totalVotes > 0 ? (double) votes / totalVotes * 100 : 0;

            // Create VBox for each option
            VBox optionBox = new VBox(5);
            
            // Create option info line
            HBox infoBox = new HBox(10);
            Label optionLabel = new Label(option.getContenu());
            optionLabel.setStyle("-fx-font-weight: bold;");
            Label votesLabel = new Label(votes + " votes (" + String.format("%.0f%%", percentage) + ")");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            infoBox.getChildren().addAll(optionLabel, spacer, votesLabel);
            
            // Create progress bar
            ProgressBar progressBar = new ProgressBar(percentage / 100);
            progressBar.setPrefHeight(20);
            progressBar.setPrefWidth(Double.MAX_VALUE);
            progressBar.getStyleClass().add("poll-progress-bar");
            
            // Use blue color for "tunis" option
            if (option.getContenu().toLowerCase().contains("tunis")) {
                progressBar.setStyle("-fx-accent: #4e73df;");
            }
            
            optionBox.getChildren().addAll(infoBox, progressBar);
            responseStatsContainer.getChildren().add(optionBox);
        }
    }

    private void loadVoteDistributionCards() throws SQLException {
        voteCardsContainer.getChildren().clear();

        List<ChoixSondage> options = choixSondageService.getBySondage(currentSondage.getId());
        
        // Collect votes for each option
        Map<String, Integer> voteMap = new HashMap<>();
        for (ChoixSondage option : options) {
            int votes = reponseService.getVotesByChoix(option.getId());
            voteMap.put(option.getContenu(), votes);
        }
        
        // Create a card for each option
        int colorIndex = 0;
        String[] colors = {"#bec5f7", "#ffd6cc", "#d1f7c4", "#ffedc9"};
        
        for (Map.Entry<String, Integer> entry : voteMap.entrySet()) {
            String option = entry.getKey();
            int votes = entry.getValue();
            
            // Create a card with dynamic CSS styling
            VBox card = new VBox();
            card.setPrefWidth(300);
            card.setPrefHeight(150);
            card.setAlignment(Pos.CENTER);
            card.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 10; -fx-padding: 15;", 
                    colors[colorIndex % colors.length]));
            
            // Vote count
            Label voteCount = new Label(String.valueOf(votes));
            voteCount.setFont(Font.font("System", FontWeight.BOLD, 48));
            voteCount.setTextFill(Color.web("#666666"));
            
            // Option name
            Label optionName = new Label(option);
            optionName.setFont(Font.font("System", 16));
            optionName.setTextFill(Color.web("#666666"));
            
            card.getChildren().addAll(voteCount, optionName);
            VBox.setMargin(optionName, new Insets(10, 0, 0, 0));
            
            voteCardsContainer.getChildren().add(card);
            colorIndex++;
        }
    }

    private void setupCommentsTable() {
        try {
            userColumn.setCellValueFactory(cellData -> {
                Commentaire comment = cellData.getValue();
                if (comment.getUser() != null) {
                    return new SimpleStringProperty("membre ieee");
                }
                return new SimpleStringProperty("Anonyme");
            });
            
            commentColumn.setCellValueFactory(new PropertyValueFactory<>("contenuComment"));
            
            createdAtColumn.setCellValueFactory(cellData -> {
                Commentaire comment = cellData.getValue();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                return new SimpleStringProperty(comment.getDateComment().format(formatter));
            });
            
            // Configure actions column (delete button)
            actionsColumn.setCellFactory(col -> new TableCell<Commentaire, Void>() {
                private final Button deleteButton = new Button("Delete");
                
                {
                    deleteButton.getStyleClass().add("bin-button");
                    deleteButton.setOnAction(e -> {
                        Commentaire comment = getTableView().getItems().get(getIndex());
                        try {
                            // Create a custom confirmation dialog
                            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
                            confirmDialog.setTitle("Confirmation");
                            confirmDialog.setHeaderText("Supprimer le commentaire");
                            confirmDialog.setContentText("Êtes-vous sûr de vouloir supprimer ce commentaire ?");
                            
                            // Customize the dialog
                            DialogPane dialogPane = confirmDialog.getDialogPane();
                            dialogPane.getStylesheets().add(getClass().getResource("/com/esprit/styles/poll-details-style.css").toExternalForm());
                            dialogPane.getStyleClass().add("custom-alert");
                            
                            // Get the confirm and cancel buttons
                            Button confirmButton = (Button) dialogPane.lookupButton(ButtonType.OK);
                            Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
                            
                            if (confirmButton != null) {
                                confirmButton.setText("Oui");
                                confirmButton.getStyleClass().add("delete-confirm-button");
                            }
                            
                            if (cancelButton != null) {
                                cancelButton.setText("Non");
                                cancelButton.getStyleClass().add("cancel-button");
                            }
                            
                            // Show dialog and process result
                            if (confirmDialog.showAndWait().filter(response -> response == ButtonType.OK).isPresent()) {
                                commentaireService.delete(comment.getId());
                                showToast("Comment deleted successfully", "success");
                                loadComments(); // Reload comments
                            }
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                            showToast("Failed to delete comment: " + ex.getMessage(), "error");
                            AlertUtils.showError("Error", "Failed to delete comment: " + ex.getMessage());
                        }
                    });
                }
                
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(deleteButton);
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Error setting up comments table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadComments() throws SQLException {
        // Get all comments
        allComments = commentaireService.getBySondage(currentSondage.getId());
        
        if (allComments.isEmpty()) {
            commentsTable.setVisible(false);
            commentsPagination.setVisible(false);
            commentsPagination.setManaged(false);
            noCommentsPane.setVisible(true);
            noCommentsPane.setManaged(true);
        } else {
            commentsTable.setVisible(true);
            commentsPagination.setVisible(true);
            commentsPagination.setManaged(true);
            noCommentsPane.setVisible(false);
            noCommentsPane.setManaged(false);
            
            // Configure pagination with page numbers
            int pageCount = Math.max((int) Math.ceil((double) allComments.size() / COMMENTS_PER_PAGE), 1);
            commentsPagination.setPageCount(pageCount);
            commentsPagination.setMaxPageIndicatorCount(5);
            commentsPagination.setCurrentPageIndex(0);
            
            // Update table for the initial page
            updateCommentsTable(0);
        }
    }
    
    private void updateCommentsTable(int pageIndex) {
        // Calculate start and end indexes
        int fromIndex = pageIndex * COMMENTS_PER_PAGE;
        int toIndex = Math.min(fromIndex + COMMENTS_PER_PAGE, allComments.size());
        
        if (fromIndex >= allComments.size()) {
            commentsTable.setItems(FXCollections.observableArrayList());
            return;
        }
        
        // Create sublist for current page
        ObservableList<Commentaire> currentPageComments = FXCollections.observableArrayList(
            allComments.subList(fromIndex, toIndex)
        );
        
        // Update table
        commentsTable.setItems(currentPageComments);
    }

    @FXML
    private void onBackButtonClick() {
        try {
            // Charger la vue AdminPollsView
            URL fxmlUrl = getClass().getResource("/com/esprit/views/AdminPollsView.fxml");
            if (fxmlUrl == null) {
                System.err.println("FXML file not found: /com/esprit/views/AdminPollsView.fxml");
                AlertUtils.showError("Error", "Fichier FXML introuvable: /com/esprit/views/AdminPollsView.fxml");
                return;
            }
            
            // Charger le CSS
            URL cssUrl = getClass().getResource("/com/esprit/styles/admin-polls-style.css");
            
            // Créer le loader
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            
            // Récupérer les dimensions de la fenêtre actuelle
            double width = backButton.getScene().getWindow().getWidth();
            double height = backButton.getScene().getWindow().getHeight();
            
            // Créer la scène avec les mêmes dimensions
            Scene scene = new Scene(root, width, height);
            
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            
            // Utiliser NavigationManager pour naviguer vers la vue AdminPollsView
            NavigationManager.navigateTo(scene);
            
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtils.showError("Error", "Erreur lors du retour à la vue précédente: " + e.getMessage());
        }
    }

    // Add toast notification system
    private void showToast(String message, String type) {
        try {
            // Create toast container and components
            VBox toastContainer = new VBox();
            toastContainer.setStyle("-fx-background-radius: 5; -fx-padding: 10 15 10 15; -fx-max-width: 300;");
            if ("success".equals(type)) {
                toastContainer.setStyle(toastContainer.getStyle() + "-fx-background-color: #4CAF50;");
            } else {
                toastContainer.setStyle(toastContainer.getStyle() + "-fx-background-color: #F44336;");
            }
            
            Label toastMessage = new Label(message);
            toastMessage.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            toastContainer.getChildren().add(toastMessage);
            
            // Create a separate stage for the toast
            Stage toastStage = new Stage();
            toastStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            
            StackPane root = new StackPane(toastContainer);
            root.setStyle("-fx-background-color: transparent;");
            root.setPadding(new Insets(20));
            
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            toastStage.setScene(scene);
            
            // Position the toast at the top-right corner of the main window
            Stage mainStage = (Stage) questionLabel.getScene().getWindow();
            toastStage.setX(mainStage.getX() + mainStage.getWidth() - 320);
            toastStage.setY(mainStage.getY() + 20);
            
            // Set initial opacity
            toastContainer.setOpacity(0);
            
            // Show the stage
            toastStage.show();
            
            // Animation fade in
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toastContainer);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
            
            // Wait and fade out
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    Platform.runLater(() -> {
                        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), toastContainer);
                        fadeOut.setFromValue(1);
                        fadeOut.setToValue(0);
                        fadeOut.setOnFinished(event -> toastStage.close());
                        fadeOut.play();
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            System.err.println("Error showing toast: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to Alert if toast fails
            Platform.runLater(() -> {
                Alert alert = new Alert("success".equals(type) ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                alert.setTitle("success".equals(type) ? "Success" : "Error");
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.show();
            });
        }
    }
} 
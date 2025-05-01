package com.esprit.controllers;

import com.esprit.models.Commentaire;
import com.esprit.models.Club;
import com.esprit.services.CommentaireService;
import com.esprit.services.ClubService;
import com.esprit.utils.AlertUtils;
import com.esprit.utils.NavigationManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import java.io.IOException;

public class AdminCommentsController implements Initializable {

    @FXML
    private Label totalCommentsLabel;

    @FXML
    private Label todayCommentsLabel;

    @FXML
    private Label flaggedCommentsLabel;

    @FXML
    private ComboBox<String> clubFilterComboBox;

    @FXML
    private TableView<Commentaire> commentsTable;

    @FXML
    private TableColumn<Commentaire, Integer> idColumn;

    @FXML
    private TableColumn<Commentaire, String> userColumn;

    @FXML
    private TableColumn<Commentaire, String> commentColumn;

    @FXML
    private TableColumn<Commentaire, String> clubColumn;

    @FXML
    private TableColumn<Commentaire, String> createdAtColumn;

    @FXML
    private TableColumn<Commentaire, Void> actionsColumn;

    @FXML
    private HBox paginationContainer;

    @FXML
    private Pane toastContainer;

    @FXML
    private VBox noCommentsContainer;

    // Sidebar navigation buttons
    @FXML
    private Button userManagementBtn;
    @FXML
    private Button clubManagementBtn;
    @FXML
    private Button eventManagementBtn;
    @FXML
    private Button productOrdersBtn;
    @FXML
    private Button competitionBtn;
    @FXML
    private Button surveyManagementBtn;
    @FXML
    private Button pollsManagementBtn;
    @FXML
    private Button commentsManagementBtn;
    @FXML
    private Button profileBtn;
    @FXML
    private Button logoutBtn;
    @FXML
    private VBox surveySubMenu;
    @FXML
    private Label adminNameLabel;

    @FXML
    private Label mostActiveUserLabel;

    @FXML
    private Label mostActiveUserCommentsLabel;

    private CommentaireService commentaireService;
    private ClubService clubService;

    private ObservableList<Commentaire> commentsList = FXCollections.observableArrayList();
    private ObservableList<String> clubsList = FXCollections.observableArrayList();

    // Statistiques
    private int totalComments = 0;
    private int todayComments = 0;
    private int flaggedComments = 0;

    // Pagination
    private int currentPage = 1;
    private final int PAGE_SIZE = 3;
    private int totalPages = 1;

    // Filtre sélectionné
    private String selectedClub = "all";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            // Initialize services
            commentaireService = new CommentaireService();
            clubService = new ClubService();

            // Setup UI components
            setupTableColumns();
            loadClubs();
            loadComments();
            setupEventHandlers();
            setupPagination();
            setupNavigationEvents();
            setupAdminInfo();

        } catch (SQLException e) {
            AlertUtils.showError("Error", "Unable to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        userColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getUser() != null) {
                return new SimpleStringProperty(
                        cellData.getValue().getUser().getFirstName() + " "
                                + cellData.getValue().getUser().getLastName());
            } else {
                return new SimpleStringProperty("Unknown");
            }
        });

        commentColumn.setCellValueFactory(cellData -> {
            String commentText = cellData.getValue().getContenuComment();
            if (commentText == null) {
                return new SimpleStringProperty("N/A");
            }

            // Traiter les commentaires longs : limiter à environ 30 caractères
            if (commentText.length() > 30) {
                return new SimpleStringProperty(commentText.substring(0, 30) + "...");
            } else {
                return new SimpleStringProperty(commentText);
            }
        });

        // Ajouter la gestion des commentaires longs avec fenêtre popup
        commentColumn.setCellFactory(col -> {
            return new TableCell<Commentaire, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        // Limiter l'affichage à une hauteur maximale
                        setMaxHeight(60);
                        setPrefHeight(50);

                        // Forcer le texte à revenir à la ligne
                        setWrapText(true);

                        // Formater le texte pour ajouter des retours à la ligne après environ 5 mots
                        String formattedText = item;
                        if (!item.endsWith("...")) {
                            String[] words = item.split(" ");
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < words.length; i++) {
                                sb.append(words[i]).append(" ");
                                if ((i + 1) % 5 == 0 && i < words.length - 1) {
                                    sb.append("\n");
                                }
                            }
                            formattedText = sb.toString();
                        }

                        setText(formattedText);

                        // Ajouter un gestionnaire de clic pour les commentaires tronqués
                        if (item.endsWith("...")) {
                            // Mettre en style avec curseur pointer pour indiquer qu'il est cliquable
                            setStyle("-fx-cursor: hand; -fx-text-fill: #0066cc; -fx-underline: true;");

                            this.setOnMouseClicked(event -> {
                                // Récupérer le commentaire complet
                                Commentaire commentaire = getTableView().getItems().get(getIndex());
                                String fullComment = commentaire.getContenuComment();

                                // Créer une boîte de dialogue pour afficher le texte complet
                                Alert dialog = new Alert(Alert.AlertType.INFORMATION);
                                dialog.setTitle("Commentaire complet");
                                dialog.setHeaderText("Commentaire de " +
                                        (commentaire.getUser() != null
                                                ? (commentaire.getUser().getFirstName() + " "
                                                        + commentaire.getUser().getLastName())
                                                : "Utilisateur inconnu"));

                                // Utiliser un TextArea pour permettre le défilement si nécessaire
                                TextArea textArea = new TextArea(fullComment);
                                textArea.setEditable(false);
                                textArea.setWrapText(true);
                                textArea.setPrefWidth(480);
                                textArea.setPrefHeight(200);

                                dialog.getDialogPane().setContent(textArea);

                                // Styliser la boîte de dialogue
                                DialogPane dialogPane = dialog.getDialogPane();
                                dialogPane.setPrefWidth(500);
                                dialogPane.getStylesheets().add(getClass()
                                        .getResource("/com/esprit/styles/admin-polls-style.css").toExternalForm());

                                // Ajouter un bouton de fermeture uniquement
                                dialog.getButtonTypes().setAll(ButtonType.CLOSE);

                                // Afficher la boîte de dialogue
                                dialog.showAndWait();
                            });
                        } else {
                            // Style normal pour les commentaires courts
                            setStyle("-fx-text-fill: #333333;");
                            this.setOnMouseClicked(null);
                        }
                    }
                }
            };
        });

        clubColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getSondage() != null && cellData.getValue().getSondage().getClub() != null) {
                return new SimpleStringProperty(cellData.getValue().getSondage().getClub().getNom());
            } else {
                return new SimpleStringProperty("Unknown");
            }
        });

        createdAtColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDateComment() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                return new SimpleStringProperty(cellData.getValue().getDateComment().format(formatter));
            } else {
                return new SimpleStringProperty("N/A");
            }
        });

        // Configuration de la colonne d'actions
        actionsColumn.setCellFactory(createActionButtonCellFactory());
    }

    private Callback<TableColumn<Commentaire, Void>, TableCell<Commentaire, Void>> createActionButtonCellFactory() {
        return new Callback<>() {
            @Override
            public TableCell<Commentaire, Void> call(final TableColumn<Commentaire, Void> param) {
                return new TableCell<>() {
                    private final Button deleteButton = new Button("Delete");

                    {
                        // Configure delete button with a proper styling
                        deleteButton.getStyleClass().addAll("btn", "btn-danger", "delete-button");
                        deleteButton.setTooltip(new Tooltip("Delete this comment"));
                        deleteButton.setStyle(
                                "-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-min-width: 80px;");

                        // Add hover effect
                        deleteButton.setOnMouseEntered(e -> deleteButton.setStyle(
                                "-fx-background-color: #c82333; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-min-width: 80px;"));
                        deleteButton.setOnMouseExited(e -> deleteButton.setStyle(
                                "-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-min-width: 80px;"));

                        // Configure click handler
                        deleteButton.setOnAction(event -> {
                            Commentaire commentaire = getTableView().getItems().get(getIndex());
                                deleteComment(commentaire);
                        });
                    }
                    
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            // Check if the comment is flagged and highlight
                            Commentaire comment = getTableView().getItems().get(getIndex());
                            if (comment.getContenuComment() != null &&
                                    comment.getContenuComment().contains("⚠️")) {
                                // Visual indicator for flagged comments
                                HBox container = new HBox(5);
                                container.setAlignment(Pos.CENTER);

                                // Label flagIndicator = new Label("⚠️");
                                // flagIndicator.setTooltip(new Tooltip("This comment was flagged"));
                                // flagIndicator.setStyle("-fx-font-size: 18px;");

                                container.getChildren().addAll(deleteButton);
                                setGraphic(container);
                            } else {
                                setGraphic(deleteButton);
                            }
                        }
                    }
                };
            }
        };
    }

    private void loadClubs() throws SQLException {
            // Get all clubs from database
            List<Club> clubsList = clubService.getAll();

            // Create a new ObservableList with "All Clubs" as first option
            this.clubsList = FXCollections.observableArrayList();
            this.clubsList.add("All Clubs");

            // Add the actual club names
            this.clubsList.addAll(clubsList.stream()
                    .map(Club::getNom)
                    .sorted() // Sort clubs alphabetically
                    .collect(Collectors.toList()));

            // Set items to the ComboBox
            clubFilterComboBox.setItems(this.clubsList);

            // Set custom cell factory for better display
            clubFilterComboBox.setCellFactory(listView -> new ListCell<String>() {
                @Override
                protected void updateItem(String club, boolean empty) {
                    super.updateItem(club, empty);

                    if (empty || club == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        // Create an HBox for the cell content
                        HBox cellBox = new HBox(10);
                        cellBox.setAlignment(Pos.CENTER_LEFT);

                        // Create icon based on whether it's "All Clubs" or a specific club
                        Label icon = new Label();
                        if ("All Clubs".equals(club)) {
                            icon.setText("🌐");
                        } else {
                            icon.setText("🏢");
                        }
                        icon.setStyle("-fx-font-size: 14px;");

                        // Create label for club name
                        Label clubLabel = new Label(club);
                        clubLabel.setStyle("-fx-font-size: 14px;");

                        // Add components to cell
                        cellBox.getChildren().addAll(icon, clubLabel);

                        setGraphic(cellBox);
                        setText(null);
                    }
                }
            });

            // Set custom button cell for the selected value display
            clubFilterComboBox.setButtonCell(new ListCell<String>() {
                @Override
                protected void updateItem(String club, boolean empty) {
                    super.updateItem(club, empty);
                    if (empty || club == null) {
                    setText(null);
                        setStyle("-fx-text-fill: #333333;"); // S'assurer que le texte est noir
                    } else {
                    setText(club);
                        setStyle("-fx-text-fill: #333333;"); // S'assurer que le texte est noir
                    }
                }
            });

            // Select first item (All Clubs)
            clubFilterComboBox.getSelectionModel().selectFirst();
            selectedClub = "all"; // Default value

            // Add style class for custom styling
            clubFilterComboBox.getStyleClass().add("club-filter-combo");
    }

    private void loadComments() {
        try {
            // Show a loading indicator (can be implemented later)
            commentsTable.setPlaceholder(new Label("Loading comments..."));

            List<Commentaire> comments;

            // Get comments based on selected club
            if ("All Clubs".equals(selectedClub) || selectedClub.equals("all")) {
                // Get all comments
                ObservableList<Commentaire> allComments = commentaireService.getAllComments();
                comments = new ArrayList<>(allComments);
                System.out.println("Loaded " + comments.size() + " comments (all clubs)");
            } else {
                // Filter by club name
                ObservableList<Commentaire> allComments = commentaireService.getAllComments();
                comments = allComments.stream()
                        .filter(comment -> comment.getSondage() != null &&
                                comment.getSondage().getClub() != null &&
                                comment.getSondage().getClub().getNom().equals(selectedClub))
                        .collect(Collectors.toList());
                System.out.println("Loaded " + comments.size() + " comments for club: " + selectedClub);
            }

            commentsList.clear();

            if (comments.isEmpty()) {
                commentsTable.setVisible(true);
                commentsTable.setPlaceholder(new Label("No comments found for the selected filter"));
                noCommentsContainer.setVisible(true);
                paginationContainer.setVisible(false);
            } else {
                commentsTable.setVisible(true);
                noCommentsContainer.setVisible(false);
                paginationContainer.setVisible(true);

                // Pagination
                int fromIndex = (currentPage - 1) * PAGE_SIZE;
                int toIndex = Math.min(fromIndex + PAGE_SIZE, comments.size());

                if (fromIndex <= toIndex) {
                    commentsList.addAll(comments.subList(fromIndex, toIndex));
                }

                totalPages = (int) Math.ceil((double) comments.size() / PAGE_SIZE);
            }

            commentsTable.setItems(commentsList);

            // Update pagination
            setupPagination();
            
        } catch (SQLException e) {
            commentsTable.setPlaceholder(new Label("Error loading comments: " + e.getMessage()));
            AlertUtils.showError("Error", "Unable to load comments: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void calculateStats() {
        try {
            // Récupérer tous les commentaires
            List<Commentaire> allComments = commentaireService.getAllComments();

            // Calculer le nombre total de commentaires
            totalComments = allComments.size();
            totalCommentsLabel.setText(String.valueOf(totalComments));
            
            // Calculer le nombre de commentaires d'aujourd'hui
            LocalDate today = LocalDate.now();
            todayComments = (int) allComments.stream()
                    .filter(c -> c.getDateComment() != null && c.getDateComment().equals(today))
                    .count();
            todayCommentsLabel.setText(String.valueOf(todayComments));
            
            // Calculer le nombre de commentaires signalés (à implémenter selon votre
            // logique métier)
            // Pour l'exemple, on considère qu'un commentaire contenant le mot "hidden" est
            // signalé
            flaggedComments = (int) allComments.stream()
                    .filter(c -> c.getContenuComment() != null
                            && c.getContenuComment().toLowerCase().contains("hidden"))
                    .count();
            flaggedCommentsLabel.setText(String.valueOf(flaggedComments));
            
            // Trouver l'utilisateur le plus actif
            findMostActiveUser(allComments);

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Erreur lors du calcul des statistiques : " + e.getMessage());
        }
    }

    /**
     * Trouve l'utilisateur ayant posté le plus de commentaires
     * 
     * @param comments Liste des commentaires
     */
    private void findMostActiveUser(List<Commentaire> comments) {
        // Vérifier s'il y a des commentaires
        if (comments == null || comments.isEmpty()) {
            mostActiveUserLabel.setText("Aucun utilisateur");
            mostActiveUserCommentsLabel.setText("0 commentaire");
            return;
        }

        // Compter les commentaires par utilisateur
        Map<String, Integer> userCommentCount = new HashMap<>();

        for (Commentaire comment : comments) {
            if (comment.getUser() != null) {
                String userName = comment.getUser().getFirstName() + " " + comment.getUser().getLastName();
                userCommentCount.put(userName, userCommentCount.getOrDefault(userName, 0) + 1);
            }
        }

        // Trouver l'utilisateur avec le plus de commentaires
        if (!userCommentCount.isEmpty()) {
            String mostActiveUser = "";
            int maxComments = 0;

            for (Map.Entry<String, Integer> entry : userCommentCount.entrySet()) {
                if (entry.getValue() > maxComments) {
                    maxComments = entry.getValue();
                    mostActiveUser = entry.getKey();
                }
            }

            // Mettre à jour les labels
            mostActiveUserLabel.setText(mostActiveUser);
            mostActiveUserCommentsLabel.setText(maxComments + " commentaire" + (maxComments > 1 ? "s" : ""));
        } else {
            mostActiveUserLabel.setText("Aucun utilisateur");
            mostActiveUserCommentsLabel.setText("0 commentaire");
        }
    }

    private void setupEventHandlers() {
        clubFilterComboBox.setOnAction(event -> {
            String selected = clubFilterComboBox.getValue();
            if (selected == null) {
                selected = "All Clubs";
            }

            // Show a short animation or visual feedback when filter changes
            clubFilterComboBox.setDisable(true); // Temporarily disable to show processing

            // Convert "All Clubs" to internal value "all"
            selectedClub = "All Clubs".equals(selected) ? "all" : selected;

            // Reset to page 1 when changing filter
            currentPage = 1;

            // Load filtered comments
            loadComments();

            // Re-enable after short delay to show processing
            new Thread(() -> {
                try {
                    Thread.sleep(300); // Short delay for visual feedback
                    Platform.runLater(() -> clubFilterComboBox.setDisable(false));
                } catch (InterruptedException e) {
                    Platform.runLater(() -> clubFilterComboBox.setDisable(false));
                    e.printStackTrace();
                }
            }).start();

            // Show toast with filter info
            showToast("Filtered by " + (selectedClub.equals("all") ? "all clubs" : "club: " + selectedClub), "info");
        });
    }

    private void setupPagination() {
        paginationContainer.getChildren().clear();
        
        if (totalPages <= 1) {
            // Hide pagination container if there's only one page
            paginationContainer.setVisible(false);
            paginationContainer.setManaged(false);
            return;
        }

        // Show pagination if more than one page
        paginationContainer.setVisible(true);
        paginationContainer.setManaged(true);
        
        // Previous button
        Button prevButton = new Button("←");
        prevButton.getStyleClass().add(currentPage == 1 ? "pagination-button-disabled" : "pagination-button");
        prevButton.setDisable(currentPage == 1);
        prevButton.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                loadComments();
            }
        });
        
        paginationContainer.getChildren().add(prevButton);
        
        // Pages numbered buttons
        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(startPage + 4, totalPages);
        
        for (int i = startPage; i <= endPage; i++) {
            Button pageButton = new Button(String.valueOf(i));
            pageButton.getStyleClass().addAll("pagination-button");

            // Add active class for current page and style
            if (i == currentPage) {
                pageButton.getStyleClass().add("pagination-button-active");
                pageButton.setStyle("-fx-font-weight: bold;");
            }
            
            final int pageNum = i;
            pageButton.setOnAction(e -> {
                currentPage = pageNum;
                loadComments();
            });
            paginationContainer.getChildren().add(pageButton);
        }
        
        // Next button
        Button nextButton = new Button("→");
        nextButton.getStyleClass().add(currentPage == totalPages ? "pagination-button-disabled" : "pagination-button");
        nextButton.setDisable(currentPage == totalPages);
        nextButton.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                loadComments();
            }
        });
        
        paginationContainer.getChildren().add(nextButton);

        // Add page count information
        Label pageInfoLabel = new Label(String.format("Page %d of %d", currentPage, totalPages));
        pageInfoLabel.setStyle("-fx-text-fill: #6c757d; -fx-padding: 5 0 0 10;");
        paginationContainer.getChildren().add(pageInfoLabel);
    }

    private void deleteComment(Commentaire commentaire) {
        try {
            // Create a custom confirmation dialog
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Delete Comment");
            confirmDialog.setHeaderText("Are you sure you want to delete this comment?");
            confirmDialog.setContentText("This action cannot be undone.");

            // Customize the dialog
            DialogPane dialogPane = confirmDialog.getDialogPane();
            dialogPane.getStylesheets()
                    .add(getClass().getResource("/com/esprit/styles/admin-polls-style.css").toExternalForm());
            dialogPane.getStyleClass().add("custom-alert");

            // Get the confirm and cancel buttons
            Button confirmButton = (Button) dialogPane.lookupButton(ButtonType.OK);
            Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);

            if (confirmButton != null) {
                confirmButton.setText("Delete");
                confirmButton.getStyleClass().add("delete-confirm-button");
                confirmButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
            }

            if (cancelButton != null) {
                cancelButton.setText("Cancel");
                cancelButton.getStyleClass().add("cancel-button");
            }

            // Show dialog and process result
            if (confirmDialog.showAndWait().filter(response -> response == ButtonType.OK).isPresent()) {
                    commentaireService.delete(commentaire.getId());
                    showToast("Comment deleted successfully", "success");
                loadComments();
                calculateStats();
            }
                } catch (SQLException e) {
                    showToast("Error deleting comment: " + e.getMessage(), "error");
            e.printStackTrace();
        } catch (Exception e) {
            showToast("Unexpected error: " + e.getMessage(), "error");
            e.printStackTrace();
                }
    }

    private void showToast(String message, String type) {
        Label toastLabel = (Label) ((HBox) toastContainer.getChildren().get(0)).getChildren().get(0);
        HBox toastHBox = (HBox) toastContainer.getChildren().get(0);

        switch (type) {
            case "error":
                toastHBox.setStyle("-fx-background-color: #dc3545; -fx-background-radius: 4px;");
                break;
            case "info":
                toastHBox.setStyle("-fx-background-color: #007bff; -fx-background-radius: 4px;");
                break;
            default: // success
                toastHBox.setStyle("-fx-background-color: #28a745; -fx-background-radius: 4px;");
                break;
        }

        toastLabel.setText(message);
        toastContainer.setVisible(true);
        
        // Add a fade-in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toastContainer);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Hide toast after 3 seconds with fade out animation
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(500), toastContainer);
                    fadeOut.setFromValue(1);
                    fadeOut.setToValue(0);
                    fadeOut.setOnFinished(e -> toastContainer.setVisible(false));
                    fadeOut.play();
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
                Platform.runLater(() -> toastContainer.setVisible(false));
            }
        }).start();
    }

    // Méthode pour rafraîchir les données
    public void refreshData() {
        loadComments();
        calculateStats();
        setupPagination();
    }

    /**
     * Configure les événements de navigation pour la sidebar
     */
    private void setupNavigationEvents() {
        // Navigation back to AdminPollsView
        pollsManagementBtn.setOnAction(event -> {
            try {
                // Charger la vue des sondages
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/AdminPollsView.fxml"));
                Parent root = loader.load();

                // Obtenir le stage actuel directement depuis la scène du bouton
                Stage stage = (Stage) pollsManagementBtn.getScene().getWindow();

                // Configurer la scène
                Scene scene = new Scene(root);

                // S'assurer que les styles sont correctement appliqués
                scene.getStylesheets().add(getClass().getResource("/com/esprit/styles/admin-polls-style.css").toExternalForm());
                scene.getStylesheets().add(getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm());

                // Appliquer la scène au stage
                stage.setScene(scene);
                stage.setMaximized(true);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showToast("Erreur lors de la navigation vers la gestion des sondages: " + e.getMessage(), "error");
            }
        });

        // Pour le bouton principal Survey Management, on peut ajouter une animation pour montrer/cacher le sous-menu
        surveyManagementBtn.setOnAction(event -> {
            // Toggle la visibilité du sous-menu
            boolean isVisible = surveySubMenu.isVisible();
            surveySubMenu.setVisible(!isVisible);
            surveySubMenu.setManaged(!isVisible);
        });

        // Navigation vers admin_dashboard
        userManagementBtn.setOnAction(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/admin_dashboard.fxml"));
                Parent root = loader.load();

                // Obtenir le stage actuel directement depuis la scène du bouton
                Stage stage = (Stage) userManagementBtn.getScene().getWindow();

                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm());

                // Appliquer la scène au stage
                stage.setScene(scene);
                stage.setMaximized(true);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showToast("Error navigating to user management: " + e.getMessage(), "error");
            }
        });

        // Configurer les autres boutons de navigation si nécessaire
        clubManagementBtn.setOnAction(e -> showToast("Fonctionnalité en développement: Gestion des clubs", "info"));
        eventManagementBtn.setOnAction(e -> showToast("Fonctionnalité en développement: Gestion des événements", "info"));
        productOrdersBtn.setOnAction(e -> showToast("Fonctionnalité en développement: Produits & Commandes", "info"));
        competitionBtn.setOnAction(e -> showToast("Fonctionnalité en développement: Compétitions", "info"));
        profileBtn.setOnAction(e -> showToast("Fonctionnalité en développement: Profil", "info"));
        logoutBtn.setOnAction(e -> handleLogout());
    }

    /**
     * Gère la déconnexion de l'utilisateur
     */
    private void handleLogout() {
        try {
            // Afficher une confirmation avant de se déconnecter
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Déconnexion");
            confirmDialog.setHeaderText("Êtes-vous sûr de vouloir vous déconnecter ?");
            confirmDialog.setContentText("Toutes les données non enregistrées seront perdues.");

            // Personnaliser la boîte de dialogue
            DialogPane dialogPane = confirmDialog.getDialogPane();
            dialogPane.getStylesheets()
                    .add(getClass().getResource("/com/esprit/styles/admin-polls-style.css").toExternalForm());

            // Afficher la boîte de dialogue et traiter le résultat
            if (confirmDialog.showAndWait().filter(response -> response == ButtonType.OK).isPresent()) {
                // Naviguer vers la page de connexion ou fermer l'application
                Platform.exit();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Erreur lors de la déconnexion: " + e.getMessage(), "error");
        }
    }

    /**
     * Configure les informations de l'administrateur
     */
    private void setupAdminInfo() {
        try {
            // Récupérer l'utilisateur connecté (à implémenter avec la gestion des sessions)
            // Pour l'instant, on affiche un nom par défaut
            if (adminNameLabel != null) {
                adminNameLabel.setText("Admin User");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err
                    .println("Erreur lors de la configuration des informations de l'administrateur: " + e.getMessage());
        }
    }
}
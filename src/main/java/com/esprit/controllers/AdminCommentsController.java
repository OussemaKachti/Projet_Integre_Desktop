package com.esprit.controllers;

import com.esprit.models.Commentaire;
import com.esprit.models.Club;
import com.esprit.services.ClubService;
import com.esprit.services.CommentaireService;
import com.esprit.services.UserService;
import com.esprit.utils.AlertUtils;
import com.esprit.utils.CommentStatsService;
import com.esprit.utils.NavigationManager;
import com.esprit.visualization.CommentBarChartVisualization;

import javafx.animation.FadeTransition;
import javafx.util.Duration;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.util.Callback;

import javafx.scene.chart.PieChart;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
    private ComboBox<String> insightsClubComboBox;

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
    private VBox eventsSubMenu;
    @FXML
    private Label adminNameLabel;

    @FXML
    private Label mostActiveUserLabel;

    @FXML
    private Label mostActiveUserCommentsLabel;

    @FXML
    private Button generateInsightsBtn;

    @FXML
    private VBox aiInsightsContainer;

    @FXML
    private HBox insightsChartContainer;

    @FXML
    private VBox insightsSummaryContainer;

    @FXML
    private Label insightsLoadingLabel;

    private CommentaireService commentaireService;
    private ClubService clubService;
    private CommentStatsService statsService;

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

    // Visualizations
    private CommentBarChartVisualization barChartVisualization;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize services
        commentaireService = new CommentaireService();
        clubService = new ClubService();
        statsService = CommentStatsService.getInstance();

        // Setup UI
        setupTableColumns();
        setupAdminInfo();
        setupNavigationEvents();

        // Load data
        loadClubs();
        setupComboBoxCellFactory(clubFilterComboBox);
        setupComboBoxCellFactory(insightsClubComboBox);
        loadComments();
        setupPagination();
        setupEventHandlers();
        calculateStats();

        // Add listener to clubFilterComboBox to update charts when selection changes
        clubFilterComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                selectedClub = newValue;
                loadComments();
                updateBarChart();
            }
        });

        // Add listener to insightsClubComboBox to update only the chart when selection
        // changes
        insightsClubComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        updateBarChart();
                    }
                });
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
                return new SimpleStringProperty(cellData.getValue().getSondage().getClub().getNomC());
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

    private void loadClubs() {
        // Get all clubs from database
        List<Club> clubsList = clubService.getAll();

        // Create a new ObservableList with "All Clubs" as first option
        this.clubsList = FXCollections.observableArrayList();
        this.clubsList.add("All Clubs");

        // Add the actual club names
        this.clubsList.addAll(clubsList.stream()
                .map(Club::getNomC)
                .sorted() // Sort clubs alphabetically
                .collect(Collectors.toList()));

        // Set items to both ComboBoxes
        clubFilterComboBox.setItems(this.clubsList);
        insightsClubComboBox.setItems(this.clubsList);

        // Set default selection
        clubFilterComboBox.getSelectionModel().selectFirst();
        insightsClubComboBox.getSelectionModel().selectFirst();

        selectedClub = "all"; // Default value

        // Add style class for custom styling
        clubFilterComboBox.getStyleClass().add("club-filter-combo");
        insightsClubComboBox.getStyleClass().add("club-filter-combo");
    }

    private void setupComboBoxCellFactory(ComboBox<String> comboBox) {
        comboBox.setCellFactory(listView -> new ListCell<String>() {
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
        comboBox.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String club, boolean empty) {
                super.updateItem(club, empty);

                if (empty || club == null) {
                    setText("All Clubs");
                    setGraphic(null);
                    setStyle("-fx-text-fill: #333333;");
                } else {
                    HBox cellBox = new HBox(10);
                    cellBox.setAlignment(Pos.CENTER_LEFT);

                    Label icon = new Label();
                    if ("All Clubs".equals(club)) {
                        icon.setText("🌐");
                    } else {
                        icon.setText("🏢");
                    }
                    icon.setStyle("-fx-font-size: 14px;");

                    Label clubLabel = new Label(club);
                    clubLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");

                    cellBox.getChildren().addAll(icon, clubLabel);

                    setGraphic(cellBox);
                    setText(null);
                    setStyle("-fx-text-fill: #333333;");
                }
            }
        });

        // Add style class for custom styling
        comboBox.getStyleClass().add("club-filter-combo");
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
                                comment.getSondage().getClub().getNomC().equals(selectedClub))
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
        // Show comment details when row is clicked
        commentsTable.setRowFactory(tableView -> {
            TableRow<Commentaire> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showCommentDetails(row.getItem());
                }
            });
            return row;
        });

        // Rename and update the button action
        generateInsightsBtn.setText("Show Comments Chart");
        generateInsightsBtn.setOnAction(e -> {
            updateBarChart();
        });

        // Initialize bar chart visualization in insightsChartContainer
        StackPane barChartContainer = new StackPane();
        barChartContainer.setPrefSize(600, 400);
        barChartVisualization = new CommentBarChartVisualization(barChartContainer);

        insightsChartContainer.getChildren().clear();
        insightsChartContainer.getChildren().add(barChartContainer);

        // Initial update of bar chart with all comments
        updateBarChart();
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
                scene.getStylesheets()
                        .add(getClass().getResource("/com/esprit/styles/admin-polls-style.css").toExternalForm());
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

        // Pour le bouton principal Survey Management, on peut ajouter une animation
        // pour montrer/cacher le sous-menu
        surveyManagementBtn.setOnAction(event -> {
            // Toggle la visibilité du sous-menu
            boolean isVisible = surveySubMenu.isVisible();
            surveySubMenu.setVisible(!isVisible);
            surveySubMenu.setManaged(!isVisible);
        });

        // For Events Management, add submenu toggle similar to survey management
        eventManagementBtn.setOnAction(event -> {
            // Toggle the visibility of the submenu
            boolean isVisible = eventsSubMenu.isVisible();
            eventsSubMenu.setVisible(!isVisible);
            eventsSubMenu.setManaged(!isVisible);
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

        // Competition button handler to navigate to AdminSaisons.fxml
        competitionBtn.setOnAction(event -> {
            try {
                // Load the seasons management view
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/AdminSaisons.fxml"));
                Parent root = loader.load();

                // Get current stage from the button's scene
                Stage stage = (Stage) competitionBtn.getScene().getWindow();

                // Configure the scene
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm());

                // Apply the scene to the stage
                stage.setScene(scene);
                stage.setMaximized(true);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showToast("Error navigating to seasons management: " + e.getMessage(), "error");
            }
        });

        // Add navigation to AdminProduitView for productOrdersBtn
        productOrdersBtn.setOnAction(event -> {
            try {
                // Load the product management view
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/esprit/views/produit/AdminProduitView.fxml"));
                Parent root = loader.load();

                // Get current stage from the button's scene
                Stage stage = (Stage) productOrdersBtn.getScene().getWindow();

                // Configure the scene
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm());

                // Apply the scene to the stage
                stage.setScene(scene);
                stage.setMaximized(true);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showToast("Error navigating to product management: " + e.getMessage(), "error");
            }
        });

        profileBtn.setOnAction(e -> showToast("Fonctionnalité en développement: Profil", "info"));
        logoutBtn.setOnAction(e -> handleLogout());

        // Add event handlers for submenu options
        if (eventsSubMenu != null && eventsSubMenu.getChildren().size() >= 2) {
            // Event Management navigation
            ((Button) eventsSubMenu.getChildren().get(0)).setOnAction(event -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/AdminEvent.fxml"));
                    Parent root = loader.load();

                    Stage stage = (Stage) eventManagementBtn.getScene().getWindow();
                    Scene scene = new Scene(root);
                    scene.getStylesheets()
                            .add(getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm());

                    stage.setScene(scene);
                    stage.setMaximized(true);
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                    showToast("Error navigating to event management: " + e.getMessage(), "error");
                }
            });

            // Category Management navigation
            ((Button) eventsSubMenu.getChildren().get(1)).setOnAction(event -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/AdminCat.fxml"));
                    Parent root = loader.load();

                    Stage stage = (Stage) eventManagementBtn.getScene().getWindow();
                    Scene scene = new Scene(root);
                    scene.getStylesheets()
                            .add(getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm());

                    stage.setScene(scene);
                    stage.setMaximized(true);
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                    showToast("Error navigating to category management: " + e.getMessage(), "error");
                }
            });
        }
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

    private void updateBarChart() {
        // Get the selected club
        String selectedClub = insightsClubComboBox.getValue();
        System.out.println("Updating bar chart for club: " + selectedClub);

        // Show loading message
        insightsLoadingLabel.setText("Chargement des données...");
        insightsLoadingLabel.setVisible(true);

        // Run in background thread to avoid freezing UI
        new Thread(() -> {
            try {
                // Get all comments to be processed
                List<Commentaire> allComments = commentaireService.getAllComments();
                System.out.println("Total comments retrieved: " + allComments.size());

                // Filter comments for the selected club if necessary
                List<Commentaire> filteredComments;
                if (selectedClub == null || selectedClub.equals("all")) {
                    filteredComments = allComments;
                } else {
                    filteredComments = new ArrayList<>();
                    for (Commentaire comment : allComments) {
                        if (comment.getSondage() != null &&
                                comment.getSondage().getClub() != null &&
                                selectedClub.equals(comment.getSondage().getClub().getNomC())) {
                            filteredComments.add(comment);
                        }
                    }
                }

                System.out.println("Loaded " + filteredComments.size() + " comments for club: " + selectedClub);

                // Prepare data for the bar chart
                List<Map<String, Object>> chartData = statsService.getCommentsByMonth(filteredComments, selectedClub);
                System.out.println("Prepared " + chartData.size() + " data points for chart");

                // Debug: Print all data points
                for (Map<String, Object> dataPoint : chartData) {
                    System.out.println("Data point: " + dataPoint);
                }

                // Update chart on JavaFX thread
                Platform.runLater(() -> {
                    if (barChartVisualization != null) {
                        if (chartData.isEmpty()) {
                            insightsLoadingLabel.setText("Aucun commentaire trouvé pour ce club");
                            insightsLoadingLabel.setVisible(true);
                        } else {
                            insightsLoadingLabel.setVisible(false);
                            barChartVisualization.updateData(chartData, selectedClub);
                        }
                    } else {
                        System.err.println("Bar chart visualization is null!");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    insightsLoadingLabel.setText("Erreur lors du chargement des données: " + e.getMessage());
                    insightsLoadingLabel.setVisible(true);
                });
            }
        }).start();
    }

    /**
     * Afficher les détails d'un commentaire dans une fenêtre modale
     * 
     * @param commentaire Le commentaire à afficher
     */
    private void showCommentDetails(Commentaire commentaire) {
        // Créer une fenêtre de dialogue pour afficher les détails du commentaire
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails du commentaire");
        alert.setHeaderText(
                "Commentaire de " + commentaire.getUser().getFirstName() + " " + commentaire.getUser().getLastName());

        // Créer un content container avec formatage
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Ajouter le texte du commentaire
        Label contentLabel = new Label("Contenu:");
        contentLabel.setStyle("-fx-font-weight: bold;");

        TextArea commentText = new TextArea(commentaire.getContenuComment());
        commentText.setEditable(false);
        commentText.setWrapText(true);
        commentText.setPrefHeight(100);

        // Ajouter la date
        Label dateLabel = new Label("Date:");
        dateLabel.setStyle("-fx-font-weight: bold;");

        Label dateValue = new Label(commentaire.getDateComment().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        // Ajouter d'autres informations si disponibles
        Label sondageLabel = new Label("Sondage associé:");
        sondageLabel.setStyle("-fx-font-weight: bold;");

        Label sondageValue = new Label(
                commentaire.getSondage() != null ? commentaire.getSondage().getQuestion() : "Non disponible");

        content.getChildren().addAll(
                contentLabel, commentText,
                dateLabel, dateValue,
                sondageLabel, sondageValue);

        // Appliquer une classe CSS pour styliser
        alert.getDialogPane().getStyleClass().add("custom-alert");
        alert.getDialogPane().setContent(content);

        // Afficher la fenêtre de dialogue
        alert.showAndWait();
    }
}
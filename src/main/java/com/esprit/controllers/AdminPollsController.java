package com.esprit.controllers;

import com.esprit.models.Sondage;
import com.esprit.models.Club;
import com.esprit.models.User;
import com.esprit.services.SondageService;
import com.esprit.services.ClubService;
import com.esprit.services.UserService;
import com.esprit.services.ReponseService;
import com.esprit.utils.AlertUtils;
import com.esprit.utils.NavigationManager;

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
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AdminPollsController implements Initializable {

    // FXML components
    @FXML
    private Label totalPollsLabel;
    @FXML
    private Label totalVotesLabel;
    @FXML
    private Label activePollsLabel;
    @FXML
    private Label mostActiveClubLabel;
    @FXML
    private Label mostActiveClubPollsLabel;
    @FXML
    private ProgressBar activePollsProgressBar;
    @FXML
    private Label activePollsPercentLabel;
    @FXML
    private TextField searchInput;
    @FXML
    private TableView<Sondage> pollsTable;
    @FXML
    private TableColumn<Sondage, Integer> idColumn;
    @FXML
    private TableColumn<Sondage, String> questionColumn;
    @FXML
    private TableColumn<Sondage, String> optionsColumn;
    @FXML
    private TableColumn<Sondage, String> clubColumn;
    @FXML
    private TableColumn<Sondage, String> createdAtColumn;
    @FXML
    private TableColumn<Sondage, Void> actionsColumn;
    @FXML
    private HBox paginationContainer;
    @FXML
    private Pane toastContainer;
    @FXML
    private LineChart<String, Number> activityChart;
    @FXML
    private Button backButton;

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
    private BorderPane borderPane;

    // Services
    public SondageService sondageService;
    private ClubService clubService;
    private UserService userService;
    private ReponseService reponseService;

    // Controller state
    private ObservableList<Sondage> pollsList = FXCollections.observableArrayList();
    private User currentUser;
    private Scene previousScene;

    // Pagination
    private int currentPage = 1;
    private final int PAGE_SIZE = 3;
    private int totalPages = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            // Initialiser les services
            sondageService = new SondageService();
            clubService = new ClubService();
            userService = new UserService();
            reponseService = new ReponseService();

            System.out.println("AdminPollsController: Initializing...");

            // Configurer les colonnes du tableau
            setupTableColumns();

            // Configurer les événements
            setupEventHandlers();

            // Configurer les événements de navigation
            setupNavigationEvents();

            // Configurer les informations de l'administrateur
            setupAdminInfo();

            // Charger les données initiales
            loadData();

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.showError("Erreur", "Erreur lors de l'initialisation: " + e.getMessage());
        }
    }

    /**
     * Load all polls from the database and update the table
     */
    private void loadData() throws SQLException {
        System.out.println("AdminPollsController: Loading data...");

        // Charger tous les sondages
        List<Sondage> allPolls = sondageService.getAll();
        pollsList = FXCollections.observableArrayList(allPolls);

        System.out.println("Loaded " + pollsList.size() + " polls from database");

        // Calculer le nombre total de pages
        totalPages = (int) Math.ceil((double) pollsList.size() / PAGE_SIZE);

        // Appliquer la pagination
        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, pollsList.size());

        // Créer une sous-liste pour la page courante
        ObservableList<Sondage> currentPagePolls;
        if (fromIndex < pollsList.size()) {
            currentPagePolls = FXCollections.observableArrayList(
                    pollsList.subList(fromIndex, toIndex));
        } else {
            currentPagePolls = FXCollections.observableArrayList();
        }

        // Vérifier que la table n'est pas null
        if (pollsTable == null) {
            System.err.println("Error: pollsTable is null!");
        } else {
            // Mettre à jour le tableau
            pollsTable.setItems(currentPagePolls);
            System.out.println("Table updated with " + currentPagePolls.size() + " items for page " + currentPage);
        }

        // Calculer les statistiques
        calculateStats();

        // Mettre à jour le graphique d'activité
        updateActivityChart();

        // Configurer la pagination
        setupPagination();
    }

    /**
     * Setup the table columns
     */
    private void setupTableColumns() {
        // Configuration de la colonne ID
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setStyle("-fx-alignment: CENTER;");

        // Configuration de la colonne Question
        questionColumn.setCellValueFactory(new PropertyValueFactory<>("question"));
        questionColumn.setStyle("-fx-alignment: CENTER;");

        // Configuration de la colonne Options
        optionsColumn.setCellValueFactory(cellData -> {
            Sondage sondage = cellData.getValue();
            if (sondage != null && sondage.getChoix() != null) {
                String options = sondage.getChoix().stream()
                        .map(choix -> choix.getContenu())
                        .collect(Collectors.joining(", "));
                return new SimpleStringProperty(options);
            }
            return new SimpleStringProperty("");
        });
        optionsColumn.setStyle("-fx-alignment: CENTER;");

        // Configuration de la colonne Club
        clubColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue() != null && cellData.getValue().getClub() != null) {
                return new SimpleStringProperty(cellData.getValue().getClub().getNomC());
            }
            return new SimpleStringProperty("N/A");
        });
        clubColumn.setStyle("-fx-alignment: CENTER;");

        // Configuration de la colonne Date
        createdAtColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue() != null && cellData.getValue().getCreatedAt() != null) {
                LocalDateTime date = cellData.getValue().getCreatedAt();
                return new SimpleStringProperty(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
            return new SimpleStringProperty("N/A");
        });
        createdAtColumn.setStyle("-fx-alignment: CENTER;");

        // Configuration de la colonne Actions
        actionsColumn.setCellFactory(col -> new TableCell<Sondage, Void>() {
            // Créer des boutons avec des images au lieu de texte
            private final Button viewButton = new Button();
            private final Button deleteButton = new Button();
            private final HBox buttonsBox = new HBox(8);

            {
                // Créer les ImageView pour les icônes - Update paths to use common resources
                // folder
                ImageView eyeIcon = new ImageView(
                        new Image(getClass().getResourceAsStream("/com/esprit/images/eye.png")));
                eyeIcon.setFitHeight(20);
                eyeIcon.setFitWidth(20);

                ImageView trashIcon = new ImageView(
                        new Image(getClass().getResourceAsStream("/com/esprit/images/trash.png")));
                trashIcon.setFitHeight(20);
                trashIcon.setFitWidth(20);

                // Configurer le bouton de détails avec l'icône d'œil
                viewButton.setGraphic(eyeIcon);
                viewButton.getStyleClass().add("icon-button");
                viewButton.getStyleClass().add("view-button");
                viewButton.setTooltip(new Tooltip("Voir les détails"));

                // Configurer le bouton de suppression avec l'icône de poubelle
                deleteButton.setGraphic(trashIcon);
                deleteButton.getStyleClass().add("icon-button");
                deleteButton.getStyleClass().add("delete-icon-button");
                deleteButton.setTooltip(new Tooltip("Delete this poll"));

                // Ajouter la classe pour centrer les boutons
                getStyleClass().add("button-cell");

                // Configuration du conteneur des boutons
                buttonsBox.setAlignment(Pos.CENTER);
                buttonsBox.getChildren().addAll(viewButton, deleteButton);

                // Action pour le bouton Voir détails
                viewButton.setOnAction(event -> {
                    Sondage sondage = getTableView().getItems().get(getIndex());
                    viewPollDetails(sondage);
                });

                // Action pour le bouton Supprimer
                deleteButton.setOnAction(event -> {
                    Sondage sondage = getTableView().getItems().get(getIndex());
                    deletePoll(sondage);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttonsBox);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        actionsColumn.setStyle("-fx-alignment: CENTER;");
    }

    /**
     * Update the statistics cards at the top of the view
     */
    private void calculateStats() throws SQLException {
        System.out.println("AdminPollsController: Calculating stats...");

        // Total des sondages depuis le service
        List<Sondage> allPolls = sondageService.getAll();
        int totalPolls = allPolls.size();

        if (totalPollsLabel != null) {
            totalPollsLabel.setText(String.valueOf(totalPolls));
            System.out.println("Total polls: " + totalPolls);
        } else {
            System.err.println("Error: totalPollsLabel is null!");
        }

        // Total des votes
        int totalVotes = reponseService.getTotalVotesForAllPolls();
        if (totalVotesLabel != null) {
            totalVotesLabel.setText(String.valueOf(totalVotes));
            System.out.println("Total votes: " + totalVotes);
        } else {
            System.err.println("Error: totalVotesLabel is null!");
        }

        // Sondages actifs (ceux créés dans les 7 derniers jours)
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        long activePolls = allPolls.stream()
                .filter(poll -> poll.getCreatedAt() != null && poll.getCreatedAt().toLocalDate().isAfter(sevenDaysAgo))
                .count();

        if (activePollsLabel != null) {
            activePollsLabel.setText(String.valueOf(activePolls));
            System.out.println("Active polls: " + activePolls);
        } else {
            System.err.println("Error: activePollsLabel is null!");
        }

        // Pourcentage de sondages actifs
        double activePercentage = totalPolls > 0 ? (double) activePolls / totalPolls : 0;
        if (activePollsProgressBar != null) {
            activePollsProgressBar.setProgress(activePercentage);
        }
        if (activePollsPercentLabel != null) {
            activePollsPercentLabel.setText(String.format("%.0f%%", activePercentage * 100));
            System.out.println("Active polls percentage: " + String.format("%.0f%%", activePercentage * 100));
        }

        // Club le plus actif - grouper les sondages par club et trouver le club avec le
        // plus de sondages
        Map<Club, Long> pollsByClub = allPolls.stream()
                .filter(poll -> poll.getClub() != null)
                .collect(Collectors.groupingBy(Sondage::getClub, Collectors.counting()));

        Optional<Map.Entry<Club, Long>> mostActive = pollsByClub.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        if (mostActiveClubLabel != null && mostActiveClubPollsLabel != null) {
            if (mostActive.isPresent() && mostActive.get().getKey() != null) {
                Club club = mostActive.get().getKey();
                mostActiveClubLabel.setText(club.getNomC());
                mostActiveClubPollsLabel.setText(mostActive.get().getValue() + " polls");
                System.out.println(
                        "Most active club: " + club.getNomC() + " with " + mostActive.get().getValue() + " polls");
            } else {
                mostActiveClubLabel.setText("No active club");
                mostActiveClubPollsLabel.setText("0 polls");
                System.out.println("No active club found");
            }
        } else {
            System.err.println("Error: mostActiveClubLabel or mostActiveClubPollsLabel is null!");
        }
    }

    /**
     * Setup the activity chart showing polls and votes over time
     */
    private void updateActivityChart() {
        // Nettoyer le graphique
        activityChart.getData().clear();

        // Créer une série pour les sondages
        XYChart.Series<String, Number> pollsSeries = new XYChart.Series<>();
        pollsSeries.setName("Sondages");

        // Grouper les sondages par date
        Map<LocalDate, Long> pollsByDate = pollsList.stream()
                .collect(Collectors.groupingBy(
                        poll -> poll.getCreatedAt().toLocalDate(),
                        Collectors.counting()));

        // Ajouter les données au graphique
        pollsByDate.forEach((date, count) -> {
            pollsSeries.getData().add(new XYChart.Data<>(
                    date.format(DateTimeFormatter.ofPattern("dd/MM")),
                    count));
        });

        activityChart.getData().add(pollsSeries);
    }

    /**
     * Setup pagination controls
     */
    private void setupPagination() {
        paginationContainer.getChildren().clear();

        if (totalPages <= 1) {
            return;
        }

        // Bouton précédent
        Button prevButton = new Button("←");
        prevButton.getStyleClass().add(currentPage == 1 ? "pagination-button-disabled" : "pagination-button");
        prevButton.setDisable(currentPage == 1);
        prevButton.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                try {
                    loadData();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showToast("Erreur lors du chargement des données: " + ex.getMessage(), "error");
                }
            }
        });

        paginationContainer.getChildren().add(prevButton);

        // Pages numérotées
        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(startPage + 4, totalPages);

        for (int i = startPage; i <= endPage; i++) {
            Button pageButton = new Button(String.valueOf(i));
            pageButton.getStyleClass().addAll("pagination-button", i == currentPage ? "pagination-button-active" : "");
            final int pageNum = i;
            pageButton.setOnAction(e -> {
                currentPage = pageNum;
                try {
                    loadData();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showToast("Erreur lors du chargement des données: " + ex.getMessage(), "error");
                }
            });
            paginationContainer.getChildren().add(pageButton);
        }

        // Bouton suivant
        Button nextButton = new Button("→");
        nextButton.getStyleClass().add(currentPage == totalPages ? "pagination-button-disabled" : "pagination-button");
        nextButton.setDisable(currentPage == totalPages);
        nextButton.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                try {
                    loadData();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showToast("Erreur lors du chargement des données: " + ex.getMessage(), "error");
                }
            }
        });

        paginationContainer.getChildren().add(nextButton);
    }

    /**
     * Setup search functionality
     */
    private void setupEventHandlers() {
        // Recherche
        searchInput.textProperty().addListener((observable, oldValue, newValue) -> {
            filterPolls(newValue);
        });
    }

    private void filterPolls(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            try {
                loadData(); // Recharger toutes les données
            } catch (SQLException e) {
                e.printStackTrace();
                showToast("Erreur lors du chargement des données: " + e.getMessage(), "error");
            }
            return;
        }

        String lowerSearchText = searchText.toLowerCase();
        ObservableList<Sondage> filteredList = FXCollections.observableArrayList();

        for (Sondage poll : pollsList) {
            boolean matches = false;

            // Vérifier la question
            if (poll.getQuestion() != null && poll.getQuestion().toLowerCase().contains(lowerSearchText)) {
                matches = true;
            }

            // Vérifier le club
            if (poll.getClub() != null && poll.getClub().getNomC() != null &&
                    poll.getClub().getNomC().toLowerCase().contains(lowerSearchText)) {
                matches = true;
            }

            if (matches) {
                filteredList.add(poll);
            }
        }

        // Appliquer la pagination à la liste filtrée
        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, filteredList.size());

        ObservableList<Sondage> currentPagePolls;
        if (fromIndex < filteredList.size()) {
            currentPagePolls = FXCollections.observableArrayList(
                    filteredList.subList(fromIndex, toIndex));
        } else {
            currentPagePolls = FXCollections.observableArrayList();
        }

        pollsTable.setItems(currentPagePolls);

        // Mettre à jour la pagination
        totalPages = (int) Math.ceil((double) filteredList.size() / PAGE_SIZE);
        setupPagination();
    }

    /**
     * Open a new view displaying detailed information about a poll
     */
    private void viewPollDetails(Sondage sondage) {
        try {
            System.out.println("Opening poll details for: " + sondage.getId() + " - " + sondage.getQuestion());

            // Vérifier que le fichier FXML est trouvé
            URL fxmlUrl = getClass().getResource("/com/esprit/views/PollDetailsView.fxml");
            if (fxmlUrl == null) {
                System.err.println("FXML file not found: /com/esprit/views/PollDetailsView.fxml");
                showToast("Erreur: fichier FXML introuvable: /com/esprit/views/PollDetailsView.fxml", "error");
                return;
            }
            System.out.println("FXML URL found: " + fxmlUrl);

            // Vérifier que le fichier CSS est trouvé
            URL cssUrl = getClass().getResource("/com/esprit/styles/poll-details-style.css");
            if (cssUrl == null) {
                System.err.println("CSS file not found: /com/esprit/styles/poll-details-style.css");
                // On continue quand même, le CSS n'est pas critique
            } else {
                System.out.println("CSS URL found: " + cssUrl);
            }

            // Créer le loader avec l'URL du FXML
            FXMLLoader loader = new FXMLLoader(fxmlUrl);

            try {
                // Charger la vue
                Parent root = loader.load();

                // Configurer le contrôleur
                PollDetailsController controller = loader.getController();
                if (controller == null) {
                    System.err.println("Error: Controller not found in FXML loader!");
                    AlertUtils.showError("Controller Error", "Le contrôleur n'a pas pu être chargé.");
                    return;
                }

                controller.setSondage(sondage);
                // borderPane.setCenter(root);
                // Get current stage directly from a scene component
                Stage currentStage = (Stage) pollsTable.getScene().getWindow();
                double width = currentStage.getWidth();
                double height = currentStage.getHeight();

                // Créer la scène avec les dimensions de la fenêtre actuelle
                Scene scene = new Scene(root, width, height);

                // Ajouter le CSS
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                } else {
                    // Utiliser le CSS de l'administration comme fallback
                    URL adminCssUrl = getClass().getResource("/com/esprit/styles/admin-polls-style.css");
                    if (adminCssUrl != null) {
                        scene.getStylesheets().add(adminCssUrl.toExternalForm());
                    }
                }

                // Appliquer la scène directement au stage
                currentStage.setScene(scene);
                currentStage.show();

            } catch (Exception e) {
                System.err.println("Error during loading or showing the view: " + e.getMessage());
                e.printStackTrace();

                StringBuilder details = new StringBuilder();
                details.append("Erreur de chargement de la vue: ").append(e.getMessage()).append("\n\n");

                Throwable cause = e.getCause();
                if (cause != null) {
                    details.append("Caused by: ").append(cause.getMessage()).append("\n");
                    if (cause.getStackTrace().length > 0) {
                        details.append("at ").append(cause.getStackTrace()[0].toString()).append("\n");
                    }
                }

                details.append("\nInspection du contexte:\n");
                details.append("- Sondage ID: ").append(sondage.getId()).append("\n");
                details.append("- FXML URL: ").append(fxmlUrl).append("\n");
                details.append("- CSS URL: ").append(cssUrl != null ? cssUrl : "null").append("\n");

                showToast(details.toString(), "error");
                throw e;
            }

        } catch (Exception e) {
            e.printStackTrace();

            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append("Erreur lors de l'ouverture des détails: ").append(e.getMessage()).append("\n\n");

            Throwable cause = e.getCause();
            if (cause != null) {
                errorMessage.append("Cause: ").append(cause.getMessage()).append("\n");
                // Afficher les premières lignes de la stack trace
                StackTraceElement[] stack = cause.getStackTrace();
                if (stack.length > 0) {
                    for (int i = 0; i < Math.min(3, stack.length); i++) {
                        errorMessage.append("  at ").append(stack[i].toString()).append("\n");
                    }
                }
            }

            showToast(errorMessage.toString(), "error");
        }
    }

    /**
     * Show confirmation dialog for poll deletion
     */
    private void deletePoll(Sondage sondage) {
        try {
            // Create a custom confirmation dialog
            Alert confirmDialog = new Alert(Alert.AlertType.NONE);
            confirmDialog.setTitle("Confirmation");
            confirmDialog.setHeaderText("Delete poll?");
            confirmDialog.setContentText("This action will permanently delete the poll \"" + sondage.getQuestion() +
                    "\" along with all its associated votes and comments. This action is irreversible.");

            // Customize the dialog
            DialogPane dialogPane = confirmDialog.getDialogPane();

            // Add icon to header
            Label headerIcon = new Label("⚠️");
            headerIcon.setStyle("-fx-font-size: 24px; -fx-text-fill: #e74c3b;");

            HBox headerLayout = new HBox(10);
            headerLayout.setAlignment(Pos.CENTER_LEFT);

            Label headerLabel = new Label("Delete poll?");
            headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e74c3b;");

            headerLayout.getChildren().addAll(headerIcon, headerLabel);
            dialogPane.setHeader(headerLayout);

            // Style for content text
            Label contentLabel = new Label(confirmDialog.getContentText());
            contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
            contentLabel.setWrapText(true);
            contentLabel.setPrefWidth(400);
            dialogPane.setContent(contentLabel);

            // Add buttons
            ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType confirmButtonType = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
            confirmDialog.getButtonTypes().addAll(cancelButtonType, confirmButtonType);

            // Apply CSS styling
            dialogPane.getStylesheets()
                    .add(getClass().getResource("/com/esprit/styles/admin-polls-style.css").toExternalForm());
            dialogPane.getStyleClass().add("custom-alert");
            dialogPane.setPrefWidth(450);
            dialogPane.setPrefHeight(200);

            // Add some style to dialog background
            dialogPane.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 3);");

            // Get the confirm and cancel buttons
            Button confirmButton = (Button) dialogPane.lookupButton(confirmButtonType);
            Button cancelButton = (Button) dialogPane.lookupButton(cancelButtonType);

            if (confirmButton != null) {
                confirmButton.getStyleClass().add("delete-confirm-button");
                confirmButton.setStyle(
                        "-fx-background-color: #e74c3b; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5px; -fx-padding: 10px 20px;");

                // Add icon to delete button
                HBox btnContent = new HBox(5);
                btnContent.setAlignment(Pos.CENTER);

                Label iconLabel = new Label("🗑️");
                Label textLabel = new Label("Delete");
                textLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

                btnContent.getChildren().addAll(iconLabel, textLabel);
                confirmButton.setGraphic(btnContent);
                confirmButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            if (cancelButton != null) {
                cancelButton.getStyleClass().add("cancel-button");
                cancelButton.setStyle(
                        "-fx-background-color: #f8f9fa; -fx-text-fill: #333; -fx-border-color: #dee2e6; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-padding: 10px 20px;");
            }

            // Show dialog and process result
            if (confirmDialog.showAndWait().filter(response -> response == confirmButtonType).isPresent()) {
                try {
                    // First delete comments linked to this poll
                    sondageService.deleteCommentsByPollId(sondage.getId());

                    // Then delete responses linked to this poll
                    sondageService.deleteResponsesByPollId(sondage.getId());

                    // Then delete options linked to this poll
                    sondageService.deleteOptionsByPollId(sondage.getId());

                    // Finally, delete the poll itself
                    sondageService.delete(sondage.getId());

                    // Show success confirmation
                    showToast("✅ The poll was successfully deleted", "success");
                    loadData();
                } catch (SQLException e) {
                    e.printStackTrace();
                    showToast("Error while deleting: " + e.getMessage(), "error");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error while displaying confirmation dialog: " + e.getMessage(), "error");
        }
    }

    /**
     * Show a toast notification with a larger area for error messages
     */
    private void showToast(String message, String type) {
        try {
            // Afficher l'erreur dans une boîte de dialogue pour les erreurs
            if ("error".equals(type)) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setHeaderText("Une erreur s'est produite");

                // Créer une zone de texte pour afficher l'erreur complète
                TextArea textArea = new TextArea(message);
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setMaxWidth(Double.MAX_VALUE);
                textArea.setMaxHeight(Double.MAX_VALUE);
                textArea.setPrefWidth(550);
                textArea.setPrefHeight(200);

                alert.getDialogPane().setContent(textArea);
                alert.getDialogPane().setPrefWidth(600);
                alert.getDialogPane().setPrefHeight(300);

                alert.showAndWait();
                return;
            }

            // Pour les autres types, utiliser le toast existant
            Label toastLabel = (Label) ((HBox) toastContainer.getChildren().get(0)).getChildren().get(0);
            HBox toastHBox = (HBox) toastContainer.getChildren().get(0);

            if ("error".equals(type)) {
                toastHBox.setStyle("-fx-background-color: #dc3545;");
            } else {
                toastHBox.setStyle("-fx-background-color: #28a745;");
            }

            toastLabel.setText(message);
            toastContainer.setVisible(true);

            // Cache le toast après 3 secondes
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    javafx.application.Platform.runLater(() -> toastContainer.setVisible(false));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            // Si showToast échoue, afficher l'erreur dans la console
            System.err.println("Erreur lors de l'affichage du toast: " + e.getMessage());
            System.err.println("Message original: " + message);
        }
    }

    /**
     * Setup back button to return to previous scene
     */
    private void setupBackButton() {
        backButton.setOnAction(e -> {
            if (previousScene != null) {
                Stage stage = (Stage) backButton.getScene().getWindow();
                stage.setScene(previousScene);
            } else {
                // Navigate to default view if previous scene is not available
                try {
                    Parent root = FXMLLoader.load(getClass().getResource("/com/esprit/views/SondageView.fxml"));
                    Scene scene = new Scene(root);
                    Stage stage = (Stage) backButton.getScene().getWindow();
                    stage.setScene(scene);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    AlertUtils.showError("Error", "Failed to navigate back: " + ex.getMessage());
                }
            }
        });
    }

    /**
     * Set the previous scene to return to when back button is clicked
     */
    public void setPreviousScene(Scene scene) {
        this.previousScene = scene;
    }

    /**
     * Refresh all data in the view
     */
    public void refreshData() {
        try {
            loadData();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Configure les événements de navigation pour la sidebar
     */
    private void setupNavigationEvents() {
        // Navigation vers AdminCommentsView
        commentsManagementBtn.setOnAction(event -> {
            try {
                // Charger la vue des commentaires
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/AdminCommentsView.fxml"));
                Parent root = loader.load();

                // Obtenir le stage actuel directement depuis la scène du bouton
                Stage stage = (Stage) commentsManagementBtn.getScene().getWindow();

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
                showToast("Erreur lors de la navigation vers la gestion des commentaires: " + e.getMessage(), "error");
            }
        });

        // Le bouton pollsManagementBtn est déjà actif, pas besoin d'action

        // For Events Management, add submenu toggle similar to survey management
        eventManagementBtn.setOnAction(event -> {
            // Toggle the visibility of the submenu
            boolean isVisible = eventsSubMenu.isVisible();
            eventsSubMenu.setVisible(!isVisible);
            eventsSubMenu.setManaged(!isVisible);
        });

        // Pour le bouton principal Survey Management, on peut ajouter une animation
        // pour montrer/cacher le sous-menu
        surveyManagementBtn.setOnAction(event -> {
            // Toggle la visibilité du sous-menu
            boolean isVisible = surveySubMenu.isVisible();
            surveySubMenu.setVisible(!isVisible);
            surveySubMenu.setManaged(!isVisible);
        });

        // Configurer les autres boutons de navigation si nécessaire
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

        productOrdersBtn.setOnAction(e -> showToast("Fonctionnalité en développement: Produits & Commandes", "info"));
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
}
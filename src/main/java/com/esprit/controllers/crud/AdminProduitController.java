package com.esprit.controllers.crud;

import com.esprit.models.Club;
import com.esprit.models.Produit;
import com.esprit.services.ClubService;
import com.esprit.services.ProduitService;
import com.esprit.utils.AlertUtils;
import com.esprit.ProduitApp;
import com.esprit.utils.DataSource;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AdminProduitController implements Initializable {

    // Table components
    @FXML
    private TableView<Produit> tableView;
    @FXML
    private TableColumn<Produit, Integer> colId;
    @FXML
    private TableColumn<Produit, String> colNom;
    @FXML
    private TableColumn<Produit, Float> colPrix;
    @FXML
    private TableColumn<Produit, String> colQuantity;
    @FXML
    private TableColumn<Produit, String> colDescription;
    @FXML
    private TableColumn<Produit, Club> colClub;
    @FXML
    private TableColumn<Produit, String> colCreatedAt;
    @FXML
    private TableColumn<Produit, Void> colActions;

    // Filter components
    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> filterClubComboBox;

    // Statistics labels
    @FXML
    private Label totalProductsLabel;
    @FXML
    private Label inStockProductsLabel;
    @FXML
    private Label outOfStockProductsLabel;
    @FXML
    private Label popularClubLabel;
    @FXML
    private Label popularClubProductsLabel;
    @FXML
    private VBox popularClubCard;
    @FXML
    private StackPane chartContainer;

    // Containers
    @FXML
    private HBox paginationContainer;
    @FXML
    private VBox noProductsContainer;
    @FXML
    private Pane toastContainer;

    // Sidebar navigation
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
    private Button btnAddProduct;
    @FXML
    private TextField txtImage;
    private TextField txtDescription;
    private TextField txtNom;
    private TextField txtPrix;
    private TextField txtQuantity;
    @FXML
    private ComboBox<Club> comboClub;

    // Services
    private final ProduitService produitService;
    private final ClubService clubService;

    // Data lists
    private ObservableList<Produit> produitList;
    private FilteredList<Produit> filteredList;

    // Pagination
    private int currentPage = 1;
    private static final int ITEMS_PER_PAGE = 2;
    private int totalPages = 1;

    // Formatting
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE);

    // Selected filter
    private String selectedClub = "all";

    // Chart fields
    private BarChart<String, Number> barChart;
    private boolean isChartVisible = false;

    public AdminProduitController() {
        this.produitService = ProduitService.getInstance();
        this.clubService = ClubService.getInstance();
        this.produitList = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            // Check database connection before proceeding
            if (!DataSource.getInstance().isConnected()) {
                showDatabaseConnectionError();
                return; // Stop initialization if no connection
            }

            // Setup table columns
            setupTableColumns();

            // Setup filters
            setupFilters();

            // Load products
            loadAllProduits();

            // Calculate statistics
            calculateStats();

            // Setup pagination
            setupPagination();

            // Setup navigation
            setupNavigationEvents();

            // Setup admin info
            setupAdminInfo();

            // Setup popular club card for chart
            setupPopularClubCard();

            // Add table styling
            tableView.getStyleClass().add("product-table");
            try {
                URL styleResource = getClass().getResource("/com/esprit/styles/admin-style.css");
                if (styleResource != null) {
                    tableView.getStylesheets().add(styleResource.toExternalForm());
                }
            } catch (Exception e) {
                System.err.println("Could not load stylesheet: " + e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("Erreur d'initialisation", "Erreur lors du chargement de l'interface",
                    "Une erreur est survenue: " + e.getMessage());
        }
    }

    private void setupPopularClubCard() {
        // Initialize the bar chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Club");
        yAxis.setLabel("Nombre de Produits Vendus");
        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Clubs les plus populaires");
        barChart.setPrefHeight(300);
        barChart.setPrefWidth(600);

        // Add the chart to the chartContainer
        chartContainer.getChildren().add(barChart);

        // Set up the click handler for the popular club card
        popularClubCard.setOnMouseClicked(event -> {
            if (!isChartVisible) {
                // Load the data and show the chart
                loadPopularClubsChart();
                chartContainer.setVisible(true);
                chartContainer.setManaged(true);
                isChartVisible = true;
            } else {
                // Hide the chart
                chartContainer.setVisible(false);
                chartContainer.setManaged(false);
                isChartVisible = false;
            }
        });
    }

    private void loadPopularClubsChart() {
        try {
            // Clear any existing data in the chart
            barChart.getData().clear();

            // Fetch the most popular clubs
            List<Object[]> topClubs = produitService.getTopClubsByProducts();

            if (topClubs.isEmpty()) {
                showToast("Aucune donnée de vente disponible pour les clubs", "info");
                return;
            }

            // Create a series for the chart
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Ventes");

            // Add data to the series
            for (Object[] club : topClubs) {
                String clubName = (String) club[0];
                int totalSales = (int) club[1];
                series.getData().add(new XYChart.Data<>(clubName, totalSales));
            }

            // Add the series to the chart
            barChart.getData().add(series);

        } catch (SQLException e) {
            e.printStackTrace();
            showToast("Erreur lors du chargement des clubs populaires: " + e.getMessage(), "error");
        }
    }

    private void showDatabaseConnectionError() {
        // Clear the table and show message
        if (tableView != null) {
            tableView.setPlaceholder(new Label("Impossible de se connecter à la base de données"));
        }

        // Show error alert
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur de connexion");
        alert.setHeaderText("Impossible de se connecter à la base de données");
        alert.setContentText("Vérifiez que le serveur MySQL est démarré et que les paramètres de connexion sont corrects.\n\n" +
                "URL: " + DataSource.getInstance().getUrl() + "\n" +
                "Utilisateur: " + DataSource.getInstance().getUser());

        // Create ButtonType for retry
        ButtonType retryButton = new ButtonType("Réessayer");
        ButtonType exitButton = new ButtonType("Quitter", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(retryButton, exitButton);

        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == retryButton) {
                // Retry connection and re-initialize
                if (DataSource.getInstance().isConnected()) {
                    initialize(null, null);
                } else {
                    showDatabaseConnectionError(); // Show error again if still can't connect
                }
            } else if (buttonType == exitButton) {
                Platform.exit(); // Close the application
            }
        });

        // Disable controls that require database
        if (btnAddProduct != null) btnAddProduct.setDisable(true);
        if (filterClubComboBox != null) filterClubComboBox.setDisable(true);
    }

    /**
     * Navigate to the product catalog view
     */
    @FXML
    public void goToCatalog() {
        try {
            if (ProduitApp.getPrimaryStage() != null) {
                ProduitApp.navigateTo("/com/esprit/views/produit/ProduitView.fxml");
            } else {
                AlertUtils.showError("Erreur", "Navigation impossible",
                        "Impossible d'accéder à la fenêtre principale.");
            }
        } catch (Exception e) {
            AlertUtils.showError("Erreur", "Navigation impossible", e.getMessage());
        }
    }

    /**
     * Opens a file chooser dialog to select an image file
     */
    @FXML
    public void browseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select an Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));

        File selectedFile = fileChooser.showOpenDialog(txtImage.getScene().getWindow());
        if (selectedFile != null) {
            txtImage.setText(selectedFile.getAbsolutePath());
        }
    }

    /**
     * Save a new product
     */
    @FXML
    public void saveProduit() {
        // This is a placeholder to make the FXML happy
        // The actual implementation would be added later
        AlertUtils.showInfo("Info", "Save Product", "This functionality will be implemented soon.");
    }

    /**
     * Update an existing product
     */
    @FXML
    public void updateProduit() {
        // This is a placeholder to make the FXML happy
        // The actual implementation would be added later
        AlertUtils.showInfo("Info", "Update Product", "This functionality will be implemented soon.");
    }

    /**
     * Configure navigation events for the sidebar
     */
    private void setupNavigationEvents() {
        // Survey Management button - toggle submenu
        if (surveyManagementBtn != null) {
            surveyManagementBtn.setOnAction(event -> {
                boolean isVisible = surveySubMenu.isVisible();
                surveySubMenu.setVisible(!isVisible);
                surveySubMenu.setManaged(!isVisible);
            });
        }

        // Polls Management button - navigate to polls view
        if (pollsManagementBtn != null) {
            pollsManagementBtn.setOnAction(event -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/AdminPollsView.fxml"));
                    Parent root = loader.load();

                    Stage stage = (Stage) pollsManagementBtn.getScene().getWindow();
                    Scene scene = new Scene(root);

                    scene.getStylesheets()
                            .add(getClass().getResource("/com/esprit/styles/admin-polls-style.css").toExternalForm());
                    scene.getStylesheets()
                            .add(getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm());

                    stage.setScene(scene);
                    stage.setMaximized(true);
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                    showToast("Erreur lors de la navigation : " + e.getMessage(), "error");
                }
            });
        }

        // Comments Management button - navigate to comments view
        if (commentsManagementBtn != null) {
            commentsManagementBtn.setOnAction(event -> {
                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/com/esprit/views/AdminCommentsView.fxml"));
                    Parent root = loader.load();

                    Stage stage = (Stage) commentsManagementBtn.getScene().getWindow();
                    Scene scene = new Scene(root);

                    scene.getStylesheets()
                            .add(getClass().getResource("/com/esprit/styles/admin-polls-style.css").toExternalForm());
                    scene.getStylesheets()
                            .add(getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm());

                    stage.setScene(scene);
                    stage.setMaximized(true);
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                    showToast("Erreur lors de la navigation : " + e.getMessage(), "error");
                }
            });
        }

        // User Management button - navigate to admin dashboard
        if (userManagementBtn != null) {
            userManagementBtn.setOnAction(event -> {
                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/com/esprit/views/admin_dashboard.fxml"));
                    Parent root = loader.load();

                    Stage stage = (Stage) userManagementBtn.getScene().getWindow();
                    Scene scene = new Scene(root);

                    scene.getStylesheets()
                            .add(getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm());

                    stage.setScene(scene);
                    stage.setMaximized(true);
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                    showToast("Erreur lors de la navigation : " + e.getMessage(), "error");
                }
            });
        }

        // Configure placeholders for other navigation buttons
        if (clubManagementBtn != null) {
            clubManagementBtn.setOnAction(e -> showToast("Fonction en développement : Gestion des clubs", "info"));
        }

        if (eventManagementBtn != null) {
            eventManagementBtn
                    .setOnAction(e -> showToast("Fonction en développement : Gestion des événements", "info"));
        }

        if (competitionBtn != null) {
            competitionBtn.setOnAction(e -> showToast("Fonction en développement : Compétitions", "info"));
        }

        if (profileBtn != null) {
            profileBtn.setOnAction(e -> showToast("Fonction en développement : Profil", "info"));
        }

        if (logoutBtn != null) {
            logoutBtn.setOnAction(e -> handleLogout());
        }
    }

    /**
     * Handle user logout
     */
    private void handleLogout() {
        // Create confirmation dialog
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Déconnexion");
        confirmDialog.setHeaderText("Êtes-vous sûr de vouloir vous déconnecter ?");
        confirmDialog.setContentText("Toutes les modifications non enregistrées seront perdues.");

        // Show dialog and process result
        if (confirmDialog.showAndWait().filter(response -> response == ButtonType.OK).isPresent()) {
            try {
                // Navigate to login page
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/login.fxml"));
                Parent root = loader.load();

                Stage stage = (Stage) logoutBtn.getScene().getWindow();
                Scene scene = new Scene(root);

                stage.setScene(scene);
                stage.setMaximized(false);
                stage.setWidth(900);
                stage.setHeight(600);
                stage.centerOnScreen();
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showToast("Erreur lors de la déconnexion : " + e.getMessage(), "error");
            }
        }
    }

    /**
     * Setup admin information
     */
    private void setupAdminInfo() {
        if (adminNameLabel != null) {
            // In a real application, you would get the current user from a session manager
            adminNameLabel.setText("Admin User");
        }
    }

    /**
     * Method to refresh data
     */
    public void refreshData() {
        loadAllProduits();
        calculateStats();
    }

    /**
     * Reset the form fields
     */
    @FXML
    public void resetForm() {
        // This is a placeholder to make the FXML happy
        // The actual implementation would be added later
        if (txtNom != null)
            txtNom.clear();
        if (txtPrix != null)
            txtPrix.clear();
        if (txtQuantity != null)
            txtQuantity.clear();
        if (txtDescription != null)
            txtDescription.clear();
        if (txtImage != null)
            txtImage.clear();
        if (comboClub != null)
            comboClub.setValue(null);
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nomProd"));

        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colPrix.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Float price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(price));
                }
            }
        });

        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colQuantity.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String qty, boolean empty) {
                super.updateItem(qty, empty);
                if (empty || qty == null) {
                    setText(null);
                    setStyle("");
                } else {
                    try {
                        int quantity = Integer.parseInt(qty);
                        setText(qty);
                        if (quantity <= 0) {
                            setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;"); // Red for out of stock
                        } else if (quantity < 5) {
                            setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;"); // Orange for low stock
                        } else {
                            setStyle("-fx-text-fill: #4CAF50;"); // Green for in stock
                        }
                    } catch (NumberFormatException e) {
                        setText(qty);
                        setStyle("");
                    }
                }
            }
        });

        colDescription.setCellValueFactory(new PropertyValueFactory<>("descProd"));
        colDescription.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String description, boolean empty) {
                super.updateItem(description, empty);
                if (empty || description == null) {
                    setText(null);
                } else {
                    // Truncate long descriptions and add tooltip
                    setText(description.length() > 50 ? description.substring(0, 47) + "..." : description);
                    setTooltip(new Tooltip(description));

                    // Add click handler for long descriptions
                    if (description.length() > 50) {
                        setStyle("-fx-cursor: hand; -fx-text-fill: #0066cc; -fx-underline: true;");

                        this.setOnMouseClicked(event -> {
                            // Create dialog to show full description
                            Alert dialog = new Alert(Alert.AlertType.INFORMATION);
                            dialog.setTitle("Description complète");
                            dialog.setHeaderText("Description du produit");

                            // Text area for scrollable content
                            TextArea textArea = new TextArea(description);
                            textArea.setEditable(false);
                            textArea.setWrapText(true);
                            textArea.setPrefWidth(480);
                            textArea.setPrefHeight(200);

                            dialog.getDialogPane().setContent(textArea);
                            dialog.getDialogPane().getStylesheets()
                                    .add(getClass().getResource("/com/esprit/styles/admin-style.css").toExternalForm());

                            dialog.getButtonTypes().setAll(ButtonType.CLOSE);
                            dialog.showAndWait();
                        });
                    } else {
                        setStyle("-fx-text-fill: #333333;");
                        this.setOnMouseClicked(null);
                    }
                }
            }
        });

        colClub.setCellValueFactory(new PropertyValueFactory<>("club"));
        colClub.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Club club, boolean empty) {
                super.updateItem(club, empty);
                if (empty || club == null) {
                    setText(null);
                } else {
                    setText(club.getNom());
                }
            }
        });

        colCreatedAt.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getCreatedAt();
            if (date == null) {
                return javafx.beans.binding.Bindings.createStringBinding(() -> "N/A");
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                return javafx.beans.binding.Bindings.createStringBinding(() -> formatter.format(date));
            }
        });

        setupActionsColumn();
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(tc -> new TableCell<>() {
            private final Button editButton = new Button("Modifier");
            private final Button deleteButton = new Button("Supprimer");
            private final HBox container = new HBox(5);

            {
                // Style edit button
                editButton.setStyle(
                        "-fx-background-color: #03DAC5; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 3;");
                editButton.setMinWidth(80);

                // Style delete button
                deleteButton.setStyle(
                        "-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 3;");
                deleteButton.setMinWidth(80);

                // Add hover effects
                editButton.setOnMouseEntered(e -> editButton.setStyle(
                        "-fx-background-color: #00a896; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 3;"));
                editButton.setOnMouseExited(e -> editButton.setStyle(
                        "-fx-background-color: #03DAC5; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 3;"));

                deleteButton.setOnMouseEntered(e -> deleteButton.setStyle(
                        "-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 3;"));
                deleteButton.setOnMouseExited(e -> deleteButton.setStyle(
                        "-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 3;"));

                // Set up actions
                editButton.setOnAction(event -> {
                    Produit produit = getTableView().getItems().get(getIndex());
                    showEditProductDialog(produit);
                });

                deleteButton.setOnAction(event -> {
                    Produit produit = getTableView().getItems().get(getIndex());
                    handleDelete(produit);
                });

                // Set up container
                container.setAlignment(Pos.CENTER);
                container.getChildren().addAll(editButton, deleteButton);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void setupFilters() {
        filteredList = new FilteredList<>(produitList);

        if (txtSearch != null) {
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
                if (filteredList != null) {
                    filteredList.setPredicate(produit -> {
                        if (newValue == null || newValue.isEmpty()) {
                            return true;
                        }
                        String lowerCaseFilter = newValue.toLowerCase();
                        return produit.getNomProd().toLowerCase().contains(lowerCaseFilter) ||
                                produit.getDescProd().toLowerCase().contains(lowerCaseFilter) ||
                                (produit.getClub() != null
                                        && produit.getClub().getNom().toLowerCase().contains(lowerCaseFilter));
                    });
                    updatePagination();
                }
            });
        }

        try {
            List<Club> clubs = clubService.getAll();
            ObservableList<String> clubNames = FXCollections.observableArrayList();
            clubNames.add("Tous les Clubs");

            clubs.forEach(club -> clubNames.add(club.getNom()));

            if (filterClubComboBox != null) {
                filterClubComboBox.setItems(clubNames);
                filterClubComboBox.setValue("Tous les Clubs");

                filterClubComboBox.setCellFactory(listView -> new ListCell<String>() {
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
                            if ("Tous les Clubs".equals(club)) {
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

                filterClubComboBox.setButtonCell(new ListCell<String>() {
                    @Override
                    protected void updateItem(String club, boolean empty) {
                        super.updateItem(club, empty);

                        if (empty || club == null) {
                            setText("Tous les Clubs");
                            setGraphic(null);
                        } else {
                            HBox cellBox = new HBox(10);
                            cellBox.setAlignment(Pos.CENTER_LEFT);

                            Label icon = new Label();
                            if ("Tous les Clubs".equals(club)) {
                                icon.setText("🌐");
                            } else {
                                icon.setText("🏢");
                            }
                            icon.setStyle("-fx-font-size: 14px;");

                            Label clubLabel = new Label(club);
                            clubLabel.setStyle("-fx-font-size: 14px;");

                            cellBox.getChildren().addAll(icon, clubLabel);

                            setGraphic(cellBox);
                            setText(null);
                        }
                    }
                });

                filterClubComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                    if (filteredList != null) {
                        filterClubComboBox.setDisable(true); // Temporarily disable to show processing

                        // Convert "Tous les Clubs" to internal value "all"
                        selectedClub = "Tous les Clubs".equals(newValue) ? "all" : newValue;

                        // Apply filter
                        filteredList.setPredicate(produit -> {
                            // First check the search text
                            String searchText = txtSearch.getText();
                            boolean matchesSearch = searchText == null || searchText.isEmpty() ||
                                    produit.getNomProd().toLowerCase().contains(searchText.toLowerCase()) ||
                                    produit.getDescProd().toLowerCase().contains(searchText.toLowerCase());

                            // Then check the club filter
                            boolean matchesClub = "all".equals(selectedClub) ||
                                    (produit.getClub() != null && produit.getClub().getNom().equals(selectedClub));

                            return matchesSearch && matchesClub;
                        });

                        // Reset to page 1 when changing filter
                        currentPage = 1;
                        updatePagination();

                        // Re-enable after short delay
                        new Thread(() -> {
                            try {
                                Thread.sleep(300);
                                Platform.runLater(() -> filterClubComboBox.setDisable(false));
                            } catch (InterruptedException e) {
                                Platform.runLater(() -> filterClubComboBox.setDisable(false));
                                e.printStackTrace();
                            }
                        }).start();

                        // Show toast with filter info
                        showToast(
                                "Filtré par "
                                        + ("all".equals(selectedClub) ? "tous les clubs" : "club: " + selectedClub),
                                "info");
                    }
                });
            }
        } catch (SQLException e) {
            AlertUtils.showError("Erreur", "Erreur lors du chargement des clubs", e.getMessage());
        }
    }

    @FXML
    private void searchProducts() {
        setupFilters();
    }

    private void loadAllProduits() {
        try {
            // Show loading placeholder
            tableView.setPlaceholder(new Label("Chargement des produits..."));

            List<Produit> produits = produitService.getAll();
            produitList.clear();
            produitList.addAll(produits);

            filteredList = new FilteredList<>(produitList);

            if (produitList.isEmpty()) {
                if (noProductsContainer != null) {
                    noProductsContainer.setVisible(true);
                }
                if (tableView != null) {
                    tableView.setVisible(false);
                }
                if (paginationContainer != null) {
                    paginationContainer.setVisible(false);
                }
            } else {
                if (noProductsContainer != null) {
                    noProductsContainer.setVisible(false);
                }
                if (tableView != null) {
                    tableView.setVisible(true);
                }
                if (paginationContainer != null) {
                    paginationContainer.setVisible(true);
                }

                // Update pagination with the new data
                updatePagination();
            }
        } catch (SQLException e) {
            AlertUtils.showError("Erreur", "Erreur lors du chargement des produits", e.getMessage());
            // Set empty placeholder message
            tableView.setPlaceholder(new Label("Erreur lors du chargement des produits"));
        }
    }

    private void updatePagination() {
        if (paginationContainer != null) {
            int itemCount = filteredList.size();
            totalPages = (itemCount + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE; // Ceiling division

            if (totalPages < 1) {
                totalPages = 1;
            }

            if (currentPage > totalPages) {
                currentPage = totalPages;
            }

            // Update table content based on current page
            loadCurrentPage();

            // Update pagination controls
            setupPagination();
        }
    }

    private void loadCurrentPage() {
        int fromIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, filteredList.size());

        ObservableList<Produit> currentPageList;
        if (fromIndex < toIndex) {
            currentPageList = FXCollections.observableArrayList(filteredList.subList(fromIndex, toIndex));
        } else {
            currentPageList = FXCollections.observableArrayList();
        }

        tableView.setItems(currentPageList);
    }

    private void setupPagination() {
        if (paginationContainer == null)
            return;

        paginationContainer.getChildren().clear();

        if (totalPages <= 1) {
            // Hide pagination if there's only one page
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
                loadCurrentPage();
                setupPagination(); // Refresh pagination buttons
            }
        });

        paginationContainer.getChildren().add(prevButton);

        // Pages numbered buttons - show max 5 pages around current page
        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(startPage + 4, totalPages);

        for (int i = startPage; i <= endPage; i++) {
            Button pageButton = new Button(String.valueOf(i));
            pageButton.getStyleClass().addAll("pagination-button");

            // Add active class for current page and style
            if (i == currentPage) {
                pageButton.getStyleClass().add("pagination-button-active");
                pageButton.setStyle("-fx-font-weight: bold; -fx-background-color: #6200EE; -fx-text-fill: white;");
            } else {
                pageButton.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #333333;");
            }

            final int pageNum = i;
            pageButton.setOnAction(e -> {
                currentPage = pageNum;
                loadCurrentPage();
                setupPagination(); // Refresh pagination buttons
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
                loadCurrentPage();
                setupPagination(); // Refresh pagination buttons
            }
        });

        paginationContainer.getChildren().add(nextButton);

        // Add page count information
        Label pageInfoLabel = new Label(String.format("Page %d sur %d", currentPage, totalPages));
        pageInfoLabel.setStyle("-fx-text-fill: #6c757d; -fx-padding: 5 0 0 10;");
        paginationContainer.getChildren().add(pageInfoLabel);
    }

    private void handleDelete(Produit produit) {
        // Create a custom confirmation dialog
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Supprimer le Produit");
        confirmDialog.setHeaderText("Êtes-vous sûr de vouloir supprimer ce produit ?");
        confirmDialog.setContentText("Cette action ne peut pas être annulée.");

        // Customize the dialog
        DialogPane dialogPane = confirmDialog.getDialogPane();
        try {
            URL styleResource = getClass().getResource("/com/esprit/styles/admin-style.css");
            if (styleResource != null) {
                dialogPane.getStylesheets().add(styleResource.toExternalForm());
            }
        } catch (Exception e) {
            System.err.println("Could not load stylesheet: " + e.getMessage());
        }
        dialogPane.getStyleClass().add("custom-alert");

        // Get the confirm and cancel buttons
        Button confirmButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);

        if (confirmButton != null) {
            confirmButton.setText("Supprimer");
            confirmButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        }

        if (cancelButton != null) {
            cancelButton.setText("Annuler");
        }

        // Show dialog and process result
        if (confirmDialog.showAndWait().filter(response -> response == ButtonType.OK).isPresent()) {
            try {
                produitService.deleteProduit(produit.getId());
                showToast("Produit supprimé avec succès", "success");
                loadAllProduits();
                calculateStats();
            } catch (SQLException e) {
                showToast("Erreur lors de la suppression : " + e.getMessage(), "error");
                e.printStackTrace();
            }
        }
    }

    private void calculateStats() {
        try {
            // Get all products
            List<Produit> allProducts = produitService.getAll();

            // Total products count
            int totalProducts = allProducts.size();
            totalProductsLabel.setText(String.valueOf(totalProducts));

            // Count products in stock
            int inStockProducts = 0;
            int outOfStockProducts = 0;

            // Map to count products by club
            Map<String, Integer> clubProductCount = new HashMap<>();

            for (Produit produit : allProducts) {
                // Count stock status
                try {
                    int quantity = Integer.parseInt(produit.getQuantity());
                    if (quantity > 0) {
                        inStockProducts++;
                    } else {
                        outOfStockProducts++;
                    }
                } catch (NumberFormatException e) {
                    // Skip if quantity is not a number
                    inStockProducts++;
                }

                // Count products by club
                if (produit.getClub() != null) {
                    String clubName = produit.getClub().getNom();
                    clubProductCount.put(clubName, clubProductCount.getOrDefault(clubName, 0) + 1);
                }
            }

            // Update in-stock and out-of-stock labels
            inStockProductsLabel.setText(String.valueOf(inStockProducts));
            outOfStockProductsLabel.setText(String.valueOf(outOfStockProducts));

            // Find the club with the most products
            if (!clubProductCount.isEmpty()) {
                String mostPopularClub = "";
                int maxProducts = 0;

                for (Map.Entry<String, Integer> entry : clubProductCount.entrySet()) {
                    if (entry.getValue() > maxProducts) {
                        maxProducts = entry.getValue();
                        mostPopularClub = entry.getKey();
                    }
                }

                popularClubLabel.setText(mostPopularClub);
                popularClubProductsLabel.setText(maxProducts + " produit" + (maxProducts > 1 ? "s" : ""));
            } else {
                popularClubLabel.setText("N/A");
                popularClubProductsLabel.setText("0 produit");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Erreur lors du calcul des statistiques : " + e.getMessage());
        }
    }

    /**
     * Opens a dialog to add a new product
     */
    @FXML
    public void showAddProductDialog() {
        // Create dialog
        Dialog<Produit> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un Produit");
        dialog.setHeaderText("Saisir les informations du produit");

        // Set button types
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form grid
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 20, 10, 10));

        // Create form fields
        TextField nomField = new TextField();
        nomField.setPromptText("Nom du produit");

        TextField prixField = new TextField();
        prixField.setPromptText("Prix (ex: 29.99)");

        TextField quantityField = new TextField();
        quantityField.setPromptText("Quantité disponible");

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Description du produit");
        descriptionArea.setPrefRowCount(4);

        TextField imageField = new TextField();
        imageField.setPromptText("Chemin de l'image");

        Button browseButton = new Button("Parcourir...");

        // Create club combobox
        ComboBox<Club> clubComboBox = new ComboBox<>();
        try {
            List<Club> clubs = clubService.getAll();
            clubComboBox.setItems(FXCollections.observableArrayList(clubs));
            clubComboBox.setCellFactory(cell -> new ListCell<Club>() {
                @Override
                protected void updateItem(Club club, boolean empty) {
                    super.updateItem(club, empty);
                    setText(empty || club == null ? "" : club.getNom());
                }
            });
            clubComboBox.setButtonCell(new ListCell<Club>() {
                @Override
                protected void updateItem(Club club, boolean empty) {
                    super.updateItem(club, empty);
                    setText(empty || club == null ? "" : club.getNom());
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.showError("Erreur", "Erreur lors du chargement des clubs", e.getMessage());
        }

        HBox imageBox = new HBox(10);
        imageBox.setAlignment(Pos.CENTER_LEFT);
        imageBox.getChildren().addAll(imageField, browseButton);

        // Add components to grid
        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Prix (€):"), 0, 1);
        grid.add(prixField, 1, 1);
        grid.add(new Label("Quantité:"), 0, 2);
        grid.add(quantityField, 1, 2);
        grid.add(new Label("Club:"), 0, 3);
        grid.add(clubComboBox, 1, 3);
        grid.add(new Label("Description:"), 0, 4);
        grid.add(descriptionArea, 1, 4);
        grid.add(new Label("Image:"), 0, 5);
        grid.add(imageBox, 1, 5);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the name field
        Platform.runLater(nomField::requestFocus);

        // Handle file chooser
        browseButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Sélectionner une image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));

            File selectedFile = fileChooser.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (selectedFile != null) {
                imageField.setText(selectedFile.getAbsolutePath());
            }
        });

        // Convert dialog result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    // Validate inputs
                    if (nomField.getText().isEmpty()) {
                        AlertUtils.showError("Erreur", "Nom invalide", "Le nom du produit est obligatoire.");
                        return null;
                    }

                    // Create product
                    Produit produit = new Produit();
                    produit.setNomProd(nomField.getText());

                    // Validate and convert price
                    try {
                        float prix = Float.parseFloat(prixField.getText().replace(',', '.'));
                        produit.setPrix(prix);
                    } catch (NumberFormatException ex) {
                        AlertUtils.showError("Erreur", "Prix invalide",
                                "Veuillez entrer un prix valide (exemple: 29.99)");
                        return null;
                    }

                    produit.setQuantity(quantityField.getText());
                    produit.setDescProd(descriptionArea.getText());
                    produit.setImgProd(imageField.getText());
                    produit.setCreatedAt(LocalDateTime.now());
                    produit.setClub(clubComboBox.getValue());

                    return produit;
                } catch (Exception ex) {
                    AlertUtils.showError("Erreur", "Erreur lors de la création du produit", ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        // Show dialog and process result
        Optional<Produit> result = dialog.showAndWait();

        result.ifPresent(produit -> {
            try {
                // Save the product
                produitService.insertProduit(produit);

                // Refresh the product list and statistics
                loadAllProduits();
                calculateStats();

                // Show success message
                showToast("Produit ajouté avec succès", "success");
            } catch (SQLException ex) {
                AlertUtils.showError("Erreur", "Erreur lors de l'enregistrement du produit", ex.getMessage());
            }
        });
    }

    /**
     * Show dialog to edit an existing product
     */
    private void showEditProductDialog(Produit produit) {
        if (produit == null)
            return;

        // Create dialog
        Dialog<Produit> dialog = new Dialog<>();
        dialog.setTitle("Modifier un Produit");
        dialog.setHeaderText("Modifier les informations du produit");

        // Set button types
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form grid
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 20, 10, 10));

        // Create form fields with existing values
        TextField nomField = new TextField(produit.getNomProd());

        TextField prixField = new TextField(String.valueOf(produit.getPrix()).replace('.', ','));

        TextField quantityField = new TextField(produit.getQuantity());

        TextArea descriptionArea = new TextArea(produit.getDescProd());
        descriptionArea.setPrefRowCount(4);

        TextField imageField = new TextField(produit.getImgProd());

        Button browseButton = new Button("Parcourir...");

        // Create club combobox
        ComboBox<Club> clubComboBox = new ComboBox<>();
        try {
            List<Club> clubs = clubService.getAll();
            clubComboBox.setItems(FXCollections.observableArrayList(clubs));
            clubComboBox.setCellFactory(cell -> new ListCell<Club>() {
                @Override
                protected void updateItem(Club club, boolean empty) {
                    super.updateItem(club, empty);
                    setText(empty || club == null ? "" : club.getNom());
                }
            });
            clubComboBox.setButtonCell(new ListCell<Club>() {
                @Override
                protected void updateItem(Club club, boolean empty) {
                    super.updateItem(club, empty);
                    setText(empty || club == null ? "" : club.getNom());
                }
            });

            // Select the current club
            clubComboBox.setValue(produit.getClub());
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.showError("Erreur", "Erreur lors du chargement des clubs", e.getMessage());
        }

        HBox imageBox = new HBox(10);
        imageBox.setAlignment(Pos.CENTER_LEFT);
        imageBox.getChildren().addAll(imageField, browseButton);

        // Add components to grid
        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Prix (€):"), 0, 1);
        grid.add(prixField, 1, 1);
        grid.add(new Label("Quantité:"), 0, 2);
        grid.add(quantityField, 1, 2);
        grid.add(new Label("Club:"), 0, 3);
        grid.add(clubComboBox, 1, 3);
        grid.add(new Label("Description:"), 0, 4);
        grid.add(descriptionArea, 1, 4);
        grid.add(new Label("Image:"), 0, 5);
        grid.add(imageBox, 1, 5);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the name field
        Platform.runLater(nomField::requestFocus);

        // Handle file chooser
        browseButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Sélectionner une image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));

            File selectedFile = fileChooser.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (selectedFile != null) {
                imageField.setText(selectedFile.getAbsolutePath());
            }
        });

        // Convert dialog result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    // Validate inputs
                    if (nomField.getText().isEmpty()) {
                        AlertUtils.showError("Erreur", "Nom invalide", "Le nom du produit est obligatoire.");
                        return null;
                    }

                    // Update product
                    produit.setNomProd(nomField.getText());

                    // Validate and convert price
                    try {
                        float prix = Float.parseFloat(prixField.getText().replace(',', '.'));
                        produit.setPrix(prix);
                    } catch (NumberFormatException ex) {
                        AlertUtils.showError("Erreur", "Prix invalide",
                                "Veuillez entrer un prix valide (exemple: 29.99)");
                        return null;
                    }

                    produit.setQuantity(quantityField.getText());
                    produit.setDescProd(descriptionArea.getText());
                    produit.setImgProd(imageField.getText());
                    produit.setClub(clubComboBox.getValue());

                    return produit;
                } catch (Exception ex) {
                    AlertUtils.showError("Erreur", "Erreur lors de la modification du produit", ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        // Show dialog and process result
        Optional<Produit> result = dialog.showAndWait();

        result.ifPresent(updatedProduit -> {
            try {
                // Update the product
                produitService.updateProduit(updatedProduit);

                // Refresh the product list and statistics
                loadAllProduits();
                calculateStats();

                // Show success message
                showToast("Produit modifié avec succès", "success");
            } catch (SQLException ex) {
                AlertUtils.showError("Erreur", "Erreur lors de la mise à jour du produit", ex.getMessage());
            }
        });
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

        // Hide toast after 3 seconds
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

    @FXML
    private void goToAdmincommande() {
        ProduitApp.navigateTo("/com/esprit/views/produit/commandeview.fxml");
    }
}
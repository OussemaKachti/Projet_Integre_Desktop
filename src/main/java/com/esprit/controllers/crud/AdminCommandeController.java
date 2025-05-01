package com.esprit.controllers.crud;

import com.esprit.models.Commande;
import com.esprit.models.enums.StatutCommandeEnum;
import com.esprit.services.CommandeService;
import com.esprit.utils.AlertUtils;
import com.esprit.utils.DataSource;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public class AdminCommandeController implements Initializable {

    // Table components
    @FXML
    private TableView<Commande> tableView;
    @FXML
    private TableColumn<Commande, Integer> colId;
    @FXML
    private TableColumn<Commande, String> colUser;
    @FXML
    private TableColumn<Commande, Double> colTotal;
    @FXML
    private TableColumn<Commande, String> colStatus;
    @FXML
    private TableColumn<Commande, LocalDate> colDate;
    @FXML
    private TableColumn<Commande, Void> colActions;

    // Filter components
    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> filterStatusComboBox;

    // Statistics labels
    @FXML
    private Label totalCommandesLabel;
    @FXML
    private Label pendingCommandesLabel;
    @FXML
    private Label completedCommandesLabel;
    @FXML
    private Label cancelledCommandesLabel;

    // Containers
    @FXML
    private HBox paginationContainer;
    @FXML
    private VBox noCommandesContainer;
    @FXML
    private Pane toastContainer;

    // New components for popular products
    @FXML
    private VBox popularProductsCard;
    @FXML
    private StackPane chartContainer;
    private BarChart<String, Number> barChart;
    private boolean isChartVisible = false;

    // Service
    private final CommandeService commandeService;

    // Data lists
    private ObservableList<Commande> commandeList;
    private FilteredList<Commande> filteredList;

    // Pagination
    private int currentPage = 1;
    private static final int ITEMS_PER_PAGE = 2;
    private int totalPages = 1;

    public AdminCommandeController() {
        this.commandeService = new CommandeService();
        this.commandeList = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            if (!DataSource.getInstance().isConnected()) {
                showDatabaseConnectionError();
                return;
            }

            setupTableColumns();
            setupFilters();
            loadAllCommandes();
            calculateStats();
            setupPagination();
            setupPopularProductsCard(); // Added to set up the popular products card

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("Erreur d'initialisation", "Erreur lors du chargement de l'interface",
                    "Une erreur est survenue: " + e.getMessage());
        }
    }

    private void showDatabaseConnectionError() {
        if (tableView != null) {
            tableView.setPlaceholder(new Label("Impossible de se connecter à la base de données"));
        }

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur de connexion");
        alert.setHeaderText("Impossible de se connecter à la base de données");
        alert.setContentText("Vérifiez que le serveur MySQL est démarré et que les paramètres de connexion sont corrects.");

        ButtonType retryButton = new ButtonType("Réessayer");
        ButtonType exitButton = new ButtonType("Quitter", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(retryButton, exitButton);

        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == retryButton) {
                if (DataSource.getInstance().isConnected()) {
                    initialize(null, null);
                } else {
                    showDatabaseConnectionError();
                }
            } else if (buttonType == exitButton) {
                Platform.exit();
            }
        });
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colUser.setCellValueFactory(cellData -> {
            if (cellData.getValue().getUser() != null) {
                return new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUser().getNom());
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });

        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                if (empty || total == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f €", total));
                }
            }
        });

        colStatus.setCellValueFactory(cellData -> {
            StatutCommandeEnum statut = cellData.getValue().getStatut();
            return new javafx.beans.property.SimpleStringProperty(statut != null ? statut.name() : "N/A");
        });
        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "EN_ATTENTE":
                            setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                            break;
                        case "CONFIRMEE":
                            setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                            break;
                        case "ANNULEE":
                            setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });

        colDate.setCellValueFactory(new PropertyValueFactory<>("dateComm"));

        setupActionsColumn();
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(tc -> new TableCell<>() {
            private final Button validateButton = new Button("Valider");
            private final Button cancelButton = new Button("Annuler");
            private final HBox container = new HBox(5);

            {
                // Style buttons
                validateButton.setStyle(
                        "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 3;");
                validateButton.setMinWidth(80);

                cancelButton.setStyle(
                        "-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 3;");
                cancelButton.setMinWidth(80);

                // Add hover effects
                validateButton.setOnMouseEntered(e -> validateButton.setStyle(
                        "-fx-background-color: #388E3C; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 3;"));
                validateButton.setOnMouseExited(e -> validateButton.setStyle(
                        "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 3;"));

                cancelButton.setOnMouseEntered(e -> cancelButton.setStyle(
                        "-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 3;"));
                cancelButton.setOnMouseExited(e -> cancelButton.setStyle(
                        "-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 3;"));

                // Set up actions
                validateButton.setOnAction(event -> {
                    Commande commande = getTableView().getItems().get(getIndex());
                    validateCommande(commande);
                });

                cancelButton.setOnAction(event -> {
                    Commande commande = getTableView().getItems().get(getIndex());
                    cancelCommande(commande);
                });

                // Set up container
                container.setAlignment(Pos.CENTER);
                container.getChildren().addAll(validateButton, cancelButton);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Commande commande = getTableView().getItems().get(getIndex());
                    validateButton.setDisable(commande.getStatut() != StatutCommandeEnum.EN_COURS);
                    cancelButton.setDisable(commande.getStatut() != StatutCommandeEnum.EN_COURS);
                    setGraphic(container);
                }
            }
        });
    }

    private void setupFilters() {
        filteredList = new FilteredList<>(commandeList);

        if (txtSearch != null) {
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredList.setPredicate(commande -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    String lowerCaseFilter = newValue.toLowerCase();
                    return (commande.getUser() != null && commande.getUser().getNom().toLowerCase().contains(lowerCaseFilter)) ||
                            commande.getStatut().name().toLowerCase().contains(lowerCaseFilter) ||
                            String.valueOf(commande.getId()).contains(newValue);
                });
                updatePagination();
            });
        }

        ObservableList<String> statusOptions = FXCollections.observableArrayList(
                "Tous les statuts",
                StatutCommandeEnum.EN_COURS.name(),
                StatutCommandeEnum.CONFIRMEE.name(),
                StatutCommandeEnum.ANNULEE.name());

        if (filterStatusComboBox != null) {
            filterStatusComboBox.setItems(statusOptions);
            filterStatusComboBox.setValue("Tous les statuts");

            filterStatusComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                filterStatusComboBox.setDisable(true);
                String selectedStatus = "Tous les statuts".equals(newValue) ? "all" : newValue;

                filteredList.setPredicate(commande -> {
                    boolean matchesStatus = "all".equals(selectedStatus) ||
                            commande.getStatut().name().equals(selectedStatus);

                    String searchText = txtSearch.getText();
                    boolean matchesSearch = searchText == null || searchText.isEmpty() ||
                            (commande.getUser() != null && commande.getUser().getNom().toLowerCase().contains(searchText.toLowerCase())) ||
                            commande.getStatut().name().toLowerCase().contains(searchText.toLowerCase()) ||
                            String.valueOf(commande.getId()).contains(searchText);

                    return matchesStatus && matchesSearch;
                });

                currentPage = 1;
                updatePagination();

                new Thread(() -> {
                    try {
                        Thread.sleep(300);
                        Platform.runLater(() -> filterStatusComboBox.setDisable(false));
                    } catch (InterruptedException e) {
                        Platform.runLater(() -> filterStatusComboBox.setDisable(false));
                    }
                }).start();

                showToast("Filtré par " + ("all".equals(selectedStatus) ? "tous les statuts" : "statut: " + selectedStatus), "info");
            });
        }
    }

    @FXML
    private void searchProducts() {
        setupFilters();
    }

    private void loadAllCommandes() {
        try {
            tableView.setPlaceholder(new Label("Chargement des commandes..."));

            List<Commande> commandes = commandeService.getAllCommandes(null);
            commandeList.clear();
            commandeList.addAll(commandes);

            filteredList = new FilteredList<>(commandeList);
            tableView.setItems(filteredList);
            if (commandeList.isEmpty()) {
                if (noCommandesContainer != null) {
                    noCommandesContainer.setVisible(true);
                }
                if (tableView != null) {
                    tableView.setVisible(false);
                }
                if (paginationContainer != null) {
                    paginationContainer.setVisible(false);
                }
            } else {
                if (noCommandesContainer != null) {
                    noCommandesContainer.setVisible(false);
                }
                if (tableView != null) {
                    tableView.setVisible(true);
                }
                if (paginationContainer != null) {
                    paginationContainer.setVisible(true);
                }

                updatePagination();
            }
        } catch (Exception e) {
            AlertUtils.showError("Erreur", "Erreur lors du chargement des commandes", e.getMessage());
            tableView.setPlaceholder(new Label("Erreur lors du chargement des commandes"));
        }
    }

    private void updatePagination() {
        if (paginationContainer != null) {
            int itemCount = filteredList.size();
            totalPages = (itemCount + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;

            if (totalPages < 1) {
                totalPages = 1;
            }

            if (currentPage > totalPages) {
                currentPage = totalPages;
            }

            loadCurrentPage();
            setupPagination();
        }
    }

    private void loadCurrentPage() {
        int fromIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, filteredList.size());

        ObservableList<Commande> currentPageList;
        if (fromIndex < toIndex) {
            currentPageList = FXCollections.observableArrayList(filteredList.subList(fromIndex, toIndex));
        } else {
            currentPageList = FXCollections.observableArrayList();
        }

        tableView.setItems(currentPageList);
    }

    private void setupPagination() {
        if (paginationContainer == null) return;

        paginationContainer.getChildren().clear();

        if (totalPages <= 1) {
            paginationContainer.setVisible(false);
            paginationContainer.setManaged(false);
            return;
        }

        paginationContainer.setVisible(true);
        paginationContainer.setManaged(true);

        Button prevButton = new Button("←");
        prevButton.getStyleClass().add(currentPage == 1 ? "pagination-button-disabled" : "pagination-button");
        prevButton.setDisable(currentPage == 1);
        prevButton.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                loadCurrentPage();
                setupPagination();
            }
        });

        paginationContainer.getChildren().add(prevButton);

        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(startPage + 4, totalPages);

        for (int i = startPage; i <= endPage; i++) {
            Button pageButton = new Button(String.valueOf(i));
            pageButton.getStyleClass().addAll("pagination-button");

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
                setupPagination();
            });
            paginationContainer.getChildren().add(pageButton);
        }

        Button nextButton = new Button("→");
        nextButton.getStyleClass().add(currentPage == totalPages ? "pagination-button-disabled" : "pagination-button");
        nextButton.setDisable(currentPage == totalPages);
        nextButton.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                loadCurrentPage();
                setupPagination();
            }
        });

        paginationContainer.getChildren().add(nextButton);

        Label pageInfoLabel = new Label(String.format("Page %d sur %d", currentPage, totalPages));
        pageInfoLabel.setStyle("-fx-text-fill: #6c757d; -fx-padding: 5 0 0 10;");
        paginationContainer.getChildren().add(pageInfoLabel);
    }

    private void validateCommande(Commande commande) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Valider la Commande");
        confirmDialog.setHeaderText("Êtes-vous sûr de vouloir valider cette commande ?");
        confirmDialog.setContentText("Cette action ne peut pas être annulée.");

        if (confirmDialog.showAndWait().filter(response -> response == ButtonType.OK).isPresent()) {
            try {
                commandeService.validerCommande(commande.getId());
                showToast("Commande validée avec succès", "success");
                loadAllCommandes();
                calculateStats();
            } catch (Exception e) {
                showToast("Erreur lors de la validation : " + e.getMessage(), "error");
                e.printStackTrace();
            }
        }
    }

    private void cancelCommande(Commande commande) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Annuler la Commande");
        confirmDialog.setHeaderText("Êtes-vous sûr de vouloir annuler cette commande ?");
        confirmDialog.setContentText("Cette action ne peut pas être annulée.");

        if (confirmDialog.showAndWait().filter(response -> response == ButtonType.OK).isPresent()) {
            try {
                // You might need to add a method in CommandeService to cancel orders
                // For now, we'll use the supprimerCommande method
                commandeService.supprimerCommande(commande.getId());
                showToast("Commande annulée avec succès", "success");
                loadAllCommandes();
                calculateStats();
            } catch (Exception e) {
                showToast("Erreur lors de l'annulation : " + e.getMessage(), "error");
                e.printStackTrace();
            }
        }
    }

    private void calculateStats() {
        try {
            List<Commande> allCommandes = commandeService.getAllCommandes(null);

            int totalCommandes = allCommandes.size();
            totalCommandesLabel.setText(String.valueOf(totalCommandes));

            int pendingCommandes = 0;
            int completedCommandes = 0;

            for (Commande commande : allCommandes) {
                switch (commande.getStatut()) {
                    case EN_COURS:
                        pendingCommandes++;
                        break;
                    case CONFIRMEE:
                        completedCommandes++;
                        break;
                }
            }

            pendingCommandesLabel.setText(String.valueOf(pendingCommandes));
            completedCommandesLabel.setText(String.valueOf(completedCommandes));

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur lors du calcul des statistiques : " + e.getMessage());
        }
    }

    private void setupPopularProductsCard() {
        // Initialize the bar chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Produit");
        yAxis.setLabel("Quantité Vendue");
        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Produits les plus populaires");
        barChart.setPrefHeight(300);
        barChart.setPrefWidth(600);

        // Add the chart to the chartContainer
        chartContainer.getChildren().add(barChart);

        // Set up the click handler for the popular products card
        popularProductsCard.setOnMouseClicked(event -> {
            if (!isChartVisible) {
                // Load the data and show the chart
                loadPopularProductsChart();
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

    private void loadPopularProductsChart() {
        try {
            // Clear any existing data in the chart
            barChart.getData().clear();

            // Fetch the most popular products
            List<Object[]> topProducts = commandeService.getTopProduits();

            // Create a series for the chart
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Ventes");

            // Add data to the series
            for (Object[] product : topProducts) {
                String productName = (String) product[0];
                int totalSales = (int) product[1];
                series.getData().add(new XYChart.Data<>(productName, totalSales));
            }

            // Add the series to the chart
            barChart.getData().add(series);

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Erreur lors du chargement des produits populaires: " + e.getMessage(), "error");
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
            default:
                toastHBox.setStyle("-fx-background-color: #28a745; -fx-background-radius: 4px;");
                break;
        }

        toastLabel.setText(message);
        toastContainer.setVisible(true);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toastContainer);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

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
}
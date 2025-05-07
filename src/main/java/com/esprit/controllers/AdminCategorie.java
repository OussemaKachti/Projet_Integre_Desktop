package com.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import com.esprit.models.Categorie;
import com.esprit.services.ServiceCategorie;
import com.esprit.services.ServiceCategorie.CategoryUsage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminCategorie implements Initializable {

    // Menu navigation elements
    @FXML
    private Label dateLabel;

    // Stats labels
    @FXML
    private Label totalCategoriesLabel;

    @FXML
    private Label popularCategoryLabel;

    // Search field
    @FXML
    private TextField searchField;

    // Category list view
    @FXML
    private ListView<Categorie> categoriesListView;

    // Filter buttons
    @FXML
    private Button allFilterButton;

    @FXML
    private Button inactiveFilterButton;

    // Pagination
    @FXML
    private Label paginationInfoLabel;

    // Add category form
    @FXML
    private TextField nomcattf;

    @FXML
    private Button ajoutercat;

    // Other buttons
    @FXML
    private Button refreshButton;

    // PieChart component
    @FXML
    private PieChart categoryUsagePieChart;

    private ServiceCategorie serviceCategorie;
    private ObservableList<Categorie> categoriesList = FXCollections.observableArrayList();
    private FilteredList<Categorie> filteredCategories;

    // Pagination variables
    private int currentPage = 1;
    private final int ITEMS_PER_PAGE = 10;
    private int totalPages = 1;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        serviceCategorie = new ServiceCategorie();

        // Set current date
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        String formattedDate = currentDate.format(formatter);
        dateLabel.setText("Today: " + formattedDate);

        // Configure ListView
        configureListView();

        // Set up filtered list
        filteredCategories = new FilteredList<>(categoriesList);

        // Load categories
        loadCategories();

        // Add search functionality
        addSearchListener();

        // Configure filter buttons
        configureFilterButtons();

        // Initialize pagination
        updatePagination();

        // Initialize PieChart
        loadCategoryUsageStats();
    }

    private void configureListView() {
        categoriesListView.setCellFactory(param -> new ListCell<Categorie>() {
            @Override
            protected void updateItem(Categorie categorie, boolean empty) {
                super.updateItem(categorie, empty);

                if (empty || categorie == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Create an HBox for the list item with category info and action buttons
                    HBox itemLayout = new HBox(10);
                    itemLayout.setStyle("-fx-padding: 10; -fx-alignment: center-left;");

                    // Category information
                    Label idLabel = new Label("#" + categorie.getId());
                    idLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 50;");

                    Label nameLabel = new Label(categorie.getNom_cat());
                    nameLabel.setStyle("-fx-min-width: 200;");

                    // Action buttons
                    Button deleteButton = new Button("Delete");
                    deleteButton.setStyle("-fx-background-color: #fff5f5; -fx-text-fill: #dc3545; -fx-border-color: #dc3545; -fx-border-radius: 3;");

                    // Add actions to buttons
                    deleteButton.setOnAction(event -> handleDeleteCategory(categorie));

                    // Create a spacer to push buttons to the right
                    javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                    HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                    // Add all elements to the layout
                    itemLayout.getChildren().addAll(idLabel, nameLabel, spacer, deleteButton);

                    setGraphic(itemLayout);
                    setText(null);
                }
            }
        });
    }

    private void loadCategories() {
        try {
            categoriesList.clear();
            categoriesList.addAll(serviceCategorie.afficher());

            // Set statistics
            updateStatistics();

            // Update pagination
            applyPaginationAndFilters();
        } catch (SQLException ex) {
            System.err.println("Error loading categories: " + ex.getMessage());
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load categories", ex.getMessage());
        }
    }


    private void updateStatistics() {
        int total = categoriesList.size();

        String popularCategory = "N/A";

        // Set a popular category if we have at least one category
        if (!categoriesList.isEmpty()) {
            popularCategory = categoriesList.get(0).getNom_cat();
        }

        // Update the labels
        totalCategoriesLabel.setText(String.valueOf(total));
        popularCategoryLabel.setText(popularCategory);
    }

    private void addSearchListener() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applySearchFilter(newValue);
        });
    }

    private void applySearchFilter(String searchText) {
        filteredCategories.setPredicate(categorie -> {
            if (searchText == null || searchText.isEmpty()) {
                return true;
            }

            return categorie.getNom_cat().toLowerCase().contains(searchText.toLowerCase());
        });

        // Reset to first page when search changes
        currentPage = 1;

        // Update the view
        applyPaginationAndFilters();
    }

    private void configureFilterButtons() {
        // All filter (default)
        allFilterButton.setOnAction(event -> {
            filteredCategories.setPredicate(categorie -> true);
            currentPage = 1;
            applyPaginationAndFilters();
        });

        // Inactive filter
        inactiveFilterButton.setOnAction(event -> {
            filteredCategories.setPredicate(categorie -> {
                // In a real app, this would check if the category is inactive
                // For this example, let's assume categories with odd IDs are inactive
                return categorie.getId() % 2 != 0;
            });
            currentPage = 1;
            applyPaginationAndFilters();
        });
    }

    private void updatePagination() {
        int totalItems = filteredCategories.size();
        totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);

        // Update pagination info label
        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE + 1;
        int endIndex = Math.min(currentPage * ITEMS_PER_PAGE, totalItems);

        if (totalItems == 0) {
            paginationInfoLabel.setText("No categories found");
        } else {
            paginationInfoLabel.setText(String.format("Showing %d-%d of %d categories",
                    startIndex, endIndex, totalItems));
        }
    }

    private void applyPaginationAndFilters() {
        updatePagination();

        // Create a sublist for the current page
        int totalItems = filteredCategories.size();
        int fromIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, totalItems);

        // Create a temporary list for the current page
        ObservableList<Categorie> currentPageItems = FXCollections.observableArrayList();

        if (fromIndex < totalItems) {
            for (int i = fromIndex; i < toIndex; i++) {
                currentPageItems.add(filteredCategories.get(i));
            }
        }

        // Update ListView
        categoriesListView.setItems(currentPageItems);
    }

    /**
     * Charge et affiche les statistiques d'utilisation des catégories dans le PieChart
     */
    private void loadCategoryUsageStats() {
        try {
            // Récupérer les statistiques d'utilisation
            List<CategoryUsage> usageStats = serviceCategorie.getCategoriesUsageStats();

            // Créer des données pour le PieChart
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

            // Limiter à 5 catégories pour une meilleure lisibilité, avec une catégorie "Autres" si nécessaire
            int othersCount = 0;
            int totalCount = 0;

            // Calculer le nombre total de clubs
            for (CategoryUsage usage : usageStats) {
                totalCount += usage.getCount();
            }

            // Ajouter les 5 premières catégories au graphique
            for (int i = 0; i < usageStats.size(); i++) {
                CategoryUsage usage = usageStats.get(i);
                if (i < 5) {
                    // Calculer le pourcentage pour l'affichage
                    double percentage = totalCount > 0 ? (usage.getCount() * 100.0 / totalCount) : 0;
                    String label = usage.getName() + " (" + String.format("%.1f", percentage) + "%)";
                    pieChartData.add(new PieChart.Data(label, usage.getCount()));
                } else {
                    // Regrouper le reste dans "Autres"
                    othersCount += usage.getCount();
                }
            }

            // Ajouter la catégorie "Autres" si nécessaire
            if (othersCount > 0) {
                double percentage = totalCount > 0 ? (othersCount * 100.0 / totalCount) : 0;
                String label = "Autres (" + String.format("%.1f", percentage) + "%)";
                pieChartData.add(new PieChart.Data(label, othersCount));
            }

            // Mettre à jour le PieChart
            categoryUsagePieChart.setData(pieChartData);

            categoryUsagePieChart.setLabelsVisible(true);

            // Ajouter une esthétique aux sections du graphique
            applyPieChartCustomization();

            // Si nous avons des statistiques, mettre à jour la catégorie la plus populaire
            if (!usageStats.isEmpty()) {
                CategoryUsage mostPopular = usageStats.get(0);
                popularCategoryLabel.setText(mostPopular.getName());
            }

        } catch (SQLException ex) {
            System.err.println("Error loading category usage statistics: " + ex.getMessage());
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load category statistics", ex.getMessage());
        }
    }

    /**
     * Applique des personnalisations esthétiques au PieChart
     */
    private void applyPieChartCustomization() {
        // Définir des couleurs attrayantes pour les sections du graphique
        String[] pieColors = {
                "rgba(41, 128, 185, 0.8)", // Bleu
                "rgba(39, 174, 96, 0.8)",  // Vert
                "rgba(192, 57, 43, 0.8)",  // Rouge
                "rgba(241, 196, 15, 0.8)", // Jaune
                "rgba(142, 68, 173, 0.8)", // Violet
                "rgba(127, 140, 141, 0.8)" // Gris (pour "Autres")
        };

        int i = 0;
        for (PieChart.Data data : categoryUsagePieChart.getData()) {
            String color = pieColors[i % pieColors.length];
            data.getNode().setStyle("-fx-pie-color: " + color + ";");

            // Ajouter une animation d'agrandissement au survol
            final int index = i;
            data.getNode().setOnMouseEntered(event -> {
                data.getNode().setStyle("-fx-pie-color: " + pieColors[index % pieColors.length].replace("0.8", "1.0") + "; -fx-scale-x: 1.1; -fx-scale-y: 1.1;");
            });

            data.getNode().setOnMouseExited(event -> {
                data.getNode().setStyle("-fx-pie-color: " + pieColors[index % pieColors.length] + "; -fx-scale-x: 1.0; -fx-scale-y: 1.0;");
            });

            i++;
        }
    }


    @FXML
    public void insererCategorie() {
        String nomCat = nomcattf.getText().trim();

        if (nomCat.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Category Name Required",
                    "Please enter a category name");
            return;
        }

        try {
            // Create and add new category
            Categorie newCategorie = new Categorie();
            newCategorie.setNom_cat(nomCat);

            serviceCategorie.ajouter(newCategorie);

            // Clear form fields
            nomcattf.clear();

            // Refresh categories list
            loadCategories();

            // Reload category usage statistics for the PieChart
            loadCategoryUsageStats();

            showAlert(Alert.AlertType.INFORMATION, "Success", "Category Added",
                    "The category has been successfully added.");
        } catch (SQLException ex) {
            System.err.println("Error adding category: " + ex.getMessage());
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add category", ex.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        loadCategories();
        searchField.clear();
        currentPage = 1;

        filteredCategories.setPredicate(categorie -> true);
        applyPaginationAndFilters();

        // Reload PieChart data
        loadCategoryUsageStats();
    }

    private void handleDeleteCategory(Categorie categorie) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText("Delete Category");
        confirmDialog.setContentText("Are you sure you want to delete the category: " + categorie.getNom_cat() + "?");

        Optional<ButtonType> result = confirmDialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                serviceCategorie.supprimer(categorie.getId());

                // Refresh categories list
                loadCategories();

                // Reload PieChart data
                loadCategoryUsageStats();

                showAlert(Alert.AlertType.INFORMATION, "Success", "Category Deleted",
                        "The category has been successfully deleted.");
            } catch (SQLException ex) {
                System.err.println("Error deleting category: " + ex.getMessage());
                showAlert(Alert.AlertType.ERROR, "Delete Error", "Failed to delete category", ex.getMessage());
            }
        }
    }

    @FXML
    private void handleBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Dashboard.fxml"));
            categoriesListView.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to return to dashboard", e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Additional navigation methods for side menu
    @FXML
    private void navigateToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Dashboard.fxml"));
            categoriesListView.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to navigate to Dashboard", e.getMessage());
        }
    }

    @FXML
    private void navigateToMembers() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Members.fxml"));
            categoriesListView.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to navigate to Members", e.getMessage());
        }
    }

    @FXML
    private void navigateToEvents() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Events.fxml"));
            categoriesListView.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to navigate to Events", e.getMessage());
        }
    }

    @FXML
    private void navigateToReports() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Reports.fxml"));
            categoriesListView.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to navigate to Reports", e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            categoriesListView.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to logout", e.getMessage());
        }
    }
}
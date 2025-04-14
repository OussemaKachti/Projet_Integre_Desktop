package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import models.Categorie;
import services.ServiceCategorie;
import utils.DataSource;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminCategorie implements Initializable {

    @FXML
    private TableView<Categorie> categoryTable;

    @FXML
    private TableColumn<Categorie, Integer> idColumn;

    @FXML
    private TableColumn<Categorie, String> nameColumn;

    @FXML
    private TableColumn<Categorie, Void> actionsColumn;

    @FXML
    private TextField searchField;

    @FXML
    private Button addCategoryButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button backButton;

    @FXML
    private Label totalCategoriesLabel;

    private ServiceCategorie serviceCategorie;
    private ObservableList<Categorie> categoriesList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        serviceCategorie = new ServiceCategorie();

        // Configure table columns
        configureTableColumns();

        // Load categories
        loadCategories();

        // Add search functionality
        addSearchListener();
    }

    private void configureTableColumns() {
        // Set cell value factories
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("nom_cat"));

        // Configure actions column
        configureActionsColumn();
    }

    private void configureActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox buttonsBox = new HBox(5);

            {
                // Styling the buttons
                editButton.getStyleClass().add("edit-button");
                deleteButton.getStyleClass().add("delete-button");

                buttonsBox.getChildren().addAll(editButton, deleteButton);

                // Action handlers
                editButton.setOnAction(event -> {
                    Categorie categorie = getTableView().getItems().get(getIndex());
                    handleEditCategory(categorie);
                });

                deleteButton.setOnAction(event -> {
                    Categorie categorie = getTableView().getItems().get(getIndex());
                    handleDeleteCategory(categorie);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttonsBox);
                }
            }
        });
    }

    private void loadCategories() {
        try {
            categoriesList.clear();
            categoriesList.addAll(serviceCategorie.afficher());
            categoryTable.setItems(categoriesList);
            updateTotalCategoriesLabel();
        } catch (SQLException ex) {
            System.err.println("Error loading categories: " + ex.getMessage());
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load categories", ex.getMessage());
        }
    }

    private void addSearchListener() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();

        ObservableList<Categorie> filteredCategories = FXCollections.observableArrayList();

        for (Categorie categorie : categoriesList) {
            boolean matchesSearch = categorie.getNom_cat().toLowerCase().contains(searchText);

            if (matchesSearch) {
                filteredCategories.add(categorie);
            }
        }

        categoryTable.setItems(filteredCategories);
        updateTotalCategoriesLabel();
    }

    private void updateTotalCategoriesLabel() {
        int displayedCount = categoryTable.getItems().size();
        int totalCount = categoriesList.size();

        if (displayedCount == totalCount) {
            totalCategoriesLabel.setText("Total Categories: " + totalCount);
        } else {
            totalCategoriesLabel.setText("Displaying " + displayedCount + " of " + totalCount + " categories");
        }
    }

    @FXML
    private void handleAddCategory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterCategorie.fxml"));
            Parent root = loader.load();
            addCategoryButton.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to open Add Category page", e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        loadCategories();
        searchField.clear();
    }

    private void handleEditCategory(Categorie categorie) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierCategorie.fxml"));
            Parent root = loader.load();

            // Assuming you have a ModifierCategorie controller that accepts a category to edit



            // Use a control that is in scope to access the scene
            addCategoryButton.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to open Edit Category page", e.getMessage());
        }
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
                categoriesList.remove(categorie);
                applyFilters(); // Reapply filters to update table
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
            backButton.getScene().setRoot(root);
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
}
package com.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import com.esprit.models.Categorie;
import com.esprit.services.ServiceCategorie;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class AjouterCategorie implements Initializable {

    @FXML
    private TextField nomcattf;
    @FXML
    private Button ajoutercat;
    @FXML
    private ListView<Categorie> categoriesListView;
    @FXML
    private TextField searchField;
    @FXML
    private Label totalItemsLabel;
    @FXML
    private Label dateLabel;

    private ServiceCategorie serviceCategorie;
    private ObservableList<Categorie> masterData = FXCollections.observableArrayList();
    private FilteredList<Categorie> filteredData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Afficher la date actuelle
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.ENGLISH);
        String formattedDate = currentDate.format(formatter);
        dateLabel.setText("Today: " + formattedDate);

        serviceCategorie = new ServiceCategorie();
        setupListView();
        setupSearch();
        loadCategories();
    }

    private void setupSearch() {
        filteredData = new FilteredList<>(masterData, p -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(categorie -> {
                if (newValue == null || newValue.isEmpty()) return true;
                return categorie.getNom_cat().toLowerCase().contains(newValue.toLowerCase());
            });
            updateTotalItemsLabel();
        });

        categoriesListView.setItems(filteredData);
    }

    private void setupListView() {
        categoriesListView.setCellFactory(param -> new ListCell<>() {
            private HBox container = new HBox();
            private Label idLabel = new Label();
            private Label nameLabel = new Label();
            private Region spacer = new Region();

            {
                container.setAlignment(Pos.CENTER_LEFT);
                container.setPadding(new Insets(10, 15, 10, 15));
                container.setSpacing(10);

                idLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                idLabel.setTextFill(Color.web("#1e90ff"));
                idLabel.setMinWidth(50);

                nameLabel.setFont(Font.font("Arial", 14));

                HBox.setHgrow(spacer, Priority.ALWAYS);
                container.getChildren().addAll(idLabel, nameLabel, spacer);

                container.setOnMouseEntered(event -> {
                    if (!isEmpty()) container.setStyle("-fx-background-color: #f5f8ff; -fx-background-radius: 5;");
                });
                container.setOnMouseExited(event -> {
                    if (!isEmpty()) container.setStyle("-fx-background-color: transparent;");
                });

                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(Categorie item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    idLabel.setText("#" + item.getId());
                    nameLabel.setText(item.getNom_cat());
                    setGraphic(container);
                }
            }
        });

        Label placeholder = new Label("Aucune catégorie disponible");
        placeholder.setStyle("-fx-text-fill: #999999;");
        placeholder.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        categoriesListView.setPlaceholder(placeholder);

        categoriesListView.setOnMouseClicked(this::handleCategoryClick);
    }

    private void handleCategoryClick(MouseEvent event) {
        Categorie selectedCategory = categoriesListView.getSelectionModel().getSelectedItem();
        if (selectedCategory != null && event.getClickCount() == 2) {
            showCategoryDetails(selectedCategory);
        }
    }

    private void showCategoryDetails(Categorie category) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de la catégorie");
        alert.setHeaderText("Catégorie: " + category.getNom_cat());
        alert.setContentText("ID: " + category.getId() + "\nNom: " + category.getNom_cat());
        alert.showAndWait();
    }

    private void loadCategories() {
        try {
            List<Categorie> categories = serviceCategorie.afficher();
            masterData.clear();
            masterData.addAll(categories);
            updateTotalItemsLabel();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les catégories: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void updateTotalItemsLabel() {
        int count = filteredData != null ? filteredData.size() : masterData.size();
        totalItemsLabel.setText("Total: " + count + " catégorie" + (count > 1 ? "s" : ""));
    }

    @FXML
    void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/ListeEvents.fxml"));
            Scene scene = new Scene(root);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible de charger la page précédente.", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    void insererCategorie(ActionEvent event) {
        String nomcattfText = nomcattf.getText().trim();
        if (nomcattfText.isEmpty()) {
            showAlert("Erreur", "Le nom de la catégorie ne peut pas être vide.", Alert.AlertType.ERROR);
            return;
        }

        try {
            Categorie categorie = new Categorie(nomcattfText);
            serviceCategorie.ajouter(categorie);
            showAlert("Succès", "La catégorie a été ajoutée avec succès.", Alert.AlertType.INFORMATION);
            nomcattf.clear();
            loadCategories();
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de l'ajout: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

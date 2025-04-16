package com.esprit.controllers.crud;

import com.esprit.ProduitApp;
import com.esprit.models.Club;
import com.esprit.models.Produit;
import com.esprit.services.ClubService;
import com.esprit.services.ProduitService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ProduitCardController implements Initializable {

    @FXML
    private FlowPane produitContainer;

    @FXML
    private ComboBox<Club> comboFilterClub;

    @FXML
    private TextField txtRecherche;

    @FXML
    private Button btnRetour;

    private final ProduitService produitService;
    private final ClubService clubService;
    private List<Produit> allProduits;

    public ProduitCardController() {
        this.produitService = ProduitService.getInstance();
        this.clubService = ClubService.getInstance();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            loadProduits();
            setupFilters();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du chargement des données", e.getMessage());
        }
    }

    private void loadProduits() throws SQLException {
        allProduits = produitService.getAll();
        displayProduits(allProduits);
    }

    private void setupFilters() throws SQLException {
        List<Club> clubs = clubService.getAll();
        comboFilterClub.getItems().add(null); // Option "Tous"
        comboFilterClub.getItems().addAll(clubs);
        comboFilterClub.setPromptText("Tous les clubs");

        comboFilterClub.setCellFactory(cell -> new ListCell<Club>() {
            @Override
            protected void updateItem(Club club, boolean empty) {
                super.updateItem(club, empty);
                setText(empty || club == null ? null : club.getNom());
            }
        });

        comboFilterClub.setButtonCell(new ListCell<Club>() {
            @Override
            protected void updateItem(Club club, boolean empty) {
                super.updateItem(club, empty);
                setText(empty || club == null ? null : club.getNom());
            }
        });

        comboFilterClub.valueProperty().addListener((obs, oldVal, newVal) -> filterProduits());
        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> filterProduits());
    }

    private void filterProduits() {
        Club selectedClub = comboFilterClub.getValue();
        String searchText = txtRecherche.getText().toLowerCase();

        List<Produit> filteredList = allProduits.stream()
                .filter(p -> selectedClub == null || (p.getClub() != null && p.getClub().getId() == selectedClub.getId()))
                .filter(p -> searchText.isEmpty() ||
                        p.getNomProd().toLowerCase().contains(searchText) ||
                        p.getDescProd().toLowerCase().contains(searchText))
                .collect(Collectors.toList());

        displayProduits(filteredList);
    }

    private void displayProduits(List<Produit> produits) {
        produitContainer.getChildren().clear();
        for (Produit produit : produits) {
            produitContainer.getChildren().add(createProduitCard(produit));
        }
    }

    private VBox createProduitCard(Produit produit) {
        VBox card = new VBox();
        card.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        card.setPrefWidth(250);
        card.setSpacing(5);
        card.setPadding(new Insets(0, 0, 10, 0));

        ImageView imageView = new ImageView();
        try {
            String imagePath = produit.getImgProd();
            if (imagePath != null && !imagePath.isEmpty()) {
                URL imageUrl = getClass().getClassLoader().getResource(imagePath);
                if (imageUrl != null) {
                    imageView.setImage(new Image(imageUrl.toString()));
                } else {
                    imageView.setImage(new Image(getClass().getClassLoader().getResource("images/default-product.png").toString()));
                }
            } else {
                imageView.setImage(new Image(getClass().getClassLoader().getResource("images/default-product.png").toString()));
            }
        } catch (Exception e) {
            try {
                imageView.setImage(new Image(getClass().getClassLoader().getResource("images/default-product.png").toString()));
            } catch (Exception ex) {
                System.err.println("Erreur image par défaut: " + ex.getMessage());
            }
        }

        imageView.setFitWidth(250);
        imageView.setFitHeight(250);
        imageView.setPreserveRatio(true);

        // ✅ Clic pour voir les détails
        imageView.setOnMouseClicked(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/produit/ProduitDetailView.fxml"));
                Parent detailRoot = loader.load();
                ProduitDetailsController controller = loader.getController();
                controller.initData(produit);
                Scene scene = new Scene(detailRoot);
                ProduitApp.getPrimaryStage().setScene(scene);
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de navigation", e.getMessage());
            }
        });

        // Etiquette Club
        Label clubLabel = new Label(produit.getClub() != null ? produit.getClub().getNom() : "");
        clubLabel.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 5px 10px; -fx-text-fill: #555;");
        Label priceLabel = new Label(produit.getPrix() + " TND");
        priceLabel.setStyle("-fx-background-color: #4285f4; -fx-text-fill: white; -fx-padding: 5px 10px; -fx-background-radius: 30;");

        StackPane imageContainer = new StackPane(imageView);
        AnchorPane overlay = new AnchorPane();
        AnchorPane.setTopAnchor(clubLabel, 10.0);
        AnchorPane.setLeftAnchor(clubLabel, 10.0);
        AnchorPane.setTopAnchor(priceLabel, 10.0);
        AnchorPane.setRightAnchor(priceLabel, 10.0);
        overlay.getChildren().addAll(clubLabel, priceLabel);
        imageContainer.getChildren().add(overlay);

        Label titleLabel = new Label(produit.getNomProd().toUpperCase());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setPadding(new Insets(5, 10, 0, 10));

        Label descriptionLabel = new Label(produit.getDescProd());
        descriptionLabel.setWrapText(true);
        descriptionLabel.setPadding(new Insets(0, 10, 5, 10));

        HBox clubInfo = new HBox(5);
        clubInfo.setAlignment(Pos.CENTER_LEFT);
        clubInfo.setPadding(new Insets(0, 10, 0, 10));

        Circle clubIcon = new Circle(10);
        clubIcon.setFill(Color.web("#4285f4"));
        Label clubNameLabel = new Label(produit.getClub() != null ? produit.getClub().getNom() : "");

        Label quantityLabel = new Label(produit.getQuantity() + " disponibles");
        quantityLabel.setStyle("-fx-text-fill: #777;");

        clubInfo.getChildren().addAll(clubIcon, clubNameLabel);

        HBox infoContainer = new HBox();
        infoContainer.setAlignment(Pos.CENTER_LEFT);
        infoContainer.setPadding(new Insets(0, 10, 5, 10));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        infoContainer.getChildren().addAll(clubInfo, spacer, quantityLabel);

        card.getChildren().addAll(imageContainer, titleLabel, descriptionLabel, infoContainer);

        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 15, 0, 0, 10);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);"));
        card.setOnMouseClicked(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/produit/ProduitDetailsView.fxml"));
                Parent detailRoot = loader.load();

                ProduitDetailsController controller = loader.getController();
                controller.initData(produit); // on passe le produit cliqué

                Scene scene = new Scene(detailRoot);
                ProduitApp.getPrimaryStage().setScene(scene);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return card;
    }

    @FXML
    private void retourGestion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/produit/ProduitView.fxml"));
            Parent root = loader.load();
            if (ProduitApp.getPrimaryStage() != null) {
                ProduitApp.getPrimaryStage().getScene().setRoot(root);
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation impossible", "Impossible d'accéder à la fenêtre principale.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de navigation", "Impossible de revenir à l'écran précédent.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

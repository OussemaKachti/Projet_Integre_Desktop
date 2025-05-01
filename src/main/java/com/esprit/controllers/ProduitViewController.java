package com.esprit.controllers;

import com.esprit.ProduitApp;
import com.esprit.models.Club;
import com.esprit.models.Produit;
import com.esprit.services.ClubService;
import com.esprit.services.ProduitService;
import com.esprit.utils.AlertUtils;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.geometry.Pos;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ProduitViewController implements Initializable {

    @FXML private FlowPane productContainer;
    @FXML private ComboBox<Club> comboFilterClub;
    @FXML private TextField txtSearch;
    @FXML private Button btnSearch;
    @FXML private VBox emptyState;
    @FXML private Button btnPanier;
    @FXML private Button btnAdmin;
    @FXML private Button btnAddProduct;
    @FXML private HBox paginationContainer; // Added for pagination

    private final ProduitService produitService;
    private final ClubService clubService;
    private List<Produit> allProduits = new ArrayList<>();
    private List<Produit> filteredProducts = new ArrayList<>(); // To store filtered products
    // Pagination variables
    private int currentPage = 1;
    private static final int ITEMS_PER_PAGE = 2; // Number of products per page (adjust as needed)
    private int totalPages = 1;

    public ProduitViewController() {
        this.produitService = ProduitService.getInstance();
        this.clubService = ClubService.getInstance();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupClubFilter();
        loadAllProduits();
    }

    /**
     * Configure le filtre par club
     */
    private void setupClubFilter() {
        try {
            // Ajouter l'option "Tous les clubs"
            Club allClubsOption = new Club();
            allClubsOption.setId(-1);
            allClubsOption.setNom("Tous les clubs");

            List<Club> clubs = clubService.getAll();
            clubs.add(0, allClubsOption); // Ajouter en première position

            comboFilterClub.getItems().setAll(clubs);
            comboFilterClub.setCellFactory(cell -> new ListCell<Club>() {
                @Override
                protected void updateItem(Club club, boolean empty) {
                    super.updateItem(club, empty);
                    setText(empty || club == null ? "" : club.getNom());
                }
            });
            comboFilterClub.setButtonCell(new ListCell<Club>() {
                @Override
                protected void updateItem(Club club, boolean empty) {
                    super.updateItem(club, empty);
                    setText(empty || club == null ? "" : club.getNom());
                }
            });

            comboFilterClub.getSelectionModel().selectFirst();

            // Ajouter un écouteur de sélection
            comboFilterClub.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    filterProducts();
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.showError("Erreur", "Erreur lors du chargement des clubs", e.getMessage());
        }
    }

    /**
     * Show the dialog to add a new product
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

                // Refresh the product list
                loadAllProduits();

                // Show success message using the new showToast method
                showToast("Produit ajouté avec succès", "success");
            } catch (SQLException ex) {
                AlertUtils.showError("Erreur", "Erreur lors de l'enregistrement du produit", ex.getMessage());
            }
        });
    }

    /**
     * Charge tous les produits
     */
    private void loadAllProduits() {
        try {
            allProduits = produitService.getAll();
            filterProducts();
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.showError("Erreur", "Erreur lors du chargement des produits", e.getMessage());
        }
    }

    /**
     * Filtre les produits selon les critères et met à jour la pagination
     */
    private void filterProducts() {
        String searchText = txtSearch.getText().toLowerCase();
        Club selectedClub = comboFilterClub.getSelectionModel().getSelectedItem();

        filteredProducts = allProduits.stream()
                .filter(p -> (searchText.isEmpty() ||
                        p.getNomProd().toLowerCase().contains(searchText) ||
                        p.getDescProd().toLowerCase().contains(searchText)))
                .filter(p -> (selectedClub == null || selectedClub.getId() == -1 ||
                        (p.getClub() != null && p.getClub().getId() == selectedClub.getId())))
                .collect(Collectors.toList());

        // Reset pagination to the first page
        currentPage = 1;
        updatePagination();
    }

    /**
     * Met à jour la pagination en fonction des produits filtrés
     */
    private void updatePagination() {
        if (paginationContainer != null) {
            int itemCount = filteredProducts.size();
            totalPages = (itemCount + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE; // Ceiling division

            if (totalPages < 1) {
                totalPages = 1;
            }

            if (currentPage > totalPages) {
                currentPage = totalPages;
            }

            // Update the displayed products based on the current page
            displayProducts();

            // Update pagination controls
            setupPagination();
        }
    }

    /**
     * Affiche les produits de la page actuelle
     */
    private void displayProducts() {
        productContainer.getChildren().clear();

        if (filteredProducts.isEmpty()) {
            emptyState.setVisible(true);
            return;
        }

        emptyState.setVisible(false);

        // Calculate the range of products to display for the current page
        int fromIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, filteredProducts.size());

        // Display only the products for the current page
        for (int i = fromIndex; i < toIndex; i++) {
            Produit produit = filteredProducts.get(i);
            try {
                // Charger le composant card pour chaque produit
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/produit/ProduitCardItem.fxml"));
                Parent cardNode = loader.load();

                // Configurer les composants de la card
                setupProductCard(cardNode, produit);

                // Ajouter des marges
                FlowPane.setMargin(cardNode, new Insets(10));
                productContainer.getChildren().add(cardNode);
            } catch (IOException e) {
                e.printStackTrace();
                AlertUtils.showError("Erreur", "Erreur lors de l'affichage des produits", e.getMessage());
            }
        }
    }

    /**
     * Configure les contrôles de pagination
     */
    private void setupPagination() {
        if (paginationContainer == null) {
            return;
        }

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
        prevButton.setStyle(currentPage == 1 ?
                "-fx-background-color: #E0E0E0; -fx-text-fill: #999999;" :
                "-fx-background-color: #6200EE; -fx-text-fill: white;");
        prevButton.setDisable(currentPage == 1);
        prevButton.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                updatePagination();
            }
        });

        paginationContainer.getChildren().add(prevButton);

        // Pages numbered buttons - show max 5 pages around current page
        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(startPage + 4, totalPages);

        for (int i = startPage; i <= endPage; i++) {
            Button pageButton = new Button(String.valueOf(i));
            if (i == currentPage) {
                pageButton.setStyle("-fx-font-weight: bold; -fx-background-color: #6200EE; -fx-text-fill: white;");
            } else {
                pageButton.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #333333;");
            }

            final int pageNum = i;
            pageButton.setOnAction(e -> {
                currentPage = pageNum;
                updatePagination();
            });
            paginationContainer.getChildren().add(pageButton);
        }

        // Next button
        Button nextButton = new Button("→");
        nextButton.setStyle(currentPage == totalPages ?
                "-fx-background-color: #E0E0E0; -fx-text-fill: #999999;" :
                "-fx-background-color: #6200EE; -fx-text-fill: white;");
        nextButton.setDisable(currentPage == totalPages);
        nextButton.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                updatePagination();
            }
        });

        paginationContainer.getChildren().add(nextButton);

        // Add page count information
        Label pageInfoLabel = new Label(String.format("Page %d sur %d", currentPage, totalPages));
        pageInfoLabel.setStyle("-fx-text-fill: #6c757d; -fx-padding: 5 0 0 10;");
        paginationContainer.getChildren().add(pageInfoLabel);
    }

    /**
     * Configure une card de produit avec les données du produit
     */
    private void setupProductCard(Parent cardNode, Produit produit) {
        // Récupérer les éléments de la card
        ImageView imgProduct = (ImageView) cardNode.lookup("#imgProduct");
        Label lblNom = (Label) cardNode.lookup("#lblNom");
        Label lblDescription = (Label) cardNode.lookup("#lblDescription");
        Label lblPrix = (Label) cardNode.lookup("#lblPrix");
        Label lblQuantity = (Label) cardNode.lookup("#lblQuantity");
        Label lblClub = (Label) cardNode.lookup("#lblClub");
        Button btnDetails = (Button) cardNode.lookup("#btnDetails");
        Button btnAddToCart = (Button) cardNode.lookup("#btnAddToCart");

        // Configurer les données
        lblNom.setText(produit.getNomProd());

        // Tronquer la description si elle est trop longue
        String description = produit.getDescProd();
        if (description != null) {
            lblDescription.setText(description.length() > 100
                    ? description.substring(0, 97) + "..."
                    : description);
        } else {
            lblDescription.setText("");
        }

        lblPrix.setText(String.format("%.2f €", produit.getPrix()));
        lblQuantity.setText("Stock: " + produit.getQuantity());

        // Safely handle null club or null club name
        if (produit.getClub() != null && produit.getClub().getNom() != null) {
            lblClub.setText(produit.getClub().getNom().toUpperCase());
        } else {
            lblClub.setText("");
        }

        // Set a blank image by default
        imgProduct.setImage(null);

        // Try to load the product image
        try {
            String imagePath = produit.getImgProd();
            if (imagePath != null && !imagePath.isEmpty()) {
                // Try first as a resource
                URL imageUrl = getClass().getResource("/" + imagePath);

                if (imageUrl != null) {
                    Image image = new Image(imageUrl.toString(), true); // Use background loading
                    imgProduct.setImage(image);
                } else {
                    // Then try as a file path
                    File file = new File(imagePath);
                    if (file.exists()) {
                        Image image = new Image(file.toURI().toString(), true);
                        imgProduct.setImage(image);
                    } else {
                        System.out.println("Image not found: " + imagePath);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Leave the image as null if loading fails
        }

        // Configurer les boutons d'action
        btnDetails.setOnAction(e -> viewProductDetails(produit));
        btnAddToCart.setOnAction(e -> addToCart(produit));
    }

    /**
     * Affiche les détails d'un produit
     */
    private void viewProductDetails(Produit produit) {
        try {
            // Stocker l'ID du produit sélectionné dans un singleton ou une classe d'état
            ProduitDetailsController.setSelectedProduit(produit);

            // Naviguer vers la vue de détails
            ProduitApp.navigateTo("/com/esprit/views/produit/ProduitDetailsView.fxml");
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("Erreur", "Erreur lors de l'affichage des détails", e.getMessage());
        }
    }

    /**
     * Ajoute un produit au panier
     */
    private void addToCart(Produit produit) {
        // TODO: Implémenter la gestion du panier
        AlertUtils.showInfo("Panier", "Produit ajouté",
                "Le produit \"" + produit.getNomProd() + "\" a été ajouté au panier.");
    }

    /**
     * Recherche des produits
     */
    @FXML
    private void searchProducts() {
        filterProducts();
    }

    /**
     * Réinitialise les filtres
     */
    @FXML
    private void resetFilters() {
        txtSearch.clear();
        comboFilterClub.getSelectionModel().selectFirst();
        filterProducts();
    }

    /**
     * Navigue vers la page du panier
     */
    @FXML
    private void voirPanier() {
        // TODO: Implémenter la navigation vers le panier
        AlertUtils.showInfo("Navigation", "Panier", "La vue du panier sera implémentée prochainement.");
    }

    /**
     * Navigue vers l'interface d'administration
     */
    @FXML
    private void goToAdmin() {
        ProduitApp.navigateTo("/com/esprit/views/produit/AdminProduitView.fxml");
    }

    /**
     * Displays a toast-like message using an Alert
     * @param message The message to display
     * @param type The type of message ("success", "error", "info")
     */
    private void showToast(String message, String type) {
        Alert alert;
        switch (type) {
            case "error":
                alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                break;
            case "info":
                alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Information");
                break;
            default: // success
                alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Succès");
                break;
        }
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
package com.esprit.controllers;

import com.esprit.ProduitApp;
import com.esprit.models.Club;
import com.esprit.models.Commande;
import com.esprit.models.Orderdetails;
import com.esprit.models.Produit;
import com.esprit.models.enums.StatutCommandeEnum;
import com.esprit.services.ClubService;
import com.esprit.services.CommandeService;
import com.esprit.services.ProduitService;
import com.esprit.utils.AlertUtilsSirine;
import com.esprit.MainApp;
import com.esprit.models.User;
import com.esprit.utils.SessionManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.geometry.Pos;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
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
    @FXML private Label cartItemCount;
    
    // Navbar components
    @FXML private StackPane userProfileContainer;
    @FXML private ImageView userProfilePic;
    @FXML private Label userNameLabel;
    @FXML private VBox profileDropdown;
    @FXML private StackPane clubsContainer;
    @FXML private VBox clubsDropdown;

    private final ProduitService produitService;
    private final ClubService clubService;
    private List<Produit> allProduits = new ArrayList<>();
    private List<Produit> filteredProducts = new ArrayList<>(); // To store filtered products
    // Pagination variables
    private int currentPage = 1;
    private static final int ITEMS_PER_PAGE = 2; // Number of products per page (adjust as needed)
    private int totalPages = 1;
    
    private User currentUser;

    public ProduitViewController() {
        this.produitService = ProduitService.getInstance();
        this.clubService = ClubService.getInstance();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Get current user from session
        currentUser = SessionManager.getInstance().getCurrentUser();
        
        // Show Add Product button only for PRESIDENT_CLUB role
        if (currentUser != null && "PRESIDENT_CLUB".equals(currentUser.getRole())) {
            btnAddProduct.setVisible(true);
        } else {
            btnAddProduct.setVisible(false);
        }
        
        setupClubFilter();
        loadAllProduits();
        
        if (currentUser != null && userNameLabel != null) {
            // Set user name
            userNameLabel.setText(currentUser.getFirstName() + " " + currentUser.getLastName());

            // Load profile picture if available
            String profilePicture = currentUser.getProfilePicture();
            if (profilePicture != null && !profilePicture.isEmpty() && userProfilePic != null) {
                try {
                    File imageFile = new File("uploads/profiles/" + profilePicture);
                    if (imageFile.exists()) {
                        Image image = new Image(imageFile.toURI().toString());
                        userProfilePic.setImage(image);
                    } else {
                        loadDefaultProfilePic();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    loadDefaultProfilePic();
                }
            } else {
                loadDefaultProfilePic();
            }

            // Apply circular clip to profile picture if exists
            if (userProfilePic != null) {
                double radius = 22.5;
                userProfilePic.setClip(new javafx.scene.shape.Circle(radius, radius, radius));
            }
        }
        
        // Initially hide the dropdowns
        if (profileDropdown != null) {
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
        }
        
        if (clubsDropdown != null) {
            clubsDropdown.setVisible(false);
            clubsDropdown.setManaged(false);
        }
        
        // Update cart badge count
        updateCartBadge();
    }
    
    private void loadDefaultProfilePic() {
        if (userProfilePic != null) {
            try {
                Image defaultImage = new Image(getClass().getResourceAsStream("/com/esprit/images/default-profile.png"));
                userProfilePic.setImage(defaultImage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    // Navigation Methods for the Navbar
    
    @FXML
    public void navigateToHome() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/home.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) productContainer.getScene().getWindow();
        stage.setScene(new Scene(root));
    }
    
    @FXML
    public void navigateToPolls() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/SondageView.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) productContainer.getScene().getWindow();
        stage.setScene(new Scene(root));
    }
    
    @FXML
    public void navigateToClubs() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/ShowClubs.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) productContainer.getScene().getWindow();
        stage.setScene(new Scene(root));
    }
    
    @FXML
    public void navigateToEvents() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/AfficherEvent.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) productContainer.getScene().getWindow();
        stage.setScene(new Scene(root));
    }
    
    @FXML
    public void navigateToCompetition() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/UserCompetition.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) productContainer.getScene().getWindow();
        stage.setScene(new Scene(root));
    }
    
    @FXML
    private void showClubsDropdown() {
        if (clubsDropdown != null) {
            clubsDropdown.setVisible(true);
            clubsDropdown.setManaged(true);
        }
    }
    
    @FXML
    private void hideClubsDropdown() {
        if (clubsDropdown != null) {
            clubsDropdown.setVisible(false);
            clubsDropdown.setManaged(false);
        }
    }
    
    @FXML
    private void navigateToMyClub() throws IOException {
        // This would navigate to the user's club page
        // For now, just navigate to clubs
        navigateToClubs();
    }
    
    @FXML
    private void showProfileDropdown() {
        if (profileDropdown != null) {
            profileDropdown.setVisible(true);
            profileDropdown.setManaged(true);
        }
    }
    
    @FXML
    private void hideProfileDropdown() {
        if (profileDropdown != null) {
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
        }
    }
    
    @FXML
    private void navigateToProfile() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Profile.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }
    
    @FXML
    private void handleLogout() throws IOException {
        // Clear the session
        SessionManager.getInstance().clearSession();

        // Navigate to login page
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }
    
    @FXML
    private void navigateToContact() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Contact.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    /**
     * Configure le filtre par club
     */
    private void setupClubFilter() {
        // Ajouter l'option "Tous les clubs"
        Club allClubsOption = new Club();
        allClubsOption.setId(-1);
        allClubsOption.setNomC("Tous les clubs");

        List<Club> clubs = clubService.getAll();
        clubs.add(0, allClubsOption); // Ajouter en première position

        comboFilterClub.getItems().setAll(clubs);
        comboFilterClub.setCellFactory(cell -> new ListCell<Club>() {
            @Override
            protected void updateItem(Club club, boolean empty) {
                super.updateItem(club, empty);
                setText(empty || club == null ? "" : club.getNomC());
            }
        });
        comboFilterClub.setButtonCell(new ListCell<Club>() {
            @Override
            protected void updateItem(Club club, boolean empty) {
                super.updateItem(club, empty);
                setText(empty || club == null ? "" : club.getNomC());
            }
        });

        comboFilterClub.getSelectionModel().selectFirst();

        // Ajouter un écouteur de sélection
        comboFilterClub.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                filterProducts();
            }
        });
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
        List<Club> clubs = clubService.getAll();
        clubComboBox.setItems(FXCollections.observableArrayList(clubs));
        clubComboBox.setCellFactory(cell -> new ListCell<Club>() {
            @Override
            protected void updateItem(Club club, boolean empty) {
                super.updateItem(club, empty);
                setText(empty || club == null ? "" : club.getNomC());
            }
        });
        clubComboBox.setButtonCell(new ListCell<Club>() {
            @Override
            protected void updateItem(Club club, boolean empty) {
                super.updateItem(club, empty);
                setText(empty || club == null ? "" : club.getNomC());
            }
        });

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
                        AlertUtilsSirine.showError("Erreur", "Nom invalide", "Le nom du produit est obligatoire.");
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
                        AlertUtilsSirine.showError("Erreur", "Prix invalide",
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
                    AlertUtilsSirine.showError("Erreur", "Erreur lors de la création du produit", ex.getMessage());
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
                AlertUtilsSirine.showError("Erreur", "Erreur lors de l'enregistrement du produit", ex.getMessage());
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
            AlertUtilsSirine.showError("Erreur", "Erreur lors du chargement des produits", e.getMessage());
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
                AlertUtilsSirine.showError("Erreur", "Erreur lors de l'affichage des produits", e.getMessage());
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
        Button prevButton = new Button("«");
        prevButton.setStyle(currentPage == 1 ?
                "-fx-background-color: #E0E0E0; -fx-text-fill: #999999; -fx-background-radius: 50; -fx-min-width: 36; -fx-min-height: 36; -fx-max-width: 36; -fx-max-height: 36; -fx-font-weight: bold;" :
                "-fx-background-color: #6200EE; -fx-text-fill: white; -fx-background-radius: 50; -fx-min-width: 36; -fx-min-height: 36; -fx-max-width: 36; -fx-max-height: 36; -fx-font-weight: bold;");
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
                pageButton.setStyle("-fx-font-weight: bold; -fx-background-color: #6200EE; -fx-text-fill: white; -fx-background-radius: 50; -fx-min-width: 36; -fx-min-height: 36; -fx-max-width: 36; -fx-max-height: 36;");
            } else {
                pageButton.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #333333; -fx-background-radius: 50; -fx-min-width: 36; -fx-min-height: 36; -fx-max-width: 36; -fx-max-height: 36;");
            }

            final int pageNum = i;
            pageButton.setOnAction(e -> {
                currentPage = pageNum;
                updatePagination();
            });
            paginationContainer.getChildren().add(pageButton);
        }

        // Next button
        Button nextButton = new Button("»");
        nextButton.setStyle(currentPage == totalPages ?
                "-fx-background-color: #E0E0E0; -fx-text-fill: #999999; -fx-background-radius: 50; -fx-min-width: 36; -fx-min-height: 36; -fx-max-width: 36; -fx-max-height: 36; -fx-font-weight: bold;" :
                "-fx-background-color: #6200EE; -fx-text-fill: white; -fx-background-radius: 50; -fx-min-width: 36; -fx-min-height: 36; -fx-max-width: 36; -fx-max-height: 36; -fx-font-weight: bold;");
        nextButton.setDisable(currentPage == totalPages);
        nextButton.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                updatePagination();
            }
        });

        paginationContainer.getChildren().add(nextButton);

        // Add page count information
        Label pageInfoLabel = new Label(String.format("Page %d of %d", currentPage, totalPages));
        pageInfoLabel.setStyle("-fx-text-fill: #6c757d; -fx-padding: 0 0 0 10;");
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

        // Style the details button
        String detailsStyle = "-fx-background-color: #03DAC5; -fx-text-fill: white; -fx-font-weight: bold; " +
                            "-fx-background-radius: 20; -fx-padding: 8 15; -fx-cursor: hand;";
        btnDetails.setStyle(detailsStyle);
        
        // Style the add to cart button
        String cartStyle = "-fx-background-color: #018786; -fx-text-fill: white; -fx-font-weight: bold; " +
                          "-fx-background-radius: 20; -fx-padding: 8 15; -fx-cursor: hand;";
        btnAddToCart.setStyle(cartStyle);

        // Add hover effects
        btnDetails.setOnMouseEntered(e -> btnDetails.setStyle(detailsStyle + "-fx-background-color: #018786;"));
        btnDetails.setOnMouseExited(e -> btnDetails.setStyle(detailsStyle));
        
        btnAddToCart.setOnMouseEntered(e -> btnAddToCart.setStyle(cartStyle + "-fx-background-color: #01635E;"));
        btnAddToCart.setOnMouseExited(e -> btnAddToCart.setStyle(cartStyle));

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

        if (produit.getClub() != null && produit.getClub().getNomC() != null) {
            lblClub.setText(produit.getClub().getNomC().toUpperCase());
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

        // Configure button actions
        btnDetails.setOnAction(e -> viewProductDetails(produit));
        btnAddToCart.setOnAction(e -> addToCart(produit));
    }

    private void viewProductDetails(Produit produit) {
        try {
            // Stocker l'ID du produit sélectionné dans un singleton ou une classe d'état
            ProduitDetailsController.setSelectedProduit(produit);

            // Naviguer vers la vue de détails
            ProduitApp.navigateTo("/com/esprit/views/produit/ProduitDetailsView.fxml");
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtilsSirine.showError("Erreur", "Erreur lors de l'affichage des détails", e.getMessage());
        }
    }

    private void addToCart(Produit produit) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            AlertUtilsSirine.showError("Authentication Required", "Please log in", "You must be logged in to add items to your cart.");
            return;
        }

        try {
            // Create order detail for the cart
            CommandeService commandeService = new CommandeService();
            
            // Check if there's an existing cart (order with EN_COURS status)
            Commande existingCart = commandeService.getCartForUser(currentUser.getId());
            
            if (existingCart == null) {
                // Create a new shopping cart
                existingCart = new Commande();
                existingCart.setDateComm(LocalDate.now());
                existingCart.setStatut(StatutCommandeEnum.EN_COURS);
                existingCart.setUser(currentUser);
                commandeService.createCommande(existingCart);
            }
            
            // Check if product is already in cart
            boolean productFound = false;
            for (Orderdetails detail : existingCart.getOrderDetails()) {
                if (detail.getProduit().getId() == produit.getId()) {
                    // Product already in cart, increase quantity
                    detail.setQuantity(detail.getQuantity() + 1);
                    detail.calculateTotal();
                    commandeService.updateCommande(existingCart);
                    productFound = true;
                    break;
                }
            }
            
            if (!productFound) {
                // Add new product to cart
                Orderdetails detail = new Orderdetails();
                detail.setProduit(produit);
                detail.setQuantity(1);
                detail.setPrice((int) produit.getPrix());
                detail.calculateTotal();
                
                existingCart.getOrderDetails().add(detail);
                commandeService.updateCommande(existingCart);
            }
            
            // Update cart count badge
            updateCartBadge();
            
            // Show success message with animation
            showAddToCartSuccess();
            
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtilsSirine.showError("Error", "Failed to add to cart", "There was an error adding the product to your cart: " + e.getMessage());
        }
    }
    
    /**
     * Navigate to the cart/order view
     */
    @FXML
    public void navigateToCart() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            AlertUtilsSirine.showError("Authentication Required", "Please log in", "You must be logged in to view your cart.");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/produit/CommandeView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) productContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtilsSirine.showError("Error", "Navigation Failed", "Failed to navigate to cart: " + e.getMessage());
        }
    }
    
    /**
     * Update the cart badge count
     */
    private void updateCartBadge() {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null || cartItemCount == null) return;
            
            CommandeService commandeService = new CommandeService();
            Commande cart = commandeService.getCartForUser(currentUser.getId());
            
            if (cart == null) {
                cartItemCount.setText("0");
                cartItemCount.setVisible(false);
                return;
            }
            
            int itemCount = cart.getOrderDetails().size();
            cartItemCount.setText(String.valueOf(itemCount));
            cartItemCount.setVisible(itemCount > 0);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Show a nice animation when product is added to cart
     */
    private void showAddToCartSuccess() {
        Alert success = new Alert(Alert.AlertType.INFORMATION);
        success.setTitle("Added to Cart");
        success.setHeaderText("Product added to your cart!");
        success.setContentText("You can view your cart by clicking the cart icon in the top right corner.");
        
        DialogPane successPane = success.getDialogPane();
        successPane.setStyle("-fx-background-color: white;");
        
        Button okBtn = (Button) success.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setStyle("-fx-background-color: #018786; -fx-text-fill: white; -fx-font-weight: bold;");
        
        success.show();
        
        // Auto-close after 1.5 seconds
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                Platform.runLater(() -> success.close());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
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
package com.esprit.controllers;

import com.esprit.MainApp;
import com.esprit.models.Commande;
import com.esprit.models.Orderdetails;
import com.esprit.models.Produit;
import com.esprit.models.User;
import com.esprit.models.enums.StatutCommandeEnum;
import com.esprit.services.CommandeService;
import com.esprit.utils.AlertUtilsSirine;
import com.esprit.utils.SessionManager;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class CommandeViewController implements Initializable {

    @FXML private Label userNameLabel;
    @FXML private ImageView userProfilePic;
    @FXML private VBox profileDropdown;
    @FXML private StackPane userProfileContainer;
    
    @FXML private VBox emptyCartState;
    @FXML private VBox cartWithItemsState;
    @FXML private TableView<Orderdetails> cartItemsTable;
    @FXML private TableColumn<Orderdetails, ImageView> productImageCol;
    @FXML private TableColumn<Orderdetails, String> productNameCol;
    @FXML private TableColumn<Orderdetails, Double> unitPriceCol;
    @FXML private TableColumn<Orderdetails, Integer> quantityCol;
    @FXML private TableColumn<Orderdetails, Double> totalPriceCol;
    @FXML private TableColumn<Orderdetails, Void> actionsCol;
    
    @FXML private Label subtotalLabel;
    @FXML private Label shippingLabel;
    @FXML private Label totalLabel;
    @FXML private Button checkoutButton;
    
    private CommandeService commandeService;
    private Commande currentCart;
    private User currentUser;
    
    public CommandeViewController() {
        this.commandeService = new CommandeService();
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Get current user
        currentUser = SessionManager.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            try {
                AlertUtilsSirine.showError("Authentication Required", "Please log in", "You must be logged in to view your cart.");
                navigateToHome();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // Setup user profile section
        if (userNameLabel != null && currentUser != null) {
            userNameLabel.setText(currentUser.getFirstName() + " " + currentUser.getLastName());
            
            // Load profile picture if available
            if (userProfilePic != null) {
                String profilePicture = currentUser.getProfilePicture();
                if (profilePicture != null && !profilePicture.isEmpty()) {
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
                
                // Apply circular clip to profile picture
                double radius = 22.5;
                userProfilePic.setClip(new javafx.scene.shape.Circle(radius, radius, radius));
            }
        }
        
        // Initially hide the dropdown
        if (profileDropdown != null) {
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
        }
        
        // Setup table columns
        setupTableColumns();
        
        // Load cart data
        loadCartData();
    }
    
    private void loadDefaultProfilePic() {
        try {
            Image defaultImage = new Image(getClass().getResourceAsStream("/com/esprit/images/default-profile.png"));
            userProfilePic.setImage(defaultImage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void setupTableColumns() {
        // Product image column
        productImageCol.setCellValueFactory(param -> {
            Produit produit = param.getValue().getProduit();
            ImageView imageView = new ImageView();
            imageView.setFitHeight(50);
            imageView.setFitWidth(50);
            imageView.setPreserveRatio(true);
            
            try {
                String imagePath = produit.getImgProd();
                if (imagePath != null && !imagePath.isEmpty()) {
                    // Try first as a resource
                    URL imageUrl = getClass().getResource("/" + imagePath);
                    if (imageUrl != null) {
                        Image image = new Image(imageUrl.toString(), true);
                        imageView.setImage(image);
                    } else {
                        // Then try as a file path
                        File file = new File(imagePath);
                        if (file.exists()) {
                            Image image = new Image(file.toURI().toString(), true);
                            imageView.setImage(image);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            return javafx.beans.binding.Bindings.createObjectBinding(() -> imageView);
        });
        
        // Product name column
        productNameCol.setCellValueFactory(param -> 
            new SimpleStringProperty(param.getValue().getProduit().getNomProd()));
        
        // Unit price column
        unitPriceCol.setCellValueFactory(param -> 
            new SimpleDoubleProperty(param.getValue().getPrice()).asObject());
        unitPriceCol.setCellFactory(tc -> new TableCell<Orderdetails, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(String.format("€%.2f", price));
                }
            }
        });
        
        // Quantity column with editable spinner
        quantityCol.setCellValueFactory(param -> 
            new SimpleIntegerProperty(param.getValue().getQuantity()).asObject());
        quantityCol.setCellFactory(tc -> new TableCell<Orderdetails, Integer>() {
            private Spinner<Integer> spinner;
            
            @Override
            protected void updateItem(Integer quantity, boolean empty) {
                super.updateItem(quantity, empty);
                
                if (empty) {
                    setGraphic(null);
                } else {
                    spinner = new Spinner<>(1, 100, quantity);
                    spinner.setEditable(true);
                    spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
                        Orderdetails item = getTableRow().getItem();
                        item.setQuantity(newValue);
                        item.calculateTotal();
                        updateCart();
                    });
                    
                    setGraphic(spinner);
                }
            }
        });
        
        // Total price column
        totalPriceCol.setCellValueFactory(param -> 
            new SimpleDoubleProperty(param.getValue().getTotal()).asObject());
        totalPriceCol.setCellFactory(tc -> new TableCell<Orderdetails, Double>() {
            @Override
            protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(String.format("€%.2f", total));
                }
            }
        });
        
        // Actions column
        actionsCol.setCellFactory(createActionsColumn());
    }
    
    private Callback<TableColumn<Orderdetails, Void>, TableCell<Orderdetails, Void>> createActionsColumn() {
        return new Callback<>() {
            @Override
            public TableCell<Orderdetails, Void> call(final TableColumn<Orderdetails, Void> param) {
                return new TableCell<>() {
                    private final Button removeButton = new Button("Remove");
                    
                    {
                        removeButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-background-radius: 5;");
                        
                        removeButton.setOnAction(event -> {
                            Orderdetails item = getTableRow().getItem();
                            if (item != null) {
                                currentCart.getOrderDetails().remove(item);
                                updateCart();
                            }
                        });
                    }
                    
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox buttons = new HBox(10, removeButton);
                            buttons.setAlignment(Pos.CENTER);
                            setGraphic(buttons);
                        }
                    }
                };
            }
        };
    }
    
    private void loadCartData() {
        try {
            if (currentUser == null) return;
            
            currentCart = commandeService.getCartForUser(currentUser.getId());
            
            if (currentCart == null || currentCart.getOrderDetails().isEmpty()) {
                // Show empty cart state
                emptyCartState.setVisible(true);
                emptyCartState.setManaged(true);
                cartWithItemsState.setVisible(false);
                cartWithItemsState.setManaged(false);
            } else {
                // Show cart with items
                emptyCartState.setVisible(false);
                emptyCartState.setManaged(false);
                cartWithItemsState.setVisible(true);
                cartWithItemsState.setManaged(true);
                
                // Populate table with cart items
                cartItemsTable.setItems(FXCollections.observableArrayList(currentCart.getOrderDetails()));
                
                // Update summary
                updateSummary();
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtilsSirine.showError("Error", "Failed to load cart", "There was an error loading your cart: " + e.getMessage());
        }
    }
    
    private void updateCart() {
        try {
            if (currentCart.getOrderDetails().isEmpty()) {
                // If cart is empty after removing item, show empty state
                commandeService.supprimerCommande(currentCart.getId());
                loadCartData();
                return;
            }
            
            // Update cart in database
            commandeService.updateCommande(currentCart);
            
            // Refresh the table
            cartItemsTable.refresh();
            
            // Update summary
            updateSummary();
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtilsSirine.showError("Error", "Failed to update cart", "There was an error updating your cart: " + e.getMessage());
        }
    }
    
    private void updateSummary() {
        double subtotal = 0.0;
        for (Orderdetails item : currentCart.getOrderDetails()) {
            subtotal += item.getTotal();
        }
        
        // Fixed shipping cost
        double shipping = 5.99;
        
        // Total
        double total = subtotal + shipping;
        
        // Update labels
        subtotalLabel.setText(String.format("€%.2f", subtotal));
        shippingLabel.setText(String.format("€%.2f", shipping));
        totalLabel.setText(String.format("€%.2f", total));
    }
    
    @FXML
    public void proceedToCheckout() {
        try {
            if (currentCart == null || currentCart.getOrderDetails().isEmpty()) {
                AlertUtilsSirine.showError("Empty Cart", "Your cart is empty", "Please add products to your cart before proceeding to checkout.");
                return;
            }
            
            // Update the cart status to CONFIRMEE (confirmed)
            currentCart.setStatut(StatutCommandeEnum.CONFIRMEE);
            commandeService.updateCommande(currentCart);
            
            // Show success message
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Order Confirmed");
            success.setHeaderText("Thank you for your order!");
            success.setContentText("Your order has been successfully placed.\nOrder ID: " + currentCart.getId());
            
            DialogPane successPane = success.getDialogPane();
            successPane.setStyle("-fx-background-color: white;");
            
            Button okBtn = (Button) success.getDialogPane().lookupButton(ButtonType.OK);
            okBtn.setStyle("-fx-background-color: #018786; -fx-text-fill: white; -fx-font-weight: bold;");
            
            success.showAndWait();
            
            // Navigate back to products page
            navigateToProducts();
            
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtilsSirine.showError("Error", "Checkout Failed", "Failed to complete checkout: " + e.getMessage());
        }
    }
    
    // Navbar navigation methods
    @FXML
    public void showProfileDropdown() {
        if (profileDropdown != null) {
            profileDropdown.setVisible(true);
            profileDropdown.setManaged(true);
        }
    }
    
    @FXML
    public void hideProfileDropdown() {
        if (profileDropdown != null) {
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
        }
    }
    
    @FXML
    public void navigateToHome() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/home.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userNameLabel.getScene().getWindow();
        stage.setScene(new Scene(root));
    }
    
    @FXML
    public void navigateToPolls() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/SondageView.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userNameLabel.getScene().getWindow();
        stage.setScene(new Scene(root));
    }
    
    @FXML
    public void navigateToClubs() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/ShowClubs.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userNameLabel.getScene().getWindow();
        stage.setScene(new Scene(root));
    }
    
    @FXML
    public void navigateToEvents() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/AfficherEvent.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userNameLabel.getScene().getWindow();
        stage.setScene(new Scene(root));
    }
    
    @FXML
    public void navigateToProducts() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/produit/ProduitView.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userNameLabel.getScene().getWindow();
        stage.setScene(new Scene(root));
    }
    
    @FXML
    public void navigateToCompetition() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/UserCompetition.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userNameLabel.getScene().getWindow();
        stage.setScene(new Scene(root));
    }
    
    @FXML
    public void navigateToProfile() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Profile.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userNameLabel.getScene().getWindow();
        stage.setScene(new Scene(root));
    }
    
    @FXML
    public void navigateToContact() throws IOException {
        // Implement when contact page is available
    }
    
    @FXML
    public void handleLogout() throws IOException {
        SessionManager.getInstance().clearSession();
        navigateToHome();
    }
} 
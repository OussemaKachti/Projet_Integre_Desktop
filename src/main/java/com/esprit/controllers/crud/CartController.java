package com.esprit.controllers.crud;
import com.esprit.controllers.ProduitCardItemController;
import com.esprit.models.Produit;
import com.esprit.services.ProduitService;
import com.esprit.utils.AlertUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.sql.SQLException;
import java.util.Map;
import java.util.ResourceBundle;

public class CartController implements Initializable {

    @FXML private TableView<CartItem> tableView;
    @FXML private TableColumn<CartItem, ImageView> colImage;
    @FXML private TableColumn<CartItem, String> colName;
    @FXML private TableColumn<CartItem, Double> colPrice;
    @FXML private TableColumn<CartItem, Integer> colQuantity;
    @FXML private TableColumn<CartItem, Void> colActions;
    @FXML private Label lblTotal;

    private final ProduitService produitService = ProduitService.getInstance();


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        loadCartItems();
        calculateTotal();
    }

    private void setupTableColumns() {
        colImage.setCellValueFactory(new PropertyValueFactory<>("imageView"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnRemove = new Button("Remove");
            private final Button btnIncrease = new Button("+");
            private final Button btnDecrease = new Button("-");
            private final HBox container = new HBox(5, btnDecrease, btnRemove, btnIncrease);

            {
                // Style buttons
                btnRemove.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white;");
                btnIncrease.setStyle("-fx-background-color: #00C851; -fx-text-fill: white;");
                btnDecrease.setStyle("-fx-background-color: #ffbb33; -fx-text-fill: white;");

                container.setAlignment(Pos.CENTER);

                // Set actions
                btnRemove.setOnAction(event -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    removeFromCart(item);
                });

                btnIncrease.setOnAction(event -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    updateQuantity(item, 1);
                });

                btnDecrease.setOnAction(event -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    updateQuantity(item, -1);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void loadCartItems() {
        Map<Integer, Integer> cart = ProduitCardItemController.getCart();
        tableView.getItems().clear();

        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            try {
                Produit produit = produitService.getProduitById(entry.getKey());
                if (produit != null) {
                    ImageView imageView = new ImageView();
                    try {
                        URL imageUrl = getClass().getResource("/" + produit.getImgProd());
                        if (imageUrl != null) {
                            imageView.setImage(new Image(imageUrl.toString()));
                            imageView.setFitWidth(50);
                            imageView.setFitHeight(50);
                            imageView.setPreserveRatio(true);
                        }
                    } catch (Exception e) {
                        // Use default image if error occurs
                        imageView.setImage(new Image(getClass().getResourceAsStream("/images/default-product.png")));
                    }

                    tableView.getItems().add(new CartItem(
                            imageView,
                            produit.getNomProd(),
                            produit.getPrix(),
                            entry.getValue(),
                            produit.getId()
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
                // Continue with next item even if one fails
            }
        }
    }

    private void removeFromCart(CartItem item) {
        ProduitCardItemController.getCart().remove(item.getProductId());
        loadCartItems();
        calculateTotal();
    }

    private void updateQuantity(CartItem item, int change) {
        Map<Integer, Integer> cart = ProduitCardItemController.getCart();
        int newQuantity = cart.get(item.getProductId()) + change;

        if (newQuantity <= 0) {
            removeFromCart(item);
        } else {
            try {
                Produit produit = produitService.getProduitById(item.getProductId());
                int availableStock = Integer.parseInt(produit.getQuantity());

                if (newQuantity > availableStock) {
                    AlertUtils.showError("Erreur", "Stock insuffisant",
                            "Quantité demandée non disponible en stock.");
                    return;
                }

                cart.put(item.getProductId(), newQuantity);
                loadCartItems();
                calculateTotal();
            } catch (SQLException e) {
                e.printStackTrace();
                AlertUtils.showError("Erreur", "Database Error", "Could not update quantity");
            }
        }
    }

    private void calculateTotal() {
        double total = 0.0;
        for (CartItem item : tableView.getItems()) {
            total += item.getPrice() * item.getQuantity();
        }
        lblTotal.setText(String.format("Total: %.2f TND", total));
    }

    @FXML
    private void proceedToCheckout() {
        if (tableView.getItems().isEmpty()) {
            AlertUtils.showError("Erreur", "Panier vide", "Votre panier est vide.");
            return;
        }

        // Implement your checkout logic here
        // For example:
        // ProduitApp.navigateTo("/com/esprit/views/checkout/CheckoutView.fxml");
    }

    // Helper class to represent cart items in the table
    public static class CartItem {
        private final ImageView imageView;
        private final String name;
        private final double price;
        private int quantity;
        private final int productId;

        public CartItem(ImageView imageView, String name, double price, int quantity, int productId) {
            this.imageView = imageView;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
            this.productId = productId;
        }

        // Getters
        public ImageView getImageView() { return imageView; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public int getQuantity() { return quantity; }
        public int getProductId() { return productId; }
    }
}
package com.esprit.controllers.crud;

import com.esprit.controllers.ProduitCardItemController;
import com.esprit.models.Commande;
import com.esprit.models.Orderdetails;
import com.esprit.models.Produit;
import com.esprit.models.User;
import com.esprit.services.UserService;
import com.esprit.models.enums.StatutCommandeEnum;
import com.esprit.services.CommandeService;
import com.esprit.services.ProduitService;
import com.esprit.utils.AlertUtilsSirine;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

import javafx.geometry.Insets;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public class CartController implements Initializable {

    @FXML private TableView<CartItem> tableView;
    @FXML private TableColumn<CartItem, ImageView> colImage;
    @FXML private TableColumn<CartItem, String> colName;
    @FXML private TableColumn<CartItem, Double> colPrice;
    @FXML private TableColumn<CartItem, Integer> colQuantity;
    @FXML private TableColumn<CartItem, Void> colActions;
    @FXML private Label lblTotal;

    private final ProduitService produitService = ProduitService.getInstance();
    private final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    //private User currentUser = new User(1); // Replace with actual current user from session/login

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        try {
            loadCartItems();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        updateTotalLabel();

        // Quantity edit directly in table
        tableView.setEditable(true);
        colQuantity.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colQuantity.setOnEditCommit(event -> {
            CartItem item = event.getRowValue();
            int newValue = event.getNewValue();
            if (newValue > 0) {
                item.setQuantity(newValue);
                updateCartInController();
                updateTotalLabel();
            }
        });
    }

    private void setupTableColumns() {
        colImage.setCellValueFactory(new PropertyValueFactory<>("imageView"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        tableView.setItems(cartItems);

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnRemove = new Button("Remove");
            private final Button btnIncrease = new Button("+");
            private final Button btnDecrease = new Button("-");
            private final HBox container = new HBox(5, btnDecrease, btnRemove, btnIncrease);

            {
                btnRemove.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white;");
                btnIncrease.setStyle("-fx-background-color: #00C851; -fx-text-fill: white;");
                btnDecrease.setStyle("-fx-background-color: #ffbb33; -fx-text-fill: white;");
                container.setAlignment(Pos.CENTER);

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

    private void loadCartItems() throws SQLException {
        cartItems.clear();
        Map<Integer, Integer> cart = ProduitCardItemController.getCart();

        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            int productId = entry.getKey();
            int quantity = entry.getValue();

            Produit produit = produitService.getProduitById(productId);
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
                    imageView.setImage(new Image(getClass().getResourceAsStream("/images/default-product.png")));
                }

                CartItem item = new CartItem();
                item.setProductId(productId);
                item.setName(produit.getNomProd());
                item.setQuantity(quantity);
                item.setPrice((int) produit.getPrix());
                item.setImageView(imageView);
                item.calculateTotal();

                cartItems.add(item);
            }
        }
    }

    private void updateQuantity(CartItem item, int change) {
        int newQuantity = item.getQuantity() + change;
        if (newQuantity <= 0) {
            removeFromCart(item);
        } else {
            item.setQuantity(newQuantity);
            updateCartInController();
            updateTotalLabel();
        }
    }

    private void removeFromCart(CartItem item) {
        cartItems.remove(item);
        updateCartInController();
        updateTotalLabel();
    }

    private void updateTotalLabel() {
        lblTotal.setText(String.format("Total: %.2f TND", calculateTotalAmount()));
    }

    private double calculateTotalAmount() {
        return cartItems.stream().mapToDouble(CartItem::getTotal).sum();
    }

    private void updateCartInController() {
        Map<Integer, Integer> updatedCart = new HashMap<>();
        for (CartItem item : cartItems) {
            updatedCart.put(item.getProductId(), item.getQuantity());
        }
        ProduitCardItemController.updateCart(updatedCart);
    }

    @FXML
    public void proceedToCheckout() {
        try {
            if (cartItems.isEmpty()) {
                AlertUtilsSirine.showError("Erreur", "Panier vide", "Votre panier est vide.");
                return;
            }
            UserService userService = UserService.getInstance();
            User staticUser = userService.getById(1);  // make sure this ID exists in your DB

            if (staticUser == null) {
                AlertUtilsSirine.showError("Erreur", "Utilisateur non trouvé", "L'utilisateur statique n'existe pas.");
                return;
            }

            Commande commande = new Commande();
            commande.setDateComm(LocalDate.now());
            commande.setStatut(StatutCommandeEnum.EN_COURS);
            commande.setUser(staticUser);

            CommandeService commandeService = new CommandeService();


            for (CartItem item : cartItems) {
                Produit produit = produitService.getProduitById(item.getProductId());

                Orderdetails detail = new Orderdetails();
                detail.setProduit(produit);
                detail.setQuantity(item.getQuantity());
                detail.setPrice(item.getPrice());
                detail.calculateTotal();

                commande.getOrderDetails().add(detail);

                int newStock = Integer.parseInt(produit.getQuantity()) - item.getQuantity();
                produit.setQuantity(String.valueOf(newStock));
                produitService.updateProduit(produit);
            }
            commandeService.createCommande(commande);

            cartItems.clear();
            ProduitCardItemController.updateCart(new HashMap<>());
            updateTotalLabel();

            showSuccessPopup(commande.getId());
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtilsSirine.showError("Erreur", "Erreur lors de la commande", e.getMessage());
        }
    }
    /**
     * Shows a custom success popup with an option to return to the catalog
     * @param commandeId The ID of the created order
     */
    private void showSuccessPopup(int commandeId) {
        try {
            // Create a new dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Commande Réussie");
            dialog.setHeaderText("Votre commande a été créée avec succès!");

            // Create custom content for the dialog
            VBox contentBox = new VBox(10);
            contentBox.setPadding(new Insets(20, 20, 20, 20));
            contentBox.setAlignment(Pos.CENTER);

            // Success message
            Label messageLabel = new Label("Commande #" + commandeId + " créée avec succès");
            messageLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            Label detailLabel = new Label("Merci pour votre achat!");
            detailLabel.setStyle("-fx-font-size: 14px;");

            // Add components to the content box
            contentBox.getChildren().addAll(messageLabel, detailLabel);

            // Set the content for the dialog
            dialog.getDialogPane().setContent(contentBox);

            // Add buttons
            ButtonType returnToCatalogType = new ButtonType("Retourner au Catalogue");
            ButtonType closeButtonType = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);

            dialog.getDialogPane().getButtonTypes().addAll(returnToCatalogType, closeButtonType);

            // Style the dialog
            dialog.getDialogPane().setPrefWidth(400);
            dialog.getDialogPane().setPrefHeight(200);

            // Show the dialog and handle the result
            dialog.showAndWait().ifPresent(buttonType -> {
                if (buttonType == returnToCatalogType) {
                    navigateToCatalogue();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtilsSirine.showError("Erreur", "Erreur d'affichage", "Impossible d'afficher la confirmation.");
        }
    }

    /**
     * Navigate to the catalogue view
     */
    private void navigateToCatalogue() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/produit/ProduitView.fxml"));
            URL fxmlLocation = getClass().getResource("/com/esprit/views/produit/ProduitView.fxml");
            System.out.println("FXML Location: " + fxmlLocation);
            Parent catalogueView = loader.load();

            Scene currentScene = tableView.getScene();
            Stage stage = (Stage) currentScene.getWindow();

            Scene catalogueScene = new Scene(catalogueView);
            stage.setScene(catalogueScene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtilsSirine.showError("Erreur", "Navigation", "Impossible de charger le catalogue.");
        }
    }


    // CartItem class
    public static class CartItem {
        private int productId;
        private String name;
        private int quantity;
        private int price;
        private double total;
        private ImageView imageView;

        public int getProductId() { return productId; }
        public void setProductId(int productId) { this.productId = productId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) {
            this.quantity = quantity;
            calculateTotal();
        }

        public int getPrice() { return price; }
        public void setPrice(int price) {
            this.price = price;
            calculateTotal();
        }

        public double getTotal() { return total; }
        public void setTotal(double total) { this.total = total; }

        public ImageView getImageView() { return imageView; }
        public void setImageView(ImageView imageView) { this.imageView = imageView; }

        public void calculateTotal() {
            this.total = this.price * this.quantity;
        }
    }
}

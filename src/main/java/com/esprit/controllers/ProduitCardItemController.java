package com.esprit.controllers;

import com.esprit.ProduitApp;
import com.esprit.models.Produit;
import com.esprit.services.ProduitService;
import com.esprit.utils.AlertUtils;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ProduitCardItemController {

    @FXML private ImageView imgProduct;
    @FXML private Label lblClub;
    @FXML private Label lblNom;
    @FXML private Label lblDescription;
    @FXML private Label lblPrix;
    @FXML private Label lblQuantity;
    @FXML private Button btnDetails;
    @FXML private Button btnAddToCart;

    private Produit produit;
    private final ProduitService produitService;
    // Static cart shared across all instances
    private static final Map<Integer, Integer> cart = new HashMap<>();
    public static Map<Integer, Integer> getCart() {
        return new HashMap<>(cart); // Return a copy for encapsulation
    }

    public ProduitCardItemController() {
        this.produitService = ProduitService.getInstance();
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
        updateUI();
    }

    private void updateUI() {
        if (produit != null) {
            lblNom.setText(produit.getNomProd());
            lblDescription.setText(produit.getDescProd());
            lblPrix.setText(String.format("%.2f €", produit.getPrix()));
            lblQuantity.setText("Stock: " + produit.getQuantity());
            
            if (produit.getClub() != null) {
                lblClub.setText(produit.getClub().getNom().toUpperCase());
            } else {
                lblClub.setText("");
            }

            // Load image if available
            try {
                String imagePath = produit.getImgProd();
                if (imagePath != null && !imagePath.isEmpty()) {
                    URL imageUrl = getClass().getResource("/" + imagePath);
                    if (imageUrl != null) {
                        Image image = new Image(imageUrl.toString());
                        imgProduct.setImage(image);
                    } else {
                        // Set default image if product image not found
                        imgProduct.setImage(new Image(getClass().getResourceAsStream("/images/default-product.png")));
                    }
                } else {
                    // Set default image if no image path
                    imgProduct.setImage(new Image(getClass().getResourceAsStream("/images/default-product.png")));
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Set default image in case of error
                imgProduct.setImage(new Image(getClass().getResourceAsStream("/images/default-product.png")));
            }
        }
    }

    @FXML
    private void viewDetails() {
        if (produit != null) {
            try {
                // Store the selected product in the details controller
                ProduitDetailsController.setSelectedProduit(produit);
                
                // Navigate to product details view
                ProduitApp.navigateTo("/com/esprit/views/produit/ProduitDetailsView.fxml");
            } catch (Exception e) {
                e.printStackTrace();
                AlertUtils.showError("Erreur", "Erreur lors de l'affichage des détails", e.getMessage());
            }
        }
    }

    @FXML
    private void addToCart() {
        /*if (produit != null) {
            // For now, just show an info message
            AlertUtils.showInfo("Panier", "Produit ajouté", 
                "Le produit \"" + produit.getNomProd() + "\" a été ajouté au panier.");*/
            // TODO: Implement actual cart functionality
            if (produit != null) {
                try {
                    // Check if product has available stock
                    int availableQuantity = Integer.parseInt(produit.getQuantity());
                    if (availableQuantity <= 0) {
                        AlertUtils.showError("Erreur", "Stock épuisé",
                                "Ce produit n'est plus disponible en stock.");
                        return;
                    }

                    // Get current quantity in cart
                    int currentCartQuantity = cart.getOrDefault(produit.getId(), 0);

                    // Check if we can add more
                    if (currentCartQuantity >= availableQuantity) {
                        AlertUtils.showError("Erreur", "Quantité maximale atteinte",
                                "Vous ne pouvez pas ajouter plus de ce produit que ce qui est disponible en stock.");
                        return;
                    }

                    // Add to cart or increment quantity
                    cart.put(produit.getId(), currentCartQuantity + 1);

                    // Show success message
                    AlertUtils.showInfo("Panier", "Produit ajouté",
                            String.format("%s ajouté au panier\nQuantité: %d",
                                    produit.getNomProd(), cart.get(produit.getId())));

                    // Update any cart badge if exists
                    //updateCartBadge();

                    // Navigate to cart view after adding
                    ProduitApp.navigateTo("/com/esprit/views/produit/produit_card.fxml");

                } catch (NumberFormatException e) {
                    AlertUtils.showError("Erreur", "Problème de quantité",
                            "La quantité disponible pour ce produit est invalide.");
                } catch (Exception e) {
                    AlertUtils.showError("Erreur", "Impossible d'accéder au panier", e.getMessage());
                }
            }

        }

} 

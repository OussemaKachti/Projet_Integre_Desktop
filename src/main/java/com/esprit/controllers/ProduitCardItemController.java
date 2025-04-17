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
        if (produit != null) {
            // For now, just show an info message
            AlertUtils.showInfo("Panier", "Produit ajouté", 
                "Le produit \"" + produit.getNomProd() + "\" a été ajouté au panier.");
            // TODO: Implement actual cart functionality
        }
    }
} 

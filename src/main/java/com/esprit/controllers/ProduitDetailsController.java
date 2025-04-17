package com.esprit.controllers;

import com.esprit.ProduitApp;
import com.esprit.models.Produit;
import com.esprit.utils.AlertUtils;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.ResourceBundle;
import java.io.File;

public class ProduitDetailsController implements Initializable {

    @FXML private ImageView imgProduit;
    @FXML private Label lblNomProduit;
    @FXML private Label lblPrix;
    @FXML private Label lblDescription;
    @FXML private Label lblQuantity;
    @FXML private Label lblClub;
    @FXML private Spinner<Integer> spinnerQuantity;
    @FXML private Button btnAddToCart;
    @FXML private Button btnBuyNow;
    @FXML private Button btnBack;
    
    // Le produit sélectionné à afficher
    private static Produit selectedProduit;
    
    /**
     * Définir le produit sélectionné (appelé avant la navigation)
     */
    public static void setSelectedProduit(Produit produit) {
        selectedProduit = produit;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (selectedProduit != null) {
            setupProductDetails();
            setupQuantitySpinner();
        } else {
            AlertUtils.showError("Erreur", "Produit non disponible", 
                "Impossible d'afficher les détails du produit. Veuillez réessayer.");
            retourCatalogue();
        }
    }

    /**
     * Affiche les détails du produit sélectionné
     */
    private void setupProductDetails() {
        lblNomProduit.setText(selectedProduit.getNomProd());
        lblPrix.setText(String.format("%.2f €", selectedProduit.getPrix()));
        lblDescription.setText(selectedProduit.getDescProd());
        
        // Afficher la quantité disponible
        String quantity = selectedProduit.getQuantity();
        int quantityInt;
        try {
            quantityInt = Integer.parseInt(quantity);
            if (quantityInt > 0) {
                lblQuantity.setText(String.format("En stock (%s disponibles)", quantity));
                lblQuantity.setStyle("-fx-text-fill: #006400;"); // Vert
            } else {
                lblQuantity.setText("Rupture de stock");
                lblQuantity.setStyle("-fx-text-fill: #B00020;"); // Rouge
                btnAddToCart.setDisable(true);
                btnBuyNow.setDisable(true);
            }
        } catch (NumberFormatException e) {
            lblQuantity.setText("Disponibilité: " + quantity);
        }
        
        // Afficher le club
        if (selectedProduit.getClub() != null && selectedProduit.getClub().getNom() != null) {
            lblClub.setText(selectedProduit.getClub().getNom().toUpperCase());
        } else {
            lblClub.setText("");
        }
        
        // Set a blank image by default
        imgProduit.setImage(null);
        
        // Try to load the product image
        try {
            String imagePath = selectedProduit.getImgProd();
            if (imagePath != null && !imagePath.isEmpty()) {
                // Try first as a resource
                URL imageUrl = getClass().getResource("/" + imagePath);
                
                if (imageUrl != null) {
                    Image image = new Image(imageUrl.toString(), true); // Use background loading
                    imgProduit.setImage(image);
                } else {
                    // Then try as a file path
                    File file = new File(imagePath);
                    if (file.exists()) {
                        Image image = new Image(file.toURI().toString(), true);
                        imgProduit.setImage(image);
                    } else {
                        System.out.println("Image not found: " + imagePath);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Leave the image as null if loading fails
        }
    }

    /**
     * Configure le spinner de quantité
     */
    private void setupQuantitySpinner() {
        // Utiliser un tableau pour stocker la valeur qui peut être modifiée
        // tout en gardant la référence finale
        final int[] maxQuantityHolder = new int[1];
        
        try {
            maxQuantityHolder[0] = Integer.parseInt(selectedProduit.getQuantity());
        } catch (NumberFormatException e) {
            maxQuantityHolder[0] = 10; // Valeur par défaut si la quantité n'est pas un nombre
        }
        
        // Limiter la quantité maximum à la disponibilité
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, maxQuantityHolder[0], 1);
        spinnerQuantity.setValueFactory(valueFactory);
        
        // Rendre le spinner éditable
        spinnerQuantity.setEditable(true);
        spinnerQuantity.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            try {
                int value = Integer.parseInt(newValue);
                if (value < 1) {
                    spinnerQuantity.getValueFactory().setValue(1);
                } else if (value > maxQuantityHolder[0]) {
                    spinnerQuantity.getValueFactory().setValue(maxQuantityHolder[0]);
                }
            } catch (NumberFormatException e) {
                // Revenir à l'ancienne valeur si le texte n'est pas un nombre
                spinnerQuantity.getEditor().setText(oldValue);
            }
        });
    }

    /**
     * Ajoute le produit au panier
     */
    @FXML
    private void ajouterAuPanier() {
        int quantity = spinnerQuantity.getValue();
        
        // TODO: Implémenter le panier
        AlertUtils.showInfo("Panier", "Produit ajouté", 
            String.format("%d × %s ajouté(s) au panier", quantity, selectedProduit.getNomProd()));
    }

    /**
     * Achète le produit immédiatement
     */
    @FXML
    private void acheterMaintenant() {
        int quantity = spinnerQuantity.getValue();
        float total = selectedProduit.getPrix() * quantity;
        
        // TODO: Implémenter le processus d'achat
        AlertUtils.showInfo("Achat", "Achat direct", 
            String.format("Achat de %d × %s pour un total de %.2f €", 
                quantity, selectedProduit.getNomProd(), total));
    }

    /**
     * Retourne au catalogue des produits
     */
    @FXML
    private void retourCatalogue() {
        ProduitApp.navigateTo("/com/esprit/views/produit/ProduitView.fxml");
    }
    
    /**
     * Increments the quantity spinner value
     */
    @FXML
    private void incrementQuantity() {
        int currentValue = spinnerQuantity.getValue();
        int maxValue = ((SpinnerValueFactory.IntegerSpinnerValueFactory)spinnerQuantity.getValueFactory()).getMax();
        
        if (currentValue < maxValue) {
            spinnerQuantity.getValueFactory().setValue(currentValue + 1);
        }
    }
    
    /**
     * Decrements the quantity spinner value
     */
    @FXML
    private void decrementQuantity() {
        int currentValue = spinnerQuantity.getValue();
        
        if (currentValue > 1) {
            spinnerQuantity.getValueFactory().setValue(currentValue - 1);
        }
    }
}
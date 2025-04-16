package com.esprit.controllers.crud;

import com.esprit.models.Produit;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class ProduitDetailsController implements Initializable {

    @FXML
    private Label lblNomProd;
    @FXML
    private Label lblDescProd;
    @FXML
    private Label lblPrix;
    @FXML
    private Label lblQuantity;
    @FXML
    private Label lblClub;
    @FXML
    private ImageView imgProd;
    @FXML
    private VBox detailsContainer;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialization if needed
    }

    public void initData(Produit produit) {
        lblNomProd.setText(produit.getNomProd());
        lblDescProd.setText(produit.getDescProd());
        lblPrix.setText(String.valueOf(produit.getPrix()) + " TND");
        lblQuantity.setText("Quantité disponible: " + produit.getQuantity());
        lblClub.setText("Club: " + (produit.getClub() != null ? produit.getClub().getNom() : "Aucun club"));

        try {
            String imagePath = produit.getImgProd();
            if (imagePath != null && !imagePath.isEmpty()) {
                URL imageUrl = getClass().getClassLoader().getResource(imagePath);
                if (imageUrl != null) {
                    imgProd.setImage(new Image(imageUrl.toString()));
                } else {
                    imgProd.setImage(new Image(getClass().getClassLoader().getResource("images/default-product.png").toString()));
                }
            } else {
                imgProd.setImage(new Image(getClass().getClassLoader().getResource("images/default-product.png").toString()));
            }
        } catch (Exception e) {
            System.err.println("Erreur de chargement de l'image: " + e.getMessage());


        }
    }
}
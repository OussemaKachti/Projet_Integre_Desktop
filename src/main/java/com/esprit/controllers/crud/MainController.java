/*package com.esprit.controllers.crud;

import com.esprit.models.Produit;
import com.esprit.services.ProduitService;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class MainController {

    @FXML private VBox vboxProduits;
    private ProduitService produitService;

    public MainController() {
        this.produitService = ProduitService.getInstance();
    }

    @FXML
    public void initialize() {
        loadProduits();
    }

    private void loadProduits() {
        ObservableList<Produit> produits = FXCollections.observableArrayList();
        try {
            produits.addAll(produitService.getAll());
            for (Produit produit : produits) {
                // Créer une instance de ProduitCardController pour chaque produit
                ProduitCardController produitCardController = new ProduitCardController();
                produitCardController.setProduit(produit);

                // Ajouter chaque card à la VBox
                vboxProduits.getChildren().add(produitCardController.hboxProduitCard);
            }
        } catch (Exception e) {
            e.printStackTrace();  // Afficher l'exception si un problème se produit
        }
    }
}*/

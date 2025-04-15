package com.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class BaseController {
    @FXML
    private StackPane contentArea;
    
    @FXML
    private ImageView logoImage;
    
    @FXML
    private ImageView footerLogo;
    
    @FXML
    public void initialize() {
        // Charger les images
        logoImage.setImage(new Image("/images/logo.png"));
        footerLogo.setImage(new Image("/images/logo.png"));
    }
    
    // Méthode pour charger une nouvelle vue dans le contentArea
    public void loadView(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
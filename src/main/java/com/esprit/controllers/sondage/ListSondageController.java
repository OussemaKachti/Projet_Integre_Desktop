package com.esprit.controllers.sondage;

import com.esprit.models.Sondage;
import com.esprit.services.SondageService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class ListSondageController implements Initializable {
    
    @FXML
    private TableView<Sondage> tableSondages;
    
    @FXML
    private TableColumn<Sondage, String> colQuestion;
    
    @FXML
    private TableColumn<Sondage, String> colDate;
    
    @FXML
    private TableColumn<Sondage, String> colClub;
    
    @FXML
    private TableColumn<Sondage, Void> colActions;
    
    @FXML
    private Button btnAjouter;
    
    private SondageService sondageService;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        sondageService = new SondageService();
        
        // Configuration des colonnes
        colQuestion.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getQuestion()));
            
        colDate.setCellValueFactory(cellData -> 
            new SimpleStringProperty(
                cellData.getValue().getCreatedAt().toString()));
                
        colClub.setCellValueFactory(cellData -> 
            new SimpleStringProperty(
                cellData.getValue().getClub().getNom()));
        
        // Ajouter les boutons d'action
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnVoir = new Button("Voir");
            private final Button btnModifier = new Button("Modifier");
            
            {
                btnVoir.setOnAction(event -> {
                    Sondage sondage = getTableView().getItems().get(getIndex());
                    ouvrirDetailSondage(sondage);
                });
                
                btnModifier.setOnAction(event -> {
                    Sondage sondage = getTableView().getItems().get(getIndex());
                    ouvrirModifierSondage(sondage);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, btnVoir, btnModifier);
                    setGraphic(buttons);
                }
            }
        });
        
        // Charger les données
        chargerSondages();
        
        // Configuration du bouton Ajouter
        btnAjouter.setOnAction(event -> ouvrirAjoutSondage());
    }
    
    private void chargerSondages() {
        try {
            ObservableList<Sondage> sondages = sondageService.getAll();
            tableSondages.setItems(sondages);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors du chargement des sondages", 
                     e.getMessage());
        }
    }
    
    private void ouvrirDetailSondage(Sondage sondage) {
        // Code pour ouvrir la vue détail
    }
    
    private void ouvrirModifierSondage(Sondage sondage) {
        // Code pour ouvrir la vue modification
    }
    
    private void ouvrirAjoutSondage() {
        // Code pour ouvrir la vue ajout
    }
    
    private void showAlert(Alert.AlertType type, String title, 
                         String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
} 
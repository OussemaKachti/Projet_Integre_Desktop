package com.esprit.controllers.sondage;

import com.esprit.models.ChoixSondage;
import com.esprit.models.Sondage;
import com.esprit.services.ChoixSondageService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.collections.ObservableList;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class ChoixSondageController implements Initializable {
    
    private final ChoixSondageService choixService = new ChoixSondageService();
    private Sondage currentSondage;
    
    @FXML private ListView<ChoixSondage> listChoix;
    @FXML private TextField txtContenu;
    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupListView();
        setupButtons();
    }
    
    public void setSondage(Sondage sondage) {
        this.currentSondage = sondage;
        chargerChoix();
    }
    
    private void setupListView() {
        listChoix.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtContenu.setText(newVal.getContenu());
                btnModifier.setDisable(false);
                btnSupprimer.setDisable(false);
            } else {
                txtContenu.clear();
                btnModifier.setDisable(true);
                btnSupprimer.setDisable(true);
            }
        });
    }
    
    private void setupButtons() {
        btnModifier.setDisable(true);
        btnSupprimer.setDisable(true);
    }
    
    private void chargerChoix() {
        try {
            if (currentSondage != null) {
                ObservableList<ChoixSondage> choix = choixService.getBySondage(currentSondage.getId());
                listChoix.setItems(choix);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors du chargement des choix", e.getMessage());
        }
    }
    
    @FXML
    private void ajouterChoix() {
        try {
            if (txtContenu.getText().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Attention", 
                         "Le contenu ne peut pas être vide", null);
                return;
            }
            
            ChoixSondage choix = new ChoixSondage();
            choix.setContenu(txtContenu.getText());
            choix.setSondage(currentSondage);
            
            choixService.add(choix);
            chargerChoix();
            txtContenu.clear();
            
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors de l'ajout du choix", e.getMessage());
        }
    }
    
    @FXML
    private void modifierChoix() {
        try {
            ChoixSondage choix = listChoix.getSelectionModel().getSelectedItem();
            if (choix == null) return;
            
            choix.setContenu(txtContenu.getText());
            choixService.update(choix);
            chargerChoix();
            
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors de la modification du choix", e.getMessage());
        }
    }
    
    @FXML
    private void supprimerChoix() {
        try {
            ChoixSondage choix = listChoix.getSelectionModel().getSelectedItem();
            if (choix == null) return;
            
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirmation");
            confirmation.setHeaderText("Supprimer le choix");
            confirmation.setContentText("Voulez-vous vraiment supprimer ce choix ?");
            
            if (confirmation.showAndWait().get() == ButtonType.OK) {
                choixService.delete(choix.getId());
                chargerChoix();
                txtContenu.clear();
            }
            
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors de la suppression du choix", e.getMessage());
        }
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
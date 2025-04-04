package com.esprit.controllers.sondage;

import com.esprit.models.*;
import com.esprit.services.ReponseService;
import com.esprit.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.collections.ObservableList;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class ReponseController implements Initializable {
    
    private final ReponseService reponseService = new ReponseService();
    private Sondage currentSondage;
    
    @FXML private VBox vboxChoix;  // Conteneur pour les RadioButtons des choix
    @FXML private Button btnVoter;
    @FXML private Button btnSupprimer;
    @FXML private Label lblResultat;
    
    private ToggleGroup choixGroup;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        choixGroup = new ToggleGroup();
        // setupButtons();
    }
    
    public void setSondage(Sondage sondage) {
        this.currentSondage = sondage;
        setupChoix();
        chargerReponseExistante();
    }
    
    private void setupChoix() {
        vboxChoix.getChildren().clear();
        
        for (ChoixSondage choix : currentSondage.getChoix()) {
            RadioButton rb = new RadioButton(choix.getContenu());
            rb.setToggleGroup(choixGroup);
            rb.setUserData(choix);  // Stocker l'objet ChoixSondage
            vboxChoix.getChildren().add(rb);
        }
    }
    
    private void chargerReponseExistante() {
        Reponse reponse = reponseService.getUserResponse(
            SessionManager.getCurrentUser().getId(),
            currentSondage.getId()
        );
        
        if (reponse != null) {
            // Sélectionner le choix existant
            for (javafx.scene.Node node : vboxChoix.getChildren()) {
                if (node instanceof RadioButton) {
                    RadioButton rb = (RadioButton) node;
                    ChoixSondage choix = (ChoixSondage) rb.getUserData();
                    if (choix.getId() == reponse.getChoixSondage().getId()) {
                        rb.setSelected(true);
                        break;
                    }
                }
            }
            btnSupprimer.setDisable(false);
        } else {
            btnSupprimer.setDisable(true);
        }
        
        afficherResultats();
    }
    
    @FXML
    private void voter() {
        RadioButton selectedRb = (RadioButton) choixGroup.getSelectedToggle();
        if (selectedRb == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", 
                     "Veuillez sélectionner une option", null);
            return;
        }
        
        ChoixSondage choix = (ChoixSondage) selectedRb.getUserData();
        
        // Supprimer l'ancien vote si existe
        Reponse existingVote = reponseService.getUserResponse(
            SessionManager.getCurrentUser().getId(),
            currentSondage.getId()
        );
        
        if (existingVote != null) {
            reponseService.delete(existingVote.getId());
        }
        
        // Créer le nouveau vote
        Reponse reponse = new Reponse();
        reponse.setUser(SessionManager.getCurrentUser());
        reponse.setChoixSondage(choix);
        reponse.setDateReponse(LocalDateTime.now());
        reponse.setSondage(currentSondage);
        
        reponseService.add(reponse);
        
        afficherResultats();
        btnSupprimer.setDisable(false);
        
        showAlert(Alert.AlertType.INFORMATION, "Succès", 
                 "Vote enregistré avec succès", null);
    }
    
    @FXML
    private void supprimerVote() {
        Reponse reponse = reponseService.getUserResponse(
            SessionManager.getCurrentUser().getId(),
            currentSondage.getId()
        );
        
        if (reponse != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirmation");
            confirmation.setHeaderText("Supprimer le vote");
            confirmation.setContentText("Voulez-vous vraiment supprimer votre vote ?");
            
            if (confirmation.showAndWait().get() == ButtonType.OK) {
                reponseService.delete(reponse.getId());
                choixGroup.selectToggle(null);
                btnSupprimer.setDisable(true);
                afficherResultats();
            }
        }
    }
    
    private void afficherResultats() {
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
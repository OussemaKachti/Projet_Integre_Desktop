package com.esprit.controllers.sondage;

import com.esprit.models.Commentaire;
import com.esprit.models.Sondage;
import com.esprit.services.CommentaireService;
// import com.esprit.utils.SessionManager;
// import com.esprit.utils.EmailService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class CommentaireController implements Initializable {
    
    private final CommentaireService commentaireService = new CommentaireService();
    // private final EmailService emailService = new EmailService();
    private Sondage currentSondage;
    
    @FXML private ListView<Commentaire> listCommentaires;
    @FXML private TextArea txtNouveauCommentaire;
    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupListView();
        // setupButtons();
    }
    
    public void setSondage(Sondage sondage) {
        this.currentSondage = sondage;
        chargerCommentaires();
    }
    
    private void setupListView() {
        listCommentaires.setCellFactory(lv -> new ListCell<Commentaire>() {
            @Override
            protected void updateItem(Commentaire comment, boolean empty) {
                super.updateItem(comment, empty);
                if (empty || comment == null) {
                    setText(null);
                } else {
                    setText(String.format("%s - %s\n%s", 
                        comment.getUser().getFullName(),
                        comment.getDateComment().toString(),
                        comment.getContenuComment()));
                }
            }
        });
        
        listCommentaires.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    txtNouveauCommentaire.setText(newVal.getContenuComment());
                    // boolean isOwner = newVal.getUser().getId() == 
                    //                 SessionManager.getCurrentUser().getId();
                    // btnModifier.setDisable(!isOwner);
                    // btnSupprimer.setDisable(!isOwner);
                }
            });
    }
    
    private void chargerCommentaires() {
        try {
            ObservableList<Commentaire> commentaires = 
                commentaireService.getBySondage(currentSondage.getId());
            listCommentaires.setItems(commentaires);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors du chargement des commentaires", e.getMessage());
        }
    }
    
    @FXML
    private void ajouterCommentaire() {
        try {
            String contenu = txtNouveauCommentaire.getText().trim();
            if (contenu.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Attention", 
                         "Le commentaire ne peut pas être vide", null);
                return;
            }
            
            // Vérification du contenu toxique
            if (isToxicContent(contenu)) {
                handleToxicComment(contenu);
                return;
            }
            
            Commentaire commentaire = new Commentaire();
            commentaire.setContenuComment(contenu);
            commentaire.setDateComment(LocalDateTime.now());
            // commentaire.setUser(SessionManager.getCurrentUser());
            commentaire.setSondage(currentSondage);
            
            commentaireService.add(commentaire);
            chargerCommentaires();
            txtNouveauCommentaire.clear();
            
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors de l'ajout du commentaire", e.getMessage());
        }
    }
    
    private boolean isToxicContent(String content) {
        // Implémentez votre logique de détection de contenu toxique ici
        // Pour l'exemple, on vérifie juste les gros mots basiques
        String[] toxicWords = {"badword1", "badword2", "badword3"};
        for (String word : toxicWords) {
            if (content.toLowerCase().contains(word)) {
                return true;
            }
        }
        return false;
    }
    
    private void handleToxicComment(String content) {
        String warningMessage = "⚠️ Commentaire masqué : Contenu inapproprié détecté";
        showAlert(Alert.AlertType.WARNING, "Contenu inapproprié", 
                 "Votre commentaire contient du contenu inapproprié", null);
                 
        // Envoyer un email d'avertissement
        // emailService.sendWarningEmail(
            // SessionManager.getCurrentUser().getEmail(),
        //     "Avertissement : Commentaire inapproprié",
        //     "Votre commentaire a été signalé comme inapproprié."
        // );
    }
    
    // ... autres méthodes (modifier, supprimer, etc.)
    
    private void showAlert(Alert.AlertType type, String title, 
                         String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
} 
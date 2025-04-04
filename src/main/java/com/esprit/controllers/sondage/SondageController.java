package com.esprit.controllers.sondage;

import com.esprit.models.*;
import com.esprit.services.*;
import com.esprit.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import java.net.URL;
import java.util.ResourceBundle;
import java.sql.SQLException;
import java.util.Map;

public class SondageController implements Initializable {
    
    private final SondageService sondageService = new SondageService();
    private final ChoixSondageService choixService = new ChoixSondageService();
    private final CommentaireService commentaireService = new CommentaireService();
    private final ReponseService reponseService = new ReponseService();
    
    // Pour la partie liste des sondages
    @FXML private TableView<Sondage> tableSondages;
    @FXML private VBox vboxCreationSondage;  // Visible uniquement pour le président
    
    // Pour la création de sondage
    @FXML private TextField txtQuestion;
    @FXML private VBox vboxChoix;  // Conteneur dynamique pour les choix
    @FXML private Button btnAjouterChoix;
    @FXML private Button btnCreerSondage;
    
    // Pour les commentaires
    @FXML private ListView<Commentaire> listCommentaires;
    @FXML private TextArea txtNouveauCommentaire;
    
    @FXML private VBox vboxOptions;
    @FXML private TextArea txtComment;
    @FXML private Button btnSubmitVote;
    @FXML private Button btnPostComment;
    @FXML private Label lblCommentCount;
    
    private ToggleGroup optionsGroup;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Vérifier si l'utilisateur est président
        if (isPresident()) {
            vboxCreationSondage.setVisible(true);
        }
        
        setupTableColumns();
        // Commentez temporairement les appels à la BD
        /*try {
            chargerSondages();
        } catch (SQLException e) {
            e.printStackTrace();
        }*/
        // setupCreationSondage();
        
        optionsGroup = new ToggleGroup();
        
        // Grouper les radio buttons
        vboxOptions.getChildren().forEach(node -> {
            if (node instanceof RadioButton) {
                ((RadioButton) node).setToggleGroup(optionsGroup);
            }
        });
        
        // Ajouter les événements des boutons
        btnSubmitVote.setOnAction(e -> handleVote());
        btnPostComment.setOnAction(e -> handleComment());
    }
    
    private void setupTableColumns() {
        TableColumn<Sondage, String> questionCol = new TableColumn<>("Question");
        questionCol.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getQuestion()));
            
        TableColumn<Sondage, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> 
            new SimpleStringProperty(
                data.getValue().getCreatedAt().toString()));
                
        TableColumn<Sondage, String> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button btnVoter = new Button("Voter");
            private final Button btnCommenter = new Button("Commenter");
            
            {
                btnVoter.setOnAction(e -> {
                    Sondage sondage = getTableView().getItems().get(getIndex());
                    ouvrirVoteSondage(sondage);
                });
                
                btnCommenter.setOnAction(e -> {
                    Sondage sondage = getTableView().getItems().get(getIndex());
                    ouvrirVoteSondage(sondage);
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, btnVoter, btnCommenter);
                    setGraphic(buttons);
                }
            }

            private void ouvrirVoteSondage(Sondage sondage) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'ouvrirVoteSondage'");
            }
        });
        
        tableSondages.getColumns().addAll(questionCol, dateCol, actionsCol);
    }
    
    private boolean isPresident() {
        User currentUser = SessionManager.getCurrentUser();
        Club currentClub = SessionManager.getCurrentClub();
        return currentClub != null && 
               currentClub.getPresident() != null && 
               currentClub.getPresident().getId() == currentUser.getId();
    }
    
    private void chargerSondages() throws SQLException {
        // Pour test, utilisez des données statiques
        Sondage sondage = new Sondage();
        sondage.setQuestion("tunisia / algeria ?");
        // ... autres initialisations
        
        ObservableList<Sondage> sondages = FXCollections.observableArrayList(sondage);
        tableSondages.setItems(sondages);
    }
    
    private void chargerChoixEtCommentaires(Sondage sondage) {
        try {
            // Charger les choix
            ObservableList<ChoixSondage> choix = choixService.getBySondage(sondage.getId());
            
            // Charger les commentaires
            ObservableList<Commentaire> commentaires = 
                commentaireService.getBySondage(sondage.getId());
                
            // Charger les résultats des votes
            Map<String, Object> resultats = 
                reponseService.getPollResults(sondage.getId());
                
            // Mettre à jour l'interface
            // updateSondageView(sondage, choix, commentaires, resultats);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // private void updateSondageView(Sondage sondage, ObservableList<ChoixSondage> choix,
    //         ObservableList<Commentaire> commentaires, Map<String resultats) {
    //     // TODO Auto-generated method stub
    //     throw new UnsupportedOperationException("Unimplemented method 'updateSondageView'");
    // }

    @FXML
    private void creerSondage() {
        try {
            if (txtQuestion.getText().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", 
                         "La question ne peut pas être vide", null);
                return;
            }
            
            Sondage sondage = new Sondage();
            sondage.setQuestion(txtQuestion.getText());
            sondage.setUser(SessionManager.getCurrentUser());
            sondage.setClub(SessionManager.getCurrentClub());
            
            // Ajouter les choix
            for (javafx.scene.Node node : vboxChoix.getChildren()) {
                if (node instanceof TextField) {
                    String choixText = ((TextField) node).getText();
                    if (!choixText.isEmpty()) {
                        ChoixSondage choix = new ChoixSondage();
                        choix.setContenu(choixText);
                        sondage.addChoix(choix);
                    }
                }
            }
            
            sondageService.add(sondage);
            clearForm();
            chargerSondages();
            
            showAlert(Alert.AlertType.INFORMATION, "Succès", 
                     "Sondage créé avec succès", null);
                     
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors de la création du sondage", e.getMessage());
        }
    }

    private void clearForm() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'clearForm'");
    }

    private void showAlert(AlertType error, String string, String string2, Object object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'showAlert'");
    }
    
    private void handleVote() {
        // Pour le test
        System.out.println("Vote submitted!");
    }
    
    private void handleComment() {
        String comment = txtComment.getText();
        if (!comment.isEmpty()) {
            System.out.println("Comment posted: " + comment);
            txtComment.clear();
        }
    }
    
    // ... autres méthodes utilitaires
} 
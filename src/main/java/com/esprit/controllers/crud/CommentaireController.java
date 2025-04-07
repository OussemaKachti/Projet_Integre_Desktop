package com.esprit.controllers.crud;

import com.esprit.models.Commentaire;
import com.esprit.models.Sondage;
import com.esprit.models.User;
import com.esprit.services.CommentaireService;
import com.esprit.services.SondageService;
import com.esprit.services.UserService;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.beans.property.SimpleStringProperty;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.Optional;

/**
 * Contrôleur pour la gestion des commentaires de sondage (CRUD)
 */
public class CommentaireController implements Initializable {
    
    // Composants de l'interface utilisateur
    @FXML private ComboBox<Sondage> cbSondages;
    @FXML private TableView<Commentaire> tableCommentaires;
    @FXML private TableColumn<Commentaire, String> colContenu;
    @FXML private TableColumn<Commentaire, String> colUser;
    @FXML private TableColumn<Commentaire, String> colDate;
    @FXML private TableColumn<Commentaire, String> colActions;
    
    @FXML private TextArea txtContenu;
    @FXML private Button btnSave;
    @FXML private Button btnUpdate;
    @FXML private Button btnCancel;
    
    // Services
    private final CommentaireService commentaireService;
    private final SondageService sondageService;
    private final UserService userService;
    
    // État du contrôleur
    private Commentaire currentCommentaire;
    private boolean editMode = false;
    
    /**
     * Constructeur
     */
    public CommentaireController() {
        this.commentaireService = new CommentaireService();
        this.sondageService = SondageService.getInstance();
        this.userService = new UserService();
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupSondageComboBox();
        setupTable();
        setupButtons();
    }
    
    /**
     * Configure la liste déroulante des sondages
     */
    private void setupSondageComboBox() {
        try {
            ObservableList<Sondage> sondages = sondageService.getAll();
            cbSondages.setItems(sondages);
            
            // Configurer l'affichage des éléments de la liste
            cbSondages.setCellFactory(lv -> new ListCell<Sondage>() {
                @Override
                protected void updateItem(Sondage sondage, boolean empty) {
                    super.updateItem(sondage, empty);
                    if (empty || sondage == null) {
                        setText(null);
                    } else {
                        setText(sondage.getQuestion());
                    }
                }
            });
            
            // Configurer l'affichage de l'élément sélectionné
            cbSondages.setButtonCell(new ListCell<Sondage>() {
                @Override
                protected void updateItem(Sondage sondage, boolean empty) {
                    super.updateItem(sondage, empty);
                    if (empty || sondage == null) {
                        setText("Sélectionner un sondage");
                    } else {
                        setText(sondage.getQuestion());
                    }
                }
            });
            
            // Gérer le changement de sélection
            cbSondages.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    loadCommentairesBySondage(newVal);
                } else {
                    tableCommentaires.getItems().clear();
                }
            });
            
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors du chargement des sondages", e.getMessage());
        }
    }
    
    /**
     * Configure les colonnes du tableau
     */
    private void setupTable() {
        colContenu.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getContenuComment()));
            
        colUser.setCellValueFactory(cellData -> {
            User user = cellData.getValue().getUser();
            return new SimpleStringProperty(user != null ? 
                user.getPrenom() + " " + user.getNom() : "Anonyme");
        });
        
        colDate.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDateComment()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        
        setupActionsColumn();
    }
    
    /**
     * Configure la colonne des actions
     */
    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<Commentaire, String>() {
            private final Button editButton = new Button("Modifier");
            private final Button deleteButton = new Button("Supprimer");
            
            {
                editButton.setOnAction(e -> {
                    Commentaire commentaire = getTableView().getItems().get(getIndex());
                    editCommentaire(commentaire);
                });
                
                deleteButton.setOnAction(e -> {
                    Commentaire commentaire = getTableView().getItems().get(getIndex());
                    deleteCommentaire(commentaire);
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty) {
                    setGraphic(null);
                } else {
                    // Vérifier si l'utilisateur courant est l'auteur du commentaire
                    Commentaire commentaire = getTableView().getItems().get(getIndex());
                    User currentUser = userService.getById(1); // Utilisateur statique ID=1
                    
                    boolean isAuthor = (commentaire.getUser() != null && 
                                      commentaire.getUser().getId() == currentUser.getId());
                    
                    HBox buttons = new HBox(5);
                    
                    // L'utilisateur ne peut éditer que ses propres commentaires
                    editButton.setDisable(!isAuthor);
                    buttons.getChildren().add(editButton);
                    
                    // L'utilisateur ne peut supprimer que ses propres commentaires
                    deleteButton.setDisable(!isAuthor);
                    buttons.getChildren().add(deleteButton);
                    
                    buttons.setPadding(new Insets(0, 0, 0, 5));
                    setGraphic(buttons);
                }
            }
        });
    }
    
    /**
     * Configure les événements des boutons
     */
    private void setupButtons() {
        btnSave.setOnAction(e -> saveCommentaire());
        btnUpdate.setOnAction(e -> updateCommentaire());
        btnCancel.setOnAction(e -> resetForm());
        
        // Masquer le bouton de mise à jour initialement
        btnUpdate.setVisible(false);
    }
    
    /**
     * Charge les commentaires d'un sondage
     */
    private void loadCommentairesBySondage(Sondage sondage) {
        try {
            ObservableList<Commentaire> commentaires = commentaireService.getBySondage(sondage.getId());
            tableCommentaires.setItems(commentaires);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors du chargement des commentaires", e.getMessage());
        }
    }
    
    /**
     * Crée un nouveau commentaire
     */
    @FXML
    private void saveCommentaire() {
        Sondage selectedSondage = cbSondages.getSelectionModel().getSelectedItem();
        
        if (selectedSondage == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Sondage requis", "Veuillez sélectionner un sondage.");
            return;
        }
        
        if (txtContenu.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Contenu requis", "Veuillez saisir un commentaire.");
            return;
        }
        
        try {
            // Récupérer l'utilisateur statique
            User currentUser = userService.getById(1);
            if (currentUser == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", 
                         "Utilisateur non trouvé", "L'utilisateur avec ID=1 n'existe pas.");
                return;
            }
            
            Commentaire commentaire = new Commentaire();
            commentaire.setContenuComment(txtContenu.getText());
            commentaire.setDateComment(LocalDateTime.now());
            commentaire.setUser(currentUser);
            commentaire.setSondage(selectedSondage);
            
            commentaireService.add(commentaire);
            
            resetForm();
            loadCommentairesBySondage(selectedSondage);
            
            showAlert(Alert.AlertType.INFORMATION, "Succès", 
                     "Commentaire ajouté", "Votre commentaire a été ajouté avec succès.");
                     
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors de l'ajout du commentaire", e.getMessage());
        }
    }
    
    /**
     * Met à jour un commentaire existant
     */
    @FXML
    private void updateCommentaire() {
        if (currentCommentaire == null) {
            return;
        }
        
        if (txtContenu.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Contenu requis", "Veuillez saisir un commentaire.");
            return;
        }
        
        try {
            // Vérifier si l'utilisateur est l'auteur du commentaire
            User currentUser = userService.getById(1);
            if (currentUser == null || currentCommentaire.getUser() == null || 
                currentCommentaire.getUser().getId() != currentUser.getId()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", 
                         "Non autorisé", "Vous ne pouvez modifier que vos propres commentaires.");
                return;
            }
            
            currentCommentaire.setContenuComment(txtContenu.getText());
            currentCommentaire.setDateComment(LocalDateTime.now()); // Mettre à jour la date
            
            commentaireService.update(currentCommentaire);
            
            resetForm();
            loadCommentairesBySondage(currentCommentaire.getSondage());
            
            showAlert(Alert.AlertType.INFORMATION, "Succès", 
                     "Commentaire modifié", "Votre commentaire a été modifié avec succès.");
                     
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors de la modification du commentaire", e.getMessage());
        }
    }
    
    /**
     * Prépare le formulaire pour modifier un commentaire existant
     */
    private void editCommentaire(Commentaire commentaire) {
        // Vérifier si l'utilisateur est l'auteur du commentaire
        try {
            User currentUser = userService.getById(1);
            if (currentUser == null || commentaire.getUser() == null || 
                commentaire.getUser().getId() != currentUser.getId()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", 
                         "Non autorisé", "Vous ne pouvez modifier que vos propres commentaires.");
                return;
            }
            
            currentCommentaire = commentaire;
            txtContenu.setText(commentaire.getContenuComment());
            
            // Sélectionner le sondage correspondant
            cbSondages.getSelectionModel().select(commentaire.getSondage());
            
            // Passer en mode édition
            editMode = true;
            btnSave.setVisible(false);
            btnUpdate.setVisible(true);
            
            // Désactiver la sélection du sondage
            cbSondages.setDisable(true);
            
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors du chargement de l'utilisateur", e.getMessage());
        }
    }
    
    /**
     * Supprime un commentaire
     */
    private void deleteCommentaire(Commentaire commentaire) {
        try {
            // Vérifier si l'utilisateur est l'auteur du commentaire
            User currentUser = userService.getById(1);
            if (currentUser == null || commentaire.getUser() == null || 
                commentaire.getUser().getId() != currentUser.getId()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", 
                         "Non autorisé", "Vous ne pouvez supprimer que vos propres commentaires.");
                return;
            }
            
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Confirmation de suppression");
            confirmDialog.setHeaderText("Supprimer le commentaire");
            confirmDialog.setContentText("Êtes-vous sûr de vouloir supprimer ce commentaire ?");
            
            Optional<ButtonType> result = confirmDialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                commentaireService.delete(commentaire.getId());
                
                loadCommentairesBySondage(commentaire.getSondage());
                
                showAlert(Alert.AlertType.INFORMATION, "Succès", 
                         "Commentaire supprimé", "Votre commentaire a été supprimé avec succès.");
            }
            
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Erreur lors de la suppression", e.getMessage());
        }
    }
    
    /**
     * Réinitialise le formulaire
     */
    private void resetForm() {
        txtContenu.clear();
        
        // Réinitialiser l'état
        currentCommentaire = null;
        editMode = false;
        
        // Réinitialiser les boutons
        btnSave.setVisible(true);
        btnUpdate.setVisible(false);
        
        // Réactiver la sélection du sondage
        cbSondages.setDisable(false);
    }
    
    /**
     * Affiche une boîte de dialogue
     */
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
} 
package com.esprit.controllers.crud;

import com.esprit.ProduitApp;
import com.esprit.models.Commande;
import com.esprit.models.Produit;
import com.esprit.models.enums.StatutCommandeEnum;
import com.esprit.services.CommandeService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.beans.property.SimpleStringProperty;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class CommandeController implements Initializable {

    @FXML private TableView<Commande> tableCommandes;
    @FXML private TableColumn<Commande, String> colDate;
    @FXML private TableColumn<Commande, String> colStatut;
    @FXML private TableColumn<Commande, String> colActions;

    @FXML private DatePicker dateCommande;
    @FXML private ComboBox<String> cbStatut;
    @FXML private Button btnSave;
    @FXML private Button btnUpdate;
    @FXML private Button btnCancel;

    private final CommandeService commandeService = new CommandeService();
    private Commande currentCommande;
    private boolean editMode = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        setupButtons();
        setupComboBox();
        loadCommandes();
    }

    private void setupTable() {
        colDate.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDateComm().toString()));
        colStatut.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatut().name()));

        colActions.setCellFactory(col -> new TableCell<Commande, String>() {
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final Button btnValider = new Button("Valider");

            {
                btnEdit.setOnAction(e -> editCommande(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> deleteCommande(getTableView().getItems().get(getIndex())));
                btnValider.setOnAction(e -> validerCommande(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(5, btnEdit, btnDelete, btnValider);
                    box.setPadding(new Insets(0, 0, 0, 5));
                    setGraphic(box);
                }
            }
        });
    }

    private void setupButtons() {
        btnSave.setOnAction(e -> saveCommande());
        btnUpdate.setOnAction(e -> updateCommande());
        btnCancel.setOnAction(e -> resetForm());

        btnUpdate.setVisible(false);
    }

    private void setupComboBox() {
        cbStatut.setItems(FXCollections.observableArrayList(
                StatutCommandeEnum.EN_COURS.name(),
                StatutCommandeEnum.CONFIRMEE.name(),
                StatutCommandeEnum.ANNULEE.name()
        ));
    }

    private void loadCommandes() {
        ObservableList<Commande> commandes = FXCollections.observableArrayList(
                commandeService.getAllCommandes(null)
        );
        tableCommandes.setItems(commandes);
    }

    private void saveCommande() {
        if (dateCommande.getValue() == null || cbStatut.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Tous les champs sont requis", "Veuillez compléter tous les champs.");
            return;
        }

        Commande cmd = new Commande();
        cmd.setDateComm(dateCommande.getValue());
        cmd.setStatut(StatutCommandeEnum.valueOf(cbStatut.getValue()));
        // TODO : ajouter utilisateur et détails si nécessaire

        commandeService.createCommande(cmd);
        showAlert(Alert.AlertType.INFORMATION, "Succès", "Commande enregistrée", "La commande a été ajoutée avec succès.");

        resetForm();
        loadCommandes();
    }

    private void updateCommande() {
        if (currentCommande == null) return;

        currentCommande.setDateComm(dateCommande.getValue());
        currentCommande.setStatut(StatutCommandeEnum.valueOf(cbStatut.getValue()));
        // TODO : ajouter logique de mise à jour dans le service si nécessaire

        showAlert(Alert.AlertType.INFORMATION, "Info", "Mise à jour", "La commande a été mise à jour.");
        resetForm();
        loadCommandes();
    }

    private void deleteCommande(Commande cmd) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la commande ?");
        confirm.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            commandeService.supprimerCommande(cmd.getId());
            loadCommandes();
        }
    }

    private void validerCommande(Commande cmd) {
        commandeService.validerCommande(cmd.getId());
        loadCommandes();
        showAlert(Alert.AlertType.INFORMATION, "Commande validée", null, "La commande a été confirmée.");
    }

    private void editCommande(Commande cmd) {
        currentCommande = cmd;
        dateCommande.setValue(cmd.getDateComm());
        cbStatut.setValue(cmd.getStatut().name());

        editMode = true;
        btnSave.setVisible(false);
        btnUpdate.setVisible(true);
    }

    private void resetForm() {
        currentCommande = null;
        editMode = false;

        dateCommande.setValue(null);
        cbStatut.setValue(null);
        btnSave.setVisible(true);
        btnUpdate.setVisible(false);
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    @FXML
    private void goToProduit(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/produit/ProduitView.fxml"));
            Parent root = loader.load();

            ProduitApp.getPrimaryStage().getScene().setRoot(root);
            ProduitApp.adjustStageSize(false); // Si tu veux resize

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

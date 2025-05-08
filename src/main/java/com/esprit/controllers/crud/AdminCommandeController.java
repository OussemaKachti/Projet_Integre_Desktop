package com.esprit.controllers.crud;

import com.esprit.models.Commande;
import com.esprit.models.enums.StatutCommandeEnum;
import com.esprit.services.CommandeService;
import com.esprit.utils.AlertUtilsSirine;
import com.esprit.utils.DataSource;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

public class AdminCommandeController implements Initializable {

    // Table components
    @FXML
    private TableView<Commande> tableView;
    @FXML
    private TableColumn<Commande, Integer> colId;
    @FXML
    private TableColumn<Commande, String> colUser;
    @FXML
    private TableColumn<Commande, Double> colTotal;
    @FXML
    private TableColumn<Commande, String> colStatus;
    @FXML
    private TableColumn<Commande, LocalDate> colDate;
    @FXML
    private TableColumn<Commande, Void> colActions;

    // Filter components
    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> filterStatusComboBox;

    // Statistics labels
    @FXML
    private Label totalCommandesLabel;
    @FXML
    private Label pendingCommandesLabel;
    @FXML
    private Label completedCommandesLabel;
    @FXML
    private Label cancelledCommandesLabel;

    // Containers
    @FXML
    private HBox paginationContainer;
    @FXML
    private VBox noCommandesContainer;
    @FXML
    private Pane toastContainer;

    // Navigation buttons
    @FXML
    private Button userManagementBtn;
    @FXML
    private Button clubManagementBtn;
    @FXML
    private Button eventManagementBtn;
    @FXML
    private Button productOrdersBtn;
    @FXML
    private Button competitionBtn;
    @FXML
    private Button surveyManagementBtn;
    @FXML
    private Button profileBtn;
    @FXML
    private Button logoutBtn;
    @FXML
    private Label adminNameLabel;

    // Service
    private final CommandeService commandeService;

    // Data lists
    private ObservableList<Commande> commandeList;
    private FilteredList<Commande> filteredList;

    // Pagination
    private int currentPage = 1;
    private static final int ITEMS_PER_PAGE = 5;
    private int totalPages = 1;

    public AdminCommandeController() {
        this.commandeService = new CommandeService();
        this.commandeList = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            if (!DataSource.getInstance().isConnected()) {
                showDatabaseConnectionError();
                return;
            }

            setupTableColumns();
            setupFilters();
            loadAllCommandes();
            calculateStats();
            setupPagination();
            setupNavigationEvents();
            setupAdminInfo();

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtilsSirine.showError("Erreur d'initialisation", "Erreur lors du chargement de l'interface",
                    "Une erreur est survenue: " + e.getMessage());
        }
    }

    private void showDatabaseConnectionError() {
        if (tableView != null) {
            tableView.setPlaceholder(new Label("Impossible de se connecter à la base de données"));
        }

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur de connexion");
        alert.setHeaderText("Impossible de se connecter à la base de données");
        alert.setContentText(
                "Vérifiez que le serveur MySQL est démarré et que les paramètres de connexion sont corrects.");

        ButtonType retryButton = new ButtonType("Réessayer");
        ButtonType exitButton = new ButtonType("Quitter", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(retryButton, exitButton);

        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == retryButton) {
                if (DataSource.getInstance().isConnected()) {
                    initialize(null, null);
                } else {
                    showDatabaseConnectionError();
                }
            } else if (buttonType == exitButton) {
                Platform.exit();
            }
        });
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colUser.setCellValueFactory(cellData -> {
            if (cellData.getValue().getUser() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getUser().getFirstName() + " "
                                + cellData.getValue().getUser().getLastName());
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });

        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                if (empty || total == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f €", total));
                }
            }
        });

        colStatus.setCellValueFactory(cellData -> {
            StatutCommandeEnum statut = cellData.getValue().getStatut();
            return new javafx.beans.property.SimpleStringProperty(statut != null ? statut.name() : "N/A");
        });
        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "EN_COURS":
                            setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                            break;
                        case "CONFIRMEE":
                            setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                            break;
                        case "ANNULEE":
                            setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });

        colDate.setCellValueFactory(new PropertyValueFactory<>("dateComm"));

        setupActionsColumn();
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(tc -> new TableCell<>() {
            private final Button validateButton = new Button("Valider");
            private final Button cancelButton = new Button("Annuler");
            private final HBox container = new HBox(5);

            {
                validateButton.setStyle(
                        "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 3;");
                validateButton.setMinWidth(80);

                cancelButton.setStyle(
                        "-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 3;");
                cancelButton.setMinWidth(80);

                validateButton.setOnMouseEntered(e -> validateButton.setStyle(
                        "-fx-background-color: #388E3C; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 3;"));
                validateButton.setOnMouseExited(e -> validateButton.setStyle(
                        "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 3;"));

                cancelButton.setOnMouseEntered(e -> cancelButton.setStyle(
                        "-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 3;"));
                cancelButton.setOnMouseExited(e -> cancelButton.setStyle(
                        "-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 3;"));

                validateButton.setOnAction(event -> {
                    Commande commande = getTableView().getItems().get(getIndex());
                    validateCommande(commande);
                });

                cancelButton.setOnAction(event -> {
                    Commande commande = getTableView().getItems().get(getIndex());
                    cancelCommande(commande);
                });

                container.setAlignment(Pos.CENTER);
                container.getChildren().addAll(validateButton, cancelButton);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Commande commande = getTableView().getItems().get(getIndex());
                    validateButton.setDisable(commande.getStatut() != StatutCommandeEnum.EN_COURS);
                    cancelButton.setDisable(commande.getStatut() != StatutCommandeEnum.EN_COURS);
                    setGraphic(container);
                }
            }
        });
    }

    private void setupFilters() {
        filteredList = new FilteredList<>(commandeList);

        if (txtSearch != null) {
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredList.setPredicate(commande -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    String lowerCaseFilter = newValue.toLowerCase();
                    return (commande.getUser() != null &&
                            (commande.getUser().getFirstName().toLowerCase().contains(lowerCaseFilter) ||
                                    commande.getUser().getLastName().toLowerCase().contains(lowerCaseFilter)))
                            ||
                            commande.getStatut().name().toLowerCase().contains(lowerCaseFilter) ||
                            String.valueOf(commande.getId()).contains(newValue);
                });
                updatePagination();
            });
        }

        ObservableList<String> statusOptions = FXCollections.observableArrayList(
                "Tous les statuts",
                StatutCommandeEnum.EN_COURS.name(),
                StatutCommandeEnum.CONFIRMEE.name(),
                StatutCommandeEnum.ANNULEE.name());

        if (filterStatusComboBox != null) {
            filterStatusComboBox.setItems(statusOptions);
            filterStatusComboBox.setValue("Tous les statuts");

            filterStatusComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                String selectedStatus = "Tous les statuts".equals(newValue) ? "all" : newValue;

                filteredList.setPredicate(commande -> {
                    boolean matchesStatus = "all".equals(selectedStatus) ||
                            commande.getStatut().name().equals(selectedStatus);

                    String searchText = txtSearch.getText();
                    boolean matchesSearch = searchText == null || searchText.isEmpty() ||
                            (commande.getUser() != null &&
                                    (commande.getUser().getFirstName().toLowerCase().contains(searchText.toLowerCase())
                                            ||
                                            commande.getUser().getLastName().toLowerCase()
                                                    .contains(searchText.toLowerCase())))
                            ||
                            commande.getStatut().name().toLowerCase().contains(searchText.toLowerCase()) ||
                            String.valueOf(commande.getId()).contains(searchText);

                    return matchesStatus && matchesSearch;
                });

                currentPage = 1;
                updatePagination();
            });
        }
    }

    @FXML
    private void searchCommandes() {
        setupFilters();
    }

    private void loadAllCommandes() {
        try {
            tableView.setPlaceholder(new Label("Chargement des commandes..."));

            List<Commande> commandes = commandeService.getAllCommandes(null);
            commandeList.clear();
            commandeList.addAll(commandes);

            filteredList = new FilteredList<>(commandeList);

            if (commandeList.isEmpty()) {
                noCommandesContainer.setVisible(true);
                tableView.setVisible(false);
                paginationContainer.setVisible(false);
            } else {
                noCommandesContainer.setVisible(false);
                tableView.setVisible(true);
                paginationContainer.setVisible(true);
                updatePagination();
            }
        } catch (Exception e) {
            AlertUtilsSirine.showError("Erreur", "Erreur lors du chargement des commandes", e.getMessage());
            tableView.setPlaceholder(new Label("Erreur lors du chargement des commandes"));
        }
    }

    private void validateCommande(Commande commande) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Valider la Commande");
        confirmDialog.setHeaderText("Êtes-vous sûr de vouloir valider cette commande ?");
        confirmDialog.setContentText("Cette action ne peut pas être annulée.");

        if (confirmDialog.showAndWait().filter(response -> response == ButtonType.OK).isPresent()) {
            try {
                commandeService.validerCommande(commande.getId());

                String userEmail = commande.getUser().getEmail();
                if (userEmail != null && !userEmail.isEmpty()) {
                    try {
                        sendEmail(userEmail, commande);
                        showToast("Email envoyé avec succès à " + userEmail, "success");
                    } catch (MessagingException e) {
                        showToast("Échec de l'envoi de l'email : " + e.getMessage(), "error");
                    }
                }

                showToast("Commande validée avec succès", "success");
                loadAllCommandes();
                calculateStats();
            } catch (Exception e) {
                showToast("Erreur lors de la validation : " + e.getMessage(), "error");
                e.printStackTrace();
            }
        }
    }

    private void cancelCommande(Commande commande) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Annuler la Commande");
        confirmDialog.setHeaderText("Êtes-vous sûr de vouloir annuler cette commande ?");
        confirmDialog.setContentText("Cette action ne peut pas être annulée.");

        if (confirmDialog.showAndWait().filter(response -> response == ButtonType.OK).isPresent()) {
            try {
                commandeService.supprimerCommande(commande.getId());
                showToast("Commande annulée avec succès", "success");
                loadAllCommandes();
                calculateStats();
            } catch (Exception e) {
                showToast("Erreur lors de l'annulation : " + e.getMessage(), "error");
                e.printStackTrace();
            }
        }
    }

    private void sendEmail(String userEmail, Commande commande) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.debug", "true");

        final String senderEmail = "wahbisirine3@gmail.com";
        final String senderPassword = "rmiv tndu ffjc deob";

        Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
            @Override
            protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                return new jakarta.mail.PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(senderEmail));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(userEmail));
        message.setSubject("Confirmation de votre commande #" + commande.getId());
        message.setText("Bonjour " + commande.getUser().getFirstName() + ",\n\n" +
                "Votre commande #" + commande.getId() + " a été validée avec succès.\n" +
                "Montant total : " + String.format("%.2f €", commande.getTotal()) + "\n" +
                "Date de commande : " + commande.getDateComm() + "\n\n" +
                "Merci de votre confiance !\n" +
                "L'équipe de gestion");

        Transport.send(message);
    }

    private void calculateStats() {
        try {
            List<Commande> allCommandes = commandeService.getAllCommandes(null);

            int totalCommandes = allCommandes.size();
            totalCommandesLabel.setText(String.valueOf(totalCommandes));

            int pendingCommandes = 0;
            int completedCommandes = 0;
            int cancelledCommandes = 0;

            for (Commande commande : allCommandes) {
                switch (commande.getStatut()) {
                    case EN_COURS:
                        pendingCommandes++;
                        break;
                    case CONFIRMEE:
                        completedCommandes++;
                        break;
                    case ANNULEE:
                        cancelledCommandes++;
                        break;
                }
            }

            pendingCommandesLabel.setText(String.valueOf(pendingCommandes));
            completedCommandesLabel.setText(String.valueOf(completedCommandes));
            cancelledCommandesLabel.setText(String.valueOf(cancelledCommandes));

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur lors du calcul des statistiques : " + e.getMessage());
        }
    }

    private void setupPagination() {
        if (paginationContainer == null)
            return;

        paginationContainer.getChildren().clear();

        if (totalPages <= 1) {
            paginationContainer.setVisible(false);
            paginationContainer.setManaged(false);
            return;
        }

        paginationContainer.setVisible(true);
        paginationContainer.setManaged(true);

        Button prevButton = new Button("←");
        prevButton.getStyleClass().add(currentPage == 1 ? "pagination-button-disabled" : "pagination-button");
        prevButton.setDisable(currentPage == 1);
        prevButton.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                loadCurrentPage();
                setupPagination();
            }
        });

        paginationContainer.getChildren().add(prevButton);

        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(startPage + 4, totalPages);

        for (int i = startPage; i <= endPage; i++) {
            Button pageButton = new Button(String.valueOf(i));
            pageButton.getStyleClass().addAll("pagination-button");

            if (i == currentPage) {
                pageButton.getStyleClass().add("pagination-button-active");
                pageButton.setStyle("-fx-font-weight: bold; -fx-background-color: #6200EE; -fx-text-fill: white;");
            } else {
                pageButton.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #333333;");
            }

            final int pageNum = i;
            pageButton.setOnAction(e -> {
                currentPage = pageNum;
                loadCurrentPage();
                setupPagination();
            });
            paginationContainer.getChildren().add(pageButton);
        }

        Button nextButton = new Button("→");
        nextButton.getStyleClass().add(currentPage == totalPages ? "pagination-button-disabled" : "pagination-button");
        nextButton.setDisable(currentPage == totalPages);
        nextButton.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                loadCurrentPage();
                setupPagination();
            }
        });

        paginationContainer.getChildren().add(nextButton);

        Label pageInfoLabel = new Label(String.format("Page %d sur %d", currentPage, totalPages));
        pageInfoLabel.setStyle("-fx-text-fill: #6c757d; -fx-padding: 5 0 0 10;");
        paginationContainer.getChildren().add(pageInfoLabel);
    }

    private void loadCurrentPage() {
        int fromIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, filteredList.size());

        ObservableList<Commande> currentPageList;
        if (fromIndex < toIndex) {
            currentPageList = FXCollections.observableArrayList(filteredList.subList(fromIndex, toIndex));
        } else {
            currentPageList = FXCollections.observableArrayList();
        }

        tableView.setItems(currentPageList);
    }

    private void updatePagination() {
        if (paginationContainer != null) {
            int itemCount = filteredList.size();
            totalPages = (itemCount + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;

            if (totalPages < 1) {
                totalPages = 1;
            }

            if (currentPage > totalPages) {
                currentPage = totalPages;
            }

            loadCurrentPage();
            setupPagination();
        }
    }

    private void setupNavigationEvents() {
        userManagementBtn.setOnAction(e -> goToAdminDashboard());
        clubManagementBtn.setOnAction(e -> goToClubManagement());
        eventManagementBtn.setOnAction(e -> goToEventManagement());
        competitionBtn.setOnAction(e -> goToCompetition());
        surveyManagementBtn.setOnAction(e -> goToSurveyManagement());
        profileBtn.setOnAction(e -> goToProfile());
        logoutBtn.setOnAction(e -> handleLogout());
    }

    private void setupAdminInfo() {
        if (adminNameLabel != null) {
            adminNameLabel.setText("Admin User");
        }
    }

    private void showToast(String message, String type) {
        if (toastContainer != null) {
            toastContainer.setVisible(true);
            HBox toast = (HBox) toastContainer.getChildren().get(0);

            // Set background color based on type
            String backgroundColor = type.equals("error") ? "#dc3545" : "#28a745";
            toast.setStyle("-fx-background-color: " + backgroundColor
                    + "; -fx-background-radius: 4px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 10);");

            // Set message
            Label messageLabel = (Label) toast.getChildren().get(0);
            messageLabel.setText(message);

            // Create fade transition
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toastContainer);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toastContainer);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setDelay(Duration.seconds(2));

            fadeIn.play();
            fadeOut.play();

            fadeOut.setOnFinished(e -> toastContainer.setVisible(false));
        }
    }

    // Navigation methods
    @FXML
    private void goToAdminDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/admin/AdminDashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) userManagementBtn.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showToast("Erreur lors de la navigation", "error");
        }
    }

    @FXML
    private void goToClubManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/club/AdminClubView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) clubManagementBtn.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showToast("Erreur lors de la navigation", "error");
        }
    }

    @FXML
    private void goToEventManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/event/AdminEventView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) eventManagementBtn.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showToast("Erreur lors de la navigation", "error");
        }
    }

    @FXML
    private void goToCompetition() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/esprit/views/competition/AdminCompetitionView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) competitionBtn.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showToast("Erreur lors de la navigation", "error");
        }
    }

    @FXML
    private void goToSurveyManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/survey/AdminSurveyView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) surveyManagementBtn.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showToast("Erreur lors de la navigation", "error");
        }
    }

    @FXML
    private void goToProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/esprit/views/profile/AdminProfileView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) profileBtn.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showToast("Erreur lors de la navigation", "error");
        }
    }

    private void handleLogout() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Déconnexion");
        confirmDialog.setHeaderText("Êtes-vous sûr de vouloir vous déconnecter ?");
        confirmDialog.setContentText("Toutes les modifications non enregistrées seront perdues.");

        if (confirmDialog.showAndWait().filter(response -> response == ButtonType.OK).isPresent()) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/login.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) logoutBtn.getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setMaximized(false);
                stage.setWidth(900);
                stage.setHeight(600);
                stage.centerOnScreen();
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showToast("Erreur lors de la déconnexion", "error");
            }
        }
    }
}
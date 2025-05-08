package com.esprit.controllers;

import com.esprit.models.Club;
import com.esprit.services.ClubService;
import com.esprit.utils.SessionManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.esprit.utils.NavigationManager.navigateTo;

public class ClubController {

    @FXML
    private Label adminNameLabel;
    @FXML
    private Label contentTitle;
    @FXML
    private TextField idField;
    @FXML
    private TextField presidentIdField;
    @FXML
    private TextField nomCField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private TextField statusField;
    @FXML
    private TextField imageField;
    @FXML
    private TextField pointsField;
    @FXML
    private TextField searchField;
    @FXML
    private ListView<Club> clubList;
    @FXML
    private BarChart<Number, String> statsChart;
    @FXML
    private TabPane tabPane;
    @FXML
    private Button userManagementButton;
    @FXML
    private Button clubManagementButton;
    @FXML
    private Button participantButton;
    @FXML
    private Button eventManagementButton;
    @FXML
    private Button productOrdersButton;
    @FXML
    private Button competitionButton;
    @FXML
    private Button surveyButton;
    @FXML
    private VBox surveySubMenu;
    @FXML
    private Button profileButton;
    @FXML
    private Button logoutButton;

    private final ClubService clubService = new ClubService();
    private final ObservableList<Club> clubs = FXCollections.observableArrayList();
    private Club selectedClub = null;
    private List<Club> allClubs;

    @FXML
    public void initialize() {
        try {
            // Load current admin user
            var currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null || !"ADMINISTRATEUR".equals(currentUser.getRole().toString())) {
                // Display alert instead of navigating
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Authentication Error");
                alert.setHeaderText(null);
                alert.setContentText("You must be logged in as an administrator to access this page.");
                alert.showAndWait();
                return;
            }

            // Set admin name
            adminNameLabel.setText(currentUser.getFirstName() + " " + currentUser.getLastName());

            // Set initial content title
            contentTitle.setText("Club Management");
            setActiveButton(clubManagementButton);

            // Load clubs and initialize UI
            loadClubs();
            allClubs = clubService.afficher();

            clubList.setCellFactory(param -> new ListCell<Club>() {
                @Override
                protected void updateItem(Club club, boolean empty) {
                    super.updateItem(club, empty);
                    if (empty || club == null) {
                        setText(null);
                    } else {
                        setText(club.getNomC());
                    }
                }
            });

            clubList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    selectedClub = newVal;
                    idField.setText(String.valueOf(newVal.getId()));
                    presidentIdField.setText(String.valueOf(newVal.getPresidentId()));
                    nomCField.setText(newVal.getNomC());
                    descriptionField.setText(newVal.getDescription());
                    statusField.setText(newVal.getStatus());
                    imageField.setText(newVal.getImage());
                    pointsField.setText(String.valueOf(newVal.getPoints()));
                }
            });

            tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab.getText().equals("Statistiques")) {
                    Platform.runLater(() -> {
                        System.out.println("Onglet Statistiques sélectionné. Chargement des statistiques...");
                        loadStatistics();
                    });
                }
            });

            // Setup search functionality
            searchField.textProperty().addListener((observable, oldValue, newValue) -> searchClubs());
        } catch (SQLException e) {
            showError("Erreur lors du chargement des clubs: " + e.getMessage());
        }
    }

    private void loadClubs() throws SQLException {
        clubs.clear();
        clubs.addAll(clubService.afficher());
        clubList.setItems(clubs);
    }

    @FXML
    private void accepterClub() {
        if (selectedClub == null) {
            showError("Veuillez sélectionner un club.");
            return;
        }

        selectedClub.setStatus("accepte");
        clubService.modifier(selectedClub);
        refreshClubList();
        clearForm();
        showSuccess("Club accepté avec succès !");
    }

    @FXML
    private void refuserClub() {
        if (selectedClub == null) {
            showError("Veuillez sélectionner un club.");
            return;
        }

        selectedClub.setStatus("Refusé");
        clubService.modifier(selectedClub);
        refreshClubList();
        clearForm();
        showSuccess("Club refusé avec succès !");
    }

    @FXML
    private void supprimerClub() {
        if (selectedClub == null) {
            showError("Veuillez sélectionner un club à supprimer.");
            return;
        }

        clubService.supprimer(selectedClub.getId());
        refreshClubList();
        clearForm();
        showSuccess("Club supprimé avec succès !");
    }

    @FXML
    private void searchClubs() {
        String searchText = searchField.getText().trim().toLowerCase();
        if (searchText.isEmpty()) {
            clubs.setAll(allClubs);
        } else {
            List<Club> filteredClubs = allClubs.stream()
                    .filter(club -> club.getNomC().toLowerCase().contains(searchText))
                    .collect(Collectors.toList());
            clubs.setAll(filteredClubs);
        }
        clubList.setItems(clubs);
    }

    private void loadStatistics() {
        try {
            if (statsChart == null) {
                showError("Le graphique n'a pas pu être initialisé.");
                return;
            }

            statsChart.setAnimated(false);
            statsChart.getData().clear();

            List<Object[]> popularityStats;
            try {
                popularityStats = clubService.getClubsByPopularity();
            } catch (Exception e) {
                popularityStats = new ArrayList<>();
                popularityStats.add(new Object[] { "Club A", 3 });
                popularityStats.add(new Object[] { "Club B", 2 });
                popularityStats.add(new Object[] { "Club C", 1 });
                showError("Erreur lors de la récupération des statistiques. Affichage de données fictives.");
            }

            if (popularityStats.isEmpty()) {
                showError("Aucune donnée de popularité disponible.");
                return;
            }

            XYChart.Series<Number, String> series = new XYChart.Series<>();
            series.setName("Participations");

            for (Object[] stat : popularityStats) {
                String clubName = (String) stat[0];
                int participationCount = ((Number) stat[1]).intValue();
                if (clubName == null || clubName.trim().isEmpty()) {
                    clubName = "Club Inconnu";
                }
                series.getData().add(new XYChart.Data<>(participationCount, clubName));
            }

            statsChart.getData().add(series);

            Platform.runLater(() -> {
                statsChart.applyCss();
                statsChart.requestLayout();
                Set<Node> nodes = statsChart.lookupAll(".chart-bar");
                for (Node node : nodes) {
                    node.setStyle("-fx-bar-fill: #f39c12;");
                    node.setOnMouseEntered(e -> node.setStyle("-fx-bar-fill: #e67e22;"));
                    node.setOnMouseExited(e -> node.setStyle("-fx-bar-fill: #f39c12;"));
                }

                if (statsChart.getXAxis() instanceof NumberAxis xAxis) {
                    xAxis.setAutoRanging(true);
                    xAxis.setForceZeroInRange(true);
                    xAxis.setLowerBound(0);
                    xAxis.setUpperBound(Math.max(5, series.getData().stream()
                            .mapToDouble(data -> data.getXValue().doubleValue())
                            .max()
                            .orElse(5)));
                    xAxis.setTickUnit(1);
                }
            });
        } catch (Exception e) {
            showError("Erreur inattendue lors du chargement des statistiques: " + e.getMessage());
        }
    }

    private void refreshClubList() {
        try {
            clubs.clear();
            allClubs = clubService.afficher();
            clubs.addAll(allClubs);
            clubList.refresh();
        } catch (Exception e) {
            showError("Erreur lors du rafraîchissement de la liste: " + e.getMessage());
            clubs.setAll(allClubs);
            clubList.refresh();
        }
    }

    private void clearForm() {
        idField.clear();
        presidentIdField.clear();
        nomCField.clear();
        descriptionField.clear();
        statusField.clear();
        imageField.clear();
        pointsField.clear();
        selectedClub = null;
        clubList.getSelectionModel().clearSelection();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void showUserManagement(ActionEvent actionEvent) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/esprit/views/admin_dashboard.fxml"));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(
                getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm(),
                getClass().getResource("/com/esprit/styles/no-scrollbar.css").toExternalForm());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    @FXML
    private void showClubManagement(ActionEvent actionEvent) {
        contentTitle.setText("Club Management");
        setActiveButton(clubManagementButton);
        // Already in Club Management view, no need to navigate
    }

    @FXML
    public void showParticipant() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/ParticipantView.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) clubList.getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(
                getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm(),
                getClass().getResource("/com/esprit/styles/no-scrollbar.css").toExternalForm());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    @FXML
    public void showCompetition(ActionEvent actionEvent) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/esprit/views/AdminCompetition.fxml"));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(
                getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm(),
                getClass().getResource("/com/esprit/styles/no-scrollbar.css").toExternalForm());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    @FXML
    public void showSurvey(ActionEvent actionEvent) {
        contentTitle.setText("Survey Management");
        setActiveButton(surveyButton);
        boolean isVisible = surveySubMenu.isVisible();
        surveySubMenu.setVisible(!isVisible);
        surveySubMenu.setManaged(!isVisible);
    }

    @FXML
    public void navigateToPollsManagement(ActionEvent actionEvent) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/esprit/views/AdminPollsView.fxml"));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(
                getClass().getResource("/com/esprit/styles/admin-polls-style.css").toExternalForm(),
                getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    @FXML
    public void navigateToCommentsManagement(ActionEvent actionEvent) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/esprit/views/AdminCommentsView.fxml"));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(
                getClass().getResource("/com/esprit/styles/admin-polls-style.css").toExternalForm(),
                getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    @FXML
    public void navigateToProfile(ActionEvent actionEvent) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/esprit/views/admin_profile.fxml"));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm());
        stage.setTitle("Admin Profile - UNICLUBS");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    @FXML
    public void handleLogout(ActionEvent actionEvent) throws IOException {
        SessionManager.getInstance().clearSession();
        Parent root = FXMLLoader.load(getClass().getResource("/com/esprit/views/login.fxml"));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm());
        stage.setTitle("Login - UNICLUBS");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    private void setActiveButton(Button activeButton) {
        for (Button btn : new Button[] { userManagementButton, clubManagementButton,
                eventManagementButton, productOrdersButton, competitionButton, surveyButton }) {
            if (btn != null) {
                btn.getStyleClass().remove("active");
            }
        }
        if (activeButton != null) {
            activeButton.getStyleClass().add("active");
        }
    }

    @FXML
    public void showClub(ActionEvent actionEvent) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/esprit/views/ClubView.fxml"));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    @FXML
    public void showEventManagement(ActionEvent actionEvent) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/esprit/views/AdminEvent.fxml"));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    @FXML
    public void showProductOrders(ActionEvent actionEvent) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/esprit/views/produit/AdminProduitView.fxml"));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

}
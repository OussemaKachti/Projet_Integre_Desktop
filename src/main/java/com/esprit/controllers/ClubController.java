package com.esprit.controllers;

import com.esprit.models.Club;
import com.esprit.services.ClubService;
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
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ClubController {

    @FXML private TextField idField;
    @FXML private TextField presidentIdField;
    @FXML private TextField nomCField;
    @FXML private TextArea descriptionField;
    @FXML private TextField statusField;
    @FXML private TextField imageField;
    @FXML private TextField pointsField;
    @FXML private TextField searchField;
    @FXML private ListView<Club> clubList;
    @FXML private BarChart<String, Number> statsChart;
    @FXML private TabPane tabPane;

    private final ClubService clubService = new ClubService();
    private final ObservableList<Club> clubs = FXCollections.observableArrayList();
    private Club selectedClub = null;
    private List<Club> allClubs; // To store the full list for filtering

    @FXML
    public void initialize() {
        // Charger les clubs dans la ListView au démarrage
        try {
            loadClubs();
            allClubs = clubService.afficher(); // Store the full list for filtering

            // Configurer l'affichage personnalisé dans la ListView
            clubList.setCellFactory(param -> new ListCell<Club>() {
                @Override
                protected void updateItem(Club club, boolean empty) {
                    super.updateItem(club, empty);
                    if (empty || club == null) {
                        setText(null);
                    } else {
                        setText(club.getNomC()); // Afficher le nom du club
                    }
                }
            });

            // Écouteur pour sélectionner un club dans la ListView
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

            // Écouteur pour charger les statistiques quand l'onglet "Statistiques" est sélectionné
            tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab.getText().equals("Statistiques")) {
                    // Délayer l'appel à loadStatistics pour s'assurer que le TabPane est prêt
                    Platform.runLater(() -> {
                        System.out.println("Onglet Statistiques sélectionné. Chargement des statistiques...");
                        loadStatistics();
                    });
                }
            });
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
    private void ajouterClub() {
        // Vérifier si les champs obligatoires sont vides
        if (idField.getText().isEmpty() ||
                presidentIdField.getText().isEmpty() ||
                nomCField.getText().isEmpty() ||
                descriptionField.getText().isEmpty() ||
                imageField.getText().isEmpty()) {

            showError("Tous les champs doivent être remplis !");
            return;
        }

        String nomC = nomCField.getText();
        String description = descriptionField.getText();

        // ✅ Bloquer les caractères spéciaux dans le nom
        if (!nomC.matches("[a-zA-Z0-9À-ÿ\\s.,!?'-]+")) {
            showError("Le nom du club contient des caractères non autorisés.");
            return;
        }

        // ✅ Vérifier que la description ne contient pas de caractères spéciaux
        if (!description.matches("[a-zA-Z0-9À-ÿ\\s.,!?'-]+")) {
            showError("La description contient des caractères non autorisés.");
            return;
        }

        // ✅ Vérifier la longueur de description (max 30 mots)
        if (description.trim().split("\\s+").length > 30) {
            showError("La description ne doit pas dépasser 30 mots.");
            return;
        }

        try {
            int id = Integer.parseInt(idField.getText());
            int presidentId = Integer.parseInt(presidentIdField.getText());
            String image = imageField.getText();

            // ✅ Statut et points par défaut
            String status = "en_attente";
            int points = 0;

            Club club = new Club(id, presidentId, nomC, description, status, image, points);
            clubService.ajouter(club); // Ajouter le club dans la base de données
            refreshClubList(); // Rafraîchir la liste des clubs
            clearForm();

            showSuccess("Club ajouté avec succès !");
        } catch (NumberFormatException e) {
            showError("Veuillez entrer des valeurs numériques valides pour ID et président.");
        }
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void modifierClub() {
        if (selectedClub == null) {
            showError("Veuillez sélectionner un club à modifier.");
            return;
        }

        try {
            selectedClub.setPresidentId(Integer.parseInt(presidentIdField.getText()));
            selectedClub.setNomC(nomCField.getText());
            selectedClub.setDescription(descriptionField.getText());
            selectedClub.setStatus(statusField.getText());
            selectedClub.setImage(imageField.getText());
            selectedClub.setPoints(Integer.parseInt(pointsField.getText()));

            clubService.modifier(selectedClub); // Mettre à jour le club dans la base de données
            refreshClubList(); // Rafraîchir la liste des clubs
            clearForm();
        } catch (NumberFormatException e) {
            showError("Veuillez entrer des valeurs valides.");
        }
    }

    @FXML
    private void supprimerClub() {
        if (selectedClub == null) {
            showError("Veuillez sélectionner un club à supprimer.");
            return;
        }

        clubService.supprimer(selectedClub.getId()); // Supprimer le club de la base de données
        refreshClubList(); // Rafraîchir la liste des clubs
        clearForm();
    }

    @FXML
    private void accepterClub() {
        if (selectedClub == null) {
            showError("Veuillez sélectionner un club.");
            return;
        }

        selectedClub.setStatus("accepte");
        clubService.modifier(selectedClub); // Mettre à jour le statut
        refreshClubList(); // Rafraîchir la liste des clubs
        clearForm();
    }

    @FXML
    private void refuserClub() {
        if (selectedClub == null) {
            showError("Veuillez sélectionner un club.");
            return;
        }

        selectedClub.setStatus("Refusé");
        clubService.modifier(selectedClub); // Mettre à jour le statut
        refreshClubList(); // Rafraîchir la liste des clubs
        clearForm();
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
            // Vérifier si le graphique est initialisé
            if (statsChart == null) {
                System.out.println("Erreur : statsChart est null !");
                showError("Le graphique n'a pas pu être initialisé.");
                return;
            }

            // Vérifier la visibilité et la taille du graphique
            System.out.println("Visibilité du graphique : " + statsChart.isVisible());
            System.out.println("Taille du graphique : " + statsChart.getWidth() + "x" + statsChart.getHeight());

            // Désactiver les animations pour éviter les problèmes de rendu
            statsChart.setAnimated(false);

            // Effacer les données précédentes
            statsChart.getData().clear();
            System.out.println("Graphique effacé.");

            // Récupérer les statistiques
            List<Object[]> popularityStats;
            try {
                popularityStats = clubService.getClubsByPopularity();
            } catch (Exception e) {
                System.out.println("Erreur lors de la récupération des statistiques: " + e.getMessage());
                // Fallback avec des données fictives en cas d'erreur
                popularityStats = new ArrayList<>();
                popularityStats.add(new Object[]{"Club A", 3});
                popularityStats.add(new Object[]{"Club B", 2});
                popularityStats.add(new Object[]{"Club C", 1});
                System.out.println("Utilisation de données fictives suite à une erreur.");
                showError("Erreur lors de la récupération des statistiques. Affichage de données fictives.");
            }

            // Ajouter un message de débogage pour vérifier les données
            System.out.println("Nombre de statistiques récupérées : " + popularityStats.size());
            for (Object[] stat : popularityStats) {
                System.out.println("Club: " + stat[0] + ", Participations: " + stat[1]);
            }

            // Vérifier si les données sont vides
            if (popularityStats.isEmpty()) {
                // Fallback avec des données fictives pour tester le rendu du graphique
                popularityStats = new ArrayList<>();
                popularityStats.add(new Object[]{"Club A", 3});
                popularityStats.add(new Object[]{"Club B", 2});
                popularityStats.add(new Object[]{"Club C", 1});
                System.out.println("Aucune donnée réelle trouvée. Utilisation de données fictives pour tester le graphique.");
                showError("Aucune donnée de popularité disponible. Affichage de données fictives pour tester le graphique.");
            }

            // Préparer les données pour le BarChart (horizontal)
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Participations");

            for (Object[] stat : popularityStats) {
                String clubName = (String) stat[0];
                int participationCount = ((Number) stat[1]).intValue(); // Convertir en int de manière sécurisée
                XYChart.Data<String, Number> data = new XYChart.Data<>(clubName, participationCount);
                series.getData().add(data);
                System.out.println("Ajout des données au graphique : " + clubName + " -> " + participationCount);
            }

            // Mettre à jour le BarChart
            statsChart.getData().add(series);
            System.out.println("Données ajoutées au graphique. Nombre de points de données : " + series.getData().size());

            // Forcer la mise à jour du graphique
            Platform.runLater(() -> {
                statsChart.applyCss();
                statsChart.requestLayout(); // Forcer le redessin du graphique
                System.out.println("Après requestLayout - Taille du graphique : " + statsChart.getWidth() + "x" + statsChart.getHeight());
                Set<Node> nodes = statsChart.lookupAll(".chart-bar");
                System.out.println("Nombre de barres trouvées : " + nodes.size());
                for (Node node : nodes) {
                    node.setStyle("-fx-bar-fill: #f39c12;"); // Orange color for bars
                    node.setOnMouseEntered(e -> node.setStyle("-fx-bar-fill: #e67e22;")); // Darker orange on hover
                    node.setOnMouseExited(e -> node.setStyle("-fx-bar-fill: #f39c12;")); // Revert to original color
                }
            });
        } catch (Exception e) {
            showError("Erreur inattendue lors du chargement des statistiques: " + e.getMessage());
            e.printStackTrace(); // Ajouter un stack trace pour plus de détails
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

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void showClub(ActionEvent actionEvent) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/esprit/views/ClubView.fxml"));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    @FXML
    public void showParticipant(ActionEvent actionEvent) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/esprit/views/ParticipantView.fxml"));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }
}

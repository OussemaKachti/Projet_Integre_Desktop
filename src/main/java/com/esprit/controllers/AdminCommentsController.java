package com.esprit.controllers;

import com.esprit.models.Commentaire;
import com.esprit.models.Club;
import com.esprit.services.CommentaireService;
import com.esprit.services.ClubService;
import com.esprit.utils.AlertUtils;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminCommentsController implements Initializable {

    @FXML
    private Label totalCommentsLabel;
    
    @FXML
    private Label todayCommentsLabel;
    
    @FXML
    private Label flaggedCommentsLabel;
    
    @FXML
    private ComboBox<String> clubFilterComboBox;
    
    @FXML
    private TableView<Commentaire> commentsTable;
    
    @FXML
    private TableColumn<Commentaire, Integer> idColumn;
    
    @FXML
    private TableColumn<Commentaire, String> userColumn;
    
    @FXML
    private TableColumn<Commentaire, String> commentColumn;
    
    @FXML
    private TableColumn<Commentaire, String> clubColumn;
    
    @FXML
    private TableColumn<Commentaire, String> createdAtColumn;
    
    @FXML
    private TableColumn<Commentaire, Void> actionsColumn;
    
    @FXML
    private HBox paginationContainer;
    
    @FXML
    private Pane toastContainer;
    
    @FXML
    private VBox noCommentsContainer;
    
    private CommentaireService commentaireService;
    private ClubService clubService;
    
    private ObservableList<Commentaire> commentsList = FXCollections.observableArrayList();
    private ObservableList<String> clubsList = FXCollections.observableArrayList();
    
    // Statistiques
    private int totalComments = 0;
    private int todayComments = 0;
    private int flaggedComments = 0;
    
    // Pagination
    private int currentPage = 1;
    private final int PAGE_SIZE = 10;
    private int totalPages = 1;
    
    // Filtre sélectionné
    private String selectedClub = "all";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        commentaireService = new CommentaireService();
        clubService = new ClubService();
        
        // Configuration des colonnes du tableau
        setupTableColumns();
        
        // Chargement des clubs pour le filtre
        try {
            loadClubs();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Chargement des commentaires
        try {
            loadComments();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Calcul des statistiques
        try {
            calculateStats();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Configuration des événements
        setupEventHandlers();
        
        // Configuration de la pagination
        setupPagination();
    }
    
    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        userColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getUser() != null) {
                return new SimpleStringProperty(cellData.getValue().getUser().getNom() + " " + cellData.getValue().getUser().getPrenom());
            } else {
                return new SimpleStringProperty("Unknown");
            }
        });
        
        commentColumn.setCellValueFactory(new PropertyValueFactory<>("contenuComment"));
        
        clubColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getSondage() != null && cellData.getValue().getSondage().getClub() != null) {
                return new SimpleStringProperty(cellData.getValue().getSondage().getClub().getNomC());
            } else {
                return new SimpleStringProperty("Unknown");
            }
        });
        
        createdAtColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDateComment() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                return new SimpleStringProperty(cellData.getValue().getDateComment().format(formatter));
            } else {
                return new SimpleStringProperty("N/A");
            }
        });
        
        // Configuration de la colonne d'actions
        actionsColumn.setCellFactory(createActionButtonCellFactory());
    }
    
    private Callback<TableColumn<Commentaire, Void>, TableCell<Commentaire, Void>> createActionButtonCellFactory() {
        return new Callback<>() {
            @Override
            public TableCell<Commentaire, Void> call(final TableColumn<Commentaire, Void> param) {
                return new TableCell<>() {
                    private final Button deleteButton = new Button();
                    
                    {
                        deleteButton.getStyleClass().add("bin-button");
                        deleteButton.setPrefHeight(25);
                        deleteButton.setPrefWidth(25);
                        deleteButton.setOnAction(event -> {
                            Commentaire commentaire = getTableView().getItems().get(getIndex());
                            try {
                                deleteComment(commentaire);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                    
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(deleteButton);
                        }
                    }
                };
            }
        };
    }
    
    private void loadClubs() throws SQLException {
        List<Club> clubs = clubService.getAll();
        clubsList.add("all");
        clubsList.addAll(clubs.stream().map(Club::getNomC).collect(Collectors.toList()));
        clubFilterComboBox.setItems(clubsList);
        clubFilterComboBox.getSelectionModel().selectFirst();
    }
    
    private void loadComments() throws SQLException {
        List<Commentaire> comments = List.of();
        
        if ("all".equals(selectedClub)) {
            comments = commentaireService.getAllComments();
        }
        
        commentsList.clear();
        
        if (comments.isEmpty()) {
            commentsTable.setVisible(false);
            noCommentsContainer.setVisible(true);
        } else {
            commentsTable.setVisible(true);
            noCommentsContainer.setVisible(false);
            
            // Pagination
            int fromIndex = (currentPage - 1) * PAGE_SIZE;
            int toIndex = Math.min(fromIndex + PAGE_SIZE, comments.size());
            
            if (fromIndex <= toIndex) {
                commentsList.addAll(comments.subList(fromIndex, toIndex));
            }
            
            totalPages = (int) Math.ceil((double) comments.size() / PAGE_SIZE);
        }
        
        commentsTable.setItems(commentsList);
    }
    
    private void calculateStats() throws SQLException {
        // Dans un cas réel, ces valeurs viendraient du service ou de la base de données
        List<Commentaire> allComments = commentaireService.getAllComments();
        LocalDate today = LocalDate.now();
        
        totalComments = allComments.size();
        
        todayComments = (int) allComments.stream()
                .filter(c -> c.getDateComment() != null && c.getDateComment().isEqual(today.atStartOfDay()))
                .count();
        
        // Pour cet exemple, on considère les commentaires flaggés comme ceux ayant un contenu avec "flag" ou "report"
        flaggedComments = (int) allComments.stream()
                .filter(c -> c.getContenuComment() != null && 
                        (c.getContenuComment().toLowerCase().contains("flag") || 
                         c.getContenuComment().toLowerCase().contains("report")))
                .count();
        
        // Mise à jour des labels
        totalCommentsLabel.setText(String.valueOf(totalComments));
        todayCommentsLabel.setText(String.valueOf(todayComments));
        flaggedCommentsLabel.setText(String.valueOf(flaggedComments));
    }
    
    private void setupEventHandlers() {
        clubFilterComboBox.setOnAction(event -> {
            selectedClub = clubFilterComboBox.getValue();
            currentPage = 1; // Réinitialiser à la première page
            try {
                loadComments();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            setupPagination();
        });
    }
    
    private void setupPagination() {
        paginationContainer.getChildren().clear();
        
        if (totalPages <= 1) {
            return;
        }
        
        // Bouton précédent
        Button prevButton = new Button("←");
        prevButton.getStyleClass().add(currentPage == 1 ? "pagination-button-disabled" : "pagination-button");
        prevButton.setDisable(currentPage == 1);
        prevButton.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                try {
                    loadComments();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
                setupPagination();
            }
        });
        
        paginationContainer.getChildren().add(prevButton);
        
        // Pages numérotées
        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(startPage + 4, totalPages);
        
        for (int i = startPage; i <= endPage; i++) {
            Button pageButton = new Button(String.valueOf(i));
            pageButton.getStyleClass().addAll("pagination-button", i == currentPage ? "pagination-button-active" : "");
            final int pageNum = i;
            pageButton.setOnAction(e -> {
                currentPage = pageNum;
                try {
                    loadComments();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
                setupPagination();
            });
            paginationContainer.getChildren().add(pageButton);
        }
        
        // Bouton suivant
        Button nextButton = new Button("→");
        nextButton.getStyleClass().add(currentPage == totalPages ? "pagination-button-disabled" : "pagination-button");
        nextButton.setDisable(currentPage == totalPages);
        nextButton.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                try {
                    loadComments();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
                setupPagination();
            }
        });
        
        paginationContainer.getChildren().add(nextButton);
    }
    
    private void deleteComment(Commentaire commentaire) throws SQLException {
        if (AlertUtils.showConfirmation("Confirmation", "Êtes-vous sûr de vouloir supprimer ce commentaire ?")) {
            boolean success = commentaireService.delete(commentaire.getId());
            
            if (success) {
                showToast("Commentaire supprimé avec succès", "success");
                loadComments();
                calculateStats();
                setupPagination();
            } else {
                showToast("Erreur lors de la suppression du commentaire", "error");
            }
        }
    }
    
    private void showToast(String message, String type) {
        Label toastLabel = (Label) ((HBox) toastContainer.getChildren().get(0)).getChildren().get(0);
        HBox toastHBox = (HBox) toastContainer.getChildren().get(0);
        
        if ("error".equals(type)) {
            toastHBox.setStyle("-fx-background-color: #dc3545;");
        } else {
            toastHBox.setStyle("-fx-background-color: #28a745;");
        }
        
        toastLabel.setText(message);
        toastContainer.setVisible(true);
        
        // Cache le toast après 3 secondes
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> toastContainer.setVisible(false));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    // Méthode pour rafraîchir les données
    public void refreshData() throws SQLException {
        loadComments();
        calculateStats();
        setupPagination();
    }
} 
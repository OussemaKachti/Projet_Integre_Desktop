package com.esprit.controllers;

import com.esprit.models.Commentaire;
import com.esprit.models.Club;
import com.esprit.services.CommentaireService;
import com.esprit.services.ClubService;
import com.esprit.utils.AlertUtils;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.stage.Stage;

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
    private final int PAGE_SIZE = 4;
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
        loadClubs();

        // Chargement des commentaires
        loadComments();

        // Calcul des statistiques
        calculateStats();

        // Configuration des événements
        setupEventHandlers();

        // Configuration de la pagination
        setupPagination();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        userColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getUser() != null) {
                return new SimpleStringProperty(
                        cellData.getValue().getUser().getNom() + " " + cellData.getValue().getUser().getPrenom());
            } else {
                return new SimpleStringProperty("Unknown");
            }
        });

        commentColumn.setCellValueFactory(new PropertyValueFactory<>("contenuComment"));

        clubColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getSondage() != null && cellData.getValue().getSondage().getClub() != null) {
                return new SimpleStringProperty(cellData.getValue().getSondage().getClub().getNom());
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
                    private final Button deleteButton = new Button("Delete");
                    
                    {
                        // Configure delete button with a proper styling
                        deleteButton.getStyleClass().addAll("btn", "btn-danger", "delete-button");
                        deleteButton.setTooltip(new Tooltip("Delete this comment"));
                        deleteButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                        
                        // Add hover effect
                        deleteButton.setOnMouseEntered(e -> 
                            deleteButton.setStyle("-fx-background-color: #c82333; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;"));
                        deleteButton.setOnMouseExited(e -> 
                            deleteButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;"));
                            
                        // Configure click handler
                        deleteButton.setOnAction(event -> {
                            Commentaire commentaire = getTableView().getItems().get(getIndex());
                            deleteComment(commentaire);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            // Check if the comment is flagged and highlight
                            Commentaire comment = getTableView().getItems().get(getIndex());
                            if (comment.getContenuComment() != null && 
                                    comment.getContenuComment().contains("⚠️")) {
                                // Visual indicator for flagged comments
                                HBox container = new HBox(5);
                                container.setAlignment(Pos.CENTER);
                                
                                Label flagIndicator = new Label("⚠️");
                                flagIndicator.setTooltip(new Tooltip("This comment was flagged"));
                                flagIndicator.setStyle("-fx-font-size: 18px;");
                                
                                container.getChildren().addAll(flagIndicator, deleteButton);
                                setGraphic(container);
                            } else {
                                setGraphic(deleteButton);
                            }
                        }
                    }
                };
            }
        };
    }

    private void loadClubs() {
        try {
            // Get all clubs from database
            List<Club> clubsList = clubService.getAll();
            
            // Create a new ObservableList with "All Clubs" as first option
            this.clubsList = FXCollections.observableArrayList();
            this.clubsList.add("All Clubs");
            
            // Add the actual club names
            this.clubsList.addAll(clubsList.stream()
                .map(Club::getNom)
                .sorted() // Sort clubs alphabetically
                .collect(Collectors.toList()));
            
            // Set items to the ComboBox
            clubFilterComboBox.setItems(this.clubsList);
            
            // Set custom cell factory for better display
            clubFilterComboBox.setCellFactory(listView -> new ListCell<String>() {
                @Override
                protected void updateItem(String club, boolean empty) {
                    super.updateItem(club, empty);
                    
                    if (empty || club == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        // Create an HBox for the cell content
                        HBox cellBox = new HBox(10);
                        cellBox.setAlignment(Pos.CENTER_LEFT);
                        
                        // Create icon based on whether it's "All Clubs" or a specific club
                        Label icon = new Label();
                        if ("All Clubs".equals(club)) {
                            icon.setText("🌐");
                        } else {
                            icon.setText("🏢");
                        }
                        icon.setStyle("-fx-font-size: 14px;");
                        
                        // Create label for club name
                        Label clubLabel = new Label(club);
                        clubLabel.setStyle("-fx-font-size: 14px;");
                        
                        // Add components to cell
                        cellBox.getChildren().addAll(icon, clubLabel);
                        
                        setGraphic(cellBox);
                        setText(null);
                    }
                }
            });
            
            // Set custom button cell for the selected value display
            clubFilterComboBox.setButtonCell(new ListCell<String>() {
                @Override
                protected void updateItem(String club, boolean empty) {
                    super.updateItem(club, empty);
                    
                    if (empty || club == null) {
                        setText("All Clubs");
                        setGraphic(null);
                    } else {
                        HBox cellBox = new HBox(10);
                        cellBox.setAlignment(Pos.CENTER_LEFT);
                        
                        Label icon = new Label();
                        if ("All Clubs".equals(club)) {
                            icon.setText("🌐");
                        } else {
                            icon.setText("🏢");
                        }
                        icon.setStyle("-fx-font-size: 14px;");
                        
                        Label clubLabel = new Label(club);
                        clubLabel.setStyle("-fx-font-size: 14px;");
                        
                        cellBox.getChildren().addAll(icon, clubLabel);
                        
                        setGraphic(cellBox);
                        setText(null);
                    }
                }
            });
            
            // Select first item (All Clubs)
            clubFilterComboBox.getSelectionModel().selectFirst();
            selectedClub = "all"; // Default value
            
            // Add style class for custom styling
            clubFilterComboBox.getStyleClass().add("club-filter-combo");
            
        } catch (SQLException e) {
            AlertUtils.showError("Error", "Unable to load clubs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadComments() {
        try {
            // Show a loading indicator (can be implemented later)
            commentsTable.setPlaceholder(new Label("Loading comments..."));
            
            List<Commentaire> comments;
            
            // Get comments based on selected club
            if ("All Clubs".equals(selectedClub) || selectedClub.equals("all")) {
                // Get all comments
                ObservableList<Commentaire> allComments = commentaireService.getAllComments();
                comments = new ArrayList<>(allComments);
                System.out.println("Loaded " + comments.size() + " comments (all clubs)");
            } else {
                // Filter by club name
                ObservableList<Commentaire> allComments = commentaireService.getAllComments();
                comments = allComments.stream()
                    .filter(comment -> comment.getSondage() != null && 
                             comment.getSondage().getClub() != null && 
                             comment.getSondage().getClub().getNom().equals(selectedClub))
                    .collect(Collectors.toList());
                System.out.println("Loaded " + comments.size() + " comments for club: " + selectedClub);
            }

            commentsList.clear();

            if (comments.isEmpty()) {
                commentsTable.setVisible(true);
                commentsTable.setPlaceholder(new Label("No comments found for the selected filter"));
                noCommentsContainer.setVisible(true);
                paginationContainer.setVisible(false);
            } else {
                commentsTable.setVisible(true);
                noCommentsContainer.setVisible(false);
                paginationContainer.setVisible(true);

                // Pagination
                int fromIndex = (currentPage - 1) * PAGE_SIZE;
                int toIndex = Math.min(fromIndex + PAGE_SIZE, comments.size());

                if (fromIndex <= toIndex) {
                    commentsList.addAll(comments.subList(fromIndex, toIndex));
                }

                totalPages = (int) Math.ceil((double) comments.size() / PAGE_SIZE);
            }

            commentsTable.setItems(commentsList);
            
            // Update pagination
            setupPagination();
            
        } catch (SQLException e) {
            commentsTable.setPlaceholder(new Label("Error loading comments: " + e.getMessage()));
            AlertUtils.showError("Error", "Unable to load comments: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void calculateStats() {
        try {
            // Get statistics from service
            totalComments = commentaireService.getTotalComments();
            todayComments = commentaireService.getTodayComments();
            flaggedComments = commentaireService.getFlaggedComments();

            // Update the labels with formatted numbers
            totalCommentsLabel.setText(String.valueOf(totalComments));
            todayCommentsLabel.setText(String.valueOf(todayComments));
            flaggedCommentsLabel.setText(String.valueOf(flaggedComments));
            
            // Apply CSS classes based on values
            if (flaggedComments > 0) {
                flaggedCommentsLabel.getStyleClass().add("warning-count");
            } else {
                flaggedCommentsLabel.getStyleClass().remove("warning-count");
            }
            
            System.out.println("Statistics updated: " + totalComments + " total, " + 
                    todayComments + " today, " + flaggedComments + " flagged");
            
        } catch (SQLException e) {
            System.err.println("Error calculating statistics: " + e.getMessage());
            e.printStackTrace();
            // Don't show error to user for non-critical stats failure
        }
    }

    private void setupEventHandlers() {
        clubFilterComboBox.setOnAction(event -> {
            String selected = clubFilterComboBox.getValue();
            if (selected == null) {
                selected = "All Clubs";
            }
            
            // Show a short animation or visual feedback when filter changes
            clubFilterComboBox.setDisable(true); // Temporarily disable to show processing
            
            // Convert "All Clubs" to internal value "all"
            selectedClub = "All Clubs".equals(selected) ? "all" : selected;
            
            // Reset to page 1 when changing filter
            currentPage = 1;
            
            // Load filtered comments
            loadComments();
            
            // Re-enable after short delay to show processing
            new Thread(() -> {
                try {
                    Thread.sleep(300); // Short delay for visual feedback
                    Platform.runLater(() -> clubFilterComboBox.setDisable(false));
                } catch (InterruptedException e) {
                    Platform.runLater(() -> clubFilterComboBox.setDisable(false));
                    e.printStackTrace();
                }
            }).start();
            
            // Show toast with filter info
            showToast("Filtered by " + (selectedClub.equals("all") ? "all clubs" : "club: " + selectedClub), "info");
        });
    }

    private void setupPagination() {
        paginationContainer.getChildren().clear();

        if (totalPages <= 1) {
            // Hide pagination container if there's only one page
            paginationContainer.setVisible(false);
            paginationContainer.setManaged(false);
            return;
        }
        
        // Show pagination if more than one page
        paginationContainer.setVisible(true);
        paginationContainer.setManaged(true);

        // Previous button
        Button prevButton = new Button("←");
        prevButton.getStyleClass().add(currentPage == 1 ? "pagination-button-disabled" : "pagination-button");
        prevButton.setDisable(currentPage == 1);
        prevButton.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                loadComments();
            }
        });

        paginationContainer.getChildren().add(prevButton);

        // Pages numbered buttons
        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(startPage + 4, totalPages);

        for (int i = startPage; i <= endPage; i++) {
            Button pageButton = new Button(String.valueOf(i));
            pageButton.getStyleClass().addAll("pagination-button");
            
            // Add active class for current page and style
            if (i == currentPage) {
                pageButton.getStyleClass().add("pagination-button-active");
                pageButton.setStyle("-fx-font-weight: bold;");
            }
            
            final int pageNum = i;
            pageButton.setOnAction(e -> {
                currentPage = pageNum;
                loadComments();
            });
            paginationContainer.getChildren().add(pageButton);
        }

        // Next button
        Button nextButton = new Button("→");
        nextButton.getStyleClass().add(currentPage == totalPages ? "pagination-button-disabled" : "pagination-button");
        nextButton.setDisable(currentPage == totalPages);
        nextButton.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                loadComments();
            }
        });

        paginationContainer.getChildren().add(nextButton);
        
        // Add page count information
        Label pageInfoLabel = new Label(String.format("Page %d of %d", currentPage, totalPages));
        pageInfoLabel.setStyle("-fx-text-fill: #6c757d; -fx-padding: 5 0 0 10;");
        paginationContainer.getChildren().add(pageInfoLabel);
    }

    private void deleteComment(Commentaire commentaire) {
        try {
            // Create a custom confirmation dialog
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Delete Comment");
            confirmDialog.setHeaderText("Are you sure you want to delete this comment?");
            confirmDialog.setContentText("This action cannot be undone.");
            
            // Customize the dialog
            DialogPane dialogPane = confirmDialog.getDialogPane();
            dialogPane.getStylesheets().add(getClass().getResource("/com/esprit/styles/admin-polls-style.css").toExternalForm());
            dialogPane.getStyleClass().add("custom-alert");
            
            // Get the confirm and cancel buttons
            Button confirmButton = (Button) dialogPane.lookupButton(ButtonType.OK);
            Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
            
            if (confirmButton != null) {
                confirmButton.setText("Delete");
                confirmButton.getStyleClass().add("delete-confirm-button");
                confirmButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
            }
            
            if (cancelButton != null) {
                cancelButton.setText("Cancel");
                cancelButton.getStyleClass().add("cancel-button");
            }
            
            // Show dialog and process result
            if (confirmDialog.showAndWait().filter(response -> response == ButtonType.OK).isPresent()) {
                commentaireService.delete(commentaire.getId());
                showToast("Comment deleted successfully", "success");
                loadComments();
                calculateStats();
            }
        } catch (SQLException e) {
            showToast("Error deleting comment: " + e.getMessage(), "error");
            e.printStackTrace();
        } catch (Exception e) {
            showToast("Unexpected error: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    private void showToast(String message, String type) {
        Label toastLabel = (Label) ((HBox) toastContainer.getChildren().get(0)).getChildren().get(0);
        HBox toastHBox = (HBox) toastContainer.getChildren().get(0);

        switch (type) {
            case "error":
                toastHBox.setStyle("-fx-background-color: #dc3545; -fx-background-radius: 4px;");
                break;
            case "info":
                toastHBox.setStyle("-fx-background-color: #007bff; -fx-background-radius: 4px;");
                break;
            default: // success
                toastHBox.setStyle("-fx-background-color: #28a745; -fx-background-radius: 4px;");
                break;
        }

        toastLabel.setText(message);
        toastContainer.setVisible(true);
        
        // Add a fade-in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toastContainer);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Hide toast after 3 seconds with fade out animation
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(500), toastContainer);
                    fadeOut.setFromValue(1);
                    fadeOut.setToValue(0);
                    fadeOut.setOnFinished(e -> toastContainer.setVisible(false));
                    fadeOut.play();
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
                Platform.runLater(() -> toastContainer.setVisible(false));
            }
        }).start();
    }

    // Méthode pour rafraîchir les données
    public void refreshData() {
        loadComments();
        calculateStats();
        setupPagination();
    }
}
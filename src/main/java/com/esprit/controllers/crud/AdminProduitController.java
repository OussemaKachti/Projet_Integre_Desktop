package com.esprit.controllers.crud;

import com.esprit.models.Club;
import com.esprit.models.Produit;
import com.esprit.services.ClubService;
import com.esprit.services.ProduitService;
import com.esprit.utils.AlertUtils;
import com.esprit.ProduitApp;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminProduitController implements Initializable {

    @FXML private TableView<Produit> tableView;
    @FXML private TableColumn<Produit, Integer> colId;
    @FXML private TableColumn<Produit, String> colNom;
    @FXML private TableColumn<Produit, Float> colPrix;
    @FXML private TableColumn<Produit, String> colQuantity;
    @FXML private TableColumn<Produit, String> colDescription;
    @FXML private TableColumn<Produit, Club> colClub;
    @FXML private TableColumn<Produit, String> colCreatedAt;
    @FXML private TableColumn<Produit, Void> colActions;
    
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> filterClubComboBox;
    @FXML private Pagination pagination;
    
    // Form fields
    @FXML private TextField txtNom;
    @FXML private TextField txtPrix;
    @FXML private TextField txtQuantity;
    @FXML private TextField txtImage;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<Club> comboClub;
    @FXML private Button btnSave;
    @FXML private Button btnUpdate;
    @FXML private Button btnCancel;

    private final ProduitService produitService;
    private final ClubService clubService;
    private ObservableList<Produit> produitList;
    private FilteredList<Produit> filteredList;
    
    private static final int ITEMS_PER_PAGE = 10;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE);

    public AdminProduitController() {
        this.produitService = ProduitService.getInstance();
        this.clubService = ClubService.getInstance();
        this.produitList = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        setupFilters();
        loadAllProduits();
        
        // Add table styling
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        try {
            URL styleResource = getClass().getResource("/com/esprit/styles/admin-style.css");
            if (styleResource != null) {
                tableView.getStylesheets().add(styleResource.toExternalForm());
            }
        } catch (Exception e) {
            System.err.println("Could not load stylesheet: " + e.getMessage());
        }
    }

    /**
     * Navigate to the product catalog view
     */
    @FXML
    public void goToCatalog() {
        try {
            if (ProduitApp.getPrimaryStage() != null) {
                ProduitApp.navigateTo("/com/esprit/views/produit/ProduitView.fxml");
            } else {
                AlertUtils.showError("Error", "Navigation failed", "Unable to access the main application window.");
            }
        } catch (Exception e) {
            AlertUtils.showError("Error", "Navigation failed", e.getMessage());
        }
    }
    
    /**
     * Opens a file chooser dialog to select an image file
     */
    @FXML
    public void browseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select an Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        
        File selectedFile = fileChooser.showOpenDialog(txtImage.getScene().getWindow());
        if (selectedFile != null) {
            txtImage.setText(selectedFile.getAbsolutePath());
        }
    }
    
    /**
     * Save a new product
     */
    @FXML
    public void saveProduit() {
        // This is a placeholder to make the FXML happy
        // The actual implementation would be added later
        AlertUtils.showInfo("Info", "Save Product", "This functionality will be implemented soon.");
    }
    
    /**
     * Update an existing product
     */
    @FXML
    public void updateProduit() {
        // This is a placeholder to make the FXML happy
        // The actual implementation would be added later
        AlertUtils.showInfo("Info", "Update Product", "This functionality will be implemented soon.");
    }
    
    /**
     * Reset the form fields
     */
    @FXML
    public void resetForm() {
        // This is a placeholder to make the FXML happy
        // The actual implementation would be added later
        if (txtNom != null) txtNom.clear();
        if (txtPrix != null) txtPrix.clear();
        if (txtQuantity != null) txtQuantity.clear();
        if (txtDescription != null) txtDescription.clear();
        if (txtImage != null) txtImage.clear();
        if (comboClub != null) comboClub.setValue(null);
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nomProd"));
        
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colPrix.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Float price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(price));
                }
            }
        });
        
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        
        colDescription.setCellValueFactory(new PropertyValueFactory<>("descProd"));
        colDescription.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String description, boolean empty) {
                super.updateItem(description, empty);
                if (empty || description == null) {
                    setText(null);
                } else {
                    setText(description.length() > 50 ? description.substring(0, 47) + "..." : description);
                    setTooltip(new Tooltip(description));
                }
            }
        });
        
        colClub.setCellValueFactory(new PropertyValueFactory<>("club"));
        colClub.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Club club, boolean empty) {
                super.updateItem(club, empty);
                if (empty || club == null) {
                    setText(null);
                } else {
                    setText(club.getNom());
                }
            }
        });
        
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colCreatedAt.setCellFactory(tc -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            @Override
            protected void updateItem(String date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(formatter.format(java.time.LocalDateTime.parse(date)));
                }
            }
        });
        
        setupActionsColumn();
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(tc -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete");
            private final HBox container = new HBox(5);

            {
                deleteButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-background-radius: 5;");
                container.setAlignment(Pos.CENTER);
                container.getChildren().add(deleteButton);

                deleteButton.setOnAction(event -> {
                    Produit produit = getTableView().getItems().get(getIndex());
                    handleDelete(produit);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void setupFilters() {
        filteredList = new FilteredList<>(produitList);
        
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredList.setPredicate(produit -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    String lowerCaseFilter = newValue.toLowerCase();
                    return produit.getNomProd().toLowerCase().contains(lowerCaseFilter) ||
                           produit.getDescProd().toLowerCase().contains(lowerCaseFilter) ||
                           (produit.getClub() != null && produit.getClub().getNom().toLowerCase().contains(lowerCaseFilter));
                });
                updatePagination();
            });
        }

        try {
            List<Club> clubs = clubService.getAll();
            ObservableList<String> clubNames = FXCollections.observableArrayList();
            clubNames.add("All Clubs");
            clubs.forEach(club -> clubNames.add(club.getNom()));
            
            if (filterClubComboBox != null) {
                filterClubComboBox.setItems(clubNames);
                filterClubComboBox.setValue("All Clubs");
                
                filterClubComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                    filteredList.setPredicate(produit -> {
                        if (newValue == null || newValue.equals("All Clubs")) {
                            return true;
                        }
                        return produit.getClub() != null && produit.getClub().getNom().equals(newValue);
                    });
                    updatePagination();
                });
            }
        } catch (SQLException e) {
            AlertUtils.showError("Error", "Error loading clubs", e.getMessage());
        }
    }

    private void loadAllProduits() {
        try {
            List<Produit> produits = produitService.getAll();
            produitList.clear();
            produitList.addAll(produits);
            updatePagination();
        } catch (SQLException e) {
            AlertUtils.showError("Error", "Error loading products", e.getMessage());
        }
    }

    private void updatePagination() {
        if (pagination != null) {
            int pageCount = (filteredList.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
            pagination.setPageCount(Math.max(1, pageCount));
            pagination.setCurrentPageIndex(0);
            pagination.setPageFactory(this::createPage);
            
            // Show/hide empty state
            boolean isEmpty = filteredList.isEmpty();
            tableView.setVisible(!isEmpty);
            if (isEmpty) {
                tableView.setPlaceholder(new Label("No products found"));
            }
        }
    }

    private VBox createPage(int pageIndex) {
        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, filteredList.size());
        
        tableView.setItems(FXCollections.observableArrayList(
            filteredList.subList(fromIndex, toIndex)
        ));
        
        VBox box = new VBox();
        box.getChildren().add(tableView);
        return box;
    }

    private void handleDelete(Produit produit) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Product");
        alert.setHeaderText("Are you sure you want to delete this product?");
        alert.setContentText("This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    produitService.deleteProduit(produit.getId());
                    loadAllProduits();
                } catch (SQLException e) {
                    AlertUtils.showError("Error", "Error deleting product", e.getMessage());
                }
            }
        });
    }
}
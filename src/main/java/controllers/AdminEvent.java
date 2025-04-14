package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import models.Evenement;
import services.ServiceEvent;
import utils.DataSource;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminEvent implements Initializable {

    @FXML
    private TableView<Evenement> eventTable;

    @FXML
    private TableColumn<Evenement, Integer> idColumn;

    @FXML
    private TableColumn<Evenement, String> nameColumn;

    @FXML
    private TableColumn<Evenement, String> typeColumn;

    @FXML
    private TableColumn<Evenement, String> locationColumn;

    @FXML
    private TableColumn<Evenement, String> categoryColumn;

    @FXML
    private TableColumn<Evenement, String> clubColumn;

    @FXML
    private TableColumn<Evenement, Date> startDateColumn;

    @FXML
    private TableColumn<Evenement, Date> endDateColumn;

    @FXML
    private TableColumn<Evenement, Void> actionsColumn;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> categoryFilter;

    @FXML
    private ComboBox<String> clubFilter;

    @FXML
    private ComboBox<String> statusFilter;

    @FXML
    private Button addEventButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button backButton;

    @FXML
    private Label totalEventsLabel;

    private ServiceEvent serviceEvent;
    private ObservableList<Evenement> eventsList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        serviceEvent = new ServiceEvent();

        // Initialize filters
        initializeFilters();

        // Configure table columns
        configureTableColumns();

        // Load events
        loadEvents();

        // Add search functionality
        addSearchListener();
    }

    private void initializeFilters() {
        // Category filter
        categoryFilter.getItems().add("All Categories");
        categoryFilter.getItems().addAll(serviceEvent.getAllCategoriesNames());
        categoryFilter.setValue("All Categories");
        categoryFilter.setOnAction(e -> applyFilters());

        // Club filter
        clubFilter.getItems().add("All Clubs");
        clubFilter.getItems().addAll(serviceEvent.getAllClubsNames());
        clubFilter.setValue("All Clubs");
        clubFilter.setOnAction(e -> applyFilters());

        // Status filter
        statusFilter.getItems().addAll("All", "Open", "Closed");
        statusFilter.setValue("All");
        statusFilter.setOnAction(e -> applyFilters());
    }

    private void configureTableColumns() {
        // Set cell value factories
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("nom_event"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("lieux"));

        // Custom cell factories for displaying category and club names
        categoryColumn.setCellValueFactory(cellData -> {
            int categoryId = cellData.getValue().getCategorie_id();
            String categoryName = serviceEvent.getCategoryNameById(categoryId);
            return javafx.beans.binding.Bindings.createStringBinding(() -> categoryName);
        });

        clubColumn.setCellValueFactory(cellData -> {
            int clubId = cellData.getValue().getClub_id();
            String clubName = serviceEvent.getClubNameById(clubId);
            return javafx.beans.binding.Bindings.createStringBinding(() -> clubName);
        });

        // Date columns
        startDateColumn.setCellValueFactory(new PropertyValueFactory<>("start_date"));
        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("end_date"));

        // Format dates
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");

        startDateColumn.setCellFactory(column -> new TableCell<Evenement, Date>() {
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(dateFormat.format(item));
                }
            }
        });

        endDateColumn.setCellFactory(column -> new TableCell<Evenement, Date>() {
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(dateFormat.format(item));
                }
            }
        });

        // Configure actions column
        configureActionsColumn();
    }

    private void configureActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final Button viewButton = new Button("View");
            private final HBox buttonsBox = new HBox(5);

            {
                // Styling the buttons
                editButton.getStyleClass().add("edit-button");
                deleteButton.getStyleClass().add("delete-button");
                viewButton.getStyleClass().add("view-button");

                buttonsBox.getChildren().addAll(viewButton, editButton, deleteButton);

                // Action handlers
                editButton.setOnAction(event -> {
                    Evenement evenement = getTableView().getItems().get(getIndex());
                    handleEditEvent(evenement);
                });

                deleteButton.setOnAction(event -> {
                    Evenement evenement = getTableView().getItems().get(getIndex());
                    handleDeleteEvent(evenement);
                });

                viewButton.setOnAction(event -> {
                    Evenement evenement = getTableView().getItems().get(getIndex());
                    handleViewEvent(evenement);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttonsBox);
                }
            }
        });
    }

    private void loadEvents() {
        try {
            Connection conn = DataSource.getInstance().getCnx();
            String query = "SELECT * FROM evenement ORDER BY start_date DESC";

            PreparedStatement pst = conn.prepareStatement(query);
            ResultSet rs = pst.executeQuery();

            eventsList.clear();

            while (rs.next()) {
                Evenement event = new Evenement();
                event.setId(rs.getInt("id"));
                event.setNom_event(rs.getString("nom_event"));
                event.setType(rs.getString("type"));
                event.setDesc_event(rs.getString("desc_event"));
                event.setImage_description(rs.getString("image_description"));
                event.setLieux(rs.getString("lieux"));
                event.setClub_id(rs.getInt("club_id"));
                event.setCategorie_id(rs.getInt("categorie_id"));
                event.setStart_date(rs.getDate("start_date"));
                event.setEnd_date(rs.getDate("end_date"));

                eventsList.add(event);
            }

            eventTable.setItems(eventsList);
            updateTotalEventsLabel();

        } catch (SQLException ex) {
            System.err.println("Error loading events: " + ex.getMessage());
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load events", ex.getMessage());
        }
    }

    private void addSearchListener() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String selectedCategory = categoryFilter.getValue();
        String selectedClub = clubFilter.getValue();
        String selectedStatus = statusFilter.getValue();

        ObservableList<Evenement> filteredEvents = FXCollections.observableArrayList();

        for (Evenement event : eventsList) {
            boolean matchesSearch = event.getNom_event().toLowerCase().contains(searchText) ||
                    event.getDesc_event().toLowerCase().contains(searchText) ||
                    event.getLieux().toLowerCase().contains(searchText);

            boolean matchesCategory = "All Categories".equals(selectedCategory) ||
                    serviceEvent.getCategoryNameById(event.getCategorie_id()).equals(selectedCategory);

            boolean matchesClub = "All Clubs".equals(selectedClub) ||
                    serviceEvent.getClubNameById(event.getClub_id()).equals(selectedClub);

            boolean matchesStatus = "All".equals(selectedStatus) ||
                    (event.getType() != null && event.getType().equals(selectedStatus));

            if (matchesSearch && matchesCategory && matchesClub && matchesStatus) {
                filteredEvents.add(event);
            }
        }

        eventTable.setItems(filteredEvents);
        updateTotalEventsLabel();
    }

    private void updateTotalEventsLabel() {
        int displayedCount = eventTable.getItems().size();
        int totalCount = eventsList.size();

        if (displayedCount == totalCount) {
            totalEventsLabel.setText("Total Events: " + totalCount);
        } else {
            totalEventsLabel.setText("Displaying " + displayedCount + " of " + totalCount + " events");
        }
    }

    @FXML
    private void handleAddEvent() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterEvent.fxml"));
            Parent root = loader.load();
            addEventButton.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to open Add Event page", e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        loadEvents();
        searchField.clear();
        categoryFilter.setValue("All Categories");
        clubFilter.setValue("All Clubs");
        statusFilter.setValue("All");
    }

    private void handleEditEvent(Evenement event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierEvent.fxml"));
            Parent root = loader.load();

            // Assuming you have a ModifierEvent controller that accepts an event to edit
            ModifierEvent modifierController = loader.getController();
            modifierController.setEventId(event.getId());

            // Use a control that is in scope to access the scene
            addEventButton.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to open Edit Event page", e.getMessage());
        }
    }

    private void handleDeleteEvent(Evenement event) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText("Delete Event");
        confirmDialog.setContentText("Are you sure you want to delete the event: " + event.getNom_event() + "?");

        Optional<ButtonType> result = confirmDialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Connection conn = DataSource.getInstance().getCnx();
                String query = "DELETE FROM evenement WHERE id = ?";

                PreparedStatement pst = conn.prepareStatement(query);
                pst.setInt(1, event.getId());

                int rowsAffected = pst.executeUpdate();

                if (rowsAffected > 0) {
                    eventsList.remove(event);
                    applyFilters(); // Reapply filters to update table
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Event Deleted",
                            "The event has been successfully deleted.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Delete Failed", "Failed to delete event",
                            "No rows were affected. The event may not exist anymore.");
                }
            } catch (SQLException ex) {
                System.err.println("Error deleting event: " + ex.getMessage());
                showAlert(Alert.AlertType.ERROR, "Delete Error", "Failed to delete event", ex.getMessage());
            }
        }
    }

    private void handleViewEvent(Evenement event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DetailsEvent.fxml"));
            Parent root = loader.load();

            // Set the event data in the details controller
            DetailsEvent detailsController = loader.getController();
            detailsController.setEventData(event);

            // Use a control that is in scope to access the scene
            addEventButton.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to open Event Details page", e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Dashboard.fxml"));
            backButton.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to return to dashboard", e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
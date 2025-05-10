package com.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import com.esprit.MainApp;
import com.esprit.models.Evenement;
import com.esprit.services.ServiceEvent;
import com.esprit.utils.DataSource;
import com.esprit.utils.SessionManager;

import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.Node;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminEvent implements Initializable {
    @FXML
    private BorderPane contentArea;
    @FXML
    private ListView<Evenement> eventListView;

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
    private Label totalEventsLabel;

    @FXML
    private Label openEventsLabel;

    @FXML
    private Label upcomingEventsLabel;

    @FXML
    private Label dateLabel;

    @FXML
    private Label paginationInfoLabel;

    @FXML
    private Label adminNameLabel;

    private ServiceEvent serviceEvent;
    private ObservableList<Evenement> eventsList = FXCollections.observableArrayList();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");

    @FXML
    private VBox surveySubMenu;

    @FXML
    private VBox eventsSubMenu;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        serviceEvent = new ServiceEvent();

        // Set current date
        SimpleDateFormat fullDateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy");
        dateLabel.setText("Today: " + fullDateFormat.format(new Date()));

        // Set admin name if available
        try {
            var currentUser = com.esprit.utils.SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                adminNameLabel.setText(currentUser.getFirstName() + " " + currentUser.getLastName());
            }
        } catch (Exception e) {
            System.err.println("Error loading user data: " + e.getMessage());
        }

        // Initialize filters
        initializeFilters();

        // Configure list view with custom cell factory
        configureListView();

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

    private void configureListView() {
        eventListView.setCellFactory(param -> new ListCell<Evenement>() {
            private final HBox content = new HBox(15);
            private final VBox eventInfo = new VBox(5);
            private final Label nameLabel = new Label();
            private final Label detailsLabel = new Label();
            private final Label dateLabel = new Label();
            private final Label statusLabel = new Label();
            private final HBox buttonsBox = new HBox(5);
            private final Button viewButton = new Button("View");
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final Region spacer = new Region();

            {
                // Setting up cell styling
                content.setStyle(
                        "-fx-padding: 10px; -fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 5px;");
                content.setPrefWidth(10);
content.maxWidthProperty().bind(eventListView.widthProperty().subtract(30));
                // Configure labels
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
                detailsLabel.setStyle("-fx-font-size: 14px;");
                dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555555;");
                statusLabel.setStyle("-fx-font-size: 14px; -fx-padding: 3px 8px; -fx-background-radius: 3px;");

                // Set up buttons with consistent styling
                viewButton.getStyleClass().add("view-button");
                viewButton
                        .setStyle("-fx-background-color: #e6f7e6; -fx-text-fill: #2e8b57; -fx-background-radius: 3px;");

                editButton.getStyleClass().add("edit-button");
                editButton
                        .setStyle("-fx-background-color: #e6e6f7; -fx-text-fill: #4169e1; -fx-background-radius: 3px;");

                deleteButton.getStyleClass().add("delete-button");
                deleteButton
                        .setStyle("-fx-background-color: #f7e6e6; -fx-text-fill: #b22222; -fx-background-radius: 3px;");

                buttonsBox.getChildren().addAll(viewButton, editButton, deleteButton);

                // Configure action handlers
                viewButton.setOnAction(event -> {
                    Evenement evenement = getItem();
                    if (evenement != null) {
                        handleViewEvent(evenement);
                    }
                });

                editButton.setOnAction(event -> {
                    Evenement evenement = getItem();
                    if (evenement != null) {
                        handleEditEvent(evenement);
                    }
                });

                deleteButton.setOnAction(event -> {
                    Evenement evenement = getItem();
                    if (evenement != null) {
                        handleDeleteEvent(evenement);
                    }
                });

                // Build layout
                eventInfo.getChildren().addAll(nameLabel, detailsLabel, dateLabel);
                HBox.setHgrow(spacer, Priority.ALWAYS);

                VBox rightSide = new VBox(10);
                rightSide.getChildren().addAll(statusLabel, buttonsBox);
                rightSide.setStyle("-fx-alignment: center-right;");

                content.getChildren().addAll(eventInfo, spacer, rightSide);
            }

            @Override
            protected void updateItem(Evenement event, boolean empty) {
                super.updateItem(event, empty);

                if (empty || event == null) {
                    setGraphic(null);
                } else {
                    // Set event data
                    nameLabel.setText(event.getNom_event());

                    String categoryName = serviceEvent.getCategoryNameById(event.getCategorie_id());
                    String clubName = serviceEvent.getClubNameById(event.getClub_id());
                    detailsLabel.setText(categoryName + " • " + clubName + " • " + event.getLieux());

                    // Format dates
                    dateLabel.setText(dateFormat.format(event.getStart_date()) + " - " +
                            dateFormat.format(event.getEnd_date()));

                    // Set status style
                    statusLabel.setText(event.getType());
                    if ("Open".equals(event.getType())) {
                        statusLabel.setStyle(
                                "-fx-background-color: #e6f7e6; -fx-text-fill: #2e8b57; -fx-padding: 3px 8px; -fx-background-radius: 3px;");
                    } else {
                        statusLabel.setStyle(
                                "-fx-background-color: #f7e6e6; -fx-text-fill: #b22222; -fx-padding: 3px 8px; -fx-background-radius: 3px;");
                    }

                    setGraphic(content);
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

            eventListView.setItems(eventsList);
            updateStatistics();

        } catch (SQLException ex) {
            System.err.println("Error loading events: " + ex.getMessage());
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load events", ex.getMessage());
        }
    }

    private void updateStatistics() {
        int totalCount = eventsList.size();
        int openCount = 0;
        int upcomingThisMonth = 0;

        // Get current date and end of month
        Calendar cal = Calendar.getInstance();
        Date currentDate = cal.getTime();

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date endOfMonth = cal.getTime();

        for (Evenement event : eventsList) {
            // Count open events
            if ("Open".equals(event.getType())) {
                openCount++;
            }

            // Count upcoming events this month (start date between now and end of month)
            Date startDate = event.getStart_date();
            if (startDate.after(currentDate) && startDate.before(endOfMonth)) {
                upcomingThisMonth++;
            }
        }

        // Update labels
        totalEventsLabel.setText(String.valueOf(totalCount));
        openEventsLabel.setText(String.valueOf(openCount));
        upcomingEventsLabel.setText(String.valueOf(upcomingThisMonth));

        // Update pagination info
        int displayedCount = eventListView.getItems().size();
        if (displayedCount == totalCount) {
            paginationInfoLabel.setText("Showing all events");
        } else {
            paginationInfoLabel.setText("Showing " + displayedCount + " of " + totalCount + " events");
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

        eventListView.setItems(filteredEvents);

        // Update pagination info
        int displayedCount = filteredEvents.size();
        int totalCount = eventsList.size();

        if (displayedCount == totalCount) {
            paginationInfoLabel.setText("Showing all events");
        } else {
            paginationInfoLabel.setText("Showing " + displayedCount + " of " + totalCount + " events");
        }
    }

    @FXML
    private void handleAddEvent() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/AjouterEvent.fxml"));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/ModifierEvent.fxml"));
            Parent root = loader.load();

            // Assuming you have a ModifierEvent controller that accepts an event to edit
            ModifierEvent modifierController = loader.getController();
            modifierController.setEventId(event.getId());

            // Create a new stage for the edit view
            Stage editStage = new Stage();
            editStage.setTitle("Modifier l'événement");
            editStage.setScene(new Scene(root));

            // Add event handler to refresh events list when edit window is closed
            editStage.setOnHidden(e -> {
                loadEvents(); // Refresh the events list when edit window is closed
            });

            // Show the edit window
            editStage.show();

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
                    applyFilters(); // Reapply filters to update list
                    updateStatistics(); // Update statistics after deletion
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/DetailsEvent.fxml"));
            Parent root = loader.load();

            // Set the event data in the details controller
            DetailsEvent detailsController = loader.getController();
            detailsController.setEventData(event);

            // Create a new stage for details view
            Stage detailsStage = new Stage();
            detailsStage.setTitle("Détails de l'événement");
            detailsStage.setScene(new Scene(root));
            detailsStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to open Event Details page", e.getMessage());
        }
    }

    // Sidebar Navigation Methods
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
    private void showClubManagement(ActionEvent actionEvent) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/esprit/views/ClubView.fxml"));
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
    private void showEventManagement(ActionEvent actionEvent) throws IOException {
        // Instead of navigating directly, toggle the submenu
        boolean isVisible = eventsSubMenu.isVisible();
        eventsSubMenu.setVisible(!isVisible);
        eventsSubMenu.setManaged(!isVisible);
    }

    @FXML
    private void navigateToEventList(ActionEvent actionEvent) throws IOException {
        // Navigate to main events page
        Parent root = FXMLLoader.load(getClass().getResource("/com/esprit/views/AdminEvent.fxml"));
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
    private void navigateToCategoryManagement(ActionEvent actionEvent) throws IOException {
        // Navigate to category management page
        Parent root = FXMLLoader.load(getClass().getResource("/com/esprit/views/AdminCat.fxml"));
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
    private void showProductOrders(ActionEvent actionEvent) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/esprit/views/produit/AdminProduitView.fxml"));
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
    private void showCompetition(ActionEvent actionEvent) throws IOException {
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
    private void showSurvey(ActionEvent actionEvent) {
        boolean isVisible = surveySubMenu.isVisible();
        surveySubMenu.setVisible(!isVisible);
        surveySubMenu.setManaged(!isVisible);
    }

    @FXML
    private void navigateToPollsManagement(ActionEvent actionEvent) throws IOException {
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
    private void navigateToCommentsManagement(ActionEvent actionEvent) throws IOException {
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
    public void navigateToProfile() {
        try {
            // Load the profile view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/admin_profile.fxml"));
            Parent root = loader.load();

            // Create a completely new stage
            Stage newStage = new Stage();

            // Create scene with appropriate initial size
            Scene scene = new Scene(root, 1200, 800); // Set initial size large enough

            // Apply the stylesheet
            scene.getStylesheets().add(getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm());

            // Configure the new stage
            newStage.setTitle("Admin Profile - UNICLUBS");
            newStage.setScene(scene);
            newStage.setMaximized(true); // Set maximized before showing

            // Close the current stage
            Stage currentStage = (Stage) contentArea.getScene().getWindow();
            currentStage.close();

            // Show the new stage
            newStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Navigation Error", "Failed to navigate to admin profile", e.getMessage());
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // Clear session
        SessionManager.getInstance().clearSession();

        // Navigate to login
        try {
            navigateToLogin();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert2("Error", "Logout Error", "Failed to navigate to login page");
        }
    }

    private void navigateToLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/login.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) (contentArea != null ? contentArea.getScene().getWindow()
                : (adminNameLabel != null ? adminNameLabel.getScene().getWindow() : null));

        if (stage != null) {
            // Use the utility method for consistent setup
            MainApp.setupStage(stage, root, "Login - UNICLUBS", true);

            stage.show();
        } else {
            // If we can't get the stage from the UI elements, create a new one
            stage = new Stage();

            // Use the utility method for consistent setup
            MainApp.setupStage(stage, root, "Login - UNICLUBS", true);

            stage.show();

            // Close any existing windows
            if (contentArea != null && contentArea.getScene() != null &&
                    contentArea.getScene().getWindow() != null) {
                ((Stage) contentArea.getScene().getWindow()).close();
            }
        }
    }

    private void showAlert2(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
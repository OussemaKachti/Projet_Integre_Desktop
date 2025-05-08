package com.esprit.controllers;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.stage.Stage;
import com.esprit.models.User;
import com.esprit.models.enums.RoleEnum;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import com.esprit.services.AuthService;
import com.esprit.services.UserService;
import com.esprit.utils.ProfanityLogManager;
import com.esprit.MainApp;
import com.esprit.utils.ProfanityLogManager;
import com.esprit.utils.SessionManager;
import javafx.application.Platform;

import static com.esprit.utils.AlertUtilsSirine.showError;

public class AdminDashboardController {

    @FXML
    private Label adminNameLabel;
    
    @FXML
    private Label contentTitle;
    
    @FXML
    private StackPane contentStackPane;
    
    @FXML
    private BorderPane contentArea;
    
    @FXML
    private Button userManagementButton;
    
    @FXML
    private Button clubManagementButton;
    
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
    
    @FXML
    private Label totalUsersLabel;
    
    @FXML
    private Label activeUsersLabel;
    
    @FXML
    private Label unverifiedUsersLabel;
    
    @FXML
    private TextField searchField;
    
    @FXML
    private ScrollPane userManagementView;
    
    @FXML
    private VBox userDetailsView;
    
    @FXML
    private VBox userDetailsContent;
    
    @FXML
    private Button backToUsersButton;
    
    @FXML
    private Button createUserButton;
    
    @FXML
    private Button userStatsButton;
    
    @FXML
    private TableView<User> usersTable;
    
    @FXML
    private TableColumn<User, Integer> idColumn;
    
    @FXML
    private TableColumn<User, String> firstNameColumn;
    
    @FXML
    private TableColumn<User, String> lastNameColumn;
    
    @FXML
    private TableColumn<User, String> emailColumn;
    
    @FXML
    private TableColumn<User, String> phoneColumn;
    
    @FXML
    private TableColumn<User, RoleEnum> roleColumn;
    
    @FXML
    private TableColumn<User, String> statusColumn;
    
    @FXML
    private TableColumn<User, Void> actionsColumn;
    
    @FXML
    private Button prevPageButton;
    
    @FXML
    private Button nextPageButton;
    
    @FXML
    private Label currentPageLabel;
    
    @FXML
    private Label totalPagesLabel;
    
    private final AuthService authService = new AuthService();
    private UserService userService;
    private User currentUser;
    private ObservableList<User> usersList = FXCollections.observableArrayList();
    private FilteredList<User> filteredUsers;
    
    // Pagination variables
    private static final int ROWS_PER_PAGE = 6;
    private int currentPage = 0;
    private int totalPages = 1;
    
    @FXML
    private void initialize() {
        try {
            // Initialize services
            userService = new UserService();
            
            // Initialize empty filtered list to prevent NullPointerException
            usersList = FXCollections.observableArrayList();
            filteredUsers = new FilteredList<>(usersList, p -> true);
            
            // Load current admin user
            currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                // Redirect to login if not logged in
                try {
                    navigateToLogin();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert("Error", "Session Error", "Could not redirect to login page");
                }
            }
            
            // Check if the user is an admin
            if (!"ADMINISTRATEUR".equals(currentUser.getRole().toString())) {
                try {
                    navigateToLogin();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert("Error", "Access Denied", "You do not have permission to access the admin dashboard");
                }
            }
            
            // Set admin name
            adminNameLabel.setText(currentUser.getFirstName() + " " + currentUser.getLastName());
            
            // Initialize user management view
            setupUserManagementView();
            
            // Hide create user button
            if (createUserButton != null) {
                createUserButton.setVisible(false);
                createUserButton.setManaged(false);
            }
            
            // Load user data - this will update the pagination
            loadUserData();
            
            // By default, show the user management view
            showUserManagement();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Initialization Error", "Failed to initialize the controller");
        }
    }
    
    private void setupUserManagementView() {
        // Set column value factories
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Set specific widths for certain columns
        actionsColumn.setPrefWidth(430.0); // Ensure actions column is wide enough for buttons
        
        // Make the table completely fill its parent container
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Force-disable scrollbars programmatically
        usersTable.setStyle("-fx-hbar-policy: never; -fx-vbar-policy: never;");
        
        // Also add a listener to ensure scrollbars are disabled after skin application
        usersTable.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                // Add classes that might help with scrollbar hiding
                usersTable.getStyleClass().addAll(
                    "hide-horizontal-scrollbar", 
                    "hide-vertical-scrollbar",
                    "no-scroll-table"
                );
                
                // Reapply inline styles as a last resort
                usersTable.setStyle(usersTable.getStyle() + 
                    "; -fx-hbar-policy: never; -fx-vbar-policy: never;");
            }
        });
        
        // Custom cell factories for formatted display
        statusColumn.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    
                    if ("active".equalsIgnoreCase(item)) {
                        setTextFill(Color.GREEN);
                    } else {
                        setTextFill(Color.RED);
                    }
                }
            }
        });
        
        // Setup Actions Column
        setupActionsColumn();
        
        // Setup search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (filteredUsers != null) {
                filteredUsers.setPredicate(user -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    
                    String lowerCaseFilter = newValue.toLowerCase();
                    
                    // Filter by name, email or phone
                    return user.getFirstName().toLowerCase().contains(lowerCaseFilter) ||
                           user.getLastName().toLowerCase().contains(lowerCaseFilter) ||
                           user.getEmail().toLowerCase().contains(lowerCaseFilter) ||
                           (user.getPhone() != null && user.getPhone().toLowerCase().contains(lowerCaseFilter));
                });
                
                // Reset pagination when search changes
                currentPage = 0;
                updatePagination();
            } 
        });
        
        // Setup fixed table display
        usersTable.setFixedCellSize(44.0);
        
        // Don't call updatePagination here - will be called after data is loaded
    }
    
    private void setupActionsColumn() {
        actionsColumn.setCellFactory(column -> {
            return new TableCell<User, Void>() {
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty) {
                        setGraphic(null);
                        return;
                    }
                    
                        try {
                            User user = getTableView().getItems().get(getIndex());
                        if (user == null) {
                            setGraphic(null);
                            return;
                        }
                            
                            // Check if user is an admin (no actions allowed)
                            boolean isAdmin = "ADMINISTRATEUR".equals(user.getRole().toString());
                            if (isAdmin) {
                                setGraphic(null);
                                return;
                            }
                            
                        // Create buttons for each row
                        HBox buttons = new HBox();
                        buttons.setSpacing(5);
                        buttons.setAlignment(Pos.CENTER);
                        
                        // Status toggle button (Activate/Disable)
                        Button statusButton;
                            if ("active".equalsIgnoreCase(user.getStatus())) {
                            statusButton = createButton("Disable", "#FF9800");
                            statusButton.setOnAction(e -> toggleUserStatus(user, false));
                            } else {
                            statusButton = createButton("Activate", "#4CAF50");
                            statusButton.setOnAction(e -> toggleUserStatus(user, true));
                        }
                        
                        // Details button
                        Button detailsButton = createButton("Details", "#2196F3");
                        detailsButton.setOnAction(e -> showUserDetails(user));
                        
                        // Delete button
                        Button deleteButton = createButton("Delete", "#F44336");
                        deleteButton.setOnAction(e -> deleteUser(user));
                        
                        buttons.getChildren().addAll(statusButton, detailsButton, deleteButton);
                        setGraphic(buttons);
                    } catch (Exception e) {
                            setGraphic(null);
                        }
                    }
                
                private Button createButton(String text, String color) {
                    Button button = new Button(text);
                    button.setStyle(
                        "-fx-background-color: " + color + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 10px;" +
                        "-fx-padding: 3 5 3 5;" +  // Smaller padding
                        "-fx-background-radius: 3;" // Rounded corners
                    );
                    button.setMaxWidth(Double.MAX_VALUE);
                    return button;
                }
            };
        });
    }
    
    private void toggleUserStatus(User user, boolean activate) {
        if (user == null) return;
        
        String newStatus = activate ? "active" : "inactive";
        String actionText = activate ? "activate" : "disable";
        
        Alert confirmDialog = new Alert(AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Status Change");
        confirmDialog.setHeaderText("Change User Status");
        confirmDialog.setContentText("Are you sure you want to " + actionText + " user " + 
                                    user.getFirstName() + " " + user.getLastName() + "?");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Store user ID to locate them after reload
                int userId = user.getId();
                
                // Update user status in memory
                user.setStatus(newStatus);
                
                // Update in database
                userService.modifier(user);
                
                // Reload all user data from database
                List<User> updatedUsers = userService.recuperer();
                usersList.clear();
                for (User u : updatedUsers) {
                    if (!"ADMINISTRATEUR".equals(u.getRole().toString())) {
                        usersList.add(u);
                    }
                }
                
                // Create a fresh filtered list
                filteredUsers = new FilteredList<>(usersList, p -> true);
                
                // Apply current search filter if any
                String currentSearch = searchField.getText();
                if (currentSearch != null && !currentSearch.isEmpty()) {
                    String lowerCaseFilter = currentSearch.toLowerCase();
                    filteredUsers.setPredicate(u -> {
                        return u.getFirstName().toLowerCase().contains(lowerCaseFilter) ||
                               u.getLastName().toLowerCase().contains(lowerCaseFilter) ||
                               u.getEmail().toLowerCase().contains(lowerCaseFilter) ||
                               (u.getPhone() != null && u.getPhone().toLowerCase().contains(lowerCaseFilter));
                    });
                }
                
                // Find the page containing the updated user
                User updatedUser = null;
                int userIndex = -1;
                for (int i = 0; i < filteredUsers.size(); i++) {
                    if (filteredUsers.get(i).getId() == userId) {
                        updatedUser = filteredUsers.get(i);
                        userIndex = i;
                        break;
                    }
                }
                
                // Calculate which page contains this user
                if (userIndex >= 0) {
                    currentPage = userIndex / ROWS_PER_PAGE;
                }
                
                // Update pagination and force UI refresh
                updatePagination();
                
                // Explicitly refresh the table
                usersTable.refresh();
                
                // If in details view, update the details content
                if (userDetailsView.isVisible() && updatedUser != null) {
                    showUserDetails(updatedUser);
                }
                
                // Updated success message to match new terminology
                String successMessage = "User " + user.getFirstName() + " " + user.getLastName();
                if (activate) {
                    successMessage += " has been activated.";
                } else {
                    successMessage += " has been disabled.";
                }
                
                showAlert("Success", "Status Updated", successMessage);
                
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Update Failed", 
                         "Failed to update user status: " + e.getMessage());
            }
        }
    }
    
    private void deleteUser(User user) {
        if (user == null) return;
        
        Alert confirmDialog = new Alert(AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Deletion");
        confirmDialog.setHeaderText("Delete User");
        confirmDialog.setContentText("Are you sure you want to delete user " + 
                                    user.getFirstName() + " " + user.getLastName() + "?\n\n" +
                                    "This action cannot be undone.");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.supprimer(user);
                
                // Refresh the table
                loadUserData();
                
                showAlert("Success", "User Deleted", 
                         "User " + user.getFirstName() + " " + user.getLastName() + 
                         " has been deleted successfully.");
                
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Deletion Failed", 
                         "Failed to delete user: " + e.getMessage());
            }
        }
    }
    
    private void showUserDetails(User user) {
        if (user == null) return;
        
        // Update content title
        contentTitle.setText("User Details: " + user.getFirstName() + " " + user.getLastName());
        
        // Clear previous content
        userDetailsContent.getChildren().clear();
        
        // Create a ScrollPane to enable scrolling for the details
        ScrollPane detailsScrollPane = new ScrollPane();
        detailsScrollPane.setFitToWidth(true);
        detailsScrollPane.setPrefViewportHeight(600);  // Set preferred viewport height
        detailsScrollPane.getStyleClass().add("edge-to-edge");
        detailsScrollPane.setStyle("-fx-background-color: transparent;");
        
        // Create user details view directly without ScrollPane
        VBox detailsContainer = new VBox();
        detailsContainer.setSpacing(15);
        detailsContainer.setPadding(new Insets(20));
        detailsContainer.getStyleClass().add("card");
        
        // User basic information section
        Label basicInfoTitle = new Label("Basic Information");
        basicInfoTitle.setFont(Font.font("System", 18));
        basicInfoTitle.setStyle("-fx-font-weight: bold;");
        
        // Create information grid
        VBox infoGrid = new VBox();
        infoGrid.setSpacing(10);
        
        // Add detail rows
        addDetailRow(infoGrid, "ID", String.valueOf(user.getId()));
        addDetailRow(infoGrid, "First Name", user.getFirstName());
        addDetailRow(infoGrid, "Last Name", user.getLastName());
        addDetailRow(infoGrid, "Email", user.getEmail());
        addDetailRow(infoGrid, "Phone", user.getPhone() != null ? user.getPhone() : "Not provided");
        addDetailRow(infoGrid, "Role", user.getRole().toString());
        addDetailRow(infoGrid, "Status", user.getStatus());
        addDetailRow(infoGrid, "Verified", user.isVerified() ? "Yes" : "No");
        
        // Additional information section (previously removed from table)
        Label additionalInfoTitle = new Label("Additional Information");
        additionalInfoTitle.setFont(Font.font("System", 18));
        additionalInfoTitle.setStyle("-fx-font-weight: bold; -fx-padding: 15 0 0 0;");
        
        VBox additionalInfo = new VBox();
        additionalInfo.setSpacing(10);
        
        // Add previously removed table columns
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String createdAt = user.getCreatedAt() != null ? 
                           user.getCreatedAt().format(formatter) : "Not available";
        
        addDetailRow(additionalInfo, "Created At", createdAt);
        addDetailRow(additionalInfo, "Warning Count", String.valueOf(user.getWarningCount()));
        
        // Account activity section
        Label activityTitle = new Label("Account Activity");
        activityTitle.setFont(Font.font("System", 18));
        activityTitle.setStyle("-fx-font-weight: bold; -fx-padding: 15 0 0 0;");
        
        VBox activityInfo = new VBox();
        activityInfo.setSpacing(10);
        
        String lastLogin = user.getLastLoginAt() != null ? 
                         user.getLastLoginAt().format(formatter) : "Never";
        
        addDetailRow(activityInfo, "Last Login", lastLogin);
        
        // Add all sections to container first
        detailsContainer.getChildren().addAll(
            basicInfoTitle, infoGrid, 
            additionalInfoTitle, additionalInfo,
            activityTitle, activityInfo
        );
        
        // Add NEW Profanity Evidence section if the user has warnings
        if (user.getWarningCount() > 0) {
            Label profanityTitle = new Label("Content Violation Evidence");
            profanityTitle.setFont(Font.font("System", 18));
            profanityTitle.setStyle("-fx-font-weight: bold; -fx-padding: 15 0 0 0; -fx-text-fill: #F44336;");
            
            VBox profanityInfo = new VBox();
            profanityInfo.setSpacing(10);
            profanityInfo.setStyle("-fx-background-color: #FFF8E1; -fx-background-radius: 5; -fx-padding: 10;");
            
            // Get violations from log file
            List<Map<String, String>> incidents = ProfanityLogManager.getProfanityIncidents(user.getId());
            
            if (incidents.isEmpty()) {
                Label noIncidents = new Label("No specific violation evidence recorded");
                noIncidents.setStyle("-fx-font-style: italic;");
                profanityInfo.getChildren().add(noIncidents);
            } else {
                Label explanationLabel = new Label("The following violations have been detected and logged:");
                explanationLabel.setStyle("-fx-font-style: italic; -fx-padding: 0 0 5 0;");
                profanityInfo.getChildren().add(explanationLabel);
                
                for (int i = 0; i < incidents.size(); i++) {
                    Map<String, String> incident = incidents.get(i);
                    
                    VBox incidentBox = new VBox(5);
                    incidentBox.setStyle("-fx-border-color: #FFE0B2; -fx-border-radius: 5; -fx-padding: 8;");
                    
                    Label dateLabel = new Label("Date: " + incident.get("date"));
                    dateLabel.setStyle("-fx-font-weight: bold;");
                    
                    Label fieldLabel = new Label("Field: " + incident.get("field"));
                    
                    // Display severity
                    String severityText = incident.get("severity");
                    Label severityLabel = new Label("Severity: " + severityText);
                    
                    // Set color based on severity
                    String textColor;
                    if ("High".equals(severityText)) {
                        textColor = "#D32F2F"; // Red
                        severityLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
                    } else if ("Medium".equals(severityText)) {
                        textColor = "#FF9800"; // Orange
                        severityLabel.setStyle("-fx-text-fill: " + textColor + ";");
                    } else {
                        textColor = "#FFC107"; // Yellow
                        severityLabel.setStyle("-fx-text-fill: " + textColor + ";");
                    }
                    
                    // Show action taken
                    Label actionLabel = new Label("Action: " + incident.get("action"));
                    
                    // NEW: Display the censored profane content if available
                    Label contentLabel = null;
                    if (incident.containsKey("profaneText") && 
                        !incident.get("profaneText").equals("[Content not recorded]")) {
                        contentLabel = new Label("Censored Content: " + incident.get("profaneText"));
                        contentLabel.setStyle("-fx-text-fill: #D32F2F; -fx-background-color: #FFEBEE; " +
                                           "-fx-padding: 3; -fx-background-radius: 3;");
                    }
                    
                    // Add all labels to the incident box
                    incidentBox.getChildren().addAll(dateLabel, fieldLabel, severityLabel, actionLabel);
                    if (contentLabel != null) {
                        incidentBox.getChildren().add(contentLabel);
                    }
                    
                    profanityInfo.getChildren().add(incidentBox);
                    
                    // Add separator between incidents
                    if (i < incidents.size() - 1) {
                        Separator separator = new Separator();
                        separator.setPadding(new Insets(5, 0, 5, 0));
                        profanityInfo.getChildren().add(separator);
                    }
                }
            }
            
            // Add options for admin to manage these incidents
            HBox actionBar = new HBox(10);
            actionBar.setPadding(new Insets(10, 0, 0, 0));
            
            Button clearWarningsBtn = new Button("Clear Warnings");
            clearWarningsBtn.setStyle("-fx-background-color: #FFA726; -fx-text-fill: white;");
            clearWarningsBtn.setOnAction(e -> clearUserWarnings(user));
            
            Button reportUserBtn = new Button("Send Warning Email");
            reportUserBtn.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
            reportUserBtn.setOnAction(e -> sendWarningEmail(user));
            
            actionBar.getChildren().addAll(clearWarningsBtn, reportUserBtn);
            
            // Add these to the details container
            detailsContainer.getChildren().addAll(profanityTitle, profanityInfo, actionBar);
        }
        
        // Set the detailsContainer as the content of the ScrollPane
        detailsScrollPane.setContent(detailsContainer);
        
        // Action buttons (put outside the scrollpane)
        HBox actionButtons = new HBox(10);
        actionButtons.setPadding(new Insets(20, 0, 0, 0));
        
        Button editButton = new Button("Edit User");
        editButton.getStyleClass().add("button-primary");
        editButton.setOnAction(e -> showEditUserDialog(user));
        
        Button statusButton;
        if ("active".equalsIgnoreCase(user.getStatus())) {
            statusButton = new Button("Disable User");
            statusButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        } else {
            statusButton = new Button("Activate User");
            statusButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        }
        statusButton.getStyleClass().add("button-secondary");
        statusButton.setOnAction(e -> {
            boolean wasActive = "active".equalsIgnoreCase(user.getStatus());
            toggleUserStatus(user, !wasActive);
            // We don't need to return to user list now, the toggleUserStatus method will refresh the details view
        });
        
        Button deleteButton = new Button("Delete User");
        deleteButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
        deleteButton.getStyleClass().add("button-secondary");
        deleteButton.setOnAction(e -> {
            deleteUser(user);
            // Return to user list after deletion (if successful)
            showUserManagement();
        });
        
        actionButtons.getChildren().addAll(editButton, statusButton, deleteButton);
        
        // Add everything to details content
        userDetailsContent.getChildren().addAll(detailsScrollPane, actionButtons);
        
        // Show the details view
        userManagementView.setVisible(false);
        userManagementView.setManaged(false);
        userDetailsView.setVisible(true);
        userDetailsView.setManaged(true);
    }
    
    // Method to clear user warnings
    private void clearUserWarnings(User user) {
        if (user == null) return;
        
        Alert confirmDialog = new Alert(AlertType.CONFIRMATION);
        confirmDialog.setTitle("Clear Warnings");
        confirmDialog.setHeaderText("Confirm Warning Reset");
        confirmDialog.setContentText("Are you sure you want to clear all warnings for user " + 
                                    user.getFirstName() + " " + user.getLastName() + "?");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Reset warning count
                user.setWarningCount(0);
                
                // Save to database
                userService.modifier(user);
                
                // Refresh the user details view
                showUserDetails(user);
                
                showAlert("Success", "Warnings Cleared", 
                         "All warnings have been cleared for user " + 
                         user.getFirstName() + " " + user.getLastName());
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to Clear Warnings", 
                         "An error occurred: " + e.getMessage());
            }
        }
    }
    
    // Method to send a warning email
    private void sendWarningEmail(User user) {
        if (user == null) return;
        
        // In a real application, this would send an actual email
        showAlert("Notification Sent", "Warning Email", 
                 "A warning email has been sent to " + user.getEmail() + 
                 " regarding their use of profanity.");
    }
    
    private void addDetailRow(VBox container, String label, String value) {
        HBox row = new HBox(10);
        
        Label labelNode = new Label(label + ":");
        labelNode.setStyle("-fx-font-weight: bold; -fx-min-width: 120;");
        
        Label valueNode = new Label(value);
        
        row.getChildren().addAll(labelNode, valueNode);
        container.getChildren().add(row);
    }
    
    private void showEditUserDialog(User user) {
        // Placeholder for edit functionality
        showAlert("Feature Not Available", "Edit User", 
                 "User editing functionality will be implemented in a future update.");
    }
    
    private void updatePagination() {
        // Handle the case where filteredUsers might be null
        if (filteredUsers == null) {
            currentPageLabel.setText("1");
            totalPagesLabel.setText("1");
            prevPageButton.setDisable(true);
            nextPageButton.setDisable(true);
            usersTable.setItems(FXCollections.observableArrayList());
            return;
        }
        
        // Calculate total pages
        totalPages = (int) Math.ceil((double) filteredUsers.size() / ROWS_PER_PAGE);
        if (totalPages < 1) totalPages = 1;
        
        // Make sure current page is valid
        if (currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }
        if (currentPage < 0) {
            currentPage = 0;
        }
        
        // Update pagination labels
        currentPageLabel.setText(String.valueOf(currentPage + 1)); // 1-based for display
        totalPagesLabel.setText(String.valueOf(totalPages));
        
        // Update button states
        prevPageButton.setDisable(currentPage == 0);
        nextPageButton.setDisable(currentPage >= totalPages - 1);
        
        // Load current page data
        loadPage(currentPage);
    }
    
    private void loadPage(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, filteredUsers.size());
        
        ObservableList<User> pageItems;
        if (filteredUsers.size() > 0 && fromIndex < toIndex) {
            pageItems = FXCollections.observableArrayList(
                filteredUsers.subList(fromIndex, toIndex));
        } else {
            pageItems = FXCollections.observableArrayList();
        }
        
        // Set table items
        usersTable.setItems(pageItems);
        
        // Ensure we have exactly 6 rows of height in the table (even with fewer items)
        int itemCount = pageItems.size();
        if (itemCount < ROWS_PER_PAGE) {
            // Add empty rows to fill the table
            for (int i = itemCount; i < ROWS_PER_PAGE; i++) {
                pageItems.add(null);
            }
        }
    }
    
    @FXML
    private void handlePrevPage() {
        if (currentPage > 0) {
            currentPage--;
            updatePagination();
        }
    }
    
    @FXML
    private void handleNextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            updatePagination();
        }
    }
    
    private void loadUserData() {
        try {
            // Get all users
            List<User> allUsers = userService.recuperer();
            
            // Filter out admin users for the table display
            usersList.clear();
            for (User user : allUsers) {
                if (!"ADMINISTRATEUR".equals(user.getRole().toString())) {
                    usersList.add(user);
                }
            }
            
            filteredUsers = new FilteredList<>(usersList, p -> true);
            
            // Reset current page and update pagination
            currentPage = 0;
            updatePagination();
            
            // Calculate new insightful statistics instead of basic counts
            
            // 1. New users this month
            LocalDate now = LocalDate.now();
            LocalDate firstOfMonth = now.withDayOfMonth(1);
            
            long newUsersThisMonth = usersList.stream()
                .filter(u -> u.getCreatedAt() != null && 
                       u.getCreatedAt().toLocalDate().isAfter(firstOfMonth.minusDays(1)))
                .count();
            
            // 2. Inactive users (haven't logged in for 30+ days)
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            
            long inactiveUsers = usersList.stream()
                .filter(u -> u.getLastLoginAt() == null || // Never logged in
                       u.getLastLoginAt().isBefore(thirtyDaysAgo))
                .count();
            
            // 3. Users requiring attention (with warning counts)
            long usersNeedingAttention = usersList.stream()
                .filter(u -> u.getWarningCount() > 0)
                .count();
            
            // Update the card labels with new statistics
            totalUsersLabel.setText(String.valueOf(newUsersThisMonth));
            activeUsersLabel.setText(String.valueOf(inactiveUsers));
            unverifiedUsersLabel.setText(String.valueOf(usersNeedingAttention));
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Data Loading Error", "Failed to load user data: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleSearch() {
        // The filtering is handled by the listener already
    }
    
    @FXML
    private void showCreateUser() {
        // Show a placeholder alert for now
        showAlert("Create User", "Feature Not Implemented", 
                 "The create user functionality will be implemented in a future update.");
    }
    
    @FXML
    private void showUserManagement() {
        contentTitle.setText("User Management");
        setActiveButton(userManagementButton);
        
        // Update the labels of the stat cards to reflect their new meaning
        // Find and update the card title labels
        updateStatCardLabels();
        
        // Switch back to user list view
        userDetailsView.setVisible(false);
        userDetailsView.setManaged(false);
        userManagementView.setVisible(true);
        userManagementView.setManaged(true);
        
        // Ensure user management view is visible in stack pane
        contentStackPane.getChildren().forEach(node -> {
            boolean isUserManagementView = node == userManagementView;
            node.setVisible(isUserManagementView);
            node.setManaged(isUserManagementView);
        });
        
        // Refresh user data
        loadUserData();
    }
    
    // Method to update the labels of the stat cards
    private void updateStatCardLabels() {
        // The method assumes there are labels near the totalUsersLabel, activeUsersLabel, etc.
        // Find their parent containers and update the title labels
        
        // Look for the parent containers of the stat value labels and find their sibling labels
        if (totalUsersLabel.getParent() != null && totalUsersLabel.getParent() instanceof VBox) {
            VBox container = (VBox) totalUsersLabel.getParent();
            for (javafx.scene.Node node : container.getChildren()) {
                if (node instanceof Label && node != totalUsersLabel) {
                    ((Label) node).setText("New This Month");
                    break;
                }
            }
        }
        
        if (activeUsersLabel.getParent() != null && activeUsersLabel.getParent() instanceof VBox) {
            VBox container = (VBox) activeUsersLabel.getParent();
            for (javafx.scene.Node node : container.getChildren()) {
                if (node instanceof Label && node != activeUsersLabel) {
                    ((Label) node).setText("Inactive (30+ days)");
                    break;
                }
            }
        }
        
        if (unverifiedUsersLabel.getParent() != null && unverifiedUsersLabel.getParent() instanceof VBox) {
            VBox container = (VBox) unverifiedUsersLabel.getParent();
            for (javafx.scene.Node node : container.getChildren()) {
                if (node instanceof Label && node != unverifiedUsersLabel) {
                    ((Label) node).setText("Need Attention");
                    break;
                }
            }
        }
    }
    
    @FXML
    private void showUserStatistics() {
        // Update header title
        contentTitle.setText("User Statistics");
        
        // Create a fresh VBox container for statistics
        VBox statsView = new VBox(15);
        statsView.setPadding(new Insets(20));
        statsView.setStyle("-fx-background-color: #f8f9fa;");
        
        // Back button
        Button backButton = new Button("← Back to Users");
        backButton.getStyleClass().add("button-primary");
        backButton.setOnAction(e -> showUserManagement());
        
        // Header area
        HBox headerArea = new HBox(15);
        headerArea.getChildren().add(backButton);
        headerArea.setPadding(new Insets(0, 0, 15, 0));
        statsView.getChildren().add(headerArea);
        
        // Main stats title
        Label statsTitle = new Label("User Statistics Dashboard");
        statsTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        statsView.getChildren().add(statsTitle);
        
        // Card stats in a grid (3 cards in a row)
        HBox statsCards = new HBox(20);
        statsCards.setAlignment(Pos.CENTER_LEFT);
        statsCards.setPadding(new Insets(10, 0, 15, 0));
        
        // Total users
        int totalUsers = (int) usersList.stream()
                .filter(u -> !"ADMINISTRATEUR".equals(u.getRole().toString()))
                .count();
        
        // Verified users
        long verifiedCount = usersList.stream()
                .filter(u -> !"ADMINISTRATEUR".equals(u.getRole().toString()) && u.isVerified())
                .count();
        
        // Active users
        long activeCount = usersList.stream()
                .filter(u -> !"ADMINISTRATEUR".equals(u.getRole().toString()) && "active".equalsIgnoreCase(u.getStatus()))
                .count();
        
        // Create simple card for total users - made more compact
        VBox totalUsersCard = new VBox(5);
        totalUsersCard.setPadding(new Insets(15));
        totalUsersCard.setAlignment(Pos.CENTER);
        totalUsersCard.setPrefWidth(160);
        totalUsersCard.setMinWidth(160);
        totalUsersCard.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e0e0e0;");
        HBox.setHgrow(totalUsersCard, Priority.ALWAYS);
        
        Label totalIcon = new Label("👥");
        totalIcon.setStyle("-fx-font-size: 28px;");
        
        Label totalCount = new Label(String.valueOf(totalUsers));
        totalCount.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2196F3;");
        
        Label totalLabel = new Label("Total Users");
        totalLabel.setStyle("-fx-font-size: 14px;");
        
        totalUsersCard.getChildren().addAll(totalIcon, totalCount, totalLabel);
        
        // Create simple card for verified users - made more compact
        VBox verifiedUsersCard = new VBox(5);
        verifiedUsersCard.setPadding(new Insets(15));
        verifiedUsersCard.setAlignment(Pos.CENTER);
        verifiedUsersCard.setPrefWidth(160);
        verifiedUsersCard.setMinWidth(160);
        verifiedUsersCard.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e0e0e0;");
        HBox.setHgrow(verifiedUsersCard, Priority.ALWAYS);
        
        Label verifiedIcon = new Label("✓");
        verifiedIcon.setStyle("-fx-font-size: 28px; -fx-text-fill: #4CAF50;");
        
        Label verifiedCountLabel = new Label(String.valueOf(verifiedCount));
        verifiedCountLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
        
        Label verifiedLabel = new Label("Verified Users");
        verifiedLabel.setStyle("-fx-font-size: 14px;");
        
        verifiedUsersCard.getChildren().addAll(verifiedIcon, verifiedCountLabel, verifiedLabel);
        
        // Create simple card for active users - made more compact
        VBox activeUsersCard = new VBox(5);
        activeUsersCard.setPadding(new Insets(15));
        activeUsersCard.setAlignment(Pos.CENTER);
        activeUsersCard.setPrefWidth(160);
        activeUsersCard.setMinWidth(160);
        activeUsersCard.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e0e0e0;");
        HBox.setHgrow(activeUsersCard, Priority.ALWAYS);
        
        Label activeIcon = new Label("●");
        activeIcon.setStyle("-fx-font-size: 28px; -fx-text-fill: #4CAF50;");
        
        Label activeCountLabel = new Label(String.valueOf(activeCount));
        activeCountLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #FFC107;");
        
        Label activeLabel = new Label("Active Users");
        activeLabel.setStyle("-fx-font-size: 14px;");
        
        activeUsersCard.getChildren().addAll(activeIcon, activeCountLabel, activeLabel);
        
        // Add all cards to card container
        statsCards.getChildren().addAll(totalUsersCard, verifiedUsersCard, activeUsersCard);
        statsView.getChildren().add(statsCards);
        
        // Create a container for the two distributions (side by side)
        HBox distributionsContainer = new HBox(20);
        distributionsContainer.setPadding(new Insets(0, 0, 15, 0));
        
        // Create left column for status distribution
        VBox statusColumn = new VBox(10);
        statusColumn.setPrefWidth(480);
        HBox.setHgrow(statusColumn, Priority.ALWAYS);
        
        // Status distribution title
        Label statusTitle = new Label("User Status Distribution");
        statusTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        statusColumn.getChildren().add(statusTitle);
        
        // Status bars container
        VBox statusContainer = new VBox(8);
        statusContainer.setPadding(new Insets(15));
        statusContainer.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e0e0e0;");
        
        // Calculate status distributions
        int activeUsers = (int)activeCount;
        int inactiveUsers = totalUsers - activeUsers;
        
        // Active status row
        HBox activeRow = new HBox(10);
        activeRow.setAlignment(Pos.CENTER_LEFT);
        
        Label activeTextLabel = new Label("Active:");
        activeTextLabel.setMinWidth(70);
        activeTextLabel.setStyle("-fx-font-weight: bold;");
        
        ProgressBar activeBar = new ProgressBar((double)activeUsers/totalUsers);
        activeBar.setPrefWidth(240);
        activeBar.setStyle("-fx-accent: #4CAF50;");
        HBox.setHgrow(activeBar, Priority.ALWAYS);
        
        Label activeStatsLabel = new Label(String.format("%d (%.1f%%)", 
                activeUsers, totalUsers > 0 ? (double)activeUsers/totalUsers*100 : 0));
        
        activeRow.getChildren().addAll(activeTextLabel, activeBar, activeStatsLabel);
        
        // Inactive status row
        HBox inactiveRow = new HBox(10);
        inactiveRow.setAlignment(Pos.CENTER_LEFT);
        
        Label inactiveTextLabel = new Label("Inactive:");
        inactiveTextLabel.setMinWidth(70);
        inactiveTextLabel.setStyle("-fx-font-weight: bold;");
        
        ProgressBar inactiveBar = new ProgressBar((double)inactiveUsers/totalUsers);
        inactiveBar.setPrefWidth(240);
        inactiveBar.setStyle("-fx-accent: #F44336;");
        HBox.setHgrow(inactiveBar, Priority.ALWAYS);
        
        Label inactiveStatsLabel = new Label(String.format("%d (%.1f%%)", 
                inactiveUsers, totalUsers > 0 ? (double)inactiveUsers/totalUsers*100 : 0));
        
        inactiveRow.getChildren().addAll(inactiveTextLabel, inactiveBar, inactiveStatsLabel);
        
        statusContainer.getChildren().addAll(activeRow, inactiveRow);
        statusColumn.getChildren().add(statusContainer);
        
        // Create right column for role distribution
        VBox roleColumn = new VBox(10);
        roleColumn.setPrefWidth(480);
        HBox.setHgrow(roleColumn, Priority.ALWAYS);
        
        // Role distribution title
        Label roleTitle = new Label("User Role Distribution");
        roleTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        roleColumn.getChildren().add(roleTitle);
        
        // Count users by role
        Map<String, Integer> roleCounts = new HashMap<>();
        for (User user : usersList) {
            if (!"ADMINISTRATEUR".equals(user.getRole().toString())) {
                String role = user.getRole().toString();
                roleCounts.put(role, roleCounts.getOrDefault(role, 0) + 1);
            }
        }
        
        // Role distribution container
        VBox roleContainer = new VBox(8);
        roleContainer.setPadding(new Insets(15));
        roleContainer.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e0e0e0;");
        
        // Add rows for each role
        for (String role : new String[]{"NON_MEMBRE", "MEMBRE", "PRESIDENT_CLUB"}) {
            int count = roleCounts.getOrDefault(role, 0);
            
            HBox roleRow = new HBox(10);
            roleRow.setAlignment(Pos.CENTER_LEFT);
            
            Label roleLabel = new Label(formatRoleName(role) + ":");
            roleLabel.setMinWidth(120);
            roleLabel.setStyle("-fx-font-weight: bold;");
            
            ProgressBar roleBar = new ProgressBar((double)count/totalUsers);
            roleBar.setPrefWidth(180);
            roleBar.setStyle("-fx-accent: " + getRoleColor(role) + ";");
            HBox.setHgrow(roleBar, Priority.ALWAYS);
            
            Label roleStatsLabel = new Label(String.format("%d (%.1f%%)", 
                    count, totalUsers > 0 ? (double)count/totalUsers*100 : 0));
            
            roleRow.getChildren().addAll(roleLabel, roleBar, roleStatsLabel);
            roleContainer.getChildren().add(roleRow);
        }
        
        roleColumn.getChildren().add(roleContainer);
        
        // Add columns to the distributions container
        distributionsContainer.getChildren().addAll(statusColumn, roleColumn);
        statsView.getChildren().add(distributionsContainer);
        
        // Recent registrations section
        Label recentTitle = new Label("Recent User Registrations");
        recentTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        statsView.getChildren().add(recentTitle);
        
        // Recent registrations container
        VBox recentContainer = new VBox(5);
        recentContainer.setPadding(new Insets(15));
        recentContainer.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e0e0e0;");
        
        // Sort users by creation date
        List<User> sortedUsers = new ArrayList<>(usersList);
        sortedUsers.sort((u1, u2) -> {
            if (u1.getCreatedAt() == null && u2.getCreatedAt() == null) return 0;
            if (u1.getCreatedAt() == null) return 1;
            if (u2.getCreatedAt() == null) return -1;
            return u2.getCreatedAt().compareTo(u1.getCreatedAt());
        });
        
        // Show up to 3 recent users
        int count = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        if (sortedUsers.isEmpty()) {
            recentContainer.getChildren().add(new Label("No users registered yet"));
        } else {
            for (User user : sortedUsers) {
                if (count >= 3) break;
                if ("ADMINISTRATEUR".equals(user.getRole().toString())) continue;
                
                HBox userRow = new HBox(10);
                userRow.setAlignment(Pos.CENTER_LEFT);
                userRow.setPadding(new Insets(5));
                userRow.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 5;");
                
                // Date
                String date = user.getCreatedAt() != null ? 
                            user.getCreatedAt().format(formatter) : "Unknown";
                
                Label dateLabel = new Label(date);
                dateLabel.setMinWidth(90);
                dateLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
                
                // User details
                VBox userDetails = new VBox(2);
                
                Label nameLabel = new Label(user.getFirstName() + " " + user.getLastName());
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                
                Label emailLabel = new Label(user.getEmail());
                emailLabel.setStyle("-fx-font-style: italic; -fx-font-size: 11px;");
                
                userDetails.getChildren().addAll(nameLabel, emailLabel);
                HBox.setHgrow(userDetails, Priority.ALWAYS);
                
                // Status
                String statusSymbol = user.isVerified() ? "✓" : "✗";
                String statusColor = user.isVerified() ? "#4CAF50" : "#F44336";
                
                Label statusLabel = new Label(statusSymbol);
                statusLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: " + statusColor + ";");
                
                userRow.getChildren().addAll(dateLabel, userDetails, statusLabel);
                recentContainer.getChildren().add(userRow);
                
                if (count < 2) {
                    Separator separator = new Separator();
                    recentContainer.getChildren().add(separator);
                }
                
                count++;
            }
        }
        
        statsView.getChildren().add(recentContainer);
        
        // Hide other views and show stats view
        userManagementView.setVisible(false);
        userManagementView.setManaged(false);
        userDetailsView.setVisible(false);
        userDetailsView.setManaged(false);
        
        // Add to content stack pane (replacing previous content)
        contentStackPane.getChildren().clear();
        contentStackPane.getChildren().addAll(userManagementView, userDetailsView, statsView);
    }
    
    // Helper method to get role color
    private String getRoleColor(String role) {
        switch (role) {
            case "MEMBRE": return "#4CAF50"; // Green
            case "PRESIDENT_CLUB": return "#FFC107"; // Gold
            default: return "#2196F3"; // Blue
        }
    }
    
    // Format role name for display
    private String formatRoleName(String roleName) {
        switch(roleName) {
            case "NON_MEMBRE":
                return "Non-Member";
            case "MEMBRE":
                return "Member";
            case "PRESIDENT_CLUB":
                return "Club President";
            case "ADMINISTRATEUR":
                return "Administrator";
            default:
                return roleName;
        }
    }

    @FXML
    public void showClubManagement() {
        try {
            // Load the ClubView
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/ClubView.fxml"));
            Parent root = loader.load();

            // Get the current stage
            Stage stage = (Stage) contentTitle.getScene().getWindow();

            // Create a new scene
            Scene scene = new Scene(root);

            // Make sure the stylesheets are properly applied
            scene.getStylesheets().add(getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/com/esprit/styles/no-scrollbar.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/com/esprit/styles/home.css").toExternalForm());

            // Set the scene to the stage
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error", "Navigation Error", "Failed to navigate to Club Management: " + e.getMessage());
        }
    }
    
    @FXML
    public void showEventManagement() {
        contentTitle.setText("Event Management");
        setActiveButton(eventManagementButton);
        
        // Show placeholder for Event module
        showModulePlaceholder("Event Management");
    }
    
    @FXML
    public void showProductOrders() {
        contentTitle.setText("Products & Orders");
        setActiveButton(productOrdersButton);
        
        // Show placeholder for Products module
        showModulePlaceholder("Products & Orders");
    }
    
    @FXML
    public void showCompetition() {
        contentTitle.setText("Competition & Season");
        setActiveButton(competitionButton);
        
        // Show placeholder for Competition module
        showModulePlaceholder("Competition & Season");
    }
    
    @FXML
    private void showSurvey() {
        contentTitle.setText("Survey Management");
        setActiveButton(surveyButton);
        
        // Toggle the visibility of the survey submenu
        boolean isVisible = surveySubMenu.isVisible();
        surveySubMenu.setVisible(!isVisible);
        surveySubMenu.setManaged(!isVisible);
    }
    
    /**
     * Navigate to the Poll Management view
     */
    @FXML
    public void navigateToPollsManagement() {
        try {
            // Load the AdminPollsView
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/AdminPollsView.fxml"));
            Parent root = loader.load();
            
            // Get the current stage
            Stage stage = (Stage) contentArea.getScene().getWindow();
            
            // Create a new scene
            Scene scene = new Scene(root);
            
            // Make sure the stylesheets are properly applied
            scene.getStylesheets().add(getClass().getResource("/com/esprit/styles/admin-polls-style.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm());
            
            // Set the scene to the stage
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Navigation Error", "Failed to navigate to Poll Management: " + e.getMessage());
        }
    }
    
    /**
     * Navigate to the Comment Management view
     */
    @FXML
    public void navigateToCommentsManagement() {
        try {
            // Load the AdminCommentsView
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/AdminCommentsView.fxml"));
            Parent root = loader.load();
            
            // Get the current stage
            Stage stage = (Stage) contentArea.getScene().getWindow();
            
            // Create a new scene
            Scene scene = new Scene(root);
            
            // Make sure the stylesheets are properly applied
            scene.getStylesheets().add(getClass().getResource("/com/esprit/styles/admin-polls-style.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/com/esprit/styles/uniclubs.css").toExternalForm());
            
            // Set the scene to the stage
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Navigation Error", "Failed to navigate to Comment Management: " + e.getMessage());
        }
    }
    
    private void showModulePlaceholder(String moduleName) {
        try {
            // Create a placeholder content
            VBox placeholder = new VBox();
            placeholder.setSpacing(20);
            placeholder.setStyle("-fx-padding: 50; -fx-alignment: center;");
            
            Label title = new Label(moduleName + " Module");
            title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
            
            Label message = new Label("This module is being developed by another team member.\nPlease check back later.");
            message.setStyle("-fx-font-size: 16px; -fx-text-alignment: center;");
            
            Button backButton = new Button("Go to User Management");
            backButton.setOnAction(e -> showUserManagement());
            backButton.getStyleClass().add("button-primary");
            
            placeholder.getChildren().addAll(title, message, backButton);
            
            // Hide both user management and user details views
            userManagementView.setVisible(false);
            userManagementView.setManaged(false);
            userDetailsView.setVisible(false);
            userDetailsView.setManaged(false);
            
            // Replace the content
            contentStackPane.getChildren().clear();
            contentStackPane.getChildren().addAll(userManagementView, userDetailsView, placeholder);
            placeholder.setVisible(true);
            placeholder.setManaged(true);
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Navigation Error", "Failed to show " + moduleName);
        }
    }
    
    private void setActiveButton(Button activeButton) {
        // Reset all buttons
        for (Button btn : new Button[]{userManagementButton, clubManagementButton, 
                                     eventManagementButton, productOrdersButton, 
                                     competitionButton, surveyButton}) {
            btn.getStyleClass().remove("active");
        }
        
        // Set the active button
        activeButton.getStyleClass().add("active");
    }
    
    @FXML
    private void navigateToProfile() {
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
            showAlert("Error", "Navigation Error", "Failed to navigate to admin profile");
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
            showAlert("Error", "Logout Error", "Failed to navigate to login page");
        }
    }
    
    private void navigateToLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/login.fxml"));
        Parent root = loader.load();
        
        Stage stage = (Stage) (contentArea != null ? contentArea.getScene().getWindow() : 
                             (adminNameLabel != null ? adminNameLabel.getScene().getWindow() : null));
        
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
    
    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
package controllers;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import entities.User;
import enums.RoleEnum;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import services.AuthService;
import services.UserService;
import test.MainApp;
import utils.SessionManager;

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
    private Pagination usersPagination;
    
    private final AuthService authService = new AuthService();
    private UserService userService;
    private User currentUser;
    private ObservableList<User> usersList = FXCollections.observableArrayList();
    private FilteredList<User> filteredUsers;
    
    // Pagination Constants
    private static final int ROWS_PER_PAGE = 7;
    
    @FXML
    private void initialize() {
        try {
            // Initialize services
            userService = new UserService();
            
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
        
        // Disable horizontal scrollbar directly - don't try to use lookup, which causes a ClassCastException
        usersTable.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            // Simply set the property directly on the TableView
            // This is more reliable than trying to access the internal VirtualFlow
            usersTable.setStyle(usersTable.getStyle() + "; -fx-hbar-policy: never;");
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
                
                // Update pagination
                updatePagination();
            } 
        });
        
        // Setup pagination
        usersPagination.setPageFactory(this::createPage);
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
                
                // Update the filtered list and pagination
                if (filteredUsers != null) {
                    filteredUsers.setPredicate(filteredUsers.getPredicate());
                    updatePagination();
                }
                
                // If in details view, update the details content
                if (userDetailsView.isVisible()) {
                    showUserDetails(user);
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
        
        // Create user details view
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
        
        // Add all sections to container
        detailsContainer.getChildren().addAll(
            basicInfoTitle, infoGrid, 
            additionalInfoTitle, additionalInfo,
            activityTitle, activityInfo
        );
        
        // Action buttons
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
        userDetailsContent.getChildren().addAll(detailsContainer, actionButtons);
        
        // Show the details view
        userManagementView.setVisible(false);
        userManagementView.setManaged(false);
        userDetailsView.setVisible(true);
        userDetailsView.setManaged(true);
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
            
            // Update pagination
            updatePagination();
            
            // Update statistics
            int totalUsers = allUsers.size() - countAdminUsers(allUsers);
            int activeUsers = countActiveUsers(allUsers);
            int unverifiedUsers = countUnverifiedUsers(allUsers);
            
            totalUsersLabel.setText(String.valueOf(totalUsers));
            activeUsersLabel.setText(String.valueOf(activeUsers));
            unverifiedUsersLabel.setText(String.valueOf(unverifiedUsers));
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Data Loading Error", "Failed to load user data: " + e.getMessage());
        }
    }
    
    private void updatePagination() {
            int totalPages = (int) Math.ceil((double) filteredUsers.size() / ROWS_PER_PAGE);
            usersPagination.setPageCount(Math.max(1, totalPages));
        
        // Reset to first page when data changes
        if (usersPagination.getCurrentPageIndex() >= totalPages) {
            usersPagination.setCurrentPageIndex(0);
        }
        
        // Create current page
        createPage(usersPagination.getCurrentPageIndex());
    }
    
    private javafx.scene.Node createPage(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, filteredUsers.size());
        
        // Create a sublist for the current page
        if (filteredUsers.size() > 0 && fromIndex < toIndex) {
            usersTable.setItems(FXCollections.observableArrayList(
                filteredUsers.subList(fromIndex, toIndex)));
        } else {
            usersTable.setItems(FXCollections.observableArrayList());
        }
        
        return usersTable;
    }
    
    private int countAdminUsers(List<User> users) {
        int count = 0;
        for (User user : users) {
            if ("ADMINISTRATEUR".equals(user.getRole().toString())) {
                count++;
            }
        }
        return count;
    }
    
    private int countActiveUsers(List<User> users) {
        int count = 0;
        for (User user : users) {
            if (!"ADMINISTRATEUR".equals(user.getRole().toString()) && "active".equalsIgnoreCase(user.getStatus())) {
                count++;
            }
        }
        return count;
    }
    
    private int countUnverifiedUsers(List<User> users) {
        int count = 0;
        for (User user : users) {
            if (!"ADMINISTRATEUR".equals(user.getRole().toString()) && !user.isVerified()) {
                count++;
            }
        }
        return count;
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
    
    @FXML
    private void showUserStatistics() {
        contentTitle.setText("User Statistics");
        
        // Create main container with BorderPane for better organization
        VBox statsView = new VBox();
        statsView.setStyle("-fx-background-color: #f8f9fa;");
        
        // Clear any previous content
        statsView.getChildren().clear();
        
        // Create container for better layout
        BorderPane mainContainer = new BorderPane();
        mainContainer.setPadding(new Insets(30));
        
        // Top bar with back button and title
        HBox topBar = new HBox(30);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 35, 0));
        
        // Back button
        Button backButton = new Button("← Back to Users");
        backButton.getStyleClass().add("transparent-button");
        backButton.setStyle("-fx-font-size: 16px;");
        backButton.setOnAction(e -> {
            showUserManagement();
        });
        
        // Title
        Label titleLabel = new Label("User Statistics Dashboard");
        titleLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold;");
        
        topBar.getChildren().addAll(backButton, titleLabel);
        
        // Set the top bar
        mainContainer.setTop(topBar);
        
        // Create a horizontal box for main content
        HBox contentBox = new HBox(40);
        contentBox.setPadding(new Insets(25));
        
        // Left section - User overview and status distribution
        VBox leftSection = new VBox(40);
        leftSection.setPrefWidth(600);
        
        // User statistics cards
        VBox statsCards = new VBox(20);
        
        Label statsTitle = new Label("User Overview");
        statsTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        statsTitle.setPadding(new Insets(0, 0, 15, 0));
        
        // Create a horizontal box for the stat cards
        HBox cardsBox = new HBox(35);
        cardsBox.setAlignment(Pos.CENTER);
        
        // Total users card
        int totalUsers = (int) usersList.stream()
                .filter(u -> !"ADMINISTRATEUR".equals(u.getRole().toString()))
                .count();
        VBox totalUsersCard = createSimpleStatCard("Total Users", String.valueOf(totalUsers), "#2196F3");
        
        // Verified users card
        long verifiedCount = usersList.stream()
                .filter(u -> !"ADMINISTRATEUR".equals(u.getRole().toString()) && u.isVerified())
                .count();
        VBox verifiedUsersCard = createSimpleStatCard("Verified Users", String.valueOf(verifiedCount), "#4CAF50");
        
        // Active users card
        long activeCount = usersList.stream()
                .filter(u -> !"ADMINISTRATEUR".equals(u.getRole().toString()) && "active".equalsIgnoreCase(u.getStatus()))
                .count();
        VBox activeUsersCard = createSimpleStatCard("Active Users", String.valueOf(activeCount), "#FFC107");
        
        cardsBox.getChildren().addAll(totalUsersCard, verifiedUsersCard, activeUsersCard);
        statsCards.getChildren().addAll(statsTitle, cardsBox);
        
        // Status distribution section
        VBox statusSection = new VBox(20);
        
        Label statusTitle = new Label("User Status Distribution");
        statusTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        statusTitle.setPadding(new Insets(25, 0, 15, 0));
        
        VBox statusBars = createStatusBars();
        statusSection.getChildren().addAll(statusTitle, statusBars);
        
        // Add sections to left container
        leftSection.getChildren().addAll(statsCards, statusSection);
        
        // Right section - Role distribution and recent registrations
        VBox rightSection = new VBox(40);
        rightSection.setPrefWidth(600);
        
        // Role distribution
        VBox roleSection = new VBox(20);
        
        Label roleTitle = new Label("User Role Distribution");
        roleTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        roleTitle.setPadding(new Insets(0, 0, 15, 0));
        
        VBox roleDistribution = createRoleDistributionTable();
        roleSection.getChildren().addAll(roleTitle, roleDistribution);
        
        // Recent registrations
        VBox recentSection = new VBox(20);
        
        Label recentTitle = new Label("Recent User Registrations");
        recentTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        recentTitle.setPadding(new Insets(25, 0, 15, 0));
        
        VBox recentUsers = createRecentRegistrationsList();
        
        // Add a simple border to the recent users list to make it stand out
        recentUsers.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-color: white; -fx-padding: 15;");
        
        recentSection.getChildren().addAll(recentTitle, recentUsers);
        
        // Add sections to right container
        rightSection.getChildren().addAll(roleSection, recentSection);
        
        // Add left and right sections to the main content box
        contentBox.getChildren().addAll(leftSection, rightSection);
        
        // Set the center content
        mainContainer.setCenter(contentBox);
        
        // Add the main container to the stats view
        statsView.getChildren().add(mainContainer);
        
        // Hide other views and show stats view
        userManagementView.setVisible(false);
        userManagementView.setManaged(false);
        userDetailsView.setVisible(false);
        userDetailsView.setManaged(false);
        
        // Add to content stack pane (replacing previous content)
        contentStackPane.getChildren().clear();
        contentStackPane.getChildren().addAll(userManagementView, userDetailsView, statsView);
    }

    // Simplified stat card method
    private VBox createSimpleStatCard(String title, String value, String color) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setMinWidth(160);
        card.setMinHeight(150);
        
        // Icon at the top with color matching the value
        Label iconLabel = new Label(getIconForStat(title));
        iconLabel.setStyle("-fx-font-size: 28px; -fx-text-fill: " + color + ";");
        
        // Value in the middle with large font
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        
        // Title at the bottom
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #757575; -fx-font-size: 16px;");
        
        card.getChildren().addAll(iconLabel, valueLabel, titleLabel);
        return card;
    }
    
    // Helper method to get appropriate icon for stat cards
    private String getIconForStat(String title) {
        switch(title) {
            case "Total Users": return "👥";
            case "Active Users": return "✅";
            case "Inactive Users": return "❌";
            case "Verified Users": return "✔️";
            case "Unverified Users": return "⚠️";
            default: return "❓";
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

    // Create role distribution table
    private VBox createRoleDistributionTable() {
        VBox container = new VBox(10);
        
        // Count users by role
        Map<String, Integer> roleCounts = new HashMap<>();
        int totalUsers = 0;
        
        for (User user : usersList) {
            if (!"ADMINISTRATEUR".equals(user.getRole().toString())) {
            String role = user.getRole().toString();
            roleCounts.put(role, roleCounts.getOrDefault(role, 0) + 1);
                totalUsers++;
            }
        }
        
        // Create grid for role data
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(5));
        
        // Headers
        Label roleHeader = new Label("Role");
        roleHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");
        Label countHeader = new Label("Count");
        countHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");
        Label percentHeader = new Label("%");
        percentHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");
        Label barHeader = new Label("Distribution");
        barHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");
        
        grid.add(roleHeader, 0, 0);
        grid.add(countHeader, 1, 0);
        grid.add(percentHeader, 2, 0);
        grid.add(barHeader, 3, 0);
        
        // Add rows for each role
        int row = 1;
        for (String role : new String[]{"NON_MEMBRE", "MEMBRE", "PRESIDENT_CLUB"}) {
            int count = roleCounts.getOrDefault(role, 0);
            double percentage = totalUsers > 0 ? (double) count / totalUsers * 100 : 0;
            
            Label roleLabel = new Label(formatRoleName(role));
            roleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            
            Label countLabel = new Label(String.valueOf(count));
            countLabel.setStyle("-fx-font-size: 14px;");
            
            Label percentLabel = new Label(String.format("%.0f%%", percentage));
            percentLabel.setStyle("-fx-font-size: 14px;");
            
            // Progress bar
            ProgressBar progressBar = new ProgressBar(percentage / 100);
            progressBar.setPrefWidth(200);
            progressBar.setPrefHeight(15);
            
            // Choose color based on role
            String color = "#2196F3"; // default blue
            if (role.equals("MEMBRE")) color = "#4CAF50"; // green for members
            if (role.equals("PRESIDENT_CLUB")) color = "#FFC107"; // gold for presidents
            
            progressBar.setStyle("-fx-accent: " + color + ";");
            
            grid.add(roleLabel, 0, row);
            grid.add(countLabel, 1, row);
            grid.add(percentLabel, 2, row);
            grid.add(progressBar, 3, row);
            
            row++;
        }
        
        // Add the grid to the container
        container.getChildren().add(grid);
        
        return container;
    }
    
    // Create status distribution bars
    private VBox createStatusBars() {
        VBox container = new VBox(12);
        
        // Count users by status
        int activeCount = 0;
        int inactiveCount = 0;
        int totalUsers = 0;
        
        for (User user : usersList) {
            if (!"ADMINISTRATEUR".equals(user.getRole().toString())) {
                totalUsers++;
            if ("active".equalsIgnoreCase(user.getStatus())) {
                activeCount++;
            } else {
                inactiveCount++;
            }
            }
        }
        
        // Calculate percentages
        double activePercentage = totalUsers > 0 ? (double) activeCount / totalUsers * 100 : 0;
        double inactivePercentage = totalUsers > 0 ? (double) inactiveCount / totalUsers * 100 : 0;
        
        // Create grid for better alignment
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(5));
        
        // Active users row
        Label activeLabel = new Label("Active");
        activeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        ProgressBar activeBar = new ProgressBar(activePercentage / 100);
        activeBar.setPrefWidth(200);
        activeBar.setPrefHeight(15);
        activeBar.setStyle("-fx-accent: #4CAF50;");
        
        Label activeStatsLabel = new Label(String.format("%d (%.0f%%)", activeCount, activePercentage));
        activeStatsLabel.setStyle("-fx-font-size: 14px;");
        
        grid.add(activeLabel, 0, 0);
        grid.add(activeBar, 1, 0);
        grid.add(activeStatsLabel, 2, 0);
        
        // Inactive users row
        Label inactiveLabel = new Label("Inactive");
        inactiveLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        ProgressBar inactiveBar = new ProgressBar(inactivePercentage / 100);
        inactiveBar.setPrefWidth(200);
        inactiveBar.setPrefHeight(15);
        inactiveBar.setStyle("-fx-accent: #F44336;");
        
        Label inactiveStatsLabel = new Label(String.format("%d (%.0f%%)", inactiveCount, inactivePercentage));
        inactiveStatsLabel.setStyle("-fx-font-size: 14px;");
        
        grid.add(inactiveLabel, 0, 1);
        grid.add(inactiveBar, 1, 1);
        grid.add(inactiveStatsLabel, 2, 1);
        
        // Add the grid to the container
        container.getChildren().add(grid);
        
        return container;
    }
    
    // Create a list of recent user registrations
    private VBox createRecentRegistrationsList() {
        VBox container = new VBox(10);
        
        // Sort users by creation date (most recent first)
        List<User> sortedUsers = new ArrayList<>(usersList);
        sortedUsers.sort((u1, u2) -> {
            if (u1.getCreatedAt() == null && u2.getCreatedAt() == null) return 0;
            if (u1.getCreatedAt() == null) return 1;
            if (u2.getCreatedAt() == null) return -1;
            return u2.getCreatedAt().compareTo(u1.getCreatedAt());
        });
        
        // Take top 5 most recent
        int count = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        if (sortedUsers.isEmpty()) {
            Label noUsers = new Label("No users registered yet");
            noUsers.setStyle("-fx-font-size: 14px;");
            container.getChildren().add(noUsers);
        } else {
            for (User user : sortedUsers) {
                if (count >= 5) break;
                if ("ADMINISTRATEUR".equals(user.getRole().toString())) continue;
                
                HBox userItem = new HBox(15);
                userItem.setAlignment(Pos.CENTER_LEFT);
                userItem.setPadding(new Insets(8));
                
                // Date
                String date = user.getCreatedAt() != null ? 
                            user.getCreatedAt().format(formatter) : "Unknown";
                
                Label dateLabel = new Label(date);
                dateLabel.setMinWidth(100);
                dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
                
                // User info
                VBox userInfo = new VBox(3);
                HBox.setHgrow(userInfo, Priority.ALWAYS);
                
                Label nameLabel = new Label(user.getFirstName() + " " + user.getLastName());
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                
                Label emailLabel = new Label(user.getEmail());
                emailLabel.setStyle("-fx-font-style: italic; -fx-font-size: 13px;");
                
                userInfo.getChildren().addAll(nameLabel, emailLabel);
                
                // Status indicator
                String statusText = user.isVerified() ? "✓" : "✗";
                String statusColor = user.isVerified() ? "#4CAF50" : "#F44336";
                
                Label statusLabel = new Label(statusText);
                statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-weight: bold; -fx-font-size: 16px;");
                
                userItem.getChildren().addAll(dateLabel, userInfo, statusLabel);
                
                // Add separator except for the last item
                container.getChildren().add(userItem);
                if (count < Math.min(4, sortedUsers.size() - 1)) {
                    Separator separator = new Separator();
                    separator.setPadding(new Insets(5, 0, 5, 0));
                    container.getChildren().add(separator);
                }
                
                count++;
            }
        }
        
        return container;
    }
    
    @FXML
    private void showClubManagement() {
        contentTitle.setText("Club Management");
        setActiveButton(clubManagementButton);
        
        // Show placeholder for Club module
        showModulePlaceholder("Club Management");
    }
    
    @FXML
    private void showEventManagement() {
        contentTitle.setText("Event Management");
        setActiveButton(eventManagementButton);
        
        // Show placeholder for Event module
        showModulePlaceholder("Event Management");
    }
    
    @FXML
    private void showProductOrders() {
        contentTitle.setText("Products & Orders");
        setActiveButton(productOrdersButton);
        
        // Show placeholder for Products module
        showModulePlaceholder("Products & Orders");
    }
    
    @FXML
    private void showCompetition() {
        contentTitle.setText("Competition & Season");
        setActiveButton(competitionButton);
        
        // Show placeholder for Competition module
        showModulePlaceholder("Competition & Season");
    }
    
    @FXML
    private void showSurvey() {
        contentTitle.setText("Survey Management");
        setActiveButton(surveyButton);
        
        // Show placeholder for Survey module
        showModulePlaceholder("Survey Management");
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin_profile.fxml"));
            Parent root = loader.load();
            
            AdminProfileController controller = loader.getController();
            
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setTitle("Admin Profile - UNICLUBS");
            
            // Create scene without explicit dimensions
            Scene scene = new Scene(root);
            stage.setScene(scene);
            
            // Ensure the stage is maximized
            MainApp.maximizeStage(stage);
            
            stage.show();
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
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
        Parent root = loader.load();
        
        Stage stage = (Stage) (contentArea != null ? contentArea.getScene().getWindow() : 
                             (adminNameLabel != null ? adminNameLabel.getScene().getWindow() : null));
        
        if (stage != null) {
            // Adjust to login screen size
            MainApp.adjustStageSize(true);
            
            // Create scene without explicit dimensions
            stage.setScene(new Scene(root));
            stage.setTitle("Login - UNICLUBS");
            stage.show();
        } else {
            // If we can't get the stage from the UI elements, create a new one
            stage = new Stage();
            stage.setTitle("Login - UNICLUBS");
            
            // Create scene with login dimensions
            Scene scene = new Scene(root, 600, 550);
            stage.setScene(scene);
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
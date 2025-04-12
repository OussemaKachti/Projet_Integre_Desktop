package controllers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import entities.User;
import enums.RoleEnum;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
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
    private TableColumn<User, Boolean> verifiedColumn;
    
    @FXML
    private TableColumn<User, Void> actionsColumn;
    
    @FXML
    private Pagination usersPagination;
    
    private final AuthService authService = new AuthService();
    private final UserService userService = new UserService();
    private User currentUser;
    private ObservableList<User> usersList = FXCollections.observableArrayList();
    private FilteredList<User> filteredUsers;
    
    // Pagination Constants
    private static final int ROWS_PER_PAGE = 10;
    
    @FXML
    private void initialize() {
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
    }
    
    private void setupUserManagementView() {
        // Set column value factories
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        
        // Keep UNCONSTRAINED_RESIZE_POLICY to make sure column widths are respected
        usersTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // Custom cell factories for formatted display
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));
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
        
        verifiedColumn.setCellValueFactory(cellData -> new SimpleBooleanProperty(cellData.getValue().isVerified()));
        verifiedColumn.setCellFactory(column -> new TableCell<User, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(item ? "✓" : "✗");
                    setTextFill(item ? Color.GREEN : Color.RED);
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
                private final Button activateButton = new Button("Activate");
                private final Button disableButton = new Button("Disable");
                private final Button detailsButton = new Button("Details");
                private final Button deleteButton = new Button("Delete");
                private final HBox pane = new HBox(10); // Increased spacing between buttons
                
                {
                    // Configure buttons with wider widths
                    activateButton.getStyleClass().add("button-secondary");
                    activateButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 12px;");
                    activateButton.setPrefWidth(90);
                    
                    disableButton.getStyleClass().add("button-secondary");
                    disableButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 12px;");
                    disableButton.setPrefWidth(90);
                    
                    detailsButton.getStyleClass().add("button-secondary");
                    detailsButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 12px;");
                    detailsButton.setPrefWidth(90);
                    
                    deleteButton.getStyleClass().add("button-secondary");
                    deleteButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-size: 12px;");
                    deleteButton.setPrefWidth(90);
                    
                    // Set action handlers
                    activateButton.setOnAction(event -> {
                        User user = getTableView().getItems().get(getIndex());
                        toggleUserStatus(user, true);
                    });
                    
                    disableButton.setOnAction(event -> {
                        User user = getTableView().getItems().get(getIndex());
                        toggleUserStatus(user, false);
                    });
                    
                    detailsButton.setOnAction(event -> {
                        User user = getTableView().getItems().get(getIndex());
                        showUserDetails(user);
                    });
                    
                    deleteButton.setOnAction(event -> {
                        User user = getTableView().getItems().get(getIndex());
                        deleteUser(user);
                    });
                    
                    // Configure HBox 
                    pane.setAlignment(Pos.CENTER);
                    pane.setPadding(new Insets(0, 0, 0, 0)); // Left padding
                }
                
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty) {
                        setGraphic(null);
                    } else {
                        try {
                            User user = getTableView().getItems().get(getIndex());
                            
                            // Check if user is an admin (no actions allowed)
                            boolean isAdmin = "ADMINISTRATEUR".equals(user.getRole().toString());
                            
                            if (isAdmin) {
                                setGraphic(null);
                                return;
                            }
                            
                            // Clear previous buttons
                            pane.getChildren().clear();
                            
                            // Show appropriate buttons based on user status
                            if ("active".equalsIgnoreCase(user.getStatus())) {
                                pane.getChildren().addAll(disableButton, detailsButton, deleteButton);
                            } else {
                                pane.getChildren().addAll(activateButton, detailsButton, deleteButton);
                            }
                            
                            setGraphic(pane);
                        } catch (IndexOutOfBoundsException e) {
                            // Handle the case where getIndex() might be out of bounds
                            setGraphic(null);
                        }
                    }
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
                user.setStatus(newStatus);
                userService.modifier(user);
                
                // Refresh the table
                loadUserData();
                
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
            toggleUserStatus(user, !"active".equalsIgnoreCase(user.getStatus()));
            // Return to user list after toggling
            showUserManagement();
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
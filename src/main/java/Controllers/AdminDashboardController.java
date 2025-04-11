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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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
    private Label clubPresidentsLabel;
    
    @FXML
    private TextField searchField;
    
    @FXML
    private ScrollPane userManagementView;
    
    @FXML
    private Button createUserButton;
    
    @FXML
    private Button activateDeactivateButton;
    
    @FXML
    private Button deleteUserButton;
    
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
    private TableColumn<User, Integer> warningCountColumn;
    
    @FXML
    private TableColumn<User, LocalDateTime> createdAtColumn;
    
    private final AuthService authService = new AuthService();
    private final UserService userService = new UserService();
    private User currentUser;
    private ObservableList<User> usersList = FXCollections.observableArrayList();
    private FilteredList<User> filteredUsers;
    
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
        // Setup table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        
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
        
        // Setup warning count column
        warningCountColumn.setCellValueFactory(new PropertyValueFactory<>("warningCount"));
        warningCountColumn.setCellFactory(column -> new TableCell<User, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(item));
                    
                    // Highlight high warning counts
                    if (item > 3) {
                        setTextFill(Color.RED);
                        setStyle("-fx-font-weight: bold;");
                    } else if (item > 0) {
                        setTextFill(Color.ORANGE);
                    } else {
                        setTextFill(Color.GREEN);
                        setText("0");
                    }
                }
            }
        });
        
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        createdAtColumn.setCellFactory(column -> new TableCell<User, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(formatter.format(item));
                }
            }
        });
        
        // Set selection listener to enable/disable buttons
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            boolean isAdmin = hasSelection && "ADMINISTRATEUR".equals(newSelection.getRole().toString());
            
            // Don't allow actions on admin users
            activateDeactivateButton.setDisable(!hasSelection || isAdmin);
            deleteUserButton.setDisable(!hasSelection || isAdmin);
        });
        
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
            }
        });
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
            usersTable.setItems(filteredUsers);
            
            // Update statistics
            int totalUsers = allUsers.size() - countAdminUsers(allUsers);
            int activeUsers = countActiveUsers(allUsers);
            int unverifiedUsers = countUnverifiedUsers(allUsers);
            int clubPresidents = countClubPresidents(allUsers);
            
            totalUsersLabel.setText(String.valueOf(totalUsers));
            activeUsersLabel.setText(String.valueOf(activeUsers));
            unverifiedUsersLabel.setText(String.valueOf(unverifiedUsers));
            clubPresidentsLabel.setText(String.valueOf(clubPresidents));
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Data Loading Error", "Failed to load user data: " + e.getMessage());
        }
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
    
    private int countClubPresidents(List<User> users) {
        int count = 0;
        for (User user : users) {
            if ("PRESIDENT_CLUB".equals(user.getRole().toString())) {
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
    private void toggleUserStatus() {
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) return;
        
        String newStatus = "active".equalsIgnoreCase(selectedUser.getStatus()) ? "inactive" : "active";
        String actionText = "active".equalsIgnoreCase(newStatus) ? "activate" : "deactivate";
        
        Alert confirmDialog = new Alert(AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Status Change");
        confirmDialog.setHeaderText("Change User Status");
        confirmDialog.setContentText("Are you sure you want to " + actionText + " user " + 
                                    selectedUser.getFirstName() + " " + selectedUser.getLastName() + "?");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                selectedUser.setStatus(newStatus);
                userService.modifier(selectedUser);
                
                // Refresh the table
                loadUserData();
                
                showAlert("Success", "Status Updated", 
                         "User " + selectedUser.getFirstName() + " " + selectedUser.getLastName() + 
                         " has been " + actionText + "d.");
                
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Update Failed", 
                         "Failed to update user status: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void deleteSelectedUser() {
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) return;
        
        Alert confirmDialog = new Alert(AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Deletion");
        confirmDialog.setHeaderText("Delete User");
        confirmDialog.setContentText("Are you sure you want to delete user " + 
                                    selectedUser.getFirstName() + " " + selectedUser.getLastName() + "?\n\n" +
                                    "This action cannot be undone.");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.supprimer(selectedUser);
                
                // Refresh the table
                loadUserData();
                
                showAlert("Success", "User Deleted", 
                         "User " + selectedUser.getFirstName() + " " + selectedUser.getLastName() + 
                         " has been deleted successfully.");
                
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Deletion Failed", 
                         "Failed to delete user: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void showUserManagement() {
        contentTitle.setText("User Management");
        setActiveButton(userManagementButton);
        
        // Ensure user management view is visible
        contentStackPane.getChildren().clear();
        contentStackPane.getChildren().add(userManagementView);
        
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
            
            // Replace the content
            contentStackPane.getChildren().clear();
            contentStackPane.getChildren().add(placeholder);
            
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
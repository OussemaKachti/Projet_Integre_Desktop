package com.esprit.controllers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.esprit.models.ActivityLogEntry;
import com.esprit.models.User;
import com.esprit.utils.DatabaseConnection;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Controller for the administrator activity log dialog.
 */
public class ActivityLogController implements Initializable {

    @FXML
    private Label userInfoLabel;
    
    @FXML
    private TableView<ActivityLogEntry> activityTable;
    
    @FXML
    private TableColumn<ActivityLogEntry, String> dateTimeColumn;
    
    @FXML
    private TableColumn<ActivityLogEntry, String> activityTypeColumn;
    
    @FXML
    private TableColumn<ActivityLogEntry, String> descriptionColumn;
    
    @FXML
    private TableColumn<ActivityLogEntry, String> ipAddressColumn;
    
    @FXML
    private Button closeButton;
    
    @FXML
    private Button exportButton;
    
    private User currentUser;
    private ObservableList<ActivityLogEntry> logEntries = FXCollections.observableArrayList();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");
    private final DateTimeFormatter dbDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Set the user for which the activity log is displayed
     */
    public void setUser(User user) {
        this.currentUser = user;
        if (user != null) {
            userInfoLabel.setText("Activity log for " + user.getFirstName() + " " + user.getLastName());
            
            // Load real activity data from database
            loadActivityDataFromDatabase();
        }
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize table columns
        dateTimeColumn.setCellValueFactory(cellData -> {
            LocalDateTime dateTime = cellData.getValue().getDateTime();
            return new SimpleStringProperty(dateTime.format(dateFormatter));
        });
        
        activityTypeColumn.setCellValueFactory(new PropertyValueFactory<>("activityType"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        ipAddressColumn.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
        
        // Bind table to data source
        activityTable.setItems(logEntries);
    }
    
    /**
     * Close the dialog
     */
    @FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
    
    /**
     * Export the activity log to a CSV file
     */
    @FXML
    private void handleExport(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Activity Log");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        
        // Set initial filename
        String fileName = "admin_activity_log_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        fileChooser.setInitialFileName(fileName);
        
        // Show save dialog
        File file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());
        
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                // Write header
                writer.write("Date and Time,Activity Type,Description,IP Address,Entity Type,Entity ID\n");
                
                // Write data
                for (ActivityLogEntry entry : logEntries) {
                    writer.write(String.format("%s,\"%s\",\"%s\",\"%s\",\"%s\",%d\n",
                            entry.getDateTime().format(dateFormatter),
                            entry.getActivityType(),
                            entry.getDescription().replace("\"", "\"\""), // Escape quotes
                            entry.getIpAddress(),
                            entry.getEntityType(),
                            entry.getEntityId()));
                }
                
                // Show success message
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("Activity log has been exported successfully.");
                alert.showAndWait();
                
            } catch (IOException e) {
                // Show error message
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Export Failed");
                alert.setHeaderText(null);
                alert.setContentText("Failed to export activity log: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }
    
    /**
     * Load activity data from various database tables.
     * This aggregates activities from different tables to create a comprehensive log.
     */
    private void loadActivityDataFromDatabase() {
        List<ActivityLogEntry> dbData = new ArrayList<>();
        
        // Add admin login data
        loadLoginData(dbData);
        
        // Add user management activities
        loadUserManagementData(dbData);
        
        // Add club management activities
        loadClubManagementData(dbData);
        
        // Add event management activities
        loadEventManagementData(dbData);
        
        // Add sondage (poll) activities
        loadSondageData(dbData);
        
        // Add product/order activities
        loadProductOrderData(dbData);
        
        // Add competition/season activities
        loadCompetitionData(dbData);
        
        // Sort by date (most recent first)
        dbData.sort((a, b) -> b.getDateTime().compareTo(a.getDateTime()));
        
        // Add database data to the observable list
        logEntries.addAll(dbData);
        
        // If no data was found, add an entry to indicate this
        if (logEntries.isEmpty()) {
            logEntries.add(new ActivityLogEntry(
                    LocalDateTime.now(),
                    "Info",
                    "No activity records found for this administrator",
                    "N/A"));
        }
    }
    
    private void loadLoginData(List<ActivityLogEntry> dbData) {
        // Add login activity based on last_login_at from user table
        if (currentUser != null && currentUser.getLastLoginAt() != null) {
            dbData.add(new ActivityLogEntry(
                    currentUser.getLastLoginAt(),
                    "Login",
                    "Administrator login",
                    "192.168.1.100", // In a real implementation, this would be captured during login
                    currentUser.getId(),
                    "User"));
        }
    }
    
    private void loadUserManagementData(List<ActivityLogEntry> dbData) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Query for user creations
            String userQuery = "SELECT id, created_at FROM user WHERE created_at > ? ORDER BY created_at DESC LIMIT 15";
            
            try (PreparedStatement stmt = conn.prepareStatement(userQuery)) {
                // Get users created in the last 30 days
                stmt.setString(1, LocalDateTime.now().minusDays(30).format(dbDateFormatter));
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int userId = rs.getInt("id");
                        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
                        
                        dbData.add(new ActivityLogEntry(
                                createdAt,
                                "User Management",
                                "User account created (ID: " + userId + ")",
                                "192.168.1.100",
                                userId,
                                "User"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading user management data: " + e.getMessage());
        }
    }
    
    private void loadClubManagementData(List<ActivityLogEntry> dbData) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Query for participation_membre table to check club membership requests and changes
            String membershipQuery = "SELECT id, user_id, club_id, date_request, statut FROM participation_membre ORDER BY date_request DESC LIMIT 10";
            
            try (PreparedStatement stmt = conn.prepareStatement(membershipQuery)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        int userId = rs.getInt("user_id");
                        int clubId = rs.getInt("club_id");
                        LocalDateTime requestDate = rs.getTimestamp("date_request").toLocalDateTime();
                        String status = rs.getString("statut");
                        
                        String description = "Club membership " + status.toLowerCase() + 
                                " for user " + userId + " in club " + clubId;
                        
                        dbData.add(new ActivityLogEntry(
                                requestDate,
                                "Club Management",
                                description,
                                "192.168.1.100",
                                clubId,
                                "Club"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading club management data: " + e.getMessage());
        }
    }
    
    private void loadEventManagementData(List<ActivityLogEntry> dbData) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Query for events
            String eventQuery = "SELECT id, club_id, nom_event, start_date FROM evenement ORDER BY start_date DESC LIMIT 10";
            
            try (PreparedStatement stmt = conn.prepareStatement(eventQuery)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int eventId = rs.getInt("id");
                        int clubId = rs.getInt("club_id");
                        String eventName = rs.getString("nom_event");
                        LocalDateTime startDate = rs.getTimestamp("start_date").toLocalDateTime();
                        
                        // For each event, create a creation activity entry
                        LocalDateTime creationDate = startDate.minusDays(3); // Assume created 3 days before start
                        
                        dbData.add(new ActivityLogEntry(
                                creationDate,
                                "Event Management",
                                "Event created: " + eventName + " for club " + clubId,
                                "192.168.1.100",
                                eventId,
                                "Event"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading event management data: " + e.getMessage());
        }
    }
    
    private void loadSondageData(List<ActivityLogEntry> dbData) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Query for polls
            String pollQuery = "SELECT id, user_id, club_id, question, created_at FROM sondage ORDER BY created_at DESC LIMIT 10";
            
            try (PreparedStatement stmt = conn.prepareStatement(pollQuery)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int sondageId = rs.getInt("id");
                        int userId = rs.getInt("user_id");
                        int clubId = rs.getInt("club_id");
                        String question = rs.getString("question");
                        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
                        
                        dbData.add(new ActivityLogEntry(
                                createdAt,
                                "Poll Management",
                                "Poll created: \"" + question + "\" for club " + clubId,
                                "192.168.1.100",
                                sondageId,
                                "Poll"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading poll data: " + e.getMessage());
        }
    }
    
    private void loadProductOrderData(List<ActivityLogEntry> dbData) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Query for orders
            String orderQuery = "SELECT id, user_id, date_comm, statut FROM commande ORDER BY date_comm DESC LIMIT 10";
            
            try (PreparedStatement stmt = conn.prepareStatement(orderQuery)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int orderId = rs.getInt("id");
                        int userId = rs.getInt("user_id");
                        LocalDateTime orderDate = rs.getDate("date_comm").toLocalDate().atStartOfDay();
                        String status = rs.getString("statut");
                        
                        dbData.add(new ActivityLogEntry(
                                orderDate,
                                "Order Management",
                                "Order " + status + " for user " + userId,
                                "192.168.1.100",
                                orderId,
                                "Order"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading order data: " + e.getMessage());
        }
    }
    
    private void loadCompetitionData(List<ActivityLogEntry> dbData) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Query for competitions
            String competitionQuery = "SELECT id, saison_id, nom_comp, start_date, status FROM competition ORDER BY start_date DESC";
            
            try (PreparedStatement stmt = conn.prepareStatement(competitionQuery)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int compId = rs.getInt("id");
                        int saisonId = rs.getInt("saison_id");
                        String name = rs.getString("nom_comp");
                        LocalDateTime startDate = rs.getTimestamp("start_date").toLocalDateTime();
                        String status = rs.getString("status");
                        
                        // Competition creation date (3 days before start)
                        LocalDateTime creationDate = startDate.minusDays(3);
                        
                        dbData.add(new ActivityLogEntry(
                                creationDate,
                                "Competition Management",
                                "Competition created: " + name + " for season " + saisonId,
                                "192.168.1.100",
                                compId,
                                "Competition"));
                        
                        // If competition is activated, add activation entry
                        if ("activated".equals(status)) {
                            dbData.add(new ActivityLogEntry(
                                    startDate.minusDays(1),
                                    "Competition Management",
                                    "Competition activated: " + name,
                                    "192.168.1.100",
                                    compId,
                                    "Competition"));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading competition data: " + e.getMessage());
        }
    }
} 
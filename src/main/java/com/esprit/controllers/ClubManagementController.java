<<<<<<< HEAD
package com.esprit.controllers;
=======
/*package com.esprit.controllers;
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87

import com.esprit.models.Club;
import com.esprit.models.User;
import com.esprit.services.ClubService;
import com.esprit.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

public class ClubManagementController {
    private final ClubService clubService = new ClubService();
    private User currentUser;

    @FXML
    public void initialize() {
        try {
            // Get the logged-in user from SessionManager
            currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                showCustomAlert("Error", "No user is currently logged in.", "error");
                return;
            }

            // Get the club associated with the current user (president)
            Club userClub = clubService.findFirstByPresident(currentUser.getId());
            if (userClub == null) {
                showCustomAlert("Error", "You must be a club president to manage club details.", "error");
                return;
            }

            // Load club details
            // ... existing code ...
        } catch (Exception e) {
            showCustomAlert("Error", "An error occurred: " + e.getMessage(), "error");
        }
    }

    @FXML
    private void handleUpdateClub() {
        try {
            // Get the club associated with the current user (president)
            Club userClub = clubService.findFirstByPresident(currentUser.getId());
            if (userClub == null) {
                showCustomAlert("Error", "You must be a club president to update club details.", "error");
                return;
            }

            // Update club details
            // ... existing code ...
        } catch (Exception e) {
            showCustomAlert("Error", "An error occurred: " + e.getMessage(), "error");
        }
    }

    private void showCustomAlert(String title, String message, String type) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Set the alert type
        ButtonType buttonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().add(buttonType);
        
        // Apply custom styling
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/com/esprit/styles/alert-style.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-alert");
        
        // Add specific style class based on alert type
        switch (type.toLowerCase()) {
            case "success":
                dialogPane.getStyleClass().add("custom-alert-success");
                break;
            case "warning":
                dialogPane.getStyleClass().add("custom-alert-warning");
                break;
            case "error":
                dialogPane.getStyleClass().add("custom-alert-error");
                break;
        }
        
        // Style the button
        Button okButton = (Button) dialogPane.lookupButton(buttonType);
        okButton.getStyleClass().add("custom-alert-button");
        
        alert.showAndWait();
    }
<<<<<<< HEAD
} 
=======
} */
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87

package com.esprit.controllers;

import com.esprit.MainApp;
import com.esprit.models.User;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import com.esprit.models.Evenement;
import com.esprit.models.Participation_event;
import com.esprit.services.ServiceEvent;
import com.esprit.services.ServiceParticipation;
import com.esprit.services.ServiceWeather;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.io.IOException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;
import org.json.simple.JSONObject;
import com.esprit.utils.SessionManager;

public class DetailsEvent implements Initializable {

    @FXML
    private Label eventTitleLabel;
    @FXML
    private Label userNameLabel;
    @FXML
    private VBox adminActionsContainer;

    @FXML
    private StackPane userProfileContainer;
    @FXML
    private Label eventTypeLabel;
    @FXML
    private Label eventCategoryLabel;
    @FXML
    private Label eventDescriptionLabel;
    @FXML
    private Label clubNameLabel;
    @FXML
    private Label startDateLabel;
    @FXML
    private Label endDateLabel;
    @FXML
    private VBox profileDropdown;
    @FXML
    private Label locationLabel;

    // Weather components
    @FXML
    private Label weatherTempLabel;
    @FXML
    private Label weatherDescLabel;
    @FXML
    private ImageView weatherIconView;
    @FXML
    private Label weatherTitleLabel;
    @FXML
    private ImageView userProfilePic;

    @FXML
    private ImageView eventImageView;

    @FXML
    private StackPane clubsContainer;

    @FXML
    private Button backButton;
    @FXML
    private Button registerButton;
    @FXML
    private Button editButton;

    @FXML
    private Button presidentButton1; // Delete button

    @FXML
    private Button presidentButton3; // View Participants button
    @FXML
    private Button qrCodeButton;      // Generate QR code for participants
    @FXML
    private Button scanQrCodeButton;  // Scan QR code for organizers

    private Evenement currentEvent;
    private ServiceEvent serviceEvent = new ServiceEvent();
    private ServiceParticipation serviceParticipation = new ServiceParticipation();
    private ServiceWeather serviceWeather = new ServiceWeather(); // Weather service
    private User currentUser;

    // Changed from long to Long to match the required type
    private Long currentUserId = 1L; // Replace with the connected user's ID

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Make the participation button visible
        registerButton.setVisible(true);
        // Hide admin features by default until we check permissions
        editButton.setVisible(false);
        presidentButton1.setVisible(false); // Delete button
        presidentButton3.setVisible(false); // View participants button
        scanQrCodeButton.setVisible(false);
        initializeUserProfile();
        setupUserPermissions();
        // By default, hide weather components until data is loaded
        if (weatherTitleLabel != null) {
            weatherTitleLabel.setVisible(false);
        }
        if (weatherTempLabel != null) {
            weatherTempLabel.setVisible(false);
        }
        if (weatherDescLabel != null) {
            weatherDescLabel.setVisible(false);
        }
        if (weatherIconView != null) {
            weatherIconView.setVisible(false);
        }

        // Configure event handlers
        presidentButton1.setOnAction(event -> handleDelete());
        registerButton.setOnAction(event -> handleRegister());
        //presidentButton3.setOnAction(event -> handleViewParticipants());

        if (qrCodeButton != null) {
            qrCodeButton.setOnAction(event -> handleQRCode());
        }

        if (scanQrCodeButton != null) {
            scanQrCodeButton.setOnAction(event -> handleScanQRCode());
        }

    }
    /**
     * Configure les permissions d'interface utilisateur basées sur le rôle de l'utilisateur
     */
 // Add this field to reference your admin VBox

    private void setupUserPermissions() {
        SessionManager sessionManager = SessionManager.getInstance();

        // Check user roles
        boolean isPresident = sessionManager.hasRole("PRESIDENT_CLUB");
        boolean isAdmin = sessionManager.hasRole("ADMINISTRATEUR");

        // Determine which users can manage events
        boolean canManageEvents = isPresident || isAdmin;

        // Buttons visible only for presidents and administrators
        editButton.setVisible(canManageEvents);
        presidentButton1.setVisible(canManageEvents); // Delete button
        presidentButton3.setVisible(canManageEvents); // View participants button
        scanQrCodeButton.setVisible(canManageEvents); // Scan QR code

        // Hide the entire admin VBox container if user has no admin permissions
        adminActionsContainer.setVisible(canManageEvents);
        adminActionsContainer.setManaged(canManageEvents); // This prevents the empty space from showing

        // QR code button is visible for all registered participants
        qrCodeButton.setVisible(true);

        // Register button is visible for all logged-in users
        registerButton.setVisible(true);
    }

    private void initializeUserProfile() {
        // Get current user from session
        currentUser = SessionManager.getInstance().getCurrentUser();

        if (currentUser != null) {
            // Set user name
            userNameLabel.setText(currentUser.getFirstName() + " " + currentUser.getLastName());

            // Load profile picture
            String profilePicture = currentUser.getProfilePicture();
            if (profilePicture != null && !profilePicture.isEmpty()) {
                try {
                    File imageFile = new File("uploads/profiles/" + profilePicture);
                    if (imageFile.exists()) {
                        Image image = new Image(imageFile.toURI().toString());
                        userProfilePic.setImage(image);
                    } else {
                        loadDefaultProfilePic();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    loadDefaultProfilePic();
                }
            } else {
                loadDefaultProfilePic();
            }

            // Apply circular clip to profile picture
            double radius = 22.5; // Match the style
            userProfilePic.setClip(new javafx.scene.shape.Circle(radius, radius, radius));

            // Initially hide the dropdown
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
        }
    }

    /**
     * Load default profile picture
     * Added method from HomeController integration
     */
    private void loadDefaultProfilePic() {
        try {
            Image defaultImage = new Image(getClass().getResourceAsStream("/com/esprit/images/default-profile.png"));
            userProfilePic.setImage(defaultImage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads event data into the view
     * @param event The event to display
     */
    public void setEventData(Evenement event) {
        this.currentEvent = event;

        // Set basic event information
        eventTitleLabel.setText(event.getNom_event());
        eventTypeLabel.setText(event.getType());
        eventDescriptionLabel.setText(event.getDesc_event());
        locationLabel.setText(event.getLieux());

        // Format dates
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy HH:mm");
        startDateLabel.setText("From: " + dateFormat.format(event.getStart_date()));
        endDateLabel.setText("To: " + dateFormat.format(event.getEnd_date()));

        // Get and set club name
        String clubName = serviceEvent.getClubNameById(event.getClub_id());
        clubNameLabel.setText(clubName);

        // Get and set category name
        String categoryName = serviceEvent.getCategoryNameById(event.getCategorie_id());
        eventCategoryLabel.setText(categoryName);

        // Load event image if available
        loadEventImage(event.getImage_description());

        // Check if the user is already participating in this event
        updateRegisterButtonStatus();
        setupUserPermissions();

        // Load weather information for the event location and date
        loadWeatherData(event.getLieux(), event.getStart_date());
    }

    /**
     * Loads weather data for the event location and date
     * @param location Event location
     * @param eventDate Event date
     */
    private void loadWeatherData(String location, Date eventDate) {
        try {
            // Check if weather UI components exist
            if (weatherTitleLabel == null || weatherTempLabel == null ||
                    weatherDescLabel == null || weatherIconView == null) {
                System.err.println("Weather display components are not available in the FXML");
                return;
            }

            // Get weather forecast for the event location and date
            JSONObject weatherData = serviceWeather.getWeatherForecast(location, eventDate);

            if (weatherData != null) {
                // Display weather section title
                weatherTitleLabel.setText("Weather Forecast for Event");
                weatherTitleLabel.setVisible(true);

                // Extract and display temperature
                double temperature = serviceWeather.getTemperature(weatherData);
                if (!Double.isNaN(temperature)) {
                    weatherTempLabel.setText(String.format("%.1f°C", temperature));
                    weatherTempLabel.setVisible(true);
                } else {
                    weatherTempLabel.setText("Temperature not available");
                    weatherTempLabel.setVisible(true);
                }

                // Extract and display weather description
                String weatherDesc = serviceWeather.getWeatherDescription(weatherData);
                weatherDescLabel.setText(weatherDesc);
                weatherDescLabel.setVisible(true);

                // Load weather icon
                String iconCode = serviceWeather.getWeatherIcon(weatherData);
                loadWeatherIcon(iconCode);
            } else {
                // Display message if weather data is not available
                weatherTitleLabel.setText("Weather forecast not available");
                weatherTitleLabel.setVisible(true);
                weatherTempLabel.setVisible(false);
                weatherDescLabel.setVisible(false);
                weatherIconView.setVisible(false);
            }
        } catch (Exception e) {
            System.err.println("Error loading weather data: " + e.getMessage());
            e.printStackTrace();
            // Hide weather components in case of error
            if (weatherTitleLabel != null) weatherTitleLabel.setText("Weather data unavailable");
            if (weatherTempLabel != null) weatherTempLabel.setVisible(false);
            if (weatherDescLabel != null) weatherDescLabel.setVisible(false);
            if (weatherIconView != null) weatherIconView.setVisible(false);
        }
    }

    /**
     * Loads the weather icon corresponding to the code provided by the API
     * @param iconCode Weather icon code
     */
    /**
     * Loads the weather icon corresponding to the code provided by the API
     * @param iconCode Weather icon code
     */
    private void loadWeatherIcon(String iconCode) {
        try {
            // Construction cohérente du chemin vers l'icône météo
            String iconPath = "/com/esprit/weather/icon/" + iconCode + ".png";
            Image iconImage = null;

            try {
                // Tentative de chargement depuis les resources
                iconImage = new Image(getClass().getResourceAsStream(iconPath));
                System.out.println("Tentative de chargement de l'icône: " + iconPath);

                if (iconImage == null || iconImage.isError()) {
                    System.err.println("Échec du premier chargement d'icône, essai avec un chemin alternatif");

                    // Alternative: essai avec un autre format de chemin
                    iconPath = "/resources" + iconPath;
                    iconImage = new Image(getClass().getResourceAsStream(iconPath));
                    System.out.println("Tentative avec le chemin alternatif: " + iconPath);
                }
            } catch (Exception e) {
                System.err.println("Impossible de charger l'icône depuis les ressources: " + e.getMessage());

                // Tentative de chargement depuis un chemin local
                File iconFile = new File("resources/com/esprit/weather/icon/" + iconCode + ".png");
                System.out.println("Tentative avec le chemin local: " + iconFile.getAbsolutePath());

                if (iconFile.exists()) {
                    iconImage = new Image(iconFile.toURI().toString());
                } else {
                    // Recherche avec un autre chemin si le premier échoue
                    iconFile = new File("src/resources/com/esprit/weather/icon/" + iconCode + ".png");
                    System.out.println("Tentative avec le chemin local alternatif: " + iconFile.getAbsolutePath());

                    if (iconFile.exists()) {
                        iconImage = new Image(iconFile.toURI().toString());
                    } else {
                        // Utilisation d'une icône par défaut si l'icône spécifique n'est pas trouvée
                        System.out.println("Utilisation de l'icône par défaut");
                        iconImage = new Image(getClass().getResourceAsStream("/com/esprit/weather/icon/01d.png"));

                        // Si l'icône par défaut n'est pas trouvée, essai avec un autre chemin
                        if (iconImage == null || iconImage.isError()) {
                            iconImage = new Image(getClass().getResourceAsStream("/resources/com/esprit/weather/icon/01d.png"));
                        }
                    }
                }
            }

            if (iconImage != null && !iconImage.isError()) {
                weatherIconView.setImage(iconImage);
                weatherIconView.setVisible(true);
                System.out.println("Icône météo chargée avec succès");
            } else {
                System.err.println("Échec du chargement de l'icône météo");
                weatherIconView.setVisible(false);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'icône météo: " + e.getMessage());
            e.printStackTrace();
            weatherIconView.setVisible(false);
        }
    }

    @FXML
    private void handleQRCode() {
        if (currentEvent == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No event selected",
                    "Cannot generate QR code because no event is selected.");
            return;
        }

        try {
            // Check if the user is registered first
            boolean isRegistered = serviceParticipation.participationExists(currentUserId, currentEvent.getId());

            if (!isRegistered) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Registration Required");
                alert.setHeaderText("Registration Needed");
                alert.setContentText("You must register for the event before you can generate a confirmation QR code.");
                alert.showAndWait();
                return;
            }

            // Load QR code generation view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/QRConfirmation.fxml"));
            Parent root = loader.load();

            // Get controller and set data
            QRConfirmation controller = loader.getController();
            controller.setData(currentEvent, currentUserId);

            // Create new stage for QR code window
            Stage stage = new Stage();
            stage.setTitle("Participation QR Code - " + currentEvent.getNom_event());
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Loading Error",
                    "Unable to load QR code generation screen: " + e.getMessage());
        }
    }

    @FXML
    private void handleScanQRCode() {
        if (currentEvent == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No event selected",
                    "Cannot scan QR code because no event is selected.");
            return;
        }

        try {
            // Check if current user is organizer/has permission
            // For demo purposes we'll skip this check
            // In a real app, you'd verify the user has permission to scan QR codes

            // Load QR scanner view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/QRScanner.fxml"));
            Parent root = loader.load();

            // Get controller and set data
            QRScanner controller = loader.getController();
            controller.setEvent(currentEvent);

            // Create new stage for scanner window
            Stage stage = new Stage();
            stage.setTitle("Scan QR Code - " + currentEvent.getNom_event());
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Loading Error",
                    "Unable to load QR code scanning screen: " + e.getMessage());
        }
    }
    /**
     * Updates the registration button status based on user participation
     */
    private void updateRegisterButtonStatus() {
        if (currentEvent == null) return;

        boolean isAlreadyRegistered = serviceParticipation.participationExists(currentUserId, currentEvent.getId());

        if (isAlreadyRegistered) {
            registerButton.setText("✓ Cancel Registration");
            registerButton.setStyle("-fx-background-color: linear-gradient(to right, #e53935, #f44336); -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 15 20; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 2);");
        } else {
            registerButton.setText("✓ Register for Event");
            registerButton.setStyle("-fx-background-color: linear-gradient(to right, #1976d2, #2979ff); -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 15 20; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 2);");
        }
    }

    /**
     * Handles event registration/deregistration
     */



    @FXML
    private void handleRegister() {
        if (currentEvent == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No event selected",
                    "Unable to register for the event because no event is selected.");
            return;
        }

        // Récupération de l'utilisateur connecté depuis le SessionManager
        User currentUser = SessionManager.getInstance().getCurrentUser();

        // Vérifier si un utilisateur est connecté
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Not logged in",
                    "You must be logged in to register for an event.");
            return;
        }

        // Utiliser l'ID de l'utilisateur récupéré du SessionManager et le stocker dans la variable de classe
        int userId = currentUser.getId();
        this.currentUserId = Long.valueOf(userId);

        // Check if the user is already registered
        boolean isAlreadyRegistered = serviceParticipation.participationExists(currentUserId, currentEvent.getId());

        if (isAlreadyRegistered) {
            // User is already registered, offer to cancel registration
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Cancel Registration");
            confirmDialog.setHeaderText("Cancel Your Registration");
            confirmDialog.setContentText("Are you sure you want to cancel your registration for the event \"" +
                    currentEvent.getNom_event() + "\"?");

            Optional<ButtonType> result = confirmDialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Delete the participation
                boolean cancelled = serviceParticipation.annulerParticipation(currentUserId, currentEvent.getId());

                if (cancelled) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Registration Cancelled",
                            "Your registration for the event has been successfully cancelled.");
                    updateRegisterButtonStatus(); // Met à jour le bouton (devient bleu "Register for Event")
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Cancellation Failed",
                            "The cancellation of your registration failed. Please try again.");
                }
            }
        } else {
            // User is not registered, offer to register
            Date currentDate = new Date();

            // Check if the event hasn't already ended
            if (currentEvent.getEnd_date().before(currentDate)) {
                showAlert(Alert.AlertType.ERROR, "Error", "Event Ended",
                        "This event has already ended. You can no longer register for it.");
                return;
            }

            // Create a new participation
            Participation_event participation = new Participation_event();
            participation.setUser_id(currentUserId);
            participation.setEvenement_id(Long.valueOf(currentEvent.getId()));
            participation.setDateparticipation(currentDate);

            // Register the participation
            boolean registered = serviceParticipation.ajouterParticipation(participation);

            if (registered) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Registration Confirmed",
                        "You are now registered for the event.");
                updateRegisterButtonStatus(); // Met à jour le bouton (devient rouge "Cancel Registration")
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Registration Failed",
                        "Registration for the event failed. Please try again.");
            }
        }
    }

    /**
     * Handles displaying the list of participants
     */
    @FXML
    private void handleViewParticipants() {
        if (currentEvent == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No event selected",
                    "Unable to display participants because no event is selected.");
            return;
        }

        try {
            // Load the participant list view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/ParticipantsList.fxml"));
            Parent root = loader.load();

            // Pass the event to the controller
            ParticipantsListController controller = loader.getController();

            // Décommenter et utiliser la méthode initData pour passer l'événement entier
            controller.initData(currentEvent);

            System.out.println("Ouverture de la liste des participants pour l'événement: " +
                    currentEvent.getNom_event() + " (ID: " + currentEvent.getId() + ")");

            Stage stage = new Stage();
            stage.setTitle("Participants for event " + currentEvent.getNom_event());
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            System.err.println("Error loading participant list: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Loading Error",
                    "Unable to load participant list: " + e.getMessage());
        }
    }
    /**
     * Loads the event image
     * @param imagePath the image path
     */
    private void loadEventImage(String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    eventImageView.setImage(image);
                } else {
                    setDefaultEventImage();
                }
            } catch (Exception e) {
                System.err.println("Error loading event image: " + e.getMessage());
                setDefaultEventImage();
            }
        } else {
            setDefaultEventImage();
        }
    }

    /**
     * Sets a default image for the event
     */
    private void setDefaultEventImage() {
        try {
            // First try to load from resources
            Image defaultImage = new Image(getClass().getResourceAsStream("/resources/images/default_event.png"));
            eventImageView.setImage(defaultImage);
        } catch (Exception e) {
            System.err.println("Error loading default image: " + e.getMessage());

            // If loading from resources fails, try an alternative path
            try {
                Image fallbackImage = new Image("file:resources/images/default_event.png");
                eventImageView.setImage(fallbackImage);
            } catch (Exception ex) {
                System.err.println("Unable to load default image: " + ex.getMessage());
            }
        }
    }

    /**
     * Handles the Back button action
     */


    /**
     * Handles the Edit button action
     */
    @FXML
    private void handleEdit() {
        try {
            // Load the ModifierEvent FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/ModifierEvent.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the ID of the event to modify
            ModifierEvent modifierController = loader.getController();
            modifierController.setEventId(currentEvent.getId());

            // Create a new scene for the ModifierEvent view
            Stage stage = new Stage();
            stage.setTitle("Edit event");
            stage.setScene(new Scene(root));

            // Show the scene
            stage.show();

        } catch (IOException e) {
            System.err.println("Error loading the modification page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles the Delete button action
     */
    @FXML
    private void handleDelete() {
        if (currentEvent == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No event to delete",
                    "Unable to delete event because no event is selected.");
            return;
        }

        // Display a confirmation before deleting
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Confirmation");
        confirmDialog.setHeaderText("Delete Event");
        confirmDialog.setContentText("Are you sure you want to delete the event \"" +
                currentEvent.getNom_event() + "\"? This action is irreversible.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Call the service to delete the event
                boolean deleted = serviceEvent.supprimerEvenement(currentEvent.getId());

                if (deleted) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Event Deleted",
                            "The event has been successfully deleted.");

                    // Close the current window
                    Stage currentStage = (Stage) presidentButton1.getScene().getWindow();

                    try {
                        // Load the AfficherEvent page
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/AfficherEvent.fxml"));
                        Parent root = loader.load();

                        // Create a new scene with the AfficherEvent page
                        Scene scene = new Scene(root);

                        // Use the same window to display the new scene
                        currentStage.setScene(scene);
                        currentStage.setTitle("Event List");
                        currentStage.show();

                    } catch (IOException e) {
                        e.printStackTrace();
                        System.err.println("Error loading AfficherEvent page: " + e.getMessage());
                        // In case of error, simply close the current window
                        currentStage.close();
                    }

                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Deletion Failed",
                            "The deletion of the event failed. Please try again.");
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Deletion Failed",
                        "An error occurred during deletion: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void showProfileDropdown() {
        profileDropdown.setVisible(true);
        profileDropdown.setManaged(true);
    }

    /**
     * Hide profile dropdown menu
     * Added method from HomeController integration
     */
    @FXML
    private void hideProfileDropdown() {
        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);
    }

    @FXML
    private void navigateToContact() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Contact.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToClubPolls() throws IOException {
        // Test database connection before attempting to load polls view
        try {

            // Navigate to SondageView
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/SondageView.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) clubsContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            // Handle any other exceptions that might occur
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Failed to open Polls view");
            alert.setContentText("An error occurred while trying to open the Polls view: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    @FXML
    private void navigateToProfile() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Profile.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }
    @FXML
    private void handleLogout() throws IOException {
        // Clear the session
        SessionManager.getInstance().clearSession();

        // Navigate to login page
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }
    /**
     * Displays an alert dialog box
     */
    private void showAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void navigateToHome1() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Home.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToProducts() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/produit/ProduitView.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void navigateToCompetition() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("views/Competition.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) userProfileContainer.getScene().getWindow();
        stage.getScene().setRoot(root);
    }

}
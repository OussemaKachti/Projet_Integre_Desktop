package com.esprit.controllers;

import com.esprit.models.Saison;
import com.esprit.services.SaisonService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class SaisonController {

    private final SaisonService saisonService = new SaisonService();
    private Saison selectedSaison;
    private File selectedImageFile;
    private String uploadDirectory = "uploads/images/";
    private boolean isEditMode = false;

    @FXML private AnchorPane saisonListPane;
    @FXML private VBox seasonListContainer;
    @FXML private VBox seasonFormContainer;

    // Form fields
    @FXML private TextField saisonNameField;
    @FXML private TextArea saisonDescField;
    @FXML private DatePicker saisonDateField;
    @FXML private ImageView imagePreview;
    @FXML private Label selectedImageLabel;
    @FXML private Label formTitleLabel;

    // Buttons
    @FXML private Button saveButton;
    @FXML private Button addButton;
    @FXML private Button cancelButton;
    @FXML private Button closeFormButton;
    @FXML private Button refreshButton;
    @FXML private Button backButton;
    @FXML private Button chooseImageButton;

    @FXML
    public void initialize() throws SQLException {
        // Initialize directory for image uploads
        initializeUploadDirectory();

        // Load all seasons
        loadAllSeasons();

        // Set up button actions
        setupButtonActions();

        // Apply Skydash-like styling to form controls
        applyCustomStyling();
    }
    private void applyCustomStyling() {
        // Add specific styling for form elements if needed
        saisonDateField.getEditor().setStyle("-fx-background-color: #F3F4F6; -fx-border-color: #e4e9f0;");
    }

    private void setupButtonActions() {
        // Add new season button
        addButton.setOnAction(event -> showAddForm());

        // Close form button
        closeFormButton.setOnAction(event -> hideForm());

        // Cancel button
        cancelButton.setOnAction(event -> hideForm());

        // Save button
        saveButton.setOnAction(event -> {
            try {
                if (isEditMode) {
                    updateSaison();
                } else {
                    createSaison();
                }
            } catch (SQLException e) {
                showAlert(AlertType.ERROR, "Database Error", "Could not save season: " + e.getMessage());
            }
        });

        // Choose image button
        chooseImageButton.setOnAction(event -> chooseImage());

        // Refresh button
        refreshButton.setOnAction(event -> {
            try {
                loadAllSeasons();
            } catch (SQLException e) {
                showAlert(AlertType.ERROR, "Database Error", "Could not refresh seasons: " + e.getMessage());
            }
        });

        // Back button
        backButton.setOnAction(event -> goBack());
    }

    private void initializeUploadDirectory() {
        // Create the uploads directory if it doesn't exist
        try {
            Path path = Paths.get(uploadDirectory);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Error", "Could not create upload directory: " + e.getMessage());
        }
    }

    private void loadAllSeasons() throws SQLException {
        // Clear existing items
        seasonListContainer.getChildren().clear();

        // Get all seasons from service
        List<Saison> saisons = saisonService.getAll();

        if (saisons.isEmpty()) {
            Label emptyLabel = new Label("No seasons found.");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6c757d;");

            emptyLabel.getStyleClass().add("no-seasons-placeholder");
            emptyLabel.setMaxWidth(Double.MAX_VALUE);
            emptyLabel.setAlignment(Pos.CENTER);


            seasonListContainer.getChildren().add(emptyLabel);
            return;
        }

        // Create a card for each season
        for (Saison saison : saisons) {
            seasonListContainer.getChildren().add(createSeasonCard(saison));
        }
    }

    private HBox createSeasonCard(Saison saison) {
        HBox card = new HBox();
        card.getStyleClass().add("season-card");
        card.setPrefWidth(Region.USE_COMPUTED_SIZE);
        card.setPrefHeight(Region.USE_COMPUTED_SIZE);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setSpacing(15);
        card.setPadding(new Insets(15));

        // Season icon
        FontIcon icon = new FontIcon("mdi-leaf");
        icon.setIconSize(32);
        icon.getStyleClass().add("season-icon");


        // Image view for season image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(120);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(true);

        // Add border and rounded corners to image
        imageView.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e4e9f0; -fx-border-radius: 4px;");


        // Load image if available
        if (saison.getImage() != null && !saison.getImage().isEmpty()) {
            try {
                File imageFile = new File(uploadDirectory + saison.getImage());
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    imageView.setImage(image);
                } else {
                    // Set placeholder
                    FontIcon placeholder = new FontIcon("mdi-image");
                    placeholder.setIconSize(48);
                    placeholder.setIconColor(javafx.scene.paint.Color.LIGHTGRAY);

                    // Create placeholder container
                    StackPane placeholderPane = new StackPane(placeholder);
                    placeholderPane.setPrefSize(120, 80);
                    placeholderPane.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e4e9f0; -fx-border-radius: 4px;");
                }
            } catch (Exception e) {
                System.err.println("Error loading image: " + e.getMessage());
            }
        } else {
            // Set placeholder
            FontIcon placeholder = new FontIcon("mdi-image");
            placeholder.setIconSize(48);
            placeholder.setIconColor(javafx.scene.paint.Color.LIGHTGRAY);

            StackPane placeholderPane = new StackPane(placeholder);
            placeholderPane.setPrefSize(120, 80);
            placeholderPane.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e4e9f0; -fx-border-radius: 4px;");
        }

        // Season content
        VBox content = new VBox(5);
        content.setStyle("-fx-padding: 0 10 0 10;");
        content.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label titleLabel = new Label(saison.getNomSaison());
        titleLabel.getStyleClass().add("season-title");

        Label descLabel = new Label(saison.getDescSaison());
        descLabel.getStyleClass().add("season-desc");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(Double.MAX_VALUE);

        HBox dateBox = new HBox();
        dateBox.setAlignment(Pos.CENTER_LEFT);
        dateBox.setPadding(new Insets(5, 0, 0, 0));


        Label dateLabel = new Label();
        if (saison.getDateFin() != null) {
            dateLabel.setText("End Date: " + saison.getDateFin().toString());
        }
        dateLabel.getStyleClass().add("date-badge");

        dateBox.getChildren().add(dateLabel);

        content.getChildren().addAll(titleLabel, descLabel, dateBox);

        // Action buttons
        VBox actions = new VBox(10);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button editButton = new Button("Edit");
        editButton.setGraphic(new FontIcon("mdi-pencil"));

        editButton.getStyleClass().add("edit-button");
        editButton.setPadding(new Insets(8, 12, 8, 12));

        editButton.setOnAction(e -> showEditForm(saison));

        Button deleteButton = new Button("Delete");
        deleteButton.setGraphic(new FontIcon("mdi-delete"));

        deleteButton.getStyleClass().add("delete-button");
        deleteButton.setPadding(new Insets(8, 12, 8, 12));

        deleteButton.setOnAction(e -> {
            try {
                deleteSaison(saison);
            } catch (SQLException ex) {
                showAlert(AlertType.ERROR, "Error", "Could not delete season: " + ex.getMessage());
            }
        });

        actions.getChildren().addAll(editButton, deleteButton);

        // Add all components to card
        card.getChildren().addAll(icon, imageView, content, actions);

        return card;
    }

    private void showAddForm() {
        isEditMode = false;
        formTitleLabel.setText("Add Season");
        clearForm();
        showForm();
    }

    private void showEditForm(Saison saison) {
        isEditMode = true;
        formTitleLabel.setText("Edit Season");
        selectedSaison = saison;

        // Populate form fields
        saisonNameField.setText(saison.getNomSaison());
        saisonDescField.setText(saison.getDescSaison());

        if (saison.getDateFin() != null) {
            saisonDateField.setValue(LocalDate.parse(saison.getDateFin().toString()));
        } else {
            saisonDateField.setValue(null);
        }

        // Load image if available
        if (saison.getImage() != null && !saison.getImage().isEmpty()) {
            try {
                File imageFile = new File(uploadDirectory + saison.getImage());
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    imagePreview.setImage(image);
                    selectedImageLabel.setText(saison.getImage());
                }
            } catch (Exception e) {
                System.err.println("Error loading image: " + e.getMessage());
            }
        } else {
            imagePreview.setImage(null);
            selectedImageLabel.setText("No file selected");
        }

        showForm();
    }

    private void showForm() {
        seasonFormContainer.setVisible(true);
        seasonFormContainer.setManaged(true);
    }

    private void hideForm() {
        seasonFormContainer.setVisible(false);
        seasonFormContainer.setManaged(false);
        clearForm();
    }

    private void clearForm() {
        saisonNameField.clear();
        saisonDescField.clear();
        saisonDateField.setValue(null);
        selectedImageFile = null;
        imagePreview.setImage(null);
        selectedImageLabel.setText("No file selected");
        selectedSaison = null;
    }

    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(saisonListPane.getScene().getWindow());
        if (selectedFile != null) {
            selectedImageFile = selectedFile;
            selectedImageLabel.setText(selectedFile.getName());

            // Show image preview
            try {
                Image image = new Image(selectedFile.toURI().toString());
                imagePreview.setImage(image);
            } catch (Exception e) {
                showAlert(AlertType.ERROR, "Error", "Could not load image preview: " + e.getMessage());
            }
        }
    }

    private void createSaison() throws SQLException {
        // Validate form first
        if (!validateForm()) {
            return;
        }
        String saisonName = saisonNameField.getText();
        String saisonDesc = saisonDescField.getText();
        LocalDate endDate = saisonDateField.getValue();



        Saison newSaison = new Saison();
        newSaison.setNomSaison(saisonName);
        newSaison.setDescSaison(saisonDesc);

        if (endDate != null) {
            // Convert LocalDate to java.sql.Date
            newSaison.setDateFin(Date.valueOf(endDate).toLocalDate());
        }

        // Handle image upload
        if (selectedImageFile != null) {
            String fileName = saveImage(selectedImageFile);
            newSaison.setImage(fileName);
        }

        saisonService.add(newSaison);
        showAlert(AlertType.INFORMATION, "Season Created", "The season was created successfully.");
        hideForm();
        loadAllSeasons();
    }

    private void updateSaison() throws SQLException {
        if (selectedSaison == null) {
            showAlert(AlertType.WARNING, "No Season Selected", "Please select a season to update.");
            return;
        }
        // Validate form first
        if (!validateForm()) {
            return;
        }
        String saisonName = saisonNameField.getText();
        String saisonDesc = saisonDescField.getText();
        LocalDate endDate = saisonDateField.getValue();



        selectedSaison.setNomSaison(saisonName);
        selectedSaison.setDescSaison(saisonDesc);

        if (endDate != null) {
            selectedSaison.setDateFin(Date.valueOf(endDate).toLocalDate());
        } else {
            selectedSaison.setDateFin(null);
        }

        // Handle image upload
        if (selectedImageFile != null) {
            String fileName = saveImage(selectedImageFile);
            selectedSaison.setImage(fileName);
        }

        saisonService.update(selectedSaison);
        showAlert(AlertType.INFORMATION, "Season Updated", "The season was updated successfully.");
        hideForm();
        loadAllSeasons();
    }

    private void deleteSaison(Saison saison) throws SQLException {
        if (saison == null) {
            showAlert(AlertType.WARNING, "No Season Selected", "Please select a season to delete.");
            return;
        }

        // Confirm deletion
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Are you sure you want to delete the season '" + saison.getNomSaison() + "'?");

        if (confirmAlert.showAndWait().get() == ButtonType.OK) {
            // Delete the image file if it exists
            if (saison.getImage() != null && !saison.getImage().isEmpty()) {
                try {
                    Files.deleteIfExists(Paths.get(uploadDirectory + saison.getImage()));
                } catch (IOException e) {
                    System.err.println("Error deleting image file: " + e.getMessage());
                }
            }

            saisonService.delete(saison.getId());
            showAlert(AlertType.INFORMATION, "Season Deleted", "The season was deleted successfully.");
            loadAllSeasons();
        }
    }

    private String saveImage(File file) {
        String fileName = UUID.randomUUID().toString() + getFileExtension(file.getName());
        Path targetPath = Paths.get(uploadDirectory + fileName);

        try {
            Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Error", "Could not save image: " + e.getMessage());
            return null;
        }
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex);
        }
        return "";
    }
    private boolean validateForm() {
        StringBuilder errorMessage = new StringBuilder();

        // Validate saison name (required field)
        if (saisonNameField.getText() == null || saisonNameField.getText().trim().isEmpty()) {
            errorMessage.append("- Please enter a valid season name.\n");
        }

        // Validate description (optional but good to check if empty)
        if (saisonDescField.getText() == null || saisonDescField.getText().trim().isEmpty()) {
            errorMessage.append("- Please enter a season description.\n");
        }

        // Validate date (not mandatory but if provided, should be valid)
        if (saisonDateField.getValue() != null) {
            // Check if date is in the past
            if (saisonDateField.getValue().isBefore(LocalDate.now())) {
                // This is just a warning, not an error - uncomment if you want to enforce this
                // errorMessage.append("- End date is in the past.\n");
            }
        }

        // Show validation errors if any
        if (errorMessage.length() > 0) {
            showAlert(AlertType.WARNING, "Form Validation Error",
                    "Please correct the following errors:\n" + errorMessage);
            return false;
        }

        return true;
    }
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/dashboard.fxml"));
            AnchorPane previousScene = loader.load();
            saisonListPane.getChildren().setAll(previousScene);
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Error", "Could not load previous scene: " + e.getMessage());
        }
    }

    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);


        DialogPane dialogPane = alert.getDialogPane();

        // Style the dialog pane background and border
        dialogPane.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #ffffff, #f7f9fc);" +
                        "-fx-border-color: #dce3f0;" +
                        "-fx-border-width: 1px;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-padding: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 4);"
        );

        // Style the content text
        Label content = (Label) dialogPane.lookup(".content");
        if (content != null) {
            content.setStyle(
                    "-fx-font-size: 14px;" +
                            "-fx-text-fill: #4b5c7b;" +
                            "-fx-padding: 0 0 10 0;"
            );
        }


        // Style the buttons
        dialogPane.getButtonTypes().forEach(buttonType -> {
            Button button = (Button) dialogPane.lookupButton(buttonType);
            button.setStyle(
                    "-fx-background-color: #4a90e2;" +
                            "-fx-text-fill: white;" +
                            "-fx-background-radius: 8px;" +
                            "-fx-padding: 6 14 6 14;" +
                            "-fx-font-size: 13px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 2, 0, 0, 1);"
            );
        });


        alert.showAndWait();
    }

}
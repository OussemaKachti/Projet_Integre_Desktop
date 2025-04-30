package com.esprit.controllers;

import com.esprit.models.Competition;
import com.esprit.models.Saison;
import com.esprit.models.enums.GoalTypeEnum;
import com.esprit.services.CompetitionService;
import com.esprit.services.SaisonService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class CompetitionController {

    private final CompetitionService competitionService = new CompetitionService();
    private final SaisonService saisonService = new SaisonService();
    private Competition selectedCompetition;
    private boolean isEditMode = false;

    @FXML private AnchorPane competitionListPane;
    @FXML private VBox missionListContainer;
    @FXML private VBox missionFormContainer;

    // Form fields
    @FXML private TextField missionTitleField;
    @FXML private TextArea missionDescField;
    @FXML private TextField pointsField;
    @FXML private DatePicker startDateField;
    @FXML private DatePicker endDateField;
    @FXML private TextField goalValueField;
    @FXML private ComboBox<GoalTypeEnum> goalTypeComboBox;
    @FXML private ComboBox<Saison> saisonComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private Label formTitleLabel;

    // Buttons
    @FXML private Button saveButton;
    @FXML private Button addButton;
    @FXML private Button cancelButton;
    @FXML private Button closeFormButton;
    @FXML private Button refreshButton;
    @FXML private Button backButton;

    @FXML
    public void initialize() {
        try {
            // Load all competitions
            loadAllCompetitions();

            // Set up button actions
            setupButtonActions();

            // Initialize dropdown values
            initializeComboBoxes();

            // Apply custom styling
            applyCustomStyling();
        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Initialization Error", "Could not initialize competitions: " + e.getMessage());
        }
    }

    private void applyCustomStyling() {
        // Add specific styling for form elements if needed
        startDateField.getEditor().setStyle("-fx-background-color: #F3F4F6; -fx-border-color: #e4e9f0;");
        endDateField.getEditor().setStyle("-fx-background-color: #F3F4F6; -fx-border-color: #e4e9f0;");
    }

    private void setupButtonActions() {
        // Add new competition button
        addButton.setOnAction(event -> showAddForm());

        // Close form button
        closeFormButton.setOnAction(event -> hideForm());

        // Cancel button
        cancelButton.setOnAction(event -> hideForm());

        // Save button
        saveButton.setOnAction(event -> {
            try {
                if (isEditMode) {
                    updateCompetition();
                } else {
                    createCompetition();
                }
            } catch (SQLException e) {
                showAlert(AlertType.ERROR, "Database Error", "Could not save mission: " + e.getMessage());
            }
        });

        // Refresh button
        refreshButton.setOnAction(event -> {
            try {
                loadAllCompetitions();
            } catch (SQLException e) {
                showAlert(AlertType.ERROR, "Database Error", "Could not refresh missions: " + e.getMessage());
            }
        });

        // Back button
        backButton.setOnAction(event -> goBack());
    }

    private void initializeComboBoxes() throws SQLException {
        // Initialize goal type combo box
        goalTypeComboBox.getItems().addAll(GoalTypeEnum.values());

        // Set up readable names for goal types
        goalTypeComboBox.setConverter(new StringConverter<GoalTypeEnum>() {
            @Override
            public String toString(GoalTypeEnum goalType) {
                if (goalType == null) return "";
                switch (goalType) {
                    case EVENT_COUNT: return "Event Count";
                    case EVENT_LIKES: return "Event Likes";
                    case MEMBER_COUNT: return "Member Count";
                    default: return goalType.toString();
                }
            }

            @Override
            public GoalTypeEnum fromString(String string) {
                return null; // Not needed for combo box
            }
        });

        // Initialize status combo box
        statusComboBox.getItems().addAll("activated", "deactivated");

        // Initialize saison combo box
        List<Saison> saisons = saisonService.getAll();
        saisonComboBox.getItems().addAll(saisons);

        // Set up readable names for saisons
        saisonComboBox.setConverter(new StringConverter<Saison>() {
            @Override
            public String toString(Saison saison) {
                return saison == null ? "" : saison.getNomSaison();
            }

            @Override
            public Saison fromString(String string) {
                return null; // Not needed for combo box
            }
        });
    }

    private void loadAllCompetitions() throws SQLException {
        // Clear existing items
        missionListContainer.getChildren().clear();

        // Get all competitions from service
        List<Competition> competitions = competitionService.getAll();

        if (competitions.isEmpty()) {
            Label emptyLabel = new Label("No missions found.");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6c757d;");
            emptyLabel.getStyleClass().add("no-missions-placeholder");
            emptyLabel.setMaxWidth(Double.MAX_VALUE);
            emptyLabel.setAlignment(Pos.CENTER);
            missionListContainer.getChildren().add(emptyLabel);
            return;
        }

        // Create a card for each competition
        for (Competition competition : competitions) {
            missionListContainer.getChildren().add(createMissionCard(competition));
        }
    }

    private VBox createMissionCard(Competition competition) {
        VBox card = new VBox();
        card.getStyleClass().add("mission-card");
        card.setPrefWidth(Region.USE_COMPUTED_SIZE);
        card.setPrefHeight(Region.USE_COMPUTED_SIZE);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setSpacing(15);
        card.setPadding(new Insets(15));

        // Mission header with title and points
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(10);

        // Mission icon
        FontIcon icon = new FontIcon("mdi-trophy");
        icon.setIconSize(24);
        icon.setIconColor(Color.valueOf("#ffb400"));

        // Mission title
        Label titleLabel = new Label(competition.getNomComp());
        titleLabel.getStyleClass().add("mission-title");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        // Points badge
        Label pointsLabel = new Label(competition.getPoints() + " Points");
        pointsLabel.getStyleClass().add("points-badge");

        header.getChildren().addAll(icon, titleLabel, pointsLabel);

        // Mission description
        Label descLabel = new Label(competition.getDescComp());
        descLabel.getStyleClass().add("mission-desc");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(Double.MAX_VALUE);

        // Mission details section
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(15);
        detailsGrid.setVgap(8);
        detailsGrid.setPadding(new Insets(10, 0, 10, 0));

        // Goal info
        Label goalLabel = new Label("Goal:");
        goalLabel.getStyleClass().add("detail-label");
        detailsGrid.add(goalLabel, 0, 0);

        Label goalValueLabel = new Label(String.valueOf(competition.getGoalValue()));
        goalValueLabel.getStyleClass().add("detail-value");
        detailsGrid.add(goalValueLabel, 1, 0);

        // Goal Type info
        Label goalTypeLabel = new Label("Goal Type:");
        goalTypeLabel.getStyleClass().add("detail-label");
        detailsGrid.add(goalTypeLabel, 0, 1);

        Label goalTypeValueLabel = new Label(formatGoalType(competition.getGoalType()));
        goalTypeValueLabel.getStyleClass().add("detail-value");
        detailsGrid.add(goalTypeValueLabel, 1, 1);

        // Status info
        Label statusLabel = new Label("Status:");
        statusLabel.getStyleClass().add("detail-label");
        detailsGrid.add(statusLabel, 0, 2);

        HBox statusContainer = new HBox();
        statusContainer.setAlignment(Pos.CENTER_LEFT);

        Label statusValueLabel = new Label(competition.getStatus());
        statusValueLabel.getStyleClass().addAll("status-badge",
                "activated".equals(competition.getStatus()) ? "status-active" : "status-inactive");

        statusContainer.getChildren().add(statusValueLabel);
        detailsGrid.add(statusContainer, 1, 2);

        // Date info
        Label dateLabel = new Label("Dates:");
        dateLabel.getStyleClass().add("detail-label");
        detailsGrid.add(dateLabel, 0, 3);

        String dateStr = "";
        if (competition.getStartDate() != null) {
            dateStr = formatDate(competition.getStartDate());
            if (competition.getEndDate() != null) {
                dateStr += " to " + formatDate(competition.getEndDate());
            }
        }

        Label dateValueLabel = new Label(dateStr);
        dateValueLabel.getStyleClass().add("detail-value");
        detailsGrid.add(dateValueLabel, 1, 3);

        // Actions section
        HBox actions = new HBox();
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setSpacing(10);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button editButton = new Button("Edit");
        editButton.setGraphic(new FontIcon("mdi-pencil"));
        editButton.getStyleClass().add("edit-button");
        editButton.setOnAction(e -> showEditForm(competition));

        Button deleteButton = new Button("Delete");
        deleteButton.setGraphic(new FontIcon("mdi-delete"));
        deleteButton.getStyleClass().add("delete-button");
        deleteButton.setOnAction(e -> {
            try {
                deleteCompetition(competition);
            } catch (SQLException ex) {
                showAlert(AlertType.ERROR, "Error", "Could not delete mission: " + ex.getMessage());
            }
        });

        actions.getChildren().addAll(editButton, deleteButton);

        // Add all components to card
        card.getChildren().addAll(header, descLabel, detailsGrid, actions);

        return card;
    }

    private String formatGoalType(GoalTypeEnum goalType) {
        if (goalType == null) return "Unknown";

        switch (goalType) {
            case EVENT_COUNT: return "Event Count";
            case EVENT_LIKES: return "Event Likes";
            case MEMBER_COUNT: return "Member Count";
            default: return goalType.toString();
        }
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.toLocalDate().toString();
    }

    private void showAddForm() {
        isEditMode = false;
        formTitleLabel.setText("Add Mission");
        clearForm();
        showForm();
    }

    private void showEditForm(Competition competition) {
        isEditMode = true;
        formTitleLabel.setText("Edit Mission");
        selectedCompetition = competition;

        // Populate form fields
        missionTitleField.setText(competition.getNomComp());
        missionDescField.setText(competition.getDescComp());
        pointsField.setText(String.valueOf(competition.getPoints()));

        if (competition.getStartDate() != null) {
            startDateField.setValue(competition.getStartDate().toLocalDate());
        }

        if (competition.getEndDate() != null) {
            endDateField.setValue(competition.getEndDate().toLocalDate());
        }

        goalValueField.setText(String.valueOf(competition.getGoalValue()));
        goalTypeComboBox.setValue(competition.getGoalType());
        saisonComboBox.setValue(competition.getSaisonId());
        statusComboBox.setValue(competition.getStatus());

        showForm();
    }

    private void showForm() {
        missionFormContainer.setVisible(true);
        missionFormContainer.setManaged(true);
    }

    private void hideForm() {
        missionFormContainer.setVisible(false);
        missionFormContainer.setManaged(false);
        clearForm();
    }

    private void clearForm() {
        missionTitleField.clear();
        missionDescField.clear();
        pointsField.clear();
        startDateField.setValue(null);
        endDateField.setValue(null);
        goalValueField.clear();
        goalTypeComboBox.getSelectionModel().clearSelection();
        saisonComboBox.getSelectionModel().clearSelection();
        statusComboBox.getSelectionModel().clearSelection();
        selectedCompetition = null;
    }

    private void createCompetition() throws SQLException {
        // Validate form
        if (!validateForm()) {
            return;
        }

        Competition newCompetition = new Competition();

        // Set competition properties from form
        newCompetition.setNomComp(missionTitleField.getText());
        newCompetition.setDescComp(missionDescField.getText());
        newCompetition.setPoints(Integer.parseInt(pointsField.getText()));

        if (startDateField.getValue() != null) {
            newCompetition.setStartDate(LocalDateTime.of(startDateField.getValue(), LocalTime.MIDNIGHT));
        }

        if (endDateField.getValue() != null) {
            newCompetition.setEndDate(LocalDateTime.of(endDateField.getValue(), LocalTime.of(23, 59, 59)));
        }

        newCompetition.setGoalValue(Integer.parseInt(goalValueField.getText()));
        newCompetition.setGoalType(goalTypeComboBox.getValue());
        newCompetition.setSaisonId(saisonComboBox.getValue());
        newCompetition.setStatus(statusComboBox.getValue());

        competitionService.add(newCompetition);
        showAlert(AlertType.INFORMATION, "Mission Created", "The mission was created successfully.");
        hideForm();
        loadAllCompetitions();
    }

    private void updateCompetition() throws SQLException {
        // Validate form
        if (!validateForm()) {
            return;
        }

        // Set competition properties from form
        selectedCompetition.setNomComp(missionTitleField.getText());
        selectedCompetition.setDescComp(missionDescField.getText());
        selectedCompetition.setPoints(Integer.parseInt(pointsField.getText()));

        if (startDateField.getValue() != null) {
            selectedCompetition.setStartDate(LocalDateTime.of(startDateField.getValue(), LocalTime.MIDNIGHT));
        } else {
            selectedCompetition.setStartDate(null);
        }

        if (endDateField.getValue() != null) {
            selectedCompetition.setEndDate(LocalDateTime.of(endDateField.getValue(), LocalTime.of(23, 59, 59)));
        } else {
            selectedCompetition.setEndDate(null);
        }

        selectedCompetition.setGoalValue(Integer.parseInt(goalValueField.getText()));
        selectedCompetition.setGoalType(goalTypeComboBox.getValue());
        selectedCompetition.setSaisonId(saisonComboBox.getValue());
        selectedCompetition.setStatus(statusComboBox.getValue());

        competitionService.update(selectedCompetition);
        showAlert(AlertType.INFORMATION, "Mission Updated", "The mission was updated successfully.");
        hideForm();
        loadAllCompetitions();
    }

    private boolean validateForm() {
        StringBuilder errorMessage = new StringBuilder();

        if (missionTitleField.getText() == null || missionTitleField.getText().trim().isEmpty()) {
            errorMessage.append("- Please enter a mission title.\n");
        }

        // Validate points (must be a positive integer)
        try {
            int points = Integer.parseInt(pointsField.getText());
            if (points <= 0) {
                errorMessage.append("- Points must be a positive number.\n");
            }
        } catch (NumberFormatException e) {
            errorMessage.append("- Please enter a valid number for points.\n");
        }

        // Validate goal value
        try {
            int goalValue = Integer.parseInt(goalValueField.getText());
            if (goalValue <= 0) {
                errorMessage.append("- Goal value must be a positive number.\n");
            }
        } catch (NumberFormatException e) {
            errorMessage.append("- Please enter a valid number for goal value.\n");
        }

        // Validate that start date is before end date if both are set
        if (startDateField.getValue() != null && endDateField.getValue() != null) {
            if (startDateField.getValue().isAfter(endDateField.getValue())) {
                errorMessage.append("- Start date must be before end date.\n");
            }
        }

        // Validate required dropdowns
        if (goalTypeComboBox.getValue() == null) {
            errorMessage.append("- Please select a goal type.\n");
        }

        if (saisonComboBox.getValue() == null) {
            errorMessage.append("- Please select a season.\n");
        }

        if (statusComboBox.getValue() == null) {
            errorMessage.append("- Please select a status.\n");
        }

        if (errorMessage.length() > 0) {
            showAlert(AlertType.WARNING, "Form Validation Error",
                    "Please correct the following errors:\n" + errorMessage.toString());
            return false;
        }

        return true;
    }

    private void deleteCompetition(Competition competition) throws SQLException {
        if (competition == null) {
            showAlert(AlertType.WARNING, "No Mission Selected", "Please select a mission to delete.");
            return;
        }

        // Confirm deletion
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Are you sure you want to delete the mission '" + competition.getNomComp() + "'?");

        if (confirmAlert.showAndWait().get() == ButtonType.OK) {
            competitionService.delete(competition.getId());
            showAlert(AlertType.INFORMATION, "Mission Deleted", "The mission was deleted successfully.");
            loadAllCompetitions();
        }
    }

    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/dashboard.fxml"));
            AnchorPane previousScene = loader.load();
            competitionListPane.getChildren().setAll(previousScene);
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

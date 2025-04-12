package com.esprit.controllers;

import com.esprit.models.ChoixSondage;
import com.esprit.models.Sondage;
import com.esprit.models.User;
import com.esprit.services.ChoixSondageService;
import com.esprit.services.SondageService;
import com.esprit.utils.AlertUtils;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.control.Tooltip;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;

/**
 * Contrôleur pour la fenêtre modale d'édition de sondage
 */
public class EditPollModalController implements Initializable {

    @FXML private TextField pollQuestionInput;
    @FXML private VBox optionsContainer;
    @FXML private Button addOptionButton;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button closeButton;

    private final SondageService sondageService = SondageService.getInstance();
    private final ChoixSondageService choixSondageService = new ChoixSondageService();
    private Stage modalStage;
    private Sondage currentPoll;
    private User currentUser;
    private boolean isCreateMode = true;
    private List<TextField> optionFields = new ArrayList<>();
    private Runnable onSaveHandler;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Setup button actions
        saveButton.setOnAction(this::handleSave);
        cancelButton.setOnAction(e -> closeModal());
        closeButton.setOnAction(e -> closeModal());
        addOptionButton.setOnAction(this::handleAddOption);
        
        // Add initial option fields in create mode
        if (isCreateMode) {
            Platform.runLater(() -> {
                addOptionField("", true);
                addOptionField("", true);
            });
        }
        
        // Add validation for poll question
        pollQuestionInput.textProperty().addListener((obs, oldVal, newVal) -> {
            validateForm();
        });
    }
    
    /**
     * Close the modal
     */
    private void closeModal() {
        if (modalStage != null) {
            modalStage.close();
        }
    }
    
    /**
     * Set the modal stage reference
     */
    public void setModalStage(Stage stage) {
        this.modalStage = stage;
        
        // Add fade-in animation when showing the modal
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), stage.getScene().getRoot());
        fadeIn.setFromValue(0.5);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }
    
    /**
     * Set edit mode with existing poll
     */
    public void setEditMode(Sondage sondage, User user) {
        this.isCreateMode = false;
        this.currentPoll = sondage;
        this.currentUser = user;
        
        Platform.runLater(() -> {
            // Clear existing option fields
            optionsContainer.getChildren().clear();
            optionFields.clear();
            
            // Set poll question
            pollQuestionInput.setText(sondage.getQuestion());
            saveButton.setText("Save Changes");
            
            // Load options directly from the poll object if available
            if (sondage.getChoix() != null && !sondage.getChoix().isEmpty()) {
                for (ChoixSondage option : sondage.getChoix()) {
                    addOptionField(option.getContenu(), false);
                }
            } else {
                // If options not in poll object, load from database
                try {
                    List<ChoixSondage> options = sondageService.getChoixBySondage(sondage.getId());
                    if (options.isEmpty()) {
                        // Add at least two empty options if none found
                        addOptionField("", false);
                        addOptionField("", false);
                    } else {
                        for (ChoixSondage option : options) {
                            // Don't show deprecated options in the UI
                            if (!option.getContenu().startsWith("[Deprecated]")) {
                                addOptionField(option.getContenu(), false);
                            }
                        }
                    }
                } catch (SQLException e) {
                    AlertUtils.showError("Error", "Could not load poll options: " + e.getMessage());
                    e.printStackTrace();
                    
                    // Add at least two empty options if loading failed
                    addOptionField("", false);
                    addOptionField("", false);
                }
            }
            
            // Ensure we have at least 2 options
            if (optionFields.size() < 2) {
                while (optionFields.size() < 2) {
                    addOptionField("", false);
                }
            }
            
            validateForm();
        });
    }
    
    /**
     * Set create mode with current user
     */
    public void setCreateMode(User user) {
        this.isCreateMode = true;
        this.currentUser = user;
        this.currentPoll = new Sondage();
        this.currentPoll.setCreatedAt(LocalDateTime.now());
        this.currentPoll.setUser(user);
        
        saveButton.setText("Create Poll");
        
        validateForm();
    }
    
    /**
     * Add a new option field
     */
    private void handleAddOption(ActionEvent event) {
        addOptionField("", true);
    }
    
    /**
     * Add an option field with the given text
     */
    private TextField addOptionField(String text, boolean animate) {
        HBox optionRow = new HBox(10);
        optionRow.setAlignment(Pos.CENTER_LEFT);
        
        TextField optionField = new TextField(text);
        optionField.setPromptText("Enter an option");
        optionField.getStyleClass().add("form-control");
        HBox.setHgrow(optionField, Priority.ALWAYS);
        
        // Add validation listener
        optionField.textProperty().addListener((obs, oldVal, newVal) -> {
            validateForm();
        });
        
        // Only show remove button if we have more than 2 options
        Button removeButton = new Button("×");
        removeButton.getStyleClass().add("remove-option-button");
        removeButton.setOnAction(e -> removeOptionField(optionRow, optionField));
        
        optionRow.getChildren().addAll(optionField, removeButton);
        optionsContainer.getChildren().add(optionRow);
        optionFields.add(optionField);
        
        // Add animation
        if (animate) {
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), optionRow);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        }
        
        validateForm();
        return optionField;
    }
    
    /**
     * Remove an option field
     */
    private void removeOptionField(HBox container, TextField field) {
        // Don't allow removing if only 2 options left
        if (optionFields.size() <= 2) {
            AlertUtils.showWarning("Warning", "At least 2 options are required for a poll.");
            return;
        }
        
        // Animate removal
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), container);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            optionsContainer.getChildren().remove(container);
            optionFields.remove(field);
            validateForm();
        });
        fadeOut.play();
    }
    
    /**
     * Validate form and enable/disable save button
     */
    private void validateForm() {
        boolean isValid = true;
        StringBuilder errorMessage = new StringBuilder();
        
        // Valider la question
        String question = pollQuestionInput.getText().trim();
        if (question.isEmpty()) {
            isValid = false;
            errorMessage.append("• Question cannot be empty\n");
        } else {
            Sondage tempSondage = new Sondage();
            tempSondage.setQuestion(question);
            
            if (tempSondage.estQuestionInvalide()) {
                isValid = false;
                errorMessage.append("• Question must be at least 5 characters long\n");
            }
            
            if (tempSondage.questionSansPointInterrogation()) {
                isValid = false;
                errorMessage.append("• Question should end with a question mark\n");
            }
        }
        
        // Valider les options
        List<String> optionTexts = new ArrayList<>();
        int validOptionsCount = 0;
        
        for (TextField field : optionFields) {
            String optionText = field.getText().trim();
            if (!optionText.isEmpty()) {
                ChoixSondage choix = new ChoixSondage();
                choix.setContenu(optionText);
                
                if (choix.estContenuInvalide()) {
                    isValid = false;
                    errorMessage.append("• Option \"").append(optionText).append("\" is invalid (must be 2-100 characters)\n");
                } else {
                    validOptionsCount++;
                    optionTexts.add(optionText);
                }
            }
        }
        
        // Vérifier le nombre d'options valides
        if (validOptionsCount < 2) {
            isValid = false;
            errorMessage.append("• At least 2 valid options are required\n");
        }
        
        // Vérifier les options dupliquées
        Set<String> uniqueOptions = new HashSet<>(optionTexts);
        if (uniqueOptions.size() < optionTexts.size()) {
            isValid = false;
            errorMessage.append("• Options must be unique\n");
        }
        
        // Mettre à jour l'état du bouton de sauvegarde
        saveButton.setDisable(!isValid);
        
        // Mettre à jour le tooltip avec les erreurs
        if (!isValid) {
            Tooltip tooltip = new Tooltip(errorMessage.toString());
            tooltip.setShowDelay(Duration.ZERO);
            saveButton.setTooltip(tooltip);
        } else {
            saveButton.setTooltip(null);
        }
    }
    
    /**
     * Handle save action
     */
    private void handleSave(ActionEvent event) {
        try {
            // Validate input
            String question = pollQuestionInput.getText().trim();
            if (question.isEmpty()) {
                AlertUtils.showWarning("Invalid Input", "Please enter a poll question.");
                return;
            }
            
            List<String> options = optionFields.stream()
                    .map(TextField::getText)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            
            if (options.size() < 2) {
                AlertUtils.showWarning("Invalid Input", "Please enter at least 2 options.");
                return;
            }
            
            // Set poll data
            currentPoll.setQuestion(question);
            
            if (isCreateMode) {
                // Create new poll
                sondageService.add(currentPoll);
                
                // Add options
                for (String option : options) {
                    ChoixSondage choix = new ChoixSondage();
                    choix.setContenu(option);
                    choix.setSondage(currentPoll);
                    choixSondageService.add(choix);
                }
                
                AlertUtils.showInformation("Success", "Poll created successfully!");
            } else {
                // When updating an existing poll:
                // 1. First update the poll question
                sondageService.update(currentPoll);
                
                try {
                    // 2. Get the current options from the database
                    List<ChoixSondage> existingOptions = sondageService.getChoixBySondage(currentPoll.getId());
                    
                    // 3. Update existing options and add new ones as needed
                    for (int i = 0; i < options.size(); i++) {
                        String optionContent = options.get(i);
                        
                        if (i < existingOptions.size()) {
                            // Update existing option
                            ChoixSondage existingOption = existingOptions.get(i);
                            existingOption.setContenu(optionContent);
                            choixSondageService.update(existingOption);
                        } else {
                            // Add new option
                            ChoixSondage newOption = new ChoixSondage();
                            newOption.setContenu(optionContent);
                            newOption.setSondage(currentPoll);
                            choixSondageService.add(newOption);
                        }
                    }
                    
                    // 4. If there are more existing options than new ones, remove the excess
                    if (existingOptions.size() > options.size()) {
                        for (int i = options.size(); i < existingOptions.size(); i++) {
                            ChoixSondage optionToRemove = existingOptions.get(i);
                            // Before deleting, check if there are any responses
                            try {
                                // Make sure that this option has no votes/responses
                                if (choixSondageService.getResponseCount(optionToRemove.getId()) > 0) {
                                    // If it has responses, keep it but mark it deprecated
                                    optionToRemove.setContenu("[Deprecated] " + optionToRemove.getContenu());
                                    choixSondageService.update(optionToRemove);
                                } else {
                                    // If no responses, safe to delete
                                    choixSondageService.delete(optionToRemove.getId());
                                }
                            } catch (SQLException ex) {
                                // If deletion fails, just update with deprecated label
                                optionToRemove.setContenu("[Deprecated] " + optionToRemove.getContenu());
                                choixSondageService.update(optionToRemove);
                            }
                        }
                    }
                    
                    AlertUtils.showInformation("Success", "Poll updated successfully!");
                    
                } catch (SQLException e) {
                    AlertUtils.showError("Error", "Failed to update poll options: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // Call save handler if provided
            if (onSaveHandler != null) {
                onSaveHandler.run();
            }
            
            // Close modal
            closeModal();
            
        } catch (SQLException e) {
            AlertUtils.showError("Error", "Could not save poll: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Set handler to be called after saving
     */
    public void setOnSaveHandler(Runnable handler) {
        this.onSaveHandler = handler;
    }
}
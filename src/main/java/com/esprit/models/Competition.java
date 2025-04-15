package com.esprit.models;

import com.esprit.models.enums.GoalTypeEnum;
import javafx.beans.property.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Competition {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final IntegerProperty points = new SimpleIntegerProperty();
    private final ObjectProperty<LocalDateTime> startDate = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> endDate = new SimpleObjectProperty<>();
    private final ObjectProperty<GoalTypeEnum> goalType = new SimpleObjectProperty<>();
    private final IntegerProperty goalValue = new SimpleIntegerProperty();
    private final ObjectProperty<Saison> saison = new SimpleObjectProperty<>();
    private final StringProperty status = new SimpleStringProperty("activated"); // default status

    public Competition() {
        // Default constructor
    }

    // --- ID ---
    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    // --- Title ---
    public String getTitle() {
        return title.get();
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    public StringProperty titleProperty() {
        return title;
    }

    // --- Description ---
    public String getDescription() {
        return description.get();
    }
    public void setDescription(String description) {
        this.description.set(description);
    }
    public StringProperty descriptionProperty() {
        return description;
    }
    // --- Points ---
    public int getPoints() {
        return points.get();
    }
    public void setPoints(int points) {
        this.points.set(points);
    }
    public IntegerProperty pointsProperty() {
        return points;
    }
    // --- Start Date ---
    public LocalDateTime getStartDate() {
        return startDate.get();
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate.set(startDate);
    }

    public ObjectProperty<LocalDateTime> startDateProperty() {
        return startDate;
    }

    // --- End Date ---
    public LocalDateTime getEndDate() {
        return endDate.get();
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate.set(endDate);
    }

    public ObjectProperty<LocalDateTime> endDateProperty() {
        return endDate;
    }

    // --- Goal Type ---
    public GoalTypeEnum getGoalType() {
        return goalType.get();
    }

    public void setGoalType(GoalTypeEnum goalType) {
        this.goalType.set(goalType);
    }

    public ObjectProperty<GoalTypeEnum> goalTypeProperty() {
        return goalType;
    }

    // --- Goal Value ---
    public int getGoalValue() {
        return goalValue.get();
    }

    public void setGoalValue(int goalValue) {
        this.goalValue.set(goalValue);
    }

    public IntegerProperty goalValueProperty() {
        return goalValue;
    }

    // --- Saison ---
    public Saison getSaison() {
        return saison.get();
    }

    public void setSaison(Saison saison) {
        this.saison.set(saison);
    }

    public ObjectProperty<Saison> saisonProperty() {
        return saison;
    }

    // --- Status ---
    public String getStatus() {
        return status.get();
    }

    public void setStatus(String status) {
        this.status.set(status);
    }

    public StringProperty statusProperty() {
        return status;
    }
}
package com.esprit.models;

import com.esprit.models.enums.GoalTypeEnum;
import javafx.beans.property.*;

public class MissionProgress {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final IntegerProperty clubId = new SimpleIntegerProperty();
    private final IntegerProperty competitionId = new SimpleIntegerProperty();
    private final IntegerProperty progress = new SimpleIntegerProperty(0);
    private final BooleanProperty isCompleted = new SimpleBooleanProperty(false);

    // Transient objects (not directly stored in the database)
    private Club club;
    private Competition competition;

    public MissionProgress() {
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

    // --- Club ID ---
    public int getClubId() {
        return clubId.get();
    }

    public void setClubId(int clubId) {
        this.clubId.set(clubId);
    }

    public IntegerProperty clubIdProperty() {
        return clubId;
    }

    // --- Competition ID ---
    public int getCompetitionId() {
        return competitionId.get();
    }

    public void setCompetitionId(int competitionId) {
        this.competitionId.set(competitionId);
    }

    public IntegerProperty competitionIdProperty() {
        return competitionId;
    }

    // --- Progress ---
    public int getProgress() {
        return progress.get();
    }

    public void setProgress(int progress) {
        // Progress cannot be negative
        if (progress < 0) {
            progress = 0;
        }
        this.progress.set(progress);
        // Check if the goal is reached whenever progress changes
        checkCompletion();
    }

    public IntegerProperty progressProperty() {
        return progress;
    }

    // --- IsCompleted ---
    public boolean getIsCompleted() {
        return isCompleted.get();
    }

    public void setIsCompleted(boolean isCompleted) {
        this.isCompleted.set(isCompleted);
    }

    public BooleanProperty isCompletedProperty() {
        return isCompleted;
    }

    // --- Club object (transient) ---
    public Club getClub() {
        return club;
    }

    public void setClub(Club club) {
        this.club = club;
        if (club != null) {
            setClubId(club.getId());
        }
    }

    // --- Competition object (transient) ---
    public Competition getCompetition() {
        return competition;
    }

    public void setCompetition(Competition competition) {
        this.competition = competition;
        if (competition != null) {
            setCompetitionId(competition.getId());
        }
    }

    // Business logic methods
    public boolean isGoalReached() {
        if (competition == null) {
            System.out.println("isGoalReached: competition is null");
            return false;
        }

        System.out.println("isGoalReached: checking progress " + progress.get() + " >= goal " + competition.getGoalValue());

        switch (competition.getGoalType()) {
            case EVENT_COUNT:
            case EVENT_LIKES:
            case MEMBER_COUNT:
                return progress.get() >= competition.getGoalValue();
            default:
                return progress.get() >= competition.getGoalValue();
        }
    }


    public void checkCompletion() {
        System.out.println("checkCompletion called - competition: " + (competition != null) + ", club: " + (club != null));

        if (competition == null || club == null) {
            System.out.println("Cannot check completion - missing competition or club");
            return;
        }

        boolean goalReached = isGoalReached();
        System.out.println("Goal reached check: " + goalReached + ", current progress: " + getProgress() + ", goal: " + competition.getGoalValue());

        if (goalReached && !isCompleted.get()) {
            System.out.println("Setting isCompleted to true");
            setIsCompleted(true);
        }
    }

    public void incrementProgress() {
        setProgress(getProgress() + 1);
        checkCompletion();
    }
}
package com.esprit.models;

import java.util.ArrayList;
import java.util.List;

/**
 * A wrapper class that combines a Club with its mission progress information
 * without modifying the Club class or database structure
 */
public class ClubWithMissionProgress {
    private Club club;
    private List<MissionProgress> missionProgressList = new ArrayList<>();

    public ClubWithMissionProgress(Club club) {
        this.club = club;
    }

    public Club getClub() {
        return club;
    }

    public void setClub(Club club) {
        this.club = club;
    }

    public List<MissionProgress> getMissionProgressList() {
        return missionProgressList;
    }

    public void setMissionProgressList(List<MissionProgress> missionProgressList) {
        this.missionProgressList = missionProgressList;
    }

    public void addMissionProgress(MissionProgress missionProgress) {
        if (missionProgress != null) {
            this.missionProgressList.add(missionProgress);
        }
    }

    // Delegate methods to access common Club properties
    public int getId() {
        return club.getId();
    }

    public String getNomC() {
        return club.getNomC();
    }

    public String getDescription() {
        return club.getDescription();
    }

    public String getLogo() {
        return club.getLogo();
    }

    public int getPoints() {
        return club.getPoints();
    }

    // Calculate total progress percentage across all missions
    public double getTotalProgressPercentage() {
        if (missionProgressList.isEmpty()) {
            return 0.0;
        }

        double totalProgress = 0.0;
        int totalMissions = 0;

        for (MissionProgress progress : missionProgressList) {
            if (progress.getCompetition() != null && progress.getCompetition().getGoalValue() > 0) {
                double progressPercentage = (double) progress.getProgress() / progress.getCompetition().getGoalValue() * 100.0;
                totalProgress += progressPercentage;
                totalMissions++;
            }
        }

        return totalMissions > 0 ? totalProgress / totalMissions : 0.0;
    }

    // Get the number of completed missions
    public int getCompletedMissionsCount() {
        int count = 0;
        for (MissionProgress progress : missionProgressList) {
            if (progress.getIsCompleted()) {
                count++;
            }
        }
        return count;
    }

    // Get total possible points from all missions
    public int getTotalPossiblePoints() {
        int totalPoints = 0;
        for (MissionProgress progress : missionProgressList) {
            if (progress.getCompetition() != null) {
                totalPoints += progress.getCompetition().getPoints();
            }
        }
        return totalPoints;
    }

    // Get earned points from completed missions
    public int getEarnedPoints() {
        int earnedPoints = 0;
        for (MissionProgress progress : missionProgressList) {
            if (progress.getIsCompleted() && progress.getCompetition() != null) {
                earnedPoints += progress.getCompetition().getPoints();
            }
        }
        return earnedPoints;
    }
}
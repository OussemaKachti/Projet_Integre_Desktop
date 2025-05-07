package com.esprit.services;

import com.esprit.models.Competition;
import com.esprit.models.Saison;
import com.esprit.utils.DataSource;
import com.esprit.models.enums.GoalTypeEnum;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CompetitionService implements IServiceYassine<Competition> {

    private final Connection connection;
    private MissionProgressService missionProgressService; // Removed final to allow lazy initialization

    public CompetitionService() {
        this.connection = DataSource.getInstance().getCnx();
        // Don't initialize missionProgressService here to break circular dependency
    }

    // Lazy initialization getter for MissionProgressService
    private MissionProgressService getMissionProgressService() {
        if (this.missionProgressService == null) {
            this.missionProgressService = MissionProgressService.getInstance();
        }
        return this.missionProgressService;
    }

    @Override
    public void add(Competition competition) throws SQLException {
        // Calculate status based on dates before adding
        competition.updateStatus();

        String sql = "INSERT INTO competition (nom_comp, desc_comp, points, start_date, end_date, goal_type, goal, saison_id, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, competition.getNomComp());
            ps.setString(2, competition.getDescComp());
            ps.setInt(3, competition.getPoints());
            ps.setTimestamp(4, competition.getStartDate() != null ? Timestamp.valueOf(competition.getStartDate()) : null);
            ps.setTimestamp(5, competition.getEndDate() != null ? Timestamp.valueOf(competition.getEndDate()) : null);
            ps.setString(6, competition.getGoalType() != null ? competition.getGoalType().name() : null);
            ps.setInt(7, competition.getGoalValue());
            ps.setInt(8, competition.getSaisonId() != null ? competition.getSaisonId().getId() : null);
            ps.setString(9, competition.getStatus());
            ps.executeUpdate();

            // Get the generated ID for the new competition
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    competition.setId(generatedKeys.getInt(1));

                    // If the competition is activated, initialize mission progress for all clubs
                    if ("activated".equals(competition.getStatus())) {
                        getMissionProgressService().initializeMissionProgressForAllClubs(competition);
                        System.out.println("Initialized mission progress for all clubs for competition: " + competition.getNomComp());
                    }
                } else {
                    throw new SQLException("Failed to get generated competition ID");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error adding competition: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void update(Competition competition) throws SQLException {
        // Get the original competition before updating to check status changes
        Competition originalCompetition = findById(competition.getId());
        String originalStatus = originalCompetition != null ? originalCompetition.getStatus() : null;

        // Calculate status based on dates before updating
        competition.updateStatus();

        String sql = "UPDATE competition SET nom_comp = ?, desc_comp = ?, points = ?, start_date = ?, end_date = ?, " +
                "goal_type = ?, goal = ?, saison_id = ?, status = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, competition.getNomComp());
            ps.setString(2, competition.getDescComp());
            ps.setInt(3, competition.getPoints());
            ps.setTimestamp(4, competition.getStartDate() != null ? Timestamp.valueOf(competition.getStartDate()) : null);
            ps.setTimestamp(5, competition.getEndDate() != null ? Timestamp.valueOf(competition.getEndDate()) : null);
            ps.setString(6, competition.getGoalType() != null ? competition.getGoalType().name() : null);
            ps.setInt(7, competition.getGoalValue());
            ps.setInt(8, competition.getSaisonId() != null ? competition.getSaisonId().getId() : null);
            ps.setString(9, competition.getStatus());
            ps.setInt(10, competition.getId());

            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated == 0) {
                throw new SQLException("Update failed, no rows affected. Competition with ID " + competition.getId() + " not found.");
            }

            // Handle mission progress based on status changes
            if (originalStatus != null && !originalStatus.equals(competition.getStatus())) {
                if ("activated".equals(competition.getStatus()) && "deactivated".equals(originalStatus)) {
                    // If competition was deactivated and is now activated, reactivate missions
                    getMissionProgressService().reactivateMission(competition);
                    System.out.println("Reactivated mission progress for competition: " + competition.getNomComp());
                } else if ("deactivated".equals(competition.getStatus()) && "activated".equals(originalStatus)) {
                    // If competition was activated and is now deactivated, reset progress
                    getMissionProgressService().resetProgressForDeactivatedMission(competition);
                    System.out.println("Reset mission progress for deactivated competition: " + competition.getNomComp());
                }
            } else if ("activated".equals(competition.getStatus())) {
                // If still active but dates may have changed, ensure all clubs have mission progress records
                getMissionProgressService().initializeMissionProgressForAllClubs(competition);
                System.out.println("Ensured mission progress records exist for updated competition: " + competition.getNomComp());
            }
        } catch (SQLException e) {
            System.err.println("Error updating competition: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Updates the status of all competitions based on current time
     */
    public void updateAllStatuses() throws SQLException {
        List<Competition> competitions = getAll();
        for (Competition competition : competitions) {
            String oldStatus = competition.getStatus();
            competition.updateStatus();

            if (!oldStatus.equals(competition.getStatus())) {
                // Status has changed, update in database and handle mission progress
                String sql = "UPDATE competition SET status = ? WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, competition.getStatus());
                    ps.setInt(2, competition.getId());
                    ps.executeUpdate();

                    // If competition was activated and is now deactivated, reset progress
                    if ("deactivated".equals(competition.getStatus()) && "activated".equals(oldStatus)) {
                        getMissionProgressService().resetProgressForDeactivatedMission(competition);
                        System.out.println("Reset mission progress for competition that became deactivated: " + competition.getNomComp());
                    }
                    // If competition was deactivated and is now activated, reactivate missions
                    else if ("activated".equals(competition.getStatus()) && "deactivated".equals(oldStatus)) {
                        getMissionProgressService().reactivateMission(competition);
                        System.out.println("Reactivated mission progress for competition: " + competition.getNomComp());
                    }
                }
            }
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        // Note: We don't need to manually delete mission_progress records if you have
        // foreign key constraints with ON DELETE CASCADE in your database

        String sql = "DELETE FROM competition WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rowsDeleted = ps.executeUpdate();
            if (rowsDeleted == 0) {
                throw new SQLException("Delete failed, no rows affected. Competition with ID " + id + " not found.");
            }
        }
    }

    @Override
    public List<Competition> getAll() throws SQLException {
        List<Competition> competitions = new ArrayList<>();
        String sql = "SELECT * FROM competition";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Competition c = extractCompetition(rs);
                // Calculate current status based on dates before returning
                c.updateStatus();
                competitions.add(c);
            }
        }

        return competitions;
    }

    
    public Competition findById(int id) throws SQLException {
        String sql = "SELECT * FROM competition WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Competition competition = extractCompetition(rs);
                    // Calculate current status based on dates before returning
                    competition.updateStatus();
                    return competition;
                }
            }
        }

        return null;
    }

    private Competition extractCompetition(ResultSet rs) throws SQLException {
        Competition c = new Competition();
        c.setId(rs.getInt("id"));
        c.setNomComp(rs.getString("nom_comp"));
        c.setDescComp(rs.getString("desc_comp"));
        c.setPoints(rs.getInt("points"));

        // Handle timestamps that might be null
        java.sql.Timestamp startTimestamp = rs.getTimestamp("start_date");
        if (startTimestamp != null) {
            c.setStartDate(startTimestamp.toLocalDateTime());
        }

        java.sql.Timestamp endTimestamp = rs.getTimestamp("end_date");
        if (endTimestamp != null) {
            c.setEndDate(endTimestamp.toLocalDateTime());
        }

        // Handle goal_type column
        try {
            String goalType = rs.getString("goal_type");
            if (goalType != null) {
                try {
                    c.setGoalType(GoalTypeEnum.valueOf(goalType));
                } catch (IllegalArgumentException e) {
                    // If the enum value is invalid, use default
                    c.setGoalType(GoalTypeEnum.EVENT_COUNT);
                }
            } else {
                c.setGoalType(GoalTypeEnum.EVENT_COUNT); // Default value
            }
        } catch (SQLException e) {
            // Column doesn't exist, set default
            c.setGoalType(GoalTypeEnum.EVENT_COUNT);
        }

        // Handle goal column - use the actual column name in your database
        try {
            c.setGoalValue(rs.getInt("goal"));
        } catch (SQLException e) {
            // If column doesn't exist or there's an error, set default
            c.setGoalValue(0);
        }

        // Handle saison_id
        try {
            int saisonId = rs.getInt("saison_id");
            if (!rs.wasNull()) {
                Saison saison = new Saison();
                saison.setId(saisonId);
                c.setSaisonId(saison);
            }
        } catch (SQLException e) {
            // If saison_id doesn't exist, set null
            c.setSaisonId(null);
        }

        // Get status
        try {
            c.setStatus(rs.getString("status"));
        } catch (SQLException e) {
            // If status column doesn't exist, calculate it
            c.updateStatus();
        }

        return c;
    }

    /**
     * Get active competitions
     */
    public List<Competition> getActiveCompetitions() throws SQLException {
        List<Competition> activeCompetitions = new ArrayList<>();
        String sql = "SELECT * FROM competition WHERE status = 'activated'";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Competition c = extractCompetition(rs);
                // Double-check the status is still active based on dates
                c.updateStatus();
                if ("activated".equals(c.getStatus())) {
                    activeCompetitions.add(c);
                }
            }
        }

        return activeCompetitions;
    }
}
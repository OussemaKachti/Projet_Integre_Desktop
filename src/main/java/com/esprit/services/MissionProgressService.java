package com.esprit.services;

import com.esprit.models.Club;
import com.esprit.models.ClubWithMissionProgress;
import com.esprit.models.Competition;
import com.esprit.models.MissionProgress;
import com.esprit.models.Saison;
import com.esprit.models.enums.GoalTypeEnum;
import com.esprit.utils.DataSource;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service class to handle mission progress tracking.
 * This class works independently from the existing event service.
 */
public class MissionProgressService {

    private final Connection connection;
    private static MissionProgressService instance;
    private final ClubService clubService;
    private CompetitionService competitionService; // Removed final to allow lazy initialization

    // List of listeners for mission completion events
    private final List<MissionCompletionListener> completionListeners = new CopyOnWriteArrayList<>();

    private MissionProgressService() {
        this.connection = DataSource.getInstance().getCnx();
        this.clubService = ClubService.getInstance();
        // Don't initialize competitionService here to break circular dependency
    }

    public static MissionProgressService getInstance() {
        if (instance == null) {
            instance = new MissionProgressService();
        }
        return instance;
    }

    // Lazy initialization getter for CompetitionService
    private CompetitionService getCompetitionService() {
        if (this.competitionService == null) {
            this.competitionService = new CompetitionService();
        }
        return this.competitionService;
    }

    /**
     * Interface for mission completion listeners
     */
    public interface MissionCompletionListener {
        void onMissionCompleted(MissionProgress missionProgress);
    }

    /**
     * Add a listener for mission completion events
     */
    public void addCompletionListener(MissionCompletionListener listener) {
        if (listener != null && !completionListeners.contains(listener)) {
            completionListeners.add(listener);
        }
    }

    /**
     * Remove a mission completion listener
     */
    public void removeCompletionListener(MissionCompletionListener listener) {
        completionListeners.remove(listener);
    }

    /**
     * Notify all listeners when a mission is completed
     */
    private void notifyMissionCompleted(MissionProgress missionProgress) {
        for (MissionCompletionListener listener : completionListeners) {
            try {
                listener.onMissionCompleted(missionProgress);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Extract a Competition object from a ResultSet
     * We need this because CompetitionService.extractCompetition is private
     */
    private Competition extractCompetitionFromResultSet(ResultSet rs) throws SQLException {
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

        // Handle goal_type
        String goalType = rs.getString("goal_type");
        if (goalType != null) {
            try {
                c.setGoalType(GoalTypeEnum.valueOf(goalType));
            } catch (IllegalArgumentException e) {
                c.setGoalType(GoalTypeEnum.EVENT_COUNT); // Default if invalid enum value
            }
        } else {
            c.setGoalType(GoalTypeEnum.EVENT_COUNT); // Default if null
        }

        c.setGoalValue(rs.getInt("goal"));
        c.setStatus(rs.getString("status"));

        // Handle saison_id
        int saisonId = rs.getInt("saison_id");
        if (!rs.wasNull()) {
            // Create a minimal Saison object with just the ID
            Saison saison = new Saison();
            saison.setId(saisonId);
            c.setSaisonId(saison);
        }

        return c;
    }

    /**
     * Add a new mission progress record
     */
    public void add(MissionProgress missionProgress) throws SQLException {
        // Check if record already exists to enforce unique constraint
        if (progressExists(missionProgress.getClubId(), missionProgress.getCompetitionId())) {
            // If it exists, update instead of add
            update(missionProgress);
            return;
        }

        String sql = "INSERT INTO mission_progress (club_id, competition_id, progress, is_completed) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, missionProgress.getClubId());
            ps.setInt(2, missionProgress.getCompetitionId());
            ps.setInt(3, missionProgress.getProgress());
            ps.setBoolean(4, missionProgress.getIsCompleted());

            ps.executeUpdate();

            // Get the generated ID
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    missionProgress.setId(generatedKeys.getInt(1));
                }
            }

            // Notify listeners if mission is completed
            if (missionProgress.getIsCompleted()) {
                notifyMissionCompleted(missionProgress);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Update an existing mission progress record
     */
    public void update(MissionProgress missionProgress) {
        System.out.println("UPDATE: Starting update for MissionProgress ID: " + missionProgress.getId());
        System.out.println("UPDATE: Progress value to save: " + missionProgress.getProgress());

        boolean wasCompleted = false;

        // Check if the mission was already completed before update
        MissionProgress existingProgress = findById(missionProgress.getId());
        if (existingProgress != null) {
            wasCompleted = existingProgress.getIsCompleted();
            System.out.println("UPDATE: Existing progress value: " + existingProgress.getProgress());
            System.out.println("UPDATE: Existing isCompleted: " + existingProgress.getIsCompleted());
        }

        String sql = "UPDATE mission_progress SET club_id = ?, competition_id = ?, progress = ?, is_completed = ? " +
                "WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, missionProgress.getClubId());
            ps.setInt(2, missionProgress.getCompetitionId());
            ps.setInt(3, missionProgress.getProgress());
            ps.setBoolean(4, missionProgress.getIsCompleted());
            ps.setInt(5, missionProgress.getId());

            System.out.println("UPDATE: Executing SQL with values:");
            System.out.println("  club_id: " + missionProgress.getClubId());
            System.out.println("  competition_id: " + missionProgress.getCompetitionId());
            System.out.println("  progress: " + missionProgress.getProgress());
            System.out.println("  is_completed: " + missionProgress.getIsCompleted());
            System.out.println("  id: " + missionProgress.getId());

            int rowsAffected = ps.executeUpdate();
            System.out.println("UPDATE: Rows affected: " + rowsAffected);

            // Notify listeners if mission was just completed
            if (missionProgress.getIsCompleted() && !wasCompleted) {
                System.out.println("UPDATE: Mission newly completed, notifying listeners");
                notifyMissionCompleted(missionProgress);
            }
        } catch (SQLException e) {
            System.out.println("UPDATE ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Find a mission progress record by ID
     */
    public MissionProgress findById(int id) {
        System.out.println("Finding MissionProgress by ID: " + id);

        String sql = "SELECT * FROM mission_progress WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    MissionProgress mp = extractMissionProgress(rs);
                    System.out.println("Found MissionProgress - Progress: " + mp.getProgress() + ", IsCompleted: " + mp.getIsCompleted());
                    return mp;
                } else {
                    System.out.println("No MissionProgress found with ID: " + id);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error in findById: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Find mission progress by club and competition
     */
    public MissionProgress findByClubAndCompetition(int clubId, int competitionId) {
        System.out.println("Finding MissionProgress for club: " + clubId + ", competition: " + competitionId);

        String sql = "SELECT * FROM mission_progress WHERE club_id = ? AND competition_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clubId);
            ps.setInt(2, competitionId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    MissionProgress mp = extractMissionProgress(rs);
                    System.out.println("Found MissionProgress ID: " + mp.getId() + ", Progress: " + mp.getProgress());
                    return mp;
                } else {
                    System.out.println("No MissionProgress found");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error in findByClubAndCompetition: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Check if a progress record already exists
     */
    public boolean progressExists(int clubId, int competitionId) throws SQLException {
        String sql = "SELECT id FROM mission_progress WHERE club_id = ? AND competition_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clubId);
            ps.setInt(2, competitionId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // Returns true if a record already exists
            }
        }
    }

    /**
     * Get all mission progresses for a specific club
     */
    public List<MissionProgress> findByClub(int clubId) {
        List<MissionProgress> missionProgresses = new ArrayList<>();
        String sql = "SELECT * FROM mission_progress WHERE club_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clubId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MissionProgress mp = extractMissionProgress(rs);
                    missionProgresses.add(mp);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return missionProgresses;
    }

    /**
     * Get all mission progresses for a specific competition
     */
    public List<MissionProgress> findByCompetition(int competitionId) {
        List<MissionProgress> missionProgresses = new ArrayList<>();
        String sql = "SELECT * FROM mission_progress WHERE competition_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, competitionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MissionProgress mp = extractMissionProgress(rs);
                    missionProgresses.add(mp);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return missionProgresses;
    }

    /**
     * Get all mission progress records
     */
    public List<MissionProgress> getAll() {
        List<MissionProgress> missionProgresses = new ArrayList<>();
        String sql = "SELECT * FROM mission_progress";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                MissionProgress mp = extractMissionProgress(rs);
                missionProgresses.add(mp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return missionProgresses;
    }

    /**
     * Extract a MissionProgress object from a ResultSet
     */
    private MissionProgress extractMissionProgress(ResultSet rs) throws SQLException {
        System.out.println("Extracting MissionProgress from ResultSet");

        MissionProgress mp = new MissionProgress();
        mp.setId(rs.getInt("id"));
        mp.setClubId(rs.getInt("club_id"));
        mp.setCompetitionId(rs.getInt("competition_id"));
        mp.setProgress(rs.getInt("progress"));
        mp.setIsCompleted(rs.getBoolean("is_completed"));

        System.out.println("Extracted - ID: " + mp.getId() + ", Progress: " + mp.getProgress() +
                ", IsCompleted: " + mp.getIsCompleted());

        // Load related entities
        try {
            Club club = clubService.getById(mp.getClubId());
            mp.setClub(club);
            System.out.println("Loaded club: " + (club != null ? club.getNomC() : "null"));
        } catch (Exception e) {
            System.out.println("Error loading club: " + e.getMessage());
        }

        try {
            Competition competition = getCompetitionService().findById(mp.getCompetitionId());
            mp.setCompetition(competition);
            System.out.println("Loaded competition: " + (competition != null ? competition.getNomComp() : "null"));
        } catch (Exception e) {
            System.out.println("Error loading competition: " + e.getMessage());
        }

        return mp;
    }

    /**
     * Initialize mission progress for all clubs when a new mission is created
     * Call this when a new competition is added
     */
    public void initializeMissionProgressForAllClubs(Competition competition) {
        try {
            // Get all clubs
            List<Club> allClubs = clubService.getAll();

            for (Club club : allClubs) {
                // Check if progress record already exists
                if (!progressExists(club.getId(), competition.getId())) {
                    // Create new progress record with 0 progress
                    MissionProgress missionProgress = new MissionProgress();
                    missionProgress.setClubId(club.getId());
                    missionProgress.setCompetitionId(competition.getId());
                    missionProgress.setProgress(0);
                    missionProgress.setIsCompleted(false);
                    missionProgress.setClub(club);
                    missionProgress.setCompetition(competition);

                    // Save to database
                    add(missionProgress);

                    System.out.println("Initialized mission progress for club: " + club.getNomC() +
                            " and mission: " + competition.getNomComp());
                }
            }
        } catch (Exception e) {
            System.err.println("Error initializing mission progress: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Reset progress when a mission is deactivated
     */
    public void resetProgressForDeactivatedMission(Competition competition) {
        if (!"activated".equals(competition.getStatus())) {
            try {
                // Get all progress records for this competition
                List<MissionProgress> progressRecords = findByCompetition(competition.getId());

                for (MissionProgress progress : progressRecords) {
                    // Reset progress and completion status
                    progress.setProgress(0);
                    progress.setIsCompleted(false);

                    // Update in database
                    update(progress);

                    System.out.println("Reset progress for club ID: " + progress.getClubId() +
                            " on deactivated mission: " + competition.getNomComp());
                }
            } catch (Exception e) {
                System.err.println("Error resetting mission progress: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Check for expired missions based on end date
     * Should be called periodically
     */
    public void checkExpiredMissions() {
        try {
            // Get all activated competitions
            String sql = "SELECT * FROM competition WHERE status = 'activated'";

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    // Use our own extraction method
                    Competition competition = extractCompetitionFromResultSet(rs);

                    // Check if end date has passed
                    if (competition.getEndDate() != null &&
                            competition.getEndDate().isBefore(LocalDateTime.now())) {

                        // Deactivate the mission
                        competition.setStatus("deactivated");
                        getCompetitionService().update(competition);

                        // Reset progress
                        resetProgressForDeactivatedMission(competition);

                        System.out.println("Automatically deactivated expired mission: " +
                                competition.getNomComp());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking expired missions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Reactivate a mission and reset progress
     */
    public void reactivateMission(Competition competition) {
        try {
            // Set status to activated
            competition.setStatus("activated");
            getCompetitionService().update(competition);

            // Reset all progress records for this competition
            List<MissionProgress> progressRecords = findByCompetition(competition.getId());

            for (MissionProgress progress : progressRecords) {
                // Reset progress and completion status
                progress.setProgress(0);
                progress.setIsCompleted(false);

                // Update in database
                update(progress);
            }

            System.out.println("Reactivated mission: " + competition.getNomComp() +
                    " and reset all progress records");
        } catch (Exception e) {
            System.err.println("Error reactivating mission: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialize mission progress for a new club
     * Call this when a new club is created
     */
    public void initializeMissionsForNewClub(int clubId) {
        try {
            // Get all active competitions
            String sql = "SELECT * FROM competition WHERE status = 'activated'";

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                Club club = clubService.getById(clubId);
                if (club == null) {
                    System.err.println("Could not find club with ID: " + clubId);
                    return;
                }

                while (rs.next()) {
                    // Use our own extraction method instead of competitionService.extractCompetition
                    Competition competition = extractCompetitionFromResultSet(rs);

                    // Check if progress record already exists
                    if (!progressExists(clubId, competition.getId())) {
                        // Create new progress record
                        MissionProgress missionProgress = new MissionProgress();
                        missionProgress.setClubId(clubId);
                        missionProgress.setCompetitionId(competition.getId());
                        missionProgress.setProgress(0);
                        missionProgress.setIsCompleted(false);
                        missionProgress.setClub(club);
                        missionProgress.setCompetition(competition);

                        // Save to database
                        add(missionProgress);

                        System.out.println("Initialized mission progress for new club: " + club.getNomC() +
                                " and mission: " + competition.getNomComp());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error initializing missions for new club: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get active mission progress records grouped by club
     * Uses a wrapper class to avoid modifying the Club class
     */
    public List<ClubWithMissionProgress> getClubsWithActiveMissionProgress() {
        List<ClubWithMissionProgress> clubsWithProgress = new ArrayList<>();

        try {
            // Get all clubs
            List<Club> allClubs = clubService.getAll();

            for (Club club : allClubs) {
                // Load active mission progress for this club
                List<MissionProgress> activeProgress = getActiveMissionProgressForClub(club.getId());

                if (!activeProgress.isEmpty()) {
                    // Create wrapper and store progress
                    ClubWithMissionProgress clubWrapper = new ClubWithMissionProgress(club);
                    clubWrapper.setMissionProgressList(activeProgress);
                    clubsWithProgress.add(clubWrapper);
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting clubs with mission progress: " + e.getMessage());
            e.printStackTrace();
        }

        return clubsWithProgress;
    }

    /**
     * Get active mission progress for a club
     */
    public List<MissionProgress> getActiveMissionProgressForClub(int clubId) {
        List<MissionProgress> activeProgress = new ArrayList<>();

        try {
            String sql = "SELECT mp.* FROM mission_progress mp " +
                    "JOIN competition c ON mp.competition_id = c.id " +
                    "WHERE mp.club_id = ? AND c.status = 'activated'";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, clubId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        MissionProgress mp = extractMissionProgress(rs);
                        activeProgress.add(mp);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting active mission progress: " + e.getMessage());
            e.printStackTrace();
        }

        return activeProgress;
    }

    /**
     * This method is called from the EventHandler to update mission progress
     * when a new event is created
     */
    public void incrementEventCountProgress(int clubId) {
        System.out.println("=== START incrementEventCountProgress for clubId: " + clubId + " ===");

        try {
            // Get all active event count competitions
            List<Competition> activeCompetitions = new ArrayList<>();
            String competitionSql = "SELECT * FROM competition WHERE status = 'activated' AND goal_type = ?";

            try (PreparedStatement ps = connection.prepareStatement(competitionSql)) {
                ps.setString(1, GoalTypeEnum.EVENT_COUNT.name());
                System.out.println("Looking for active competitions with goal_type: " + GoalTypeEnum.EVENT_COUNT.name());

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Competition competition = extractCompetitionFromResultSet(rs);
                        activeCompetitions.add(competition);
                        System.out.println("Found active competition: " + competition.getNomComp() +
                                " (ID: " + competition.getId() + ", Goal: " + competition.getGoalValue() + ")");
                    }
                }
            }

            System.out.println("Total active competitions found: " + activeCompetitions.size());

            // For each active competition, update or create mission progress
            for (Competition competition : activeCompetitions) {
                System.out.println("\nProcessing competition: " + competition.getNomComp());

                // Check if mission progress already exists
                MissionProgress missionProgress = findByClubAndCompetition(clubId, competition.getId());

                if (missionProgress == null) {
                    System.out.println("No existing progress found. Creating new mission progress.");
                    // Create new mission progress
                    missionProgress = new MissionProgress();
                    missionProgress.setClubId(clubId);
                    missionProgress.setCompetitionId(competition.getId());
                    missionProgress.setProgress(1); // Starting with 1 as we're adding an event

                    // Load related entities for business logic
                    Club club = clubService.getById(clubId);
                    missionProgress.setClub(club);
                    missionProgress.setCompetition(competition);

                    System.out.println("New progress initialized with value: 1");

                    // Check if goal is reached
                    missionProgress.checkCompletion();

                    // Save to database
                    add(missionProgress);

                    // If goal is reached, award points to club
                    if (missionProgress.getIsCompleted()) {
                        System.out.println("Goal reached on first event! Awarding points...");
                        awardPointsToClub(missionProgress);
                    }
                } else {
                    System.out.println("Found existing progress with ID: " + missionProgress.getId());
                    System.out.println("Current progress: " + missionProgress.getProgress() +
                            ", Completed: " + missionProgress.getIsCompleted());

                    if (!missionProgress.getIsCompleted()) {
                        // IMPORTANT: Capture completion status BEFORE updating progress
                        boolean wasCompleted = missionProgress.getIsCompleted();
                        System.out.println("Was completed before increment: " + wasCompleted);

                        // Update existing mission progress
                        int oldProgress = missionProgress.getProgress();
                        int newProgress = oldProgress + 1;
                        missionProgress.setProgress(newProgress);

                        System.out.println("Incremented progress from " + oldProgress + " to " + newProgress);

                        // Make sure related entities are loaded for business logic
                        if (missionProgress.getClub() == null) {
                            missionProgress.setClub(clubService.getById(clubId));
                            System.out.println("Loaded club entity");
                        }

                        if (missionProgress.getCompetition() == null) {
                            missionProgress.setCompetition(competition);
                            System.out.println("Loaded competition entity");
                        }

                        System.out.println("Club loaded: " + (missionProgress.getClub() != null));
                        System.out.println("Competition loaded: " + (missionProgress.getCompetition() != null));

                        // Check if goal is reached after incrementing
                        System.out.println("Is completed after increment: " + missionProgress.getIsCompleted());

                        System.out.println("About to update database with progress: " + missionProgress.getProgress());
                        update(missionProgress);
                        System.out.println("Database update completed");

                        // Verify the update by re-fetching
                        MissionProgress verifyProgress = findById(missionProgress.getId());
                        System.out.println("Verified progress from DB: " + verifyProgress.getProgress());

                        // If goal was just reached, award points to club
                        if (!wasCompleted && missionProgress.getIsCompleted()) {
                            System.out.println("Goal just reached! Calling awardPointsToClub()");
                            awardPointsToClub(missionProgress);
                        } else {
                            System.out.println("Goal not yet reached or already completed");
                        }
                    } else {
                        System.out.println("Mission already completed. Skipping.");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR in incrementEventCountProgress: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== END incrementEventCountProgress ===\n");
    }

    /**
     * Method to award points to a club
     */
    private void awardPointsToClub(MissionProgress missionProgress) {
        try {
            if (missionProgress.getCompetition() == null || missionProgress.getClub() == null) {
                System.out.println("Loading related entities...");
                // Load related entities if needed
                if (missionProgress.getCompetition() == null) {
                    missionProgress.setCompetition(getCompetitionService().findById(missionProgress.getCompetitionId()));
                }

                if (missionProgress.getClub() == null) {
                    missionProgress.setClub(clubService.getById(missionProgress.getClubId()));
                }

                // If still null, return
                if (missionProgress.getCompetition() == null || missionProgress.getClub() == null) {
                    System.out.println("ERROR: Could not load club or competition!");
                    return;
                }
            }

            int points = missionProgress.getCompetition().getPoints();
            System.out.println("Competition points value: " + points);
            if (points > 0) {
                Club club = missionProgress.getClub();
                int oldPoints = club.getPoints();
                club.setPoints(club.getPoints() + points);
                System.out.println("Updating club points from " + oldPoints + " to " + club.getPoints());
                clubService.modifier(club);

                System.out.println("Awarded " + points + " points to club " + club.getNomC()
                        + " for completing mission: " + missionProgress.getCompetition().getNomComp());

                // Notify listeners about mission completion
                notifyMissionCompleted(missionProgress);
                System.out.println("Club update completed successfully");
            } else {
                System.out.println("WARNING: Competition has 0 points!");
            }
        } catch (Exception e) {
            System.out.println("ERROR in awardPointsToClub: " + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * Initialize a single mission progress record for a specific club and competition
     * @param clubId The club ID
     * @param competitionId The competition ID
     * @throws SQLException If there's a database error
     */
    public void initializeMissionProgressForClub(int clubId, int competitionId) throws SQLException {
        // Check if progress record already exists
        if (!progressExists(clubId, competitionId)) {
            // Get the competition details
            Competition competition = getCompetitionService().findById(competitionId);
            Club club = clubService.getById(clubId);

            if (competition != null && club != null) {
                // Create new progress record with 0 progress
                MissionProgress missionProgress = new MissionProgress();
                missionProgress.setClubId(clubId);
                missionProgress.setCompetitionId(competitionId);
                missionProgress.setProgress(0);
                missionProgress.setIsCompleted(false);
                missionProgress.setClub(club);
                missionProgress.setCompetition(competition);

                // Save to database
                add(missionProgress);

                System.out.println("Initialized mission progress for club: " + club.getNomC() +
                        " and mission: " + competition.getNomComp());
            } else {
                throw new SQLException("Could not find club or competition with provided IDs");
            }
        }
    }
}
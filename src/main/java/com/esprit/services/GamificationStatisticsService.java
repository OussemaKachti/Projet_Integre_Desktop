package com.esprit.services;

import com.esprit.models.*;
import com.esprit.models.enums.GoalTypeEnum;
import com.esprit.utils.DataSource;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class GamificationStatisticsService {

    private final Connection connection;
    private static GamificationStatisticsService instance;
    private final ClubService clubService;
    private final CompetitionService competitionService;
    private final MissionProgressService missionProgressService;
    private final SaisonService saisonService;

    private GamificationStatisticsService() {
        this.connection = DataSource.getInstance().getCnx();
        this.clubService = ClubService.getInstance();
        this.competitionService = new CompetitionService();
        this.missionProgressService = MissionProgressService.getInstance();
        this.saisonService = new SaisonService();
    }

    public static GamificationStatisticsService getInstance() {
        if (instance == null) {
            instance = new GamificationStatisticsService();
        }
        return instance;
    }

    // Club Statistics
    public static class ClubStatistics {
        private int totalPoints;
        private int completedMissions;
        private int totalMissions;
        private double completionRate;
        private int currentRank;
        private int pointsThisSeason;
        private Map<GoalTypeEnum, Integer> completedMissionsByType;

        public ClubStatistics() {
            this.completedMissionsByType = new HashMap<>();
        }

        // Getters and setters
        public int getTotalPoints() { return totalPoints; }
        public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }
        public int getCompletedMissions() { return completedMissions; }
        public void setCompletedMissions(int completedMissions) { this.completedMissions = completedMissions; }
        public int getTotalMissions() { return totalMissions; }
        public void setTotalMissions(int totalMissions) { this.totalMissions = totalMissions; }
        public double getCompletionRate() { return completionRate; }
        public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }
        public int getCurrentRank() { return currentRank; }
        public void setCurrentRank(int currentRank) { this.currentRank = currentRank; }
        public int getPointsThisSeason() { return pointsThisSeason; }
        public void setPointsThisSeason(int pointsThisSeason) { this.pointsThisSeason = pointsThisSeason; }
        public Map<GoalTypeEnum, Integer> getCompletedMissionsByType() { return completedMissionsByType; }
        public void setCompletedMissionsByType(Map<GoalTypeEnum, Integer> completedMissionsByType) {
            this.completedMissionsByType = completedMissionsByType;
        }
    }

    // Season Statistics
    public static class SeasonStatistics {
        private int totalCompetitions;
        private int activeCompetitions;
        private int completedCompetitions;
        private int totalPointsDistributed;
        private Map<String, Integer> topClubs; // Club name -> Points
        private Map<GoalTypeEnum, Integer> competitionsByType;

        public SeasonStatistics() {
            this.topClubs = new LinkedHashMap<>();
            this.competitionsByType = new HashMap<>();
        }

        // Getters and setters
        public int getTotalCompetitions() { return totalCompetitions; }
        public void setTotalCompetitions(int totalCompetitions) { this.totalCompetitions = totalCompetitions; }
        public int getActiveCompetitions() { return activeCompetitions; }
        public void setActiveCompetitions(int activeCompetitions) { this.activeCompetitions = activeCompetitions; }
        public int getCompletedCompetitions() { return completedCompetitions; }
        public void setCompletedCompetitions(int completedCompetitions) { this.completedCompetitions = completedCompetitions; }
        public int getTotalPointsDistributed() { return totalPointsDistributed; }
        public void setTotalPointsDistributed(int totalPointsDistributed) { this.totalPointsDistributed = totalPointsDistributed; }
        public Map<String, Integer> getTopClubs() { return topClubs; }
        public void setTopClubs(Map<String, Integer> topClubs) { this.topClubs = topClubs; }
        public Map<GoalTypeEnum, Integer> getCompetitionsByType() { return competitionsByType; }
        public void setCompetitionsByType(Map<GoalTypeEnum, Integer> competitionsByType) {
            this.competitionsByType = competitionsByType;
        }
    }

    // Competition Statistics
    public static class CompetitionStatistics {
        private int participatingClubs;
        private int completedByClubs;
        private double averageProgress;
        private double completionRate;
        private List<ClubProgress> clubProgressList;

        public CompetitionStatistics() {
            this.clubProgressList = new ArrayList<>();
        }

        // Inner class for club progress
        public static class ClubProgress {
            private String clubName;
            private int progress;
            private boolean isCompleted;

            public ClubProgress(String clubName, int progress, boolean isCompleted) {
                this.clubName = clubName;
                this.progress = progress;
                this.isCompleted = isCompleted;
            }

            public String getClubName() { return clubName; }
            public int getProgress() { return progress; }
            public boolean isCompleted() { return isCompleted; }
        }

        // Getters and setters
        public int getParticipatingClubs() { return participatingClubs; }
        public void setParticipatingClubs(int participatingClubs) { this.participatingClubs = participatingClubs; }
        public int getCompletedByClubs() { return completedByClubs; }
        public void setCompletedByClubs(int completedByClubs) { this.completedByClubs = completedByClubs; }
        public double getAverageProgress() { return averageProgress; }
        public void setAverageProgress(double averageProgress) { this.averageProgress = averageProgress; }
        public double getCompletionRate() { return completionRate; }
        public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }
        public List<ClubProgress> getClubProgressList() { return clubProgressList; }
        public void setClubProgressList(List<ClubProgress> clubProgressList) { this.clubProgressList = clubProgressList; }
    }

    // 1. Get statistics for a specific club
    public ClubStatistics getClubStatistics(int clubId) throws SQLException {
        ClubStatistics stats = new ClubStatistics();
        Club club = clubService.getById(clubId);

        if (club == null) {
            return null;
        }

        stats.setTotalPoints(club.getPoints());

        // Get mission progress statistics
        List<MissionProgress> missionProgressList = missionProgressService.findByClub(clubId);
        int completedMissions = 0;

        for (MissionProgress progress : missionProgressList) {
            if (progress.getIsCompleted()) {
                completedMissions++;
                GoalTypeEnum type = progress.getCompetition().getGoalType();
                stats.getCompletedMissionsByType().merge(type, 1, Integer::sum);
            }
        }

        stats.setCompletedMissions(completedMissions);
        stats.setTotalMissions(missionProgressList.size());
        stats.setCompletionRate(missionProgressList.size() > 0 ?
                (double) completedMissions / missionProgressList.size() * 100 : 0.0);

        // Get current rank
        stats.setCurrentRank(getClubRank(clubId));

        // Get points for current season
        stats.setPointsThisSeason(getClubPointsForCurrentSeason(clubId));

        return stats;
    }

    // 2. Get statistics for a specific season
    public SeasonStatistics getSeasonStatistics(int seasonId) throws SQLException {
        SeasonStatistics stats = new SeasonStatistics();
        Saison season = saisonService.getById(seasonId);

        if (season == null) {
            return null;
        }

        // Get competitions for this season
        String competitionSql = "SELECT * FROM competition WHERE saison_id = ?";
        List<Competition> competitions = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(competitionSql)) {
            ps.setInt(1, seasonId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Competition comp = extractCompetition(rs);
                    competitions.add(comp);
                }
            }
        }

        int activeCount = 0;
        int completedCount = 0;
        int totalPoints = 0;

        for (Competition comp : competitions) {
            if ("activated".equals(comp.getStatus())) {
                activeCount++;
            } else if ("deactivated".equals(comp.getStatus()) &&
                    comp.getEndDate() != null &&
                    LocalDateTime.now().isAfter(comp.getEndDate())) {
                completedCount++;
            }
            totalPoints += comp.getPoints();

            GoalTypeEnum type = comp.getGoalType();
            stats.getCompetitionsByType().merge(type, 1, Integer::sum);
        }

        stats.setTotalCompetitions(competitions.size());
        stats.setActiveCompetitions(activeCount);
        stats.setCompletedCompetitions(completedCount);
        stats.setTotalPointsDistributed(totalPoints);

        // Get top clubs for this season
        stats.setTopClubs(getTopClubsForSeason(seasonId, 5));

        return stats;
    }

    // 3. Get statistics for a specific competition
    public CompetitionStatistics getCompetitionStatistics(int competitionId) throws SQLException {
        CompetitionStatistics stats = new CompetitionStatistics();
        Competition competition = competitionService.findById(competitionId);

        if (competition == null) {
            return null;
        }

        List<MissionProgress> progressList = missionProgressService.findByCompetition(competitionId);
        int completedCount = 0;
        double totalProgress = 0;

        for (MissionProgress progress : progressList) {
            Club club = clubService.getById(progress.getClubId());
            if (club != null) {
                stats.getClubProgressList().add(new CompetitionStatistics.ClubProgress(
                        club.getNomC(),
                        progress.getProgress(),
                        progress.getIsCompleted()
                ));
            }

            if (progress.getIsCompleted()) {
                completedCount++;
            }

            double progressPercentage = competition.getGoalValue() > 0 ?
                    (double) progress.getProgress() / competition.getGoalValue() * 100 : 0;
            totalProgress += progressPercentage;
        }

        stats.setParticipatingClubs(progressList.size());
        stats.setCompletedByClubs(completedCount);
        stats.setAverageProgress(progressList.size() > 0 ? totalProgress / progressList.size() : 0);
        stats.setCompletionRate(progressList.size() > 0 ?
                (double) completedCount / progressList.size() * 100 : 0);

        return stats;
    }

    // 4. Get leaderboard (top N clubs by points)
    public List<Club> getLeaderboard(int limit) throws SQLException {
        List<Club> leaderboard = new ArrayList<>();
        String sql = "SELECT * FROM club ORDER BY points DESC LIMIT ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Club club = mapResultSetToClub(rs);
                    leaderboard.add(club);
                }
            }
        }

        return leaderboard;
    }

    // 5. Get active missions summary
    public Map<String, Object> getActiveMissionsSummary() throws SQLException {
        Map<String, Object> summary = new HashMap<>();

        List<Competition> activeCompetitions = competitionService.getActiveCompetitions();
        summary.put("totalActiveMissions", activeCompetitions.size());

        Map<GoalTypeEnum, Integer> missionsByType = new HashMap<>();
        int totalPoints = 0;

        for (Competition comp : activeCompetitions) {
            missionsByType.merge(comp.getGoalType(), 1, Integer::sum);
            totalPoints += comp.getPoints();
        }

        summary.put("missionsByType", missionsByType);
        summary.put("totalPointsAvailable", totalPoints);

        return summary;
    }

    // Helper methods
    private int getClubRank(int clubId) throws SQLException {
        String sql = "SELECT COUNT(*) + 1 AS rank FROM club WHERE points > " +
                "(SELECT points FROM club WHERE id = ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clubId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("rank");
                }
            }
        }

        return 0;
    }

    private int getClubPointsForCurrentSeason(int clubId) throws SQLException {
        // Get the current season (the one with the latest end date)
        String currentSeasonSql = "SELECT id FROM saison ORDER BY date_fin DESC LIMIT 1";
        int currentSeasonId = 0;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(currentSeasonSql)) {
            if (rs.next()) {
                currentSeasonId = rs.getInt("id");
            }
        }

        if (currentSeasonId == 0) {
            return 0;
        }

        // Get points from completed missions in current season
        String sql = "SELECT SUM(c.points) as total_points " +
                "FROM mission_progress mp " +
                "JOIN competition c ON mp.competition_id = c.id " +
                "WHERE mp.club_id = ? AND mp.is_completed = true " +
                "AND c.saison_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clubId);
            ps.setInt(2, currentSeasonId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total_points");
                }
            }
        }

        return 0;
    }

    private Map<String, Integer> getTopClubsForSeason(int seasonId, int limit) throws SQLException {
        Map<String, Integer> topClubs = new LinkedHashMap<>();

        String sql = "SELECT cl.nom_c, SUM(c.points) as total_points " +
                "FROM mission_progress mp " +
                "JOIN competition c ON mp.competition_id = c.id " +
                "JOIN club cl ON mp.club_id = cl.id " +
                "WHERE mp.is_completed = true AND c.saison_id = ? " +
                "GROUP BY cl.id, cl.nom_c " +
                "ORDER BY total_points DESC " +
                "LIMIT ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, seasonId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    topClubs.put(rs.getString("nom_c"), rs.getInt("total_points"));
                }
            }
        }

        return topClubs;
    }

    // Helper method to extract Competition from ResultSet
    private Competition extractCompetition(ResultSet rs) throws SQLException {
        Competition c = new Competition();
        c.setId(rs.getInt("id"));
        c.setNomComp(rs.getString("nom_comp"));
        c.setDescComp(rs.getString("desc_comp"));
        c.setPoints(rs.getInt("points"));

        java.sql.Timestamp startTimestamp = rs.getTimestamp("start_date");
        if (startTimestamp != null) {
            c.setStartDate(startTimestamp.toLocalDateTime());
        }

        java.sql.Timestamp endTimestamp = rs.getTimestamp("end_date");
        if (endTimestamp != null) {
            c.setEndDate(endTimestamp.toLocalDateTime());
        }

        String goalType = rs.getString("goal_type");
        if (goalType != null) {
            try {
                c.setGoalType(GoalTypeEnum.valueOf(goalType));
            } catch (IllegalArgumentException e) {
                c.setGoalType(GoalTypeEnum.EVENT_COUNT);
            }
        } else {
            c.setGoalType(GoalTypeEnum.EVENT_COUNT);
        }

        c.setGoalValue(rs.getInt("goal"));
        c.setStatus(rs.getString("status"));

        int saisonId = rs.getInt("saison_id");
        if (!rs.wasNull()) {
            Saison saison = new Saison();
            saison.setId(saisonId);
            c.setSaisonId(saison);
        }

        return c;
    }

    // Helper method to map ResultSet to Club
    private Club mapResultSetToClub(ResultSet rs) throws SQLException {
        Club club = new Club();
        club.setId(rs.getInt("id"));
        club.setPresidentId(rs.getInt("president_id"));
        club.setNomC(rs.getString("nom_c"));
        club.setDescription(rs.getString("description"));
        club.setStatus(rs.getString("status"));
        club.setImage(rs.getString("image"));
        club.setPoints(rs.getInt("points"));
        return club;
    }

    // 6. Get club performance over time
    public List<Map<String, Object>> getClubPerformanceOverTime(int clubId, int numberOfSeasons) throws SQLException {
        List<Map<String, Object>> performance = new ArrayList<>();

        String sql = "SELECT s.nom_saison, " +
                "COUNT(CASE WHEN mp.is_completed = true THEN 1 END) as completed_missions, " +
                "COUNT(mp.id) as total_missions, " +
                "COALESCE(SUM(CASE WHEN mp.is_completed = true THEN c.points ELSE 0 END), 0) as points_earned " +
                "FROM saison s " +
                "LEFT JOIN competition c ON c.saison_id = s.id " +
                "LEFT JOIN mission_progress mp ON mp.competition_id = c.id AND mp.club_id = ? " +
                "GROUP BY s.id, s.nom_saison " +
                "ORDER BY s.date_fin DESC " +
                "LIMIT ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clubId);
            ps.setInt(2, numberOfSeasons);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> seasonPerformance = new HashMap<>();
                    seasonPerformance.put("seasonName", rs.getString("nom_saison"));
                    seasonPerformance.put("completedMissions", rs.getInt("completed_missions"));
                    seasonPerformance.put("totalMissions", rs.getInt("total_missions"));
                    seasonPerformance.put("pointsEarned", rs.getInt("points_earned"));
                    performance.add(seasonPerformance);
                }
            }
        }

        return performance;
    }

    // 7. Get competition completion trends
    public Map<GoalTypeEnum, Double> getCompetitionCompletionTrends() throws SQLException {
        Map<GoalTypeEnum, Double> trends = new HashMap<>();

        String sql = "SELECT c.goal_type, " +
                "COUNT(CASE WHEN mp.is_completed = true THEN 1 END) * 100.0 / COUNT(*) as completion_rate " +
                "FROM competition c " +
                "JOIN mission_progress mp ON mp.competition_id = c.id " +
                "GROUP BY c.goal_type";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String goalTypeStr = rs.getString("goal_type");
                if (goalTypeStr != null) {
                    try {
                        GoalTypeEnum goalType = GoalTypeEnum.valueOf(goalTypeStr);
                        trends.put(goalType, rs.getDouble("completion_rate"));
                    } catch (IllegalArgumentException e) {
                        // Skip invalid goal types
                    }
                }
            }
        }

        return trends;
    }
}
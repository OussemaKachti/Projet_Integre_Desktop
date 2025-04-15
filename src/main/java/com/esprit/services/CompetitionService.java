package com.esprit.services;

import com.esprit.models.Competition;
import com.esprit.models.Saison;
import com.esprit.utils.DatabaseConnection;
import com.esprit.models.enums.GoalTypeEnum;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CompetitionService implements IService<Competition> {

    private final Connection connection;

    public CompetitionService() {
        this.connection = DatabaseConnection.getInstance().getCnx();
    }

    @Override
    public void add(Competition competition) throws SQLException {
        String sql = "INSERT INTO competition (title, description, points, startDate, endDate, goalType, goalValue, saison) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, competition.getTitle());
            ps.setString(2, competition.getDescription());
            ps.setInt(3, competition.getPoints());
            ps.setTimestamp(4, Timestamp.valueOf(competition.getStartDate()));
            ps.setTimestamp(5, Timestamp.valueOf(competition.getEndDate()));
            ps.setString(6, competition.getGoalType().name());
            ps.setInt(7, competition.getGoalValue());
            ps.setInt(8, competition.getSaison().getId());

            ps.executeUpdate();
        }
    }

    @Override
    public void update(Competition competition) throws SQLException {
        String sql = "UPDATE competition SET title = ?, description = ?, points = ?, startDate = ?, endDate = ?, " +
                "goalType = ?, goalValue = ?, saison = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, competition.getTitle());
            ps.setString(2, competition.getDescription());
            ps.setInt(3, competition.getPoints());
            ps.setTimestamp(4, Timestamp.valueOf(competition.getStartDate()));
            ps.setTimestamp(5, Timestamp.valueOf(competition.getEndDate()));
            ps.setString(6, competition.getGoalType().name());
            ps.setInt(7, competition.getGoalValue());
            ps.setInt(8, competition.getSaison().getId());
            ps.setInt(9, competition.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM competition WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
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
                competitions.add(c);
            }
        }

        return competitions;
    }

    @Override
    public Competition findById(int id) throws SQLException {
        String sql = "SELECT * FROM competition WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractCompetition(rs);
                }
            }
        }

        return null;
    }

    private Competition extractCompetition(ResultSet rs) throws SQLException {
        Competition c = new Competition();
        c.setId(rs.getInt("id"));
        c.setTitle(rs.getString("title"));
        c.setDescription(rs.getString("description"));
        c.setPoints(rs.getInt("points"));
        c.setStartDate(rs.getTimestamp("startDate").toLocalDateTime());
        c.setEndDate(rs.getTimestamp("endDate").toLocalDateTime());
        c.setGoalType(GoalTypeEnum.valueOf(rs.getString("goalType")));
        c.setGoalValue(rs.getInt("goalValue"));

        // Mocked Saison with only id (you can improve this with a SaisonService)
        Saison saison = new Saison();
        saison.setId(rs.getInt("saison"));
        c.setSaison(saison);

        return c;
    }
}
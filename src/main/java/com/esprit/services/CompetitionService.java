package com.esprit.services;

import com.esprit.models.Competition;
import com.esprit.models.Saison;
import com.esprit.utils.DataSource;
import com.esprit.models.enums.GoalTypeEnum;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CompetitionService implements IServiceYassine<Competition> {

    private final Connection connection;

    public CompetitionService() {
        this.connection = DataSource.getInstance().getCnx();
    }

    @Override
    public void add(Competition competition) throws SQLException {
        String sql = "INSERT INTO competition (nom_comp, desc_comp, points, start_date, end_date, goal_type, goal_value, saison_id) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, competition.getNomComp());
            ps.setString(2, competition.getDescComp());
            ps.setInt(3, competition.getPoints());
            ps.setTimestamp(4, Timestamp.valueOf(competition.getStartDate()));
            ps.setTimestamp(5, Timestamp.valueOf(competition.getEndDate()));
            ps.setString(6, competition.getGoalType().name());
            ps.setInt(7, competition.getGoalValue());
            ps.setInt(8, competition.getSaisonId().getId());

            ps.executeUpdate();
        }
    }

    @Override
    public void update(Competition competition) throws SQLException {
        String sql = "UPDATE competition SET nom_comp = ?, desc_comp = ?, points = ?, start_date = ?, end_date = ?, " +
                "goal_type = ?, goal_value = ?, saison_id = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, competition.getNomComp());
            ps.setString(2, competition.getDescComp());
            ps.setInt(3, competition.getPoints());
            ps.setTimestamp(4, Timestamp.valueOf(competition.getStartDate()));
            ps.setTimestamp(5, Timestamp.valueOf(competition.getEndDate()));
            ps.setString(6, competition.getGoalType().name());
            ps.setInt(7, competition.getGoalValue());
            ps.setInt(8, competition.getSaisonId().getId());
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

        // Handle goal_type and goal_value columns that might not exist
        try {
            String goalType = rs.getString("goal_type");
            if (goalType != null) {
                c.setGoalType(GoalTypeEnum.fromString(goalType));
            } else {
                c.setGoalType(GoalTypeEnum.NONE); // Provide a default value
            }
        } catch (SQLException e) {
            // Column doesn't exist, set a default
            c.setGoalType(GoalTypeEnum.NONE);
        }

        try {
            c.setGoalValue(rs.getInt("goal_value"));
        } catch (SQLException e) {
            // Column doesn't exist, set a default value of 0
            c.setGoalValue(0);
        }

        // Mocked Saison with only id
        try {
            Saison saison = new Saison();
            saison.setId(rs.getInt("saison_id"));
            c.setSaisonId(saison);
        } catch (SQLException e) {
            // If saison_id doesn't exist, set null
            c.setSaisonId(null);
        }

        return c;
    }

}
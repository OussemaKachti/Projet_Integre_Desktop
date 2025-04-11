package com.esprit.services;

import com.esprit.models.Club;
import com.esprit.models.Participant;
import com.esprit.models.User;
import com.esprit.utils.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipantService {
    private Connection conn;
    private static ParticipantService instance;

    public ParticipantService() {
        conn = DataSource.getInstance().getCnx();
    }

    public static ParticipantService getInstance() {
        if (instance == null) {
            instance = new ParticipantService();
        }
        return instance;
    }

    // Add a new participant request (User wants to join a club)
    public void add(Participant participant) throws SQLException {
        String query = "INSERT INTO participant (user_id, club_id, status) VALUES (?, ?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, participant.getUser().getId());
            pst.setInt(2, participant.getClub().getId());
            pst.setString(3, participant.getStatus()); // "pending", "accepted", "rejected"

            pst.executeUpdate();

            // Retrieve generated ID for participant if necessary
            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    participant.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    // Get a participant request by its ID
    public Participant getById(int id) throws SQLException {
        String query = "SELECT * FROM participant WHERE id = ?";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToParticipant(rs);
                }
            }
        }
        return null;
    }

    // Get all participants for a specific club
    public List<Participant> getAllByClub(int clubId) throws SQLException {
        List<Participant> participants = new ArrayList<>();
        String query = "SELECT * FROM participant WHERE club_id = ?";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setInt(1, clubId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    participants.add(mapResultSetToParticipant(rs));
                }
            }
        }
        return participants;
    }

    // Accept or reject a participant request (Admin decision)
    public void updateStatus(int participantId, String status) throws SQLException {
        String query = "UPDATE participant SET status = ? WHERE id = ?";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, status); // "accepted", "rejected"
            pst.setInt(2, participantId);
            pst.executeUpdate();
        }
    }

    // Map the ResultSet to a Participant object
    private Participant mapResultSetToParticipant(ResultSet rs) throws SQLException {
        Participant participant = new Participant();
        participant.setId(rs.getInt("id"));

        // Get user details
        int userId = rs.getInt("user_id");
        UserService userService = UserService.getInstance();
        User user = userService.getById(userId);
        participant.setUser(user);

        // Get club details
        int clubId = rs.getInt("club_id");
        ClubService clubService = ClubService.getInstance();
        Club club = clubService.getById(clubId);
        participant.setClub(club);

        // Set the status of the participant (pending, accepted, or rejected)
        participant.setStatus(rs.getString("status"));

        return participant;
    }

    // Delete a participant request
    public void delete(int participantId) throws SQLException {
        String query = "DELETE FROM participant WHERE id = ?";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setInt(1, participantId);
            pst.executeUpdate();
        }
    }
}


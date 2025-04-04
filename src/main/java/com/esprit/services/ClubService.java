package com.esprit.services;

import com.esprit.models.Club;
import com.esprit.utils.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClubService {
    private Connection connection;
    private static ClubService instance;

    ClubService() {
        connection = DataSource.getInstance().getCnx();
    }

    public static ClubService getInstance() {
        if (instance == null) {
            instance = new ClubService();
        }
        return instance;
    }

    public void add(Club club) throws SQLException {
        String query = "INSERT INTO club (nom, description, logo, date_creation, president_id, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, club.getNom());
            pst.setString(2, club.getDescription());
            pst.setString(3, club.getLogo());
            pst.setTimestamp(4, Timestamp.valueOf(club.getDateCreation()));
            pst.setInt(5, club.getPresident().getId());
            pst.setString(6, club.getStatus());
            
            pst.executeUpdate();
            
            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    club.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public Club getById(int id) throws SQLException {
        String query = "SELECT * FROM club WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToClub(rs);
                }
            }
        }
        return null;
    }

    public List<Club> getAll() throws SQLException {
        List<Club> clubs = new ArrayList<>();
        String query = "SELECT * FROM club";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                clubs.add(mapResultSetToClub(rs));
            }
        }
        return clubs;
    }

    public void update(Club club) throws SQLException {
        String query = "UPDATE club SET nom=?, description=?, logo=?, status=?, president_id=? WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, club.getNom());
            pst.setString(2, club.getDescription());
            pst.setString(3, club.getLogo());
            pst.setString(4, club.getStatus());
            pst.setInt(5, club.getPresident().getId());
            pst.setInt(6, club.getId());
            
            pst.executeUpdate();
        }
    }

    private Club mapResultSetToClub(ResultSet rs) throws SQLException {
        Club club = new Club();
        club.setId(rs.getInt("id"));
        club.setNom(rs.getString("nom"));
        club.setDescription(rs.getString("description"));
        club.setLogo(rs.getString("logo"));
        club.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
        club.setStatus(rs.getString("status"));
        
        // Get president
        UserService userService = UserService.getInstance();
        club.setPresident(userService.getById(rs.getInt("president_id")));
        
        return club;
    }
}
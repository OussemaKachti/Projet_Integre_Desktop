package com.esprit.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.esprit.models.Club;
import com.esprit.utils.DatabaseConnection;

public class ClubService {
    private Connection connection;
    private static ClubService instance;

    public ClubService() {
        connection = DatabaseConnection.getInstance();
    }
    
    public static ClubService getInstance() {
        if (instance == null) {
            instance = new ClubService();
        }
        return instance;
    }

    public List<Club> getAll() {
        List<Club> clubs = new ArrayList<>();
        String query = "SELECT * FROM club";
        
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Club club = mapResultSetToClub(rs);
                clubs.add(club);
            }
        } catch (SQLException e) {
            System.err.println("Error getting clubs: " + e.getMessage());
            e.printStackTrace();
        }
        
        return clubs;
    }

    public Club getById(int id) {
        String query = "SELECT * FROM club WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToClub(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting club by ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    public List<Club> findByPresident(int presidentId) {
        List<Club> clubs = new ArrayList<>();
        String query = "SELECT * FROM club WHERE president_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, presidentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Club club = mapResultSetToClub(rs);
                    clubs.add(club);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding clubs by president: " + e.getMessage());
            e.printStackTrace();
        }
        
        return clubs;
    }

    public Club findFirstByPresident(int presidentId) {
        String query = "SELECT * FROM club WHERE president_id = ? LIMIT 1";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, presidentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToClub(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding first club by president: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    private Club mapResultSetToClub(ResultSet rs) throws SQLException {
        Club club = new Club();
        club.setId(rs.getInt("id"));
        club.setNom(rs.getString("nom"));
        club.setDescription(rs.getString("description"));
        
        // Handle date_creation that might be null
        java.sql.Date dateCreation = rs.getDate("date_creation");
        if (dateCreation != null) {
            club.setDateCreation(dateCreation.toLocalDate());
        }
        
        // Map the logo field
        club.setLogo(rs.getString("logo"));
        
        // Map the points field
        club.setPoints(rs.getInt("points"));
        
        return club;
    }
} 
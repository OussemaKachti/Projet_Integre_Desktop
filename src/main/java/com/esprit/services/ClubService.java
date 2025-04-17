package com.esprit.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.esprit.models.Club;
import com.esprit.utils.DatabaseConnection;

public class ClubService {

    private final Connection cnx;
    private static ClubService instance;

    // Constructeur qui initialise la connexion via le singleton DatabaseConnection
    public ClubService() {
        cnx = DatabaseConnection.getInstance();
    }
    
    public static ClubService getInstance() {
        if (instance == null) {
            instance = new ClubService();
        }
        return instance;
    }

    // Ajouter un club
    public void ajouter(Club club) {
        String query = "INSERT INTO club (president_id, nom_c, description, status, image, points) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = cnx.prepareStatement(query)) {
            stmt.setInt(1, club.getPresidentId());
            stmt.setString(2, club.getNomC());
            stmt.setString(3, club.getDescription());
            stmt.setString(4, club.getStatus());
            stmt.setString(5, club.getImage());
            stmt.setInt(6, club.getPoints());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Modifier un club
    public void modifier(Club club) {
        String query = "UPDATE club SET president_id = ?, nom_c = ?, description = ?, status = ?, image = ?, points = ? WHERE id = ?";

        try (PreparedStatement stmt = cnx.prepareStatement(query)) {
            stmt.setInt(1, club.getPresidentId());
            stmt.setString(2, club.getNomC());
            stmt.setString(3, club.getDescription());
            stmt.setString(4, club.getStatus());
            stmt.setString(5, club.getImage());
            stmt.setInt(6, club.getPoints());
            stmt.setInt(7, club.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Supprimer un club
    public void supprimer(int id) {
        String query = "DELETE FROM club WHERE id = ?";

        try (PreparedStatement stmt = cnx.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Afficher tous les clubs
    public List<Club> afficher() {
        List<Club> clubs = new ArrayList<>();
        String query = "SELECT * FROM club";

        try (Statement stmt = cnx.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Club club = new Club();
                club.setId(rs.getInt("id"));
                club.setPresidentId(rs.getInt("president_id"));
                club.setNomC(rs.getString("nom_c"));
                club.setDescription(rs.getString("description"));
                club.setStatus(rs.getString("status"));
                club.setImage(rs.getString("image"));
                club.setPoints(rs.getInt("points"));
                clubs.add(club);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return clubs;
    }

    // Afficher un club par ID
    public Club getClubById(int id) {
        Club club = null;
        String query = "SELECT * FROM club WHERE id = ?";

        try (PreparedStatement stmt = cnx.prepareStatement(query)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    club = new Club();
                    club.setId(rs.getInt("id"));
                    club.setPresidentId(rs.getInt("president_id"));
                    club.setNomC(rs.getString("nom_c"));
                    club.setDescription(rs.getString("description"));
                    club.setStatus(rs.getString("status"));
                    club.setImage(rs.getString("image"));
                    club.setPoints(rs.getInt("points"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return club;
    }

    // Get a club by ID (renamed method to match other callers)
    public Club getById(int id) {
        return getClubById(id);
    }
    
    // Find clubs by president ID
    public List<Club> findByPresident(int presidentId) {
        List<Club> clubs = new ArrayList<>();
        String query = "SELECT * FROM club WHERE president_id = ?";
        
        try (PreparedStatement stmt = cnx.prepareStatement(query)) {
            stmt.setInt(1, presidentId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Club club = new Club();
                club.setId(rs.getInt("id"));
                club.setPresidentId(rs.getInt("president_id"));
                club.setNomC(rs.getString("nom_c"));
                club.setDescription(rs.getString("description"));
                club.setStatus(rs.getString("status"));
                club.setImage(rs.getString("image"));
                club.setPoints(rs.getInt("points"));
                clubs.add(club);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return clubs;
    }
    
    // Get all clubs (renamed to match standard service pattern)
    public List<Club> getAll() {
        return afficher();
    }

    // Find the first club by president ID
    public Club findFirstByPresident(int presidentId) {
        List<Club> clubs = findByPresident(presidentId);
        if (clubs != null && !clubs.isEmpty()) {
            return clubs.get(0);
        }
        return null;
    }
    
    // Helper method to map result set to Club object
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
}

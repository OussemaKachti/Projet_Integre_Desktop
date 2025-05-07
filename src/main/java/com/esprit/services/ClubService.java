package com.esprit.services;

import com.esprit.models.Club;
import com.esprit.utils.DataSource;
import com.esprit.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClubService {

    private final Connection cnx;

    public ClubService() {
        this.cnx = DataSource.getInstance().getCnx();
    }


    public static ClubService getInstance() {
        return null;
    }

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

    public void supprimer(int id) {
        String query = "DELETE FROM club WHERE id = ?";

        try (PreparedStatement stmt = cnx.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

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
    public List<Club> findByPresident2(int presidentId) {
        List<Club> clubs = new ArrayList<>();
        String query = "SELECT * FROM club WHERE president_id = ?";

        try (PreparedStatement stmt = cnx.prepareStatement(query)) {
            stmt.setInt(1, presidentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String clubName = rs.getString("nom_c");
                int participationCount = rs.getInt("participation_count");
                stats.add(new Object[]{clubName, participationCount});
                System.out.println("Donnée brute: Club = " + clubName + ", Count = " + participationCount);
            }
        } catch (SQLException e) {
            System.err.println("SQLException in getClubsByPopularity: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des statistiques de popularité: " + e.getMessage());
        }

        return clubs;
    }

    public Club findByPresident(int presidentId) throws SQLException {
        String query = "SELECT * FROM club WHERE president_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, presidentId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToClub(rs);
                }
            }
        }
        return null;
    }

    // Get all clubs (renamed to match standard service pattern)
    public List<Club> getAll() {
        return afficher();
    }

    // Find the first club by president ID
    public Club findFirstByPresident(int presidentId) {
        List<Club> clubs = findByPresident2(presidentId);
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
    public List<Object[]> getClubsByPopularity() {
        List<Object[]> stats = new ArrayList<>();
        String query = "SELECT c.nom_c, " +
                "COUNT(CASE WHEN pm.statut = 'accepte' THEN pm.id ELSE NULL END) as participation_count " +
                "FROM club c " +
                "LEFT JOIN participation_membre pm ON c.id = pm.club_id " +
                "GROUP BY c.nom_c " +
                "ORDER BY participation_count DESC";

        try (PreparedStatement stmt = cnx.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String clubName = rs.getString("nom_c");
                int participationCount = rs.getInt("participation_count");
                stats.add(new Object[]{clubName, participationCount});
                System.out.println("Donnée brute: Club = " + clubName + ", Count = " + participationCount);
            }
        } catch (SQLException e) {
            System.err.println("SQLException in getClubsByPopularity: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des statistiques de popularité: " + e.getMessage());
        }

        System.out.println("Total données récupérées dans getClubsByPopularity : " + stats.size());
        return stats;
    }
}
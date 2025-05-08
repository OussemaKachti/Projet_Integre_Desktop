package com.esprit.services;

import com.esprit.models.Club;
import com.esprit.models.User;
import com.esprit.models.enums.RoleEnum;
import com.esprit.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClubService {

    private final Connection cnx;

    public ClubService() {
        this.cnx = DataSource.getInstance().getCnx();
    }

    public static ClubService getInstance() {
        return new ClubService(); // Fixed to return a new instance
    }

    public Connection getConnection() {
        return cnx;
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
            throw new RuntimeException("Erreur lors de l'ajout du club: " + e.getMessage());
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
            throw new RuntimeException("Erreur lors de la modification du club: " + e.getMessage());
        }
    }

    public void supprimer(int id) {
        String query = "DELETE FROM club WHERE id = ?";

        try (PreparedStatement stmt = cnx.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la suppression du club: " + e.getMessage());
        }
    }

    public List<Club> afficher() {
        List<Club> clubs = new ArrayList<>();
        String clubQuery = "SELECT * FROM club";
        String userQuery = "SELECT * FROM user WHERE id = ?";

        try (Statement stmt = cnx.createStatement(); ResultSet rs = stmt.executeQuery(clubQuery)) {
            while (rs.next()) {
                Club club = mapResultSetToClub(rs);

                // Fetch the president (User) for this club
                try (PreparedStatement userStmt = cnx.prepareStatement(userQuery)) {
                    userStmt.setInt(1, club.getPresidentId());
                    try (ResultSet userRs = userStmt.executeQuery()) {
                        if (userRs.next()) {
                            User president = mapResultSetToUser(userRs);
                            club.setPresident(president);
                            System.out
                                    .println("President found for club " + club.getId() + ": " + president.getEmail());
                        } else {
                            System.out.println("No president found for club " + club.getId() + " with president_id = "
                                    + club.getPresidentId());
                        }
                    }
                } catch (SQLException userEx) {
                    System.err
                            .println("Error fetching president for club " + club.getId() + ": " + userEx.getMessage());
                    userEx.printStackTrace();
                }

                clubs.add(club);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des clubs: " + e.getMessage());
        }
        return clubs;
    }

    public Club getClubById(int id) {
        Club club = null;
        String clubQuery = "SELECT * FROM club WHERE id = ?";
        String userQuery = "SELECT * FROM user WHERE id = ?";

        try (PreparedStatement stmt = cnx.prepareStatement(clubQuery)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    club = mapResultSetToClub(rs);

                    // Fetch the president (User) for this club
                    try (PreparedStatement userStmt = cnx.prepareStatement(userQuery)) {
                        userStmt.setInt(1, club.getPresidentId());
                        try (ResultSet userRs = userStmt.executeQuery()) {
                            if (userRs.next()) {
                                User president = mapResultSetToUser(userRs);
                                club.setPresident(president);
                                System.out.println(
                                        "President found for club " + club.getId() + ": " + president.getEmail());
                            } else {
                                System.out.println("No president found for club " + club.getId()
                                        + " with president_id = " + club.getPresidentId());
                            }
                        }
                    } catch (SQLException userEx) {
                        System.err.println(
                                "Error fetching president for club " + club.getId() + ": " + userEx.getMessage());
                        userEx.printStackTrace();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération du club: " + e.getMessage());
        }
        return club;
    }

    public List<Club> findByPresident2(int presidentId) {
        List<Club> clubs = new ArrayList<>();
        String clubQuery = "SELECT * FROM club WHERE president_id = ?";
        String userQuery = "SELECT * FROM user WHERE id = ?";

        try (PreparedStatement stmt = cnx.prepareStatement(clubQuery)) {
            stmt.setInt(1, presidentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Club club = mapResultSetToClub(rs);

                    // Fetch the president (User) for this club
                    try (PreparedStatement userStmt = cnx.prepareStatement(userQuery)) {
                        userStmt.setInt(1, club.getPresidentId());
                        try (ResultSet userRs = userStmt.executeQuery()) {
                            if (userRs.next()) {
                                User president = mapResultSetToUser(userRs);
                                club.setPresident(president);
                                System.out.println(
                                        "President found for club " + club.getId() + ": " + president.getEmail());
                            } else {
                                System.out.println("No president found for club " + club.getId()
                                        + " with president_id = " + club.getPresidentId());
                            }
                        }
                    } catch (SQLException userEx) {
                        System.err.println(
                                "Error fetching president for club " + club.getId() + ": " + userEx.getMessage());
                        userEx.printStackTrace();
                    }

                    clubs.add(club);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la recherche des clubs: " + e.getMessage());
        }
        return clubs;
    }

    public Club findByPresident(int presidentId) throws SQLException {
        String clubQuery = "SELECT * FROM club WHERE president_id = ?";
        String userQuery = "SELECT * FROM user WHERE id = ?";

        try (PreparedStatement pst = cnx.prepareStatement(clubQuery)) {
            pst.setInt(1, presidentId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Club club = mapResultSetToClub(rs);

                    // Fetch the president (User) for this club
                    try (PreparedStatement userStmt = cnx.prepareStatement(userQuery)) {
                        userStmt.setInt(1, club.getPresidentId());
                        try (ResultSet userRs = userStmt.executeQuery()) {
                            if (userRs.next()) {
                                User president = mapResultSetToUser(userRs);
                                club.setPresident(president);
                                System.out.println(
                                        "President found for club " + club.getId() + ": " + president.getEmail());
                            } else {
                                System.out.println("No president found for club " + club.getId()
                                        + " with president_id = " + club.getPresidentId());
                            }
                        }
                    } catch (SQLException userEx) {
                        System.err.println(
                                "Error fetching president for club " + club.getId() + ": " + userEx.getMessage());
                        userEx.printStackTrace();
                    }

                    return club;
                }
            }
        }
        return null;
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
                stats.add(new Object[] { clubName, participationCount });
                System.out.println("Donnée brute: Club = " + clubName + ", Count = " + participationCount);
            }
        } catch (SQLException e) {
            System.err.println("SQLException in getClubsByPopularity: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(
                    "Erreur lors de la récupération des statistiques de popularité: " + e.getMessage());
        }

        System.out.println("Total données récupérées dans getClubsByPopularity : " + stats.size());
        return stats;
    }

    public Club getById(int id) {
        return getClubById(id);
    }

    public Club findFirstByPresident(int presidentId) {
        List<Club> clubs = findByPresident2(presidentId);
        if (clubs != null && !clubs.isEmpty()) {
            return clubs.get(0);
        }
        return null;
    }

    public List<Club> getAll() {
        return afficher();
    }

    private Club mapResultSetToClub(ResultSet rs) throws SQLException {
        Club club = new Club();
        club.setId(rs.getInt("id"));
        club.setPresidentId(rs.getInt("president_id"));
        club.setNomC(rs.getString("nom_c"));
        club.setDescription(rs.getString("description"));
        club.setStatus(rs.getString("status"));
        club.setImage(rs.getString("image"));
        club.setPoints(rs.getInt("points"));
        System.out.println("Fetching club: ID = " + club.getId() + ", President ID = " + club.getPresidentId());
        return club;
    }

    private User mapResultSetToUser(ResultSet userRs) throws SQLException {
        User president = new User();
        president.setId(userRs.getInt("id"));
        president.setFirstName(userRs.getString("prenom"));
        president.setLastName(userRs.getString("nom"));
        president.setEmail(userRs.getString("email"));
        president.setPassword(userRs.getString("password"));
        president.setRole(RoleEnum.valueOf(userRs.getString("role")));
        president.setPhone(userRs.getString("tel"));
        president.setProfilePicture(userRs.getString("profile_picture"));
        president.setStatus(userRs.getString("status"));
        president.setVerified(userRs.getBoolean("is_verified"));
        president.setConfirmationToken(userRs.getString("confirmation_token"));
        president.setConfirmationTokenExpiresAt(userRs.getTimestamp("confirmation_token_expires_at") != null
                ? userRs.getTimestamp("confirmation_token_expires_at").toLocalDateTime()
                : null);
        president.setCreatedAt(
                userRs.getTimestamp("created_at") != null ? userRs.getTimestamp("created_at").toLocalDateTime() : null);
        president.setLastLoginAt(
                userRs.getTimestamp("last_login_at") != null ? userRs.getTimestamp("last_login_at").toLocalDateTime()
                        : null);
        president.setWarningCount(userRs.getInt("warning_count"));
        president.setVerificationAttempts(userRs.getInt("verification_attempts"));
        president.setLastCodeSentTime(userRs.getTimestamp("last_code_sent_time") != null
                ? userRs.getTimestamp("last_code_sent_time").toLocalDateTime()
                : null);
        return president;
    }
}
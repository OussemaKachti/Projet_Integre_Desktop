package com.esprit.services;

import com.esprit.models.Club;
import com.esprit.models.ParticipationMembre;
import com.esprit.models.User;
import com.esprit.utils.DataSource;

import java.sql.*;
<<<<<<< HEAD
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour gérer les participations des membres aux clubs
 */
public class ParticipationMembreService {
    private Connection connection;
    private static ParticipationMembreService instance;
    
    private final UserService userService = new UserService();
    private final ClubService clubService = new ClubService();
    
    public ParticipationMembreService() {
        connection = DataSource.getInstance().getCnx();
    }
    
    public static ParticipationMembreService getInstance() {
        if (instance == null) {
            instance = new ParticipationMembreService();
        }
        return instance;
    }
    
    /**
     * Ajoute une participation de membre
     * 
     * @param participation l'objet participation à ajouter
     * @throws SQLException en cas d'erreur SQL
     */
    public void add(ParticipationMembre participation) throws SQLException {
        String query = "INSERT INTO participation_membre (date_request, statut, user_id, club_id, description) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pst = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pst.setTimestamp(1, Timestamp.valueOf(participation.getDateRequest()));
            pst.setString(2, participation.getStatut());
            pst.setInt(3, participation.getUser().getId());
            pst.setInt(4, participation.getClub().getId());
            pst.setString(5, participation.getDescription());
            
            pst.executeUpdate();
            
            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                participation.setId(rs.getInt(1));
            }
        }
    }
    
    /**
     * Met à jour le statut d'une participation
     * 
     * @param participationId id de la participation
     * @param statut nouveau statut
     * @throws SQLException en cas d'erreur SQL
     */
    public void updateStatut(int participationId, String statut) throws SQLException {
        String query = "UPDATE participation_membre SET statut = ? WHERE id = ?";
        
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, statut);
            pst.setInt(2, participationId);
            
            pst.executeUpdate();
        }
    }
    
=======
import java.util.ArrayList;
import java.util.List;

public class ParticipationMembreService {

    private final Connection cnx;
    private final UserService userService = new UserService();
    private final ClubService clubService = new ClubService();

    public ParticipationMembreService() {
        this.cnx = DataSource.getInstance().getCnx();
    }

    public void ajouter(ParticipationMembre p) {
        String req = "INSERT INTO participation_membre(user_id, club_id, date_request, statut, description) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, p.getUser().getId());
            pst.setInt(2, p.getClub().getId());
            pst.setTimestamp(3, Timestamp.valueOf(p.getDateRequest()));
            pst.setString(4, p.getStatut());
            pst.setString(5, p.getDescription());
            pst.executeUpdate();
            System.out.println("Participant ajouté avec succès !");
        } catch (SQLException e) {
            System.err.println("Erreur d'ajout : " + e.getMessage());
        }
    }

    public void modifier(ParticipationMembre p) {
        String req = "UPDATE participation_membre SET user_id=?, club_id=?, date_request=?, statut=?, description=? WHERE id=?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, p.getId());
            pst.setInt(2, p.getClub().getId());
            pst.setTimestamp(3, Timestamp.valueOf(p.getDateRequest()));
            pst.setString(4, p.getStatut());
            pst.setString(5, p.getDescription());
            pst.setInt(6, p.getId());
            pst.executeUpdate();
            System.out.println("Participant modifié avec succès !");
        } catch (SQLException e) {
            System.err.println("Erreur de modification : " + e.getMessage());
        }
    }

    public boolean supprimer(int id) throws SQLException {
        String req = "DELETE FROM participation_membre WHERE id=?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, id);
            int rowsAffected = pst.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Participant supprimé avec succès !");
                return true;
            } else {
                System.out.println("Aucun participant trouvé avec l'ID: " + id);
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Erreur de suppression : " + e.getMessage());
            throw e;
        }
    }

    public List<ParticipationMembre> afficher() throws SQLException {
        List<ParticipationMembre> list = new ArrayList<>();
        String req = "SELECT pm.id, pm.user_id, pm.club_id, pm.date_request, pm.statut, pm.description, " +
                "c.id AS club_id, c.president_id, c.nom_c, c.description AS club_description, c.status, c.image, c.points, "
                +
                "u.nom AS user_name " +
                "FROM participation_membre pm " +
                "LEFT JOIN club c ON pm.club_id = c.id " +
                "LEFT JOIN user u ON pm.user_id = u.id"; // Adjust 'users' and 'nom' to match your schema
        try (PreparedStatement pst = cnx.prepareStatement(req);
                ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                ParticipationMembre p = new ParticipationMembre();
                p.setId(rs.getInt("id"));

                // Create User object
                User user = new User();
                user.setId(rs.getInt("user_id"));
                // Set name if available
                if (rs.getString("user_name") != null) {
                    user.setFirstName(rs.getString("user_name"));
                }
                p.setUser(user);

                // Populate the associated Club
                Club club = new Club();
                club.setId(rs.getInt("club_id"));
                club.setPresidentId(rs.getInt("president_id"));
                club.setNomC(rs.getString("nom_c"));
                club.setDescription(rs.getString("club_description"));
                club.setStatus(rs.getString("status"));
                club.setImage(rs.getString("image"));
                club.setPoints(rs.getInt("points"));
                p.setClub(club);

                p.setDateRequest(rs.getTimestamp("date_request").toLocalDateTime());
                p.setStatut(rs.getString("statut"));
                p.setDescription(rs.getString("description"));

                list.add(p);
            }

            System.out.println("Nombre de participants récupérés : " + list.size());
            return list;
        } catch (SQLException e) {
            System.err.println("Erreur d'affichage : " + e.getMessage());
            throw e;
        }
    }

    public List<ParticipationMembre> getAllParticipants() throws SQLException {
        return afficher();
    }

>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
    /**
     * Récupère les participations par club avec un statut donné
     * 
     * @param clubId id du club
     * @param statut statut des participations à récupérer
     * @return liste des participations
     * @throws SQLException en cas d'erreur SQL
     */
    public List<ParticipationMembre> getParticipationsByClubAndStatut(int clubId, String statut) throws SQLException {
        List<ParticipationMembre> participations = new ArrayList<>();
        String query = "SELECT * FROM participation_membre WHERE club_id = ? AND statut = ?";
<<<<<<< HEAD
        
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, clubId);
            pst.setString(2, statut);
            
=======

        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, clubId);
            pst.setString(2, statut);

>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    participations.add(mapResultSetToParticipation(rs));
                }
            }
        }
<<<<<<< HEAD
        
        return participations;
    }
    
=======

        return participations;
    }

>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
    /**
     * Récupère toutes les participations d'un club
     * 
     * @param clubId id du club
     * @return liste des participations
     * @throws SQLException en cas d'erreur SQL
     */
    public List<ParticipationMembre> getParticipationsByClub(int clubId) throws SQLException {
        List<ParticipationMembre> participations = new ArrayList<>();
        String query = "SELECT * FROM participation_membre WHERE club_id = ?";
<<<<<<< HEAD
        
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, clubId);
            
=======

        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, clubId);

>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    participations.add(mapResultSetToParticipation(rs));
                }
            }
        }
<<<<<<< HEAD
        
        return participations;
    }
    
=======

        return participations;
    }

>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
    /**
     * Vérifie si un utilisateur est membre d'un club
     * 
     * @param userId id de l'utilisateur
     * @param clubId id du club
     * @return true si l'utilisateur est membre du club
     * @throws SQLException en cas d'erreur SQL
     */
    public boolean isUserMemberOfClub(int userId, int clubId) throws SQLException {
        String query = "SELECT COUNT(*) FROM participation_membre WHERE user_id = ? AND club_id = ? AND statut = 'accepte'";
<<<<<<< HEAD
        
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, clubId);
            
=======

        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, clubId);

>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
<<<<<<< HEAD
        
        return false;
    }
    
    private ParticipationMembre mapResultSetToParticipation(ResultSet rs) throws SQLException {
        ParticipationMembre participation = new ParticipationMembre();
        participation.setId(rs.getInt("id"));
        
        Timestamp timestamp = rs.getTimestamp("date_request");
        participation.setDateRequest(timestamp.toLocalDateTime());
        
        participation.setStatut(rs.getString("statut"));
        participation.setDescription(rs.getString("description"));
        
        // Charger l'utilisateur et le club
        User user = userService.getById(rs.getInt("user_id"));
        Club club = clubService.getById(rs.getInt("club_id"));
        
        participation.setUser(user);
        participation.setClub(club);
        
        return participation;
    }
} 
=======

        return false;
    }

    private ParticipationMembre mapResultSetToParticipation(ResultSet rs) throws SQLException {
        ParticipationMembre participation = new ParticipationMembre();
        participation.setId(rs.getInt("id"));

        Timestamp timestamp = rs.getTimestamp("date_request");
        participation.setDateRequest(timestamp.toLocalDateTime());

        participation.setStatut(rs.getString("statut"));
        participation.setDescription(rs.getString("description"));

        // Charger l'utilisateur et le club
        User user = userService.getById(rs.getInt("user_id"));
        Club club = clubService.getById(rs.getInt("club_id"));

        participation.setUser(user);
        participation.setClub(club);

        return participation;
    }
}
>>>>>>> 63ffc7c6ff36402bf8d8bc0e437c1fe3d58b5b87

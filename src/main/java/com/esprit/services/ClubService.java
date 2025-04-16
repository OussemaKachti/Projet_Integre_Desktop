package com.esprit.services;

import com.esprit.models.Club;
import com.esprit.utils.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

public class ClubService {
    private Connection connection;
    private static ClubService instance;

    public ClubService() {
        connection = DataSource.getInstance().getCnx();
    }

    public static ClubService getInstance() {
        if (instance == null) {
            instance = new ClubService();
        }
        return instance;
    }

    public void add(Club club) throws SQLException {
        String query = "INSERT INTO club (nom_c, description, logo, date_creation, president_id, status,points) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, club.getNomC());
            pst.setString(2, club.getDescription());
            pst.setString(3, club.getLogo());
            pst.setTimestamp(4, Timestamp.valueOf(club.getDateCreation()));
            pst.setInt(5, club.getPresident().getId());
            pst.setString(6, club.getStatus());
            pst.setInt(7, club.getPoints());
            
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

    public Club findByPresident(int presidentId) throws SQLException {
        String query = "SELECT * FROM club WHERE president_id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, presidentId);
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
        String query = "UPDATE club SET nom_c=?, description=?, logo=?, points=?, status=?, president_id=? WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, club.getNomC());
            pst.setString(2, club.getDescription());
            pst.setString(3, club.getLogo());
            pst.setInt(4, club.getPoints());
            pst.setString(5, club.getStatus());
            pst.setInt(6, club.getPresident().getId());
            pst.setInt(7, club.getId());

            
            pst.executeUpdate();
        }
    }

    private Club mapResultSetToClub(ResultSet rs) throws SQLException {
        Club club = new Club();
        club.setId(rs.getInt("id"));
        
        // Adaptation aux noms de colonnes de Symfony
        try {
            // Essayer d'abord avec les noms de colonnes Java
            club.setNomC(rs.getString("nom"));
        } catch (SQLException e) {
            // Si échec, essayer avec les noms de colonnes Symfony
            club.setNomC(rs.getString("nom_c"));
        }
        
        try {
            club.setDescription(rs.getString("description"));
        } catch (SQLException e) {
            // Si échec, essayer avec les noms de colonnes Symfony
            club.setDescription(rs.getString("description_c"));
        }
        
        try {
            club.setLogo(rs.getString("logo"));
        } catch (SQLException e) {
            // Ignorer si la colonne n'existe pas
        }
        try {
            club.setPoints(rs.getInt("points"));
        } catch (SQLException e) {
            // Default to 0 if column doesn't exist
            club.setPoints(0);
        }
        
        try {
            club.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
        } catch (SQLException e) {
            // Si échec, essayer avec un autre format ou définir une date par défaut
            try {
                club.setDateCreation(rs.getDate("date_creation").toLocalDate().atStartOfDay());
            } catch (SQLException ex) {
                // Si toujours échec, utiliser la date actuelle
                club.setDateCreation(LocalDateTime.now());
            }
        }
        
        try {
            club.setStatus(rs.getString("status"));
        } catch (SQLException e) {
            // Ignorer si la colonne n'existe pas
        }
        
        // Get president - adapter au nom de colonne correct
        UserService userService = UserService.getInstance();
        int presidentId;
        try {
            presidentId = rs.getInt("president_id");
        } catch (SQLException e) {
            // Si échec, essayer d'autres noms possibles
            presidentId = rs.getInt("user_id");
        }
        
        club.setPresident(userService.getById(presidentId));
        
        return club;
    }
}
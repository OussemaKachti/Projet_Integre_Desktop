package com.esprit.services;

import com.esprit.models.Club;
import com.esprit.models.ParticipationMembre;
import com.esprit.models.User;
import com.esprit.utils.DataSource;

import java.sql.*;
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
        
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, clubId);
            pst.setString(2, statut);
            
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    participations.add(mapResultSetToParticipation(rs));
                }
            }
        }
        
        return participations;
    }
    
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
        
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, clubId);
            
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    participations.add(mapResultSetToParticipation(rs));
                }
            }
        }
        
        return participations;
    }
    
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
        
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, clubId);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        
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
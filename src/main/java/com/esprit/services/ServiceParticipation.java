package com.esprit.services;

import com.esprit.models.Participation_event;
import com.esprit.models.User;
import com.esprit.utils.DataSource;

import javax.xml.crypto.Data;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ServiceParticipation {
    private Connection cnx;

    public ServiceParticipation() {
        cnx = DataSource.getInstance().getCnx();
    }

    /**
     * Ajoute une nouvelle participation à un événement
     * 
     * @param participation La participation à ajouter
     * @return true si l'ajout est réussi, false sinon
     */
    public boolean ajouterParticipation(Participation_event participation) {
        try {
            String requete = "INSERT INTO participation_event (user_id, evenement_id, dateparticipation) VALUES (?, ?, ?)";
            PreparedStatement pst = cnx.prepareStatement(requete);
            pst.setLong(1, participation.getUser_id());
            pst.setLong(2, participation.getEvenement_id());
            pst.setDate(3, new java.sql.Date(participation.getDateparticipation().getTime()));

            int result = pst.executeUpdate();
            return result > 0;
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
            return false;
        }
    }

    /**
     * Vérifie si un utilisateur participe déjà à un événement
     * 
     * @param userId  ID de l'utilisateur
     * @param eventId ID de l'événement
     * @return true si l'utilisateur participe déjà, false sinon
     */
    public boolean participationExists(long userId, long eventId) {
        try {
            String requete = "SELECT * FROM participation_event WHERE user_id = ? AND evenement_id = ?";
            PreparedStatement pst = cnx.prepareStatement(requete);
            pst.setLong(1, userId);
            pst.setLong(2, eventId);

            ResultSet rs = pst.executeQuery();
            return rs.next(); // Si un résultat existe, l'utilisateur participe déjà
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
            return false;
        }
    }

    /**
     * Supprime la participation d'un utilisateur à un événement
     * 
     * @param userId  ID de l'utilisateur
     * @param eventId ID de l'événement
     * @return true si la suppression est réussie, false sinon
     */
    public boolean annulerParticipation(long userId, long eventId) {
        try {
            String requete = "DELETE FROM participation_event WHERE user_id = ? AND evenement_id = ?";
            PreparedStatement pst = cnx.prepareStatement(requete);
            pst.setLong(1, userId);
            pst.setLong(2, eventId);

            int result = pst.executeUpdate();
            return result > 0;
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
            return false;
        }
    }

    /**
     * Récupère toutes les participations d'un utilisateur
     * 
     * @param userId ID de l'utilisateur
     * @return Liste des participations de l'utilisateur
     */
    public List<Participation_event> getParticipationsByUser(long userId) {
        List<Participation_event> participations = new ArrayList<>();

        try {
            String requete = "SELECT * FROM participation_event WHERE user_id = ?";
            PreparedStatement pst = cnx.prepareStatement(requete);
            pst.setLong(1, userId);

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Participation_event p = new Participation_event();
                p.setUser_id(rs.getLong("user_id"));
                p.setEvenement_id(rs.getLong("evenement_id"));
                p.setDateparticipation(rs.getDate("dateparticipation"));

                participations.add(p);
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }

        return participations;
    }

    /**
     * Récupère toutes les participations à un événement
     * 
     * @param eventId ID de l'événement
     * @return Liste des participations à l'événement
     */
    public List<Participation_event> getParticipationsByEvent(long eventId) {
        List<Participation_event> participations = new ArrayList<>();

        try {
            String requete = "SELECT * FROM participation_event WHERE evenement_id = ?";
            PreparedStatement pst = cnx.prepareStatement(requete);
            pst.setLong(1, eventId);

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Participation_event p = new Participation_event();
                p.setUser_id(rs.getLong("user_id"));
                p.setEvenement_id(rs.getLong("evenement_id"));
                p.setDateparticipation(rs.getDate("dateparticipation"));

                participations.add(p);
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }

        return participations;
    }

    /**
     * Compte le nombre de participants à un événement
     * 
     * @param eventId ID de l'événement
     * @return Nombre de participants
     */
    public int countParticipants(long eventId) {
        try {
            String requete = "SELECT COUNT(*) as count FROM participation_event WHERE evenement_id = ?";
            PreparedStatement pst = cnx.prepareStatement(requete);
            pst.setLong(1, eventId);

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }

        return 0;
    }

    /**
     * Récupère la liste des utilisateurs qui participent à un événement spécifique
     * 
     * @param eventId L'ID de l'événement
     * @return Liste des utilisateurs participants
     */
    public List<User> getParticipantsByEvent(Long eventId) {
        List<User> participants = new ArrayList<>();

        try {
            // Requête SQL corrigée pour utiliser les noms de colonnes corrects
            String req = "SELECT u.*, p.dateparticipation FROM user u " +
                    "JOIN participation_event p ON u.id = p.user_id " +
                    "WHERE p.evenement_id = ? " +
                    "ORDER BY p.dateparticipation DESC";

            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setLong(1, eventId);

            ResultSet rs = ps.executeQuery();

            System.out.println("Exécution de la requête pour l'événement ID: " + eventId);

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                // Utilisez les noms corrects des colonnes de la BDD
                user.setFirstName(rs.getString("prenom"));
                user.setLastName(rs.getString("nom"));
                user.setEmail(rs.getString("email"));

                // Date d'inscription à l'événement (pour affichage dans la colonne
                // registrationDate)
                // Stockez la date de participation dans lastLoginAt comme solution temporaire
                java.sql.Date sqlDate = rs.getDate("dateparticipation");
                if (sqlDate != null) {
                    java.time.LocalDateTime participationDate = sqlDate.toLocalDate().atStartOfDay();
                    user.setLastLoginAt(participationDate);
                }

                System.out.println("Participant trouvé: " + user.getFullName() + " (" + user.getEmail() + ")");
                participants.add(user);
            }

            System.out.println("Total des participants trouvés: " + participants.size());

        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des participants: " + e.getMessage());
            e.printStackTrace();
        }

        return participants;
    }

    /**
     * Vérifie si la classe User a un champ pour stocker la date d'inscription
     * 
     * @return true si le champ existe, false sinon
     */
    private boolean hasRegistrationDateField() {
        try {
            User.class.getDeclaredField("registrationDate");
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

}
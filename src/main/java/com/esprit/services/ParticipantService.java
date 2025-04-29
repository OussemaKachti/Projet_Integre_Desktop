package com.esprit.services;

import com.esprit.models.Club;
import com.esprit.models.Participant;
import com.esprit.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipantService {

    private final Connection cnx;

    public ParticipantService() {
        cnx = DatabaseConnection.getInstance().getCnx();
    }

    public void ajouter(Participant p) {
        String req = "INSERT INTO participation_membre(user_id, club_id, date_request, statut, description) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, p.getUser_id());
            pst.setInt(2, p.getClub_id());
            pst.setTimestamp(3, Timestamp.valueOf(p.getDate_request()));
            pst.setString(4, p.getStatut());
            pst.setString(5, p.getDescription());
            pst.executeUpdate();
            System.out.println("Participant ajouté avec succès !");
        } catch (SQLException e) {
            System.err.println("Erreur d'ajout : " + e.getMessage());
        }
    }

    public void modifier(Participant p) {
        String req = "UPDATE participation_membre SET user_id=?, club_id=?, date_request=?, statut=?, description=? WHERE id=?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, p.getUser_id());
            pst.setInt(2, p.getClub_id());
            pst.setTimestamp(3, Timestamp.valueOf(p.getDate_request()));
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

    public List<Participant> afficher() throws SQLException {
        List<Participant> list = new ArrayList<>();
        String req = "SELECT pm.id, pm.user_id, pm.club_id, pm.date_request, pm.statut, pm.description, " +
                "c.id AS club_id, c.president_id, c.nom_c, c.description AS club_description, c.status, c.image, c.points, " +
                "u.nom AS user_name " +
                "FROM participation_membre pm " +
                "LEFT JOIN club c ON pm.club_id = c.id " +
                "LEFT JOIN user u ON pm.user_id = u.id"; // Adjust 'users' and 'nom' to match your schema
        try (PreparedStatement pst = cnx.prepareStatement(req);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                Participant p = new Participant();
                p.setId(rs.getInt("id"));
                p.setUser_id(rs.getInt("user_id"));
                p.setClub_id(rs.getInt("club_id"));
                p.setDate_request(rs.getTimestamp("date_request").toLocalDateTime());
                p.setStatut(rs.getString("statut"));
                p.setDescription(rs.getString("description"));
                p.setName(rs.getString("user_name"));

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

                list.add(p);
            }

            System.out.println("Nombre de participants récupérés : " + list.size());
            return list;
        } catch (SQLException e) {
            System.err.println("Erreur d'affichage : " + e.getMessage());
            throw e;
        }
    }

    public List<Participant> getAllParticipants() throws SQLException {
        return afficher();
    }
}

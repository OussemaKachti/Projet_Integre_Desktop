package com.esprit.services;

import com.esprit.models.Participant;
import com.esprit.utils.DataSource;
import com.esprit.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipantService {

    private final Connection cnx;

    // Constructeur qui initialise la connexion via le singleton DatabaseConnection
    public ParticipantService() {
        this.cnx = DataSource.getInstance().getCnx();
    }

    public void ajouter(Participant p) {
        String req = "INSERT INTO participation_membre(user_id, club_id, date_request, statut, description) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, p.getUser_id());  // Corrected the user_id
            pst.setInt(2, p.getClub_id());  // Corrected the club_id (was incorrectly set to user_id before)
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
            throw e; // Re-throw the exception to be handled by the controller
        }
    }

    public List<Participant> afficher() {
        List<Participant> list = new ArrayList<>();
        String req = "SELECT * FROM participation_membre";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {
                Participant p = new Participant();
                p.setId(rs.getInt("id"));
                p.setUser_id(rs.getInt("user_id"));
                p.setClub_id(rs.getInt("club_id"));
                p.setDate_request(rs.getTimestamp("date_request").toLocalDateTime());
                p.setStatut(rs.getString("statut"));
                p.setDescription(rs.getString("description"));
                list.add(p);
            }

        } catch (SQLException e) {
            System.err.println("Erreur d'affichage : " + e.getMessage());
        }

        return list;
    }

    public Iterable<Throwable> getAllParticipants() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllParticipants'");
    }
}
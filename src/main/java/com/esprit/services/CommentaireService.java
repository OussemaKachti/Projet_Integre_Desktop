package com.esprit.services;

import com.esprit.models.Commentaire;
import com.esprit.models.Sondage;
import com.esprit.models.User;
import com.esprit.utils.DatabaseConnection;
import com.esprit.utils.DataSource;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalDate;

public class CommentaireService {
    private Connection connection;
    private AiService aiService; // Service pour la détection de toxicité

    public CommentaireService() {
        connection = DataSource.getInstance().getCnx();
        aiService = new AiService();
    }

    public void add(Commentaire commentaire) throws SQLException {
        // Vérifier le contenu toxique avant d'ajouter
        if (aiService.isToxic(commentaire.getContenuComment())) {
            commentaire.setContenuComment("⚠️ Comment hidden: This content was flagged by our AI moderation system.");
            // Vous pouvez également envoyer un email ici si nécessaire
        }

        String query = "INSERT INTO commentaire (contenu_comment, date_comment, user_id, sondage_id) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pst = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, commentaire.getContenuComment());
            
            // Conversion de LocalDate en Timestamp pour la base de données
            LocalDate date = commentaire.getDateComment();
            pst.setTimestamp(2, date != null ? Timestamp.valueOf(date.atStartOfDay()) : Timestamp.valueOf(LocalDate.now().atStartOfDay()));
            
            pst.setInt(3, commentaire.getUser().getId());
            pst.setInt(4, commentaire.getSondage().getId());

            pst.executeUpdate();

            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                commentaire.setId(rs.getInt(1));
            }
        }
    }

    public void update(Commentaire commentaire) throws SQLException {
        // Vérifier si l'utilisateur est autorisé à modifier
        if (!isUserAuthorized(commentaire.getUser().getId(), commentaire.getId())) {
            throw new SecurityException("User not authorized to edit this comment");
        }

        String query = "UPDATE commentaire SET contenu_comment = ?, date_comment = ? WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, commentaire.getContenuComment());
            
            // Conversion de LocalDate en Timestamp pour la base de données
            LocalDate now = LocalDate.now();
            pst.setTimestamp(2, Timestamp.valueOf(now.atStartOfDay()));
            
            pst.setInt(3, commentaire.getId());
            pst.executeUpdate();
        }
    }

    /**
     * Supprime un commentaire avec vérification d'autorisation
     * @param commentId ID du commentaire à supprimer
     * @param userId ID de l'utilisateur qui fait la demande de suppression
     * @throws SQLException En cas d'erreur SQL
     * @throws SecurityException Si l'utilisateur n'est pas autorisé
     */
    public void delete(int commentId, int userId) throws SQLException {
        // Vérifier si l'utilisateur est autorisé à supprimer
        if (!isUserAuthorized(userId, commentId)) {
            throw new SecurityException("User not authorized to delete this comment");
        }

        String query = "DELETE FROM commentaire WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, commentId);
            pst.executeUpdate();
        }
    }
    
    /**
     * Supprime un commentaire sans vérification d'autorisation
     * Utilisé principalement par le contrôleur CRUD
     * @param commentId ID du commentaire à supprimer
     * @throws SQLException En cas d'erreur SQL
     */
    public void delete(int commentId) throws SQLException {
        String query = "DELETE FROM commentaire WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, commentId);
            pst.executeUpdate();
        }
    }

    public ObservableList<Commentaire> getBySondage(int sondageId) throws SQLException {
        ObservableList<Commentaire> commentaires = FXCollections.observableArrayList();
        String query = "SELECT * FROM commentaire WHERE sondage_id = ? ORDER BY date_comment DESC";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, sondageId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                commentaires.add(mapResultSetToCommentaire(rs));
            }
        }
        return commentaires;
    }

    public ObservableList<Commentaire> getAllComments() throws SQLException {
        ObservableList<Commentaire> commentaires = FXCollections.observableArrayList();
        String query = "SELECT c.*, s.question, cl.nom_c FROM commentaire c " +
                "LEFT JOIN sondage s ON c.sondage_id = s.id " +
                "LEFT JOIN club cl ON s.club_id = cl.id " +
                "ORDER BY c.date_comment DESC";

        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                commentaires.add(mapResultSetToCommentaire(rs));
            }
        }
        return commentaires;
    }

    private Commentaire mapResultSetToCommentaire(ResultSet rs) throws SQLException {
        Commentaire commentaire = new Commentaire();
        commentaire.setId(rs.getInt("id"));
        commentaire.setContenuComment(rs.getString("contenu_comment"));
        
        // Conversion de Timestamp en LocalDate
        Timestamp timestamp = rs.getTimestamp("date_comment");
        commentaire.setDateComment(timestamp.toLocalDateTime().toLocalDate());

        // Charger l'utilisateur et le sondage
        UserService userService = new UserService();
        SondageService sondageService = new SondageService();

        commentaire.setUser(userService.getById(rs.getInt("user_id")));
        commentaire.setSondage(sondageService.getById(rs.getInt("sondage_id")));

        return commentaire;
    }

    private boolean isUserAuthorized(int userId, int commentId) throws SQLException {
        String query = "SELECT user_id FROM commentaire WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, commentId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt("user_id") == userId;
            }
        }
        return false;
    }

    // Méthodes statistiques
    public int getTotalComments() throws SQLException {
        String query = "SELECT COUNT(*) FROM commentaire";
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int getTodayComments() throws SQLException {
        String query = "SELECT COUNT(*) FROM commentaire WHERE DATE(date_comment) = CURRENT_DATE";
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int getFlaggedComments() throws SQLException {
        String query = "SELECT COUNT(*) FROM commentaire WHERE contenu_comment LIKE '%⚠️%'";
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}
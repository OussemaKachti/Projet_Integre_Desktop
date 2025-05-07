/*package com.esprit.services;

import com.esprit.models.Commentaire;
import com.esprit.models.Sondage;
import com.esprit.models.User;
import com.esprit.utils.DataSource;
import com.esprit.utils.AiService; // Use the utils version explicitly
import com.esprit.utils.EmailService; // Use the utils version explicitly

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class CommentaireService {
    private Connection connection;
    private AiService aiService; // Service pour la détection de toxicité
    private OpenAIService openAIService; // Service pour la génération de résumés
    private EmailService emailService; // Service for sending warning emails
    private UserService userService; // Service for updating user warnings

    // The warning message prefix that marks a toxic comment
    private static final String TOXIC_COMMENT_PREFIX = "⚠️ Comment hidden: This content was flagged by our AI moderation system";

    public CommentaireService() {
        connection = DataSource.getInstance().getCnx();
        aiService = new AiService();
        emailService = EmailService.getInstance();
        userService = new UserService();
        try {
            openAIService = new OpenAIService();
        } catch (IllegalStateException e) {
            System.err.println("Warning: OpenAI API key not found. Summary generation will not be available.");
        }
    }

    public void add(Commentaire commentaire) throws SQLException {
        // First check if user is banned from commenting by counting their toxic comments
        User user = commentaire.getUser();
        if (user != null) {
            int toxicCommentCount = countUserToxicComments(user.getId());
            if (toxicCommentCount >= 3) {
                throw new SecurityException("This user is banned from commenting due to multiple violations of our community guidelines.");
            }
        }
        
        // Analyze the content for toxicity
        String originalContent = commentaire.getContenuComment();
        boolean isToxic = aiService.isToxic(originalContent);
        
        if (isToxic) {
            // Get toxicity details for the email
            JSONObject toxicityAnalysis = aiService.analyzeToxicity(originalContent);
            
            // Replace the content with a warning message
            String warningMessage = TOXIC_COMMENT_PREFIX + " for potentially inappropriate language. " +
                                  "We encourage respectful and constructive discussions. " +
                                  "If you believe this is an error, please contact our support team.";
            commentaire.setContenuComment(warningMessage);
            
            // Send warning email to the user
            if (user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
                int warningLevel = countUserToxicComments(user.getId()) + 1; // +1 for the current comment
                sendWarningEmail(user, originalContent, toxicityAnalysis, warningLevel);
            }
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
   /* public void delete(int commentId, int userId) throws SQLException {
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
     *
     * @param commentId ID du commentaire à supprimer
     * @return
     * @throws SQLException En cas d'erreur SQL
     */
   /* public boolean delete(int commentId) throws SQLException {
        String query = "DELETE FROM commentaire WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, commentId);
            pst.executeUpdate();
        }
        return false;
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
        
        // Conversion of Timestamp to LocalDateTime with null check
        Timestamp timestamp = rs.getTimestamp("date_comment");
        commentaire.setDateComment(timestamp != null ? timestamp.toLocalDateTime().toLocalDate() : LocalDate.now());

        // Load user and poll
        UserService userService = new UserService();
        SondageService             sondageService = SondageService.getInstance();


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

    // Add this new method to generate a summary of comments
    public String generateCommentsSummary(int sondageId) throws SQLException {
        ObservableList<Commentaire> comments = getBySondage(sondageId);
        
        if (comments.isEmpty()) {
            return "No comments available to summarize.";
        }
        
        // Check if OpenAI service is available
        if (openAIService == null) {
            try {
                // Try to initialize it again
                openAIService = new OpenAIService();
            } catch (IllegalStateException e) {
                System.err.println("ERROR: " + e.getMessage());
                // Use the fallback method instead
                return generateManualSummary(comments);
            }
        }
        
        try {
            System.out.println("Generating summary for " + comments.size() + " comments...");
            String summary = openAIService.summarizeComments(comments);
            System.out.println("Summary generated successfully");
            return summary;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to generate summary: " + e.getMessage());
            // Use the fallback method instead
            return generateManualSummary(comments);
        }
    }
    
    /**
     * Fallback method to generate a basic summary without using AI
     * @param comments The list of comments to summarize
     * @return A basic summary of the comments
     */
    private String generateManualSummary(ObservableList<Commentaire> comments) {
        StringBuilder summary = new StringBuilder();
        summary.append("Summary of comments:\n\n");

        for (int i = 0; i < comments.size(); i++) {
            Commentaire comment = comments.get(i);
            summary.append(i + 1).append(". ")
                    .append(comment.getUser().getFirstName()).append(" ")
                    .append(comment.getUser().getLastName()).append(": ")
                    .append(comment.getContenuComment()).append(" (")
                    .append(comment.getDateComment()).append(")\n");
        }

        // Add basic statistics
        summary.append("\n--- Statistics ---\n");
        summary.append("Total Comments: ").append(comments.size()).append("\n");

        // Count unique users
        long uniqueUsers = comments.stream()
                .map(c -> c.getUser().getId())
                .distinct()
                .count();
        summary.append("Unique Commenters: ").append(uniqueUsers).append("\n");
        
        return summary.toString();
    }

    /**
     * Counts how many toxic comments a user has posted
     * A toxic comment is identified by the prefix in the content
     * 
     * @param userId The user ID to check
     * @return The count of toxic comments by this user
     */
    public int countUserToxicComments(int userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM commentaire WHERE user_id = ? AND contenu_comment LIKE ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setString(2, TOXIC_COMMENT_PREFIX + "%");
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
    
    /**
     * Checks if a user is banned from commenting based on their toxic comment count
     * 
     * @param userId The user ID to check
     * @return True if the user has 3 or more toxic comments and is therefore banned
     */
    public boolean isUserBannedFromCommenting(int userId) throws SQLException {
        return countUserToxicComments(userId) >= 3;
    }
    
    /**
     * Sends a warning email to a user who posted toxic content
     */
    private void sendWarningEmail(User user, String originalContent, JSONObject toxicityAnalysis, int warningLevel) {
        try {
            // Get toxic words as a list
            List<String> toxicWords = new ArrayList<>();
            JSONArray toxicWordsArray = toxicityAnalysis.getJSONArray("toxicWords");
            for (int i = 0; i < toxicWordsArray.length(); i++) {
                toxicWords.add(toxicWordsArray.getString(i));
            }
            
            // Generate HTML content based on warning level
            String emailContent = createWarningEmailContent(
                user.getFirstName(), 
                warningLevel, 
                originalContent, 
                toxicWords, 
                toxicityAnalysis.getString("reason")
            );
            
            // Send the email
            String subject = "Warning Level " + warningLevel + ": Inappropriate Comment Detected";
            emailService.sendEmailAsync(user.getEmail(), subject, emailContent);
            
        } catch (Exception e) {
            System.err.println("Failed to send warning email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates HTML content for the warning email with appropriate styling based on warning level
     */
    private String createWarningEmailContent(String userName, int warningLevel, String commentContent, 
                                            List<String> toxicWords, String reason) {
        // Determine color based on warning level
        String warningColor;
        String severityText;
        String consequenceText;
        String progressBar;
        
        switch (warningLevel) {
            case 1:
                warningColor = "#FFD700"; // Brighter yellow for first warning
                severityText = "This is your first warning (1/3).";
                consequenceText = "Please be mindful of our community guidelines in future comments.";
                progressBar = "<div style=\"width: 100%; background-color: #f0f0f0; height: 8px; border-radius: 4px; margin: 15px 0;\">" +
                              "<div style=\"width: 33%; background-color: " + warningColor + "; height: 100%; border-radius: 4px;\"></div></div>";
                break;
            case 2:
                warningColor = "#FF8C00"; // Brighter orange for second warning
                severityText = "This is your second warning (2/3).";
                consequenceText = "Please note that a third violation will result in a commenting ban.";
                progressBar = "<div style=\"width: 100%; background-color: #f0f0f0; height: 8px; border-radius: 4px; margin: 15px 0;\">" +
                              "<div style=\"width: 66%; background-color: " + warningColor + "; height: 100%; border-radius: 4px;\"></div></div>";
                break;
            default:
                warningColor = "#F44336"; // Red for third warning
                severityText = "This is your final warning (3/3).";
                consequenceText = "Your account has been restricted from commenting due to multiple violations of our community guidelines.";
                progressBar = "<div style=\"width: 100%; background-color: #f0f0f0; height: 8px; border-radius: 4px; margin: 15px 0;\">" +
                              "<div style=\"width: 100%; background-color: " + warningColor + "; height: 100%; border-radius: 4px;\"></div></div>";
                break;
        }
        
        // Build the email HTML content
        StringBuilder toxicWordsHtml = new StringBuilder();
        for (String word : toxicWords) {
            toxicWordsHtml.append("<span style=\"background-color: rgba(244, 67, 54, 0.1); padding: 2px 5px; border-radius: 3px; color: #F44336; font-weight: bold;\">")
                          .append(word)
                          .append("</span> ");
        }
        
        return "<!DOCTYPE html>\n" +
               "<html>\n" +
               "<head>\n" +
               "    <meta charset=\"UTF-8\">\n" +
               "    <style>\n" +
               "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; }\n" +
               "        .header { background-color: " + warningColor + "; color: white; padding: 20px; border-radius: 5px 5px 0 0; }\n" +
               "        .content { padding: 20px; border: 1px solid #ddd; border-top: none; border-radius: 0 0 5px 5px; }\n" +
               "        .warning-level { font-size: 24px; font-weight: bold; }\n" +
               "        .original-comment { background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 15px 0; border-left: 4px solid " + warningColor + "; }\n" +
               "        .toxic-words { margin: 15px 0; }\n" +
               "        .footer { text-align: center; font-size: 12px; color: #888; margin-top: 30px; }\n" +
               "        .severity { font-weight: bold; color: " + warningColor + "; }\n" +
               "        .consequences { background-color: rgba(" + (warningLevel == 3 ? "244, 67, 54" : warningLevel == 2 ? "255, 140, 0" : "255, 215, 0") + ", 0.1); padding: 15px; border-radius: 5px; margin: 15px 0; }\n" +
               "        .progress-label { font-size: 12px; color: #666; margin-bottom: 5px; text-align: right; }\n" +
               "    </style>\n" +
               "</head>\n" +
               "<body>\n" +
               "    <div class=\"header\">\n" +
               "        <h2>Comment Warning - Level " + warningLevel + "</h2>\n" +
               "    </div>\n" +
               "    \n" +
               "    <div class=\"content\">\n" +
               "        <p>Hello " + userName + ",</p>\n" +
               "        \n" +
               "        <p>Our AI moderation system has flagged a comment you recently posted for potentially inappropriate content.</p>\n" +
               "        \n" +
               "        <div class=\"progress-label\">Warning " + warningLevel + " of 3</div>\n" +
               "        " + progressBar + "\n" +
               "        \n" +
               "        <p class=\"severity\">" + severityText + "</p>\n" +
               "        \n" +
               "        <h3>Your Original Comment:</h3>\n" +
               "        <div class=\"original-comment\">\n" +
               "            \"" + commentContent + "\"\n" +
               "        </div>\n" +
               "        \n" +
               "        <h3>Reason for Flagging:</h3>\n" +
               "        <p>" + reason + "</p>\n" +
               "        \n" +
               "        <div class=\"toxic-words\">\n" +
               "            <h3>Flagged Content:</h3>\n" +
               "            <p>" + toxicWordsHtml.toString() + "</p>\n" +
               "        </div>\n" +
               "        \n" +
               "        <div class=\"consequences\">\n" +
               "            <h3>What This Means:</h3>\n" +
               "            <p>" + consequenceText + "</p>\n" +
               (warningLevel >= 3 ? "<p><strong>Since this is your third violation, you are now restricted from posting new comments.</strong></p>" : "") +
               "        </div>\n" +
               "        \n" +
               "        <p>If you believe this is an error, please contact our support team at <a href=\"mailto:support@uniclubs.com\">support@uniclubs.com</a>.</p>\n" +
               "        \n" +
               "        <p>Thank you for your understanding.</p>\n" +
               "    </div>\n" +
               "    \n" +
               "    <div class=\"footer\">\n" +
               "        <p>This is an automated message. Please do not reply to this email.</p>\n" +
               "        <p>© " + java.time.Year.now().getValue() + " UNICLUBS</p>\n" +
               "    </div>\n" +
               "</body>\n" +
               "</html>";
    }

    /**
     * Checks if a comment is flagged as toxic
     * @param comment The comment to check
     * @return true if the comment is flagged as toxic
     */
    public boolean isCommentFlaggedAsToxic(Commentaire comment) {
        if (comment == null || comment.getContenuComment() == null) {
            return false;
        }
        return comment.getContenuComment().startsWith(TOXIC_COMMENT_PREFIX);
    }
    
    /**
     * Checks if a comment's content is flagged as toxic
     * @param commentContent The comment content to check
     * @return true if the comment is flagged as toxic
     */
    public boolean isContentFlaggedAsToxic(String commentContent) {
        if (commentContent == null) {
            return false;
        }
        return commentContent.startsWith(TOXIC_COMMENT_PREFIX);
    }
}
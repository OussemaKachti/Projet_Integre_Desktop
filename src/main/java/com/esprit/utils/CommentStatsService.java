package com.esprit.utils;

import com.esprit.models.Commentaire;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating comment statistics and preparing data for
 * visualizations
 */
public class CommentStatsService {
    private static CommentStatsService instance;

    private CommentStatsService() {
        // Private constructor for singleton
    }

    public static synchronized CommentStatsService getInstance() {
        if (instance == null) {
            instance = new CommentStatsService();
        }
        return instance;
    }

    /**
     * Get comment data formatted for monthly bar chart visualization
     * 
     * @param comments List of comments to analyze
     * @param clubName Name of the club to filter by (or "all" for all clubs)
     * @return List of maps containing comment data by month
     */
    public List<Map<String, Object>> getCommentsByMonth(List<Commentaire> comments, String clubName) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (comments == null || comments.isEmpty()) {
            return result;
        }

        // Filter comments by club if needed
        List<Commentaire> filteredComments;
        if (clubName == null || clubName.equals("all")) {
            filteredComments = comments;
        } else {
            filteredComments = new ArrayList<>();
            for (Commentaire comment : comments) {
                String commentClubName = "Inconnu";
                if (comment.getSondage() != null && comment.getSondage().getClub() != null) {
                    commentClubName = comment.getSondage().getClub().getNomC();
                }
                if (clubName.equals(commentClubName)) {
                    filteredComments.add(comment);
                }
            }
        }

        // Convert comments to the format expected by the visualization
        for (Commentaire comment : filteredComments) {
            Map<String, Object> commentData = new HashMap<>();

            // Format date as ISO string (YYYY-MM-DD)
            LocalDate commentDate = comment.getDateComment();
            commentData.put("date", commentDate.toString());
            String commentClubName = "Inconnu";
            if (comment.getSondage() != null && comment.getSondage().getClub() != null) {
                commentClubName = comment.getSondage().getClub().getNomC();
            }
            commentData.put("club", commentClubName);
            commentData.put("content", comment.getContenuComment());

            result.add(commentData);
        }

        return result;
    }

    /**
     * Analyze comment sentiment distribution by club
     * 
     * @param comments List of comments to analyze
     * @param clubName Name of the club to filter by (or "all" for all clubs)
     * @return Map containing sentiment counts and percentages
     */
    public Map<String, Object> analyzeClubCommentSentiment(List<Commentaire> comments, String clubName) {
        Map<String, Object> result = new HashMap<>();

        if (comments == null || comments.isEmpty()) {
            result.put("positive", 0.0);
            result.put("negative", 0.0);
            result.put("neutral", 0.0);
            result.put("totalComments", 0);
            return result;
        }

        // Filter comments by club if needed
        List<Commentaire> filteredComments;
        if (clubName == null || clubName.equals("all")) {
            filteredComments = comments;
        } else {
            filteredComments = new ArrayList<>();
            for (Commentaire comment : comments) {
                String commentClubName = "Inconnu";
                if (comment.getSondage() != null && comment.getSondage().getClub() != null) {
                    commentClubName = comment.getSondage().getClub().getNomC();
                }
                if (clubName.equals(commentClubName)) {
                    filteredComments.add(comment);
                }
            }
        }

        // For simplicity, we'll do a basic sentiment analysis here
        // In a real app, you might want to use the SentimentAnalysisService
        int positive = 0, negative = 0, neutral = 0;

        for (Commentaire comment : filteredComments) {
            String content = comment.getContenuComment().toLowerCase();

            // Very basic sentiment analysis
            if (content.contains("good") || content.contains("great") ||
                    content.contains("excellent") || content.contains("love") ||
                    content.contains("awesome") || content.contains("nice")) {
                positive++;
            } else if (content.contains("bad") || content.contains("poor") ||
                    content.contains("terrible") || content.contains("hate") ||
                    content.contains("worst") || content.contains("awful")) {
                negative++;
            } else {
                neutral++;
            }
        }

        int total = filteredComments.size();

        // Calculate percentages
        double positivePercent = total > 0 ? (double) positive / total : 0;
        double negativePercent = total > 0 ? (double) negative / total : 0;
        double neutralPercent = total > 0 ? (double) neutral / total : 0;

        result.put("positive", positivePercent);
        result.put("negative", negativePercent);
        result.put("neutral", neutralPercent);
        result.put("totalComments", total);

        return result;
    }
}
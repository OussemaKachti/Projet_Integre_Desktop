package com.esprit.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.esprit.models.User;
import com.esprit.services.UserService;

/**
 * Utility class for logging profanity incidents and managing user warnings
 */
public class ProfanityLogManager {
    
    private static final String LOG_DIRECTORY = "logs";
    private static final String LOG_FILE = LOG_DIRECTORY + "/profanity_incidents.log";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private static UserService userService;
    
    static {
        try {
            // Ensure log directory exists
            File directory = new File(LOG_DIRECTORY);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // Initialize the user service
            userService = new UserService();
        } catch (Exception e) {
            System.err.println("Error initializing ProfanityLogManager: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Logs a profanity incident and increments the user's warning count
     * 
     * @param user The user who attempted to use profanity
     * @param fieldName The form field containing profanity
     * @param profaneText The actual profane text (will be partially censored)
     * @param severity The severity level (High, Medium, Low)
     * @param action The action taken (e.g., "Profile update rejected")
     * @return True if the incident was logged and warning count incremented, false otherwise
     */
    public static boolean logProfanityIncident(User user, String fieldName, String profaneText, String severity, String action) {
        if (user == null || fieldName == null) {
            return false;
        }
        
        try {
            // Skip censoring for profile images, but still censor text content
            String textToLog;
            if (fieldName.toLowerCase().contains("profile image")) {
                textToLog = profaneText; // Use uncensored text for profile images
            } else {
                textToLog = censorProfanity(profaneText); // Censor text for other fields
            }
            
            // Format: timestamp|userId|fieldName|severity|action|censoredText
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String logEntry = String.format("%s|%d|%s|%s|%s|%s%n", 
                                          timestamp, 
                                          user.getId(), 
                                          fieldName, 
                                          severity, 
                                          action,
                                          textToLog);
            
            // Append to log file
            try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
                writer.write(logEntry);
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("Error logging profanity incident: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Creates a partially censored version of profane text
     * 
     * @param text The text to censor
     * @return Partially censored text
     */
    private static String censorProfanity(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Create a simple partial censoring by replacing middle characters with asterisks
        // but keeping the first and last character visible
        if (text.length() <= 2) {
            return text; // Too short to censor meaningfully
        }
        
        char firstChar = text.charAt(0);
        char lastChar = text.charAt(text.length() - 1);
        
        // For long words, keep first and last letter
        StringBuilder censored = new StringBuilder();
        censored.append(firstChar);
        
        // Add asterisks for middle characters (preserve word length)
        for (int i = 1; i < text.length() - 1; i++) {
            // Keep some letters uncensored for readability (every 3rd character)
            if (i % 3 == 0 && Character.isLetter(text.charAt(i))) {
                censored.append(text.charAt(i));
            } else {
                censored.append('*');
            }
        }
        
        censored.append(lastChar);
        
        return censored.toString();
    }
    
    /**
     * Gets all profanity incidents for a specific user from the log file
     * 
     * @param userId The user ID to filter incidents for
     * @return List of incident details as maps
     */
    public static List<Map<String, String>> getProfanityIncidents(int userId) {
        List<Map<String, String>> incidents = new ArrayList<>();
        
        try {
            File logFile = new File(LOG_FILE);
            if (!logFile.exists()) {
                return incidents;
            }
            
            List<String> logLines = Files.readAllLines(Paths.get(LOG_FILE));
            
            for (String line : logLines) {
                String[] parts = line.split("\\|");
                if (parts.length >= 4 && parts[1].equals(String.valueOf(userId))) {
                    Map<String, String> incident = new HashMap<>();
                    
                    // Parse the timestamp to format it for display
                    LocalDateTime timestamp = LocalDateTime.parse(parts[0], TIMESTAMP_FORMAT);
                    incident.put("date", timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    
                    incident.put("field", parts[2]);
                    incident.put("severity", parts[3]);
                    
                    if (parts.length > 4) {
                        incident.put("action", parts[4]);
                    } else {
                        incident.put("action", "Content rejected");
                    }
                    
                    // Add the censored profane text if available (position 5)
                    if (parts.length > 5) {
                        incident.put("profaneText", parts[5]);
                    } else {
                        incident.put("profaneText", "[Content not recorded]");
                    }
                    
                    incidents.add(incident);
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading profanity log: " + e.getMessage());
            e.printStackTrace();
        }
        
        return incidents;
    }
    
    /**
     * Determines the severity of profanity based on the field
     * 
     * @param fieldName The name of the field containing profanity
     * @return Severity level (High, Medium, Low)
     */
    public static String determineSeverity(String fieldName) {
        if (fieldName == null) {
            return "Low";
        }
        
        // Personal identity fields are considered higher severity
        if (fieldName.toLowerCase().contains("name") || 
            fieldName.toLowerCase().contains("email") ||
            fieldName.toLowerCase().contains("username")) {
            return "High";
        }
        
        // Bio or description fields are medium severity
        if (fieldName.toLowerCase().contains("bio") || 
            fieldName.toLowerCase().contains("description") ||
            fieldName.toLowerCase().contains("about")) {
            return "Medium";
        }
        
        // Default to low severity
        return "Low";
    }
    
    /**
     * Clears all profanity incidents for a specific user from the log file
     * 
     * @param userId The user ID to clear incidents for
     * @return True if incidents were cleared successfully, false otherwise
     */
    public static boolean clearProfanityIncidents(int userId) {
        try {
            File logFile = new File(LOG_FILE);
            if (!logFile.exists()) {
                return true; // No log file means nothing to clear
            }
            
            // Read all lines from the log file
            List<String> logLines = Files.readAllLines(Paths.get(LOG_FILE));
            
            // Filter out lines related to the specified user
            List<String> filteredLines = logLines.stream()
                .filter(line -> {
                    String[] parts = line.split("\\|");
                    return parts.length < 2 || !parts[1].equals(String.valueOf(userId));
                })
                .collect(Collectors.toList());
            
            // Write the filtered lines back to the log file
            Files.write(Paths.get(LOG_FILE), filteredLines);
            
            System.out.println("Cleared profanity incidents for user ID: " + userId);
            return true;
        } catch (Exception e) {
            System.err.println("Error clearing profanity incidents: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
} 
// Path: src/main/java/utils/ValidationUtils.java
package com.esprit.utils;

import java.util.regex.Pattern;

public class ValidationUtils {
    
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$");
    
    // Tunisian phone number pattern
    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^((\\+|00)216)?([2579][0-9]{7}|(3[012]|4[01]|8[0128])[0-9]{6}|42[16][0-9]{5})$");
    
    /**
     * Validates email format
     * @param email Email to validate
     * @return True if valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Validates password complexity
     * @param password Password to validate
     * @return True if valid, false otherwise
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }
    
    /**
     * Validates Tunisian phone number format
     * @param phone Phone number to validate
     * @return True if valid, false otherwise
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return true; // Empty phone is valid since it's optional
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }
    
    /**
     * Checks if an exception is related to unique constraint violation
     * @param e The exception to check
     * @param fieldName The field to check for in the error message
     * @return True if it's a unique constraint violation for the given field
     */
    public static boolean isUniqueConstraintViolation(Exception e, String fieldName) {
        if (e == null) return false;
        
        // Extract complete exception message chain
        StringBuilder fullMessageBuilder = new StringBuilder();
        Throwable current = e;
        while (current != null) {
            if (current.getMessage() != null) {
                fullMessageBuilder.append(current.getMessage()).append(" ");
            }
            current = current.getCause();
        }
        
        String fullMessage = fullMessageBuilder.toString().toLowerCase();
        
        // First, check if it's any type of constraint violation
        boolean isConstraintViolation = 
            fullMessage.contains("duplicate") || 
            fullMessage.contains("unique") ||
            fullMessage.contains("constraint") ||
            fullMessage.contains("integrity") ||
            fullMessage.contains("violation") ||
            e.getClass().getSimpleName().contains("Constraint");
        
        if (!isConstraintViolation) {
            return false;
        }
        
        // MySQL specific pattern for duplicate entry
        // Example: Duplicate entry 'baltinour118@gmail.com' for key 'UNIQ_8D93D649E7927C74'
        // Example: Duplicate entry '29103858' for key 'UNIQ_8D93D649435805D'
        if (fullMessage.contains("duplicate entry")) {
            // For email field
            if ("email".equals(fieldName)) {
                return fullMessage.contains("@") || // Emails contain @
                       fullMessage.contains("e7927c74"); // MySQL uses this key for email
            }
            
            // For phone field
            if ("tel".equals(fieldName) || "phone".equals(fieldName)) {
                return fullMessage.matches(".*duplicate entry ['\"]\\d{8,}['\"].*") || // Phone numbers are 8+ digits
                       fullMessage.contains("435805d"); // MySQL uses this key for phone number
            }
        }
        
        // For email field general check
        if ("email".equals(fieldName)) {
            return fullMessage.contains("email") || 
                   fullMessage.contains("@");
        }
        
        // For phone field general check
        if ("tel".equals(fieldName) || "phone".equals(fieldName)) {
            return fullMessage.contains("tel") || 
                   fullMessage.contains("phone");
        }
        
        // Generic check for other fields
        return fullMessage.contains(fieldName.toLowerCase());
    }

    /**
     * Checks if the given text contains profanity
     * @param text Text to check for profanity
     * @return True if no profanity is found, false if profanity is detected
     */
    public static boolean isCleanText(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        return !ProfanityFilter.containsProfanity(text);
    }
    
    /**
     * Parses validation error messages from a ConstraintViolationException
     * @param e The exception containing validation errors
     * @return A map of field names to error messages
     */
    public static java.util.Map<String, String> parseValidationErrors(Exception e) {
        java.util.Map<String, String> errors = new java.util.HashMap<>();
        
        if (e == null || e.getMessage() == null) {
            return errors;
        }
        
        String message = e.getMessage();
        
        // Parse messages that follow pattern: "fieldName: error message"
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([a-zA-Z]+):\\s*([^,]+)");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        
        while (matcher.find()) {
            String fieldName = matcher.group(1);
            String errorMessage = matcher.group(2).trim();
            errors.put(fieldName, errorMessage);
        }
        
        return errors;
    }
}

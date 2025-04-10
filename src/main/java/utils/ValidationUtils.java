package utils;

import java.util.regex.Pattern;

public class ValidationUtils {
    
    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    
    // Password validation pattern (uppercase, lowercase, number, special char, 8+ chars)
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$");
    
    // Name validation pattern (letters, spaces, hyphens and apostrophes)
    private static final Pattern NAME_PATTERN = 
        Pattern.compile("^[a-zA-ZÀ-ÿ\\s'-]+");
    
    /**
     * Validates email format
     * @param email Email to validate
     * @return true if email is valid
     */
    public static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Validates password complexity
     * @param password Password to validate
     * @return true if password meets complexity requirements
     */
    public static boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }
    
    /**
     * Validates name format
     * @param name Name to validate
     * @return true if name is valid
     */
    public static boolean isValidName(String name) {
        if (name == null || name.length() < 2) {
            return false;
        }
        return NAME_PATTERN.matcher(name).matches();
    }
    
    /**
     * Validates phone number format
     * @param phone Phone number to validate
     * @return true if phone number is valid
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return true; // Phone is optional
        }
        // Simple validation - just digits, at least 8
        return phone.replaceAll("[^0-9]", "").length() >= 8;
    }
}
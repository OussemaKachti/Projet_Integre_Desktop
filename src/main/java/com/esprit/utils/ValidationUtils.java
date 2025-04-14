// Path: src/main/java/utils/ValidationUtils.java
package com.esprit.utils;

import java.util.regex.Pattern;

public class ValidationUtils {
    
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
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
        
        String message = e.getMessage();
        if (message == null) {
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                message = e.getCause().getMessage();
            } else {
                return false;
            }
        }
        
        return message.toLowerCase().contains("unique") && 
               message.toLowerCase().contains(fieldName.toLowerCase());
    }
}

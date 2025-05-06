package com.esprit.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {
    
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }
    
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
    
    public static String generateRandomToken() {
        return BCrypt.gensalt(12).replace("/", "").replace(".", "");
    }
}
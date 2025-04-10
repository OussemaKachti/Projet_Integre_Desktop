package services;

import entities.User;
import utils.PasswordUtils;
import jakarta.persistence.TypedQuery;
import java.util.List;
import enums.RoleEnum;
import java.time.LocalDateTime;

public class AuthService {
    
    private final UserService userService;
    private final EmailService emailService;
    
    // Error codes for authentication
    public static final int AUTH_SUCCESS = 0;
    public static final int AUTH_INVALID_CREDENTIALS = 1;
    public static final int AUTH_NOT_VERIFIED = 2;
    
    private int lastAuthErrorCode = AUTH_SUCCESS;
    
    public AuthService() {
        this.userService = new UserService();
        this.emailService = new EmailService();
    }
    
    /**
     * Authenticate a user with email and password
     * @param email User's email
     * @param password User's password
     * @return User object if authentication successful, null otherwise
     */
    public User authenticate(String email, String password) {
        // Reset error code
        lastAuthErrorCode = AUTH_SUCCESS;
        
        // Find user by email
        User user = userService.findByEmail(email);
        if (user == null) {
            lastAuthErrorCode = AUTH_INVALID_CREDENTIALS;
            return null;
        }
        
        // Check if the user is verified
        if (!user.isVerified()) {
            lastAuthErrorCode = AUTH_NOT_VERIFIED;
            return null;
        }
        
        // Check password
        if (user.getPassword().equals(password) || 
            (user.getPassword().startsWith("$2a$") && PasswordUtils.verifyPassword(password, user.getPassword()))) {
            // Update last login time
            user.setLastLoginAt(LocalDateTime.now());
            userService.modifier(user);
            return user;
        }
        
        lastAuthErrorCode = AUTH_INVALID_CREDENTIALS;
        return null;
    }
    
    /**
     * Get the last authentication error code
     * @return Error code
     */
    public int getLastAuthErrorCode() {
        return lastAuthErrorCode;
    }
    
    /**
     * Get error message for the last authentication error
     * @return Error message
     */
    public String getLastAuthErrorMessage() {
        switch (lastAuthErrorCode) {
            case AUTH_SUCCESS:
                return "Authentication successful";
            case AUTH_INVALID_CREDENTIALS:
                return "Invalid email or password";
            case AUTH_NOT_VERIFIED:
                return "Account not verified. Please check your email for verification instructions.";
            default:
                return "Unknown authentication error";
        }
    }
    
    public String generatePasswordResetToken(String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            return null;
        }
        
        String token = PasswordUtils.generateRandomToken();
        user.setConfirmationToken(token);
        user.setConfirmationTokenExpiresAt(LocalDateTime.now().plusHours(24));
        userService.modifier(user);
        
        // Send password reset email
        emailService.sendPasswordResetEmail(
            user.getEmail(),
            user.getFirstName() + " " + user.getLastName(),
            token
        );
        
        return token;
    }
    
    public boolean resetPassword(String token, String newPassword) {
        try {
            TypedQuery<User> query = userService.getEntityManager().createQuery(
                "SELECT u FROM User u WHERE u.confirmationToken = :token AND u.confirmationTokenExpiresAt > :now",
                User.class
            );
            query.setParameter("token", token);
            query.setParameter("now", LocalDateTime.now());
            
            List<User> results = query.getResultList();
            if (results.isEmpty()) {
                return false;
            }
            
            User user = results.get(0);
            // Hash the password for security
            user.setPassword(PasswordUtils.hashPassword(newPassword));
            user.setConfirmationToken(null);
            user.setConfirmationTokenExpiresAt(null);
            userService.modifier(user);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public User registerUser(User user) {
        try {
            // Set default values
            user.setRole(RoleEnum.NON_MEMBRE);
            user.setStatus("active");
            user.setVerified(false);
            user.setCreatedAt(LocalDateTime.now());
            user.setWarningCount(0);
            
            // Hash the password
            user.setPassword(PasswordUtils.hashPassword(user.getPassword()));
            
            // Generate confirmation token
            String token = PasswordUtils.generateRandomToken();
            user.setConfirmationToken(token);
            user.setConfirmationTokenExpiresAt(LocalDateTime.now().plusDays(1));
            
            // Save user
            userService.ajouter(user);
            
            // Send verification email
            emailService.sendVerificationEmail(
                user.getEmail(),
                user.getFirstName() + " " + user.getLastName(),
                token
            );
            
            return user;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public boolean verifyEmail(String token) {
        try {
            TypedQuery<User> query = userService.getEntityManager().createQuery(
                "SELECT u FROM User u WHERE u.confirmationToken = :token AND u.confirmationTokenExpiresAt > :now",
                User.class
            );
            query.setParameter("token", token);
            query.setParameter("now", LocalDateTime.now());
            
            List<User> results = query.getResultList();
            if (results.isEmpty()) {
                return false;
            }
            
            User user = results.get(0);
            user.setVerified(true);
            user.setConfirmationToken(null);
            user.setConfirmationTokenExpiresAt(null);
            userService.modifier(user);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Resend verification email for an unverified account
     * @param email User's email
     * @return true if email sent successfully, false otherwise
     */
    public boolean resendVerificationEmail(String email) {
        User user = userService.findByEmail(email);
        if (user == null || user.isVerified()) {
            return false;
        }
        
        // Generate new token
        String token = PasswordUtils.generateRandomToken();
        user.setConfirmationToken(token);
        user.setConfirmationTokenExpiresAt(LocalDateTime.now().plusDays(1));
        userService.modifier(user);
        
        // Send verification email
        return emailService.sendVerificationEmail(
            user.getEmail(),
            user.getFirstName() + " " + user.getLastName(),
            token
        );
    }
    
    public void close() {
        userService.close();
    }
}
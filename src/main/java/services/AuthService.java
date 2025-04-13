package services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import entities.User;
import enums.RoleEnum;
import jakarta.persistence.TypedQuery;
import utils.PasswordUtils;

public class AuthService {
    
    private final UserService userService;
    private final EmailService emailService;
    
    // Error codes for authentication
    public static final int AUTH_SUCCESS = 0;
    public static final int AUTH_INVALID_CREDENTIALS = 1;
    public static final int AUTH_NOT_VERIFIED = 2;
    
    // Verification settings
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private static final int VERIFICATION_CODE_LENGTH = 6;
    private static final int VERIFICATION_EXPIRY_HOURS = 2;
    
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
            userService.updateLastLoginTime(user.getId());
            user.setLastLoginAt(LocalDateTime.now());
            
            return user;
        }
        
        lastAuthErrorCode = AUTH_INVALID_CREDENTIALS;
        return null;
    }
    
    /**
     * Generate a numeric verification code
     * @return A 6-digit numeric code
     */
    private String generateVerificationCode() {
        // Generate a 6-digit numeric code
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < VERIFICATION_CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
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
    
    /**
     * Generate a password reset code (numeric) and send it via email
     * @param email User's email
     * @return Reset code or null if user not found
     */
    public String generatePasswordResetCode(String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            return null;
        }
        
        // Generate a 6-digit numeric code
        String resetCode = generateVerificationCode();
        user.setConfirmationToken(resetCode);
        user.setConfirmationTokenExpiresAt(LocalDateTime.now().plusHours(2));
        user.setLastCodeSentTime(LocalDateTime.now());
        userService.modifier(user);
        
        // Send password reset email asynchronously
        emailService.sendPasswordResetEmailAsync(
            user.getEmail(),
            user.getFirstName() + " " + user.getLastName(),
            resetCode
        );
        
        return resetCode;
    }
    
    /**
     * Verify a password reset code without changing the password
     * This is used to validate the code before showing the password reset form
     * 
     * @param code The reset code to verify
     * @param email The user's email address
     * @return true if the code is valid, false otherwise
     */
    public boolean verifyResetCode(String code, String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            return false;
        }
        
        // Check if token matches and is not expired
        return user.getConfirmationToken() != null && 
               user.getConfirmationToken().equals(code) && 
               user.getConfirmationTokenExpiresAt().isAfter(LocalDateTime.now());
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
            user.setStatus("inactive"); // Change status to inactive until verified
            user.setVerified(false);
            user.setCreatedAt(LocalDateTime.now());
            user.setWarningCount(0);
            user.setVerificationAttempts(0); // Initialize verification attempts to 0
            user.setLastCodeSentTime(LocalDateTime.now()); // Set initial code sent time
            
            // Hash the password
            user.setPassword(PasswordUtils.hashPassword(user.getPassword()));
            
            // Generate numeric verification code instead of token
            String verificationCode = generateVerificationCode();
            user.setConfirmationToken(verificationCode);
            user.setConfirmationTokenExpiresAt(
                LocalDateTime.now().plusHours(VERIFICATION_EXPIRY_HOURS));
            
            // Save user
            userService.ajouter(user);
            
            // Send verification email with code asynchronously
            // We don't wait for this to complete - it will happen in the background
            emailService.sendVerificationEmailAsync(
                user.getEmail(),
                user.getFirstName() + " " + user.getLastName(),
                verificationCode
            );
            
            return user;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public boolean verifyEmail(String token, String email) {
        try {
            // Find user by email first
            User user = userService.findByEmail(email);
            if (user == null) {
                return false;
            }
            
            // Check if account is locked due to too many attempts
            if (user.getVerificationAttempts() >= MAX_VERIFICATION_ATTEMPTS) {
                return false;
            }
            
            // Check if token matches and is not expired
            if (user.getConfirmationToken() != null && 
                user.getConfirmationToken().equals(token) && 
                user.getConfirmationTokenExpiresAt().isAfter(LocalDateTime.now())) {
                
                // Successful verification
                user.setVerified(true);
                user.setStatus("active"); // Activate account on verification
                user.setConfirmationToken(null);
                user.setConfirmationTokenExpiresAt(null);
                user.setVerificationAttempts(0); // Reset attempts
                userService.modifier(user);
                return true;
            } else {
                // Failed verification attempt
                user.setVerificationAttempts(user.getVerificationAttempts() + 1);
                userService.modifier(user);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Legacy verification method for backward compatibility
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
            user.setStatus("active"); // Ensure user is active after verification
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
     * Check account verification status
     * @param email User's email
     * @return Status code: -1=not found, 0=pending, 1=verified, 2=locked, 3=expired
     */
    public int checkAccountVerificationStatus(String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            return -1; // User not found
        }
        
        if (user.isVerified()) {
            return 1; // Already verified
        }
        
        if (user.getVerificationAttempts() >= MAX_VERIFICATION_ATTEMPTS) {
            return 2; // Account locked due to too many attempts
        }
        
        if (user.getConfirmationTokenExpiresAt() != null && 
            user.getConfirmationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            return 3; // Verification code expired
        }
        
        return 0; // Pending verification
    }
    
    /**
     * Resend verification code
     * @param email User's email
     * @return true if process started successfully, false otherwise
     */
    public boolean resendVerificationCode(String email) {
        User user = userService.findByEmail(email);
        if (user == null || user.isVerified()) {
            return false;
        }
        
        // Check if last code was sent within the last 1 minute (rate limiting)
        LocalDateTime lastCodeSentTime = user.getLastCodeSentTime();
        if (lastCodeSentTime != null && 
            lastCodeSentTime.isAfter(LocalDateTime.now().minusMinutes(1))) {
            return false; // Too many requests - reduced from 2 minutes to 1 minute
        }
        
        // Generate new verification code
        String verificationCode = generateVerificationCode();
        user.setConfirmationToken(verificationCode);
        user.setConfirmationTokenExpiresAt(
            LocalDateTime.now().plusHours(VERIFICATION_EXPIRY_HOURS));
        user.setLastCodeSentTime(LocalDateTime.now());
        user.setVerificationAttempts(0); // Reset attempts
        userService.modifier(user);
        
        // Send verification email asynchronously
        // Return true immediately, as we've updated the user record
        // The email will be sent in the background
        emailService.sendVerificationEmailAsync(
            user.getEmail(),
            user.getFirstName() + " " + user.getLastName(),
            verificationCode
        );
        
        return true;
    }
    
    /**
     * Resend verification email for an unverified account - legacy method
     * @param email User's email
     * @return true if email sent successfully, false otherwise
     */
    public boolean resendVerificationEmail(String email) {
        return resendVerificationCode(email);
    }
    
    public void close() {
        userService.close();
    }
    
    // Find user by email
    public User findUserByEmail(String email) {
        return userService.findByEmail(email);
    }

    // Find user by phone
    public User findUserByPhone(String phone) {
        try {
            TypedQuery<User> query = userService.getEntityManager().createQuery(
                "SELECT u FROM User u WHERE u.phone = :phone", 
                User.class
            );
            query.setParameter("phone", phone);
            List<User> result = query.getResultList();
            return result.isEmpty() ? null : result.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Update user profile information
    public boolean updateUserProfile(User user) {
        try {
            userService.modifier(user);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Change user password
    public boolean changePassword(String email, String currentPassword, String newPassword) {
        // First authenticate with current password
        User user = authenticate(email, currentPassword);
        
        if (user == null) {
            return false;
        }
        
        try {
            // Hash the new password
            user.setPassword(PasswordUtils.hashPassword(newPassword));
            userService.modifier(user);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
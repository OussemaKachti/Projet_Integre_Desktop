package com.esprit.services;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.esprit.utils.EmailConfig;

import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class EmailService {

    // Thread pool for handling email sending in background
    private static final Executor emailExecutor = Executors.newFixedThreadPool(2);

    /**
     * Asynchronously sends an email on a background thread
     * 
     * @param to      Recipient email
     * @param subject Email subject
     * @param content Email content (HTML)
     * @return CompletableFuture that completes with true if sent successfully,
     *         false otherwise
     */
    public CompletableFuture<Boolean> sendEmailAsync(String to, String subject, String content) {
        return CompletableFuture.supplyAsync(() -> {
            return sendEmailInternal(to, subject, content);
        }, emailExecutor);
    }

    /**
     * Synchronously sends an email (for backwards compatibility)
     * 
     * @param to      Recipient email
     * @param subject Email subject
     * @param content Email content (HTML)
     * @return True if sent successfully
     */
    public boolean sendEmail(String to, String subject, String content) {
        // Start async operation but wait for result
        try {
            return sendEmailAsync(to, subject, content).join();
        } catch (Exception e) {
            System.out.println("Sync email send failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Internal method that actually sends the email
     */
    private boolean sendEmailInternal(String to, String subject, String content) {
        try {
            // Get email configuration
            Properties props = new Properties();
            props.put("mail.smtp.auth", EmailConfig.getProperties().getProperty("mail.smtp.auth"));
            props.put("mail.smtp.host", EmailConfig.getProperties().getProperty("mail.smtp.host"));
            props.put("mail.smtp.port", EmailConfig.getProperties().getProperty("mail.smtp.port"));
            props.put("mail.smtp.starttls.enable",
                    EmailConfig.getProperties().getProperty("mail.smtp.starttls.enable"));

            // Create session with authentication
            Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            EmailConfig.getUsername(),
                            EmailConfig.getPassword());
                }
            });

            // Create message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EmailConfig.getFromEmail()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(content, "text/html; charset=utf-8");

            // Send message
            Transport.send(message);
            System.out.println("Email sent successfully to: " + to);
            return true;
        } catch (Exception e) {
            System.out.println("Email send failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sends a verification email to a user asynchronously
     * 
     * @param email User's email
     * @param name  User's name
     * @param token Verification token
     * @return CompletableFuture that completes with true if sent successfully
     */
    public CompletableFuture<Boolean> sendVerificationEmailAsync(String email, String name, String code) {
        String subject = "Your UNICLUBS Verification Code";

        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
                "<h2 style='color: #00A0E3;'>Welcome to UNICLUBS!</h2>" +
                "<p>Hello " + name + ",</p>" +
                "<p>Thank you for creating an account. To verify your email address, please use the following verification code:</p>" +
                "<div style='background-color: #f4f4f4; padding: 20px; margin: 20px 0; text-align: center; border-radius: 5px;'>" +
                "<h2 style='margin: 0; color: #00A0E3; font-size: 32px; letter-spacing: 5px;'>" + code + "</h2>" +
                "</div>" +
                "<p>This verification code will expire in <strong>2 hours</strong>.</p>" +
                "<p>If you did not request this code, please ignore this email.</p>" +
                "<p style='margin-top: 30px; padding-top: 15px; border-top: 1px solid #eee; font-size: 12px; color: #666;'>" +
                "This is an automated message, please do not reply. If you need assistance, please contact support.</p>" +
                "</div>";

        return sendEmailAsync(email, subject, content);
    }

    /**
     * Synchronous version for backward compatibility
     */
    public boolean sendVerificationEmail(String email, String name, String code) {
        try {
            return sendVerificationEmailAsync(email, name, code).join();
        } catch (Exception e) {
            System.out.println("Sync verification email send failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sends a password reset email to a user asynchronously
     * 
     * @param email User's email
     * @param name  User's name
     * @param token Password reset token
     * @return CompletableFuture that completes with true if sent successfully
     */
    public CompletableFuture<Boolean> sendPasswordResetEmailAsync(String email, String name, String token) {
        String subject = "Your UNICLUBS Password Reset Code";

        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
                "<h2 style='color: #00A0E3;'>Password Reset</h2>" +
                "<p>Hello " + name + ",</p>" +
                "<p>We received a request to reset your password. To proceed, please use the following code:</p>" +
                "<div style='background-color: #f4f4f4; padding: 20px; margin: 20px 0; text-align: center; border-radius: 5px;'>" +
                "<h2 style='margin: 0; color: #00A0E3; font-size: 32px; letter-spacing: 5px;'>" + token + "</h2>" +
                "</div>" +
                "<p>This reset code will expire in <strong>2 hours</strong>.</p>" +
                "<p>If you did not request this code, please ignore this email or contact support if you have concerns.</p>" +
                "<p style='margin-top: 30px; padding-top: 15px; border-top: 1px solid #eee; font-size: 12px; color: #666;'>" +
                "This is an automated message, please do not reply. If you need assistance, please contact support.</p>" +
                "</div>";

        return sendEmailAsync(email, subject, content);
    }

    /**
     * Synchronous version for backward compatibility
     */
    public boolean sendPasswordResetEmail(String email, String name, String token) {
        try {
            return sendPasswordResetEmailAsync(email, name, token).join();
        } catch (Exception e) {
            System.out.println("Sync password reset email send failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sends a password change notification email to a user asynchronously
     * 
     * @param email User's email
     * @param name  User's name
     * @return CompletableFuture that completes with true if sent successfully
     */
    public CompletableFuture<Boolean> sendPasswordChangeNotificationAsync(String email, String name) {
        String subject = "Your UNICLUBS Password Has Been Changed";

        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
                "<h2 style='color: #00A0E3;'>Password Changed Successfully</h2>" +
                "<p>Hello " + name + ",</p>" +
                "<p>This email is to confirm that your UNICLUBS account password has been successfully changed.</p>" +
                "<p>If you did not make this change, please contact our support team immediately.</p>" +
                "<p style='margin-top: 30px; padding-top: 15px; border-top: 1px solid #eee; font-size: 12px; color: #666;'>" +
                "This is an automated message, please do not reply. If you need assistance, please contact support.</p>" +
                "</div>";

        return sendEmailAsync(email, subject, content);
    }

    /**
     * Synchronous version for backward compatibility
     */
    public boolean sendPasswordChangeNotification(String email, String name) {
        try {
            return sendPasswordChangeNotificationAsync(email, name).join();
        } catch (Exception e) {
            System.out.println("Sync password change notification email send failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sends a content warning notification email to a user asynchronously
     * 
     * @param email User's email
     * @param name User's name
     * @param warningCount Number of warnings the user has received
     * @param contentType Type of inappropriate content (e.g., "profile image", "profile data")
     * @param imageCaption Optional image caption to include if the warning is for an image
     * @return CompletableFuture that completes with true if sent successfully
     */
    public CompletableFuture<Boolean> sendContentWarningEmailAsync(String email, String name, int warningCount, String contentType, String imageCaption) {
        String subject = "UNICLUBS Content Policy Warning";

        // Set warning level
        String warningLevel = "first";
        if (warningCount == 2) {
            warningLevel = "second";
        } else if (warningCount >= 3) {
            warningLevel = "final";
        }

        // Set color based on severity
        String headerColor = "#FFA726"; // Orange for first warning
        String borderColor = "#FFB74D";
        
        if (warningCount == 2) {
            headerColor = "#F57C00"; // Darker orange for second warning
            borderColor = "#EF6C00";
        } else if (warningCount >= 3) {
            headerColor = "#D32F2F"; // Red for final warning
            borderColor = "#B71C1C";
        }

        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid " + borderColor + "; border-radius: 8px; overflow: hidden;'>" +
                "<div style='background-color: " + headerColor + "; padding: 15px; color: white;'>" +
                "<h2 style='margin: 0;'>Content Policy Warning</h2>" +
                "</div>" +
                "<div style='padding: 20px;'>" +
                "<p>Hello " + name + ",</p>" +
                "<p>Our automated system has detected potentially inappropriate content in your recent " + contentType + " submission.</p>" +
                "<p>This is your <strong>" + warningLevel + " warning</strong>. Please be mindful of our community guidelines and ensure all content you upload complies with our terms of use.</p>";

        // Add image caption if provided
        if (imageCaption != null && !imageCaption.isEmpty()) {
            content += "<div style='background-color: #f4f4f4; padding: 15px; border-left: 4px solid " + headerColor + "; margin: 15px 0;'>" +
                    "<p><strong>AI detected content:</strong> \"" + imageCaption + "\"</p>" +
                    "</div>";
        }

        // Add stronger warning for repeat offenders
        if (warningCount == 2) {
            content += "<p style='color: #F57C00; font-weight: bold;'>Please note that a third violation may result in your account being restricted.</p>";
        } else if (warningCount >= 3) {
            content += "<p style='color: #D32F2F; font-weight: bold;'>Important: Your account has been temporarily deactivated due to repeated content policy violations.</p>" +
                      "<p>To restore access to your account, please contact our support team.</p>";
        }

        content += "<p>If you believe this is a mistake, you can contact our support team.</p>" +
                "<p style='margin-top: 30px; padding-top: 15px; border-top: 1px solid #eee; font-size: 12px; color: #666;'>" +
                "This is an automated message, please do not reply. If you need assistance, please contact support.</p>" +
                "</div></div>";

        return sendEmailAsync(email, subject, content);
    }

    /**
     * Synchronous version for backward compatibility
     */
    public boolean sendContentWarningEmail(String email, String name, int warningCount, String contentType, String imageCaption) {
        try {
            return sendContentWarningEmailAsync(email, name, warningCount, contentType, imageCaption).join();
        } catch (Exception e) {
            System.out.println("Sync content warning email send failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Overloaded version without image caption for backward compatibility
     */
    public CompletableFuture<Boolean> sendContentWarningEmailAsync(String email, String name, int warningCount, String contentType) {
        return sendContentWarningEmailAsync(email, name, warningCount, contentType, null);
    }

    /**
     * Synchronous overloaded version without image caption for backward compatibility
     */
    public boolean sendContentWarningEmail(String email, String name, int warningCount, String contentType) {
        return sendContentWarningEmail(email, name, warningCount, contentType, null);
    }
}
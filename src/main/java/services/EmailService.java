package services;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import utils.EmailConfig;

public class EmailService {
    
    /**
     * Sends an email
     * 
     * @param to Recipient email
     * @param subject Email subject
     * @param content Email content (HTML)
     * @return True if sent successfully
     */
    public boolean sendEmail(String to, String subject, String content) {
        try {
            // Get email configuration
            Properties props = new Properties();
            props.put("mail.smtp.auth", EmailConfig.getProperties().getProperty("mail.smtp.auth"));
            props.put("mail.smtp.host", EmailConfig.getProperties().getProperty("mail.smtp.host"));
            props.put("mail.smtp.port", EmailConfig.getProperties().getProperty("mail.smtp.port"));
            props.put("mail.smtp.starttls.enable", EmailConfig.getProperties().getProperty("mail.smtp.starttls.enable"));
            
            // Create session with authentication
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                        EmailConfig.getUsername(), 
                        EmailConfig.getPassword()
                    );
                }
            });
            
            // Uncomment for debugging
            // session.setDebug(true);
            
            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(
                EmailConfig.getFromEmail(), 
                EmailConfig.getFromName()
            ));
            message.setRecipients(
                Message.RecipientType.TO, 
                InternetAddress.parse(to)
            );
            message.setSubject(subject);
            
            // Set content as HTML
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
     * Sends a verification email to a user
     * 
     * @param email User's email
     * @param name User's name
     * @param token Verification token
     * @return True if sent successfully
     */
public boolean sendVerificationEmail(String email, String name, String code) {
    String subject = "Your UNICLUBS Verification Code";
    
    String content = 
        "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
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
    
    return sendEmail(email, subject, content);
}
    /**
     * Sends a password reset email to a user
     * 
     * @param email User's email
     * @param name User's name
     * @param token Password reset token
     * @return True if sent successfully
     */
    public boolean sendPasswordResetEmail(String email, String name, String token) {
        String subject = "Reset Your Password";
        
        // Similar adjustment as for verification
        String resetUrl = EmailConfig.getAppUrl() + "/reset-password?token=" + token;
        
        // Build email content - focusing on the token
        String content = 
            "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
            "<h2>Password Reset Request</h2>" +
            "<p>Hello " + name + ",</p>" +
            "<p>We received a request to reset your password. Please use the following reset code:</p>" +
            "<div style='background-color: #f4f4f4; padding: 15px; margin: 15px 0; text-align: center;'>" +
            "<h3 style='margin: 0; color: #2196F3; font-size: 24px;'>" + token + "</h3>" +
            "</div>" +
            "<p>Open the application and navigate to the password reset page to enter this code.</p>" +
            "<p>If you did not request a password reset, please ignore this email.</p>" +
            "<p>Regards,<br>Club Management Team</p>" +
            "</div>";
        
        return sendEmail(email, subject, content);
    }
}
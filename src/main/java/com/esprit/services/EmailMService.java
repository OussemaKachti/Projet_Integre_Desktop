package com.esprit.services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailMService {

    private static final String FROM_EMAIL = "wahbisirine3@gmail.com";
    private static final String EMAIL_PASSWORD = "rmiv tndu ffjc deob"; // App password from Gmail

    /**
     * Sends a confirmation email to the club owner when the club is approved.
     * @param toEmail recipient's email
     * @param clubName name of the approved club
     * @throws MessagingException if sending fails
     */
    public static void sendClubApprovalEmail(String toEmail, String clubName) throws MessagingException {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            throw new MessagingException("Email address cannot be empty");
        }

        // Email content
        String subject = "🎉 Club Approved - Welcome to UniClub!";
        String body = String.format("""
                Hello,

                Welcome to UniClub! Your club '%s' has been successfully created and approved by the admin.

                Best regards,
                UniClub Team
                """, clubName);

        // SMTP properties for Gmail
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        // Auth session
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, EMAIL_PASSWORD);
            }
        });

        // Compose message
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);
        message.setText(body);

        // Send email
        Transport.send(message);
        System.out.println("✅ Email sent successfully to: " + toEmail);
    }
}


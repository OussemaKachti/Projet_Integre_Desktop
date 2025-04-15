package com.esprit.utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service pour l'envoi d'emails
 */
public class EmailService {
    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());
    
    private ConfigurationManager config;
    private Session session;
    
    private static EmailService instance;
    
    public static EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }
    
    private EmailService() {
        config = ConfigurationManager.getInstance();
        initSession();
    }
    
    /**
     * Initialise la session JavaMail
     */
    private void initSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", config.getSmtpHost());
        props.put("mail.smtp.port", config.getSmtpPort());
        
        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                    config.getSmtpUsername(), 
                    config.getSmtpPassword()
                );
            }
        });
    }
    
    /**
     * Recharge la configuration et réinitialise la session
     */
    public void reloadConfig() {
        initSession();
    }
    
    /**
     * Envoie un email simple
     * 
     * @param to adresse du destinataire
     * @param subject sujet de l'email
     * @param content contenu HTML de l'email
     * @return true si l'email a été envoyé avec succès
     */
    public boolean sendEmail(String to, String subject, String content) {
        try {
            Message message = prepareMessage(to, subject, content);
            Transport.send(message);
            LOGGER.info("Email envoyé avec succès à " + to);
            return true;
        } catch (MessagingException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'envoi de l'email à " + to, e);
            return false;
        }
    }
    
    /**
     * Envoie un email de manière asynchrone
     * 
     * @param to adresse du destinataire
     * @param subject sujet de l'email
     * @param content contenu HTML de l'email
     * @return CompletableFuture<Boolean> qui sera complété quand l'email sera envoyé
     */
    public CompletableFuture<Boolean> sendEmailAsync(String to, String subject, String content) {
        return CompletableFuture.supplyAsync(() -> sendEmail(to, subject, content));
    }
    
    private Message prepareMessage(String to, String subject, String content) throws MessagingException {
        Message message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(config.getFromEmail(), config.getFromName(), "UTF-8"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(content, "text/html; charset=utf-8");
            message.setSentDate(new Date());
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la préparation du message", e);
            throw new MessagingException("Encoding error", e);
        }
        return message;
    }
    
    /**
     * Crée un template HTML pour la notification d'un nouveau sondage
     * 
     * @param clubName nom du club
     * @param memberName prénom du membre
     * @param pollQuestion question du sondage
     * @param options liste des options du sondage
     * @return contenu HTML de l'email
     */
    public String createNewPollEmailTemplate(String clubName, String memberName, String pollQuestion, String[] options) {
        StringBuilder optionsHtml = new StringBuilder();
        for (String option : options) {
            optionsHtml.append("<div style=\"background-color: #f5f5f5; padding: 8px; margin: 5px 0; border-radius: 4px;\">")
                      .append(option)
                      .append("</div>");
        }
        
        return "<!DOCTYPE html>\n" +
               "<html>\n" +
               "<head>\n" +
               "    <meta charset=\"UTF-8\">\n" +
               "    <style>\n" +
               "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; }\n" +
               "        .header { background-color: #4b83cd; color: white; padding: 10px 20px; }\n" +
               "        .content { padding: 20px; }\n" +
               "        .footer { text-align: center; font-size: 12px; color: #888; margin-top: 30px; }\n" +
               "        .button { display: inline-block; background-color: #4b83cd; color: white; padding: 10px 20px; \n" +
               "                 text-decoration: none; border-radius: 5px; margin-top: 15px; }\n" +
               "        .choices { margin: 15px 0; }\n" +
               "        .choice { background-color: #f5f5f5; padding: 8px; margin: 5px 0; border-radius: 4px; }\n" +
               "    </style>\n" +
               "</head>\n" +
               "<body>\n" +
               "    <div class=\"header\">\n" +
               "        <h2>Nouveau sondage dans " + clubName + "</h2>\n" +
               "    </div>\n" +
               "    \n" +
               "    <div class=\"content\">\n" +
               "        <p>Bonjour " + memberName + ",</p>\n" +
               "        \n" +
               "        <p>Un nouveau sondage a été créé dans votre club <strong>" + clubName + "</strong>.</p>\n" +
               "        \n" +
               "        <h3>" + pollQuestion + "</h3>\n" +
               "        \n" +
               "        <div class=\"choices\">\n" +
               "            <p>Options disponibles:</p>\n" +
               optionsHtml.toString() +
               "        </div>\n" +
               "        \n" +
               "        <p>Connectez-vous à la plateforme pour participer au sondage.</p>\n" +
               "        \n" +
               "        <a href=\"#\" class=\"button\">Voter dans le sondage</a>\n" +
               "        \n" +
               "        <p>Merci de votre participation!</p>\n" +
               "    </div>\n" +
               "    \n" +
               "    <div class=\"footer\">\n" +
               "        <p>Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>\n" +
               "        <p>© " + java.time.Year.now().getValue() + " " + clubName + "</p>\n" +
               "    </div>\n" +
               "</body>\n" +
               "</html>";
    }
} 
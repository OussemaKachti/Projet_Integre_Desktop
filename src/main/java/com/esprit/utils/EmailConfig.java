package com.esprit.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class EmailConfig {
    private static final Properties properties = new Properties();
    
    static {
        try (InputStream input = EmailConfig.class.getClassLoader().getResourceAsStream("com/esprit/email.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find email.properties");
                // Default Mailtrap settings
                properties.put("mail.smtp.auth", "true");
                properties.put("mail.smtp.host", "smtp.mailtrap.io");
                properties.put("mail.smtp.port", "2525");
                properties.put("mail.smtp.starttls.enable", "true");
                properties.put("mail.username", "your-mailtrap-username");
                properties.put("mail.password", "your-mailtrap-password");
                properties.put("mail.from", "noreply@yourapp.com");
                properties.put("mail.from.name", "Club Management System");
                properties.put("app.url", "http://localhost:8080");
            } else {
                properties.load(input);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public static Properties getProperties() {
        return properties;
    }
    
    public static String getUsername() {
        return properties.getProperty("mail.username");
    }
    
    public static String getPassword() {
        return properties.getProperty("mail.password");
    }
    
    public static String getFromEmail() {
        return properties.getProperty("mail.from");
    }
    
    public static String getFromName() {
        return properties.getProperty("mail.from.name");
    }
    
    public static String getAppUrl() {
        return properties.getProperty("app.url");
    }
}

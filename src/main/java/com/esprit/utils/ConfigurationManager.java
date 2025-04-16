package com.esprit.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestionnaire de configuration de l'application
 * Permet de sauvegarder et charger des paramètres de configuration
 */
public class ConfigurationManager {
    private static final Logger LOGGER = Logger.getLogger(ConfigurationManager.class.getName());
    private static final String CONFIG_FILE = "config.properties";
    
    private static ConfigurationManager instance;
    private Properties properties;
    
    // Paramètres par défaut
    private static final String DEFAULT_SMTP_HOST = "sandbox.smtp.mailtrap.io";
    private static final String DEFAULT_SMTP_PORT = "2525";
    private static final String DEFAULT_SMTP_USERNAME = "";
    private static final String DEFAULT_SMTP_PASSWORD = "";
    private static final String DEFAULT_FROM_EMAIL = "admin@gmail.com";
    private static final String DEFAULT_FROM_NAME = "Club PI";
    
    private ConfigurationManager() {
        properties = new Properties();
        loadConfig();
    }
    
    public static ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }
        return instance;
    }
    
    /**
     * Charge la configuration depuis le fichier properties
     */
    private void loadConfig() {
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
            LOGGER.info("Configuration loaded successfully");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not load configuration file, using defaults", e);
            setDefaultProperties();
        }
    }
    
    /**
     * Définit les propriétés par défaut
     */
    private void setDefaultProperties() {
        properties.setProperty("smtp.host", DEFAULT_SMTP_HOST);
        properties.setProperty("smtp.port", DEFAULT_SMTP_PORT);
        properties.setProperty("smtp.username", DEFAULT_SMTP_USERNAME);
        properties.setProperty("smtp.password", DEFAULT_SMTP_PASSWORD);
        properties.setProperty("email.from.address", DEFAULT_FROM_EMAIL);
        properties.setProperty("email.from.name", DEFAULT_FROM_NAME);
        
        saveConfig();
    }
    
    /**
     * Sauvegarde la configuration dans le fichier properties
     */
    public void saveConfig() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "ClubPI Application Configuration");
            LOGGER.info("Configuration saved successfully");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not save configuration file", e);
        }
    }
    
    /**
     * Récupère une propriété
     * 
     * @param key clé de la propriété
     * @return valeur de la propriété ou valeur par défaut
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * Récupère une propriété avec une valeur par défaut
     * 
     * @param key clé de la propriété
     * @param defaultValue valeur par défaut
     * @return valeur de la propriété ou valeur par défaut
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Définit une propriété
     * 
     * @param key clé de la propriété
     * @param value valeur de la propriété
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }
    
    /**
     * Obtient le nom d'hôte SMTP
     * 
     * @return hostname SMTP
     */
    public String getSmtpHost() {
        return getProperty("smtp.host", DEFAULT_SMTP_HOST);
    }
    
    /**
     * Obtient le port SMTP
     * 
     * @return port SMTP
     */
    public String getSmtpPort() {
        return getProperty("smtp.port", DEFAULT_SMTP_PORT);
    }
    
    /**
     * Obtient le nom d'utilisateur SMTP
     * 
     * @return username SMTP
     */
    public String getSmtpUsername() {
        return getProperty("smtp.username", DEFAULT_SMTP_USERNAME);
    }
    
    /**
     * Obtient le mot de passe SMTP
     * 
     * @return password SMTP
     */
    public String getSmtpPassword() {
        return getProperty("smtp.password", DEFAULT_SMTP_PASSWORD);
    }
    
    /**
     * Obtient l'adresse email d'expédition
     * 
     * @return adresse email d'expédition
     */
    public String getFromEmail() {
        return getProperty("email.from.address", DEFAULT_FROM_EMAIL);
    }
    
    /**
     * Obtient le nom d'expédition
     * 
     * @return nom d'expédition
     */
    public String getFromName() {
        return getProperty("email.from.name", DEFAULT_FROM_NAME);
    }
    
    /**
     * Définit les paramètres SMTP
     * 
     * @param host nom d'hôte SMTP
     * @param port port SMTP
     * @param username nom d'utilisateur SMTP
     * @param password mot de passe SMTP
     * @param fromEmail adresse email d'expédition
     * @param fromName nom d'expédition
     */
    public void setEmailConfig(String host, String port, String username, String password, 
                              String fromEmail, String fromName) {
        setProperty("smtp.host", host);
        setProperty("smtp.port", port);
        setProperty("smtp.username", username);
        setProperty("smtp.password", password);
        setProperty("email.from.address", fromEmail);
        setProperty("email.from.name", fromName);
        saveConfig();
    }
} 
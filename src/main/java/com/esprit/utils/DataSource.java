package com.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DataSource {
    private static DataSource instance;
    private Connection cnx;

    private final String url = "jdbc:mysql://localhost:3306/dbpi";
    private final String user = "root";
    private final String password = "";

    private DataSource() {
        try {
            // Connexion à la base de données
            cnx = DriverManager.getConnection(url, user, password);

            // Vérification si la connexion est réussie en envoyant une requête simple
            if (cnx != null && !cnx.isClosed()) {
                System.out.println("Connected to Database!");
                // Vous pouvez aussi tester avec une requête simple ici, par exemple :
                Statement stmt = cnx.createStatement();
                stmt.executeQuery("SELECT 1"); // Requête simple pour tester la connexion
            }
        } catch (SQLException ex) {
            System.err.println("Error connecting to database: " + ex.getMessage());
        }
    }

    public static DataSource getInstance() {
        if (instance == null) {
            instance = new DataSource();
        }
        return instance;
    }

    public Connection getCnx() {
        try {
            // Check if connection is still valid
            if (cnx == null || cnx.isClosed() || !isConnectionValid()) {
                System.out.println("Connection is invalid or closed, attempting to reconnect...");
                connectWithRetry();
            }
        } catch (SQLException e) {
            System.err.println("Error checking connection: " + e.getMessage());
            connectWithRetry();
        }
        return cnx;
    }

    // Méthode pour vérifier la connexion
    public boolean isConnected() {
        try {
            if (cnx != null && !cnx.isClosed()) {
                return true; // La connexion est ouverte et valide
            }
        } catch (SQLException ex) {
            System.err.println("Error checking connection: " + ex.getMessage());
        }
        return false; // La connexion est fermée ou invalide
    }
    
}

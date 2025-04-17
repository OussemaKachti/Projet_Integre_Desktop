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
    
    // Maximum number of connection attempts
    private final int MAX_RETRIES = 3;
    private final int RETRY_DELAY_MS = 2000; // 2 seconds

    private DataSource() {
        connectWithRetry();
    }
    
    private void connectWithRetry() {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Explicitly load MySQL driver
                Class.forName("com.mysql.cj.jdbc.Driver");
                
                // Connection to database
                cnx = DriverManager.getConnection(url, user, password);

                // Verify connection
                if (cnx != null && !cnx.isClosed()) {
                    System.out.println("Connected to Database!");
                    // Test with a simple query
                    Statement stmt = cnx.createStatement();
                    stmt.executeQuery("SELECT 1");
                    return; // Success, exit the method
                }
            } catch (ClassNotFoundException e) {
                System.err.println("MySQL JDBC driver not found: " + e.getMessage());
                break; // No need to retry if driver is missing
            } catch (SQLException ex) {
                System.err.println("Connection attempt " + attempt + " failed: " + ex.getMessage());
                if (attempt < MAX_RETRIES) {
                    System.out.println("Retrying connection in " + (RETRY_DELAY_MS/1000) + " seconds...");
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    System.err.println("Failed to connect to database after " + MAX_RETRIES + " attempts.");
                }
            }
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
    
    private boolean isConnectionValid() {
        try {
            if (cnx != null) {
                // Test if connection is valid with 5 second timeout
                return cnx.isValid(5);
            }
        } catch (SQLException e) {
            System.err.println("Error checking connection validity: " + e.getMessage());
        }
        return false;
    }

    // Method to check connection
    public boolean isConnected() {
        try {
            return cnx != null && !cnx.isClosed() && isConnectionValid();
        } catch (SQLException ex) {
            System.err.println("Error checking connection: " + ex.getMessage());
            return false;
        }
    }

    // Getter for url
    public String getUrl() {
        return url;
    }
    
    // Getter for user 
    public String getUser() {
        return user;
    }
}

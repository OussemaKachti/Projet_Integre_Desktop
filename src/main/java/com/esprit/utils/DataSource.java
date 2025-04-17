package com.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataSource {
    private static final Logger LOGGER = Logger.getLogger(DataSource.class.getName());
    private static DataSource instance;
    private Connection cnx;

    private final String url = "jdbc:mysql://localhost:3306/dbpi?serverTimezone=UTC";
    private final String user = "root";
    private final String password = "";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY = 2000; // 2 seconds

    private DataSource() {
        initializeConnection();
    }

    private void initializeConnection() {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES && (cnx == null || !isConnectionValid())) {
            try {
                // Load the JDBC driver explicitly
                Class.forName("com.mysql.cj.jdbc.Driver");
                
                cnx = DriverManager.getConnection(url, user, password);
                LOGGER.info("Successfully connected to database!");
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Database connection failed (Attempt " + (retryCount + 1) + "/" + MAX_RETRIES + "): " + ex.getMessage());
                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.SEVERE, "MySQL JDBC Driver not found: " + e.getMessage());
                break;
            }
        }

        if (cnx == null) {
            LOGGER.severe("Failed to establish database connection after " + MAX_RETRIES + " attempts");
            throw new RuntimeException("Unable to establish database connection");
        }
    }

    private boolean isConnectionValid() {
        try {
            return cnx != null && !cnx.isClosed() && cnx.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    public static DataSource getInstance() {
        if (instance == null) {
            instance = new DataSource();
        } else if (!instance.isConnectionValid()) {
            // If connection is invalid, reinitialize it
            instance.initializeConnection();
        }
        return instance;
    }

    public Connection getCnx() {
        if (!isConnectionValid()) {
            initializeConnection();
        }
        return cnx;
    }
}
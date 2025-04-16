package com.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to manage database connections
 */
public class DatabaseConnection {
    
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    
    // Connection details - these would normally be in a properties file
    private static final String URL = "jdbc:mysql://localhost:3306/dbpi";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    
    private static Connection connection;
    
    /**
     * Get a connection to the database
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Load JDBC driver
                Class.forName("com.mysql.cj.jdbc.Driver");
                
                // Create connection
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                LOGGER.info("Database connection established");
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.SEVERE, "MySQL JDBC Driver not found", e);
                throw new SQLException("MySQL JDBC Driver not found", e);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to connect to database", e);
                throw e;
            }
        }
        
        return connection;
    }
    
    /**
     * Backward compatibility method for code using getInstance()
     * This method simply delegates to getConnection()
     * @return Connection object
     */
    public static Connection getInstance() {
        try {
            return getConnection();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get connection in getInstance()", e);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Close the database connection
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                LOGGER.info("Database connection closed");
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing database connection", e);
            }
        }
    }
} 
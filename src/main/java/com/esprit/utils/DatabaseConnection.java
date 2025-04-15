package com.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private final String url = "jdbc:mysql://localhost:3306/dbpi";
    private final String user = "root";
    private final String password = "";
    private Connection cnx;
    private static DatabaseConnection instance;

    private DatabaseConnection() {
        try {
            cnx = DriverManager.getConnection(url, user, password);
            System.out.println("Connexion établie");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public static DatabaseConnection getInstance() {
        if (instance == null)
            instance = new DatabaseConnection();
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }
}

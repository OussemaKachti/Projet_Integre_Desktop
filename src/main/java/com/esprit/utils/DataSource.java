package com.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataSource {
    private static DataSource instance;
    private Connection cnx;

    private final String url = "jdbc:mysql://localhost:3306/dbpi";
    private final String user = "root";
    private final String password = "";


    private DataSource() {
        try {
            cnx = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to Database!");
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public static DataSource getInstance() {
        if (instance == null) {
            instance = new DataSource();
        }
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }
}
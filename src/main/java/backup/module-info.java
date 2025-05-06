module com.esprit {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jakarta.persistence;
    requires javafx.base;
    requires java.net.http;
    requires jakarta.validation;
    requires jakarta.mail;
    requires org.json;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    // Use the jar name as automatic module name
    requires org.mindrot.jbcrypt;
    requires org.kordamp.ikonli.javafx;

    // Open all modules to allow reflection access
    opens com.esprit to javafx.fxml;
    opens com.esprit.controllers to javafx.fxml;
    opens com.esprit.models to jakarta.persistence, javafx.base;
    opens com.esprit.utils;
    opens com.esprit.services;

    exports com.esprit;
    exports com.esprit.models;
    exports com.esprit.controllers;
    exports com.esprit.services;
    exports com.esprit.utils;
}
module com.example.projetpi {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.esprit to javafx.fxml;
    opens com.esprit.controllers to javafx.fxml;

    exports com.esprit;

}
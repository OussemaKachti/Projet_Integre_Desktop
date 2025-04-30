@echo off
REM Run script for JavaFX application with proper module path configuration

REM Set JAVA_HOME if not set correctly in environment
REM SET JAVA_HOME=C:\path\to\your\jdk

REM Run the application
mvn clean javafx:run -Djavafx.modules=javafx.controls,javafx.fxml,javafx.graphics,javafx.web

echo.
echo If you encounter issues, make sure your pom.xml has the correct JavaFX dependencies
echo and that you have Java 11 or later installed. 
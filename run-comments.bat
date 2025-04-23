@echo off
echo Starting Comments Management Application...

REM Set JAVA_HOME if not set correctly in environment
REM SET JAVA_HOME=C:\path\to\your\jdk

REM Run the application with explicit JavaFX modules
mvn clean javafx:run -Djavafx.modules=javafx.controls,javafx.fxml,javafx.graphics -Djavafx.mainClass=com.esprit.AdminCommentsApplication

echo.
echo If you encounter issues, make sure:
echo 1. Your pom.xml has the correct JavaFX dependencies
echo 2. You have Java 17 or later installed
echo 3. MySQL is running on port 3306 
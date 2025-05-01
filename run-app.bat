@echo off
echo Starting Task Manager Application...
java --module-path "C:\Program Files\Java\javafx-sdk-23.0.2\lib" --add-modules javafx.controls,javafx.fxml -jar target/TaskManagerApp-1.0-SNAPSHOT.jar
pause 
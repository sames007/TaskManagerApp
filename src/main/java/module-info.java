module edu.farmingdale.taskmanagerapp {
    requires java.sql;
    requires java.net.http;
    requires javafx.controls;
    requires javafx.fxml;
    requires jfxtras.agenda;
    requires java.desktop;
    requires mysql.connector.j;
    requires annotations;
    requires java.prefs;
    requires com.google.gson;

    opens edu.farmingdale.taskmanagerapp to javafx.fxml, com.google.gson;
    exports edu.farmingdale.taskmanagerapp;
}

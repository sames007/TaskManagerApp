module edu.farmingdale.taskmanagerapp {
    requires java.sql;
    requires java.net.http;
    requires javafx.controls;
    requires javafx.fxml;
    requires jfxtras.agenda;
    requires java.desktop;
    requires mysql.connector.j;

    opens edu.farmingdale.taskmanagerapp to javafx.fxml;
    exports edu.farmingdale.taskmanagerapp;
}

module edu.farmingdale.taskmanagerapp {
    requires javafx.controls;
    requires javafx.fxml;


    opens edu.farmingdale.taskmanagerapp to javafx.fxml;
    exports edu.farmingdale.taskmanagerapp;
}
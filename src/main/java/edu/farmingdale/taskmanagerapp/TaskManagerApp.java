package edu.farmingdale.taskmanagerapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

import static javafx.scene.control.PopupControl.USE_COMPUTED_SIZE;

/**
 * Main application class that loads the FXML view,
 * applies the external CSS file for styling, and shows the stage.
 */
public class TaskManagerApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load the main FXML layout
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/TaskManagerView.fxml"));

        // Create a DatabaseManager instance
        DatabaseManager dbManager = new DatabaseManager();

        // Load the FXML file
        Parent root = loader.load();

        // Get the TaskManagerController instance and inject the DatabaseManager
        TaskManagerController taskManagerController = loader.getController();
        taskManagerController.setDatabaseManager(dbManager);

        // Create a new scene with specified width and height
        Scene scene = new Scene(root, USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);

        // Apply external CSS files for styling the UI and chat window
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/styles.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/ChatBox.css")).toExternalForm());

        // Set the window title and scene, then display the stage
        primaryStage.setTitle("Task Management System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}

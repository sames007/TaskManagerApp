package edu.farmingdale.taskmanagerapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


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

        // Get the TaskManagerController instance
        TaskManagerController taskManagerController = loader.getController();
        taskManagerController.setDatabaseManager(dbManager);

        // Create a new scene with width 800 and height 600
        Scene scene = new Scene(root, 800, 600);

        // Apply the external CSS file to style the UI
        scene.getStylesheets().add(getClass().getResource("/edu/farmingdale/taskmanagerapp/styles.css").toExternalForm());

        // Set the window title and scene, then display the stage
        primaryStage.setTitle("Task Management System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
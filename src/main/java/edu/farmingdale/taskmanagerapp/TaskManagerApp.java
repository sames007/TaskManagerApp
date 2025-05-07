package edu.farmingdale.taskmanagerapp;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static javafx.scene.control.PopupControl.USE_COMPUTED_SIZE;

/**
 * The main application class that loads the FXML view,
 * applies the external CSS file for styling, and shows the stage.
 */
public class TaskManagerApp extends Application {

    private DatabaseManager dbManager; // Store instance for reuse

    @Override
    public void start(@NotNull Stage primaryStage) throws Exception {
        dbManager = new DatabaseManager();

        // Load splash screen
        FXMLLoader splashLoader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/SplashScreen.fxml"));
        Parent splashRoot = splashLoader.load();

        // Match size of TaskManagerView by loading it offscreen
        FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/TaskManagerView.fxml"));
        Parent mainRoot = mainLoader.load();
        double width = mainRoot.prefWidth(-1);
        double height = mainRoot.prefHeight(-1);

        Scene splashScene = new Scene(splashRoot, width, height);
        primaryStage.setTitle("Loading...");
        primaryStage.setScene(splashScene);
        primaryStage.show();

        // Fade-in animation
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), splashRoot);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        PauseTransition pause = new PauseTransition(Duration.seconds(1.5));

        // Fade-out
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), splashRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);


        // After fade-out, switch to main scene
        fadeOut.setOnFinished(event -> {
            try {
                TaskManagerController controller = mainLoader.getController();
                controller.setDatabaseManager(dbManager);

                Scene mainScene = new Scene(mainRoot, width, height);
                mainScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/styles.css")).toExternalForm());
                mainScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/ChatBox.css")).toExternalForm());

                primaryStage.setTitle("Task Management System");
                primaryStage.setScene(mainScene);
            } catch (Exception e) {
                e.printStackTrace();
                Platform.exit();
            }
        });

        fadeIn.setOnFinished(e -> pause.play());
        pause.setOnFinished(e -> fadeOut.play());
        fadeIn.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

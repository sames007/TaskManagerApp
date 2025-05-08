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

/**
 * The main entry point for the Task Manager JavaFX application.
 * <p>
 * This class shows a splash screen with animation and then transitions
 * into the login screen, while keeping the window dimensions consistent.
 * </p>
 * It also initializes and passes a shared instance of {@link DatabaseManager}
 * to the main controller and login controller.
 */
public class TaskManagerApp extends Application {

    /** A shared instance of the database manager for all controllers */
    private DatabaseManager dbManager;

    /**
     * Application startup method.
     * Loads the splash screen and then transitions to the login screen.
     *
     * @param primaryStage The main application window (Stage)
     * @throws Exception If loading FXML resources fails
     */
    @Override
    public void start(@NotNull Stage primaryStage) throws Exception {
        // Initialize shared database manager
        dbManager = new DatabaseManager();

        // Load the splash screen
        FXMLLoader splashLoader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/SplashScreen.fxml"));
        Parent splashRoot = splashLoader.load();

        // Preload main view just to measure size (for consistent window size)
        FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/TaskManagerView.fxml"));
        Parent mainRoot = mainLoader.load();
        double width = mainRoot.prefWidth(-1);
        double height = mainRoot.prefHeight(-1);

        // Show splash screen with computed size
        Scene splashScene = new Scene(splashRoot, width, height);
        primaryStage.setTitle("Loading...");
        primaryStage.setScene(splashScene);
        primaryStage.show();

        // Fade in splash screen
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), splashRoot);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        // Wait before transitioning
        PauseTransition pause = new PauseTransition(Duration.seconds(1.5));

        // Fade out splash screen
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), splashRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        // After fade-out, show login screen
        fadeOut.setOnFinished(event -> {
            try {
                // Load login screen
                FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/LoginView.fxml"));
                Parent loginRoot = loginLoader.load();

                // Inject shared main controller and DB manager
                LoginController loginController = loginLoader.getController();
                TaskManagerController mainController = mainLoader.getController();
                mainController.setDatabaseManager(dbManager);
                loginController.setMainController(mainController);

                // Set up the login scene with same dimensions
                Scene loginScene = new Scene(loginRoot, width, height);
                loginScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/styles.css")).toExternalForm());

                primaryStage.setTitle("Login");
                primaryStage.setScene(loginScene);
            } catch (Exception e) {
                e.printStackTrace();
                Platform.exit();
            }
        });

        // Sequence of animations
        fadeIn.setOnFinished(e -> pause.play());
        pause.setOnFinished(e -> fadeOut.play());
        fadeIn.play();
    }

    /**
     * Main method — entry point for the Java application.
     *
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {
        launch(args);
    }
}

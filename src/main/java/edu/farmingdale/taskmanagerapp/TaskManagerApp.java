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

import java.util.logging.Level;
import java.util.logging.Logger;

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
    private static final Logger LOGGER = Logger.getLogger(TaskManagerApp.class.getName());

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
        dbManager = new DatabaseManager();

        FXMLLoader splashLoader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/SplashScreen.fxml"));
        Parent splashRoot = splashLoader.load();

        // Preload the main view so the splash and login windows use the final app size.
        FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/TaskManagerView.fxml"));
        Parent mainRoot = mainLoader.load();
        double width = mainRoot.prefWidth(-1);
        double height = mainRoot.prefHeight(-1);

        Scene splashScene = new Scene(splashRoot, width, height);
        ThemeManager.bindToSystemTheme(splashScene);
        primaryStage.setTitle("Loading...");
        primaryStage.setScene(splashScene);
        primaryStage.show();

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), splashRoot);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        PauseTransition pause = new PauseTransition(Duration.seconds(1.5));

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), splashRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(event -> {
            try {
                FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/LoginView.fxml"));
                Parent loginRoot = loginLoader.load();

                LoginController loginController = loginLoader.getController();
                TaskManagerController mainController = mainLoader.getController();
                mainController.setDatabaseManager(dbManager);
                loginController.setMainController(mainController);

                Scene loginScene = new Scene(loginRoot, width, height);
                ThemeManager.bindToSystemTheme(loginScene);

                primaryStage.setTitle("Login");
                primaryStage.setScene(loginScene);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unable to load login screen.", e);
                Platform.exit();
            }
        });

        fadeIn.setOnFinished(e -> pause.play());
        pause.setOnFinished(e -> fadeOut.play());
        fadeIn.play();
    }

    /**
     * Main method and entry point for the Java application.
     *
     * @param args command-line arguments passed to JavaFX
     */
    public static void main(String[] args) {
        launch(args);
    }
}

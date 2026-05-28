package edu.farmingdale.taskmanagerapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Login screen.
 */
public class LoginController {
    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    @FXML
    private TextField emailTextField;

    @FXML
    private PasswordField passwordTextField;

    @FXML
    private Button loginButton;

    @FXML
    private Hyperlink registerLink;

    @FXML
    private Hyperlink forgotPasswordLink;

    private TaskManagerController mainController;

    /**
     * Initializes the controller.
     */
    @FXML
    public void initialize() {
        if (forgotPasswordLink != null) {
            forgotPasswordLink.setOnAction(e -> handleForgotPassword());
        }
    }

    private void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/ForgotPasswordView.fxml"));
            Parent root = loader.load();

            ForgotPasswordController controller = loader.getController();
            controller.setDatabaseManager(mainController.getDbManager());

            Stage dialog = new Stage();
            dialog.setTitle("Forgot Password");
            dialog.initOwner(emailTextField.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            Scene scene = new Scene(root);
            ThemeManager.bindToSystemTheme(scene);
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (IOException e) {
            mainController.showAlert("Failed to open password recovery: " + e.getMessage());
        }
    }

    /**
     * Injects the shared TaskManagerController from the application.
     *
     * @param controller the main TaskManagerController
     */
    public void setMainController(TaskManagerController controller) {
        this.mainController = controller;
    }

    /**
     * Handles the Log In button click.
     * Validates credentials and opens the main dashboard if successful.
     *
     * @param actionEvent the button click event
     */
    @FXML
    public void login(ActionEvent actionEvent) {
        try {
            String email = emailTextField.getText().trim();
            String password = passwordTextField.getText();

            if (!validateInputs(email, password)) {
                return;
            }

            UserSession authenticatedUser = authenticateUser(email, password);
            if (authenticatedUser == null) {
                return;
            }

            openMainDashboard(actionEvent, authenticatedUser);
            
        } catch (Exception e) {
            handleLoginError(e);
        }
    }

    /**
     * Validates the user input fields
     */
    private boolean validateInputs(@NotNull String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            mainController.showAlert("Please enter your email or username and password.");
            return false;
        }
        return true;
    }

    /**
     * Authenticates the user against the database
     */
    @Nullable
    private UserSession authenticateUser(String email, String password) {
        if (!mainController.isDatabaseAvailable()) {
            return createOfflineUser(email, password);
        }

        UserSession authenticatedUser = mainController.getDbManager().authenticateUser(email, password);

        if (authenticatedUser == null || authenticatedUser.getUserName().isEmpty()) {
            mainController.showAlert("Login failed. Please check your credentials.");
            return null;
        }
        return authenticatedUser;
    }

    private UserSession createOfflineUser(String identifier, String password) {
        String userName = identifier.contains("@")
                ? identifier.substring(0, identifier.indexOf('@'))
                : identifier;
        String email = identifier.contains("@") ? identifier : identifier + "@offline.local";

        UserSession offlineUser = new UserSession(userName, email, password, "", "");
        offlineUser.setUserID(Math.abs(email.toLowerCase().hashCode()));
        return offlineUser;
    }

    /**
     * Opens the main dashboard after successful login
     */
    private void openMainDashboard(@NotNull ActionEvent actionEvent, UserSession authenticatedUser) throws IOException {
        mainController.setCurrentUser(authenticatedUser);
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();

        FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/TaskManagerView.fxml"));
        Parent mainRoot = mainLoader.load();

        TaskManagerController controller = mainLoader.getController();
        controller.setDatabaseManager(mainController.getDbManager());
        controller.setCurrentUser(authenticatedUser);

        Scene mainScene = new Scene(mainRoot, stage.getScene().getWidth(), stage.getScene().getHeight());
        loadStylesheets(mainScene);

        stage.setTitle("Task Manager");
        stage.setScene(mainScene);
    }

    /**
     * Loads the required stylesheets for the scene
     */
    private void loadStylesheets(@NotNull Scene scene) {
        ThemeManager.bindToSystemTheme(scene);
    }

    /**
     * Handles any errors that occur during login
     */
    private void handleLoginError(@NotNull Exception e) {
        LOGGER.log(Level.WARNING, "Login failed.", e);
        mainController.showAlert("Login error: " + e.getMessage());
    }

    /**
     * Opens the Sign-Up screen when the register link is clicked.
     */
    @FXML
    public void openSignUpScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/SignUpView.fxml"));
            Parent root = loader.load();

            SignUpController signUpController = loader.getController();
            signUpController.setMainController(mainController);

            Stage stage = (Stage) emailTextField.getScene().getWindow();
            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            ThemeManager.bindToSystemTheme(scene);
            
            stage.setTitle("Sign Up");
            stage.setScene(scene);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to open sign-up screen.", e);
            mainController.showAlert("Failed to open Sign Up screen: " + e.getMessage());
        }
    }
}

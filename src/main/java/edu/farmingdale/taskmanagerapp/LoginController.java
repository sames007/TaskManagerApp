package edu.farmingdale.taskmanagerapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Controller for the Login screen.
 * <p>
 * Handles user input for logging in, navigation to the sign-up screen,
 * and showing forgotten password alert.
 * </p>
 */
public class LoginController {

    @FXML
    private TextField emailTextField;

    @FXML
    private TextField passwordTextField;

    @FXML
    private Button loginButton;

    @FXML
    private Hyperlink registerLink;

    @FXML
    private Hyperlink forgotPasswordLink;

    private TaskManagerController mainController;

    /**
     * Initializes the controller.
     * Binds link actions such as register and forgot password.
     */
    @FXML
    public void initialize() {
        // Set behavior for "Register here" link
        if (registerLink != null) {
            registerLink.setOnAction(e -> openSignUpScreen());
        }

        // Set behavior for "Forgot your password?" link
        if (forgotPasswordLink != null) {
            forgotPasswordLink.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Forgot Password");
                alert.setHeaderText("Password Recovery");
                alert.setContentText("Please contact support or check your email for password recovery.");
                alert.showAndWait();
            });
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
     * Handles the Log In button.
     * Validates credentials and opens the main dashboard if successful.
     *
     * @param actionEvent the button click event
     */
    @FXML
    public void login(ActionEvent actionEvent) {
        try {
            String email = emailTextField.getText().trim();
            String password = passwordTextField.getText().trim();

            if (email.isEmpty() || password.isEmpty()) {
                mainController.showAlert("Please enter both email and password.");
                return;
            }

            UserSession attemptedLogin = new UserSession("", email, password);
            UserSession authenticatedUser = mainController.getDbManager().getAccount(attemptedLogin.getEmail());

            if (authenticatedUser == null || authenticatedUser.getUserName().isEmpty() ||
                    !authenticatedUser.getPassword().equals(password)) {
                mainController.showAlert("Login failed. Please check your credentials.");
                return;
            }

            // Set current user and transition to main view
            mainController.setCurrentUser(authenticatedUser);
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();

            FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/TaskManagerView.fxml"));
            Parent mainRoot = mainLoader.load();

            TaskManagerController controller = mainLoader.getController();
            controller.setDatabaseManager(mainController.getDbManager());
            controller.setCurrentUser(authenticatedUser);

            Scene mainScene = new Scene(mainRoot, stage.getScene().getWidth(), stage.getScene().getHeight());
            mainScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/styles.css")).toExternalForm());
            mainScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/ChatBox.css")).toExternalForm());

            stage.setTitle("Task Manager");
            stage.setScene(mainScene);
        } catch (Exception e) {
            e.printStackTrace();
            mainController.showAlert("Login error: " + e.getMessage());
        }
    }

    /**
     * Opens the Sign Up screen in the same window.
     */
    private void openSignUpScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/SignUpView.fxml"));
            Parent root = loader.load();

            SignUpController signUpController = loader.getController();
            signUpController.setMainController(mainController);

            Stage stage = (Stage) emailTextField.getScene().getWindow();
            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/styles.css")).toExternalForm());
            stage.setScene(scene);
        } catch (IOException e) {
            mainController.showAlert("Failed to open Sign Up screen: " + e.getMessage());
        }
    }
}

package edu.farmingdale.taskmanagerapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

/**
 * Controller for the Login screen.
 */
public class LoginController {

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

    /**
     * Handles the forgot password functionality
     */
    private void handleForgotPassword() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Forgot Password");
        alert.setHeaderText("Password Recovery");
        alert.setContentText("Please contact support or check your email for password recovery.");
        alert.showAndWait();
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
            String emailOrUsername = emailTextField.getText().trim();
            String password = passwordTextField.getText().trim();

            if (!validateInputs(emailOrUsername, password)) {
                return;
            }

            UserSession authenticatedUser = authenticateUser(emailOrUsername, password);
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
    private boolean validateInputs(@NotNull String emailOrUsername, String password) {
        if (emailOrUsername.isEmpty() || password.isEmpty()) {
            mainController.showAlert("Please enter both email or username and password.");
            return false;
        }
        return true;
    }

    /**
     * Authenticates the user against the database
     */
    @Nullable
    private UserSession authenticateUser(String emailOrUsername, String password) {
        // Try by email first
        UserSession authenticatedUser = mainController.getDbManager().getAccountByEmail(emailOrUsername);
        if (authenticatedUser == null) {
            // Try by username if not found by email
            authenticatedUser = mainController.getDbManager().getAccount(emailOrUsername);
        }
        if (authenticatedUser == null || authenticatedUser.getUserName().isEmpty() ||
                !authenticatedUser.getPassword().equals(password)) {
            mainController.showAlert("Login failed. Please check your credentials.");
            return null;
        }
        // Ensure userID is set
        UserSession dbUser = mainController.getDbManager().getAccount(authenticatedUser.getUserName());
        if (dbUser != null) {
            authenticatedUser.setUserID(dbUser.getUserID());
        }
        System.out.println("[DEBUG] Logged in userID: " + authenticatedUser.getUserID());
        return authenticatedUser;
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
        scene.getStylesheets().addAll(
            Objects.requireNonNull(getClass().getResource("styling/styles.css")).toExternalForm(),
            Objects.requireNonNull(getClass().getResource("styling/ChatBox.css")).toExternalForm()
        );
    }

    /**
     * Handles any errors that occur during login
     */
    private void handleLoginError(@NotNull Exception e) {
        e.printStackTrace();
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
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/styles.css")).toExternalForm());
            
            stage.setTitle("Sign Up");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            mainController.showAlert("Failed to open Sign Up screen: " + e.getMessage());
        }
    }
}
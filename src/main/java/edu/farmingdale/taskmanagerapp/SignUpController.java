package edu.farmingdale.taskmanagerapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Controller for the sign-up screen.
 * Handles creating new accounts and navigating back to the login screen.
 */
public class SignUpController {

    @FXML

    private TextField usernameField, emailField, passwordField, securityAnswerField;
    @FXML
    private Button createButton, backButton;
    @FXML
    private ComboBox<String> securityQuestionBox;
    private DatabaseManager dm;

    @FXML
    private Hyperlink loginLink;


    private TaskManagerController mainController;

    /**
     * Initializes the controller and binds the login link behavior.
     */
    @FXML
    public void initialize() {
        if (securityQuestionBox != null) {
            securityQuestionBox.getItems().addAll(
                    "What is the name of your first pet?",
                    "What is the name of the street you grew up on?",
                    "What is the name of your childhood best friend?",
                    "What is your mother's maiden name?",
                    "What was your favorite book as a child?"
            );
        } else {
            System.err.println("Warning: securityQuestionBox was not properly initialized!");
        }

        if (loginLink != null) {
            loginLink.setOnAction(e -> {
                try {
                    if (mainController == null) {
                        System.err.println("Warning: mainController is not initialized!");
                        return;
                    }
                    goToLogin();
                } catch (Exception ex) {
                    if (mainController != null) {
                        mainController.showAlert("Error navigating to login: " + ex.getMessage());
                    } else {
                        System.err.println("Error navigating to login: " + ex.getMessage());
                    }
                }
            });
        }

    }

    /**
     * Injects the main controller for database access.
     * @param controller the TaskManagerController
     */
    public void setMainController (TaskManagerController controller){
        this.mainController = controller;
    }

    /**
     * Handles the account creation logic and validation.
     * @param actionEvent the button event
     */
    @FXML
    public void createNewAccount (ActionEvent actionEvent){
        try {

            String username = usernameField.getText();
            String email = emailField.getText();
            String password = passwordField.getText();
            String selectedQuestion = securityQuestionBox.getValue();
            String securityAnswer = securityAnswerField.getText();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || securityAnswer.isEmpty()) {
                mainController.showAlert("Please fill in all fields");
                return;
            }


            UserSession newUser = new UserSession(usernameField.getText(), emailField.getText(), passwordField.getText(), selectedQuestion, securityAnswer);
            UserSession existingUser = mainController.getDbManager().getAccount(newUser.getEmail());


            if (existingUser == null) {
                mainController.getDbManager().registerUser(newUser);
                mainController.showAlert("Account created successfully!");
                mainController.setCurrentUser(newUser);

                // Automatically navigate to main dashboard after signup
                FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/TaskManagerView.fxml"));
                Parent mainRoot = mainLoader.load();

                TaskManagerController controller = mainLoader.getController();
                controller.setDatabaseManager(mainController.getDbManager());
                controller.setCurrentUser(newUser);

                Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
                Scene scene = new Scene(mainRoot, stage.getScene().getWidth(), stage.getScene().getHeight());
                scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/styles.css")).toExternalForm());
                scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/ChatBox.css")).toExternalForm());

                stage.setTitle("Task Manager");
                stage.setScene(scene);
            } else {
                mainController.showAlert("Email is already registered. Please use a different email.");
            }
        } catch (Exception e) {
            mainController.showAlert("Error creating account: " + e.getMessage());
        }
    }

    /**
     * Navigates back to the login screen.
     */
    @FXML
    public void handleBack () {
        goToLogin();
    }

    /**
     * Helper method to load the login screen into the current stage.
     */
    private void goToLogin () {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/LoginView.fxml"));
            Parent root = loader.load();

            LoginController loginController = loader.getController();
            loginController.setMainController(mainController);

            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/styles.css")).toExternalForm());

            stage.setScene(scene);
        } catch (IOException e) {
            mainController.showAlert("Failed to open Login screen: " + e.getMessage());
        }
    }
}
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
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the sign-up screen.
 * Handles creating new accounts and navigating back to the login screen.
 */
public class SignUpController {
    private static final Logger LOGGER = Logger.getLogger(SignUpController.class.getName());

    @FXML
    private TextField usernameField, emailField, securityAnswerField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button createButton, backButton;
    @FXML
    private ComboBox<String> securityQuestionBox;
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
            securityQuestionBox.setVisibleRowCount(5);
            securityQuestionBox.setCellFactory(listView -> createSecurityQuestionCell());
            securityQuestionBox.setButtonCell(createSecurityQuestionCell());
        } else {
            LOGGER.warning("Security question combo box was not initialized.");
        }

        if (loginLink != null) {
            loginLink.setOnAction(e -> {
                try {
                    if (mainController == null) {
                        LOGGER.warning("Main controller is not initialized.");
                        return;
                    }
                    goToLogin();
                } catch (Exception ex) {
                    if (mainController != null) {
                        mainController.showAlert("Error navigating to login: " + ex.getMessage());
                    } else {
                        LOGGER.log(Level.WARNING, "Error navigating to login.", ex);
                    }
                }
            });
        }
    }

    private ListCell<String> createSecurityQuestionCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setTooltip(empty || item == null ? null : new Tooltip(item));
            }
        };
    }

    /**
     * Injects the main controller for database access.
     * @param controller the TaskManagerController
     */
    public void setMainController(TaskManagerController controller) {
        this.mainController = controller;
    }

    /**
     * Handles the account creation logic and validation.
     * @param actionEvent the button event
     */
    @FXML
    public void createNewAccount(ActionEvent actionEvent) {
        try {
            String username = usernameField.getText().trim();
            String email = emailField.getText().trim();
            String password = passwordField.getText();
            String selectedQuestion = securityQuestionBox.getValue();
            String securityAnswer = securityAnswerField.getText().trim();

            String validationError = validateAccountFields(username, email, password, selectedQuestion, securityAnswer);
            if (validationError != null) {
                mainController.showAlert(validationError);
                return;
            }

            UserSession newUser = new UserSession(username, email, password, selectedQuestion, securityAnswer);
            if (!mainController.isDatabaseAvailable()) {
                newUser.setUserID(Math.abs(email.toLowerCase().hashCode()));
                mainController.showAlert("Account created in offline mode. Tasks will be saved locally.");
                mainController.setCurrentUser(newUser);
                openMainDashboard(actionEvent, newUser);
                return;
            }

            UserSession existingEmail = mainController.getDbManager().getAccountByEmail(email);
            UserSession existingUsername = mainController.getDbManager().getAccount(username);


            if (existingEmail == null && existingUsername == null) {
                mainController.getDbManager().registerUser(newUser);
                mainController.showAlert("Account created successfully!");
                mainController.setCurrentUser(newUser);
                openMainDashboard(actionEvent, newUser);
            } else {
                mainController.showAlert("Username or email is already registered. Please use different account details.");
            }
        } catch (Exception e) {
            mainController.showAlert("Error creating account: " + e.getMessage());
        }
    }

    private String validateAccountFields(String username, String email, String password,
                                         String selectedQuestion, String securityAnswer) {
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()
                || selectedQuestion == null || securityAnswer.isEmpty()) {
            return "Please fill in all fields.";
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            return "Please enter a valid email address.";
        }
        if (securityAnswer.length() < 2) {
            return "Security answer must be at least 2 characters long.";
        }
        return PasswordUtil.validatePassword(password);
    }

    private void openMainDashboard(ActionEvent actionEvent, UserSession user) throws IOException {
        FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/TaskManagerView.fxml"));
        Parent mainRoot = mainLoader.load();

        TaskManagerController controller = mainLoader.getController();
        controller.setDatabaseManager(mainController.getDbManager());
        controller.setCurrentUser(user);

        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene scene = new Scene(mainRoot, stage.getScene().getWidth(), stage.getScene().getHeight());
        ThemeManager.bindToSystemTheme(scene);

        stage.setTitle("Task Manager");
        stage.setScene(scene);
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
            ThemeManager.bindToSystemTheme(scene);

            stage.setScene(scene);
        } catch (IOException e) {
            mainController.showAlert("Failed to open Login screen: " + e.getMessage());
        }
    }
}

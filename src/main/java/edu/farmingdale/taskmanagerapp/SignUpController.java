package edu.farmingdale.taskmanagerapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for the sign-up screen.
 */
public class SignUpController {
    @FXML
    private TextField usernameField, emailField, passwordField;
    @FXML
    private Button createButton, backButton;
    
    private TaskManagerController mainController;

    /**
     * Initializes the controller after the FXML file is loaded.
     */
    public void initialize() {}

    /**
     * @param controller The main controller
     */
    public void setMainController(TaskManagerController controller) {
        this.mainController = controller;
    }

    /**
     * Handles the create account button click.
     * @param actionEvent The event triggered by the button click
     */
    @FXML
    public void createNewAccount(ActionEvent actionEvent) {
        try {
        String username = usernameField.getText();
            String email = emailField.getText();
        String password = passwordField.getText();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                mainController.showAlert("Please fill in all fields");
                return;
            }

            UserSession newUser = new UserSession(username, email, password);
            UserSession existingUser = mainController.getDbManager().getAccount(newUser.getEmail());

            if (existingUser == null) {
                mainController.getDbManager().registerUser(newUser);
                mainController.showAlert("Account created successfully!");
                mainController.setCurrentUser(newUser);
                Stage window = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
                window.close();
            } else {
                mainController.showAlert("Email is already registered. Please use a different email.");
            }
        } catch (Exception e) {
            mainController.showAlert("Error creating account: " + e.getMessage());
        }
    }

        /**
         * Handles the back button click.
         */
        @FXML
        public void handleBack() {
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }

    /**
     * @return The get creation button
     */
    public Button getCreateButton() {
        return createButton;
    }

    /**
     * @param createButton The set creation button
     */
    public void setCreateButton(Button createButton) {
        this.createButton = createButton;
    }
}


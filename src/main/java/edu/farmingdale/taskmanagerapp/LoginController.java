package edu.farmingdale.taskmanagerapp;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

/**
 * Controller for the login screen.
 */
public class LoginController {

    @FXML
    private TextField emailTextField, passwordTextField;
    @FXML
    private Button loginButton, backButton;

    private TaskManagerController mainController;

    /**
     * Initializes the controller after the FXML file is loaded.
     */
    @FXML
    public void initialize() {}

    /**
     * @param controller The main controller
     */
    public void setMainController(TaskManagerController controller) {
        this.mainController = controller;
    }

    /**
     * Handles the login button click.
     * @param actionEvent The event triggered by the button click
     */
    @FXML
    public void login(ActionEvent actionEvent) {
        try {
            String email = emailTextField.getText();
            String password = passwordTextField.getText();

            UserSession user = new UserSession("", email, password, "", "");
            UserSession authenticatedUser = mainController.getDbManager().getAccount(user.getEmail());
            
            if (authenticatedUser == null || authenticatedUser.getUserName().isEmpty() || 
                !authenticatedUser.getPassword().equals(password)) {
                mainController.showAlert("Login failed, please enter a valid email/password");
            } else {
                mainController.setCurrentUser(authenticatedUser);
                Stage window = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
                window.close();
            }
        } catch (Exception e) {
            mainController.showAlert("Error during login: " + e.getMessage());
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
}
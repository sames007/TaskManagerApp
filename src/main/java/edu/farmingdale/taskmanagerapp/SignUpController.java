package edu.farmingdale.taskmanagerapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SignUpController {
    @FXML
    private TextField usernameField, emailField, passwordField;
    @FXML
    private Button createButton, backButton;
    
    private TaskManagerController mainController;

    public void initialize() {
    }

    public void setMainController(TaskManagerController controller) {
        this.mainController = controller;
    }

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

    @FXML
    public void handleBack() {
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }
}


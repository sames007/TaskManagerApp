package edu.farmingdale.taskmanagerapp;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.IOException;

public class LoginController {
    @FXML
    private TextField emailTextField, passwordTextField;
    @FXML
    private Button loginButton, backButton;


    @FXML
    public void initialize() {

    }

    @FXML
    public void login(ActionEvent actionEvent) {
        try {
            DatabaseManager dm = new DatabaseManager();
            String email = emailTextField.getText();
            String password = passwordTextField.getText();


            UserSession user = new UserSession("", email, password);
            UserSession s = dm.getAccount(user.getEmail());
            if (s.getUserName().isEmpty() || !s.getPassword().equals(password)) {
                System.out.println("Login failed, please enter a valid email/password");
            } else {
                // Load the main FXML layout
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/TaskManagerView.fxml"));

                // Create a DatabaseManager instance
                DatabaseManager dbManager = new DatabaseManager();

                // Load the FXML file
                Parent root = loader.load();

                // Get the TaskManagerController instance and inject the DatabaseManager
                TaskManagerController taskManagerController = loader.getController();
                taskManagerController.setDatabaseManager(dbManager);

                // Create a new scene with specified width and height
                Scene scene = new Scene(root, 800, 600);

                // Apply external CSS files for styling the UI and chat window
                scene.getStylesheets().add(getClass().getResource("/edu/farmingdale/taskmanagerapp/styles.css").toExternalForm());
                scene.getStylesheets().add(getClass().getResource("/edu/farmingdale/taskmanagerapp/ChatBox.css").toExternalForm());

                // Get the current stage (login window) and close it
                Stage window = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
                window.close();  // Close the login window
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
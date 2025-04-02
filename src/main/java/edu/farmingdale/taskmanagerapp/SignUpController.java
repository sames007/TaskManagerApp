package edu.farmingdale.taskmanagerapp;

import com.mysql.cj.conf.BooleanProperty;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignUpController {
    @FXML
    private TextField usernameField, emailField, passwordField;
    @FXML
    private Button createButton;
    
    private DatabaseManager dm;

    public void initialize() {
        dm = new DatabaseManager();
    }

    public void createNewAccount(ActionEvent actionEvent) {
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        
        UserSession s = new UserSession(username, email, password);

        try {
            UserSession existingUser = dm.getAccount(username);
            if (existingUser == null) {
                dm.registerUser(s);
                System.out.println("Account created!");
            } else {
                System.out.println("Username is taken! Please choose another");
            }
        } catch (Exception e) {
            System.out.println("Unable to create account");
            e.printStackTrace();
        }
    }
}


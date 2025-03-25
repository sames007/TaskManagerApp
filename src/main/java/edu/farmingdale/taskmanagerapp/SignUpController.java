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
    Button createButton;

    public void initialize() {

    }
    public void createNewAccount(ActionEvent actionEvent) {
        dm = new DatabaseManager();
        String priv = "NONE";
        String username = usernameField.getText();
        String password = passwordField.getText();
        UserSession s = new UserSession(usernameField.getText(), emailField.getText(), passwordField.getText(),priv);

        try{
            UserSession existingUser = dm.getAccount(s.getUserName());
            if(existingUser==null) {
                dm.registerUser(s);
                System.out.println("Account created!");
            }else{
                System.out.println("Username is taken! Please choose another");
            }
        } catch (Exception e) {
            System.out.println("Unable to create account");
            e.printStackTrace();
        }

    }
}


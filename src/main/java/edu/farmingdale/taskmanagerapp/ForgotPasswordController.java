package edu.farmingdale.taskmanagerapp;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;


public class ForgotPasswordController {
    @FXML
    private Text securityQuestionText, newPasswordText, confirmPasswordText;
    @FXML
    private TextField answerField, newPasswordField, confirmField, emailField;
    @FXML
    private Button backButton, confirmButton, verifyButton, retrieveQuestionButton;

    private DatabaseManager dbManager;

    @FXML
    public void initialize(){

        dbManager = new DatabaseManager();

        newPasswordField.setVisible(false);
        confirmField.setVisible(false);
        confirmButton.setVisible(false);
        confirmPasswordText.setVisible(false);
        newPasswordText.setVisible(false);

        ChangeListener<String> passwordMatchListener = (obs, oldVal, newVal)->{
            boolean match = newPasswordField.getText().equals(confirmField.getText());
            confirmButton.setDisable(!match);
        };

        newPasswordField.textProperty().addListener(passwordMatchListener);
        confirmField.textProperty().addListener(passwordMatchListener);
    }

    private void showAlert(String message){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Alert");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void retrieveQuestion(){
        String email = emailField.getText();
        if(email.isEmpty()){
            showAlert("Please enter your email");
            return;
        }

        UserSession user = dbManager.getAccountByEmail(email);

        if(user == null || user.getSecurityQuestion() == null || user.getSecurityQuestion().isEmpty()){
            showAlert("No account found with that email!");
            return;
        }
        securityQuestionText.setText(user.getSecurityQuestion());
    }

    @FXML
    public void verifyAnswer(){
        String email = emailField.getText();
        String answer = answerField.getText();

        if(email.isEmpty()||answer.isEmpty()){
            showAlert("Please enter both your email and security answer!");
            return;
        }

        UserSession user = dbManager.getAccountByEmail(email);

        if(user==null){
            showAlert("No account found with that email!");
            return;
        }

        String correctAnswer = user.getSecurityAnswer();

        if(correctAnswer==null||!correctAnswer.equalsIgnoreCase(answer)){
            showAlert("Incorrect answer to security question!");
            return;
        }

        newPasswordField.setVisible(true);
        confirmField.setVisible(true);
        confirmButton.setVisible(true);
        confirmPasswordText.setVisible(true);
        newPasswordText.setVisible(true);

    }

    @FXML
    public void confirmPassword(){
        String email = emailField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmField.getText();

        if(newPassword.isEmpty()||confirmPassword.isEmpty()){
            showAlert("Please fill in both new password fields!");
            return;
        }

        if(!newPassword.equals(confirmPassword)){
            showAlert("Passwords do not match!");
            return;
        }
        try {
            System.out.println("Updating password for email with new password: " + email + " " + newPassword);
            dbManager.updatePassword(email, newPassword);
            showAlert("Password successfully updated!");
            Stage window = (Stage) confirmButton.getScene().getWindow();
            window.close();
        }catch(RuntimeException e){
            showAlert(e.getMessage());
        }
    }


    @FXML
    public void handleBack() {
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }


}

package edu.farmingdale.taskmanagerapp;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;


public class ForgotPasswordController {
    @FXML
    private Text securityQuestionText, newPasswordText, confirmPasswordText;
    @FXML
    private TextField answerField, emailField;
    @FXML
    private PasswordField newPasswordField, confirmField;
    @FXML
    private Button backButton, confirmButton, verifyButton, retrieveQuestionButton;

    private DatabaseManager dbManager;

    /**
     * Initializes the controller.
     */
    @FXML
    public void initialize(){

        newPasswordField.setVisible(false);
        confirmField.setVisible(false);
        confirmButton.setVisible(false);
        confirmPasswordText.setVisible(false);
        newPasswordText.setVisible(false);

        ChangeListener<String> passwordMatchListener = (obs, oldVal, newVal)->{
            boolean match = !newPasswordField.getText().isEmpty()
                    && newPasswordField.getText().equals(confirmField.getText());
            confirmButton.setDisable(!match);
        };

        newPasswordField.textProperty().addListener(passwordMatchListener);
        confirmField.textProperty().addListener(passwordMatchListener);
    }

    public void setDatabaseManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Shows an alert with the provided message.
     * @param message the message to show
     */
    private void showAlert(String message){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Alert");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Handles the retrieve question button action.
     */
    @FXML
    public void retrieveQuestion(){
        DatabaseManager manager = getDatabaseManager();
        if (!manager.isAvailable()) {
            showAlert("Password recovery is unavailable in offline mode.");
            return;
        }
        String email = emailField.getText().trim();
        if(email.isEmpty()){
            showAlert("Please enter your email");
            return;
        }

        try {
            UserSession user = manager.getAccountByEmail(email);

            if(user == null || user.getSecurityQuestion() == null || user.getSecurityQuestion().isEmpty()){
                showAlert("No account found with that email!");
                return;
            }
            securityQuestionText.setText(user.getSecurityQuestion());
        } catch (RuntimeException e) {
            showAlert("Unable to retrieve security question: " + e.getMessage());
        }
    }

    /**
     * Handles the verify button action.
     */
    @FXML
    public void verifyAnswer(){
        DatabaseManager manager = getDatabaseManager();
        if (!manager.isAvailable()) {
            showAlert("Password recovery is unavailable in offline mode.");
            return;
        }
        String email = emailField.getText().trim();
        String answer = answerField.getText();

        if(email.isEmpty()||answer.isEmpty()){
            showAlert("Please enter both your email and security answer!");
            return;
        }

        UserSession user;
        try {
            user = manager.getAccountByEmail(email);
        } catch (RuntimeException e) {
            showAlert("Unable to verify answer: " + e.getMessage());
            return;
        }

        if(user==null){
            showAlert("No account found with that email!");
            return;
        }

        String expectedAnswer = user.getSecurityAnswer();
        String normalizedAnswer = PasswordUtil.normalizeSecurityAnswer(answer);

        boolean answerMatches = expectedAnswer != null
                && (PasswordUtil.verifyPassword(normalizedAnswer, expectedAnswer)
                || expectedAnswer.equalsIgnoreCase(answer.trim()));
        if(!answerMatches){
            showAlert("Incorrect answer to security question!");
            return;
        }

        newPasswordField.setVisible(true);
        confirmField.setVisible(true);
        confirmButton.setVisible(true);
        confirmPasswordText.setVisible(true);
        newPasswordText.setVisible(true);

    }

    /**
     * Handles the confirm button action.
     */
    @FXML
    public void confirmPassword(){
        DatabaseManager manager = getDatabaseManager();
        if (!manager.isAvailable()) {
            showAlert("Password recovery is unavailable in offline mode.");
            return;
        }
        String email = emailField.getText().trim();
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

        String validationError = PasswordUtil.validatePassword(newPassword);
        if (validationError != null) {
            showAlert(validationError);
            return;
        }

        try {
            manager.updatePassword(email, newPassword);
            showAlert("Password successfully updated!");
            Stage window = (Stage) confirmButton.getScene().getWindow();
            window.close();
        }catch(RuntimeException e){
            showAlert(e.getMessage());
        }
    }

    /**
     * Handles the back button action.
     */
    @FXML
    public void handleBack() {
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }

    private DatabaseManager getDatabaseManager() {
        if (dbManager == null) {
            dbManager = new DatabaseManager();
        }
        return dbManager;
    }
}

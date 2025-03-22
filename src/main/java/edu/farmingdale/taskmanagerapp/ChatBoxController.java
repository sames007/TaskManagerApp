package edu.farmingdale.taskmanagerapp;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ChatBoxController {
    // The text field where the user types their message
    @FXML
    private TextField inputField;
    // The button that sends the message
    @FXML
    private Button sendButton;
    // The area where chat messages (user and AI) are displayed
    @FXML
    private TextArea chatArea;

    // HTTP client used to make asynchronous calls to the AI service
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // This method is automatically called after the FXML is loaded
    @FXML
    public void initialize() {
        // Set up the send button to call sendMessage() when clicked
        sendButton.setOnAction(event -> sendMessage());

        // Also, send the message when the Enter key is pressed in the input field
        inputField.setOnKeyPressed((KeyEvent event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendMessage();
                event.consume(); // Prevents the Enter key from triggering other events
            }
        });
    }

    // This method handles sending the message to the AI service
    private void sendMessage() {
        // Get the text the user typed and remove any extra spaces
        String userInput = inputField.getText().trim();
        if (userInput.isEmpty()) {
            return; // Do nothing if the input is empty
        }
        // Show the user's message in the chat area
        chatArea.appendText("User: " + userInput + "\n");
        // Clear the input field for the next message
        inputField.clear();

        // Create the JSON payload for the AI request,
        // escaping any quotes in the user input to avoid errors
        String jsonPayload = "{\"contents\": [{\"parts\": [{\"text\": \""
                + userInput.replace("\"", "\\\"") + "\"}]}]}";

        // Get the API key from the configuration file
        String apiKey = AI_Helper.getAPIKey();
        if (apiKey == null || apiKey.isEmpty()) {
            chatArea.appendText("Error: API key not found in config.properties.\n");
            return; // Stop execution if the API key is missing
        }

        // Build the URL for the AI API, adding the API key as a query parameter
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

        // Build the HTTP POST request with the JSON payload and the proper header
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        // Send the HTTP request asynchronously
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)  // Get the response body
                .thenAccept(responseBody -> Platform.runLater(() -> {
                    // Parse the response to extract the AI's text answer
                    String aiResponse = parseResponse(responseBody);
                    // Format the response text so that newline escapes become real newlines
                    aiResponse = formatAIResponse(aiResponse);
                    // Display the AI's response in the chat area
                    chatArea.appendText("AI: " + aiResponse + "\n");
                }))
                .exceptionally(e -> {
                    // If there is an error during the HTTP call, display the error message
                    Platform.runLater(() -> chatArea.appendText("Error: " + e.getMessage() + "\n"));
                    return null;
                });
    }

    // Helper method to extract the "text" field from the AI response.
    // In production, consider using a proper JSON parser like Gson or Jackson.
    private String parseResponse(String responseBody) {
        int index = responseBody.indexOf("\"text\":");
        if (index != -1) {
            int start = responseBody.indexOf("\"", index + 7) + 1;
            int end = responseBody.indexOf("\"", start);
            if (start != -1 && end != -1) {
                return responseBody.substring(start, end);
            }
        }
        return "Could not parse response.";
    }

    // Helper method to convert escaped newline characters (e.g., "\n")
    // into actual newline characters for better readability.
    private String formatAIResponse(String response) {
        return response.replace("\\n", "\n");
    }
}

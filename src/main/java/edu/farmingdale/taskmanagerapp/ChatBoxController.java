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
        String userInput = inputField.getText().trim();
        if (userInput.isEmpty()) return;

        // Display user message
        chatArea.appendText("User: " + userInput + "\n");
        inputField.clear();

        // Escape quotes
        String safeInput = userInput.replace("\"", "\\\"");

        // Build JSON payload with system_instruction + user content
        String jsonPayload = "{"
                + "\"system_instruction\": {"
                + "    \"parts\": [{"
                + "        \"text\": \"You are a helpful project‐planning assistant. "
                + "When the user mentions a due date, generate task ideas, "
                + "assign priorities, and suggest a schedule.\""
                + "    }]"
                + "},"
                + "\"contents\": [{"
                + "    \"parts\": [{"
                + "        \"text\": \"" + safeInput + "\""
                + "    }]"
                + "}],"
                + "\"generationConfig\": {"
                + "    \"temperature\": 0.2,"       // more focused
                + "    \"maxOutputTokens\": 512"   // adjust as needed
                + "}"
                + "}";

        String apiKey = AI_Helper.getAPIKey();
        if (apiKey == null || apiKey.isEmpty()) {
            chatArea.appendText("Error: API key not found.\n");
            return;
        }

        String url = "https://generativelanguage.googleapis.com/"
                + "v1beta/models/gemini-2.0-flash:generateContent?key="
                + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(body -> Platform.runLater(() -> {
                    String aiResponse = parseResponse(body);
                    aiResponse = formatAIResponse(aiResponse);
                    chatArea.appendText("AI: " + aiResponse + "\n");
                }))
                .exceptionally(e -> {
                    Platform.runLater(() ->
                            chatArea.appendText("Error: " + e.getMessage() + "\n"));
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

package edu.farmingdale.taskmanagerapp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Controller for the AI chat window.
 */
public class ChatBoxController {
    private static final URI GEMINI_ENDPOINT = URI.create(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
    );
    private static final int MAX_PROMPT_LENGTH = 4_000;

    @FXML
    private TextField inputField;
    @FXML
    private Button sendButton;
    @FXML
    private TextArea chatArea;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Initializes chat actions after the FXML file is loaded.
     */
    @FXML
    public void initialize() {
        sendButton.setOnAction(event -> sendMessage());
        inputField.setOnKeyPressed((KeyEvent event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendMessage();
                event.consume();
            }
        });
    }

    /**
     * Sends the user's prompt to Gemini and appends the response to the chat area.
     */
    private void sendMessage() {
        String userInput = inputField.getText().trim();
        if (userInput.isEmpty()) {
            return;
        }
        if (userInput.length() > MAX_PROMPT_LENGTH) {
            chatArea.appendText("Error: Message is too long. Please keep it under "
                    + MAX_PROMPT_LENGTH + " characters.\n");
            return;
        }

        chatArea.appendText("User: " + userInput + "\n");
        inputField.clear();
        sendButton.setDisable(true);

        String apiKey = AI_Helper.getAPIKey();
        if (apiKey == null || apiKey.isEmpty()) {
            chatArea.appendText("Error: API key not found.\n");
            sendButton.setDisable(false);
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(GEMINI_ENDPOINT)
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestPayload(userInput)))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    String aiResponse = parseResponse(response.statusCode(), response.body());
                    chatArea.appendText("AI: " + formatAIResponse(aiResponse) + "\n");
                    sendButton.setDisable(false);
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        chatArea.appendText("Error: " + e.getMessage() + "\n");
                        sendButton.setDisable(false);
                    });
                    return null;
                });
    }

    /**
     * Builds the Gemini request body with Gson so user text is escaped safely.
     */
    @NotNull
    @Contract(pure = true)
    static String buildRequestPayload(@NotNull String userInput) {
        JsonObject root = new JsonObject();

        JsonObject systemInstruction = new JsonObject();
        JsonArray systemParts = new JsonArray();
        JsonObject systemText = new JsonObject();
        systemText.addProperty("text", "You are a helpful project-planning assistant. "
                + "When the user mentions a due date, generate task ideas, assign priorities, "
                + "and suggest a practical schedule.");
        systemParts.add(systemText);
        systemInstruction.add("parts", systemParts);
        root.add("system_instruction", systemInstruction);

        JsonObject userPart = new JsonObject();
        userPart.addProperty("text", userInput);
        JsonArray userParts = new JsonArray();
        userParts.add(userPart);
        JsonObject content = new JsonObject();
        content.add("parts", userParts);
        JsonArray contents = new JsonArray();
        contents.add(content);
        root.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.2);
        generationConfig.addProperty("maxOutputTokens", 512);
        root.add("generationConfig", generationConfig);

        return root.toString();
    }

    /**
     * Extracts generated text from Gemini's JSON response.
     */
    @NotNull
    static String parseResponse(int statusCode, @NotNull String responseBody) {
        if (statusCode < 200 || statusCode >= 300) {
            return parseErrorMessage(responseBody, "AI service returned status " + statusCode + ".");
        }

        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return parseErrorMessage(responseBody, "AI service returned no response.");
            }

            JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
            JsonArray parts = content == null ? null : content.getAsJsonArray("parts");
            if (parts == null || parts.isEmpty()) {
                return "AI service returned an empty response.";
            }

            JsonElement text = parts.get(0).getAsJsonObject().get("text");
            return text == null ? "AI service returned an empty response." : text.getAsString();
        } catch (RuntimeException e) {
            return "Could not parse AI response.";
        }
    }

    @NotNull
    @Contract(pure = true)
    static String formatAIResponse(@NotNull String response) {
        return response.replace("\\n", "\n");
    }

    private static String parseErrorMessage(String responseBody, String fallback) {
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonObject error = root.getAsJsonObject("error");
            if (error != null && error.has("message")) {
                return "AI service error: " + error.get("message").getAsString();
            }
        } catch (RuntimeException ignored) {
            // Keep a friendly fallback when the service returns non-JSON text.
        }
        return fallback;
    }
}

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
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller for the AI chat window.
 */
public class ChatBoxController {
    private static final String GEMINI_ENDPOINT_BASE =
            "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String DEFAULT_GEMINI_MODEL = "gemini-flash-latest";
    private static final Pattern MODEL_NAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Pattern RETRY_DELAY_PATTERN = Pattern.compile(
            "retry in\\s+([0-9.]+)s",
            Pattern.CASE_INSENSITIVE
    );
    private static final int MAX_PROMPT_LENGTH = 4_000;
    private static final String MISSING_API_KEY_MESSAGE = "Error: Gemini API key is not configured. "
            + "Set GEMINI_API_KEY, GOOGLE_API_KEY, or API_KEY outside source control.";
    private static final String INVALID_API_KEY_MESSAGE = "AI service error: The configured Gemini API key is invalid. "
            + "Update GEMINI_API_KEY, GOOGLE_API_KEY, or API_KEY with a valid Gemini key, then restart the app.";
    private static final String QUOTA_EXCEEDED_MESSAGE = "AI service error: Gemini quota is exhausted for this key "
            + "and model. Check your Google AI Studio quota/billing, wait for quota to reset, or set GEMINI_MODEL "
            + "to another model available to this API key.";

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
            chatArea.appendText(MISSING_API_KEY_MESSAGE + "\n");
            sendButton.setDisable(false);
            return;
        }

        URI endpoint;
        try {
            endpoint = buildGeminiEndpoint(resolveGeminiModel());
        } catch (IllegalArgumentException e) {
            chatArea.appendText("Error: " + e.getMessage() + "\n");
            sendButton.setDisable(false);
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(endpoint)
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

    static String resolveGeminiModel() {
        return AppConfig.get("GEMINI_MODEL").orElse(DEFAULT_GEMINI_MODEL);
    }

    static URI buildGeminiEndpoint(@NotNull String modelName) {
        String normalizedModelName = normalizeModelName(modelName);
        return URI.create(GEMINI_ENDPOINT_BASE + normalizedModelName + ":generateContent");
    }

    private static String normalizeModelName(String modelName) {
        String normalizedModelName = modelName.trim();
        if (normalizedModelName.startsWith("models/")) {
            normalizedModelName = normalizedModelName.substring("models/".length());
        }

        if (!MODEL_NAME_PATTERN.matcher(normalizedModelName).matches()) {
            throw new IllegalArgumentException(
                    "GEMINI_MODEL must contain only letters, numbers, dots, underscores, and hyphens."
            );
        }
        return normalizedModelName;
    }

    /**
     * Extracts generated text from Gemini's JSON response.
     */
    @NotNull
    static String parseResponse(int statusCode, @NotNull String responseBody) {
        if (statusCode < 200 || statusCode >= 300) {
            return parseErrorMessage(statusCode, responseBody, "AI service returned status " + statusCode + ".");
        }

        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return parseErrorMessage(statusCode, responseBody, "AI service returned no response.");
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

    private static String parseErrorMessage(int statusCode, String responseBody, String fallback) {
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonObject error = root.getAsJsonObject("error");
            if (error != null && error.has("message")) {
                String message = error.get("message").getAsString();
                if (isInvalidApiKeyError(statusCode, message, error)) {
                    return INVALID_API_KEY_MESSAGE;
                }
                if (isQuotaExceededError(statusCode, message, error)) {
                    return quotaExceededMessage(message, error);
                }
                return "AI service error: " + message;
            }
        } catch (RuntimeException ignored) {
            // Keep a friendly fallback when the service returns non-JSON text.
        }
        return fallback;
    }

    private static boolean isQuotaExceededError(int statusCode, String message, JsonObject error) {
        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        JsonElement status = error.get("status");
        return statusCode == 429
                || normalizedMessage.contains("quota exceeded")
                || (status != null && "RESOURCE_EXHAUSTED".equalsIgnoreCase(status.getAsString()));
    }

    private static String quotaExceededMessage(String message, JsonObject error) {
        StringBuilder response = new StringBuilder(QUOTA_EXCEEDED_MESSAGE);
        Optional<String> retryDelay = extractRetryDelay(error).or(() -> extractRetryDelay(message));
        retryDelay.ifPresent(delay -> response.append(" Google suggested retrying in ").append(delay).append("."));
        return response.toString();
    }

    private static Optional<String> extractRetryDelay(JsonObject error) {
        JsonArray details = error.getAsJsonArray("details");
        if (details == null) {
            return Optional.empty();
        }

        for (JsonElement detail : details) {
            if (!detail.isJsonObject()) {
                continue;
            }

            JsonObject detailObject = detail.getAsJsonObject();
            JsonElement retryDelay = detailObject.get("retryDelay");
            if (retryDelay != null) {
                return Optional.of(retryDelay.getAsString());
            }
        }
        return Optional.empty();
    }

    private static Optional<String> extractRetryDelay(String message) {
        Matcher matcher = RETRY_DELAY_PATTERN.matcher(message);
        if (matcher.find()) {
            return Optional.of(matcher.group(1) + "s");
        }
        return Optional.empty();
    }

    private static boolean isInvalidApiKeyError(int statusCode, String message, JsonObject error) {
        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        if (normalizedMessage.contains("api key not valid")
                || normalizedMessage.contains("api key invalid")
                || normalizedMessage.contains("apikey invalid")) {
            return true;
        }

        if (statusCode != 400 && statusCode != 401 && statusCode != 403) {
            return false;
        }

        JsonArray details = error.getAsJsonArray("details");
        if (details == null) {
            return false;
        }

        for (JsonElement detail : details) {
            if (detail.isJsonObject()) {
                JsonObject detailObject = detail.getAsJsonObject();
                JsonElement reason = detailObject.get("reason");
                if (reason != null && "API_KEY_INVALID".equalsIgnoreCase(reason.getAsString())) {
                    return true;
                }
            }
        }
        return false;
    }
}

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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
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
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b");
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "\\bat\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern HAVE_TASK_SUBJECT_PATTERN = Pattern.compile(
            "\\bi\\s+have\\s+(?:a|an|the)?\\s*(.+?)\\s+(?:tmr|tomorrow|today|tonight|on\\b|by\\b)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern FOR_TASK_SUBJECT_PATTERN = Pattern.compile(
            "\\b(?:for|about)\\s+(.+?)\\s+(?:tmr|tomorrow|today|tonight|on\\b|by\\b)",
            Pattern.CASE_INSENSITIVE
    );
    private static final int MAX_PROMPT_LENGTH = 4_000;
    private static final int MAX_TASK_DESCRIPTION_LENGTH = 200;
    private static final LocalTime DEFAULT_AI_DUE_TIME = LocalTime.of(9, 0);
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

    private String userDisplayName = "You";
    private TaskCreationHandler taskCreationHandler;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    interface TaskCreationHandler {
        boolean createTask(Task task);
    }

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

    void configure(UserSession currentUser, TaskCreationHandler taskCreationHandler) {
        this.userDisplayName = displayNameFor(currentUser);
        this.taskCreationHandler = taskCreationHandler;
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

        chatArea.appendText(userDisplayName + ": " + userInput + "\n");
        inputField.clear();
        sendButton.setDisable(true);

        String apiKey = AI_Helper.getAPIKey();
        if (apiKey == null || apiKey.isEmpty()) {
            Optional<TaskDraft> localDraft = inferLocalTaskDraft(userInput);
            if (localDraft.isPresent()) {
                chatArea.appendText("AI: Gemini is not configured, so I created the task locally from your message.\n");
                createTasksFromDrafts(List.of(localDraft.get()));
            } else {
                chatArea.appendText(MISSING_API_KEY_MESSAGE + "\n");
            }
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
                    AiResponse aiResponse = parseAiResponse(response.statusCode(), response.body());
                    AiResponse finalResponse = withLocalTaskFallback(userInput, aiResponse);
                    chatArea.appendText("AI: " + formatAIResponse(finalResponse.reply()) + "\n");
                    createTasksFromDrafts(finalResponse.taskDrafts());
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

    private static AiResponse withLocalTaskFallback(String userInput, AiResponse aiResponse) {
        if (!aiResponse.taskDrafts().isEmpty()) {
            return aiResponse;
        }

        Optional<TaskDraft> localDraft = inferLocalTaskDraft(userInput);
        if (localDraft.isEmpty()) {
            return aiResponse;
        }

        String reply = aiResponse.reply().startsWith("AI service error:")
                ? "Gemini is temporarily unavailable, so I created the task locally from your message."
                : "I created a task locally from your message.";
        return new AiResponse(reply, List.of(localDraft.get()));
    }

    private void createTasksFromDrafts(List<TaskDraft> taskDrafts) {
        if (taskDrafts.isEmpty()) {
            return;
        }

        if (taskCreationHandler == null) {
            chatArea.appendText("AI: I drafted a task, but this chat is not connected to the task list.\n");
            return;
        }

        for (TaskDraft draft : taskDrafts) {
            try {
                Task task = draft.toTask();
                if (taskCreationHandler.createTask(task)) {
                    chatArea.appendText("Task created: " + task.getDescription()
                            + " (due " + task.getDueDate() + " at " + task.getDueTime() + ")\n");
                }
            } catch (IllegalArgumentException e) {
                chatArea.appendText("AI: I need a little more information before creating that task. "
                        + e.getMessage() + "\n");
            }
        }
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
        systemText.addProperty("text", "You are a helpful project-planning assistant inside a task manager app. "
                + "Today's date is " + LocalDate.now() + ". "
                + "Only include tasks when the user clearly asks to create, add, make, or schedule a task. "
                + "If a task request is missing a due date, ask for it in reply and return an empty tasks array. "
                + "Use Medium priority and Other category when the user does not specify them.");
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
        generationConfig.addProperty("maxOutputTokens", 768);
        generationConfig.add("responseFormat", buildResponseFormatSchema());
        root.add("generationConfig", generationConfig);

        return root.toString();
    }

    private static JsonObject buildResponseFormatSchema() {
        JsonObject textFormat = new JsonObject();
        textFormat.addProperty("mimeType", "APPLICATION_JSON");
        textFormat.add("schema", buildAiResponseSchema());

        JsonObject responseFormat = new JsonObject();
        responseFormat.add("text", textFormat);
        return responseFormat;
    }

    private static JsonObject buildAiResponseSchema() {
        JsonObject root = schemaObject();

        JsonObject properties = new JsonObject();
        properties.add("reply", schemaString("Short message shown to the user."));

        JsonObject tasks = schemaArray("Tasks to create in the app. Return an empty array when no task should be created.");
        tasks.add("items", buildTaskDraftSchema());
        properties.add("tasks", tasks);

        root.add("properties", properties);
        root.add("required", stringArray("reply", "tasks"));
        root.add("propertyOrdering", stringArray("reply", "tasks"));
        root.addProperty("additionalProperties", false);
        return root;
    }

    private static JsonObject buildTaskDraftSchema() {
        JsonObject task = schemaObject();

        JsonObject properties = new JsonObject();
        properties.add("description", schemaString("Clear task title, 200 characters or fewer."));

        JsonObject dueDate = schemaString("Due date in YYYY-MM-DD format.");
        dueDate.addProperty("format", "date");
        properties.add("dueDate", dueDate);

        JsonObject dueTime = schemaString("Due time in 24-hour HH:mm format.");
        dueTime.addProperty("format", "time");
        properties.add("dueTime", dueTime);

        JsonObject priority = schemaString("Task priority.");
        priority.add("enum", stringArray("Extreme", "High", "Medium", "Low"));
        properties.add("priority", priority);

        JsonObject category = schemaString("Task category.");
        category.add("enum", stringArray("School", "Work", "Personal", "Family", "Other"));
        properties.add("category", category);

        JsonObject reminder = new JsonObject();
        reminder.add("type", stringArray("string", "null"));
        reminder.addProperty("format", "date");
        reminder.addProperty("description", "Optional reminder date in YYYY-MM-DD format, or null.");
        properties.add("reminder", reminder);

        task.add("properties", properties);
        task.add("required", stringArray("description", "dueDate", "dueTime", "priority", "category", "reminder"));
        task.add("propertyOrdering", stringArray("description", "dueDate", "dueTime", "priority", "category", "reminder"));
        task.addProperty("additionalProperties", false);
        return task;
    }

    private static JsonObject schemaObject() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        return schema;
    }

    private static JsonObject schemaString(String description) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "string");
        schema.addProperty("description", description);
        return schema;
    }

    private static JsonObject schemaArray(String description) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "array");
        schema.addProperty("description", description);
        return schema;
    }

    private static JsonArray stringArray(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
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
        return parseAiResponse(statusCode, responseBody).reply();
    }

    @NotNull
    static AiResponse parseAiResponse(int statusCode, @NotNull String responseBody) {
        if (statusCode < 200 || statusCode >= 300) {
            return new AiResponse(
                    parseErrorMessage(statusCode, responseBody, "AI service returned status " + statusCode + "."),
                    List.of()
            );
        }

        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return new AiResponse(
                        parseErrorMessage(statusCode, responseBody, "AI service returned no response."),
                        List.of()
                );
            }

            JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
            JsonArray parts = content == null ? null : content.getAsJsonArray("parts");
            if (parts == null || parts.isEmpty()) {
                return new AiResponse("AI service returned an empty response.", List.of());
            }

            String text = combineTextParts(parts);
            return text == null
                    ? new AiResponse("AI service returned an empty response.", List.of())
                    : parseCandidateText(text);
        } catch (RuntimeException e) {
            return new AiResponse("Could not parse AI response.", List.of());
        }
    }

    private static String combineTextParts(JsonArray parts) {
        StringBuilder combinedText = new StringBuilder();
        for (JsonElement part : parts) {
            if (!part.isJsonObject()) {
                continue;
            }

            JsonElement text = part.getAsJsonObject().get("text");
            if (text != null && !text.isJsonNull()) {
                combinedText.append(text.getAsString());
            }
        }

        String output = combinedText.toString().trim();
        return output.isEmpty() ? null : output;
    }

    static AiResponse parseCandidateText(@NotNull String candidateText) {
        Optional<JsonObject> structuredResponse = parseJsonObject(candidateText);
        if (structuredResponse.isEmpty()) {
            return new AiResponse(candidateText, List.of());
        }

        JsonObject root = structuredResponse.get();
        String reply = stringValue(root.get("reply")).orElse(candidateText);
        return new AiResponse(reply, parseTaskDrafts(root.get("tasks")));
    }

    @NotNull
    @Contract(pure = true)
    static String formatAIResponse(@NotNull String response) {
        return response.replace("\\n", "\n");
    }

    static String displayNameFor(UserSession currentUser) {
        if (currentUser == null) {
            return "You";
        }

        String username = cleanString(currentUser.getUserName());
        if (username != null) {
            return username;
        }

        String email = cleanString(currentUser.getEmail());
        if (email != null) {
            int atIndex = email.indexOf('@');
            return atIndex > 0 ? email.substring(0, atIndex) : email;
        }

        return "You";
    }

    static Optional<TaskDraft> inferLocalTaskDraft(@NotNull String userInput) {
        String normalizedInput = userInput.trim();
        String lowerInput = normalizedInput.toLowerCase(Locale.ROOT);
        if (!looksLikeTaskCreationRequest(lowerInput)) {
            return Optional.empty();
        }

        LocalDate dueDate = inferDueDate(lowerInput);
        if (dueDate == null) {
            return Optional.empty();
        }

        String description = inferTaskDescription(normalizedInput);
        LocalTime dueTime = inferDueTime(normalizedInput).orElse(DEFAULT_AI_DUE_TIME);
        String category = inferCategory(lowerInput);
        LocalDate reminder = dueDate.isAfter(LocalDate.now()) ? LocalDate.now() : null;
        return Optional.of(new TaskDraft(description, dueDate, dueTime, "Medium", category, reminder));
    }

    private static boolean looksLikeTaskCreationRequest(String lowerInput) {
        return lowerInput.contains("make a task")
                || lowerInput.contains("create a task")
                || lowerInput.contains("add a task")
                || lowerInput.contains("make task")
                || lowerInput.contains("create task")
                || lowerInput.contains("add task")
                || lowerInput.contains("schedule")
                || lowerInput.contains("remind me");
    }

    private static LocalDate inferDueDate(String lowerInput) {
        Matcher dateMatcher = ISO_DATE_PATTERN.matcher(lowerInput);
        if (dateMatcher.find()) {
            try {
                return LocalDate.parse(dateMatcher.group());
            } catch (RuntimeException ignored) {
                return null;
            }
        }

        if (lowerInput.contains("tmr") || lowerInput.contains("tomorrow")) {
            return LocalDate.now().plusDays(1);
        }
        if (lowerInput.contains("today") || lowerInput.contains("tonight")) {
            return LocalDate.now();
        }
        if (lowerInput.contains("next week")) {
            return LocalDate.now().plusWeeks(1);
        }
        return null;
    }

    private static Optional<LocalTime> inferDueTime(String userInput) {
        Matcher matcher = TIME_PATTERN.matcher(userInput);
        if (!matcher.find()) {
            return Optional.empty();
        }

        int hour = Integer.parseInt(matcher.group(1));
        int minute = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
        String amPm = matcher.group(3);

        if (minute < 0 || minute > 59 || hour < 1 || hour > 23) {
            return Optional.empty();
        }
        if (amPm != null) {
            if (hour > 12) {
                return Optional.empty();
            }
            if ("pm".equalsIgnoreCase(amPm) && hour < 12) {
                hour += 12;
            } else if ("am".equalsIgnoreCase(amPm) && hour == 12) {
                hour = 0;
            }
        }

        try {
            return Optional.of(LocalTime.of(hour, minute));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static String inferTaskDescription(String userInput) {
        String subject = extractSubject(userInput).orElse(null);
        if (subject == null) {
            String lowerInput = userInput.toLowerCase(Locale.ROOT);
            if (lowerInput.contains("test") || lowerInput.contains("exam") || lowerInput.contains("quiz")) {
                subject = "test";
            } else if (lowerInput.contains("homework")) {
                subject = "homework";
            } else if (lowerInput.contains("project")) {
                subject = "project";
            } else if (lowerInput.contains("meeting")) {
                subject = "meeting";
            } else {
                subject = "task";
            }
        }

        String cleanedSubject = subject.replaceAll("\\b(can you|please|make|create|add|task|for it)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleanedSubject.isBlank()) {
            cleanedSubject = "task";
        }

        String lowerSubject = cleanedSubject.toLowerCase(Locale.ROOT);
        if (lowerSubject.contains("test") || lowerSubject.contains("exam") || lowerSubject.contains("quiz")) {
            return "Study for " + cleanedSubject;
        }
        if (lowerSubject.contains("homework")) {
            return "Complete " + cleanedSubject;
        }
        if (lowerSubject.contains("meeting")) {
            return "Prepare for " + cleanedSubject;
        }
        return capitalize(cleanedSubject);
    }

    private static Optional<String> extractSubject(String userInput) {
        Matcher haveMatcher = HAVE_TASK_SUBJECT_PATTERN.matcher(userInput);
        if (haveMatcher.find()) {
            return Optional.ofNullable(cleanString(haveMatcher.group(1)));
        }

        Matcher forMatcher = FOR_TASK_SUBJECT_PATTERN.matcher(userInput);
        if (forMatcher.find()) {
            return Optional.ofNullable(cleanString(forMatcher.group(1)));
        }
        return Optional.empty();
    }

    private static String inferCategory(String lowerInput) {
        if (lowerInput.contains("test")
                || lowerInput.contains("exam")
                || lowerInput.contains("quiz")
                || lowerInput.contains("homework")
                || lowerInput.contains("school")
                || lowerInput.contains("class")) {
            return "School";
        }
        if (lowerInput.contains("work") || lowerInput.contains("meeting") || lowerInput.contains("client")) {
            return "Work";
        }
        if (lowerInput.contains("family")) {
            return "Family";
        }
        return "Other";
    }

    private static String capitalize(String value) {
        if (value.isBlank()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private static Optional<JsonObject> parseJsonObject(String candidateText) {
        List<String> candidates = List.of(candidateText.trim(), stripJsonFence(candidateText.trim()));
        for (String candidate : candidates) {
            if (candidate.isBlank()) {
                continue;
            }

            try {
                JsonElement parsed = JsonParser.parseString(candidate);
                if (parsed.isJsonObject()) {
                    return Optional.of(parsed.getAsJsonObject());
                }
            } catch (RuntimeException ignored) {
                // The model can still return regular text if JSON mode is unavailable.
            }
        }
        return Optional.empty();
    }

    private static String stripJsonFence(String text) {
        if (!text.startsWith("```")) {
            return text;
        }

        int firstLineBreak = text.indexOf('\n');
        int lastFence = text.lastIndexOf("```");
        if (firstLineBreak < 0 || lastFence <= firstLineBreak) {
            return text;
        }

        return text.substring(firstLineBreak + 1, lastFence).trim();
    }

    private static List<TaskDraft> parseTaskDrafts(JsonElement taskElement) {
        if (taskElement == null || !taskElement.isJsonArray()) {
            return List.of();
        }

        List<TaskDraft> drafts = new ArrayList<>();
        JsonArray taskArray = taskElement.getAsJsonArray();
        for (JsonElement element : taskArray) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject taskObject = element.getAsJsonObject();
            drafts.add(new TaskDraft(
                    cleanString(taskObject.get("description")),
                    parseDate(taskObject.get("dueDate")),
                    parseTime(taskObject.get("dueTime")),
                    normalizePriority(cleanString(taskObject.get("priority"))),
                    normalizeCategory(cleanString(taskObject.get("category"))),
                    parseDate(taskObject.get("reminder"))
            ));
        }
        return drafts;
    }

    private static String cleanString(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return cleanString(element.getAsString());
    }

    private static String cleanString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Optional<String> stringValue(JsonElement element) {
        return Optional.ofNullable(cleanString(element));
    }

    private static LocalDate parseDate(JsonElement element) {
        String value = cleanString(element);
        if (value == null || "null".equalsIgnoreCase(value)) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static LocalTime parseTime(JsonElement element) {
        String value = cleanString(element);
        if (value == null || "null".equalsIgnoreCase(value)) {
            return null;
        }

        try {
            return LocalTime.parse(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String normalizePriority(String priority) {
        return switch (priority == null ? "" : priority.toLowerCase(Locale.ROOT)) {
            case "extreme" -> "Extreme";
            case "high" -> "High";
            case "low" -> "Low";
            default -> "Medium";
        };
    }

    private static String normalizeCategory(String category) {
        return switch (category == null ? "" : category.toLowerCase(Locale.ROOT)) {
            case "school" -> "School";
            case "work" -> "Work";
            case "personal" -> "Personal";
            case "family" -> "Family";
            default -> "Other";
        };
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

    record AiResponse(String reply, List<TaskDraft> taskDrafts) {
    }

    record TaskDraft(
            String description,
            LocalDate dueDate,
            LocalTime dueTime,
            String priority,
            String category,
            LocalDate reminder
    ) {
        Task toTask() {
            if (description == null) {
                throw new IllegalArgumentException("Please include a task description.");
            }
            if (description.length() > MAX_TASK_DESCRIPTION_LENGTH) {
                throw new IllegalArgumentException("Please keep the task description under "
                        + MAX_TASK_DESCRIPTION_LENGTH + " characters.");
            }
            if (dueDate == null) {
                throw new IllegalArgumentException("Please include a due date.");
            }
            if (dueDate.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("The due date cannot be in the past.");
            }

            Task task = new Task(description, dueDate, dueTime == null ? DEFAULT_AI_DUE_TIME : dueTime, priority);
            task.setCategory(category);
            task.setStatus("In Progress");
            task.setReminder(reminder != null && !reminder.isAfter(dueDate) ? reminder : null);
            return task;
        }
    }
}

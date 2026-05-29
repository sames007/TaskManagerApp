package edu.farmingdale.taskmanagerapp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChatBoxControllerTest {

    @Test
    void buildRequestPayloadEscapesUserTextSafely() {
        String userText = "Plan \"math\" homework\nfor Friday";

        JsonObject payload = JsonParser.parseString(ChatBoxController.buildRequestPayload(userText)).getAsJsonObject();
        String parsedText = payload.getAsJsonArray("contents")
                .get(0)
                .getAsJsonObject()
                .getAsJsonArray("parts")
                .get(0)
                .getAsJsonObject()
                .get("text")
                .getAsString();

        assertEquals(userText, parsedText);
        assertEquals(
                "APPLICATION_JSON",
                payload.getAsJsonObject("generationConfig")
                        .getAsJsonObject("responseFormat")
                        .getAsJsonObject("text")
                        .get("mimeType")
                        .getAsString()
        );
        assertTrue(
                payload.getAsJsonObject("generationConfig")
                        .getAsJsonObject("responseFormat")
                        .getAsJsonObject("text")
                        .getAsJsonObject("schema")
                        .getAsJsonArray("required")
                        .contains(JsonParser.parseString("\"tasks\""))
        );
    }

    @Test
    void buildRequestPayloadIncludesRecentHistoryWhenProvided() {
        JsonObject payload = JsonParser.parseString(
                ChatBoxController.buildRequestPayload("What should I do next?", "zero: I have a test tomorrow")
        ).getAsJsonObject();
        String parsedText = payload.getAsJsonArray("contents")
                .get(0)
                .getAsJsonObject()
                .getAsJsonArray("parts")
                .get(0)
                .getAsJsonObject()
                .get("text")
                .getAsString();

        assertTrue(parsedText.contains("Recent saved conversation history:"));
        assertTrue(parsedText.contains("zero: I have a test tomorrow"));
        assertTrue(parsedText.contains("Current user message:"));
        assertTrue(parsedText.endsWith("What should I do next?"));
    }

    @Test
    void parseResponseExtractsCandidateText() {
        String body = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "text": "Create a study checklist." }
                        ]
                      }
                    }
                  ]
                }
                """;

        assertEquals("Create a study checklist.", ChatBoxController.parseResponse(200, body));
    }

    @Test
    void parseResponseCombinesMultipleTextParts() {
        String dueDate = LocalDate.now().plusDays(1).toString();
        String body = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "text": "{\\"reply\\":\\"Created it.\\",\\"tasks\\":[" },
                          { "text": "{\\"description\\":\\"Study for test\\",\\"dueDate\\":\\"%s\\",\\"dueTime\\":\\"09:00\\",\\"priority\\":\\"Medium\\",\\"category\\":\\"School\\",\\"reminder\\":null}]}" }
                        ]
                      }
                    }
                  ]
                }
                """.formatted(dueDate);

        ChatBoxController.AiResponse response = ChatBoxController.parseAiResponse(200, body);

        assertEquals("Created it.", response.reply());
        assertEquals("Study for test", response.taskDrafts().get(0).description());
    }

    @Test
    void buildGeminiEndpointUsesConfiguredModelName() {
        assertEquals(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent",
                ChatBoxController.buildGeminiEndpoint("models/gemini-flash-latest").toString()
        );
    }

    @Test
    void buildGeminiEndpointRejectsUnsafeModelName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ChatBoxController.buildGeminiEndpoint("../gemini-flash-latest")
        );
    }

    @Test
    void displayNameUsesCurrentUsername() {
        UserSession user = new UserSession("saim", "saim@example.com", "secret");

        assertEquals("saim", ChatBoxController.displayNameFor(user));
    }

    @Test
    void parseCandidateTextExtractsTaskDraft() {
        String dueDate = LocalDate.now().plusDays(1).toString();
        String candidateText = """
                {
                  "reply": "I created the task for you.",
                  "tasks": [
                    {
                      "description": "Finish math homework",
                      "dueDate": "%s",
                      "dueTime": "18:30",
                      "priority": "High",
                      "category": "School",
                      "reminder": "%s"
                    }
                  ]
                }
                """.formatted(dueDate, LocalDate.now());

        ChatBoxController.AiResponse response = ChatBoxController.parseCandidateText(candidateText);
        Task task = response.taskDrafts().get(0).toTask();

        assertEquals("I created the task for you.", response.reply());
        assertEquals("Finish math homework", task.getDescription());
        assertEquals(LocalDate.parse(dueDate), task.getDueDate());
        assertEquals(LocalTime.of(18, 30), task.getDueTime());
        assertEquals("High", task.getPriority());
        assertEquals("School", task.getCategory());
        assertEquals(LocalDate.now(), task.getReminder());
    }

    @Test
    void inferLocalTaskDraftCreatesSimpleTestTask() {
        ChatBoxController.TaskDraft draft = ChatBoxController
                .inferLocalTaskDraft("Hi, I have a test tmr can you make a task for it")
                .orElseThrow();
        Task task = draft.toTask();

        assertEquals("Study for test", task.getDescription());
        assertEquals(LocalDate.now().plusDays(1), task.getDueDate());
        assertEquals(LocalTime.of(9, 0), task.getDueTime());
        assertEquals("Medium", task.getPriority());
        assertEquals("School", task.getCategory());
    }

    @Test
    void inferLocalTaskDraftKeepsSpecificTestSubject() {
        ChatBoxController.TaskDraft draft = ChatBoxController
                .inferLocalTaskDraft("Hi I have a history test tmr can you make a task for it")
                .orElseThrow();

        assertEquals("Study for history test", draft.toTask().getDescription());
    }

    @Test
    void inferLocalTaskDraftUnderstandsReminderRequests() {
        ChatBoxController.TaskDraft draft = ChatBoxController
                .inferLocalTaskDraft("remind me to review chapter 4 tomorrow at 7pm")
                .orElseThrow();
        Task task = draft.toTask();

        assertEquals("Review chapter 4", task.getDescription());
        assertEquals(LocalDate.now().plusDays(1), task.getDueDate());
        assertEquals(LocalTime.of(19, 0), task.getDueTime());
    }

    @Test
    void storedHistoryIsTrimmedFromLineBoundary() {
        String history = "first line\nsecond line\nthird line\n";

        String trimmedHistory = ChatBoxController.recentHistoryForPrompt(history.repeat(300));

        assertTrue(trimmedHistory.length() <= 6_000);
        assertTrue(trimmedHistory.startsWith("[Earlier chat history trimmed]\n"));
    }

    @Test
    void parseResponseShowsServiceErrorMessage() {
        String body = """
                {
                  "error": { "message": "Service unavailable" }
                }
                """;

        assertEquals("AI service error: Service unavailable", ChatBoxController.parseResponse(500, body));
    }

    @Test
    void parseResponseShowsQuotaGuidance() {
        String body = """
                {
                  "error": {
                    "message": "Quota exceeded for metric: generate_content_free_tier_requests. Please retry in 23.9s.",
                    "status": "RESOURCE_EXHAUSTED"
                  }
                }
                """;

        assertEquals(
                "AI service error: Gemini quota is exhausted for this key and model. "
                        + "Check your Google AI Studio quota/billing, wait for quota to reset, or set GEMINI_MODEL "
                        + "to another model available to this API key. Google suggested retrying in 23.9s.",
                ChatBoxController.parseResponse(429, body)
        );
    }

    @Test
    void localFallbackKeepsGeminiErrorReason() {
        ChatBoxController.AiResponse response = ChatBoxController.withLocalTaskFallback(
                "I have a test tmr can you make a task for it",
                new ChatBoxController.AiResponse("AI service error: Gemini quota is exhausted.", List.of())
        );

        assertTrue(response.reply().startsWith("AI service error: Gemini quota is exhausted."));
        assertTrue(response.reply().contains("I found a local task draft from your message instead."));
        assertEquals(1, response.taskDrafts().size());
    }

    @Test
    void parseResponseShowsInvalidApiKeyGuidance() {
        String body = """
                {
                  "error": {
                    "message": "API key not valid. Please pass a valid API key.",
                    "details": [
                      { "reason": "API_KEY_INVALID" }
                    ]
                  }
                }
                """;

        assertEquals(
                "AI service error: The configured Gemini API key is invalid. "
                        + "Update GEMINI_API_KEY, GOOGLE_API_KEY, or API_KEY with a valid Gemini key, "
                        + "then restart the app.",
                ChatBoxController.parseResponse(400, body)
        );
    }
}

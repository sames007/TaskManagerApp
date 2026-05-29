package edu.farmingdale.taskmanagerapp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

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
                "application/json",
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

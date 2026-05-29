package edu.farmingdale.taskmanagerapp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

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

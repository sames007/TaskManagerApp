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
    void parseResponseShowsServiceErrorMessage() {
        String body = """
                {
                  "error": { "message": "Quota exceeded" }
                }
                """;

        assertEquals("AI service error: Quota exceeded", ChatBoxController.parseResponse(429, body));
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

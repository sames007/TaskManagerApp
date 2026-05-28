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
                  "error": { "message": "API key invalid" }
                }
                """;

        assertEquals("AI service error: API key invalid", ChatBoxController.parseResponse(401, body));
    }
}

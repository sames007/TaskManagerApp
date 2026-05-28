package edu.farmingdale.taskmanagerapp;

/**
 * Retrieves AI service credentials without requiring secrets in source control.
 */
public class AI_Helper {

    private AI_Helper() {
    }

    /**
     * @return the configured Gemini/Google API key, or null when none is configured
     */
    public static String getAPIKey() {
        return AppConfig.getFirst("GEMINI_API_KEY", "GOOGLE_API_KEY", "API_KEY").orElse(null);
    }
}

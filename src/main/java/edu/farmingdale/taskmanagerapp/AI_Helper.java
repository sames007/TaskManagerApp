package edu.farmingdale.taskmanagerapp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The AI_Helper class is a utility class designed to manage configuration properties
 * for an application.
 * The configuration file is expected to be located in the classpath under
 * "edu/farmingdale/taskmanagerapp/config.properties".
 * A static block is used to ensure that the configuration file is loaded when the
 * class is first accessed.
 */
public class AI_Helper {
    // A Properties object to hold our key-value pairs from the config file
    private static final Properties properties = new Properties();

    // Static block to load the config file when the class is first loaded
    static {
        // Try to load the config.properties file from the classpath
        try (InputStream in = AI_Helper.class.getResourceAsStream("/edu/farmingdale/taskmanagerapp/config.properties")) {
            if (in == null) {
                // If the file is not found, print an error message
                System.out.println("Error: config.properties file not found!");
            } else {
                // Load the properties from the file
                properties.load(in);
            }
        } catch (IOException e) {
            // Print the error if there is a problem reading the file
            System.out.println("Error loading config.properties: " + e.getMessage());
        }
    }

    // Public method to get the API key from the loaded properties

    /**
     * @return the API key as a String
     */
    public static String getAPIKey() {
        return properties.getProperty("API_KEY");
    }
}

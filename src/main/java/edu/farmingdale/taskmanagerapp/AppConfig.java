package edu.farmingdale.taskmanagerapp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central configuration loader.
 *
 * Values are loaded in this order, from highest to lowest precedence:
 * Java system properties, ignored local config.local.properties, environment
 * variables, then bundled config.properties.
 */
final class AppConfig {
    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());
    private static final String CLASSPATH_CONFIG = "/edu/farmingdale/taskmanagerapp/config.properties";
    private static final Path LOCAL_CONFIG = Path.of("config.local.properties");
    private static final Properties BUNDLED_PROPERTIES = loadBundledProperties();
    private static final Properties LOCAL_PROPERTIES = loadLocalProperties();

    private AppConfig() {
    }

    static Optional<String> get(String key) {
        String systemValue = clean(System.getProperty(key));
        if (systemValue != null) {
            return Optional.of(systemValue);
        }

        String localValue = clean(LOCAL_PROPERTIES.getProperty(key));
        if (localValue != null) {
            return Optional.of(localValue);
        }

        String envValue = clean(System.getenv(toEnvironmentKey(key)));
        if (envValue == null) {
            envValue = clean(System.getenv(key));
        }
        if (envValue != null) {
            return Optional.of(envValue);
        }

        return Optional.ofNullable(clean(BUNDLED_PROPERTIES.getProperty(key)));
    }

    static Optional<String> getFirst(String... keys) {
        for (String key : keys) {
            Optional<String> value = get(key);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    private static Properties loadBundledProperties() {
        Properties properties = new Properties();

        try (InputStream bundled = AppConfig.class.getResourceAsStream(CLASSPATH_CONFIG)) {
            if (bundled != null) {
                properties.load(bundled);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Unable to read bundled config.properties", e);
        }

        return properties;
    }

    private static Properties loadLocalProperties() {
        Properties properties = new Properties();

        if (Files.isRegularFile(LOCAL_CONFIG)) {
            try (InputStream local = Files.newInputStream(LOCAL_CONFIG)) {
                properties.load(local);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Unable to read config.local.properties", e);
            }
        }

        return properties;
    }

    private static String toEnvironmentKey(String key) {
        return key.toUpperCase(Locale.ROOT)
                .replace('.', '_')
                .replace('-', '_');
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

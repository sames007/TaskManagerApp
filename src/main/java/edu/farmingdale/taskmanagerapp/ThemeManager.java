package edu.farmingdale.taskmanagerapp;

import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.prefs.Preferences;

/**
 * Applies the application theme from the operating system color preference.
 */
final class ThemeManager {
    private static final String LIGHT_THEME = "/edu/farmingdale/taskmanagerapp/styling/styles.css";
    private static final String DARK_THEME = "/edu/farmingdale/taskmanagerapp/styling/darkTheme.css";
    private static final String LIGHT_MODE_CLASS = "light-mode";
    private static final String DARK_MODE_CLASS = "dark-mode";
    private static final String THEME_PREF_KEY = "themePreference";
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(ThemeManager.class);
    private static final Map<Scene, ToggleButton> BOUND_SCENES = new WeakHashMap<>();
    private static boolean systemListenerRegistered;

    enum ThemePreference {
        SYSTEM,
        LIGHT,
        DARK
    }

    private ThemeManager() {
    }

    static void bindToSystemTheme(Scene scene) {
        bindToSystemTheme(scene, null);
    }

    static void bindToSystemTheme(Scene scene, ToggleButton indicator) {
        if (scene == null) {
            return;
        }

        BOUND_SCENES.put(scene, indicator);
        registerSystemThemeListener();
        applyPreferredTheme(scene, indicator);
    }

    static void applySystemTheme(Scene scene) {
        applySystemTheme(scene, null);
    }

    static void applySystemTheme(Scene scene, ToggleButton indicator) {
        applyPreferredTheme(scene, indicator);
    }

    static ThemePreference getPreference() {
        try {
            return ThemePreference.valueOf(PREFERENCES.get(THEME_PREF_KEY, ThemePreference.SYSTEM.name()));
        } catch (IllegalArgumentException e) {
            return ThemePreference.SYSTEM;
        }
    }

    static void setPreference(ThemePreference preference) {
        PREFERENCES.put(THEME_PREF_KEY, preference.name());
        refreshBoundScenes();
    }

    static void applyTheme(Scene scene, boolean darkMode) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }

        String lightTheme = Objects.requireNonNull(ThemeManager.class.getResource(LIGHT_THEME)).toExternalForm();
        String darkTheme = Objects.requireNonNull(ThemeManager.class.getResource(DARK_THEME)).toExternalForm();
        scene.getStylesheets().remove(lightTheme);
        scene.getStylesheets().remove(darkTheme);
        scene.getStylesheets().add(0, darkMode ? darkTheme : lightTheme);

        scene.getRoot().getStyleClass().removeAll(LIGHT_MODE_CLASS, DARK_MODE_CLASS);
        scene.getRoot().getStyleClass().add(darkMode ? DARK_MODE_CLASS : LIGHT_MODE_CLASS);
    }

    static boolean isSystemDark() {
        return Platform.getPreferences().getColorScheme() == ColorScheme.DARK;
    }

    static String currentThemeLabel() {
        ThemePreference preference = getPreference();
        return switch (preference) {
            case SYSTEM -> "Theme: System (" + (isSystemDark() ? "Dark" : "Light") + ")";
            case LIGHT -> "Theme: Light";
            case DARK -> "Theme: Dark";
        };
    }

    static String labelFor(ThemePreference preference) {
        return switch (preference) {
            case SYSTEM -> "Use System Theme";
            case LIGHT -> "Light Theme";
            case DARK -> "Dark Theme";
        };
    }

    private static void applyPreferredTheme(Scene scene, ToggleButton indicator) {
        applyTheme(scene, resolveDarkMode());
        updateThemeIndicator(indicator);
    }

    private static boolean resolveDarkMode() {
        return switch (getPreference()) {
            case SYSTEM -> isSystemDark();
            case LIGHT -> false;
            case DARK -> true;
        };
    }

    private static void updateThemeIndicator(ToggleButton indicator) {
        if (indicator == null) {
            return;
        }

        indicator.setSelected(resolveDarkMode());
        indicator.setText(switch (getPreference()) {
            case SYSTEM -> "System: " + (isSystemDark() ? "Dark" : "Light");
            case LIGHT -> "Theme: Light";
            case DARK -> "Theme: Dark";
        });
        indicator.setTooltip(new Tooltip("Click to switch between System, Light, and Dark themes."));
        indicator.setOnAction(event -> setPreference(nextPreference()));
    }

    private static ThemePreference nextPreference() {
        return switch (getPreference()) {
            case SYSTEM -> ThemePreference.LIGHT;
            case LIGHT -> ThemePreference.DARK;
            case DARK -> ThemePreference.SYSTEM;
        };
    }

    private static void refreshBoundScenes() {
        BOUND_SCENES.forEach(ThemeManager::applyPreferredTheme);
    }

    private static void registerSystemThemeListener() {
        if (systemListenerRegistered) {
            return;
        }

        Platform.getPreferences().colorSchemeProperty().addListener((observable, oldValue, newValue) -> {
            if (getPreference() == ThemePreference.SYSTEM) {
                refreshBoundScenes();
            }
        });
        systemListenerRegistered = true;
    }
}

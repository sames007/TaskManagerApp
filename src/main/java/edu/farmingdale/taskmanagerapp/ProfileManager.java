package edu.farmingdale.taskmanagerapp;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ProfileManager class handles the management of user profiles,
 * including profile picture updates, displaying user-specific options,
 * and session-related functionality.
 */
public class ProfileManager {
    private static final Logger LOGGER = Logger.getLogger(ProfileManager.class.getName());
    private static final String DEFAULT_PROFILE_PIC = "/edu/farmingdale/taskmanagerapp/images/profilePicture.png";
    private static final Path PROFILE_PICS_DIR = Paths.get("profile_pictures").toAbsolutePath().normalize();
    private static final long MAX_PROFILE_PICTURE_BYTES = 5_000_000;
    private final TaskManagerController mainController;
    private ImageView profilePicture;
    private UserSession currentUser;
    private ToggleButton themeToggleBtn;

    /**
     * Constructor for the ProfileManager.
     * @param controller The main controller
     */
    public ProfileManager(TaskManagerController controller) {
        this.mainController = controller;
        createProfilePicsDirectory();
    }

    /**
     * @param profilePicture The ImageView to initialize
     */
    public void initialize(ImageView profilePicture, ToggleButton themeToggleBtn) {
        this.profilePicture = profilePicture;
        this.themeToggleBtn = themeToggleBtn;
        setupProfilePictureClickHandler();
    }

    /**
     * Sets up the click handler for the profile picture.
     */
    private void setupProfilePictureClickHandler() {
        if (profilePicture != null && themeToggleBtn != null) {
            profilePicture.setOnMouseClicked(event -> {
                if (currentUser == null) {
                    showNotLoggedInMenu();
                } else {
                    showLoggedInMenu();
                }
            });
        }
    }

    /**
     * Shows the not logged-in menu.
     */
    private void showNotLoggedInMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem loginItem = new MenuItem("Login");
        MenuItem registerItem = new MenuItem("Register");

        loginItem.setOnAction(event -> mainController.showLoginScreen());
        registerItem.setOnAction(event -> mainController.showSignUpScreen());

        contextMenu.getItems().addAll(loginItem, registerItem, new SeparatorMenuItem(), createThemeMenu());
        contextMenu.show(profilePicture, profilePicture.getScene().getWindow().getX() + profilePicture.getLayoutX(), 
                        profilePicture.getScene().getWindow().getY() + profilePicture.getLayoutY());
    }

    /**
     * Shows the logged-in menu.
     */
    private void showLoggedInMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem changePictureItem = new MenuItem("Change Profile Picture");
        MenuItem viewProfileItem = new MenuItem("View Profile");
        MenuItem logoutItem = new MenuItem("Logout");

        changePictureItem.setOnAction(event -> changeProfilePicture());
        viewProfileItem.setOnAction(event -> viewProfile());
        logoutItem.setOnAction(event -> logout());

        contextMenu.getItems().addAll(changePictureItem, viewProfileItem, logoutItem, new SeparatorMenuItem(), createThemeMenu());
        contextMenu.show(profilePicture, profilePicture.getScene().getWindow().getX() + profilePicture.getLayoutX(),
                        profilePicture.getScene().getWindow().getY() + profilePicture.getLayoutY());
    }

    private Menu createThemeMenu() {
        Menu themeMenu = new Menu(ThemeManager.currentThemeLabel());
        ToggleGroup group = new ToggleGroup();

        for (ThemeManager.ThemePreference preference : ThemeManager.ThemePreference.values()) {
            RadioMenuItem item = new RadioMenuItem(ThemeManager.labelFor(preference));
            item.setToggleGroup(group);
            item.setSelected(ThemeManager.getPreference() == preference);
            item.setOnAction(event -> ThemeManager.setPreference(preference));
            themeMenu.getItems().add(item);
        }

        return themeMenu;
    }

    /**
     * Changes the profile picture.
     */
    private void changeProfilePicture() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(profilePicture.getScene().getWindow());
        if (selectedFile != null) {
            try {
                if (!isValidImageFile(selectedFile)) {
                    mainController.showAlert("Please choose a PNG, JPG, JPEG, or GIF image under 5 MB.");
                    return;
                }

                String fileName = currentUser.getUserID() + "_" + sanitizeFileName(selectedFile.getName());
                Path targetPath = PROFILE_PICS_DIR.resolve(fileName).normalize();
                if (!targetPath.startsWith(PROFILE_PICS_DIR)) {
                    throw new SecurityException("Invalid profile picture path.");
                }

                Files.createDirectories(PROFILE_PICS_DIR);
                Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                // Update the profile picture in the UI
                Image newImage = new Image(targetPath.toUri().toString());
                profilePicture.setImage(newImage);
                
                // Update the user's profile picture path in the database
                if (mainController.isDatabaseAvailable()) {
                    currentUser.setProfilePicturePath(targetPath.toString());
                    mainController.getDbManager().updateUserProfilePicture(currentUser);
                } else {
                    currentUser.setProfilePicturePath(targetPath.toString());
                }
            } catch (Exception e) {
                mainController.showAlert("Failed to update profile picture: " + e.getMessage());
            }
        }
    }

    /**
     * Views the user's profile.
     */
    private void viewProfile() {
        if (currentUser != null) {
            mainController.showAlert("Profile Information:\nUsername: " + currentUser.getUserName() + 
                                   "\nEmail: " + currentUser.getEmail());
        }
    }

    /**
     * Logs out the user.
     */
    private void logout() {
        currentUser = null;
        mainController.handleLogout();
        resetProfilePicture();
    }

    /**
     * Sets the current user.
     * @param user The user to set
     */
    public void setCurrentUser(UserSession user) {
        this.currentUser = user;
        if (profilePicture == null) {
            return; // Early return if ImageView is not initialized yet
        }

        if (user != null && user.getProfilePicturePath() != null) {
            try {
                File profilePicFile = new File(user.getProfilePicturePath());
                if (profilePicFile.exists()) {
                    Image userImage = new Image(profilePicFile.toURI().toString());
                    profilePicture.setImage(userImage);
                } else {
                    resetProfilePicture();
                }
            } catch (Exception e) {
                resetProfilePicture();
            }
        } else {
            resetProfilePicture();
        }
    }

    /**
     * Resets the profile picture to the default image.
     */
    private void resetProfilePicture() {
        if (profilePicture == null) {
            return; // Early return if ImageView is not initialized yet
        }

        try {
            Image defaultImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(DEFAULT_PROFILE_PIC)));
            profilePicture.setImage(defaultImage);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load default profile picture.", e);
        }
    }


    /**
     * Creates the profile pictures directory if it doesn't exist.
     */
    private void createProfilePicsDirectory() {
        try {
            Files.createDirectories(PROFILE_PICS_DIR);
        } catch (Exception e) {
            mainController.showAlert("Failed to create profile pictures directory: " + e.getMessage());
        }
    }

    private boolean isValidImageFile(File file) {
        if (!file.isFile() || file.length() > MAX_PROFILE_PICTURE_BYTES) {
            return false;
        }

        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".png")
                || name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".gif");
    }

    private String sanitizeFileName(String originalName) {
        String sanitized = originalName.replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.isBlank() ? "profile-picture.png" : sanitized;
    }
}

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
import java.util.Objects;

/**
 * The ProfileManager class handles the management of user profiles,
 * including profile picture updates, displaying user-specific options,
 * and session-related functionality.
 */
public class ProfileManager {
    private static final String DEFAULT_PROFILE_PIC = "/edu/farmingdale/taskmanagerapp/images/profilePicture.png";
    private static final String PROFILE_PICS_DIR = "profile_pictures";
    private final TaskManagerController mainController;
    private ImageView profilePicture;
    private UserSession currentUser;

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
    public void initialize(ImageView profilePicture) {
        this.profilePicture = profilePicture;
        setupProfilePictureClickHandler();
    }

    /**
     * Sets up the click handler for the profile picture.
     */
    private void setupProfilePictureClickHandler() {
        if (profilePicture != null) {
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

        contextMenu.getItems().addAll(loginItem, registerItem);
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

        contextMenu.getItems().addAll(changePictureItem, viewProfileItem, logoutItem);
        contextMenu.show(profilePicture, profilePicture.getScene().getWindow().getX() + profilePicture.getLayoutX(),
                        profilePicture.getScene().getWindow().getY() + profilePicture.getLayoutY());
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
                String fileName = currentUser.getUserID() + "_" + selectedFile.getName();
                Path targetPath = Paths.get(PROFILE_PICS_DIR, fileName);
                Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                // Update the profile picture in the UI
                Image newImage = new Image(targetPath.toUri().toString());
                profilePicture.setImage(newImage);
                
                // Update the user's profile picture path in the database
                if (mainController.getDbManager() != null) {
                    currentUser.setProfilePicturePath(targetPath.toString());
                    mainController.getDbManager().updateUserProfilePicture(currentUser);
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
        Image defaultImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(DEFAULT_PROFILE_PIC)));
        profilePicture.setImage(defaultImage);
    }

    /**
     * Creates the profile pictures directory if it doesn't exist.
     */
    private void createProfilePicsDirectory() {
        try {
            Files.createDirectories(Paths.get(PROFILE_PICS_DIR));
        } catch (Exception e) {
            mainController.showAlert("Failed to create profile pictures directory: " + e.getMessage());
        }
    }
} 
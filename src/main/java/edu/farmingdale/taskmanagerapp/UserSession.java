package edu.farmingdale.taskmanagerapp;

public class UserSession {
    private static UserSession instance;
    private int userID;
    private String userName;
    private String email;
    private String password;
    private String profilePicturePath;

    public UserSession(String userName, String email, String password) {
        this.userName = userName;
        this.email = email;
        this.password = password;
        this.userID = 0;
        this.profilePicturePath = null;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public String getProfilePicturePath() {
        return profilePicturePath;
    }

    public void setProfilePicturePath(String profilePicturePath) {
        this.profilePicturePath = profilePicturePath;
    }

    public void cleanUserSession() {
        this.userID = 0;
        this.userName = "";
        this.password = "";
        this.email = "";
        this.profilePicturePath = null;
        }
    }



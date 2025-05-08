package edu.farmingdale.taskmanagerapp;

/**
 * Gets User Details 
 */
public class UserSession {
    private static UserSession instance;
    private int userID;
    private String userName;
    private String email;
    private String password;
    private String securityQuestion;
    private String securityAnswer;
    private String profilePicturePath;

    /**
     * @param userName User's name
     * @param email User's email
     * @param password User's password
     */
    public UserSession(String userName, String email, String password, String securityQuestion, String securityAnswer) {
        this.userName = userName;
        this.email = email;
        this.password = password;
        this.securityQuestion = securityQuestion;
        this.securityAnswer = securityAnswer;
        this.userID = 0;
        this.profilePicturePath = null;
    }


    /**
     * Gets the user's name
     * @return the user's name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the user's name
     * @param userName the user's name to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Gets the user's password
     * @return the user's password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the user's password
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets the user's email
     * @return the user's email
     */
    public String getEmail() {
        return email;
    }


    /**
     * Sets the user's email
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the user's ID
     * @return the user's ID
     */
    public int getUserID() {
        return userID;
    }

    /**
     * Sets the user's ID
     * @param userID the user ID to set
     */
    public void setUserID(int userID) {
        this.userID = userID;
    }

    /**
     * Gets the path to the user's profile picture
     * @return the profile picture path
     */
    public String getProfilePicturePath() {
        return profilePicturePath;
    }

    /**
     * Sets the path to the user's profile picture
     * @param profilePicturePath the profile picture path to set
     */
    public void setProfilePicturePath(String profilePicturePath) {
        this.profilePicturePath = profilePicturePath;
    }

    /**
     * Gets the user's security question
     * @return the user's security question
     */
    public String getSecurityQuestion(){return this.securityQuestion;}

    /**
     * Sets the user's security question
     * @param s the user security question to set
     */
    public void setSecurityQuestion(String s){this.securityQuestion = s;}

    /**
     * Gets the user's security answer
     * @return the user's security answer
     */
    public String getSecurityAnswer(){return this.securityAnswer;}

    /**
     * Sets the user's security a
     * @param s the user security answer to set
     */
    public void setSecurityAnswer(String s){this.securityAnswer = s;}

    /**
     * Erases User Information
     */
    public void cleanUserSession() {
        this.userID = 0;
        this.userName = "";
        this.password = "";
        this.email = "";
        this.profilePicturePath = null;
        }
    }
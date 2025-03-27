package edu.farmingdale.taskmanagerapp;

public class UserSession {
    private static UserSession instance;
    private int userID;
    private String userName;
    private String email;
    private String password;

    public UserSession(int UserID, String userName, String email, String password){
        this.userID = userID;
        this.userName = userName;
        this.email = email;
        this.password = password;
    }
    public UserSession(String userName, String email, String password){
        this.userID = userID;
        this.userName = userName;
        this.email = email;
        this.password = password;
    }

    public String getUserName(){return this.userName;}
    public void setUserName(String s){this.userName = s;}

    public String getPassword(){return this.password;}
    public void setPassword(String s){this.password = s;}

    public String getEmail(){return this.email;}
    public void setEmail(String s){this.email = s;}

    public int getUserID(){return this.userID;}
    public void setUserID(int i){this.userID = i;}

    public void cleanUserSession(){
        this.userID = 0;
        this.userName = "";
        this.password = "";
        this.email = "";
        }
    }



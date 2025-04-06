package edu.farmingdale.taskmanagerapp;

import javafx.collections.ObservableList;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://taskmanagerdbserver.mysql.database.azure.com:3306/TaskManagerDB";
    private static final String USER = "adminuser";
    private static final String PASS = "philippejean1234$";

    private Connection conn;

    public DatabaseManager() {
        try {
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (SQLException e) {
            System.out.println("Error connecting to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int getUserID(String userName) {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT UserID FROM users WHERE UserName = ?")) {
            stmt.setString(1, userName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("UserID");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting user ID: " + e.getMessage());
        }
        return -1; // Return -1 if not found
    }

    private void insertDefaultUser() {
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (UserID, UserName) VALUES (?, ?)")) {
            stmt.setInt(1, 1); // Assuming default user ID
            stmt.setString(2, "DefaultUser");
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error inserting default user: " + e.getMessage());
        }
    }

    public void registerUser(UserSession s){
        try {
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            Statement statement = conn.createStatement();
            String sql = "INSERT INTO users (UserName, PassWord, Email) VALUES (?, ?, ?)";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, s.getUserName());
            preparedStatement.setString(2, s.getPassword());
            preparedStatement.setString(3, s.getEmail());

            int row = preparedStatement.executeUpdate();
            statement.close();
            conn.close();

        } catch (Exception e){

        }
    }

    public UserSession getAccount(String username) {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE UserName = ?")) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    UserSession s = new UserSession(rs.getString("UserName"), rs.getString("Email"), rs.getString("PassWord"));
                    return s;
                } else {
                    return null;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }catch (SQLException e){
            throw new RuntimeException(e);
        }

    }
    public void addTask(Task task) {
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO Tasks (Description, DueDate, DueTime, FK_PriorityID, FK_CategoryID, FK_UserID, Status) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, task.getDescription());
            stmt.setDate(2, Date.valueOf(task.getDueDate()));
            stmt.setTime(3, Time.valueOf(task.getDueTime()));
            stmt.setInt(4, getPriorityID(task.getPriority()));
            stmt.setInt(5, getCategoryID(task.getCategory()));

            // Check if user exists, otherwise insert a default user
            int userID = getUserID("DefaultUser");
            if (userID == -1) {
                insertDefaultUser();
                userID = getUserID("DefaultUser");
            }

            stmt.setInt(6, userID);
            stmt.setString(7, task.getStatus());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error adding task to database: " + e.getMessage());
        }
    }


    public void deleteTask(int taskID) {
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM Tasks WHERE TaskID = ?")) {
            stmt.setInt(1, taskID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error deleting task from database: " + e.getMessage());
        }
    }
    public void updateTask(Task task) {
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE Tasks SET Description = ?, DueDate = ?, DueTime = ?, FK_PriorityID = ?, FK_CategoryID = ?, Status = ? WHERE TaskID = ?")) {
            stmt.setString(1, task.getDescription());
            stmt.setDate(2, Date.valueOf(task.getDueDate()));
            stmt.setTime(3, Time.valueOf(task.getDueTime()));
            stmt.setInt(4, getPriorityID(task.getPriority()));
            stmt.setInt(5, getCategoryID(task.getCategory()));
            stmt.setString(6, task.getStatus());
            stmt.setInt(7, task.getTaskID()); // Assuming you have a getTaskID method in Task class
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error updating task in database: " + e.getMessage());
        }
    }
    public void markTaskComplete(int taskID) {
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE Tasks SET Status = 'Completed' WHERE TaskID = ?")) {
            stmt.setInt(1, taskID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error marking task as complete: " + e.getMessage());
        }
    }

    // Helper methods to get IDs for priority and category
    private int getPriorityID(String priority) {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT PriorityID FROM Priorities WHERE PriorityLevel = ?")) {
            stmt.setString(1, priority);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("PriorityID");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting priority ID: " + e.getMessage());
        }
        return -1; // Return -1 if not found
    }

    private int getCategoryID(String category) {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT CategoryID FROM Categories WHERE CategoryName = ?")) {
            stmt.setString(1, category);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("CategoryID");
                } else {
                    // Insert missing category and return its ID
                    try (PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO Categories (CategoryName) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                        insertStmt.setString(1, category);
                        insertStmt.executeUpdate();
                        try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                return generatedKeys.getInt(1);
                            }
                        }
                    } catch (SQLException e) {
                        System.out.println("Error inserting category: " + e.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting category ID: " + e.getMessage());
        }
        return -1; // Return -1 if all else fails
    }

    // Method to retrieve all tasks from the database
    public void loadTasks(ObservableList<Task> tasks) {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM Tasks")) {
            while (rs.next()) {
                Task task = new Task(
                        rs.getString("Description"),
                        rs.getDate("DueDate").toLocalDate(),
                        rs.getTime("DueTime") != null ? rs.getTime("DueTime").toLocalTime() : null,
                        getPriorityName(rs.getInt("FK_PriorityID"))
                );
                task.setCategory(getCategoryName(rs.getInt("FK_CategoryID")));
                task.setStatus(rs.getString("Status"));
                task.setTaskID(rs.getInt("TaskID"));
                tasks.add(task);
                System.out.println("Task added to list: " + task.getDescription()); // Verify tasks are added
            }
        } catch (SQLException e) {
            System.out.println("Error loading tasks from database: " + e.getMessage());
        }
    }




    // Helper methods to get names for priority and category
    private String getPriorityName(int priorityID) {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT PriorityLevel FROM Priorities WHERE PriorityID = ?")) {
            stmt.setInt(1, priorityID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("PriorityLevel");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting priority name: " + e.getMessage());
        }
        return ""; // Return empty string if not found
    }

    private String getCategoryName(int categoryID) {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT CategoryName FROM Categories WHERE CategoryID = ?")) {
            stmt.setInt(1, categoryID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("CategoryName");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting category name: " + e.getMessage());
        }
        return ""; // Return empty string if not found
    }

    // Close the connection when done
    public void closeConnection() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.out.println("Error closing database connection: " + e.getMessage());
            }
        }
    }
}
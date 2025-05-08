package edu.farmingdale.taskmanagerapp;

import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The DatabaseManager class is responsible for managing a connection to the database
 * and performing various operations such as user account management and task management.
 */
public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final String DB_URL = "jdbc:mysql://taskmanagerdbserver.mysql.database.azure.com:3306/TaskManagerDB";
    private static final String USER = "adminuser";
    private static final String PASS = "philippejean1234$";
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;

    private Connection conn;

    /**
     * Constructor to initialize the connection
     */
    public DatabaseManager() {
        initializeConnection();
    }

    /**
     * Initializes the database connection.
     */
    public void initializeConnection() {
        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                Properties props = new Properties();
                props.setProperty("user", USER);
                props.setProperty("password", PASS);
                props.setProperty("useSSL", "true");
                props.setProperty("autoReconnect", "true");
                props.setProperty("maxReconnects", "3");

                conn = DriverManager.getConnection(DB_URL, props);

                // Check if the ProfilePicturePath column exists and add it if it doesn't
                try (Statement stmt = conn.createStatement()) {
                    // Check if a column exists
                    ResultSet rs = conn.getMetaData().getColumns(null, null, "users", "ProfilePicturePath");
                    if (!rs.next()) {
                        // Column doesn't exist, so add it
                        stmt.execute("ALTER TABLE users ADD COLUMN ProfilePicturePath VARCHAR(255)");
                        stmt.execute("UPDATE users SET ProfilePicturePath = '/edu/farmingdale/taskmanagerapp/images/profilePicture.png'");
                        LOGGER.log(Level.INFO, "Added ProfilePicturePath column to users table");
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error checking/adding ProfilePicturePath column: " + e.getMessage());
                }

                return;
            } catch (SQLException e) {
                retries++;
                LOGGER.log(Level.SEVERE, "Error connecting to database (attempt " + retries + "): " + e.getMessage());
                if (retries == MAX_RETRIES) {
                    throw new RuntimeException("Failed to connect to database after " + MAX_RETRIES + " attempts", e);
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Connection interrupted", ie);
                }
            }
        }
    }

    /**
     * @return The database connection
     */
    private Connection getConnection() {
        try {
            if (conn == null || conn.isClosed()) {
                initializeConnection();
            }
            return conn;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting database connection: " + e.getMessage());
            throw new RuntimeException("Database connection error", e);
        }
    }

    /**
     * @param userName The name of the user
     * @return The user's ID
     */
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

    /**
     * Inserts a default user if none exist
     */
    private void insertDefaultUser() {
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (UserID, UserName) VALUES (?, ?)")) {
            stmt.setInt(1, 1); // Assuming default user ID
            stmt.setString(2, "DefaultUser");
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error inserting default user: " + e.getMessage());
        }
    }

    /**
     * @param s The user session to register
     */
    public void registerUser(UserSession s) {
        try {
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            Statement statement = conn.createStatement();
            String sql = "INSERT INTO users (UserName, PassWord, Email, SecurityQuestion, SecurityAnswer) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, s.getUserName());
            preparedStatement.setString(2, s.getPassword());
            preparedStatement.setString(3, s.getEmail());
            preparedStatement.setString(4, s.getSecurityQuestion());
            preparedStatement.setString(5, s.getSecurityAnswer());

            int row = preparedStatement.executeUpdate();
            statement.close();
            conn.close();

        } catch (Exception e) {

        }
    }

    /**
     * @param username The username of the user
     * @return The user session
     */
    public UserSession getAccount(String username) {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE UserName = ?")) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    UserSession s = new UserSession(rs.getString("UserName"), rs.getString("Email"), rs.getString("PassWord"), rs.getString("SecurityQuestion"), rs.getString("SecurityAnswer"));
                    return s;
                } else {
                    return null;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * @param email The email of the user
     * @return The user session
     */
    public UserSession getAccountByEmail(String email) {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE Email = ?")) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    UserSession s = new UserSession(rs.getString("UserName"), rs.getString("Email"), rs.getString("PassWord"), rs.getString("SecurityQuestion"), rs.getString("SecurityAnswer"));
                    return s;
                } else {
                    return null;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }


    /**
     * Updates password in database
     * @param email
     * @param newPassword
     * @return boolean
     */
    public boolean updatePassword(String email, String newPassword) {
        try {
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            String sql = "UPDATE users SET Password = ? WHERE Email = ?";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, newPassword);
            preparedStatement.setString(2, email);

            int rows = preparedStatement.executeUpdate();
            preparedStatement.close();
            conn.close();

            if (rows == 0) {
                throw new RuntimeException("No user found with that email!");
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Error updating password: " + e.getMessage());
        }
    }

    /**
     * @param task The task to add
     */
    public void addTask(@NotNull Task task) {
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO Tasks (Description, DueDate, DueTime, FK_PriorityID, FK_CategoryID, FK_UserID, Status) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, task.getDescription());
            stmt.setDate(2, Date.valueOf(task.getDueDate()));
            stmt.setTime(3, Time.valueOf(task.getDueTime()));
            stmt.setInt(4, getPriorityID(task.getPriority()));
            stmt.setInt(5, getCategoryID(task.getCategory()));

            // Check if a user exists, otherwise insert a default user
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

    /**
     * @param task The task to update
     */
    public void updateTask(@NotNull Task task) {
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

    /**
     * @param taskID The ID of the task to mark as complete
     */
    public void markTaskComplete(int taskID) {
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE Tasks SET Status = 'Completed' WHERE TaskID = ?")) {
            stmt.setInt(1, taskID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error marking task as complete: " + e.getMessage());
        }
    }

    // Helper methods to get IDs for priority and category
    /**
     * @param priority The priority level
     * @return The priority ID
     */
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

    /**
     * @param category The category name
     * @return The category ID
     */
    private int getCategoryID(String category) {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT CategoryID FROM Categories WHERE CategoryName = ?")) {
            stmt.setString(1, category);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("CategoryID");
                } else {
                    // Insert the missing category and return its ID
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
    /**
     * @param tasks The list to add tasks to
     */
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
    /**
     * @param priorityID The ID of the priority
     * @return The priority name
     */
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
        return ""; // Return an empty string if not found
    }

    /**
     * @param categoryID The ID of the category
     * @return The category name
     */
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
        return ""; // Return an empty string if not found
    }

    /**
     * @param userName The name of the user
     * @param profilePicturePath The path to the profile picture
     */
    public void updateProfilePicture(@NotNull String userName, String profilePicturePath) {
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE users SET ProfilePicturePath = ? WHERE UserName = ?")) {
            stmt.setString(1, profilePicturePath);
            stmt.setString(2, userName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating profile picture: " + e.getMessage());
            throw new RuntimeException("Failed to update profile picture", e);
        }
    }

    /**
     * @param user The user session to update
     */
    public void updateUserProfilePicture(@NotNull UserSession user) {
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE users SET ProfilePicturePath = ? WHERE UserID = ?")) {
            stmt.setString(1, user.getProfilePicturePath());
            stmt.setInt(2, user.getUserID());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating profile picture: " + e.getMessage());
            throw new RuntimeException("Failed to update profile picture in database", e);
        }
    }

    // Close the connection when done
    /**
     * Closes the database connection
     */
    public void closeConnection() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error closing database connection: " + e.getMessage());
            } finally {
                conn = null;
            }
        }
    }
}
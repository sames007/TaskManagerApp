package edu.farmingdale.taskmanagerapp;

import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Types;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages all database access for users, authentication, profile data, and tasks.
 */
public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1_000;

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final boolean configured;
    private boolean available;
    private Connection conn;

    /**
     * Creates a database manager using DB_URL, DB_USER, and DB_PASSWORD config.
     */
    public DatabaseManager() {
        this.dbUrl = AppConfig.get("DB_URL").orElse(null);
        this.dbUser = AppConfig.get("DB_USER").orElse(null);
        this.dbPassword = AppConfig.get("DB_PASSWORD").orElse(null);
        this.configured = dbUrl != null && dbUser != null && dbPassword != null;

        if (configured) {
            try {
                initializeConnection();
            } catch (RuntimeException e) {
                available = false;
                LOGGER.log(Level.WARNING, "Database is configured but unavailable; continuing in offline mode.", e);
            }
        } else {
            LOGGER.warning("Database is not configured. Set DB_URL, DB_USER, and DB_PASSWORD to enable it.");
        }
    }

    /**
     * @return true when the app has the minimum database settings needed to connect
     */
    public boolean isConfigured() {
        return configured;
    }

    public boolean isAvailable() {
        return configured && available;
    }

    /**
     * Initializes or refreshes the database connection.
     */
    public final void initializeConnection() {
        if (!configured) {
            return;
        }

        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                Properties props = new Properties();
                props.setProperty("user", dbUser);
                props.setProperty("password", dbPassword);
                props.setProperty("sslMode", AppConfig.get("DB_SSL_MODE").orElse("REQUIRED"));
                props.setProperty("connectTimeout", "10000");
                props.setProperty("socketTimeout", "30000");
                props.setProperty("enabledTLSProtocols", "TLSv1.2,TLSv1.3");

                conn = DriverManager.getConnection(dbUrl, props);
                ensureProfilePictureColumn(conn);
                available = true;
                return;
            } catch (SQLException e) {
                available = false;
                retries++;
                LOGGER.log(Level.SEVERE, "Error connecting to database (attempt " + retries + ")", e);
                if (retries == MAX_RETRIES) {
                    throw new IllegalStateException("Failed to connect to the database.", e);
                }
                sleepBeforeRetry();
            }
        }
    }

    /**
     * Authenticates a user by email address or username.
     */
    public UserSession authenticateUser(String identifier, String password) {
        UserSession user = identifier != null && identifier.contains("@")
                ? getAccountByEmail(identifier)
                : getAccount(identifier);

        if (user == null || !PasswordUtil.verifyPassword(password, user.getPassword())) {
            return null;
        }

        if (PasswordUtil.needsRehash(user.getPassword())) {
            String upgradedHash = PasswordUtil.hashPassword(password);
            updatePasswordHash(user.getEmail(), upgradedHash);
            user.setPassword(upgradedHash);
        }

        return user;
    }

    /**
     * Registers a new user and stores password/security answers as salted hashes.
     */
    public void registerUser(@NotNull UserSession user) {
        String sql = """
                INSERT INTO users (UserName, PassWord, Email, SecurityQuestion, SecurityAnswer)
                VALUES (?, ?, ?, ?, ?)
                """;

        String passwordHash = PasswordUtil.hashPassword(user.getPassword());
        String securityAnswerHash = PasswordUtil.hashPassword(
                PasswordUtil.normalizeSecurityAnswer(user.getSecurityAnswer())
        );

        try (PreparedStatement stmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUserName());
            stmt.setString(2, passwordHash);
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getSecurityQuestion());
            stmt.setString(5, securityAnswerHash);

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setUserID(keys.getInt(1));
                }
            }
            user.setPassword(passwordHash);
            user.setSecurityAnswer(securityAnswerHash);
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalArgumentException("Username or email is already registered.", e);
        } catch (SQLException e) {
            throw new IllegalStateException("Error registering user.", e);
        }
    }

    /**
     * Looks up an account by username.
     */
    public UserSession getAccount(String username) {
        String sql = """
                SELECT UserID, UserName, Email, PassWord, SecurityQuestion, SecurityAnswer, ProfilePicturePath
                FROM users
                WHERE LOWER(UserName) = LOWER(?)
                """;
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? toUserSession(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error retrieving account.", e);
        }
    }

    /**
     * Looks up an account by email address.
     */
    public UserSession getAccountByEmail(String email) {
        String sql = """
                SELECT UserID, UserName, Email, PassWord, SecurityQuestion, SecurityAnswer, ProfilePicturePath
                FROM users
                WHERE LOWER(Email) = LOWER(?)
                """;
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? toUserSession(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error retrieving account by email.", e);
        }
    }

    /**
     * Updates a user's password after hashing it.
     */
    public boolean updatePassword(String email, String newPassword) {
        return updatePasswordHash(email, PasswordUtil.hashPassword(newPassword));
    }

    /**
     * Adds a task for the given user.
     */
    public void addTask(@NotNull Task task, int userID) {
        String sql = """
                INSERT INTO Tasks
                    (Description, DueDate, DueTime, FK_PriorityID, FK_CategoryID, FK_UserID, Status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setTaskFields(stmt, task);
            stmt.setInt(6, userID);
            stmt.setString(7, task.getStatus());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    task.setTaskID(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error adding task to database.", e);
        }
    }

    /**
     * Backward-compatible default-user task insert.
     */
    public void addTask(@NotNull Task task) {
        int defaultUserId = getOrCreateDefaultUserID();
        addTask(task, defaultUserId);
    }

    public void deleteTask(int taskID, int userID) {
        try (PreparedStatement stmt = getConnection().prepareStatement(
                "DELETE FROM Tasks WHERE TaskID = ? AND FK_UserID = ?")) {
            stmt.setInt(1, taskID);
            stmt.setInt(2, userID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Error deleting task.", e);
        }
    }

    public void deleteTask(int taskID) {
        try (PreparedStatement stmt = getConnection().prepareStatement("DELETE FROM Tasks WHERE TaskID = ?")) {
            stmt.setInt(1, taskID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Error deleting task.", e);
        }
    }

    /**
     * Updates a task owned by the given user.
     */
    public void updateTask(@NotNull Task task, int userID) {
        String sql = """
                UPDATE Tasks
                SET Description = ?, DueDate = ?, DueTime = ?, FK_PriorityID = ?, FK_CategoryID = ?, Status = ?
                WHERE TaskID = ? AND FK_UserID = ?
                """;

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            setTaskFields(stmt, task);
            stmt.setString(6, task.getStatus());
            stmt.setInt(7, task.getTaskID());
            stmt.setInt(8, userID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Error updating task.", e);
        }
    }

    public void updateTask(@NotNull Task task) {
        String sql = """
                UPDATE Tasks
                SET Description = ?, DueDate = ?, DueTime = ?, FK_PriorityID = ?, FK_CategoryID = ?, Status = ?
                WHERE TaskID = ?
                """;

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            setTaskFields(stmt, task);
            stmt.setString(6, task.getStatus());
            stmt.setInt(7, task.getTaskID());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Error updating task.", e);
        }
    }

    public void markTaskComplete(int taskID, int userID) {
        try (PreparedStatement stmt = getConnection().prepareStatement(
                "UPDATE Tasks SET Status = 'Completed' WHERE TaskID = ? AND FK_UserID = ?")) {
            stmt.setInt(1, taskID);
            stmt.setInt(2, userID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Error marking task as complete.", e);
        }
    }

    /**
     * Loads tasks for a single user.
     */
    public void loadTasks(ObservableList<Task> tasks, int userID) {
        String sql = """
                SELECT t.TaskID, t.Description, t.DueDate, t.DueTime, t.Status,
                       p.PriorityLevel, c.CategoryName
                FROM Tasks t
                LEFT JOIN Priorities p ON t.FK_PriorityID = p.PriorityID
                LEFT JOIN Categories c ON t.FK_CategoryID = c.CategoryID
                WHERE t.FK_UserID = ?
                ORDER BY t.DueDate, t.DueTime
                """;

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, userID);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Task task = new Task(
                            rs.getString("Description"),
                            rs.getDate("DueDate").toLocalDate(),
                            rs.getTime("DueTime") != null ? rs.getTime("DueTime").toLocalTime() : null,
                            rs.getString("PriorityLevel")
                    );
                    task.setTaskID(rs.getInt("TaskID"));
                    task.setStatus(rs.getString("Status"));
                    task.setCategory(rs.getString("CategoryName"));
                    tasks.add(task);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error loading tasks.", e);
        }
    }

    /**
     * Backward-compatible task load for tools that do not have a logged-in user.
     */
    public void loadTasks(ObservableList<Task> tasks) {
        String sql = """
                SELECT t.TaskID, t.Description, t.DueDate, t.DueTime, t.Status,
                       p.PriorityLevel, c.CategoryName
                FROM Tasks t
                LEFT JOIN Priorities p ON t.FK_PriorityID = p.PriorityID
                LEFT JOIN Categories c ON t.FK_CategoryID = c.CategoryID
                ORDER BY t.DueDate, t.DueTime
                """;

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Task task = new Task(
                        rs.getString("Description"),
                        rs.getDate("DueDate").toLocalDate(),
                        rs.getTime("DueTime") != null ? rs.getTime("DueTime").toLocalTime() : null,
                        rs.getString("PriorityLevel")
                );
                task.setTaskID(rs.getInt("TaskID"));
                task.setStatus(rs.getString("Status"));
                task.setCategory(rs.getString("CategoryName"));
                tasks.add(task);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error loading tasks.", e);
        }
    }

    /**
     * Updates a user's profile picture path by username.
     */
    public void updateProfilePicture(@NotNull String userName, String profilePicturePath) {
        try (PreparedStatement stmt = getConnection().prepareStatement(
                "UPDATE users SET ProfilePicturePath = ? WHERE UserName = ?")) {
            stmt.setString(1, profilePicturePath);
            stmt.setString(2, userName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update profile picture.", e);
        }
    }

    /**
     * Updates a user's profile picture path by user ID.
     */
    public void updateUserProfilePicture(@NotNull UserSession user) {
        try (PreparedStatement stmt = getConnection().prepareStatement(
                "UPDATE users SET ProfilePicturePath = ? WHERE UserID = ?")) {
            stmt.setString(1, user.getProfilePicturePath());
            stmt.setInt(2, user.getUserID());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update profile picture in database.", e);
        }
    }

    /**
     * Closes the database connection.
     */
    public void closeConnection() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error closing database connection", e);
            } finally {
                conn = null;
            }
        }
    }

    private boolean updatePasswordHash(String email, String passwordHash) {
        try (PreparedStatement stmt = getConnection().prepareStatement(
                "UPDATE users SET PassWord = ? WHERE LOWER(Email) = LOWER(?)")) {
            stmt.setString(1, passwordHash);
            stmt.setString(2, email);

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new IllegalArgumentException("No user found with that email.");
            }
            return true;
        } catch (SQLException e) {
            throw new IllegalStateException("Error updating password.", e);
        }
    }

    private Connection getConnection() {
        if (!configured) {
            throw new IllegalStateException("Database is not configured. Set DB_USER and DB_PASSWORD.");
        }

        try {
            if (conn == null || conn.isClosed()) {
                initializeConnection();
            }
            return conn;
        } catch (SQLException e) {
            throw new IllegalStateException("Database connection error.", e);
        }
    }

    private void ensureProfilePictureColumn(Connection connection) {
        try (ResultSet rs = connection.getMetaData().getColumns(null, null, "users", "ProfilePicturePath")) {
            if (rs.next()) {
                return;
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("ALTER TABLE users ADD COLUMN ProfilePicturePath VARCHAR(255)");
                stmt.execute("""
                        UPDATE users
                        SET ProfilePicturePath = '/edu/farmingdale/taskmanagerapp/images/profilePicture.png'
                        WHERE ProfilePicturePath IS NULL
                        """);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Unable to verify ProfilePicturePath column", e);
        }
    }

    private UserSession toUserSession(ResultSet rs) throws SQLException {
        UserSession session = new UserSession(
                rs.getString("UserName"),
                rs.getString("Email"),
                rs.getString("PassWord"),
                rs.getString("SecurityQuestion"),
                rs.getString("SecurityAnswer")
        );
        session.setUserID(rs.getInt("UserID"));

        try {
            session.setProfilePicturePath(rs.getString("ProfilePicturePath"));
        } catch (SQLException ignored) {
            session.setProfilePicturePath(null);
        }

        return session;
    }

    private void setTaskFields(PreparedStatement stmt, Task task) throws SQLException {
        stmt.setString(1, task.getDescription());
        stmt.setDate(2, Date.valueOf(task.getDueDate()));
        if (task.getDueTime() == null) {
            stmt.setNull(3, Types.TIME);
        } else {
            stmt.setTime(3, Time.valueOf(task.getDueTime()));
        }
        stmt.setInt(4, getPriorityID(task.getPriority()));
        stmt.setInt(5, getCategoryID(task.getCategory()));
    }

    private int getPriorityID(String priority) {
        try (PreparedStatement stmt = getConnection().prepareStatement(
                "SELECT PriorityID FROM Priorities WHERE LOWER(PriorityLevel) = LOWER(?)")) {
            stmt.setString(1, priority);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("PriorityID");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error getting priority ID.", e);
        }
        throw new IllegalArgumentException("Unknown priority: " + priority);
    }

    private int getCategoryID(String category) {
        try (PreparedStatement stmt = getConnection().prepareStatement(
                "SELECT CategoryID FROM Categories WHERE LOWER(CategoryName) = LOWER(?)")) {
            stmt.setString(1, category);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("CategoryID");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error getting category ID.", e);
        }

        try (PreparedStatement insertStmt = getConnection().prepareStatement(
                "INSERT INTO Categories (CategoryName) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS)) {
            insertStmt.setString(1, category);
            insertStmt.executeUpdate();
            try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error inserting category.", e);
        }

        throw new IllegalStateException("Unable to resolve category ID.");
    }

    private int getOrCreateDefaultUserID() {
        UserSession defaultUser = getAccount("DefaultUser");
        if (defaultUser != null) {
            return defaultUser.getUserID();
        }

        UserSession user = new UserSession(
                "DefaultUser",
                "default@example.com",
                "DefaultPass123",
                "Default question",
                "Default answer"
        );
        registerUser(user);
        return user.getUserID();
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Connection interrupted.", e);
        }
    }
}

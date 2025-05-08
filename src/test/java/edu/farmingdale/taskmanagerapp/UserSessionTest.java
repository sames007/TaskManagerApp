package edu.farmingdale.taskmanagerapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for validating the functionality of the UserSession class.
 * Provides unit tests to ensure correct behavior of various methods and properties
 * within the UserSession class, including username, password, email, user ID,
 * and session cleanup.
 */
class UserSessionTest {

private UserSession session;
    //Make a new session before each test

    /**
     * Initializes the test environment before each test is executed.
     * This method creates and configures a new UserSession instance with predefined
     * values for username, email, and password. Additionally, it manually sets the
     * user ID to 100 for testing purposes, as the constructor does not set the ID automatically.
     * This setup ensures that a consistent state is maintained for all test cases
     * within this test class.
     */
    @BeforeEach
    void setUp() {
        session = new UserSession("testUser", "test@email.com", "testPassword321", "What is your favorite color?", "blue");
        session.setUserID(100); // set ID manually since the constructor doesn't set id automatically
    }

    /**
     * Tests retrieval of username from UserSession.
     * Verifies that getUserName() returns the correct username set during initialization.
     */
    @Test
    void getUserName() {
        assertEquals("testUser",session.getUserName());
    }

    /**
     * Tests setting a new username in UserSession.
     * Verifies that the username is correctly updated when using setUserName().
     */
    @Test
    void setUserName() {
        session.setUserName("newUser");
        assertEquals("newUser", session.getUserName());
    }

    /**
     * Tests retrieval of password from UserSession.
     * Verifies that getPassword() returns the correct password set during initialization.
     */
    @Test
    void getPassword() {
        assertEquals("testPassword321",session.getPassword());
    }

    /**
     * Tests setting a new password in UserSession.
     * Verifies that the password is correctly updated when using setPassword().
     */
    @Test
    void setPassword() {
        session.setPassword("newTestPassword123");
        assertEquals("newTestPassword123",session.getPassword());
    }

    /**
     * Tests retrieval of email from UserSession.
     * Verifies that getEmail() returns the correct email set during initialization.
     */
    @Test
    void getEmail() {
        assertEquals("test@email.com",session.getEmail());
    }

    /**
     * Tests setting a new email in UserSession.
     * Verifies that the email is correctly updated when using setEmail().
     */
    @Test
    void setEmail() {
        session.setEmail("newTest@email.com");
        assertEquals("newTest@email.com",session.getEmail());
    }

    /**
     * Tests retrieval of user ID from UserSession.
     * Verifies that getUserID() returns the correct ID set during initialization.
     */
    @Test
    void getUserID() {
        assertEquals(100,session.getUserID());
    }

    /**
     * Tests setting a new user ID in UserSession.
     * Verifies that the user ID is correctly updated when using setUserID().
     */
    @Test
    void setUserID() {
        session.setUserID(200);
        assertEquals(200,session.getUserID());
    }

    /**
     * Tests the session cleanup functionality.
     * Verifies that cleanUserSession() properly resets all user data to default values.
     */
    @Test
    void cleanUserSession() {
        session.cleanUserSession();
        assertEquals(0,session.getUserID());
        assertEquals("",session.getUserName());
        assertEquals("",session.getPassword());
        assertEquals("",session.getEmail());
    }
}
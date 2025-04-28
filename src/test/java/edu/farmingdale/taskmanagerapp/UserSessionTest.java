package edu.farmingdale.taskmanagerapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
class UserSessionTest {
private UserSession session;

//Make a new session before each test
    @BeforeEach
    void setUp() {
        session = new UserSession("testUser","test@email.com","testPassword321");
        session.setUserID(100); // set ID manually since the constructor doesn't set id automatically
    }

    @Test
    void getUserName() {
        assertEquals("testUser",session.getUserName());
    }

    @Test
    void setUserName() {
        session.setUserName("newUser");
        assertEquals("newUser", session.getUserName());
    }

    @Test
    void getPassword() {
        assertEquals("testPassword321",session.getPassword());
    }

    @Test
    void setPassword() {
        session.setPassword("newTestPassword123");
        assertEquals("newTestPassword123",session.getPassword());
    }

    @Test
    void getEmail() {
        assertEquals("test@email.com",session.getEmail());
    }

    @Test
    void setEmail() {
        session.setEmail("newTest@email.com");
        assertEquals("newTest@email.com",session.getEmail());
    }

    @Test
    void getUserID() {
        assertEquals(100,session.getUserID());
    }

    @Test
    void setUserID() {
        session.setUserID(200);
        assertEquals(200,session.getUserID());
    }

    @Test
    void cleanUserSession() {
        session.cleanUserSession();
        assertEquals(0,session.getUserID());
        assertEquals("",session.getUserName());
        assertEquals("",session.getPassword());
        assertEquals("",session.getEmail());
    }
}
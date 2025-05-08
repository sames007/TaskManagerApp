package edu.farmingdale.taskmanagerapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for validating the functionality of the Task class.
 * Provides unit tests to ensure correct behavior of various methods and properties within the Task class,
 * such as checking overdue conditions and verifying field values.
 */
class TaskTest {

    private Task task;
    private LocalDate today;
    private LocalTime now;

    /**
     * Initializes the test environment before each test is executed.
     * This method sets up the required objects and assigns initial values
     * to the necessary variables for testing Task-related functionality.
     * - Initializes the `today` variable with the current date.
     * - Initializes the `now` variable with the current time.
     * - Creates a new `Task` instance with predefined default values, including
     *   description, due date, due time, and priority level set to "High".
     */
    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        now = LocalTime.now();
        task = new Task("Test Description",today,now,"High");
    }

    /**
     * Verifies the fields that are set
     */
    @Test
    void testFields(){
        assertEquals("Test Description",task.getDescription());
        assertEquals(today,task.getDueDate());
        assertEquals(now,task.getDueTime());
        assertEquals("High",task.getPriority());
        assertEquals("Pending",task.getStatus());
    }

    /**
     * Checks if a task is overdue.
     * Returns true if the date and time are past the
     * due date and time, otherwise false
     */
    @Test
    void overduePastDate() {
        //Tasks with a past date should be overdue
        Task pastDate = new Task("Past Task", today.minusDays(1), now, "Medium");
        assertTrue(pastDate.isOverdue());
    }

    /**
     * Verifies that a task with a due date of today and a due time in the past is marked as overdue.
     */
    @Test
        void overdueTodayPastTime() {
        //Tasks that are due today but the time's already passed should be overdue
        Task pastTime = new Task("Due Today, Past Time", today, now.minusHours(1), "Low");
        assertTrue(pastTime.isOverdue());
    }

    /**
     * Verifies that a task with a future date is not marked as overdue.
     */
    @Test
    void overdueFutureDate() {
        //Task with a future date should not be overdue
        Task futureDate = new Task("Future Task", today.plusDays(1), now, "High");
        assertFalse(futureDate.isOverdue());
    }

    /**
     * Verifies that a task with a due date of today and a due time in the future is not marked as overdue.
     * This test ensures the correct functionality of the `isOverdue` method when the due time
     * has not yet passed on the current day.
     */
    @Test
    void overdueTodayFutureTime() {
        //Task that is due today but the time hasn't passed should not be overdue
        Task futureTime = new Task("Due Today, Future Time",today,now.plusHours(1),"Medium");
        assertFalse(futureTime.isOverdue());
    }
}
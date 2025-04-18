package edu.farmingdale.taskmanagerapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class TaskTest {

    private Task task;
    private LocalDate today;
    private LocalTime now;

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
     * Checks if task is overdue.
     * returns true if the date and time are past the
     * due date and time, otherwise false
     */
    @Test
    void overduePastDate() {
        //Tasks with a past date should be overdue
        Task pastDate = new Task("Past Task", today.minusDays(1), now, "Medium");
        assertTrue(pastDate.isOverdue());
    }

    @Test
        void overdueTodayPastTime() {
        //Tasks that are due today but the time's already passed should be overdue
        Task pastTime = new Task("Due Today, Past Time", today, now.minusHours(1), "Low");
        assertTrue(pastTime.isOverdue());
    }

    @Test
    void overdueFutureDate() {
        //Task with a future date should not be overdue
        Task futureDate = new Task("Future Task", today.plusDays(1), now, "High");
        assertFalse(futureDate.isOverdue());
    }

    @Test
    void overdueTodayFutureTime() {
        //Task that are due today but the time hasn't passed should not be overdue
        Task futureTime = new Task("Due Today, Future Time",today,now.plusHours(1),"Medium");
        assertFalse(futureTime.isOverdue());
    }
}
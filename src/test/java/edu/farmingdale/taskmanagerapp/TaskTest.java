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
        task = new Task("Test Description", today, now, "High");
    }

    @Test
    void testFields() {
        assertEquals("Test Description", task.getDescription());
        assertEquals(today, task.getDueDate());
        assertEquals(now, task.getDueTime());
        assertEquals("High", task.getPriority());
        assertEquals("Pending", task.getStatus());
    }

    @Test
    void overduePastDate() {
        Task pastDate = new Task("Past Task", today.minusDays(1), now, "Medium");

        assertTrue(pastDate.isOverdue());
    }

    @Test
    void overdueTodayPastTime() {
        Task pastTime = new Task("Due Today, Past Time", today, now.minusHours(1), "Low");

        assertTrue(pastTime.isOverdue());
    }

    @Test
    void overdueFutureDate() {
        Task futureDate = new Task("Future Task", today.plusDays(1), now, "High");

        assertFalse(futureDate.isOverdue());
    }

    @Test
    void overdueTodayFutureTime() {
        Task futureTime = new Task("Due Today, Future Time", today, now.plusHours(1), "Medium");

        assertFalse(futureTime.isOverdue());
    }
}

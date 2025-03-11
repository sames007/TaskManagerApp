package edu.farmingdale.taskmanagerapp;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Simple model class representing a Task.
 */
public class Task {
    private String description;
    private LocalDate dueDate;
    private LocalTime dueTime; // Stores time in 24-hour format
    private String priority;
    private String status;
    private String category;
    private LocalDate reminder;

    /**
     * Constructs a new Task with description, due date, and priority.
     * Status is set to "Pending" by default.
     */
    public Task(String description, LocalDate dueDate, String priority) {
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.status = "Pending";
    }

    // Getters and setters for each property

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public LocalDate getDueDate() {
        return dueDate;
    }
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
    public LocalTime getDueTime() {
        return dueTime;
    }
    public void setDueTime(LocalTime dueTime) {
        this.dueTime = dueTime;
    }
    public String getPriority() {
        return priority;
    }
    public void setPriority(String priority) {
        this.priority = priority;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public LocalDate getReminder() {
        return reminder;
    }
    public void setReminder(LocalDate reminder) {
        this.reminder = reminder;
    }
}

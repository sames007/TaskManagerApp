package edu.farmingdale.taskmanagerapp;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Simple model class representing a Task.
 */
public class Task {
    private int taskID;
    private String description;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private String priority;
    private String status;
    private String category;
    private LocalDate reminder;

    // Constructor for a new task with a default status "Pending"
    /**
     * Constructor for a new task with a default status "Pending".
     * @param description The description or title of the task
     * @param dueDate The date when the task is due to be completed
     * @param dueTime The time when the task is due to be completed
     * @param priority The priority level of the task (Extreme, High, Medium, Low)
     */
    public Task(String description, LocalDate dueDate, LocalTime dueTime, String priority) {
        this.description = description;
        this.dueDate = dueDate;
        this.dueTime = dueTime;
        this.priority = priority;
        this.status = "Pending";
    }

    // Getters and setters
    /**
     * Gets the task ID.
     * @return the task ID
     */
    public int getTaskID() {
        return taskID;
    }

    /**
     * Sets the task ID.
     * @param taskID the task ID to set
     */
    public void setTaskID(int taskID) {
        this.taskID = taskID;
    }

    /**
     * Gets the task description.
     * @return the task description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the task description.
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the due date.
     * @return the due date
     */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /**
     * Sets the due date.
     * @param dueDate the due date to set
     */
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * Gets the due time.
     * @return the due time
     */
    public LocalTime getDueTime() {
        return dueTime;
    }

    /**
     * Sets the due time.
     * @param dueTime the due time to set
     */
    public void setDueTime(LocalTime dueTime) {
        this.dueTime = dueTime;
    }

    /**
     * Gets the priority level.
     * @return the priority level
     */
    public String getPriority() {
        return priority;
    }

    /**
     * Sets the priority level.
     * @param priority the priority level to set
     */
    public void setPriority(String priority) {
        this.priority = priority;
    }

    /**
     * Gets the task status.
     * @return the task status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the task status.
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the task category.
     * @return the task category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the task category.
     * @param category the category to set
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Gets the reminder date.
     * @return the reminder date
     */
    public LocalDate getReminder() {
        return reminder;
    }

    /**
     * Sets the reminder date.
     * @param reminder the reminder date to set
     */
    public void setReminder(LocalDate reminder) {
        this.reminder = reminder;
    }

    /**
     * Checks if a task is overdue.
     * @return true if the date and time are past the
     * due date and time, otherwise false
      */
    public boolean isOverdue() {
    LocalDate today = LocalDate.now();
    LocalTime now = LocalTime.now();

        if (dueDate == null) {
            return false;
        }

    if (dueDate.isBefore(today)) {
        return true; // Past due date
        } else if (dueDate.isEqual(today) && dueTime != null && dueTime.isBefore(now)) {
        return true; // Due today but time's already passed
    }
    return false; // Not overdue
    }

    /**
     * Validates if the due date is in the future
     * @return true if the date is valid (in the future), false otherwise
     */
    public boolean isValidDueDate() {
        if (dueDate == null) {
            return false;
        }
        return !dueDate.isBefore(LocalDate.now());
    }
}

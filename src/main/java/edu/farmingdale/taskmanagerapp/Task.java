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
    public Task(String description, LocalDate dueDate, LocalTime dueTime, String priority) {
        this.description = description;
        this.dueDate = dueDate;
        this.dueTime = dueTime;
        this.priority = priority;
        this.status = "Pending";
    }

    // Getters and setters
    public int getTaskID() {
        return taskID;
    }
    public void setTaskID(int taskID) {
        this.taskID = taskID;
    }
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

    /**
     * Checks if task is overdue.
     * @return true if the date and time are past the
     * due date and time, otherwise false
      */
    public boolean isOverdue(){
    LocalDate today = LocalDate.now();
    LocalTime now = LocalTime.now();

    if (dueDate.isBefore(today)) {
        return true; // Past due date
    } else if (dueDate.isEqual(today) && dueTime.isBefore(now)){
        return true; // Due today but time's already passed
    }
    return false; // Not overdue
    }
}

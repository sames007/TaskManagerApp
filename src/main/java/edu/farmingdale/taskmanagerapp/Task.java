package edu.farmingdale.taskmanagerapp;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * A comprehensive Task class representing a task in the Task Manager application.
 * This class provides functionality for managing task details including description,
 * due date, priority, status, and overdue status. Enhanced with features for
 * better user engagement and daily activity management.
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
    private Duration estimatedDuration;
    private String notes;
    private boolean isRecurring;
    private String recurringPattern;
    private int completionPercentage;
    private LocalDate lastModified;

    // Valid priority levels with visual indicators
    public static final String PRIORITY_LOW = "Low ⚪";
    public static final String PRIORITY_MEDIUM = "Medium 🟡";
    public static final String PRIORITY_HIGH = "High 🟠";
    public static final String PRIORITY_URGENT = "Urgent 🔴";

    // Valid status values with visual indicators
    public static final String STATUS_PENDING = "Pending ⏳";
    public static final String STATUS_IN_PROGRESS = "In Progress 🔄";
    public static final String STATUS_COMPLETED = "Completed ✅";
    public static final String STATUS_CANCELLED = "Cancelled ❌";

    // Common task categories
    public static final String CATEGORY_WORK = "Work 💼";
    public static final String CATEGORY_PERSONAL = "Personal 👤";
    public static final String CATEGORY_SHOPPING = "Shopping 🛍️";
    public static final String CATEGORY_HEALTH = "Health 🏥";
    public static final String CATEGORY_EDUCATION = "Education 📚";
    public static final String CATEGORY_ENTERTAINMENT = "Entertainment 🎮";
    public static final String CATEGORY_OTHER = "Other 📌";

    /**
     * Constructor for a new task with default status "Pending"
     * @param description The task description
     * @param dueDate The due date of the task
     * @param dueTime The due time of the task
     * @param priority The priority level of the task
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public Task(String description, LocalDate dueDate, LocalTime dueTime, String priority) {
        setDescription(description);
        setDueDate(dueDate);
        setDueTime(dueTime);
        setPriority(priority);
        this.status = STATUS_PENDING;
        this.completionPercentage = 0;
        this.lastModified = LocalDate.now();
    }

    /**
     * Gets the task description
     * @return The task description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the task description
     * @param description The new task description
     * @throws IllegalArgumentException if description is null or empty
     */
    public void setDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        this.description = description.trim();
    }

    /**
     * Gets the due date of the task
     * @return The due date
     */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /**
     * Sets the due date of the task
     * @param dueDate The new due date
     * @throws IllegalArgumentException if dueDate is null
     */
    public void setDueDate(LocalDate dueDate) {
        if (dueDate == null) {
            throw new IllegalArgumentException("Due date cannot be null");
        }
        this.dueDate = dueDate;
    }

    /**
     * Gets the due time of the task
     * @return The due time
     */
    public LocalTime getDueTime() {
        return dueTime;
    }

    /**
     * Sets the due time of the task
     * @param dueTime The new due time
     */
    public void setDueTime(LocalTime dueTime) {
        this.dueTime = dueTime;
    }

    /**
     * Gets the task ID
     * @return The task ID
     */
    public int getTaskID() {
        return taskID;
    }

    /**
     * Sets the task ID
     * @param taskID The new task ID
     */
    public void setTaskID(int taskID) {
        this.taskID = taskID;
    }

    /**
     * Gets the category of the task
     * @return The task category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the category of the task
     * @param category The new category
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Gets the reminder date of the task
     * @return The reminder date
     */
    public LocalDate getReminder() {
        return reminder;
    }

    /**
     * Sets the reminder date of the task
     * @param reminder The new reminder date
     */
    public void setReminder(LocalDate reminder) {
        this.reminder = reminder;
    }

    /**
     * Gets the priority level of the task
     * @return The priority level
     */
    public String getPriority() {
        return priority;
    }

    /**
     * Sets the priority level of the task
     * @param priority The new priority level
     * @throws IllegalArgumentException if priority is not a valid priority level
     */
    public void setPriority(String priority) {
        if (priority == null || !isValidPriority(priority)) {
            throw new IllegalArgumentException("Invalid priority level. Must be one of: High, Medium, Low");
        }
        // Convert plain text priority to emoji version if needed
        switch (priority.replaceAll("[^a-zA-Z]", "").toLowerCase()) {
            case "high":
                this.priority = PRIORITY_HIGH;
                break;
            case "medium":
                this.priority = PRIORITY_MEDIUM;
                break;
            case "low":
                this.priority = PRIORITY_LOW;
                break;
            default:
                this.priority = priority;
        }
    }

    /**
     * Checks if a given priority level is valid
     * @param priority The priority level to check
     * @return true if the priority is valid, false otherwise
     */
    private boolean isValidPriority(String priority) {
        if (priority == null) return false;
        // Remove any emojis and whitespace for comparison
        String cleanPriority = priority.replaceAll("[^a-zA-Z]", "").toLowerCase();
        return cleanPriority.equals("high") ||
               cleanPriority.equals("medium") ||
               cleanPriority.equals("low") ||
               priority.equals(PRIORITY_LOW) ||
               priority.equals(PRIORITY_MEDIUM) ||
               priority.equals(PRIORITY_HIGH) ||
               priority.equals(PRIORITY_URGENT);
    }

    /**
     * Gets the current status of the task
     * @return The task status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status of the task
     * @param status The new status
     * @throws IllegalArgumentException if status is not a valid status value
     */
    public void setStatus(String status) {
        if (status == null || !isValidStatus(status)) {
            throw new IllegalArgumentException("Invalid status. Must be one of: " +
                    STATUS_PENDING + ", " + STATUS_IN_PROGRESS + ", " + STATUS_COMPLETED + ", " + STATUS_CANCELLED);
        }
        this.status = status;
    }

    /**
     * Checks if the task is overdue
     * @return true if the task is overdue, false otherwise
     */
    public boolean isOverdue() {
        if (status.equals(STATUS_COMPLETED) || status.equals(STATUS_CANCELLED)) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        if (dueDate.isBefore(today)) {
            return true;
        }
        
        if (dueDate.equals(today) && dueTime != null) {
            return dueTime.isBefore(LocalTime.now());
        }
        
        return false;
    }

    /**
     * Checks if a given status is valid
     * @param status The status to check
     * @return true if the status is valid, false otherwise
     */
    private boolean isValidStatus(String status) {
        return status.equals(STATUS_PENDING) ||
               status.equals(STATUS_IN_PROGRESS) ||
               status.equals(STATUS_COMPLETED) ||
               status.equals(STATUS_CANCELLED);
    }

    /**
     * Gets the estimated duration of the task
     * @return The estimated duration
     */
    public Duration getEstimatedDuration() {
        return estimatedDuration;
    }

    /**
     * Sets the estimated duration of the task
     * @param estimatedDuration The new estimated duration
     */
    public void setEstimatedDuration(Duration estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    /**
     * Gets the notes associated with the task
     * @return The task notes
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Sets the notes for the task
     * @param notes The new notes
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Checks if the task is recurring
     * @return true if the task is recurring
     */
    public boolean isRecurring() {
        return isRecurring;
    }

    /**
     * Sets whether the task is recurring
     * @param recurring true if the task should be recurring
     */
    public void setRecurring(boolean recurring) {
        isRecurring = recurring;
    }

    /**
     * Gets the recurring pattern of the task
     * @return The recurring pattern
     */
    public String getRecurringPattern() {
        return recurringPattern;
    }

    /**
     * Sets the recurring pattern for the task
     * @param recurringPattern The new recurring pattern
     */
    public void setRecurringPattern(String recurringPattern) {
        this.recurringPattern = recurringPattern;
    }

    /**
     * Gets the completion percentage of the task
     * @return The completion percentage (0-100)
     */
    public int getCompletionPercentage() {
        return completionPercentage;
    }

    /**
     * Sets the completion percentage of the task
     * @param completionPercentage The new completion percentage (0-100)
     * @throws IllegalArgumentException if percentage is not between 0 and 100
     */
    public void setCompletionPercentage(int completionPercentage) {
        if (completionPercentage < 0 || completionPercentage > 100) {
            throw new IllegalArgumentException("Completion percentage must be between 0 and 100");
        }
        this.completionPercentage = completionPercentage;
        this.lastModified = LocalDate.now();
    }

    /**
     * Gets the last modification date of the task
     * @return The last modification date
     */
    public LocalDate getLastModified() {
        return lastModified;
    }

    /**
     * Updates the task's progress
     * @param percentage The new completion percentage
     * @param notes Optional notes about the progress
     */
    public void updateProgress(int percentage, String notes) {
        setCompletionPercentage(percentage);
        if (notes != null && !notes.trim().isEmpty()) {
            this.notes = (this.notes != null ? this.notes + "\n" : "") + 
                        LocalDate.now() + ": " + notes;
        }
        if (percentage == 100) {
            setStatus(STATUS_COMPLETED);
        } else if (percentage > 0) {
            setStatus(STATUS_IN_PROGRESS);
        }
    }

    /**
     * Gets the time remaining until the task is due
     * @return The time remaining as a Duration
     */
    public Duration getTimeRemaining() {
        if (status.equals(STATUS_COMPLETED) || status.equals(STATUS_CANCELLED)) {
            return Duration.ZERO;
        }

        LocalDate today = LocalDate.now();
        if (dueDate.isBefore(today)) {
            return Duration.ZERO;
        }

        if (dueDate.equals(today)) {
            if (dueTime != null) {
                Duration remaining = Duration.between(LocalTime.now(), dueTime);
                return remaining.isNegative() ? Duration.ZERO : remaining;
            }
            return Duration.ofDays(1);
        }

        return Duration.ofDays(ChronoUnit.DAYS.between(today, dueDate));
    }

    /**
     * Gets a formatted string representation of the time remaining
     * @return A human-readable string of the time remaining
     */
    public String getTimeRemainingFormatted() {
        Duration remaining = getTimeRemaining();
        if (remaining.isZero()) {
            return "Due now";
        }

        long days = remaining.toDays();
        long hours = remaining.toHoursPart();
        long minutes = remaining.toMinutesPart();

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m");
        
        return sb.length() > 0 ? sb.toString().trim() + " remaining" : "Less than a minute remaining";
    }

    /**
     * Gets a visual progress bar representation of the task's completion
     * @return A string containing a visual progress bar
     */
    public String getProgressBar() {
        int width = 20;
        int filled = (int) ((completionPercentage * width) / 100);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < width; i++) {
            bar.append(i < filled ? "█" : "░");
        }
        bar.append("] ").append(completionPercentage).append("%");
        return bar.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return taskID == task.taskID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskID);
    }

    @Override
    public String toString() {
        return String.format("Task: %s\nPriority: %s\nStatus: %s\nDue: %s %s\nCategory: %s\nProgress: %s\nTime Remaining: %s",
                description,
                priority,
                status,
                dueDate,
                dueTime != null ? dueTime : "",
                category != null ? category : "Uncategorized",
                getProgressBar(),
                getTimeRemainingFormatted());
    }
}

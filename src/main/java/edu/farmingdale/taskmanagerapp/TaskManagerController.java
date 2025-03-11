package edu.farmingdale.taskmanagerapp;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Controller for managing tasks. It handles adding, editing,
 * marking tasks complete, and deleting tasks.
 */
public class TaskManagerController {
    // UI controls from the FXML file
    @FXML private TextField taskInput;
    @FXML private DatePicker dueDatePicker;
    @FXML private Spinner<Integer> hourSpinner;       // Spinner for hour (1-12)
    @FXML private Spinner<Integer> minuteSpinner;       // Spinner for minute (0-59)
    @FXML private ComboBox<String> amPmComboBox;          // ComboBox for AM/PM selection
    @FXML private ComboBox<String> priorityComboBox;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private DatePicker reminderDatePicker;
    @FXML private TableView<Task> taskTable;
    @FXML private TableColumn<Task, String> taskColumn;
    @FXML private TableColumn<Task, LocalDate> dueDateColumn;
    @FXML private TableColumn<Task, LocalTime> timeColumn; // Column to display task time
    @FXML private TableColumn<Task, String> priorityColumn;
    @FXML private TableColumn<Task, String> statusColumn;

    // Observable list to hold tasks for the TableView
    private ObservableList<Task> tasks = FXCollections.observableArrayList();

    /**
     * Initializes the controller after the FXML file is loaded.
     * Sets up the ComboBoxes, TableView columns, and Spinners.
     */
    @FXML
    public void initialize() {
        // Set up options for priority and category ComboBoxes
        priorityComboBox.setItems(FXCollections.observableArrayList("High", "Medium", "Low"));
        categoryComboBox.setItems(FXCollections.observableArrayList("School", "Work", "Personal"));

        // Bind table columns to the properties of Task objects
        taskColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("dueTime"));
        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        taskTable.setItems(tasks);

        // Set up hour spinner (1-12) and make it editable
        SpinnerValueFactory<Integer> hourFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, 12);
        hourSpinner.setValueFactory(hourFactory);
        hourSpinner.setEditable(true);

        // Set up minute spinner (0-59) and make it editable
        SpinnerValueFactory<Integer> minuteFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0);
        minuteSpinner.setValueFactory(minuteFactory);
        minuteSpinner.setEditable(true);

        // Set up AM/PM ComboBox with default value "AM"
        amPmComboBox.setItems(FXCollections.observableArrayList("AM", "PM"));
        amPmComboBox.setValue("AM");
    }

    /**
     * Called when the user clicks the "Add Task" button or presses Enter.
     * Validates input fields, converts time, and adds a new task.
     */
    @FXML
    private void addTask() {
        // Get and trim the input values
        String description = taskInput.getText().trim();
        LocalDate dueDate = dueDatePicker.getValue();
        Integer hour = hourSpinner.getValue();
        Integer minute = minuteSpinner.getValue();
        String amPm = amPmComboBox.getValue();
        String priority = priorityComboBox.getValue();
        String category = categoryComboBox.getValue();
        LocalDate reminder = reminderDatePicker.getValue();

        // Check if any required field is missing
        if (description.isEmpty() || dueDate == null || hour == null || minute == null || amPm == null || priority == null || category == null) {
            showAlert("Please fill in all required fields (Task, Due Date, Time, Priority, Category)");
            return;
        }
        // Ensure the due date is in the future
        if (!dueDate.isAfter(LocalDate.now())) {
            showAlert("Due date must be in the future.");
            return;
        }

        // Convert 12-hour time to 24-hour format
        int convertedHour = hour;
        if ("PM".equals(amPm) && hour != 12) {
            convertedHour += 12;
        } else if ("AM".equals(amPm) && hour == 12) {
            convertedHour = 0;
        }
        LocalTime dueTime = LocalTime.of(convertedHour, minute);

        // Create a new Task with the provided information
        Task task = new Task(description, dueDate, priority);
        task.setDueTime(dueTime);
        task.setCategory(category);
        task.setReminder(reminder);
        tasks.add(task);

        // Clear input fields after adding the task
        clearInputs();
    }

    /**
     * Called when the user clicks the "Edit Task" button.
     * Loads the selected task's data into the input fields for editing.
     */
    @FXML
    private void editTask() {
        Task selectedTask = taskTable.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            // Preload task data into input fields
            taskInput.setText(selectedTask.getDescription());
            dueDatePicker.setValue(selectedTask.getDueDate());

            // Convert the stored 24-hour time to 12-hour format for display
            LocalTime time = selectedTask.getDueTime();
            int hour24 = time.getHour();
            int displayHour = (hour24 == 0 || hour24 == 12) ? 12 : hour24 % 12;
            String amPm = (hour24 < 12) ? "AM" : "PM";

            hourSpinner.getValueFactory().setValue(displayHour);
            minuteSpinner.getValueFactory().setValue(time.getMinute());
            amPmComboBox.setValue(amPm);

            priorityComboBox.setValue(selectedTask.getPriority());
            categoryComboBox.setValue(selectedTask.getCategory());
            reminderDatePicker.setValue(selectedTask.getReminder());

            // Remove the task to be replaced by the updated version
            tasks.remove(selectedTask);
        } else {
            showAlert("Please select a task to edit");
        }
    }

    /**
     * Called when the user clicks the "Mark Complete" button.
     * Marks the selected task as completed.
     */
    @FXML
    private void markTaskComplete() {
        Task selectedTask = taskTable.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            selectedTask.setStatus("Completed");
            taskTable.refresh(); // Refresh the table to update the status display
        } else {
            showAlert("Please select a task to mark as complete");
        }
    }

    /**
     * Called when the user clicks the "Delete Task" button.
     * Removes the selected task from the list.
     */
    @FXML
    private void deleteTask() {
        Task selectedTask = taskTable.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            tasks.remove(selectedTask);
        } else {
            showAlert("Please select a task to delete");
        }
    }

    /**
     * Clears all input fields and resets them to default values.
     */
    private void clearInputs() {
        taskInput.clear();
        dueDatePicker.setValue(null);
        // Reset time inputs to defaults
        hourSpinner.getValueFactory().setValue(12);
        minuteSpinner.getValueFactory().setValue(0);
        amPmComboBox.setValue("AM");
        priorityComboBox.setValue(null);
        categoryComboBox.setValue(null);
        reminderDatePicker.setValue(null);
    }

    /**
     * Displays an information alert with the provided message.
     * @param message the message to display in the alert
     */
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Adds an imported task (from a file) to the list and refreshes the table.
     * @param task the task to add
     */
    public void addImportedTask(Task task) {
        tasks.add(task);
        taskTable.refresh();
    }
}

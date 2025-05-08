package edu.farmingdale.taskmanagerapp;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Controller for the Add Task dialog.
 */
public class AddTaskController {

    @FXML
    private TextField taskInput;
    @FXML
    private DatePicker dueDatePicker;
    @FXML
    private Spinner<Integer> hourSpinner;
    @FXML
    private Spinner<Integer> minuteSpinner;
    @FXML
    private ComboBox<String> amPmComboBox;
    @FXML
    private ComboBox<String> priorityComboBox;
    @FXML
    private ComboBox<String> categoryComboBox;
    @FXML
    private DatePicker reminderDatePicker;
    @FXML
    private Button submitTaskButton; // Reference to the button

    // Reference to the main controller
    private TaskManagerController mainController; // Reference to the main controller
    private Task taskToEdit = null;
    private boolean editMode = false;

    // Setter to inject the main controller
    /**
     * @param mainController the main controller to set
     */
    public void setMainController(TaskManagerController mainController) {
        this.mainController = mainController;
    }

    /**
     * @param task the task to edit
     */
    public void setTaskToEdit(Task task) {
        this.taskToEdit = task;
        this.editMode = (task != null);

        if (editMode) {
            populateFields();
            submitTaskButton.setText("Update Task");
        } else {
            clearFields();
            submitTaskButton.setText("Add Task");
        }
    }

    /**
     * Populates the fields for adding tasks
     */
    private void populateFields() {
        if (taskToEdit == null) {
            return;
        }

        taskInput.setText(taskToEdit.getDescription());
        dueDatePicker.setValue(taskToEdit.getDueDate());

        LocalTime dueTime = taskToEdit.getDueTime();

        if (dueTime != null) {
            int hour = dueTime.getHour();
            int minute = dueTime.getMinute();
            String amPm = "AM";

            if (hour == 0) {
                hour = 12;
                amPm = "AM";
            } else if (hour == 12) {
                amPm = "PM";
            } else if (hour > 12) {
                hour = hour - 12;
                amPm = "PM";
            }

            hourSpinner.getValueFactory().setValue(hour);

            minuteSpinner.getValueFactory().setValue(minute);
            amPmComboBox.setValue(amPm);

        } else {
            hourSpinner.getValueFactory().setValue(12);
            minuteSpinner.getValueFactory().setValue(0);
            amPmComboBox.setValue("AM");
        }
        priorityComboBox.setValue(taskToEdit.getPriority());
        categoryComboBox.setValue(taskToEdit.getCategory());
        reminderDatePicker.setValue(taskToEdit.getReminder());
    }

    /**
     * Initializes the controller after the FXML file is loaded.
     */
    @FXML
    public void initialize() {
        // Initialize ComboBoxes with their items
        priorityComboBox.setItems(FXCollections.observableArrayList("Extreme", "High", "Medium", "Low"));
        categoryComboBox.setItems(FXCollections.observableArrayList("School", "Work", "Personal", "Family", "Other"));

        // Set up ComboBox selection listeners
        priorityComboBox.setOnAction(event -> {
            String selectedPriority = priorityComboBox.getValue();
            if (selectedPriority != null) {
                // Keep the current category selection
                String currentCategory = categoryComboBox.getValue();
                categoryComboBox.setValue(currentCategory);
            }
        });

        categoryComboBox.setOnAction(event -> {
            String selectedCategory = categoryComboBox.getValue();
            if (selectedCategory != null) {
                // Keep the current priority selection
                String currentPriority = priorityComboBox.getValue();
                priorityComboBox.setValue(currentPriority);
            }
        });

        // Initialize time spinners
        SpinnerValueFactory<Integer> hourFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, 12);
        hourSpinner.setValueFactory(hourFactory);
        hourSpinner.setEditable(true);

        SpinnerValueFactory<Integer> minuteFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0);
        minuteSpinner.setValueFactory(minuteFactory);
        minuteSpinner.setEditable(true);
        minuteSpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            minuteSpinner.getEditor().setText(String.format("%02d", newValue));
        });

        // Initialize AM/PM ComboBox
        amPmComboBox.setItems(FXCollections.observableArrayList("AM", "PM"));
        amPmComboBox.setValue("AM");

        // Set the initial button text
        submitTaskButton.setText("Add Task");

        // Add validation listeners
        addValidationListeners();
    }

    /**
     * Adds listeners to update the button state based on required fields.
     */
    private void addValidationListeners() {
        taskInput.textProperty().addListener((obs, oldVal, newVal) -> validateFields());
        dueDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> validateFields());
        priorityComboBox.valueProperty().addListener((obs, oldVal, newVal) -> validateFields());
        categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> validateFields());
    }

    /**
     * Validates Fields To Make Sure All Required Fields Are Filled
     */
    private void validateFields() {
        boolean isValid = !taskInput.getText().trim().isEmpty() &&
                         dueDatePicker.getValue() != null &&
                         priorityComboBox.getValue() != null &&
                         categoryComboBox.getValue() != null;
        submitTaskButton.setDisable(!isValid);
    }

    /**
     * Clears all fields in the dialog.
     */
    private void clearFields() {
        taskInput.clear();
        dueDatePicker.setValue(null);
        hourSpinner.getValueFactory().setValue(12);
        minuteSpinner.getValueFactory().setValue(0);
        amPmComboBox.setValue("AM");
        priorityComboBox.setValue(null);
        categoryComboBox.setValue(null);
        reminderDatePicker.setValue(null);
    }

    /**
     * Called when the user clicks the "Submit Task" button.
     */
    @FXML
    private void submitTask() {
        // --- Validation (similar to original addTask) ---
        String description = taskInput.getText().trim();
        LocalDate dueDate = dueDatePicker.getValue();
        Integer hour = hourSpinner.getValue();
        Integer minute = minuteSpinner.getValue();
        String amPm = amPmComboBox.getValue();
        String priority = priorityComboBox.getValue();
        String category = categoryComboBox.getValue();
        LocalDate reminder = reminderDatePicker.getValue(); // Optional

        if (!isValid(description, dueDate, hour, minute, amPm, priority, category)) {
            return; // showAlert is called within isValidInput
        }
        // Convert time using the helper method from the main controller
        LocalTime dueTime = mainController.convertToLocalTime(hour, minute, amPm);
        if (dueTime == null) {
            mainController.showAlert("Invalid Time"); // Handle potential null from conversion
            return;
        }
        if (editMode && taskToEdit != null) {
            taskToEdit.setDescription(description);
            taskToEdit.setDueDate(dueDate);
            taskToEdit.setDueTime(dueTime);
            taskToEdit.setPriority(priority);
            taskToEdit.setCategory(category);
            taskToEdit.setReminder(reminder);

            mainController.updateTaskFrom(taskToEdit);
        } else {
            Task newTask = new Task(description, dueDate, dueTime, priority);
            newTask.setCategory(category);
            newTask.setReminder(reminder);
            newTask.setStatus("In Progress");

            mainController.addNewTaskFrom(newTask);
        }
        closeDialog();
    }

    /**
     * Validates the task input fields to ensure all required data is present and valid.
     * @param description the task description text
     * @param dueDate the due date for the task
     * @param hour the hour component of the due time (1-12)
     * @param minute the minute component of the due time (0-59)
     * @param amPm the AM/PM indicator for the due time
     * @param priority the priority level of the task
     * @param category the category the task belongs to
     * @return true if all inputs are valid, false otherwise
     */
    private boolean isValid(@NotNull String description, LocalDate dueDate, Integer hour, Integer minute,
                            String amPm, String priority, String category) {
        if (description.isEmpty() || dueDate == null || hour == null || minute == null
           || amPm == null || priority == null || category == null) {
            mainController.showAlert("Please Fill All Required Fields (Description, Due Date, Time, Priority, Category)");
            return false;
        }
        if (dueDate.isBefore(LocalDate.now())) {
            mainController.showAlert("Due Date Cannot Be In The Past.");
            return false;
        }

        return true;
    }

    // Helper to close the dialog window
    /**
     * Closes the dialog window.
     */
    private void closeDialog() {
        Stage stage = (Stage) submitTaskButton.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}
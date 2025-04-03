package edu.farmingdale.taskmanagerapp;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import jfxtras.scene.control.agenda.Agenda;
import jfxtras.scene.control.agenda.Agenda.AppointmentImplLocal;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @FXML private MenuItem createNewAccount, loginToExisting;


    // Container for the Agenda control (set in FXML)
    @FXML private VBox agendaContainer;

    // Rename JFXtras Agenda control to "agenda"
    private Agenda agenda;

    // Observable list to hold tasks for the TableView
    private ObservableList<Task> tasks = FXCollections.observableArrayList();

    private DatabaseManager dbManager;

    // Field to hold the task that is currently being edited (if any)
    private Task currentEditingTask = null;

    /**
     * Setter for DatabaseManager instance.
     * @param dbManager the DatabaseManager to use for DB operations.
     */
    public void setDatabaseManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Initializes the controller after the FXML file is loaded.
     * Sets up the ComboBoxes, TableView columns, Spinners, and agenda.
     */
    @FXML
    public void initialize() {
        // Set up options for priority and category ComboBoxes with emojis
        priorityComboBox.setItems(FXCollections.observableArrayList(
            "High 🔴", "Medium 🟡", "Low ⚪"
        ));
        categoryComboBox.setItems(FXCollections.observableArrayList(
            "Work 💼", "Personal 👤", "Shopping 🛍️", "Health 🏥", 
            "Education 📚", "Entertainment 🎮", "Other 📌"
        ));

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

        // Set up the TableView columns with property mappings and custom cell factories
        taskColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("dueTime"));
        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Add custom cell factories for better visual presentation
        priorityColumn.setCellFactory(column -> new TableCell<Task, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Set background color based on priority
                    if (item.contains("🔴")) {
                        setStyle("-fx-background-color: #ffebee;");
                    } else if (item.contains("🟡")) {
                        setStyle("-fx-background-color: #fff3e0;");
                    } else if (item.contains("⚪")) {
                        setStyle("-fx-background-color: #e8f5e9;");
                    }
                }
            }
        });

        statusColumn.setCellFactory(column -> new TableCell<Task, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Set background color based on status
                    if (item.contains("✅")) {
                        setStyle("-fx-background-color: #e8f5e9;");
                    } else if (item.contains("⏳")) {
                        setStyle("-fx-background-color: #fff3e0;");
                    } else if (item.contains("❌")) {
                        setStyle("-fx-background-color: #ffebee;");
                    }
                }
            }
        });

        // Bind the observable list of tasks to the TableView
        taskTable.setItems(tasks);

        // Load tasks from the database if available
        if (dbManager != null) {
            dbManager.loadTasks(tasks);
            taskTable.refresh();
        }

        // Create and add the Agenda control programmatically
        agenda = new Agenda();
        agenda.setPrefHeight(500);
        agenda.setPrefWidth(380);
        agenda.setAllowDragging(true);
        agenda.setAllowResize(true);
        agendaContainer.getChildren().add(agenda);

        // Initialize the agenda appointments based on current tasks
        refreshAgendaAppointments();

        // Add welcome message
        showWelcomeMessage();
    }

    private void showWelcomeMessage() {
        Alert welcomeAlert = new Alert(Alert.AlertType.INFORMATION);
        welcomeAlert.setTitle("Welcome to Task Manager! 👋");
        welcomeAlert.setHeaderText("Let's get organized! 🎯");
        welcomeAlert.setContentText(
            "Here's what you can do:\n\n" +
            "📝 Add new tasks with priority and due dates\n" +
            "✅ Mark tasks as complete\n" +
            "🗑️ Delete tasks you no longer need\n" +
            "📅 View your schedule in the agenda\n" +
            "📊 Track your progress\n\n" +
            "Need help? Click the Help menu or chat with our AI assistant! 🤖"
        );
        welcomeAlert.showAndWait();
    }

    /**
     * Refreshes the Agenda control with appointments based on the tasks list.
     */
    private void refreshAgendaAppointments() {
        // Clear any existing appointments
        agenda.appointments().clear();
        // Create an appointment for each task
        for (Task task : tasks) {
            LocalDateTime start = LocalDateTime.of(task.getDueDate(), task.getDueTime());
            LocalDateTime end = start.plusHours(1);
            AppointmentImplLocal appointment = new AppointmentImplLocal()
                    .withStartLocalDateTime(start)
                    .withEndLocalDateTime(end)
                    .withSummary(task.getDescription())
                    .withDescription("Priority: " + task.getPriority());
            agenda.appointments().add(appointment);
        }
    }

    /**
     * Called when the user clicks the "Add Task" button or presses Enter.
     * Validates input fields, converts time, and adds a new task.
     */
    @FXML
    private void addTask() {
        try {
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
            if (description.isEmpty() || dueDate == null || hour == null || minute == null
                    || amPm == null || priority == null || category == null) {
                showAlert("⚠️ Please fill in all required fields", "Missing Information");
                return;
            }

            // Ensure the due date is in the future
            if (!dueDate.isAfter(LocalDate.now())) {
                showAlert("⚠️ Due date must be in the future", "Invalid Date");
                return;
            }

            // Convert 12-hour time to 24-hour format
            LocalTime dueTime = convertToLocalTime(hour, minute, amPm);

            // Create a new Task with the provided information
            Task task = new Task(description, dueDate, dueTime, priority);
            task.setCategory(category);
            task.setReminder(reminder);

            // Add task to the observable list and database
            tasks.add(task);
            if (dbManager != null) {
                dbManager.addTask(task);
            }

            // Refresh the agenda to include the new task
            refreshAgendaAppointments();

            // Clear the input fields for the next entry
            clearInputs();

            // Show success message with animation
            showSuccessMessage("✨ Task added successfully!");
        } catch (Exception e) {
            showAlert("❌ Error adding task: " + e.getMessage(), "Error");
        }
    }

    /**
     * Handles the editing of a task. Uses an editing mode so that the user first selects
     * a task to edit and then confirms the changes by clicking the same "Edit Task" button.
     */
    @FXML
    private void editTask() {
        // If not in editing mode, load the selected task into the input fields.
        if (currentEditingTask == null) {
            Task selectedTask = taskTable.getSelectionModel().getSelectedItem();
            if (selectedTask != null) {
                currentEditingTask = selectedTask;
                populateFields(currentEditingTask);
                showAlert("Editing mode: modify the fields and click 'Edit Task' again to save changes.", "Edit Mode");
            } else {
                showAlert("Please select a task to edit", "No Task Selected");
            }
        } else {
            // If in editing mode, update the task with the new values.
            updateTask(currentEditingTask);
            if (dbManager != null) {
                dbManager.updateTask(currentEditingTask);
            }
            taskTable.refresh();
            refreshAgendaAppointments();
            showSuccessMessage("✨ Task updated successfully!");
            currentEditingTask = null;
            clearInputs();
        }
    }

    /**
     * Helper method to populate the input fields with the task data.
     * @param task the task whose data is to be loaded
     */
    private void populateFields(Task task) {
        taskInput.setText(task.getDescription());
        dueDatePicker.setValue(task.getDueDate());

        // Convert stored 24-hour time to 12-hour format
        LocalTime time = task.getDueTime();
        int hour24 = time.getHour();
        int displayHour = (hour24 == 0 || hour24 == 12) ? 12 : hour24 % 12;
        String amPm = (hour24 < 12) ? "AM" : "PM";

        hourSpinner.getValueFactory().setValue(displayHour);
        minuteSpinner.getValueFactory().setValue(time.getMinute());
        amPmComboBox.setValue(amPm);

        priorityComboBox.setValue(task.getPriority());
        categoryComboBox.setValue(task.getCategory());
        reminderDatePicker.setValue(task.getReminder());
    }

    /**
     * Helper method to update a task with data from the input fields.
     * @param task the task to update
     */
    private void updateTask(Task task) {
        String updatedDescription = taskInput.getText().trim();
        LocalDate updatedDueDate = dueDatePicker.getValue();
        Integer updatedHour = hourSpinner.getValue();
        Integer updatedMinute = minuteSpinner.getValue();
        String updatedAmPm = amPmComboBox.getValue();
        String updatedPriority = priorityComboBox.getValue();
        String updatedCategory = categoryComboBox.getValue();
        LocalDate updatedReminder = reminderDatePicker.getValue();

        // Convert 12-hour time to 24-hour format using helper method
        LocalTime updatedDueTime = convertToLocalTime(updatedHour, updatedMinute, updatedAmPm);

        // Update the task properties
        task.setDescription(updatedDescription);
        task.setDueDate(updatedDueDate);
        task.setDueTime(updatedDueTime);
        task.setPriority(updatedPriority);
        task.setCategory(updatedCategory);
        task.setReminder(updatedReminder);
    }

    /**
     * Called when the user clicks the "Mark Complete" button.
     * Marks the selected task as completed.
     */
    @FXML
    private void markTaskComplete() {
        Task selectedTask = taskTable.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            selectedTask.setStatus(Task.STATUS_COMPLETED);
            selectedTask.setCompletionPercentage(100);
            taskTable.refresh();
            refreshAgendaAppointments();
            showSuccessMessage("🎉 Task marked as complete!");
        } else {
            showAlert("⚠️ Please select a task to mark as complete", "No Task Selected");
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
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Delete Task");
            confirmDialog.setHeaderText("Are you sure?");
            confirmDialog.setContentText("This action cannot be undone.");
            
            if (confirmDialog.showAndWait().get() == ButtonType.OK) {
                tasks.remove(selectedTask);
                if (dbManager != null) {
                    dbManager.deleteTask(selectedTask.getTaskID());
                }
                refreshAgendaAppointments();
                showSuccessMessage("🗑️ Task deleted successfully!");
            }
        } else {
            showAlert("⚠️ Please select a task to delete", "No Task Selected");
        }
    }

    /**
     * Clears all input fields and resets them to default values.
     */
    private void clearInputs() {
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
     * Helper method to display an information alert with the provided message.
     * @param message the message to display in the alert
     */
    private void showAlert(String message, String title) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Helper method to display a success message with the provided message.
     * @param message the message to display in the success alert
     */
    private void showSuccessMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Helper method to convert 12-hour time (with AM/PM) into 24-hour format.
     * @param hour the hour value (1-12)
     * @param minute the minute value (0-59)
     * @param amPm "AM" or "PM"
     * @return the converted LocalTime
     */
    private LocalTime convertToLocalTime(int hour, int minute, String amPm) {
        int convertedHour = hour;
        if ("PM".equals(amPm) && hour != 12) {
            convertedHour += 12;
        } else if ("AM".equals(amPm) && hour == 12) {
            convertedHour = 0;
        }
        return LocalTime.of(convertedHour, minute);
    }

    /**
     * Adds an imported task (from a file) to the list and refreshes the table.
     * @param task the task to add
     */
    public void addImportedTask(Task task) {
        if (task != null) {
            tasks.add(task);
            if (dbManager != null) {
                try {
                    dbManager.addTask(task);
                    showSuccessMessage("📥 Task imported successfully!");
                } catch (Exception e) {
                    showAlert("❌ Error saving task to database: " + e.getMessage(), "Import Error");
                }
            }
            taskTable.refresh();
            refreshAgendaAppointments();
        } else {
            showAlert("⚠️ Attempted to add a null task", "Import Error");
        }
    }

    /**
     * Opens the AI Chat window when the user clicks the "Open AI Chat" button.
     */
    @FXML
    private void openChatWindow() {
        try {
            FXMLLoader chatLoader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/ChatBoxView.fxml"));
            Parent chatRoot = chatLoader.load();
            Stage chatStage = new Stage();
            chatStage.setTitle("AI Chat");
            Scene chatScene = new Scene(chatRoot);
            chatStage.setScene(chatScene);
            chatStage.show();
        } catch (IOException e) {
            System.out.println("Error opening chat window: " + e.getMessage());
        }
    }
    @FXML
    private void displayLogin(){
        try {
            FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/LoginView.fxml"));
            Parent loginRoot = loginLoader.load();
            Stage loginStage = new Stage();
            Scene loginScene = new Scene(loginRoot);
            loginStage.setScene(loginScene);
            loginStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

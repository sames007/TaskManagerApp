package edu.farmingdale.taskmanagerapp;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
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
    @FXML private MenuItem createNewAccount, loginToExisting, checkOverdueTasksMenuItem;


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
        // Set up options for priority and category ComboBoxes
        priorityComboBox.setItems(FXCollections.observableArrayList("High", "Medium", "Low"));
        categoryComboBox.setItems(FXCollections.observableArrayList("School", "Work", "Personal"));

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

        // Set up the TableView columns with property mappings
        taskColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("dueTime"));
        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Bind the observable list of tasks to the TableView
        taskTable.setItems(tasks);

        // Load tasks from the database if available
        if (dbManager != null) {
            dbManager.loadTasks(tasks);
            taskTable.refresh();
        }

        // --- Create and add the Agenda control programmatically, now named "agenda" ---
        agenda = new Agenda();
        agenda.setPrefHeight(500);
        agenda.setPrefWidth(380);
        // Set additional properties on the agenda control
        agenda.setAllowDragging(true);
        agenda.setAllowResize(true);

        // Add the agenda control to the calendar container in the UI
        agendaContainer.getChildren().add(agenda);
        agendaContainer.sceneProperty().addListener((obs,oldVal, newVal) -> {
            if(newVal != null) refreshAgendaAppointments();
        });
        // Initialize the agenda appointments based on current tasks
        refreshAgendaAppointments();

        //Adds a listener to the list of notifications that updates the current number of notifications
        notifications.addListener((ListChangeListener.Change<? extends String> change) -> {
            int notifCount = notifications.size();
            notificationsMenuItem.setText("Notifications (" + notifCount + ")");
        });
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
            showAlert("Please fill in all required fields (Task, Due Date, Time, Priority, Category)");
            return;
        }
        // Ensure the due date is in the future
        if (!dueDate.isAfter(LocalDate.now())) {
            showAlert("Due date must be in the future.");
            return;
        }

        // Convert 12-hour time to 24-hour format using helper method
        LocalTime dueTime = convertToLocalTime(hour, minute, amPm);

        // Create a new Task with the provided information
        Task task = new Task(description, dueDate, dueTime, priority);
        task.setCategory(category);
        task.setReminder(reminder);

        // Add task to the observable list and database
        tasks.add(task);
        System.out.println("Tasks List: " + tasks);

        //notifications for adding task
        notifications.add("New task added: " + description);
        showNotification("New task added: " + description);

        try {
            dbManager.addTask(task);
        } catch (Exception e) {
            System.out.println("Error saving task to database: " + e.getMessage());
            showAlert("Error saving task to database.");
        }

        // Refresh the agenda to include the new task
        refreshAgendaAppointments();

        // Clear the input fields for the next entry
        clearInputs();
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
                showAlert("Editing mode: modify the fields and click 'Edit Task' again to save changes.");

                //notifications for editing task
                notifications.add("Task editing: " + selectedTask.getDescription());
                showNotification("Task editing: " + selectedTask.getDescription());

            } else {
                showAlert("Please select a task to edit");
            }
        } else {
            // If in editing mode, update the task with the new values.
            updateTask(currentEditingTask);
            if (dbManager != null) {
                dbManager.updateTask(currentEditingTask);
            }
            taskTable.refresh();
            refreshAgendaAppointments();
            showAlert("Task updated successfully.");
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
            selectedTask.setStatus("Completed");
            taskTable.refresh();
            refreshAgendaAppointments();

            //notifications for completing task
            notifications.add("Task completed: " + selectedTask.getDescription());
            showNotification("Task completed: " + selectedTask.getDescription());

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
            if (dbManager != null) {
                dbManager.deleteTask(selectedTask.getTaskID());
            }
            refreshAgendaAppointments();

            //notifications for deleting task
            notifications.add("Task deleted: " + selectedTask.getDescription());
            showNotification("Task deleted: " + selectedTask.getDescription());

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
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
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
                } catch (Exception e) {
                    System.out.println("Error saving task to database: " + e.getMessage());
                    showAlert("Error saving task to database.");
                }
            }
            taskTable.refresh();
            refreshAgendaAppointments();
        } else {
            System.out.println("Attempted to add a null task.");
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
    @FXML
    private void displaySignUp(){
        try {
            FXMLLoader signUpLoader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/SignUpView.fxml"));
            Parent signUpRoot = signUpLoader.load();
            Stage signUpStage = new Stage();
            Scene signUpScene = new Scene(signUpRoot);
            signUpStage.setScene(signUpScene);
            signUpStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if the tasks are overdue or not
     */
    @FXML
private void overDueTaskChecker() {
        // Checks if there are any tasks in the list
        if(tasks.isEmpty()) {
            showAlert("No tasks are available to check");
            return;
        }

        // Reiterating through tasks and checks for tasks that are overdue
        StringBuilder tasksOverdue = new StringBuilder();
        StringBuilder tasksNotOverdue = new StringBuilder();

        for(Task t : tasks) {
            if(t.isOverdue()) {
                tasksOverdue.append("Task: ").append(t.getDescription())
                        .append("\nDue Date: ").append(t.getDueDate())
                        .append("\nDue Time: ").append(t.getDueTime())
                        .append("\n\n");
            } else {
                tasksNotOverdue.append("Task: ").append(t.getDescription())
                        .append("\nDue Date: ").append(t.getDueDate())
                        .append("\nDue Time: ").append(t.getDueTime())
                        .append("\n\n");
            }
        }

        // Show popup based on the results
        if(tasksOverdue.length() > 0) {
            showAlert("Overdue Tasks:\n\n" + tasksOverdue.toString());
        } else {
            showAlert("No overdue tasks are found.");
        }
    if(tasksNotOverdue.length() > 0) {
        showAlert("No Overdue Tasks:\n\n" + tasksNotOverdue.toString());
    }
}

    @FXML
    // Add notification fields
    private MenuItem notificationsMenuItem;
    private ObservableList<String> notifications = FXCollections.observableArrayList();
    private Stage notificationStage;

    //Notification display method
    private void showNotification(String msg) {
        Platform.runLater(() -> {
            Label labelNotification = new Label(msg);
            labelNotification.setStyle("-fx-background-color: #e0e0e0; -fx-padding: 10px; -fx-border-radius: 5px; -fx-background-radius: 5px;");

            StackPane popups = new StackPane(labelNotification);
            popups.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 5);");

            Scene sc = new Scene(popups);
            sc.setFill(Color.TRANSPARENT);

            notificationStage = new Stage();
            notificationStage.initOwner(taskTable.getScene().getWindow());
            notificationStage.initStyle(StageStyle.TRANSPARENT);
            notificationStage.setScene(sc);

            Stage primaryStage = (Stage) taskTable.getScene().getWindow();
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

            double x = primaryStage.isFullScreen() ? screenBounds.getMaxX() : primaryStage.getX() + primaryStage.getWidth();
            double y = primaryStage.isFullScreen() ? screenBounds.getMinY() : primaryStage.getY();

            //Putting notifications in top-right
            notificationStage.setX(x - 200);
            notificationStage.setY(y + 20);
            notificationStage.show();

            //Auto-close after 5 seconds
            PauseTransition delay = new PauseTransition(Duration.seconds(3));
            delay.setOnFinished(e -> notificationStage.close());
            delay.play();
        });
    }

    @FXML
    private void notificationHistory(){
        ListView<String> lv = new ListView<>(notifications);
        lv.setPrefSize(300,200);

        // Create a Clear All button
        Button clearNotifications = new Button("Clear All");
        clearNotifications.setOnAction(e -> {
            // Execute the clearing on the JavaFX thread
            Platform.runLater(() -> {
                notifications.clear();
                lv.setItems(FXCollections.observableArrayList());
            });
        });

        // Create a VBox to hold the ListView and Clear All button
        VBox vbox = new VBox(10, lv, clearNotifications);
        vbox.setPadding(new Insets(10));

        // Create and display the scene
        Scene sc = new Scene(vbox, 300, 200);
        Stage notifHistoryStage = new Stage();
        notifHistoryStage.setTitle("Notification History");
        notifHistoryStage.setScene(sc);

        // Set close request handler
        notifHistoryStage.setOnCloseRequest(e -> {
            System.out.println("Notification history window is closing.");
        });
        notifHistoryStage.show();
    }
}


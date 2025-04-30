package edu.farmingdale.taskmanagerapp;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import jfxtras.scene.control.agenda.Agenda;
import jfxtras.scene.control.agenda.Agenda.AppointmentImplLocal;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import javafx.application.Platform;

/**
 * Controller for managing tasks. It handles adding, editing,
 * marking tasks complete, and deleting tasks.
 */
public class TaskManagerController {

    @FXML
    private Button notificationBtn;
    @FXML
    private TableView<Task> taskTable;
    @FXML
    private TableColumn<Task, String> taskColumn;
    @FXML
    private TableColumn<Task, LocalDate> dueDateColumn;
    @FXML
    private TableColumn<Task, LocalTime> timeColumn;
    @FXML
    private TableColumn<Task, String> priorityColumn;
    @FXML
    private TableColumn<Task, String> statusColumn;
    @FXML
    private VBox agendaVbox; // Renamed from agendaContainer
    @FXML
    private Label agendaLabel;
    @FXML
    private ListView<String> agendaList;
    @FXML
    private DatePicker calendarView; // Renamed from calenderView
    @FXML
    private VBox previewPane;
    @FXML
    private Button addTaskButton;
    @FXML
    private Button markCompleteBtn;
    @FXML
    private Button deleteBtn;
    @FXML
    private Label welcomeLabel;

    private Agenda agenda; // JFXtras Agenda control
    private ObservableList<Task> tasks = FXCollections.observableArrayList();
    private DatabaseManager dbManager;

    /**
     * Setter for DatabaseManager instance.
     *
     * @param dbManager the DatabaseManager to use for DB operations.
     */
    public void setDatabaseManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        loadTasksFromDb();
    }

    public TaskManagerController() {}
    /**
     * Initializes the controller after the FXML file is loaded.
     * Sets up the ComboBoxes, TableView columns, Spinners, and agenda.
     */
    @FXML
    public void initialize() {

        // Set up the TableView columns with property mappings
        taskColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("dueTime"));
        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Bind the observable list of tasks to the TableView
        taskTable.setItems(tasks);

        // --- Create and add the Agenda control programmatically, now named "agenda" ---
        agenda = new Agenda();
        agenda.setAllowDragging(true);
        agenda.setAllowResize(true);

        // Add the agenda control to the calendar container in the UI
        if (agendaVbox != null) {
            agendaVbox.getChildren().add(agenda);
        } else {
            System.err.println("'agendaContainer' is EMPTY");
        }

        if (addTaskButton != null) {
            addTaskButton.setOnAction(event -> showTaskDialog(null));
        } else {
            System.err.println("'addTaskButton' is EMPTY");
        }

        if (notificationBtn != null) {
            notificationBtn.setOnAction(event -> System.out.println("Notification Button Clicked"));
        }

        if (calendarView != null) {
            calendarView.setOnAction(event -> {
                LocalDate selectedDate = calendarView.getValue();
                System.out.println("Date Selected In Calender: " + selectedDate);
                if (selectedDate != null && agenda != null) {
                    agenda.setDisplayedLocalDateTime(selectedDate.atStartOfDay());
                }
            });
        }

        if (markCompleteBtn != null) {
            markCompleteBtn.setOnAction(event -> markTaskComplete());
        } else {
            System.err.println("'markCompleteBtn' is EMPTY");
        }

        if (deleteBtn != null) {
            deleteBtn.setOnAction(event -> deleteTask());
        } else {
            System.err.println("'deleteBtn' is EMPTY");
        }

        Platform.runLater(this::refreshAgendaAppointments);
    }

    private void loadTasksFromDb() {
        if (dbManager != null) {
            tasks.clear();
            dbManager.loadTasks(tasks);
            taskTable.refresh();
            Platform.runLater(this::refreshAgendaAppointments);
        } else {
            System.err.println("Database Manager EMPTY - Cannot Load Tasks");
            showAlert("COULD NOT CONNECT TO DATABASE TO LOAD TASKS");
        }
    }

    /**
     * Refreshes the Agenda control with appointments based on the tasks list.
     */
    private void refreshAgendaAppointments() {
        // Clear any existing appointments
        if (agenda == null) {
            System.err.println("Agenda Control Null - Cannot Refresh");
            return;
        }

        agenda.appointments().clear();
        // Create an appointment for each task
        for (Task task : tasks) {
            if (!"Completed".equalsIgnoreCase(task.getStatus())) {
                try {
                    if (task.getDueDate() == null || task.getDueTime() == null) {
                        LocalDateTime start = LocalDateTime.of(task.getDueDate(), task.getDueTime());
                        LocalDateTime end = start.plusHours(1);
                        AppointmentImplLocal appointment = new AppointmentImplLocal()
                                .withStartLocalDateTime(start)
                                .withEndLocalDateTime(end)
                                .withSummary(task.getDescription())
                                .withDescription("Priority: " + task.getPriority());
                        agenda.appointments().add(appointment);
                    } else {
                        System.err.println("Skipped Appointment For Task With NULL Data/Time");
                    }
                } catch (NullPointerException e) {
                    System.err.println("Skipped Appointment For Task With EMPTY Data/Time: " + task.getDescription());
                }
            }

            if (agenda.getSkin() != null) {
                agenda.refresh();
            } else {
                System.err.println("Agenda Skin STILL NULL - Cannot Refresh");
            }
        }
    }

    private Agenda.AppointmentGroup getAppointmentGroupForPriority(String priority) {

        Agenda.AppointmentGroup highPriorityGroup = new Agenda.AppointmentGroupImpl().withStyleClass("priority-high");
        Agenda.AppointmentGroup mediumPriorityGroup = new Agenda.AppointmentGroupImpl().withStyleClass("priority-medium");
        Agenda.AppointmentGroup lowPriorityGroup = new Agenda.AppointmentGroupImpl().withStyleClass("priority-low");
        Agenda.AppointmentGroup defaultGroup = new Agenda.AppointmentGroupImpl().withStyleClass("priority-default");

        if (priority == null) return defaultGroup;

        return switch (priority.toLowerCase()) {
            case "extreme", "high" -> highPriorityGroup;
            case "medium" -> mediumPriorityGroup;
            case "low" -> lowPriorityGroup;
            default -> defaultGroup;
        };
    }

    /**
     * Called when the user intends to edit a task (e.g., double-clicking the row or clicking an Edit button).
     * Opens the Add/Edit Task dialog in "edit" mode.
     */
    @FXML
    private void handleEditTask() {
        Task selectedTask = taskTable.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            showTaskDialog(selectedTask);
        } else {
            showAlert("Please select a task to edit.");
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
            if (!"Completed".equalsIgnoreCase(selectedTask.getStatus())) {
                selectedTask.setStatus("Completed");
                if (dbManager != null) {
                    dbManager.updateTask(selectedTask);
                    taskTable.refresh();
                    refreshAgendaAppointments();
                } else {
                    System.err.println("Database Manager EMPTY - Added to UI But NOT SAVED In Database");
                    showAlert("Task Added To List But NOT SAVED to Database");
                }
                taskTable.refresh(); // Refresh table to show status change
                refreshAgendaAppointments(); // Remove completed task from agenda
            } else {
                showAlert("Task Already Marked Complete");
            }
        } else {
            showAlert("Please Select A Task");
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
            // Confirmation dialog
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirm Deletion");
            confirmation.setHeaderText("Delete Task: " + selectedTask.getDescription());
            confirmation.setContentText("Are you sure you want to permanently delete this task?");

            confirmation.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    boolean deletedFromDb = false;
                    if (dbManager != null) {
                        try {
                            dbManager.deleteTask(selectedTask.getTaskID());
                            deletedFromDb = true;
                            System.out.println("Task '" + selectedTask.getDescription() + "' deleted from DB.");
                        } catch (Exception e) {
                            System.err.println("Error deleting task from Database: " + e.getMessage());
                            showAlert("Error deleting task from database. Please try again.");
                        }
                    } else {
                        System.err.println("Database Manager is NULL - Cannot delete task from Database");
                        showAlert("Could not connect to the database to delete the task.");
                    }

                    if (deletedFromDb || dbManager == null) {
                        tasks.remove(selectedTask);
                        taskTable.refresh();
                        refreshAgendaAppointments();
                        System.out.println("Task '" + selectedTask.getDescription() + "' removed from UI.");
                    }
                }
            });
        } else {
            showAlert("Please select a task to delete.");
        }
    }

    /**
     * Helper method to display an information alert with the provided message.
     *
     * @param message the message to display in the alert
     */
    public void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Helper method to convert 12-hour time (with AM/PM) into 24-hour format.
     *
     * @param hour   the hour value (1-12)
     * @param minute the minute value (0-59)
     * @param amPm   "AM" or "PM"
     * @return the converted LocalTime
     */
     public LocalTime convertToLocalTime(int hour, int minute, String amPm) {
        if (hour < 1 || hour > 12 || minute < 0 || minute > 59 || amPm == null) {
            System.err.println("Invalid time input provided: " + hour + ":" + minute + " " + amPm);
            return null;
        }

        int convertedHour = hour;
        if ("PM".equalsIgnoreCase(amPm)) {
            if (hour != 12) {
                convertedHour += 12;
            }

        } else if ("AM".equalsIgnoreCase(amPm)) {
            if (hour == 12) {
                convertedHour = 0;
            }
        } else {
            System.err.println("Invalid AM/PM value: " + amPm);
            return null;
        }

        try {
            return LocalTime.of(convertedHour, minute);
        } catch (Exception e) {
            System.err.println("Error creating LocalTime: " + e.getMessage());
            return null;
        }
    }

    /**
     * Opens the Add Task dialog window.
     */
    @FXML
    private void showTaskDialog(Task taskToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AddTaskDialog.fxml"));
            if (loader.getLocation() == null) {
                throw new IOException("Cannot find FXML file: AddTaskDialog.fxml");
            }
            Parent root = loader.load();

            AddTaskController dialogController = loader.getController();
            dialogController.setMainController(this);
            dialogController.setTaskToEdit(taskToEdit);

            Stage dialogStage = new Stage();
            dialogStage.setTitle(taskToEdit == null ? "Add New Task" : "Edit Task");

            Scene mainScene = addTaskButton.getScene();
            if (mainScene != null && mainScene.getWindow() != null) {
                dialogStage.initOwner(mainScene.getWindow());
            } else {
                System.err.println("Warning: Could not set owner for dialog stage.");
            }
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            Scene scene = new Scene(root);
            try {
                scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/styles.css")).toExternalForm());
                scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/addTaskDialog.css")).toExternalForm());
            } catch (NullPointerException e) {
                System.err.println("Warning: Could Not Load One Or More CSS Files For Dialog.");
            }
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            dialogStage.showAndWait();

        } catch (IOException e) {
            System.err.println("Error Loading Add/Edit Task Dialog: " + e.getMessage());
            e.printStackTrace();
            showAlert("CANNOT Open Task Dialog - Check Files");
        } catch (IllegalStateException e) {
            System.err.println("Error During FXML Loading: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error Occurred While Initializing");
        } catch (NullPointerException e) {
            System.err.println("Null Pointer Exception during dialog setup: " + e.getMessage());
            e.printStackTrace();
            showAlert("An internal error occurred opening the dialog.");
        }
    }


    public void addNewTaskFromDialog(Task newTask) {
        if (newTask != null) {
            if (dbManager != null) {
                try {
                    dbManager.addTask(newTask); // Save to DB first
                    tasks.add(newTask); // Add to UI list only after successful DB save
                    taskTable.refresh();
                    refreshAgendaAppointments();
                    System.out.println("Task Added & Saved: " + newTask.getDescription());
                } catch (Exception e) {
                    System.err.println("Error Saving New Task From Dialog To Database: " + e.getMessage());
                    showAlert("Error Saving New Task");
                }
            } else {
                tasks.add(newTask);
                taskTable.refresh();
                refreshAgendaAppointments();
                System.err.println("Database Manager NULL - Task added to UI but NOT SAVED in Database");
                showAlert("Task Added To List - CANNOT SAVE TO DATABASE - Please Check Connection.");
            }
        } else {
            System.err.println("Attempted To Add A NULL (EMPTY) Task");
        }
    }

    public void updateTaskFromDialog(Task updatedTask) {
        if (updatedTask == null) {
            System.err.println("updateTaskFromDialog Called With NULL Task");
            showAlert("Cannot Update Task - No Data To Receive");
            return;
        }

        if (dbManager != null) {
            try {
                dbManager.updateTask(updatedTask); // Update in DB
                System.out.println("Task updated in DB: " + updatedTask.getDescription());
                taskTable.refresh(); // Refresh the table to show changes
                refreshAgendaAppointments();
            } catch (Exception e) {
                System.err.println("Error Updating Task In Database: " + e.getMessage());
                e.printStackTrace();
                showAlert("Error Updating Task In Database - Check Connection");
            }
        } else {
            System.err.println("Database Manager is NULL - Task updated in UI but NOT SAVED");
            showAlert("Task Updated In List But NOT SAVED to Database");
            taskTable.refresh();
            refreshAgendaAppointments();
        }
    }

    private void showEditTaskDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AddTaskDialog.fxml"));
            Parent root = loader.load();

            AddTaskController dialogController = loader.getController();
            dialogController.setMainController(this);

            Task selectedTask = taskTable.getSelectionModel().getSelectedItem();
            if (selectedTask != null) {
                dialogController.setTaskToEdit(selectedTask);
            } else {
                showAlert("Please Select A Task To Edit");
            }

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Editing Task");
            dialogStage.initOwner(addTaskButton.getScene().getWindow());

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (IOException e) {
            System.err.println("Error Loading Edit Task: " + e.getMessage());
            e.printStackTrace();
            showAlert("Could Not Open Edit Task Window - Check FXML File");
        }
    }
    /**
     * Adds a new task received from the dialog, updates DB, and refreshes UI.
     * This method is called by AddTaskDialogController.
     *
     * @param task The new task created in the dialog.
     */
    public void addNewTaskFrom(Task task) {
        if (task != null) {
            tasks.add(task); // Add to the observable list
            System.out.println("Task Added From Dialog: " + task);

            if (dbManager != null) {
                try {
                    dbManager.addTask(task);
                } catch (Exception e) {
                    System.err.println("Error Saving Task From Dialog to Database: " + e.getMessage());
                    showAlert("Error Saving Task To Database.");
                    tasks.remove(task); // Rollback UI change if DB fails
                    return;
                }
            } else {
                System.err.println("Database Manager EMPTY - Added to UI But NOT SAVED In Database");
                showAlert("Task Added To List But NOT SAVED to Database");
            }
            // Refresh the agenda if needed
            refreshAgendaAppointments();
        } else {
            System.err.println("'addNewTask' Called With EMPTY Task");
        }
    }
    /**
     * Adds an imported task (from a file) to the list and refreshes the table.
     *
     * @param task the task to add
     */
    public void addImportedTask(Task task) {
        if (task != null) {
            if (dbManager != null) {
                try {
                    dbManager.addTask(task);
                    tasks.add(task);
                    taskTable.refresh();
                    refreshAgendaAppointments();
                } catch (Exception e) {
                    System.out.println("Error Saving To Database: " + e.getMessage());
                    showAlert("Error Saving To Database.");
                    return;
                }
            } else {
                tasks.add(task);
                taskTable.refresh();
                refreshAgendaAppointments();
                System.err.println("Database Manager NULL - Imported Task Added But NOT SAVED to Database");
                showAlert("Imported Task Added To List But NOT SAVED to Database");
            }
        } else {
            System.out.println("Attempted To Add A NULL (EMPTY) Imported Task.");
        }
    }

    /**
     * Opens the AI Chat window when the user clicks the "Open AI Chat" button.
     */
    @FXML
    private void openChatWindow() {
        try {
            FXMLLoader chatLoader = new FXMLLoader(getClass().getResource("ChatBoxView.fxml"));
            if (chatLoader.getLocation() == null) {
                throw new IOException("Cannot Find FXML file: ChatBoxView.fxml");
            }
            Parent chatRoot = chatLoader.load();
            Stage chatStage = new Stage();
            chatStage.setTitle("AI Chat Assist");
            Scene chatScene = new Scene(chatRoot);
            chatScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/styles.css")).toExternalForm());
            chatStage.setScene(chatScene);
            chatStage.show();
        } catch (IOException e) {
            System.out.println("Error Opening Chat Window: " + e.getMessage());
            showAlert("Cannot Open Chat Window - Check FXML File");
        }
    }

    @FXML
    private void displayLogin() {
        try {
            FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/LoginView.fxml"));
            if (loginLoader.getLocation() == null) {
                throw new IOException("Cannot Find FXML file: LoginView.fxml");
            }
            Parent loginRoot = loginLoader.load();
            Stage loginStage = new Stage();
            loginStage.setTitle("Login");
            Scene loginScene = new Scene(loginRoot);
            loginScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/styles.css")).toExternalForm());
            loginStage.setScene(loginScene);
            loginStage.showAndWait();
        } catch (IOException e) {
            System.err.println("Error Opening Login Window: " + e.getMessage());
            e.printStackTrace();
            showAlert("Cannot Open Login Window.");
        }
    }

    @FXML
    private void displaySignUp() {
        try {
            FXMLLoader signUpLoader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/SignUpView.fxml"));
            if (signUpLoader.getLocation() == null) {
                throw new IOException("Cannot Find FXML File: SignUpView.fxml");
            }
            Parent signUpRoot = signUpLoader.load();
            Stage signUpStage = new Stage();
            signUpStage.setTitle("Sign Up");
            Scene signUpScene = new Scene(signUpRoot);
            signUpScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/styles.css")).toExternalForm());
            signUpStage.setScene(signUpScene);
            signUpStage.showAndWait();
        } catch (IOException e) {
            System.err.println("Error Opening Sign Up Window" + e.getMessage());
            e.printStackTrace();
            showAlert("Cannot Open Sign Window");
        }
    }

    public void updateTaskFrom(Task taskToEdit) {
        if (taskToEdit == null) {
            System.err.println("updateTaskFrom called with a null task.");
            showAlert("Cannot update task: No task data received.");
            return;
        }

        if (dbManager != null) {
            try {
                dbManager.updateTask(taskToEdit);
                System.out.println("Task Updated In DB: " + taskToEdit.getDescription());
            } catch (Exception e) {
                System.err.println("Error Updating Task In Database: " + e.getMessage());
                e.printStackTrace();
                showAlert("Error Updating Task In Database");
            }

        } else {
            System.err.println("'dbManager' is EMPTY - Updated in UI but NOT SAVED");
            showAlert("'dbManager' is EMPTY - Updated in UI but NOT SAVED");

        }

        taskTable.refresh();
        refreshAgendaAppointments();
    }
}
package edu.farmingdale.taskmanagerapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import jfxtras.scene.control.agenda.Agenda;
import jfxtras.scene.control.agenda.Agenda.AppointmentImplLocal;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import javafx.scene.image.ImageView;

/**
 * Controller for managing tasks. It handles adding, editing,
 * marking tasks complete, and deleting tasks.
 */
public class TaskManagerController extends Application {

    @FXML private Button notificationBtn;
    @FXML private TableView<Task> taskTable;
    @FXML private TableColumn<Task, String> taskColumn;
    @FXML private TableColumn<Task, LocalDate> dueDateColumn;
    @FXML private TableColumn<Task, LocalTime> timeColumn;
    @FXML private TableColumn<Task, String> priorityColumn;
    @FXML private TableColumn<Task, String> statusColumn;
    @FXML private VBox agendaVbox; // Renamed from agendaContainer
    @FXML private Label agendaLabel;
    @FXML private ListView<String> agendaList;
    @FXML private DatePicker calendarView; // Renamed from calenderView
    @FXML private VBox previewPane;
    @FXML private Button addTaskButton;
    @FXML private Button markCompleteBtn;
    @FXML private Button deleteBtn;
    @FXML private Label welcomeLabel;
    @FXML private ImageView profilePicture;
    @FXML private Circle notificationIndicator;
    @FXML private ToggleButton themeToggleBtn;

    private Agenda agenda; // JFXtras Agenda control
    private ObservableList<Task> tasks = FXCollections.observableArrayList();
    private DatabaseManager dbManager;
    private ProfileManager profileManager;
    private final BooleanProperty hasPendingNotification = new SimpleBooleanProperty(false);
    private ContextMenu currentContextMenu = null;
    private static final String light_Theme = "styling/styles.css";
    private static final String dark_Theme = "styling/darkTheme.css";
    private boolean darkMode = false;

    /**
     * Setter for DatabaseManager instance.
     *
     * @param dbManager the DatabaseManager to use for DB operations.
     */
    public void setDatabaseManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        loadTasksFromDb();

        NotificationService.startNotificationService(this);
    }

    /**
     * Default constructor.
     */
    public TaskManagerController() {}

    /**
     * Initializes the controller after the FXML file is loaded.
     * Sets up the ComboBoxes, TableView columns, Spinners, and agenda.
     */
    @FXML
    public void initialize() {
        // Initialize profile manager
        profileManager = new ProfileManager(this);
        if (profilePicture != null) {
            profileManager.initialize(profilePicture);
        } else {
            System.err.println("Profile picture ImageView not found in FXML!");
        }

        // Set up the TableView columns with property mappings
        taskColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("dueTime"));
        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Bind the observable list of tasks to the TableView
        taskTable.setItems(tasks);

        // Handle mouse clicks on the task table:
        // - Single left-click: Select task
        // - Double left-click: Edit a task if clicked on a row with data, add a new task if clicked on an empty area
        // - Right-click is handled separately by setOnContextMenuRequested
        taskTable.setOnMouseClicked(event -> {
            if (!event.getButton().equals(javafx.scene.input.MouseButton.PRIMARY)) {
                return;
            }
            if (currentContextMenu != null) {
                currentContextMenu.hide();
            }
            if (event.getClickCount() == 2) { // Check for double-click
                Node target = event.getPickResult().getIntersectedNode();
                TableRow<Task> row = null;
                while (target != null && target != taskTable && !(target instanceof TableRow)) {
                    target = target.getParent();
                }

                if (target instanceof TableRow) {
                    row = (TableRow<Task>) target;
                    // Check if the row contains data
                    if (!row.isEmpty()) {
                        Task clickedTask = row.getItem();
                        if (clickedTask != null) {
                            // Double-clicked ON a row with a task -> Edit that task
                            showTaskDialog(clickedTask);
                        } else {
                            // Double-clicked on a row object that is somehow empty (unlikely) -> Add
                            taskTable.getSelectionModel().clearSelection(); // Clear selection just in case
                            showTaskDialog(null);
                        }
                    } else {
                        // Double-clicked on an empty row representation (e.g., below the last item) -> Add
                        taskTable.getSelectionModel().clearSelection(); // Clear selection just in case
                        showTaskDialog(null);
                    }
                } else {
                    // Clicked outside any row (e.g., header, empty table background) -> Add
                    taskTable.getSelectionModel().clearSelection(); // Clear selection just in case
                    showTaskDialog(null);
                }
                event.consume();
            }
        });

        // Handle right-click on the task table
        taskTable.setOnContextMenuRequested(event -> {
            Node target = event.getPickResult().getIntersectedNode();
            TableRow<Task> row = null;
            while (target != null && target != taskTable && !(target instanceof TableRow)) {
                target = target.getParent();
            }

            boolean clickedOnEmptySpace = true; // Assume empty space initially

            // Check if the row contains data
            if (target instanceof TableRow) {
                row = (TableRow<Task>) target;
                if (!row.isEmpty()) {
                    // Clicked on a row with an actual task item
                    clickedOnEmptySpace = false;
                    if (currentContextMenu != null) {
                        currentContextMenu.hide();
                    }
                    System.out.println("Right-clicked on task: " + row.getItem().getDescription()); // For debugging
                }
            }

            // Handle a right-click context menu:
            // - If clicked on empty space (no task row): Show "Add New Task" menu
            // - Row context menus are handled separately via row factory
            // - the Current context menu is tracked and hidden when appropriate
            // - Auto-hide enabled to dismiss when clicking outside
            if (clickedOnEmptySpace) {
                if (currentContextMenu != null) {
                    currentContextMenu.hide();
                }
                ContextMenu contextMenu = new ContextMenu();
                MenuItem addItem = new MenuItem("Add New Task");

                // Set the action for the menu item
                addItem.setOnAction(actionEvent -> {
                    // Clear selection just before showing the dialog
                    taskTable.getSelectionModel().clearSelection();
                    showTaskDialog(null); // Open the dialog to add a NEW task
                });
                contextMenu.getItems().add(addItem);
                contextMenu.setAutoHide(true); // Important for hiding on clicks outside

                // Store reference and set handler to clear it when hidden
                currentContextMenu = contextMenu; // Store this specific menu instance
                contextMenu.setOnHidden(e -> {
                    // Only clear if the hidden menu is the one we stored
                    if (currentContextMenu == contextMenu) {
                        currentContextMenu = null;
                    }
                });
                contextMenu.show(taskTable, event.getScreenX(), event.getScreenY());
                event.consume();
            }
        });

        // Initialize the notification service
        notificationIndicator.visibleProperty().bind(hasPendingNotification);
        notificationIndicator.managedProperty().bind(hasPendingNotification);

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
            notificationBtn.setOnAction(event -> showNotificationSettings());
        } else {
            System.err.println("'notificationBtn' is EMPTY");
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

        // Add this at the end of initialize(), after taskTable.setItems(tasks);
        taskTable.setRowFactory(tv -> {
            TableRow<Task> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();
            MenuItem editItem = new MenuItem("Edit");
            editItem.setOnAction(evt -> {
                Task clickedTask = row.getItem();
                if (clickedTask != null) {
                    showTaskDialog(clickedTask);
                }
            });
            contextMenu.getItems().add(editItem);

            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );
            return row;
        });

        // Start the periodic service
        NotificationService.startNotificationService(this);

        // ALSO re‐run the indicator logic if tasks list changes
        tasks.addListener((ListChangeListener<Task>) change -> {
            NotificationService.checkDueTasks(this);
        });

        hasPendingNotification.bind(Bindings.createBooleanBinding(
                () -> tasks.stream().anyMatch(NotificationService::shouldNotifyTask),
                tasks   // re‐compute whenever `tasks` changes
        ));

        // Toggles the label of the themes based on user click
        Platform.runLater(() -> {
            if(themeToggleBtn != null) {
                themeToggleBtn.setSelected(false);
                themeToggleBtn.setText("Dark Mode");
                themeToggleBtn.setOnAction(e -> {
                    darkMode = themeToggleBtn.isSelected();
                    themeToggleBtn.setText(darkMode ? "Light Mode" : "Dark Mode");
                    Scene scene = themeToggleBtn.getScene();
                    if(scene != null) {
                        applyTheme(scene, darkMode);
                    }
                });
            }
        });
    }

    /**
     * Loads tasks from the database into the application's task list and refreshes the UI components.
     * If the database manager is available, existing tasks in the list are cleared and replaced
     * by tasks retrieved from the database.
     */
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
        if (agenda == null) {
            System.err.println("Agenda Control Null - Cannot Refresh");
            return;
        }

        agenda.appointments().clear();

        for (Task task : tasks) {
            if (!"Completed".equalsIgnoreCase(task.getStatus())) {
                try {
                    if (task.getDueDate() != null) {
                        LocalDateTime start = LocalDateTime.of(
                            task.getDueDate(),
                            task.getDueTime() != null ? task.getDueTime() : LocalTime.of(9, 0)
                        );
                        LocalDateTime end = start.plusHours(1);

                        AppointmentImplLocal appointment = new AppointmentImplLocal()
                            .withStartLocalDateTime(start)
                            .withEndLocalDateTime(end)
                            .withSummary(task.getDescription())
                            .withDescription("Priority: " + task.getPriority())
                            .withAppointmentGroup(getAppointmentGroupForPriority(task.getPriority()));

                        agenda.appointments().add(appointment);
                    }
                } catch (Exception e) {
                    System.err.println("Error creating appointment for task: " + task.getDescription() + " - " + e.getMessage());
                }
            }
        }

        if (agenda.getSkin() != null) {
            agenda.refresh();
        }
    }

    /**
     * @param priority the priority of the task
     * @return Priority of task
     */
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
        if (selectedTask == null) {
            showAlert("Please select a task to mark as complete.");
            return;
        }

        if ("Completed".equalsIgnoreCase(selectedTask.getStatus())) {
            showAlert("This task is already marked as complete.");
            return;
        }

        try {
            selectedTask.setStatus("Completed");
            if (dbManager != null) {
                dbManager.updateTask(selectedTask);
                taskTable.refresh();
                refreshAgendaAppointments();
            } else {
                throw new IllegalStateException("Database connection not available");
            }
        } catch (Exception e) {
            showAlert("Error updating task status: " + e.getMessage());
            selectedTask.setStatus("Pending"); // Revert the status change
            taskTable.refresh();
        }
    }

    /**
     * Called when the user clicks the "Delete Task" button.
     * Removes the selected task from the list.
     */
    @FXML
    private void deleteTask() {
        Task selectedTask = taskTable.getSelectionModel().getSelectedItem();
        if (selectedTask == null) {
            showAlert("Please select a task to delete.");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Deletion");
        confirmDialog.setHeaderText("Delete Task");
        confirmDialog.setContentText("Are you sure you want to delete this task?");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    if (dbManager != null) {
                        dbManager.deleteTask(selectedTask.getTaskID());
                        tasks.remove(selectedTask);
                        taskTable.refresh();
                        refreshAgendaAppointments();
                    } else {
                        throw new IllegalStateException("Database connection not available");
                    }
                } catch (Exception e) {
                    showAlert("Error deleting task: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Helper method to display an information alert with the provided message.
     *
     * @param message the message to display in the alert
     */
    public void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Task Manager");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.showAndWait();
        });
    }

    /**
     * Helper method to convert 12-hour time (with AM/PM) into 24-hour format.
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

    /**
     * @param newTask the new task to add
     */
    public void addNewTaskFromDialog(Task newTask) {
        if (newTask == null) {
            showAlert("Invalid task data.");
            return;
        }

        if (!newTask.isValidDueDate()) {
            showAlert("Due date must be in the future.");
            return;
        }

        try {
            if (dbManager != null) {
                dbManager.addTask(newTask);
                tasks.add(newTask);
                taskTable.refresh();
                refreshAgendaAppointments();
            } else {
                throw new IllegalStateException("Database connection not available");
            }
        } catch (Exception e) {
            showAlert("Error adding task: " + e.getMessage());
        }
    }

    /**
     * @param updatedTask the task to update
     */
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

    /**
     * Opens the Add Task dialog window.
     */
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

    /**
     * Opens the Login window when the user clicks the "Login" button.
     */
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

    /**
     * Opens the Sign Up window when the user clicks the "Sign Up" button.
     */
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

    /**
     * @param taskToEdit the task to edit
     */
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

    /**
     * Returns the list of tasks for the notification service
     */
    public ObservableList<Task> getTasks() {
        return tasks;
    }

    /**
     * Shows the notification settings dialog
     */
    private void showNotificationSettings() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Task Notifications");
        dialog.setHeaderText(null);

        // Create a custom dialog pane
        DialogPane dialogPane = dialog.getDialogPane();
        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20;");

        // Header section
        Label headerLabel = new Label("Upcoming Tasks");
        headerLabel.setStyle(
            "-fx-font-size: 24px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #2c3e50;"
        );

        // Settings info
        Label settingsLabel = new Label(
            "• Notifications are enabled for tasks due within 24 hours\n" +
            "• Only incomplete tasks will trigger notifications\n" +
            "• Notifications will appear in the bottom-right corner"
        );
        settingsLabel.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-text-fill: #34495e;"
        );

        // Create a separator
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #bdc3c7;");

        Label upcomingTasksLabel = new Label("Tasks Due Soon:");
        upcomingTasksLabel.setStyle(
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #2c3e50;"
        );

        VBox tasksContainer = new VBox(10);
        boolean hasUpcomingTasks = false;

        // Sort tasks by due date/time
        List<Task> sortedTasks = new ArrayList<>(tasks);
        sortedTasks.sort((t1, t2) -> {
            LocalDateTime dt1 = LocalDateTime.of(t1.getDueDate(), t1.getDueTime() != null ? t1.getDueTime() : LocalTime.of(9, 0));
            LocalDateTime dt2 = LocalDateTime.of(t2.getDueDate(), t2.getDueTime() != null ? t2.getDueTime() : LocalTime.of(9, 0));
            return dt1.compareTo(dt2);
        });

        for (Task task : sortedTasks) {
            if (task.getStatus().equals("Completed")) {
                continue;
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime dueDateTime = LocalDateTime.of(task.getDueDate(),
                task.getDueTime() != null ? task.getDueTime() : LocalTime.of(9, 0));

            long hoursUntilDue = ChronoUnit.HOURS.between(now, dueDateTime);

            if (hoursUntilDue > 0 && hoursUntilDue <= 24) {
                hasUpcomingTasks = true;

                VBox taskCard = new VBox(5);
                taskCard.setStyle(
                    "-fx-background-color: white;" +
                    "-fx-padding: 10;" +
                    "-fx-background-radius: 5;" +
                    "-fx-border-radius: 5;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);"
                );

                // Task description with priority indicator
                HBox taskHeader = new HBox(10);
                Circle priorityIndicator = new Circle(6);
                priorityIndicator.setStyle(getPriorityColor(task.getPriority()));

                Label taskDesc = new Label(task.getDescription());
                taskDesc.setStyle(
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #2c3e50;"
                );

                taskHeader.getChildren().addAll(priorityIndicator, taskDesc);

                // Due time and category
                Label dueInfo = new Label(String.format("Due in %d hours • %s",
                    hoursUntilDue, task.getCategory()));
                dueInfo.setStyle("-fx-text-fill: #7f8c8d;");

                taskCard.getChildren().addAll(taskHeader, dueInfo);
                tasksContainer.getChildren().add(taskCard);
            }
        }

        if (!hasUpcomingTasks) {
            Label noTasksLabel = new Label("No tasks due in the next 24 hours");
            noTasksLabel.setStyle(
                "-fx-font-size: 14px;" +
                "-fx-text-fill: #7f8c8d;" +
                "-fx-font-style: italic;"
            );
            tasksContainer.getChildren().add(noTasksLabel);
        }

        // Add scroll capability for many tasks
        ScrollPane scrollPane = new ScrollPane(tasksContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(200);
        scrollPane.setStyle(
            "-fx-background: transparent;" +
            "-fx-background-color: transparent;"
        );

        // Add all components to the content
        content.getChildren().addAll(
            headerLabel,
            settingsLabel,
            separator,
            upcomingTasksLabel,
            scrollPane
        );

        // Style the dialog
        dialogPane.setContent(content);
        dialogPane.getStyleClass().add("notification-settings-dialog");
        dialogPane.setStyle(
            "-fx-background-color: #f5f6fa;" +
            "-fx-padding: 20;"
        );

        // Add the OK button
        dialogPane.getButtonTypes().add(ButtonType.OK);
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setStyle(
            "-fx-background-color: #2c3e50;" +
            "-fx-text-fill: white;" +
            "-fx-padding: 8 20;" +
            "-fx-background-radius: 5;"
        );

        dialog.showAndWait();
    }

    /** Called by NotificationService to show/hide the red dot */
    public void setPendingNotification(boolean anyDueSoon) {
        hasPendingNotification.set(anyDueSoon);
    }

    /**
     * @param priority the priority of the task
     * @return Priority color string
     */
    private String getPriorityColor(String priority) {
        if (priority == null) return "-fx-fill: #95a5a6;"; // Default gray

        return switch (priority.toLowerCase()) {
            case "extreme" -> "-fx-fill: #e74c3c;"; // Red
            case "high" -> "-fx-fill: #e67e22;";    // Orange
            case "medium" -> "-fx-fill: #f1c40f;";  // Yellow
            case "low" -> "-fx-fill: #2ecc71;";     // Green
            default -> "-fx-fill: #95a5a6;";        // Gray
        };
    }

    /**
     * Starts the notification service
     */
    @Override
    public void start(Stage primaryStage) {
        // This method is required by Application but not used in this controller
    }

    /**
     * Starts the notification service
     */
    @Override
    public void stop() {
        NotificationService.stopNotificationService();
    }

    /**
     * Called when the user clicks the "Logout" button.
     */
    public void handleLogout() {
        tasks.clear();
        welcomeLabel.setText("Welcome!");
        if (dbManager != null) {
            dbManager.closeConnection();
        }
        showAlert("Successfully logged out!");
    }

    /**
     * @param user the user to set
     */
    public void setCurrentUser(UserSession user) {
        if (profileManager != null) {
            profileManager.setCurrentUser(user);
        }
        if (user != null) {
            welcomeLabel.setText("Welcome, " + user.getUserName() + "!");
            loadTasksFromDb(); // Reload tasks for the new user
        } else {
            welcomeLabel.setText("Welcome!");
            tasks.clear();
        }
    }

    public DatabaseManager getDbManager() {
        return dbManager;
    }

    /**
     * Shows the login screen
     */
    public void showLoginScreen() {
        try {
            FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/LoginView.fxml"));
            Parent loginRoot = loginLoader.load();

            LoginController loginController = loginLoader.getController();
            loginController.setMainController(this); // Changed to setMainController to match pattern from SignUpController

            Stage loginStage = new Stage();
            loginStage.setTitle("Login");
            Scene loginScene = new Scene(loginRoot);
            loginScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/styles.css")).toExternalForm());
            loginStage.setScene(loginScene);
            loginStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            loginStage.initOwner(profilePicture.getScene().getWindow());
            loginStage.showAndWait();
        } catch (IOException e) {
            System.err.println("Error Opening Login Window: " + e.getMessage());
            e.printStackTrace();
            showAlert("Cannot Open Login Window.");
        }
    }

    /**
     * Shows the sign-up screen
     */
    public void showSignUpScreen() {
        try {
            FXMLLoader signUpLoader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/SignUpView.fxml"));
            Parent signUpRoot = signUpLoader.load();

            SignUpController signUpController = signUpLoader.getController();
            signUpController.setMainController(this);

            Stage signUpStage = new Stage();
            signUpStage.setTitle("Sign Up");
            Scene signUpScene = new Scene(signUpRoot);
            signUpScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/styles.css")).toExternalForm());
            signUpStage.setScene(signUpScene);
            signUpStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            signUpStage.initOwner(profilePicture.getScene().getWindow());
            signUpStage.showAndWait();
        } catch (IOException e) {
            System.err.println("Error Opening Sign Up Window" + e.getMessage());
            e.printStackTrace();
            showAlert("Cannot Open Sign Up Window");
        }
    }

    /**
     * Applying selected theme into given scene.
     * @param scene The scene where the theme is applied.
     * @param darkMode appears dark, otherwise false.
     */
    private void applyTheme(Scene scene, boolean darkMode) {
        String lightTheme = getClass().getResource(light_Theme).toExternalForm();
        String darkTheme = getClass().getResource(dark_Theme).toExternalForm();
        scene.getStylesheets().remove(lightTheme);
        scene.getStylesheets().remove(darkTheme);
        if (darkMode) {
            scene.getStylesheets().add(darkTheme);
        } else {
            scene.getStylesheets().add(lightTheme);
        }
    }
}
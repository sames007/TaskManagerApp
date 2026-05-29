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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import jfxtras.scene.control.agenda.Agenda;
import jfxtras.scene.control.agenda.Agenda.AppointmentImplLocal;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.prefs.Preferences;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.scene.image.ImageView;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Controller for managing tasks. It handles adding, editing,
 * marking tasks complete, and deleting tasks.
 */
public class TaskManagerController extends Application {
    private static final Logger LOGGER = Logger.getLogger(TaskManagerController.class.getName());
    private static final long MAX_CSV_IMPORT_BYTES = 2_000_000;


    @FXML public Button importBttn;
    @FXML public Button exportBttn;
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
    @FXML private VBox mainVBox;

    private Agenda agenda; // JFXtras Agenda control
    private ObservableList<Task> tasks = FXCollections.observableArrayList();
    private DatabaseManager dbManager;
    private final LocalTaskStore localTaskStore = new LocalTaskStore();
    private ProfileManager profileManager;
    private UserSession currentUser;
    private final BooleanProperty hasPendingNotification = new SimpleBooleanProperty(false);
    private ContextMenu currentContextMenu = null;
    /**
     * Setter for DatabaseManager instance.
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
        Preferences prefs = Preferences.userNodeForPackage(getClass());

        // Restore & save column widths
        for (int i = 0; i < taskTable.getColumns().size(); i++) {
            final int idx = i;
            TableColumn<Task,?> col = taskTable.getColumns().get(i);
            double savedWidth = prefs.getDouble("colWidth" + idx, col.getPrefWidth());
            col.setPrefWidth(savedWidth);
            col.widthProperty().addListener((obs, oldW, newW) ->
                    prefs.putDouble("colWidth" + idx, newW.doubleValue())
            );
        }

        // Capture the built-in sort policy before overriding
        Callback<TableView<Task>,Boolean> defaultSortPolicy = taskTable.getSortPolicy();

        // Restore saved sort order
        String savedOrder = prefs.get("tableSortOrder", "");
        if (!savedOrder.isBlank()) {
            for (String idxStr : savedOrder.split(",")) {
                int idx = Integer.parseInt(idxStr);
                if (idx < taskTable.getColumns().size()) {
                    taskTable.getSortOrder().add(taskTable.getColumns().get(idx));
                }
            }
        }

        // Override sort policy: save prefs, then delegate to default
        taskTable.setSortPolicy(tv -> {
            // persist new sort order
            String order = tv.getSortOrder().stream()
                    .map(tv.getColumns()::indexOf)
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
            prefs.put("tableSortOrder", order);

            // delegate back to JavaFX's original sort logic
            return defaultSortPolicy.call(tv);
        });

        // Apply constrained-resize to columns
        taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Restore & save center-VBox height
        double savedHeight = prefs.getDouble("mainVBoxHeight", mainVBox.getPrefHeight());
        mainVBox.setPrefHeight(savedHeight);
        mainVBox.heightProperty().addListener((obs, oldH, newH) ->
                prefs.putDouble("mainVBoxHeight", newH.doubleValue())
        );

        // Initialize profile manager
        profileManager = new ProfileManager(this);
        if (profilePicture != null && themeToggleBtn != null) {
            profileManager.initialize(profilePicture, themeToggleBtn);
        } else {
            LOGGER.warning("Profile picture ImageView or theme toggle not found in FXML.");
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
        if (notificationIndicator != null) {
            notificationIndicator.visibleProperty().bind(hasPendingNotification);
            notificationIndicator.managedProperty().bind(hasPendingNotification);
        } else {
            LOGGER.warning("Notification indicator not found in FXML.");
        }

        // --- Create and add the Agenda control programmatically, now named "agenda" ---
        agenda = new Agenda();
        agenda.setAllowDragging(true);
        agenda.setAllowResize(true);

        // Add the agenda control to the calendar container in the UI
        if (agendaVbox != null) {
            agendaVbox.getChildren().add(agenda);
        } else {
            LOGGER.warning("Agenda container is missing from FXML.");
        }

        if (addTaskButton != null) {
            addTaskButton.setOnAction(event -> showTaskDialog(null));
        } else {
            LOGGER.warning("Add task button is missing from FXML.");
        }

        if (notificationBtn != null) {
            notificationBtn.setOnAction(event -> showNotificationSettings());
        } else {
            LOGGER.warning("Notification button is missing from FXML.");
        }

        if (calendarView != null) {
            calendarView.setOnAction(event -> {
                LocalDate selectedDate = calendarView.getValue();
                if (selectedDate != null && agenda != null) {
                    agenda.setDisplayedLocalDateTime(selectedDate.atStartOfDay());
                }
            });
        }

        if (markCompleteBtn != null) {
            markCompleteBtn.setOnAction(event -> markTaskComplete());
        } else {
            LOGGER.warning("Mark complete button is missing from FXML.");
        }

        if (deleteBtn != null) {
            deleteBtn.setOnAction(event -> deleteTask());
        } else {
            LOGGER.warning("Delete button is missing from FXML.");
        }

        Platform.runLater(this::refreshAgendaAppointments);

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

        NotificationService.startNotificationService(this);

        tasks.addListener((ListChangeListener<Task>) change -> {
            NotificationService.checkDueTasks(this);
            refreshUpcomingPreview();
        });

        hasPendingNotification.bind(Bindings.createBooleanBinding(
                () -> tasks.stream().anyMatch(NotificationService::shouldNotifyTask),
                tasks
        ));

        Platform.runLater(() -> {
            Scene scene = mainVBox == null ? null : mainVBox.getScene();
            if (scene != null) {
                ThemeManager.bindToSystemTheme(scene, themeToggleBtn);
            }
        });
    }

    /**
     * Exports the current list of tasks to a CSV file.
     * This method opens a file chooser dialog allowing the user to specify the
     * location and name of the output CSV file. It writes each task as a line in
     * the file, including the following fields:
     * Description, DueDate, DueTime, Priority, Status, Category, and Reminder.
     * Fields that contain commas or quotes are properly escaped to ensure CSV
     * format compatibility. If the export is successful, a confirmation alert
     * is shown. If an error occurs (e.g., I/O error), an alert displays the
     * corresponding error message.
     * Example output line:
     * "Finish report",2025-05-10,14:30,HIGH, In Progress, Work,2025-05-08
     */
    @FXML
    private void exportCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Tasks to CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(taskTable.getScene().getWindow());

        if (file != null) {
            try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                writer.write("Description,DueDate,DueTime,Priority,Status,Category,Reminder\n");
                for (Task task : tasks) {
                    writer.write(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                            escapeCsv(task.getDescription()),
                            task.getDueDate(),
                            task.getDueTime(),
                            task.getPriority(),
                            task.getStatus(),
                            task.getCategory(),
                            task.getReminder() != null ? task.getReminder() : ""
                    ));
                }
                showAlert("Tasks successfully exported.");
            } catch (IOException e) {
                showAlert("Error exporting CSV: " + e.getMessage());
            }
        }
    }

    /**
     * @param input the string to escape
     * @return the escaped string
     */
    @NotNull
    private String escapeCsv(String input) {
        if (input == null) return "";
        if (input.contains(",") || input.contains("\"")) {
            return "\"" + input.replace("\"", "\"\"") + "\"";
        }
        return input;
    }

    /**
     * Imports tasks from a user-selected CSV file and adds them to the task list.
     */
    @FXML
    private void importCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Tasks from CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showOpenDialog(taskTable.getScene().getWindow());

        if (file != null) {
            if (!isAllowedCsvImport(file)) {
                showAlert("Please choose a CSV file under 2 MB.");
                return;
            }

            int importedCount = 0;
            int skippedCount = 0;

            try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                String line = reader.readLine(); // Skip header
                int lineNumber = 2; // Header is line 1

                while ((line = reader.readLine()) != null) {
                    try {
                        String[] tokens = parseCsvLine(line);
                        if (tokens.length < 6) {
                            LOGGER.warning("[CSV Import] Skipped line " + lineNumber + ": Not enough fields.");
                            skippedCount++;
                            lineNumber++;
                            continue;
                        }

                        Task task = getTask(tokens);

                        addImportedTask(task);
                        importedCount++;
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING,
                                "[CSV Import] Skipped line " + lineNumber + ": " + e.getMessage());
                        skippedCount++;
                    }
                    lineNumber++;
                }

                // Alert summary to user
                if (skippedCount > 0) {
                    showAlert("Import completed with issues:\n" +
                            importedCount + " task(s) added, " +
                            skippedCount + " line(s) skipped due to formatting errors.\n" +
                            "Check console for details.");
                } else {
                    showAlert("Successfully imported " + importedCount + " task(s).");
                }

            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "[CSV Import] Failed to read the selected file.", e);
                showAlert("Failed to read the selected file.");
            }
        }
    }

    /**
     * @param tokens the array of tokens
     * @return the token value
     */
    @NotNull
    private static Task getTask(@NotNull String[] tokens) {
        String description = tokens[0];
        LocalDate dueDate = LocalDate.parse(tokens[1]);
        LocalTime dueTime = LocalTime.parse(tokens[2]);
        String priority = tokens[3];
        String status = tokens[4];
        String category = tokens[5];
        LocalDate reminder = (tokens.length > 6 && !tokens[6].isEmpty()) ? LocalDate.parse(tokens[6]) : null;

        Task task = new Task(description, dueDate, dueTime, priority);
        task.setStatus(status);
        task.setCategory(category);
        task.setReminder(reminder);
        return task;
    }

    /**
     * Parses a single line of a CSV file into an array of string tokens.
     * Handles fields enclosed in double quotes to ensure proper parsing of
     * values containing commas or other special characters.
     * @param line the CSV line to parse; must not be null
     * @return an array of string tokens representing the fields in the CSV line
     */
    @NotNull
    private String[] parseCsvLine(@NotNull String line) {
        List<String> tokens = new ArrayList<>();
        boolean insideQuote = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                if (insideQuote && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    sb.append('\"');
                    i++;
                } else {
                    insideQuote = !insideQuote;
                }
            } else if (c == ',' && !insideQuote) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim()); // add last token
        return tokens.toArray(new String[0]);
    }

    /**
     * Loads tasks from the database when available, otherwise from the local JSON store.
     */
    private void loadTasksFromDb() {
        tasks.clear();
        if (currentUser == null) {
            taskTable.refresh();
            Platform.runLater(this::refreshAgendaAppointments);
            return;
        }

        if (shouldUseDatabaseForCurrentUser()) {
            try {
                dbManager.loadTasks(tasks, currentUser.getUserID());
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Database task load failed; using local task store.", e);
                tasks.setAll(localTaskStore.loadTasks(currentUser));
            }
        } else {
            tasks.setAll(localTaskStore.loadTasks(currentUser));
        }

        taskTable.refresh();
        Platform.runLater(() -> {
            refreshAgendaAppointments();
            refreshUpcomingPreview();
        });
    }

    private boolean isAllowedCsvImport(File file) {
        return file.isFile()
                && file.length() <= MAX_CSV_IMPORT_BYTES
                && file.getName().toLowerCase(Locale.ROOT).endsWith(".csv");
    }

    /**
     * Refreshes the Agenda control with appointments based on the current tasks list.
     */
    private void refreshAgendaAppointments() {
        if (agenda == null) {
            LOGGER.warning("Agenda control is not initialized; cannot refresh appointments.");
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
                    LOGGER.log(Level.WARNING, "Error creating appointment for task.", e);
                }
            }
        }

        if (agenda.getSkin() != null) {
            agenda.refresh();
        }
    }

    private void refreshUpcomingPreview() {
        if (previewPane == null) {
            return;
        }

        previewPane.getChildren().clear();
        List<Task> upcomingTasks = tasks.stream()
                .filter(task -> !"Completed".equalsIgnoreCase(task.getStatus()))
                .filter(task -> task.getDueDate() != null)
                .sorted(Comparator.comparing(this::getTaskDueDateTime))
                .limit(4)
                .toList();

        if (upcomingTasks.isEmpty()) {
            Label emptyLabel = new Label("No upcoming tasks yet.");
            emptyLabel.getStyleClass().add("empty-state");
            previewPane.getChildren().add(emptyLabel);
            return;
        }

        for (Task task : upcomingTasks) {
            VBox card = new VBox(4);
            card.getStyleClass().add("upcoming-card");

            HBox header = new HBox(8);
            Circle priorityDot = new Circle(5);
            priorityDot.setStyle(getPriorityColor(task.getPriority()));

            Label title = new Label(task.getDescription());
            title.getStyleClass().add("upcoming-title");
            title.setWrapText(true);
            header.getChildren().addAll(priorityDot, title);

            Label metadata = new Label(formatTaskMetadata(task));
            metadata.getStyleClass().add("upcoming-meta");
            metadata.setWrapText(true);

            card.getChildren().addAll(header, metadata);
            previewPane.getChildren().add(card);
        }
    }

    private LocalDateTime getTaskDueDateTime(Task task) {
        return LocalDateTime.of(
                task.getDueDate(),
                task.getDueTime() == null ? LocalTime.of(9, 0) : task.getDueTime()
        );
    }

    private String formatTaskMetadata(Task task) {
        String dueTime = task.getDueTime() == null ? "No time" : task.getDueTime().toString();
        String category = task.getCategory() == null || task.getCategory().isBlank()
                ? "Uncategorized"
                : task.getCategory();
        return task.getDueDate() + " at " + dueTime + " | " + category + " | " + task.getPriority();
    }

    /**
     * @param priority the priority of the task
     * @return Priority of the task
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

        String previousStatus = selectedTask.getStatus();
        try {
            selectedTask.setStatus("Completed");
            if (currentUser == null) {
                throw new IllegalStateException("Please log in before updating tasks.");
            }
            if (shouldUseDatabaseForCurrentUser()) {
                try {
                    dbManager.updateTask(selectedTask, currentUser.getUserID());
                } catch (RuntimeException e) {
                    LOGGER.log(Level.WARNING, "Database status update failed; saving task locally.", e);
                    if (!saveTasksLocallySafely()) {
                        throw e;
                    }
                }
            } else if (!saveTasksLocallySafely()) {
                throw new IllegalStateException("Unable to save task locally.");
            }
            taskTable.refresh();
            refreshAgendaAppointments();
            refreshUpcomingPreview();
        } catch (Exception e) {
            showAlert("Error updating task status: " + e.getMessage());
            selectedTask.setStatus(previousStatus);
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
                    if (currentUser == null) {
                        throw new IllegalStateException("Please log in before deleting tasks.");
                    }
                    boolean deletedInDatabase = false;
                    if (shouldUseDatabaseForCurrentUser()) {
                        try {
                            dbManager.deleteTask(selectedTask.getTaskID(), currentUser.getUserID());
                            deletedInDatabase = true;
                        } catch (RuntimeException e) {
                            LOGGER.log(Level.WARNING, "Database delete failed; deleting task locally.", e);
                        }
                    }
                    tasks.remove(selectedTask);
                    if (!deletedInDatabase) {
                        saveTasksLocallySafely();
                    }
                    taskTable.refresh();
                    refreshAgendaAppointments();
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
            LOGGER.warning("Invalid time input provided.");
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
            LOGGER.warning("Invalid AM/PM value.");
            return null;
        }

        try {
            return LocalTime.of(convertedHour, minute);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error creating LocalTime.", e);
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
                LOGGER.warning("Could not set owner for dialog stage.");
            }
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            Scene scene = new Scene(root);
            try {
                scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/addTaskDialog.css")).toExternalForm());
                ThemeManager.bindToSystemTheme(scene);
            } catch (NullPointerException e) {
                LOGGER.log(Level.WARNING, "Could not load one or more CSS files for dialog.", e);
            }
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            dialogStage.showAndWait();

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error loading add/edit task dialog.", e);
            showAlert("CANNOT Open Task Dialog - Check Files");
        } catch (IllegalStateException e) {
            LOGGER.log(Level.WARNING, "Error during FXML loading.", e);
            showAlert("Error Occurred While Initializing");
        } catch (NullPointerException e) {
            LOGGER.log(Level.WARNING, "Error during dialog setup.", e);
            showAlert("An internal error occurred opening the dialog.");
        }
    }

    /**
     * Adds a new task received from the dialog, updates DB, and refreshes UI.
     * This method is called by AddTaskDialogController.
     * @param task The new task created in the dialog.
     */
    public void addNewTaskFrom(Task task) {
        addTaskToActiveStore(task);
    }

    private boolean addTaskFromAssistant(Task task) {
        return addTaskToActiveStore(task);
    }

    private boolean addTaskToActiveStore(Task task) {
        if (task == null) {
            LOGGER.warning("Attempted to add a null task.");
            return false;
        }
        if (currentUser == null) {
            showAlert("Please log in before adding tasks.");
            return false;
        }

        tasks.add(task);

        if (shouldUseDatabaseForCurrentUser()) {
            try {
                dbManager.addTask(task, currentUser.getUserID());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Database save failed; saving task locally instead.", e);
                if (!saveTasksLocallySafely()) {
                    refreshTaskViews();
                    return false;
                }
            }
        } else if (!saveTasksLocallySafely()) {
            refreshTaskViews();
            return false;
        }

        refreshTaskViews();
        return true;
    }

    /**
     * Adds an imported task (from a file) to the list and refreshes the table.
     * @param task the task to add
     */
    public void addImportedTask(Task task) {
        addTaskToActiveStore(task);
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
            ChatBoxController chatController = chatLoader.getController();
            chatController.configure(currentUser, this::addTaskFromAssistant);
            Stage chatStage = new Stage();
            chatStage.setTitle("AI Chat Assist");
            Scene chatScene = new Scene(chatRoot);
            chatScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styling/ChatBox.css")).toExternalForm());
            ThemeManager.bindToSystemTheme(chatScene);
            chatStage.setScene(chatScene);
            chatStage.show();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error opening chat window.", e);
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
            LoginController loginController = loginLoader.getController();
            loginController.setMainController(this);
            Stage loginStage = new Stage();
            loginStage.setTitle("Login");
            Scene loginScene = new Scene(loginRoot);
            ThemeManager.bindToSystemTheme(loginScene);
            loginStage.setScene(loginScene);
            loginStage.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error opening login window.", e);
            showAlert("Cannot Open Login Window.");
        }
    }

    /**
     * Opens the Sign-Up window when the user clicks the "Sign Up" button.
     */
    @FXML
    private void displaySignUp() {
        try {
            FXMLLoader signUpLoader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/SignUpView.fxml"));
            if (signUpLoader.getLocation() == null) {
                throw new IOException("Cannot Find FXML File: SignUpView.fxml");
            }
            Parent signUpRoot = signUpLoader.load();
            SignUpController signUpController = signUpLoader.getController();
            signUpController.setMainController(this);
            Stage signUpStage = new Stage();
            signUpStage.setTitle("Sign Up");
            Scene signUpScene = new Scene(signUpRoot);
            ThemeManager.bindToSystemTheme(signUpScene);
            signUpStage.setScene(signUpScene);
            signUpStage.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error opening sign-up window.", e);
            showAlert("Cannot Open Sign Window");
        }
    }

    /**
     * @param taskToEdit the task to edit
     */
    public void updateTaskFrom(Task taskToEdit) {
        if (taskToEdit == null) {
            LOGGER.warning("updateTaskFrom called with a null task.");
            showAlert("Cannot update task: No task data received.");
            return;
        }

        if (currentUser == null) {
            showAlert("Please log in before updating tasks.");
            return;
        }

        if (shouldUseDatabaseForCurrentUser()) {
            try {
                dbManager.updateTask(taskToEdit, currentUser.getUserID());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Database task update failed; saving task locally.", e);
                saveTasksLocallySafely();
            }

        } else {
            saveTasksLocallySafely();
        }

        taskTable.refresh();
        refreshAgendaAppointments();
        refreshUpcomingPreview();
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

        DialogPane dialogPane = dialog.getDialogPane();
        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20;");

        Label headerLabel = new Label("Upcoming Tasks");
        headerLabel.setStyle(
            "-fx-font-size: 24px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #2c3e50;"
        );

        Label settingsLabel = new Label(
            "- Notifications are enabled for tasks due within 24 hours\n" +
            "- Only incomplete tasks will trigger notifications\n" +
            "- Notifications will appear in the bottom-right corner"
        );
        settingsLabel.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-text-fill: #34495e;"
        );

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

        List<Task> sortedTasks = tasks.stream()
                .filter(task -> !"Completed".equalsIgnoreCase(task.getStatus()))
                .filter(task -> task.getDueDate() != null)
                .sorted(Comparator.comparing(this::getTaskDueDateTime))
                .toList();

        for (Task task : sortedTasks) {
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

                HBox taskHeader = new HBox(10);
                Circle priorityIndicator = new Circle(6);
                priorityIndicator.setStyle(getPriorityColor(task.getPriority()));

                Label taskDesc = new Label(task.getDescription());
                taskDesc.setStyle(
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #2c3e50;"
                );

                taskHeader.getChildren().addAll(priorityIndicator, taskDesc);

                Label dueInfo = new Label(String.format("Due in %d hours - %s",
                    hoursUntilDue, task.getCategory() == null ? "Uncategorized" : task.getCategory()));
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

        ScrollPane scrollPane = new ScrollPane(tasksContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(200);
        scrollPane.setStyle(
            "-fx-background: transparent;" +
            "-fx-background-color: transparent;"
        );

        content.getChildren().addAll(
            headerLabel,
            settingsLabel,
            separator,
            upcomingTasksLabel,
            scrollPane
        );

        dialogPane.setContent(content);
        dialogPane.getStyleClass().add("notification-settings-dialog");
        dialogPane.setStyle(
            "-fx-background-color: #f5f6fa;" +
            "-fx-padding: 20;"
        );

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

    /**
     * Updates the notification indicator for tasks that are due soon.
     */
    public void setPendingNotification(boolean anyDueSoon) {
        hasPendingNotification.set(anyDueSoon);
    }

    /**
     * @param priority the priority of the task
     * @return Priority color string
     */
    @NotNull
    @Contract(pure = true)
    private String getPriorityColor(String priority) {
        if (priority == null) return "-fx-fill: #95a5a6;";

        return switch (priority.toLowerCase()) {
            case "extreme" -> "-fx-fill: #e74c3c;";
            case "high" -> "-fx-fill: #e67e22;";
            case "medium" -> "-fx-fill: #f1c40f;";
            case "low" -> "-fx-fill: #2ecc71;";
            default -> "-fx-fill: #95a5a6;";
        };
    }

    /**
     * The controller is not launched as a standalone JavaFX application.
     */
    @Override
    public void start(Stage primaryStage) {
    }

    /**
     * Stops notification resources when the controller is closed.
     */
    @Override
    public void stop() {
        NotificationService.stopNotificationService();
    }

    /**
     * Called when the user clicks the "Logout" button.
     */
    public void handleLogout() {
        currentUser = null;
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
        this.currentUser = user;
        if (profileManager != null) {
            profileManager.setCurrentUser(user);
        }
        if (user != null) {
            String mode = isDatabaseAvailable() ? "Connected" : "Offline";
            welcomeLabel.setText("Welcome, " + user.getUserName() + " | " + mode + " mode");
            loadTasksFromDb(); // Reload tasks for the new user
        } else {
            welcomeLabel.setText("Welcome!");
            tasks.clear();
        }
    }

    public DatabaseManager getDbManager() {
        return dbManager;
    }

    public boolean isDatabaseAvailable() {
        return dbManager != null && dbManager.isAvailable();
    }

    private boolean shouldUseDatabaseForCurrentUser() {
        return currentUser != null && currentUser.getUserID() > 0 && isDatabaseAvailable();
    }

    private void saveTasksLocally() {
        if (currentUser != null) {
            localTaskStore.saveTasks(currentUser, tasks);
        }
    }

    private boolean saveTasksLocallySafely() {
        try {
            saveTasksLocally();
            return true;
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Error saving task list to local storage.", e);
            showAlert("Error Saving Task Locally.");
            return false;
        }
    }

    private void refreshTaskViews() {
        taskTable.refresh();
        refreshAgendaAppointments();
        refreshUpcomingPreview();
    }

    /**
     * Shows the login screen
     */
    public void showLoginScreen() {
        try {
            FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/edu/farmingdale/taskmanagerapp/LoginView.fxml"));
            Parent loginRoot = loginLoader.load();

            LoginController loginController = loginLoader.getController();
            loginController.setMainController(this); // Changed to setMainController to match the pattern from SignUpController

            Stage loginStage = new Stage();
            loginStage.setTitle("Login");
            Scene loginScene = new Scene(loginRoot);
            ThemeManager.bindToSystemTheme(loginScene);
            loginStage.setScene(loginScene);
            loginStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            loginStage.initOwner(profilePicture.getScene().getWindow());
            loginStage.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error opening login window.", e);
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
            ThemeManager.bindToSystemTheme(signUpScene);
            signUpStage.setScene(signUpScene);
            signUpStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            signUpStage.initOwner(profilePicture.getScene().getWindow());
            signUpStage.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error opening sign-up window.", e);
            showAlert("Cannot Open Sign Up Window");
        }
    }

    /**
     * Applying the selected theme into the given scene.
     * @param scene The scene where the theme is applied.
     * @param darkMode appears dark, otherwise false.
     */
    void applyTheme(@NotNull Scene scene, boolean darkMode) {
        ThemeManager.applyTheme(scene, darkMode);
    }
}

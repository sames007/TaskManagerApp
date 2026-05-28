package edu.farmingdale.taskmanagerapp;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller that handles file drag and drop to import tasks.
 */
public class FileProcessorController {
    private static final Logger LOGGER = Logger.getLogger(FileProcessorController.class.getName());
    private static final long MAX_IMPORT_BYTES = 1_000_000;

    @FXML private VBox dropZone;
    @FXML private Label dropLabel;
    private TaskManagerController taskManagerController;
    private DatabaseManager dbManager;

    /**
     * Called automatically when the FXML file is loaded.
     * Sets up the drag and drop behavior.
     */
    public void initialize() {
        setupDragAndDrop();
    }

    /**
     * Sets the DatabaseManager instance.
     * @param dbManager the DatabaseManager to use.
     */
    public void setDatabaseManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Allows the main TaskManagerController to be set so imported tasks can be added.
     * @param controller the TaskManagerController
     */
    public void setTaskManagerController(TaskManagerController controller) {
        this.taskManagerController = controller;
    }

    private void setupDragAndDrop() {
        dropZone.setOnDragOver(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropZone.setOnDragEntered(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                dropZone.getStyleClass().add("drop-zone-hover");
                dropLabel.setText("Drop file to import tasks");
            }
            event.consume();
        });

        dropZone.setOnDragExited(event -> {
            dropZone.getStyleClass().remove("drop-zone-hover");
            dropLabel.setText("Drag and drop task file here");
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                success = true;
                File file = db.getFiles().get(0);
                processFile(file);
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    /**
     * Reads the file line by line and sends its content for processing.
     * @param file the file to be processed
     */
    private void processFile(File file) {
        if (!isAllowedImportFile(file)) {
            showError("Please import a .txt or .csv file under 1 MB.");
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            processFileContent(content.toString());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading imported file.", e);
            showError("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error processing imported file.", e);
            showError("Error processing file: " + e.getMessage());
        }
    }

    /**
     * Splits the file content into sections, extracts task information,
     * and creates tasks in the TaskManagerController.
     * @param content the complete file content as a String
     */
    private void processFileContent(@NotNull String content) {
        String[] sections = content.split("\n\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (String section : sections) {
            String[] lines = section.split("\n");
            String taskName = "";
            LocalDate deadline = null;
            String description = "";
            String priority = "Medium";
            LocalTime dueTime = null;
            for (String line : lines) {
                if (line.startsWith("Task Name:")) {
                    taskName = line.replace("Task Name:", "").trim();
                } else if (line.startsWith("Deadline:")) {
                    String dateStr = line.replace("Deadline:", "").trim();
                    try {
                        deadline = LocalDate.parse(dateStr, formatter);
                    } catch (Exception e) {
                        continue;
                    }
                } else if (line.startsWith("Description:")) {
                    description = line.replace("Description:", "").trim();
                } else if (line.startsWith("Priority:")) {
                    priority = line.replace("Priority:", "").trim();
                } else if (line.startsWith("Due Time:")) {
                    String timeStr = line.replace("Due Time:", "").trim();
                    try {
                        String[] timeParts = timeStr.split(":");
                        int hour = Integer.parseInt(timeParts[0]);
                        int minute = Integer.parseInt(timeParts[1]);
                        dueTime = LocalTime.of(hour, minute);
                    } catch (Exception e) {
                        dueTime = LocalTime.of(0, 0);
                    }
                }
            }
            if (!taskName.isEmpty() && deadline != null) {
                Task task = new Task(
                        taskName + (description.isEmpty() ? "" : " - " + description),
                        deadline,
                        dueTime != null ? dueTime : LocalTime.of(0, 0),
                        priority
                );

                if (taskManagerController != null) {
                    taskManagerController.addImportedTask(task);
                } else if (dbManager != null && dbManager.isAvailable()) {
                    dbManager.addTask(task);
                }
            }
        }
    }

    private boolean isAllowedImportFile(File file) {
        if (file == null || !file.isFile() || file.length() > MAX_IMPORT_BYTES) {
            return false;
        }

        String name = file.getName().toLowerCase();
        return name.endsWith(".txt") || name.endsWith(".csv");
    }

    /**
     * Shows an error alert with the provided message.
     * @param message the error message to show
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

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
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller that handles file drag and drop to import tasks.
 */
public class FileProcessorController {
    @FXML private VBox dropZone; // The area where files can be dropped
    @FXML private Label dropLabel; // Label inside the drop zone
    private TaskManagerController taskManagerController; // Reference to the main task controller
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

    /**
     * Sets up drag and drop events for the dropZone.
     * CSS classes are added/removed for visual feedback.
     */
    private void setupDragAndDrop() {
        // Accept files if they are dragged over the drop zone
        dropZone.setOnDragOver(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        // When a file enters the drop zone, update style and label text
        dropZone.setOnDragEntered(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                dropZone.getStyleClass().add("drop-zone-hover");
                dropLabel.setText("Drop file to import tasks");
            }
            event.consume();
        });

        // When a file leaves, remove hover-style and reset label text
        dropZone.setOnDragExited(event -> {
            dropZone.getStyleClass().remove("drop-zone-hover");
            dropLabel.setText("Drag and drop task file here");
            event.consume();
        });

        // When a file is dropped, process the file and mark the event as completed
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
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            // Read each line from the file
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            processFileContent(content.toString());
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error processing file: " + e.getMessage());
        }
    }

    /**
     * Splits the file content into sections, extracts task information,
     * and creates tasks in the TaskManagerController.
     * @param content the complete file content as a String
     */
    private void processFileContent(@NotNull String content) {
        // Split the file into sections separated by blank lines
        String[] sections = content.split("\n\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (String section : sections) {
            String[] lines = section.split("\n");
            String taskName = "";
            LocalDate deadline = null;
            String description = "";
            String priority = "Medium"; // Default priority
            LocalTime dueTime = null;
            for (String line : lines) {
                if (line.startsWith("Task Name:")) {
                    taskName = line.replace("Task Name:", "").trim();
                } else if (line.startsWith("Deadline:")) {
                    String dateStr = line.replace("Deadline:", "").trim();
                    try {
                        deadline = LocalDate.parse(dateStr, formatter);
                    } catch (Exception e) {
                        // Skip if a date is not valid
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
                        // Default to 00:00 if parsing fails
                        dueTime = LocalTime.of(0, 0);
                    }
                }
            }
            // Create and add the task if the taskName and deadline are valid
            if (!taskName.isEmpty() && deadline != null) {
                Task task = new Task(
                        taskName + (description.isEmpty() ? "" : " - " + description),
                        deadline,
                        dueTime != null ? dueTime : LocalTime.of(0, 0), // Use parsed due time if available
                        priority
                );

                // Add the task to TaskManagerController
                if (taskManagerController != null) {
                    taskManagerController.addImportedTask(task);
                }

                // Save the task to the database if available
                if (dbManager != null) {
                    dbManager.addTask(task);
                }
            }
        }
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

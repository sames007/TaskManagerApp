package edu.farmingdale.taskmanagerapp;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
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
    private TaskManagerController taskManagerController; // Reference to main task controller

    /**
     * Called automatically when the FXML file is loaded.
     * Sets up the drag and drop behavior.
     */
    public void initialize() {
        setupDragAndDrop();
    }

    private DatabaseManager dbManager;

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
     * Instead of inline styles, CSS classes are added/removed.
     */
    private void setupDragAndDrop() {
        // When a file is dragged over the drop zone, accept it if files exist
        dropZone.setOnDragOver(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        // When a file enters the drop zone, add a hover CSS class and change the label text
        dropZone.setOnDragEntered(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                dropZone.getStyleClass().add("drop-zone-hover");
                dropLabel.setText("Drop file to import tasks");
            }
            event.consume();
        });

        // When a file leaves the drop zone, remove the hover CSS class and reset the label text
        dropZone.setOnDragExited(event -> {
            dropZone.getStyleClass().remove("drop-zone-hover");
            dropLabel.setText("Drag and drop task file here");
            event.consume();
        });

        // When a file is dropped, process the first file and mark the event as completed
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
    private LocalTime dueTime; // Add this field to processFileContent method scope

    private void processFileContent(String content) {
        // Split the file into sections separated by blank lines
        String[] sections = content.split("\n\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (String section : sections) {
            String[] lines = section.split("\n");
            String taskName = "";
            LocalDate deadline = null;
            String description = "";
            String priority = "Medium"; // Default priority
            dueTime = null; // Reset dueTime for each task
            for (String line : lines) {
                if (line.startsWith("Task Name:")) {
                    taskName = line.replace("Task Name:", "").trim();
                } else if (line.startsWith("Deadline:")) {
                    String dateStr = line.replace("Deadline:", "").trim();
                    try {
                        deadline = LocalDate.parse(dateStr, formatter);
                    } catch (Exception e) {
                        // Skip if date is not valid
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
                        // Handle invalid time format
                        dueTime = LocalTime.of(0, 0); // Default to 00:00 if parsing fails
                    }
                }
            }
            // Create and add the task if taskName and deadline are valid
            if (!taskName.isEmpty() && deadline != null) {
                Task task = new Task(
                        taskName + (description.isEmpty() ? "" : " - " + description),
                        deadline,
                        dueTime != null ? dueTime : LocalTime.of(0, 0), // Use parsed due time if available
                        priority
                );

                // Add task to TaskManagerController
                taskManagerController.addImportedTask(task);

                // Save task to database
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
package edu.farmingdale.taskmanagerapp;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDate;

public class TaskManagerController {
    @FXML private TextField taskInput;
    @FXML private DatePicker dueDatePicker;
    @FXML private ComboBox<String> priorityComboBox;
    @FXML private ComboBox<String> categoryComboBox; // New for categorization
    @FXML private DatePicker reminderDatePicker;     // New for reminder
    @FXML private TableView<Task> taskTable;
    @FXML private TableColumn<Task, String> taskColumn;
    @FXML private TableColumn<Task, LocalDate> dueDateColumn;
    @FXML private TableColumn<Task, String> priorityColumn;
    @FXML private TableColumn<Task, String> statusColumn;

    private ObservableList<Task> tasks = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Initialize priority options
        priorityComboBox.setItems(FXCollections.observableArrayList("High", "Medium", "Low"));
        // Initialize category options
        categoryComboBox.setItems(FXCollections.observableArrayList("School", "Work", "Personal"));

        // Initialize table columns
        taskColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        taskTable.setItems(tasks);
    }

    @FXML
    private void addTask() {
        String description = taskInput.getText().trim();
        LocalDate dueDate = dueDatePicker.getValue();
        String priority = priorityComboBox.getValue();
        String category = categoryComboBox.getValue();
        LocalDate reminder = reminderDatePicker.getValue();

        if (!description.isEmpty() && dueDate != null && priority != null && category != null) {
            Task task = new Task(description, dueDate, priority);
            task.setCategory(category);
            task.setReminder(reminder); // Can be null if not set
            tasks.add(task);
            clearInputs();
        } else {
            showAlert("Please fill in all required fields (Task, Due Date, Priority, Category)");
        }
    }

    @FXML
    private void markTaskComplete() {
        Task selectedTask = taskTable.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            selectedTask.setStatus("Completed");
            taskTable.refresh();
        } else {
            showAlert("Please select a task to mark as complete");
        }
    }

    @FXML
    private void deleteTask() {
        Task selectedTask = taskTable.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            tasks.remove(selectedTask);
        } else {
            showAlert("Please select a task to delete");
        }
    }

    // New: Edit Task functionality
    @FXML
    private void editTask() {
        Task selectedTask = taskTable.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            // Load values from the selected task into the input fields
            taskInput.setText(selectedTask.getDescription());
            dueDatePicker.setValue(selectedTask.getDueDate());
            priorityComboBox.setValue(selectedTask.getPriority());
            categoryComboBox.setValue(selectedTask.getCategory());
            reminderDatePicker.setValue(selectedTask.getReminder());

            // Remove the task temporarily; it will be re-added with updates
            tasks.remove(selectedTask);
        } else {
            showAlert("Please select a task to edit");
        }
    }

    private void clearInputs() {
        taskInput.clear();
        dueDatePicker.setValue(null);
        priorityComboBox.setValue(null);
        categoryComboBox.setValue(null);
        reminderDatePicker.setValue(null);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void addImportedTask(Task task) {
        tasks.add(task);
        taskTable.refresh();
    }
}

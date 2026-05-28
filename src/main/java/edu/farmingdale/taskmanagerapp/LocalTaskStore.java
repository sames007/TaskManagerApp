package edu.farmingdale.taskmanagerapp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lightweight JSON persistence used when the database is unavailable.
 */
class LocalTaskStore {
    private static final Logger LOGGER = Logger.getLogger(LocalTaskStore.class.getName());
    private static final Path DEFAULT_STORE = Path.of(
            System.getProperty("user.home"),
            ".taskmanagerapp",
            "tasks.json"
    );
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path storePath;

    LocalTaskStore() {
        this(DEFAULT_STORE);
    }

    LocalTaskStore(Path storePath) {
        this.storePath = storePath;
    }

    List<Task> loadTasks(UserSession user) {
        StoreData data = readStore();
        List<TaskRecord> records = data.tasksByUser.getOrDefault(userKey(user), List.of());
        List<Task> tasks = new ArrayList<>();
        for (TaskRecord record : records) {
            Task task = record.toTask();
            if (task != null) {
                tasks.add(task);
            }
        }
        return tasks;
    }

    void saveTasks(UserSession user, List<Task> tasks) {
        StoreData data = readStore();
        List<TaskRecord> records = new ArrayList<>();
        int nextId = nextTaskId(tasks);

        for (Task task : tasks) {
            if (task.getTaskID() <= 0) {
                task.setTaskID(nextId++);
            }
            records.add(TaskRecord.from(task));
        }

        data.tasksByUser.put(userKey(user), records);
        writeStore(data);
    }

    private StoreData readStore() {
        if (!Files.isRegularFile(storePath)) {
            return new StoreData();
        }

        try (Reader reader = Files.newBufferedReader(storePath, StandardCharsets.UTF_8)) {
            StoreData data = GSON.fromJson(reader, StoreData.class);
            return data == null ? new StoreData() : data;
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "Unable to read local task store; starting with an empty list.", e);
            return new StoreData();
        }
    }

    private void writeStore(StoreData data) {
        try {
            Files.createDirectories(storePath.getParent());
            try (Writer writer = Files.newBufferedWriter(storePath, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to save local tasks.", e);
        }
    }

    private int nextTaskId(List<Task> tasks) {
        return tasks.stream()
                .mapToInt(Task::getTaskID)
                .max()
                .orElse(0) + 1;
    }

    private String userKey(UserSession user) {
        String identifier = user.getEmail() == null || user.getEmail().isBlank()
                ? user.getUserName()
                : user.getEmail();
        return identifier.trim().toLowerCase(Locale.ROOT);
    }

    private static class StoreData {
        private Map<String, List<TaskRecord>> tasksByUser = new HashMap<>();
    }

    private static class TaskRecord {
        private int taskID;
        private String description;
        private String dueDate;
        private String dueTime;
        private String priority;
        private String status;
        private String category;
        private String reminder;

        static TaskRecord from(Task task) {
            TaskRecord record = new TaskRecord();
            record.taskID = task.getTaskID();
            record.description = task.getDescription();
            record.dueDate = task.getDueDate() == null ? null : task.getDueDate().toString();
            record.dueTime = task.getDueTime() == null ? null : task.getDueTime().toString();
            record.priority = task.getPriority();
            record.status = task.getStatus();
            record.category = task.getCategory();
            record.reminder = task.getReminder() == null ? null : task.getReminder().toString();
            return record;
        }

        Task toTask() {
            if (description == null || dueDate == null || priority == null) {
                return null;
            }

            Task task = new Task(
                    description,
                    LocalDate.parse(dueDate),
                    dueTime == null || dueTime.isBlank() ? null : LocalTime.parse(dueTime),
                    priority
            );
            task.setTaskID(taskID);
            task.setStatus(status == null || status.isBlank() ? "Pending" : status);
            task.setCategory(category);
            task.setReminder(reminder == null || reminder.isBlank() ? null : LocalDate.parse(reminder));
            return task;
        }
    }
}

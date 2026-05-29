package edu.farmingdale.taskmanagerapp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalTaskStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsTasksForUser() {
        LocalTaskStore store = new LocalTaskStore(tempDir.resolve("tasks.json"));
        UserSession user = new UserSession("offline", "offline@example.com", "Password123");
        Task task = new Task("Study", LocalDate.now().plusDays(1), LocalTime.of(9, 30), "High");
        task.setStatus("In Progress");
        task.setCategory("School");
        task.setReminder(LocalDate.now());

        store.saveTasks(user, List.of(task));
        List<Task> loadedTasks = store.loadTasks(user);

        assertEquals(1, loadedTasks.size());
        assertTrue(task.getTaskID() > 0);
        assertEquals("Study", loadedTasks.get(0).getDescription());
        assertEquals("School", loadedTasks.get(0).getCategory());
    }

    @Test
    void keepsUsersSeparate() {
        LocalTaskStore store = new LocalTaskStore(tempDir.resolve("tasks.json"));
        UserSession firstUser = new UserSession("first", "first@example.com", "Password123");
        UserSession secondUser = new UserSession("second", "second@example.com", "Password123");

        store.saveTasks(firstUser, List.of(new Task(
                "First task",
                LocalDate.now().plusDays(1),
                LocalTime.NOON,
                "Medium"
        )));

        assertEquals(1, store.loadTasks(firstUser).size());
        assertTrue(store.loadTasks(secondUser).isEmpty());
    }

    @Test
    void savesAfterExistingStoreFileIsEmpty() throws IOException {
        Path storePath = tempDir.resolve("tasks.json");
        Files.writeString(storePath, "");
        LocalTaskStore store = new LocalTaskStore(storePath);
        UserSession user = new UserSession("offline", "offline@example.com", "Password123");

        store.saveTasks(user, List.of(new Task(
                "Recovered task",
                LocalDate.now().plusDays(1),
                LocalTime.NOON,
                "Medium"
        )));

        assertEquals(1, store.loadTasks(user).size());
    }

    @Test
    void repairsStoreWithMissingUserMap() throws IOException {
        Path storePath = tempDir.resolve("tasks.json");
        Files.writeString(storePath, "{}");
        LocalTaskStore store = new LocalTaskStore(storePath);
        UserSession user = new UserSession("offline", "", "Password123");

        store.saveTasks(user, List.of(new Task(
                "Username fallback task",
                LocalDate.now().plusDays(1),
                LocalTime.NOON,
                "Medium"
        )));

        assertEquals(1, store.loadTasks(user).size());
    }
}

package edu.farmingdale.taskmanagerapp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalChatStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsHistoryForUser() {
        LocalChatStore store = new LocalChatStore(tempDir.resolve("chat-history.json"));
        UserSession user = new UserSession("zero", "zero@example.com", "Password123");

        store.saveHistory(user, "zero: hello\nAI: hi\n");

        assertEquals("zero: hello\nAI: hi\n", store.loadHistory(user));
    }

    @Test
    void keepsUserHistoriesSeparate() {
        LocalChatStore store = new LocalChatStore(tempDir.resolve("chat-history.json"));
        UserSession firstUser = new UserSession("first", "first@example.com", "Password123");
        UserSession secondUser = new UserSession("second", "second@example.com", "Password123");

        store.saveHistory(firstUser, "first: plan my day\n");

        assertEquals("first: plan my day\n", store.loadHistory(firstUser));
        assertEquals("", store.loadHistory(secondUser));
    }

    @Test
    void clearHistoryRemovesOnlyCurrentUser() {
        LocalChatStore store = new LocalChatStore(tempDir.resolve("chat-history.json"));
        UserSession firstUser = new UserSession("first", "first@example.com", "Password123");
        UserSession secondUser = new UserSession("second", "second@example.com", "Password123");
        store.saveHistory(firstUser, "first history");
        store.saveHistory(secondUser, "second history");

        store.clearHistory(firstUser);

        assertEquals("", store.loadHistory(firstUser));
        assertEquals("second history", store.loadHistory(secondUser));
    }

    @Test
    void savesAfterExistingStoreFileIsEmpty() throws IOException {
        Path storePath = tempDir.resolve("chat-history.json");
        Files.writeString(storePath, "");
        LocalChatStore store = new LocalChatStore(storePath);
        UserSession user = new UserSession("offline", "offline@example.com", "Password123");

        store.saveHistory(user, "offline: resume this chat\n");

        assertEquals("offline: resume this chat\n", store.loadHistory(user));
    }
}

package edu.farmingdale.taskmanagerapp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores AI chat transcripts locally so each user can resume prior conversations.
 */
class LocalChatStore {
    private static final Logger LOGGER = Logger.getLogger(LocalChatStore.class.getName());
    private static final Path DEFAULT_STORE = Path.of(
            System.getProperty("user.home"),
            ".taskmanagerapp",
            "chat-history.json"
    );
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path storePath;

    LocalChatStore() {
        this(DEFAULT_STORE);
    }

    LocalChatStore(Path storePath) {
        this.storePath = storePath;
    }

    String loadHistory(UserSession user) {
        StoreData data = readStore();
        return data.historyByUser.getOrDefault(userKey(user), "");
    }

    void saveHistory(UserSession user, String history) {
        StoreData data = readStore();
        String key = userKey(user);
        String cleanedHistory = history == null ? "" : history;

        if (cleanedHistory.isBlank()) {
            data.historyByUser.remove(key);
        } else {
            data.historyByUser.put(key, cleanedHistory);
        }

        writeStore(data);
    }

    void clearHistory(UserSession user) {
        StoreData data = readStore();
        data.historyByUser.remove(userKey(user));
        writeStore(data);
    }

    private StoreData readStore() {
        if (!Files.isRegularFile(storePath)) {
            return new StoreData();
        }

        try (Reader reader = Files.newBufferedReader(storePath, StandardCharsets.UTF_8)) {
            StoreData data = GSON.fromJson(reader, StoreData.class);
            return normalizeStoreData(data);
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "Unable to read local chat history; starting with an empty history.", e);
            return new StoreData();
        }
    }

    private void writeStore(StoreData data) {
        Path tempPath = null;
        try {
            Path parent = storePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            tempPath = parent == null
                    ? Files.createTempFile("chat-history", ".json.tmp")
                    : Files.createTempFile(parent, "chat-history", ".json.tmp");
            try (Writer writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }

            try {
                Files.move(tempPath, storePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempPath, storePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            deleteIncompleteTempFile(tempPath);
            throw new IllegalStateException("Unable to save local chat history.", e);
        }
    }

    private void deleteIncompleteTempFile(Path tempPath) {
        if (tempPath == null) {
            return;
        }

        try {
            Files.deleteIfExists(tempPath);
        } catch (IOException cleanupError) {
            LOGGER.log(Level.FINE, "Unable to delete incomplete chat history temp file.", cleanupError);
        }
    }

    private StoreData normalizeStoreData(StoreData data) {
        StoreData normalized = data == null ? new StoreData() : data;
        if (normalized.historyByUser == null) {
            normalized.historyByUser = new HashMap<>();
        }
        return normalized;
    }

    private String userKey(UserSession user) {
        if (user == null) {
            return "local-user";
        }

        String identifier = firstNonBlank(user.getEmail(), user.getUserName());
        return identifier.toLowerCase(Locale.ROOT);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "local-user";
    }

    private static class StoreData {
        private Map<String, String> historyByUser = new HashMap<>();
    }
}

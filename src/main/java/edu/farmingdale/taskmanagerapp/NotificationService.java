package edu.farmingdale.taskmanagerapp;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * The NotificationService class is responsible for managing task due notifications.
 * It periodically checks tasks and displays notifications for tasks that are due soon.
 * Notifications are presented as visually styled pop-ups in the bottom-right corner
 * of the screen.
 * - Regularly checks tasks for due dates within a specific time threshold.
 * - Displays pop-up notifications for tasks that are due soon.
 * - Manages the lifecycle of the notification service and active notifications.
 */
public class NotificationService {
    private static final int NOTIFICATION_DURATION = 5000; // 5 seconds
    private static final int CHECK_INTERVAL = 60000; // Check every minute
    private static final int DUE_SOON_THRESHOLD = 24; // Notify if due within 24 hours
    private static final List<Stage> activeNotifications = new ArrayList<>();
    private static Timeline notificationCheckTimeline;

    /**
     * Starts the notification service.
     * @param controller The main controller
     */
    public static void startNotificationService(TaskManagerController controller) {
        if (notificationCheckTimeline != null) {
            notificationCheckTimeline.stop();
        }

        notificationCheckTimeline = new Timeline(
            new KeyFrame(Duration.millis(CHECK_INTERVAL), event -> checkDueTasks(controller))
        );
        notificationCheckTimeline.setCycleCount(Timeline.INDEFINITE);
        notificationCheckTimeline.play();
    }

    /**
     * @param controller The main controller
     */
    private static void checkDueTasks(TaskManagerController controller) {
        Platform.runLater(() -> {
            for (Task task : controller.getTasks()) {
                if (shouldNotifyTask(task)) {
                    showNotification(task);
                }
            }
        });
    }

    /**
     * @param task The task to check
     * @return True if the task is due soon, false otherwise
     */
    private static boolean shouldNotifyTask(@NotNull Task task) {
        if (task.getStatus().equals("Completed")) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueDateTime = LocalDateTime.of(task.getDueDate(), 
            task.getDueTime() != null ? task.getDueTime() : LocalTime.of(9, 0));

        long hoursUntilDue = ChronoUnit.HOURS.between(now, dueDateTime);
        return hoursUntilDue > 0 && hoursUntilDue <= DUE_SOON_THRESHOLD;
    }

    /**
     * @param task The task to notify
     */
    public static void showNotification(Task task) {
        Platform.runLater(() -> {
            Stage notificationStage = new Stage(StageStyle.TRANSPARENT);
            VBox notificationBox = new VBox(10);
            notificationBox.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #2c3e50, #34495e);" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 15;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);"
            );

            Label titleLabel = new Label("Task Due Soon!");
            titleLabel.setStyle(
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: white;"
            );

            Label taskLabel = new Label(task.getDescription());
            taskLabel.setStyle(
                "-fx-font-size: 14px;" +
                "-fx-text-fill: #ecf0f1;"
            );

            LocalDateTime dueDateTime = LocalDateTime.of(task.getDueDate(), 
                task.getDueTime() != null ? task.getDueTime() : LocalTime.of(9, 0));
            long hoursUntilDue = ChronoUnit.HOURS.between(LocalDateTime.now(), dueDateTime);
            
            Label timeLabel = new Label(String.format("Due in %d hours", hoursUntilDue));
            timeLabel.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: #bdc3c7;"
            );

            notificationBox.getChildren().addAll(titleLabel, taskLabel, timeLabel);
            notificationBox.setAlignment(Pos.CENTER);

            Scene scene = new Scene(notificationBox);
            scene.setFill(Color.TRANSPARENT);
            notificationStage.setScene(scene);

            // Position the notification in the bottom-right corner
            notificationStage.setX(javafx.stage.Screen.getPrimary().getVisualBounds().getWidth() - 300);
            notificationStage.setY(javafx.stage.Screen.getPrimary().getVisualBounds().getHeight() - 150);

            notificationStage.show();
            activeNotifications.add(notificationStage);

            // Auto-close the notification after duration
            Timeline closeTimeline = new Timeline(
                new KeyFrame(Duration.millis(NOTIFICATION_DURATION), event -> {
                    notificationStage.close();
                    activeNotifications.remove(notificationStage);
                })
            );
            closeTimeline.play();
        });
    }

    /**
     * Stops the notification service.
     */
    public static void stopNotificationService() {
        if (notificationCheckTimeline != null) {
            notificationCheckTimeline.stop();
        }
        // Close all active notifications
        for (Stage notification : activeNotifications) {
            notification.close();
        }
        activeNotifications.clear();
    }
} 
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
 * Checks tasks for due-soon reminders and displays lightweight desktop notifications.
 */
public class NotificationService {
    private static final int NOTIFICATION_DURATION = 5000;
    private static final int CHECK_INTERVAL = 60000;
    private static final int DUE_SOON_THRESHOLD = 24;
    private static final List<Stage> activeNotifications = new ArrayList<>();
    private static Timeline notificationCheckTimeline;

    /**
     * Starts the notification service.
     * @param controller the TaskManagerController
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

    public static void checkDueTasks(TaskManagerController controller) {
        Platform.runLater(() -> {
            for (Task task : controller.getTasks()) {
                if (shouldNotifyTask(task)) {
                    showNotification(task);
                }
            }
        });
    }

    /**
     * Determines whether an incomplete task is due within the notification window.
     * @param task the task to evaluate
     * @return true when the task is incomplete and due within 24 hours
     */
    public static boolean shouldNotifyTask(@NotNull Task task) {
        if ("Completed".equalsIgnoreCase(task.getStatus()) || task.getDueDate() == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueDateTime = LocalDateTime.of(task.getDueDate(), 
            task.getDueTime() != null ? task.getDueTime() : LocalTime.of(9, 0));

        long hoursUntilDue = ChronoUnit.HOURS.between(now, dueDateTime);
        return hoursUntilDue > 0 && hoursUntilDue <= DUE_SOON_THRESHOLD;
    }

    /**
     * Shows a notification for a given task.
     * @param task the task to notify about
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

            notificationStage.setX(javafx.stage.Screen.getPrimary().getVisualBounds().getWidth() - 300);
            notificationStage.setY(javafx.stage.Screen.getPrimary().getVisualBounds().getHeight() - 150);

            notificationStage.show();
            activeNotifications.add(notificationStage);

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
        for (Stage notification : activeNotifications) {
            notification.close();
        }
        activeNotifications.clear();
    }
}

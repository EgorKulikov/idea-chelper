package net.egork.chelper.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;

/**
 * @author egorku@yandex-team.ru
 */
public class Messenger {
    public static void publishMessage(String message, NotificationType type) {
        Notifications.Bus.notify(new Notification("chelper", "CHelper", message, type));
    }
}

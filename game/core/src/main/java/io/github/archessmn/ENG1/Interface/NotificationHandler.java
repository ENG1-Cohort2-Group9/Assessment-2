package io.github.archessmn.ENG1.Interface;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;

import java.util.LinkedList;
import java.util.Queue;

/**
 * This class manages the notification holder that can be displayed. The notification can have text or both text
 * and an image displayed, and this notification is displayed within a set game time.
 */
public class NotificationHandler {
    // The in-game time that the notification will be displayed for
    public static final int NOTIFICATION_DISPLAY_TIME = 10;

    // The timer for the active notification
    private static float notifTimer = NOTIFICATION_DISPLAY_TIME;

    // Storage for all active notifications
    public static Queue<Notification> notifQueue;
    public static Notification currentNotif;


    /**
     * This creates the background image for the notification and initialises the notification queue.
     * @param notificationBackgroundTexture The texture for the notification background
     */
    public static void initNotification(Texture notificationBackgroundTexture) {
        notifQueue = new LinkedList<>();

        GameScreen.notificationBackground = new Sprite(notificationBackgroundTexture);
        GameScreen.notificationBackground.setOrigin(0, 100);
    }

    /**
     * A new notification is displayed and added to the queue, with the given title and text.
     * @param title The header title for the notification (max. 30 characters)
     * @param text The body text for the notification (max. 112 characters)
     */
    public static void displayNotification(String title, String text) {
        Notification notification = new Notification(title, text, null);
        notifQueue.add(notification);
    }

    /**
     * A new notification is displayed and added to the queue, with the given title, text and image.
     * @param title The header title for the notification (max. 20 characters)
     * @param text The body text for the notification (max. 100 characters)
     * @param notifImage The texture that will be displayed in the notification's icon
     */
    public static void displayNotification(String title, String text, Texture notifImage) {
        Notification notification = new Notification(title, text, notifImage);
        notifQueue.add(notification);
    }

    /**
     * Updates the notification timer, if any active notifications are present in the queue. It also controls the queue,
     * updating the current notification to be displayed.
     */
    public static void updateNotificationProcess() {
        if (hasNotifications()) {
            if (notifTimer == NOTIFICATION_DISPLAY_TIME) {
                currentNotif = getNextNotification();

                if (currentNotif.notifImage != null) {
                    GameScreen.notificationImage = new Sprite(currentNotif.notifImage);
                    GameScreen.notificationImage.setOrigin(0, 80);
                    GameScreen.notificationImage.setSize(80, 80);
                }
            }

            if (notifTimer > 0f) {
                notifTimer -= Gdx.graphics.getDeltaTime();
            }
            else {
                notifTimer = NOTIFICATION_DISPLAY_TIME;
                deleteCurrentNotification();
            }
        }
    }

    /**
     * @return The next notification in the queue.
     */
    public static Notification getNextNotification() { return notifQueue.peek(); }

    /**
     * @return The current notification to be displayed.
     */
    public static Notification getCurrentNotification() { return currentNotif; }

    /**
     * @return Whether the queue has notifications or not.
     */
    public static boolean hasNotifications() { return !notifQueue.isEmpty(); }

    /**
     * Deletes the current notification from the queue, once it is finished being displayed.
     */
    public static void deleteCurrentNotification() { notifQueue.remove(currentNotif); }
}


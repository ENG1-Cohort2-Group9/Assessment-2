package io.github.archessmn.ENG1.Interface;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;

/**
 * This class manages the notification holder that can be displayed. The notification can have text or both text
 * and an image displayed, and this notification is displayed within a set game time.
 */
public class NotificationHandler {
    public static boolean showNotif = false;
    public static float notifEndDisplayTime;

    public static final int NOTIFICATION_DISPLAY_TIME = 10; // The in-game time that the notification will be displayed for
    public static final int IMAGELESS_TITLE_MAX_CHARS = 30;
    public static final int IMAGELESS_TEXT_MAX_CHARS = 112;
    public static final int IMAGE_TITLE_MAX_CHARS = 20;
    public static final int IMAGE_TEXT_MAX_CHARS = 100;


    public static String notifTitle = "";
    public static String notifText = "";
    public static Texture notifImage;

    /**
     * This creates the background image for the notification. It must be called first before any other method
     * can be.
     * @param notificationBackgroundTexture The texture for the notification background
     */
    public static void initNotification(Texture notificationBackgroundTexture) {
        GameScreen.notificationBackground = new Sprite(notificationBackgroundTexture);
        GameScreen.notificationBackground.setOrigin(0, 100);
    }

    /**
     * A new notification is displayed with the given title and text, for the set duration.
     * @param title The header title for the notification (max. 30 characters)
     * @param text The body text for the notification (max. 112 characters)
     * @param currentTime The time that the notification will be start the countdown from
     */
    public static void displayNotification(String title, String text, float currentTime) {
        notifTitle = title;
        notifText = text;
        notifImage = null;

        // Validation length checks
        if (notifText.length() > IMAGELESS_TEXT_MAX_CHARS) {
            throw new IllegalArgumentException("Notification text given is more than 112 characters");
        }
        else if (notifTitle.length() > IMAGELESS_TITLE_MAX_CHARS) {
            throw new IllegalArgumentException("Notification title given is more than 30 characters");
        }

        notifText = wrapText(notifText, IMAGELESS_TEXT_MAX_CHARS);

        showNotif = true;
        notifEndDisplayTime = currentTime + NOTIFICATION_DISPLAY_TIME;
    }

    /**
     * A new notification is displayed with the given title, text and icon, for the set duration.
     * @param title The header title for the notification (max. 20 characters)
     * @param text The body text for the notification (max. 70 characters)
     * @param imageTexture The image texture for the notification icon
     * @param currentTime The time that the notification will be start the countdown from
     */
    public static void displayNotification(String title, String text, Texture imageTexture, float currentTime) {
        notifTitle = title;
        notifText = text;
        notifImage = imageTexture;

        GameScreen.notificationImage = new Sprite(notifImage);
        GameScreen.notificationImage.setOrigin(0, 80);
        GameScreen.notificationImage.setSize(80, 80);

        // Validation length checks
        if (notifText.length() > IMAGE_TEXT_MAX_CHARS) {
            throw new IllegalArgumentException("Notification text given is more than 70 characters");
        }
        else if (notifTitle.length() > IMAGE_TITLE_MAX_CHARS) {
            throw new IllegalArgumentException("Notification title given is more than 20 characters");
        }

        notifText = wrapText(notifText, IMAGE_TEXT_MAX_CHARS);

        showNotif = true;
        notifEndDisplayTime = currentTime + NOTIFICATION_DISPLAY_TIME;
    }

    /**
     * Checks to see if the current notification has passed it's TTL and thus should be hidden.
     * @param currentTime The current time
     */
    public static void updateNotificationProcess(float currentTime) {
        if (showNotif && currentTime > notifEndDisplayTime) {
            notifTitle = "";
            notifText = "";
            notifImage = null;

            showNotif = false;
            notifEndDisplayTime = 0;
        }
    }

    /**
     * Ensures 2-line text wrapping is done correctly, given a max character length.
     * @param text The given text to wrap
     * @param maxChar The max character length of the text
     * @return The wrapped text
     */
    private static String wrapText(String text, int maxChar) {
        if (text.length() > maxChar / 2) {
            int firstLineEndIndex = text.substring(0, maxChar / 2).lastIndexOf(" ");
            StringBuilder stringBuilder = new StringBuilder(text);
            if (firstLineEndIndex == -1) {
                stringBuilder.insert(maxChar / 2, "\n");
                stringBuilder.replace((maxChar / 2) + 1, (maxChar / 2) + 2, "");
            }
            else {
                stringBuilder.insert(firstLineEndIndex, "\n");
                stringBuilder.replace(firstLineEndIndex + 1, firstLineEndIndex + 2, "");
            }
            text = stringBuilder.toString();
        }

        return text;
    }
}

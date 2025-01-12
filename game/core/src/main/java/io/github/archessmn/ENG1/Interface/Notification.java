package io.github.archessmn.ENG1.Interface;

import com.badlogic.gdx.graphics.Texture;

/**
 * This the holder for all information relating to a notification that can be displayed onto the screen. Notifications
 * can only be instantiated by the NotificationHandler, which deals with how they are processed and stored.
 * <p>
 * Notifications can have an icon or no icon, alongside a title and body to be displayed.
 */
public class Notification {
    /*
    These constants determine how many characters a notification should be allowed to display
    IMAGELESS = Notification without an icon
    IMAGE = Notification with an icon
     */
    public static final int IMAGELESS_TITLE_MAX_CHARS = 30;
    public static final int IMAGELESS_TEXT_MAX_CHARS = 112;
    public static final int IMAGE_TITLE_MAX_CHARS = 20;
    public static final int IMAGE_TEXT_MAX_CHARS = 85;

    public String notifTitle;
    public String notifText;
    public Texture notifImage;

    /**
     * Creates the notification.
     * @param notifTitle The title of the notification
     * @param notifText The main body of the notification
     * @param notifImage The image for the icon of the notification. Set as null if no icon is to be displayed
     */
    public Notification(String notifTitle, String notifText, Texture notifImage) {
        // Validation length checks and text wrapping so that the text fits into the notification holder.
        if (notifImage == null) {
            if (notifText.length() > IMAGELESS_TEXT_MAX_CHARS) {
                notifText = notifText.substring(0, IMAGELESS_TEXT_MAX_CHARS);
            } else if (notifTitle.length() > IMAGELESS_TITLE_MAX_CHARS) {
                notifTitle = notifTitle.substring(0, IMAGELESS_TITLE_MAX_CHARS);
            }

            this.notifText = wrapText(notifText, IMAGELESS_TEXT_MAX_CHARS);
        }
        else {
            if (notifText.length() > IMAGE_TEXT_MAX_CHARS) {
                notifText = notifText.substring(0, IMAGE_TEXT_MAX_CHARS);
            } else if (notifTitle.length() > IMAGE_TITLE_MAX_CHARS) {
                notifTitle = notifTitle.substring(0, IMAGE_TITLE_MAX_CHARS);
            }

            this.notifText = wrapText(notifText, IMAGE_TEXT_MAX_CHARS);
        }

        this.notifTitle = notifTitle;
        this.notifImage = notifImage;
    }

    /**
     * Ensures 2-line text wrapping is done correctly, given a max character length.
     * @param text The given text to wrap
     * @param maxChar The max character length of the text
     * @return The wrapped text
     */
    private static String wrapText(String text, int maxChar) {
        if (text.length() > maxChar / 2) { // Only wraps the text if it exceeds one line
            int firstLineEndIndex = text.substring(0, maxChar / 2).lastIndexOf(" "); // Gets the end of the first line without cutting a word off
            StringBuilder stringBuilder = new StringBuilder(text);


            if (firstLineEndIndex == -1) { // If the end of the first line didn't need adjustments for the word cut off
                stringBuilder.insert(maxChar / 2, "\n");
                stringBuilder.replace((maxChar / 2) + 1, (maxChar / 2) + 2, "");
            }
            else { // If an adjustment was made to ensure a word wasn't cut off
                stringBuilder.insert(firstLineEndIndex, "\n");
                stringBuilder.replace(firstLineEndIndex + 1, firstLineEndIndex + 2, "");
            }
            text = stringBuilder.toString();
        }

        return text;
    }
}

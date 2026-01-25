package com.joogiebear.hytalevault.util;

import com.hypixel.hytale.server.core.Message;

/**
 * Utility class for creating Hytale Message objects.
 */
public class MessageUtil {

    /**
     * Create a Message from a plain string.
     */
    public static Message of(String text) {
        return Message.raw(text);
    }

    /**
     * Create a colored message.
     */
    public static Message colored(String text, String color) {
        return of(text).color(color);
    }

    /**
     * Create an error message (red).
     */
    public static Message error(String text) {
        return of(text).color("#FF5555");
    }

    /**
     * Create a success message (green).
     */
    public static Message success(String text) {
        return of(text).color("#55FF55");
    }

    /**
     * Create an info message (yellow).
     */
    public static Message info(String text) {
        return of(text).color("#FFFF55");
    }
}

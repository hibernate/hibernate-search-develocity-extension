package org.hibernate.infra.develocity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Log {

    private static final Logger LOGGER = LoggerFactory.getLogger(Log.class.getPackageName());

    public static void debug(String message) {
        if (!LOGGER.isDebugEnabled()) {
            return;
        }

        LOGGER.debug(message);
    }

    public static void error(String message) {
        LOGGER.error(message);
    }

    public static void error(String message, Throwable e) {
        LOGGER.error(message, e);
    }

    public static void warn(String message) {
        LOGGER.warn(message);
    }

    public static void warn(String message, Throwable e) {
        LOGGER.warn(message, e);
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void debug(String pluginName, String message) {
        if (!LOGGER.isDebugEnabled()) {
            return;
        }

        LOGGER.debug("[" + pluginName + "] " + message);
    }

    public static void error(String pluginName, String message) {
        LOGGER.error("[" + pluginName + "] " + message);
    }

    public static void error(String pluginName, String message, Throwable e) {
        LOGGER.error("[" + pluginName + "] " + message, e);
    }

    public static void warn(String pluginName, String message) {
        LOGGER.warn("[" + pluginName + "] " + message);
    }

    public static void warn(String pluginName, String message, Throwable e) {
        LOGGER.warn("[" + pluginName + "] " + message, e);
    }

    public static void info(String pluginName, String message) {
        LOGGER.info("[" + pluginName + "] " + message);
    }

    private Log() {
    }
}

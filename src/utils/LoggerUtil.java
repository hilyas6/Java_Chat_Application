package utils;

import java.util.logging.*;

/**
 * Utility class for initialising and providing access to a shared application logger.
 */
public class LoggerUtil {
    private static final Logger logger = Logger.getLogger("GroupChatLogger");

    // Static block to configure logger settings when the class is loaded
    static {
        try {
            LogManager.getLogManager().reset();
            FileHandler fh = new FileHandler("groupchat.log", true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            logger.setLevel(Level.INFO);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialise logger", e);
        }
    }

    /**
     * Provides global access to the configured logger instance.
     *
     * @return the logger instance
     */
    public static Logger getLogger() {
        return logger;
    }
}

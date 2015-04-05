package net.tuis.ubench;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Collection of common static utility methods used in other areas of the
 * package.
 * 
 * @author rolf
 *
 */
public final class UUtils {

    private UUtils() {
        // private constructor, no instances possible.
    }

    // Create a LOGGER instance for the **PACKAGE**.
    // This will be the owner of the ubench namespace.
    private static final Logger LOGGER = Logger.getLogger(UScale.class.getPackage().getName());

    /**
     * Simple wrapper that forces initialization of the package-level LOGGER
     * instance.
     * 
     * @param clazz
     *            The class to be logged.
     * @return A Logger using the name of the class as its hierarchy.
     */
    public static Logger getLogger(final Class<?> clazz) {
        if (!clazz.getPackage().getName().startsWith(LOGGER.getName())) {
            throw new IllegalArgumentException(String.format("Class %s is not a child of the package %s",
                    clazz.getName(), LOGGER.getName()));
        }
        LOGGER.fine(() -> String.format("Locating logger for class %s", clazz));
        return Logger.getLogger(clazz.getName());
    }

    /**
     * Enable regular logging for all UBench code at the specified level.
     * 
     * @param level
     *            the level to log for.
     */
    public static void setStandaloneLogging(Level level) {
        LOGGER.setUseParentHandlers(false);
        for (Handler h : LOGGER.getHandlers()) {
            LOGGER.removeHandler(h);
        }
        StdoutHandler handler = new StdoutHandler();
        handler.setFormatter(new InlineFormatter());
        LOGGER.addHandler(handler);
        
        final UncaughtExceptionHandler ueh = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            LOGGER.log(Level.SEVERE, "Uncaught Exception in thread " + t.getName(), e);
            if (ueh != null) {
                ueh.uncaughtException(t, e);
            }
        });
        
        setLogLevel(level);
    }

    /**
     * Enable the specified debug level messages to be output. Note that both
     * this Logger and whatever Handler you use, have to be set to enable the
     * required log level for the handler to output the messages. If this UBench
     * code is logging 'stand alone' then this method will also change the
     * output level of the log handlers.
     * 
     * @param level
     */
    public static void setLogLevel(Level level) {
        // all other ubench loggers inherit from here.
        LOGGER.finer("Changing logging from " + LOGGER.getLevel());
        LOGGER.setLevel(level);
        if (!LOGGER.getUseParentHandlers()) {
            LOGGER.setLevel(level);
            Stream.of(LOGGER.getHandlers()).forEach(h -> h.setLevel(level));
        }
        LOGGER.finer("Changed logging to " + LOGGER.getLevel());
    }

    private static final AtomicLong NANO_TICK = new AtomicLong(-1);
    /**
     * The minimum increment that the System.nanotime() can do. The nanotime
     * value returned by the system does not necessarily increment by 1, this
     * value indicates the smallest observed increment of the timer.
     * @return The number of nanoseconds in the smallest recorded clock tick.
     */
    public static long getNanoTick() {
        synchronized(NANO_TICK) {
            long tick = NANO_TICK.get();
            if (tick > 0) {
                return tick;
            }
            tick = computeTick();
            NANO_TICK.set(tick);
            return tick;
        }
    }

    private static final long singleTick() {

        final long start = System.nanoTime();
        long end = start;
        while (end == start) {
            end = System.nanoTime();
        }
        return end - start;

    }

    private static long computeTick() {
        final long ticklen = LongStream.range(0, 1000).map(i -> singleTick()).min().getAsLong();
        LOGGER.fine(() -> String.format("Incremental System.nanotime() tick is %d", ticklen));
        return ticklen;
    }

    /**
     * Load a resource stored in the classpath, as a String.
     * 
     * @param path
     *            the system resource to read
     * @return the resource as a String.
     */
    public static String readResource(String path) {
        final long start = System.nanoTime();
        try (InputStream is = UScale.class.getClassLoader().getResourceAsStream(path);) {
            int len = 0;
            byte[] buffer = new byte[2048];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((len = is.read(buffer)) >= 0) {
                baos.write(buffer, 0, len);
            }
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e, () -> "IOException loading resource " + path);
            throw new IllegalStateException("Unable to read class loaded stream " + path, e);
        } catch (RuntimeException re) {
            LOGGER.log(Level.WARNING, re, () -> "Unexpected exception loading resource " + path);
            throw re;
        } finally {
            LOGGER.info(() -> String.format("Loaded resource %s in %.3fms", path,
                    (System.nanoTime() - start) / 1000000.0));
        }
    }

}

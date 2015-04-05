package net.tuis.ubench;

import java.util.logging.ConsoleHandler;

/**
 * Direct logs to STDOut and not STDErr.
 * 
 * @author rolf
 *
 */
final class StdoutHandler extends ConsoleHandler {

    public StdoutHandler() {
        super();
        setOutputStream(System.out);
    }
}

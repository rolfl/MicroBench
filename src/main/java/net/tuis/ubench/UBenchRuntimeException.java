package net.tuis.ubench;

/**
 * Simple Wrapper for IllegalStateException (RuntimeException) which allows for a more granular form of error handling.
 * 
 * @author rolf
 *
 */
public class UBenchRuntimeException extends IllegalStateException {

    /**
     * 
     */
    private static final long serialVersionUID = -7469405348075121722L;

    /**
     * Initialize a new custom UBenchRuntimeException, which is just a specialized IllegalStateException.
     * 
     * @param message The message to pass through to the IllegalStateException.
     * @param cause The cause to pass through to the IllegalStateException.
     */
    public UBenchRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Initialize a new custom UBenchRuntimeException, which is just a specialized IllegalStateException.
     * 
     * @param message The message to pass through to the IllegalStateException.
     */
    public UBenchRuntimeException(String message) {
        super(message);
    }
}

package net.tuis.ubench;

/**
 * This simple interface allows a task to be managed as an instance in the
 * UBench suite, and with a single time() entry point
 * 
 * @author rolf
 *
 */
@FunctionalInterface
interface Task {
    long time();
}
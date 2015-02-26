package net.tuis.ubench;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Class containing the in-flight details of the execution of a single task.
 * This class allows the class to be run in spurts, and to collect the results
 * in a single place
 * 
 * @author rolf
 *
 */
final class TaskRunner {

    // simply give up after a billion executions.
    private static final int MAX_RESULTS = 1_000_000_000;

    /**
     * Compute the length of the results array with some space for growth
     * 
     * @param length
     *            the current length
     * @return the desired length
     */
    private static int expandTo(int length) {
        // add 25% + 100 - limit to Integer.Max
        int toAdd = 100 + (length >> 2);
        toAdd = Math.min(MAX_RESULTS - length, toAdd);
        return toAdd == 0 ? -1 : toAdd + length;
    }

    /**
     * Compute whether any of the values in times exceed the given bound,
     * relative to the minimum value in times.
     * 
     * @param times
     *            the times to compute the bounds on
     * @param bound
     *            the bound is represented as a value like 1.10 for 10% greater
     *            than the minimum
     * @return true if all values are in bounds.
     */
    private static final boolean inBounds(long[] times, double bound) {
        long min = times[0];
        long max = times[0];
        long limit = (long) (min * bound);
        for (int i = 1; i < times.length; i++) {
            if (times[i] < min) {
                min = times[i];
                limit = (long) (min * bound);
                if (max > limit) {
                    return false;
                }
            }
            if (times[i] > max) {
                max = times[i];
                // new max, is it slower than the worst allowed?
                if (max > limit) {
                    return false;
                }
            }
        }
        return true;
    }

    private final Task task;
    private final String name;
    private final int index;
    private final long[] recents;
    private final int limit;
    private final double maxVariance;

    private long[] results;
    private boolean complete = false;
    private long remainingTime = 0L;
    private int iterations = 0;

    TaskRunner(String name, Task task, int index, final int iterations, final int minStabilityLen,
            final double maxVariance, final long timeLimit, final TimeUnit timeUnit) {
        this.name = name;
        this.task = task;
        this.index = index;
        this.maxVariance = maxVariance;
        recents = new long[Math.min(minStabilityLen, iterations)];
        results = new long[Math.min(iterations, 10000)];
        remainingTime = timeUnit.toNanos(timeLimit);
        limit = iterations;
    }

    /**
     * Perform a single additional iteration of the task.
     * 
     * @return true if this task is now complete.
     */
    boolean invoke() {
        if (complete) {
            return complete;
        }

        if (iterations >= results.length) {
            int newlen = expandTo(results.length);
            if (newlen < 0) {
                complete = true;
                return complete;
            }
            results = Arrays.copyOf(results, newlen);
        }

        long res = Math.max(task.time(), 1);
        results[iterations] = res;
        if (recents.length > 0) {
            recents[iterations % recents.length] = res;
            if (iterations > recents.length && inBounds(recents, maxVariance)) {
                complete = true;
            }
        }

        remainingTime -= res;
        iterations++;

        if (iterations >= limit || remainingTime < 0) {
            complete = true;
        }
        return complete;
    }

    /**
     * Collect all statistics in to a single public UStats instance.
     * 
     * @param suite
     *            the name of the test suite this task is part of.
     * @return the UStats instance containing the data for statistical analysis
     */
    UStats collect(String suite) {
        return new UStats(suite, name, index, Arrays.copyOf(results, iterations));
    }

}
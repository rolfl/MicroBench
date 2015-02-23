package net.tuis.ubench;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The UBench class encompasses a suite of tasks that are to be compared...
 * possibly relative to each other.
 * <p>
 * Each task can be added to the suite. Once you have the tasks you need, then
 * all tasks can be benchmarked according to limits given in the run.
 * 
 * @author rolf
 *
 */
public class UBench {

    private final List<Task<?>> tasks = new ArrayList<>();
    private final String suiteName;

    public UBench(String suiteName) {
        this.suiteName = suiteName;
    }

    /**
     * Add a task to the suite.
     * <p>
     * Note that there are helper methods on the Task class to help you build
     * task instances.
     * 
     * @param task
     *            the task to add to this suite.
     */
    public void addTask(Task<?> task) {
        synchronized (tasks) {
            tasks.add(task);
        }
    }

    /**
     * Benchmark a task until it completes the desired iterations, exceeds the
     * time limit, or reaches stability, whichever comes first.
     * 
     * @param iterations
     *            maximum number of iterations to run.
     * @param minStabilityLen
     *            If this many iterations in a row are all within the
     *            maxVariance, then the benchmark ends.
     * @param maxVariance
     *            Expressed as a percent from 0.0 to 100.0, and so on
     * @return the results of all completed tasks.
     */
    public List<TaskStats> benchMark(final int iterations, final int minStabilityLen, final double maxVariance,
            final long timeLimit, final TimeUnit timeUnit) {

        List<Task<?>> mytasks = getTasks();
        TaskStats[] ret = new TaskStats[mytasks.size()];
        int i = 0;
        for (Task<?> task : mytasks) {
            ret[i++] = runTask(task, iterations, minStabilityLen, 1 + (maxVariance / 100.0), timeLimit, timeUnit);
        }

        return Arrays.asList(ret);
    }

    /**
     * Benchmark all tasks until it they complete the desired elapsed time
     * 
     * @param iterations
     *            number of iterations to run.
     * @return the results of all completed tasks.
     */
    public List<TaskStats> benchMark(final long timeLimit, final TimeUnit timeUnit) {
        return benchMark(Integer.MAX_VALUE, 0, 100, timeLimit, timeUnit);
    }

    /**
     * Benchmark all tasks until it they complete the desired iteration count
     * 
     * @param iterations
     *            number of iterations to run.
     * @return the results of all completed tasks.
     */
    public List<TaskStats> benchMark(final int iterations) {
        return benchMark(iterations, 0, 100, 1000, TimeUnit.DAYS);
    }

    private List<Task<?>> getTasks() {
        synchronized (tasks) {
            return new ArrayList<>(tasks);
        }
    }

    private TaskStats runTask(final Task<?> task, final int iterations, final int minStability, final double maxLimit,
            final long timeLimit, final TimeUnit timeUnit) {
        long[] results = new long[Math.min(iterations, 10000)];
        long[] recents = new long[Math.min(minStability, iterations)];
        int rPos = 0;

        long limit = System.currentTimeMillis() + timeUnit.toMillis(timeLimit);

        for (int i = 0; i < iterations; i++) {
            long res = Math.max(task.compute(), 1);
            if (rPos >= results.length) {
                results = Arrays.copyOf(results, expandTo(results.length));
            }
            if (minStability > 0) {
                recents[rPos % recents.length] = res;
            }
            results[rPos++] = res;
            if ((timeLimit > 0 && System.currentTimeMillis() >= limit)
                    || (minStability > 0 && rPos >= recents.length && inBounds(recents, maxLimit))) {
                return new TaskStats(task.getName(), Arrays.copyOf(results, rPos));
            }
        }
        return new TaskStats(task.getName(), Arrays.copyOf(results, rPos));
    }

    private int expandTo(int length) {
        // add 25% + 100 - limit to Integer.Max
        int toAdd = 100 + (length >> 2);
        toAdd = Math.min(Integer.MAX_VALUE - length, toAdd);
        return toAdd + length;
    }

    @Override
    public String toString() {
        return String.format("%s with tasks: %s", suiteName, tasks.toString());
    }

    /**
     * Compute whether any of the values in times exceed the given bound,
     * realtive to the minimum value in times.
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

}

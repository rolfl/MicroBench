package net.tuis.ubench;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

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
public final class UBench {

    /**
     * Statistics representing the runs in this task.
     * <p>
     * Presents various statistics related to the run times that are useful for
     * interpreting the run performance.
     */
    public static final class Stats {

        private static final double NANOxMILLI = 1000000.0;

        private final long[] results;
        private final long min;
        private final long max;
        private final double average;
        private final String suit;
        private final String name;

        /**
         * Construct statistics based on the nanosecond times of multiple runs.
         * 
         * @param name
         *            The name of the task that has been benchmarked
         * @param results
         *            The nano-second run times of each successful run.
         */
        Stats(String suit, String name, long[] results) {
            this.suit = suit;
            this.name = name;
            this.results = results;
            LongSummaryStatistics lss = LongStream.of(results).summaryStatistics();
            min = lss.getMin();
            max = lss.getMax();
            average = lss.getAverage();
        }

        /**
         * Get the raw data the statistics are based off.
         * 
         * @return the individual test run times (in nanoseconds, and in order
         *         of execution).
         */
        public long[] getRawData() {
            return Arrays.copyOf(results, results.length);
        }

        /**
         * Summarize the time-progression of the run time for each iteration, in
         * order of execution (in milliseconds).
         * <p>
         * An example helps. If there are 200 results, and a request for 10
         * zones, then return 10 double values representing the average time of
         * the first 20 runs, then the next 20, and so on, until the 10th zone
         * contains the average time of the last 20 runs.
         * <p>
         * This is a good way to see the effects of warm-up times and different
         * compile levels
         * 
         * @param zoneCount
         * @return
         */
        public final double[] getZoneTimesMilli(int zoneCount) {
            double[] ret = new double[Math.min(zoneCount, results.length)];
            int perblock = results.length / ret.length;
            int overflow = results.length % ret.length;
            int pos = 0;
            for (int block = 0; block < ret.length; block++) {
                int count = perblock + (block < overflow ? 1 : 0);
                int limit = pos + count;
                long nanos = 0;
                while (pos < limit) {
                    nanos += results[pos];
                    pos++;
                }
                ret[block] = (nanos / NANOxMILLI) / count;
            }
            return ret;
        }

        /**
         * Compute a log-2-based histogram relative to the fastest run in the
         * data set.
         * <p>
         * This gives a sense of what the general shape of the runs are in terms
         * of distribution of run times. The histogram is based on the fastest
         * run.
         * <p>
         * By way of an example, the output: <code>100, 50, 10, 1, 0, 1</code>
         * would suggest that:
         * <ul>
         * <li>100 runs were between 1 times and 2 times as slow as the fastest.
         * <li>50 runs were between 2 and 4 times slower than the fastest.
         * <li>10 runs were between 4 and 8 times slower
         * <li>1 run was between 8 and 16 times slower
         * <li>1 run was between 32 and 64 times slower
         * 
         * @return
         */
        public final int[] getHistogramByDoublingFactor() {
            int count = (int) (max / min);
            int[] histo = new int[Integer.numberOfTrailingZeros(Integer.highestOneBit(count)) + 1];
            LongStream.of(results).mapToInt(t -> Integer.numberOfTrailingZeros(Integer.highestOneBit((int) (t / min))))
                    .forEach(i -> histo[i]++);
            return histo;
        }

        /**
         * Compute the 95<sup>th</sup> percentile of runtimes (in milliseconds).
         * <p>
         * 95% of all runs completed in this time, or faster.
         * 
         * @return the millisecond time of the 95<sup>th</sup> percentile.
         */
        public final double get95thPercentile() {
            if (results.length < 100) {
                return getSlowest();
            }
            long limit = ((results.length + 1) * 95) / 100;
            return LongStream.of(results).sorted().limit(limit).max().getAsLong() / NANOxMILLI;
        }

        /**
         * Compute the average time of all runs (in milliseconds).
         * 
         * @return the average time (in milliseconds)
         */
        public final double getAverage() {
            return average / NANOxMILLI;
        }

        /**
         * Compute the slowest run (in milliseconds).
         * 
         * @return The slowest run time (in milliseconds).
         */
        public final double getSlowest() {
            return max / NANOxMILLI;
        }

        /**
         * Compute the fastest run (in milliseconds).
         * 
         * @return The fastest run time (in milliseconds).
         */
        public final double getFastest() {
            return min / NANOxMILLI;
        }

        @Override
        public String toString() {
            return String.format("Task %s -> %s:\n" 
                    + "  Iterations  : %12d\n" 
                    + "  Fastest     : %12.5fms\n"
                    + "  Average     : %12.5fms\n" 
                    + "  95Pctile    : %12.5fms\n" 
                    + "  Slowest     : %12.5fms\n"
                    + "  TimeBlock   : %s\n" 
                    + "  FactorHisto : %s\n",
                    suit, name, results.length, getFastest(),
                    getAverage(), get95thPercentile(), getSlowest(), formatMillis(getZoneTimesMilli(10)),
                    formatHisto(getHistogramByDoublingFactor()));
        }

        private String formatHisto(int[] histogramByXFactor) {
            return IntStream.of(histogramByXFactor).mapToObj(i -> String.format("%5d", i))
                    .collect(Collectors.joining(" "));
        }

        private String formatMillis(double[] zoneTimesMilli) {
            return DoubleStream.of(zoneTimesMilli).mapToObj(d -> String.format("%.5fms", d))
                    .collect(Collectors.joining(" "));
        }

        public String getSuit() {
            return suit;
        }

        public String getName() {
            return name;
        }

    }

    private static class NamedTask {

        private final String name;
        private final Task task;

        public NamedTask(String name, Task task) {
            super();
            this.name = name;
            this.task = task;
        }

        public String getName() {
            return name;
        }

        public Task getTask() {
            return task;
        }

    }

    @FunctionalInterface
    private interface Task {
        long time();
    }

    private final Map<String, Task> tasks = new LinkedHashMap<>();
    private final String suiteName;

    public UBench(String suiteName) {
        this.suiteName = suiteName;
    }

    private void putTask(String name, Task t) {
        synchronized (tasks) {
            tasks.put(name, t);
        }
    }

    /**
     * Include a named task (and validator) in to the benchmark. 
     * @param name The name of the task. Only one task with any one name is allowed.
     * @param task The task to perform
     * @param check The check of the results from the task.
     */
    public <T> void addTask(String name, Supplier<T> task, Predicate<T> check) {
        putTask(name, () -> {
            long start = System.nanoTime();
            T result = task.get();
            long time = System.nanoTime() - start;
            if (check != null && !check.test(result)) {
                throw new IllegalStateException(String.format("Task %s failed Result: %s", name, result));
            }
            return time;
        });
    }

    /**
     * Include a named task in to the benchmark. 
     * @param name The name of the task. Only one task with any one name is allowed.
     * @param task The task to perform
     */
    public <T> void addTask(String name, Supplier<T> task) {
        addTask(name, task, null);
    }

    /**
     * Include an int-specialized named task (and validator) in to the benchmark. 
     * @param name The name of the task. Only one task with any one name is allowed.
     * @param task The task to perform
     * @param check The check of the results from the task.
     */
    public void addIntTask(String name, IntSupplier task, IntPredicate check) {
        putTask(name, () -> {
            long start = System.nanoTime();
            int result = task.getAsInt();
            long time = System.nanoTime() - start;
            if (check != null && !check.test(result)) {
                throw new IllegalStateException(String.format("Task %s failed Result: %s", name, result));
            }
            return time;
        });
    }

    /**
     * Include an int-specialized named task in to the benchmark. 
     * @param name The name of the task. Only one task with any one name is allowed.
     * @param task The task to perform
     */

    public void addIntTask(String name, IntSupplier task) {
        addIntTask(name, task, null);
    }

    /**
     * Include a long-specialized named task (and validator) in to the benchmark. 
     * @param name The name of the task. Only one task with any one name is allowed.
     * @param task The task to perform
     * @param check The check of the results from the task.
     */
    public void addLongTask(String name, LongSupplier task, LongPredicate check) {
        putTask(name, () -> {
            long start = System.nanoTime();
            long result = task.getAsLong();
            long time = System.nanoTime() - start;
            if (check != null && !check.test(result)) {
                throw new IllegalStateException(String.format("Task %s failed Result: %s", name, result));
            }
            return time;
        });
    }

    /**
     * Include a long-specialized named task in to the benchmark. 
     * @param name The name of the task. Only one task with any one name is allowed.
     * @param task The task to perform
     */
    public void addLongTask(String name, LongSupplier task) {
        addLongTask(name, task, null);
    }

    /**
     * Include a double-specialized named task (and validator) in to the benchmark. 
     * @param name The name of the task. Only one task with any one name is allowed.
     * @param task The task to perform
     * @param check The check of the results from the task.
     */
    public void addDoubleTask(String name, DoubleSupplier task, DoublePredicate check) {
        putTask(name, () -> {
            long start = System.nanoTime();
            double result = task.getAsDouble();
            long time = System.nanoTime() - start;
            if (check != null && !check.test(result)) {
                throw new IllegalStateException(String.format("Task %s failed Result: %s", name, result));
            }
            return time;
        });
    }

    /**
     * Include a double-specialized named task in to the benchmark. 
     * @param name The name of the task. Only one task with any one name is allowed.
     * @param task The task to perform
     */
    public void addDoubleTask(String name, DoubleSupplier task) {
        addDoubleTask(name, task, null);
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
    public List<Stats> benchMark(final int iterations, final int minStabilityLen, final double maxVariance,
            final long timeLimit, final TimeUnit timeUnit) {

        List<NamedTask> mytasks = getTasks();
        Stats[] ret = new Stats[mytasks.size()];
        int i = 0;
        for (NamedTask task : mytasks) {
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
    public List<Stats> benchMark(final long timeLimit, final TimeUnit timeUnit) {
        return benchMark(Integer.MAX_VALUE, 0, 100, timeLimit, timeUnit);
    }

    /**
     * Benchmark all tasks until it they complete the desired iteration count
     * 
     * @param iterations
     *            number of iterations to run.
     * @return the results of all completed tasks.
     */
    public List<Stats> benchMark(final int iterations) {
        return benchMark(iterations, 0, 100, 1000, TimeUnit.DAYS);
    }

    private List<NamedTask> getTasks() {
        synchronized (tasks) {
            return tasks.entrySet().stream().map(e -> new NamedTask(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        }
    }

    private Stats runTask(final NamedTask ntask, final int iterations, final int minStability, final double maxLimit,
            final long timeLimit, final TimeUnit timeUnit) {
        long[] results = new long[Math.min(iterations, 10000)];
        long[] recents = new long[Math.min(minStability, iterations)];
        int rPos = 0;

        long limit = System.currentTimeMillis() + timeUnit.toMillis(timeLimit);

        for (int i = 0; i < iterations; i++) {
            long res = Math.max(ntask.getTask().time(), 1);
            if (rPos >= results.length) {
                results = Arrays.copyOf(results, expandTo(results.length));
            }
            if (minStability > 0) {
                recents[rPos % recents.length] = res;
            }
            results[rPos++] = res;
            if ((timeLimit > 0 && System.currentTimeMillis() >= limit)
                    || (minStability > 0 && rPos >= recents.length && inBounds(recents, maxLimit))) {
                return new Stats(suiteName, ntask.getName(), Arrays.copyOf(results, rPos));
            }
        }
        return new Stats(suiteName, ntask.getName(), Arrays.copyOf(results, rPos));
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

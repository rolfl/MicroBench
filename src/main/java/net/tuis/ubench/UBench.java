package net.tuis.ubench;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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
import java.util.stream.Stream;

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

    public static final Comparator<UStats> BY_95PCTILE = Comparator.comparingLong(UStats::get95thPercentileNanos);
    public static final Comparator<UStats> BY_99PCTILE = Comparator.comparingLong(UStats::get99thPercentileNanos);
    public static final Comparator<UStats> BY_FASTEST = Comparator.comparingLong(UStats::getFastestRawNanos);
    public static final Comparator<UStats> BY_SLOWEST = Comparator.comparingLong(UStats::getFastestRawNanos);
    public static final Comparator<UStats> BY_CONSISTENCY = Comparator.comparingDouble(s -> s.getSlowestRawNanos() / (s.getFastestRawNanos() * 1.0));
    public static final Comparator<UStats> BY_AVERAGE = Comparator.comparingDouble(UStats::getAverageRawNanos);
    public static final Comparator<UStats> BY_ADDED = Comparator.comparingDouble(UStats::getIndex);
    

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
     * 
     * @param name
     *            The name of the task. Only one task with any one name is
     *            allowed.
     * @param task
     *            The task to perform
     * @param check
     *            The check of the results from the task.
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
     * 
     * @param name
     *            The name of the task. Only one task with any one name is
     *            allowed.
     * @param task
     *            The task to perform
     */
    public <T> void addTask(String name, Supplier<T> task) {
        addTask(name, task, null);
    }

    /**
     * Include an int-specialized named task (and validator) in to the
     * benchmark.
     * 
     * @param name
     *            The name of the task. Only one task with any one name is
     *            allowed.
     * @param task
     *            The task to perform
     * @param check
     *            The check of the results from the task.
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
     * 
     * @param name
     *            The name of the task. Only one task with any one name is
     *            allowed.
     * @param task
     *            The task to perform
     */

    public void addIntTask(String name, IntSupplier task) {
        addIntTask(name, task, null);
    }

    /**
     * Include a long-specialized named task (and validator) in to the
     * benchmark.
     * 
     * @param name
     *            The name of the task. Only one task with any one name is
     *            allowed.
     * @param task
     *            The task to perform
     * @param check
     *            The check of the results from the task.
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
     * 
     * @param name
     *            The name of the task. Only one task with any one name is
     *            allowed.
     * @param task
     *            The task to perform
     */
    public void addLongTask(String name, LongSupplier task) {
        addLongTask(name, task, null);
    }

    /**
     * Include a double-specialized named task (and validator) in to the
     * benchmark.
     * 
     * @param name
     *            The name of the task. Only one task with any one name is
     *            allowed.
     * @param task
     *            The task to perform
     * @param check
     *            The check of the results from the task.
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
     * 
     * @param name
     *            The name of the task. Only one task with any one name is
     *            allowed.
     * @param task
     *            The task to perform
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
    public List<UStats> benchMark(final UMode model, final int iterations, final int minStabilityLen,
            final double maxVariance, final long timeLimit, final TimeUnit timeUnit) {

        TaskRunner[] mytasks = getTasks(iterations, minStabilityLen, maxVariance, timeLimit, timeUnit);
        UStats[] ret = model.getModel().executeTasks(suiteName, mytasks);
        return Arrays.asList(ret);
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
    public List<UStats> benchMark(final int iterations, final int minStabilityLen,
            final double maxVariance, final long timeLimit, final TimeUnit timeUnit) {
        return benchMark(UMode.INTERLEAVED, iterations, minStabilityLen, maxVariance, timeLimit, timeUnit);
    }

   /**
     * Benchmark all tasks until it they complete the desired elapsed time
     * 
     * @param iterations
     *            number of iterations to run.
     * @return the results of all completed tasks.
     */
    public List<UStats> benchMark(final long timeLimit, final TimeUnit timeUnit) {
        return benchMark(UMode.INTERLEAVED, Integer.MAX_VALUE, 0, 100, timeLimit, timeUnit);
    }

    /**
     * Benchmark all tasks until it they complete the desired iteration count
     * 
     * @param iterations
     *            number of iterations to run.
     * @return the results of all completed tasks.
     */
    public List<UStats> benchMark(final int iterations) {
        return benchMark(iterations, 0, 100, 1000, TimeUnit.DAYS);
    }

    public void reportStats(String title, List<UStats> stats) {
        reportStats(title, stats, BY_FASTEST);
    }

    private void reportStats(String title, List<UStats> stats, Comparator<UStats> comparator) {
        
        if (title != null) {
            System.out.println(title);
            System.out.println(Stream.generate(() -> "=").limit(title.length()).collect(Collectors.joining()));
            System.out.println();
        }
        long mintime = stats.stream().mapToLong(s -> s.getFastestRawNanos()).min().getAsLong();
        TimeUnit tUnit = UStats.findBestUnit(mintime);
        stats.stream().sorted(comparator).map(stat -> stat.formatResults(tUnit)).forEach(System.out::println);
    }

    private TaskRunner[] getTasks(final int iterations, final int minStabilityLen, final double maxVariance,
            final long timeLimit, final TimeUnit timeUnit) {
        synchronized (tasks) {
            TaskRunner[] tr = new TaskRunner[tasks.size()];
            int pos = 0;
            for (Map.Entry<String, Task> me : tasks.entrySet()) {
                tr[pos++] = new TaskRunner(me.getKey(), me.getValue(), pos, iterations, minStabilityLen, maxVariance,
                        timeLimit, timeUnit);
            }
            return tr;
        }
    }

    @Override
    public String toString() {
        return String.format("%s with tasks: %s", suiteName, tasks.toString());
    }

}

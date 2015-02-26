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

    /**
     * At most a billion iterations of any task will be attempted.
     */
    public static final int MAX_RESULTS = 1_000_000_000;

    /**
     * A Comparator which sorts collections of UStats by the 95<sup>th</sup>
     * percentile time (ascending - fastest first)
     */
    public static final Comparator<UStats> BY_95PCTILE = Comparator.comparingLong(UStats::get95thPercentileNanos);
    /**
     * A Comparator which sorts collections of UStats by the 99<sup>th</sup>
     * percentile time (ascending - fastest first)
     */
    public static final Comparator<UStats> BY_99PCTILE = Comparator.comparingLong(UStats::get99thPercentileNanos);
    /**
     * A Comparator which sorts collections of UStats by the fastest time
     * (ascending - fastest first)
     */
    public static final Comparator<UStats> BY_FASTEST = Comparator.comparingLong(UStats::getFastestRawNanos);
    /**
     * A Comparator which sorts collections of UStats by the slowest time
     * (ascending - quickest of the slowest first)
     */
    public static final Comparator<UStats> BY_SLOWEST = Comparator.comparingLong(UStats::getFastestRawNanos);
    /**
     * A Comparator which sorts collections of UStats by the time consistency -
     * calculated as the slowest/fastest ratio (ascending - most consistent
     * first)
     */
    public static final Comparator<UStats> BY_CONSISTENCY = Comparator.comparingDouble(s -> s.getSlowestRawNanos()
            / (s.getFastestRawNanos() * 1.0));
    /**
     * A Comparator which sorts collections of UStats by the average time
     * (ascending - fastest first)
     */
    public static final Comparator<UStats> BY_AVERAGE = Comparator.comparingDouble(UStats::getAverageRawNanos);
    /**
     * A Comparator which sorts collections of UStats by the order in which they
     * were added to the UBench suite
     */
    public static final Comparator<UStats> BY_ADDED = Comparator.comparingDouble(UStats::getIndex);

    private final Map<String, Task> tasks = new LinkedHashMap<>();
    private final String suiteName;

    /**
     * Create a new UBench suite with the supplied name.
     * 
     * @param suiteName
     *            to be used in some automated reports.
     */
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
     * @param <T>
     *            The type of the task return value (which is the input to be
     *            tested in the validator)
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
     * @param <T>
     *            The type of the return value from the task. It is ignored.
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
     * Benchmark all added tasks until they complete the desired iteration
     * count, reaches stability, or exceed the given time limit, whichever comes
     * first.
     * 
     * @param mode
     *            The UMode execution model to use for task execution
     * @param iterations
     *            maximum number of iterations to run.
     * @param stableSpan
     *            If this many iterations in a row are all within the
     *            maxVariance, then the benchmark ends. A value less than, or
     *            equal to 0 turns off this check
     * @param stableBound
     *            Expressed as a percent from 0.0 to 100.0, and so on
     * @param timeLimit
     *            combined with the timeUnit, indicates how long to run tests
     *            for. A value less than or equal to 0 turns off this check.
     * @param timeUnit
     *            combined with the timeLimit, indicates how long to run tests
     *            for.
     * @return the results of all completed tasks.
     */
    public List<UStats> press(final UMode mode, final int iterations, final int stableSpan, final double stableBound,
            final long timeLimit, final TimeUnit timeUnit) {

        // make sense of any out-of-bounds input parameters.
        UMode vmode = mode == null ? UMode.INTERLEAVED : mode;
        int vit = iterations <= 0 ? 0 : Math.min(MAX_RESULTS, iterations);
        int vmin = (stableSpan <= 0 || stableBound <= 0) ? 0 : Math.min(stableSpan, vit);
        long vtime = timeLimit <= 0 ? 0 : (timeUnit == null ? 0 : timeUnit.toNanos(timeLimit));

        TaskRunner[] mytasks = getTasks(vit, vmin, stableBound, vtime);
        UStats[] ret = vmode.getModel().executeTasks(suiteName, mytasks);
        return Arrays.asList(ret);
    }

    /**
     * Benchmark all added tasks until they exceed the set time limit
     * 
     * @param mode
     *            The UMode execution model to use for task execution
     * @param timeLimit
     *            combined with the timeUnit, indicates how long to run tests
     *            for. A value less than or equal to 0 turns off this check.
     * @param timeUnit
     *            combined with the timeLimit, indicates how long to run tests
     *            for.
     * @return the results of all completed tasks.
     */
    public List<UStats> press(UMode mode, final long timeLimit, final TimeUnit timeUnit) {
        return press(mode, 0, 0, 0.0, timeLimit, timeUnit);
    }

    /**
     * Benchmark all added tasks until they complete the desired iteration
     * count.
     * 
     * @param mode
     *            The UMode execution model to use for task execution
     * @param iterations
     *            maximum number of iterations to run.
     * @return the results of all completed tasks.
     */
    public List<UStats> press(UMode mode, final int iterations) {
        return press(mode, iterations, 0, 0.0, 0, null);
    }

    /**
     * Benchmark all added tasks until they complete the desired iteration
     * count, reaches stability, or exceed the given time limit, whichever comes
     * first.
     * 
     * @param iterations
     *            maximum number of iterations to run.
     * @param stableSpan
     *            If this many iterations in a row are all within the
     *            maxVariance, then the benchmark ends. A value less than, or
     *            equal to 0 turns off this check
     * @param stableBound
     *            Expressed as a percent from 0.0 to 100.0, and so on
     * @param timeLimit
     *            combined with the timeUnit, indicates how long to run tests
     *            for. A value less than or equal to 0 turns off this check.
     * @param timeUnit
     *            combined with the timeLimit, indicates how long to run tests
     *            for.
     * @return the results of all completed tasks.
     */
    public List<UStats> press(final int iterations, final int stableSpan, final double stableBound,
            final long timeLimit, final TimeUnit timeUnit) {
        return press(null, iterations, stableSpan, stableBound, timeLimit, timeUnit);
    }

    /**
     * Benchmark all added tasks until they exceed the set time limit
     * 
     * @param timeLimit
     *            combined with the timeUnit, indicates how long to run tests
     *            for. A value less than or equal to 0 turns off this check.
     * @param timeUnit
     *            combined with the timeLimit, indicates how long to run tests
     *            for.
     * @return the results of all completed tasks.
     */
    public List<UStats> press(final long timeLimit, final TimeUnit timeUnit) {
        return press(null, timeLimit, timeUnit);
    }

    /**
     * Benchmark all added tasks until they complete the desired iteration
     * count.
     * 
     * @param iterations
     *            maximum number of iterations to run.
     * @return the results of all completed tasks.
     */
    public List<UStats> press(final int iterations) {
        return press(null, iterations);
    }

    /**
     * Generate and print (System.out) the statistics report with the supplied
     * title, and using the default ({@link UBench#BY_ADDED}) sort order.
     * 
     * @param title
     *            the title to use (e.g. "Warmup", "Cached Files", etc.)
     * @param stats
     *            the UStats data to report
     */
    public void report(String title, List<UStats> stats) {
        report(title, stats, null);
    }

    /**
     * Generate and print (System.out) the statistics report with the supplied
     * title, and using the specified sort order.
     * 
     * @param title
     *            the title to use (e.g. "Warmup", "Cached Files", etc.)
     * @param stats
     *            the UStats data to report
     * @param comparator
     *            the Comparator to sort the UStats by (see class constants for
     *            some useful suggestions)
     */
    public void report(String title, List<UStats> stats, Comparator<UStats> comparator) {

        if (title != null) {
            System.out.println(title);
            System.out.println(Stream.generate(() -> "=").limit(title.length()).collect(Collectors.joining()));
            System.out.println();
        }
        Comparator<UStats> comp = comparator != null ? comparator : BY_ADDED;
        long mintime = stats.stream().mapToLong(s -> s.getFastestRawNanos()).min().getAsLong();
        TimeUnit tUnit = UStats.findBestUnit(mintime);
        stats.stream().sorted(comp).map(stat -> stat.formatResults(tUnit)).forEach(System.out::println);
    }

    private TaskRunner[] getTasks(final int count, final int stabLength, final double stabVariance, final long timeLimit) {
        synchronized (tasks) {
            TaskRunner[] tr = new TaskRunner[tasks.size()];
            int pos = 0;
            for (Map.Entry<String, Task> me : tasks.entrySet()) {
                tr[pos++] = new TaskRunner(me.getKey(), me.getValue(), pos, count, stabLength, stabVariance, timeLimit);
            }
            return tr;
        }
    }

    @Override
    public String toString() {
        return String.format("%s with tasks: %s", suiteName, tasks.toString());
    }

}

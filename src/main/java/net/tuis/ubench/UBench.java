package net.tuis.ubench;

import java.util.Arrays;
import java.util.LinkedHashMap;
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
import java.util.logging.Logger;

/**
 * The UBench class encompasses a suite of tasks that are to be compared...
 * possibly relative to each other.
 * <p>
 * Each task can be added to the suite. Once you have the tasks you need, then
 * all tasks can be benchmarked according to limits given in the run.
 * 
 * <hr>
 * Example usages - which is faster, <code>Arrays.sort(...)</code>, or <code>IntStream.sorted()</code>?:
 * <pre>
        Random random = new Random();

        // create an array of 10,000 random integer values.
        final int[] data = IntStream.generate(random::nextInt).limit(10000).toArray();
        // create a sorted version, trust the algorithm for the moment.
        final int[] sorted = Arrays.stream(data).sorted().toArray();
        // a way to ensure the value is in fact sorted.
        Predicate&lt;int[]&gt; validate = v -&gt; Arrays.equals(v, sorted);
        
        // A stream-based way to sort an array of integers.
        Supplier&lt;int[]&gt; stream = () -&gt; Arrays.stream(data).sorted().toArray();
        
        // The traditional way to sort an array of integers.
        Supplier&lt;int[]&gt; trad = () -&gt; {
            int[] copy = Arrays.copyOf(data, data.length);
            Arrays.sort(copy);
            return copy;
        };
        
        UBench bench = new UBench("Sort Algorithms")
              .addTask("Functional", stream, validate);
              .addTask("Traditional", trad, validate);
              .press(10000)
              .report("With Warmup");
 * </pre>
 * 
 * You can expect results similar to:
 * 
 * <pre>

        With Warmup
        ===========
        
        Task Sort Algorithms -&gt; Functional: (Unit: MILLISECONDS)
          Count    :    10000      Average  :   0.4576
          Fastest  :   0.4194      Slowest  :   4.1327
          95Pctile :   0.5030      99Pctile :   0.6028
          TimeBlock : 0.493 0.436 0.443 0.459 0.458 0.454 0.457 0.458 0.463 0.456
          Histogram :  9959    19    21     1
        
        Task Sort Algorithms -&gt; Traditional: (Unit: MILLISECONDS)
          Count    :    10000      Average  :   0.4219
          Fastest  :   0.4045      Slowest  :   3.6714
          95Pctile :   0.4656      99Pctile :   0.5420
          TimeBlock : 0.459 0.417 0.418 0.417 0.416 0.416 0.416 0.419 0.423 0.417
          Histogram :  9971    18    10     1


 * </pre>
 * 
 * See {@link UStats} for more details on what the statistics mean.
 * 
 * @author rolf
 *
 */
public final class UBench {
    
    private static final Logger LOGGER = UUtils.getLogger(UBench.class); 

    /**
     * At most a billion iterations of any task will be attempted.
     */
    public static final int MAX_RESULTS = 1_000_000_000;

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
        LOGGER.info(() -> String.format("Creating UBench for suite %s", suiteName));
    }

    private UBench putTask(String name, Task t) {
        synchronized (tasks) {
            tasks.put(name, t);
        }
        LOGGER.fine(() -> String.format("UBench suite %s: adding task %s", suiteName, name));
        return this;
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
     * @return The same object, for chaining calls.
     */
    public <T> UBench addTask(String name, Supplier<T> task, Predicate<T> check) {
        return putTask(name, () -> {
            long start = System.nanoTime();
            T result = task.get();
            long time = System.nanoTime() - start;
            if (check != null && !check.test(result)) {
                throw new UBenchRuntimeException(String.format("Task %s failed Result: %s", name, result));
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
     *
     * @return The same object, for chaining calls.
     */
    public <T> UBench addTask(String name, Supplier<T> task) {
        return addTask(name, task, null);
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
     * @return The same object, for chaining calls.
     */
    public UBench addIntTask(String name, IntSupplier task, IntPredicate check) {
        return putTask(name, () -> {
            long start = System.nanoTime();
            int result = task.getAsInt();
            long time = System.nanoTime() - start;
            if (check != null && !check.test(result)) {
                throw new UBenchRuntimeException(String.format("Task %s failed Result: %d", name, result));
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
     * @return The same object, for chaining calls.
     */
    public UBench addIntTask(String name, IntSupplier task) {
        return addIntTask(name, task, null);
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
     * @return The same object, for chaining calls.
     */
    public UBench addLongTask(String name, LongSupplier task, LongPredicate check) {
        return putTask(name, () -> {
            long start = System.nanoTime();
            long result = task.getAsLong();
            long time = System.nanoTime() - start;
            if (check != null && !check.test(result)) {
                throw new UBenchRuntimeException(String.format("Task %s failed Result: %d", name, result));
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
     * @return The same object, for chaining calls.
     */
    public UBench addLongTask(String name, LongSupplier task) {
        return addLongTask(name, task, null);
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
     * @return The same object, for chaining calls.
     */
    public UBench addDoubleTask(String name, DoubleSupplier task, DoublePredicate check) {
        return putTask(name, () -> {
            long start = System.nanoTime();
            double result = task.getAsDouble();
            long time = System.nanoTime() - start;
            if (check != null && !check.test(result)) {
                throw new UBenchRuntimeException(String.format("Task %s failed Result: %f", name, result));
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
     * @return The same object, for chaining calls.
     */
    public UBench addDoubleTask(String name, DoubleSupplier task) {
        return addDoubleTask(name, task, null);
    }
    
    
    /**
     * Include a named task that has no output value in to the benchmark.
     * 
     * @param name
     *            The name of the task. Only one task with any one name is
     *            allowed.
     * @param task
     *            The task to perform
     * @return The same object, for chaining calls.
     */
    public UBench addTask(String name, Runnable task) {
        return putTask(name, () -> {
            long start = System.nanoTime();
            task.run();
            return System.nanoTime() - start;
        });
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
    public UReport press(final UMode mode, final int iterations, final int stableSpan, final double stableBound,
            final long timeLimit, final TimeUnit timeUnit) {

        // make sense of any out-of-bounds input parameters.
        UMode vmode = mode == null ? UMode.INTERLEAVED : mode;
        int vit = iterations <= 0 ? 0 : Math.min(MAX_RESULTS, iterations);
        int vmin = (stableSpan <= 0 || stableBound <= 0) ? 0 : Math.min(stableSpan, vit);
        long vtime = timeLimit <= 0 ? 0 : (timeUnit == null ? 0 : timeUnit.toNanos(timeLimit));
        
        final long start = System.nanoTime();
        LOGGER.fine(() -> String.format("UBench suite %s: running all tasks in mode %s", suiteName, vmode.name()));

        TaskRunner[] mytasks = getTasks(vit, vmin, stableBound, vtime);
        UStats[] ret = vmode.getModel().executeTasks(suiteName, mytasks);
        
        final long time = System.nanoTime() - start;
        LOGGER.info(() -> String.format("UBench suite %s: completed benchmarking all tasks using mode %s in %.3fms", 
                suiteName, vmode.name(), time / 1000000.0));
        
        return new UReport(Arrays.asList(ret));
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
    public UReport press(UMode mode, final long timeLimit, final TimeUnit timeUnit) {
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
    public UReport press(UMode mode, final int iterations) {
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
    public UReport press(final int iterations, final int stableSpan, final double stableBound,
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
    public UReport press(final long timeLimit, final TimeUnit timeUnit) {
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
    public UReport press(final int iterations) {
        return press(null, iterations);
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

    /**
     * Return the name this UBench suite was created with.
     * @return the name of this suite.
     */
    public String getSuiteName() {
        return suiteName;
    }

}

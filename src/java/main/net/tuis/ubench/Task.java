package net.tuis.ubench;

import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Tasks represent actions which can be benchmarked on the MicroBench tool.
 * <p>
 * This class encompasses the concept of running code, timing that run, and
 * being aware that the code may need to check the result outside of the timed
 * portion. The calling code will effectively perform:
 * 
 * <pre>
 * R result = perform()
 * if (!check(result)) {
 *     throw new IllegalStateException(...);
 * }
 * </pre>
 * 
 * but the <code>perform()</code> call will be timed.
 * 
 * 
 * @author rolf
 *
 * @param <S>
 *            The type of data that can be created before each task is run.
 * @param <R>
 *            The type of the result that the task produces.
 */
public abstract class Task<R> {

    /**
     * Build a task that ensures the function produces the correct result
     * (result is compared with the equivalent of
     * <code>Objects.equals(expect, function.get())</code> )
     * 
     * @param name
     *            The task name
     * @param benchmark
     *            The supplier that produces a result, the item that is
     *            benchmarked.
     * @param expect
     *            the value the benchmarked code is expected to produce
     * @return the Task ready to be added to the MicroBench tool.
     */
    public static final <T> Task<T> buildCheckedTask(final String name, final Supplier<T> benchmark, final T expect) {
        return new Task<T>(name) {

            @Override
            protected T perform() throws Exception {
                return benchmark.get();
            }

            @Override
            protected boolean check(T result) {
                return Objects.equals(expect, result);
            }

        };
    }

    /**
     * Build a task that ensures the function produces the correct result
     * (result is compared with the equivalent of
     * <code>function.getAsInt() == expect</code> )
     * 
     * @param name
     *            The task name
     * @param benchmark
     *            The supplier that produces a result, the item that is
     *            benchmarked.
     * @param expect
     *            the value the benchmarked code is expected to produce
     * @return the Task ready to be added to the MicroBench tool.
     */
    public static final Task<?> buildCheckedIntTask(final String name, final IntSupplier benchmark, final int expect) {
        return new Task<Boolean>(name) {

            @Override
            protected Boolean perform() throws Exception {
                int got = benchmark.getAsInt();
                return Boolean.valueOf(expect == got);
            }

            @Override
            protected boolean check(Boolean result) {
                return result.booleanValue();
            }

        };
    }

    /**
     * Build a task that just runs the benchmark code.
     * 
     * @param name
     *            The task name
     * @param benchmark
     *            The supplier that produces a result, the item that is
     *            benchmarked.
     * @return the Task ready to be added to the MicroBench tool.
     */

    public static final Task<?> buildVoidTask(final String name, final Runnable function) {
        return new Task<Object>(name) {

            @Override
            protected Object perform() throws Exception {
                function.run();
                return null;
            }

            @Override
            protected boolean check(Object result) {
                return true;
            }

        };
    }

    private final String name;

    /**
     * Create a new Task instance (expected to inherit this class).
     * 
     * @param name
     *            The task name (used in reports).
     */
    public Task(String name) {
        this.name = name;
    }

    /**
     * The task's name
     * 
     * @return the name this task was created with.
     */
    public final String getName() {
        return name;
    }

    /**
     * Execute one iteration of the task, producing a result useful for
     * statistics.
     * 
     * @return The runtime results of the task.
     * @throws IllegalStateException
     *             if the task being tested throws an exception, or does not
     *             match the expected value.
     */
    final long compute() {
        try {

            long start = System.nanoTime();

            R r = perform();

            long done = System.nanoTime();

            if (!check(r)) {
                throw new IllegalStateException(String.format("Unexpected result in task %s -> %s", name, r));
            }

            return done - start;
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Failed execution in %s with %s", name, e.getMessage()), e);
        }
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Perform the benchmarked code. This is the time-critical aspect.
     * 
     * @return the value the benchmark should produce
     * @throws Exception
     *             if there is an execution problem
     */
    protected abstract R perform() throws Exception;

    /**
     * Check the results of execution. Simply return true for unchecked runs.
     * The timing of this code is not critical
     * 
     * @param result
     *            The result from the perform method
     * @return true if the check passed.
     */
    protected abstract boolean check(R result);

}
package net.tuis.ubench;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongFunction;

/**
 * Factory class and reporting instances that allow functions to be tested for
 * scalability.
 * 
 * @author rolf
 * @author zomis
 *
 */
public class UScale {

    private static final int MAX_ITERATIONS = 1000000;

    private static final class ScaleResult {
        final int scale;
        final UStats stats;

        ScaleResult(int scale, UStats stats) {
            super();
            this.scale = scale;
            this.stats = stats;
        }

        public int getScale() {
            return scale;
        }

        public UStats getStats() {
            return stats;
        }

    }

    @FunctionalInterface
    private interface TaskRunnerBuilder {
        TaskRunner build(String name, int scale);
    }

    private final List<ScaleResult> stats;

    private UScale(List<ScaleResult> stats) {
        this.stats = stats;
    }

    /**
     * Generate and print (System.out) the scalability report.
     */
    public void report() {
        stats.stream()
                .sorted(Comparator.comparingInt(ScaleResult::getScale))
                .map(sr -> String.format("Scale %4d -> %8d (count %d)\n", sr.getScale(), sr.getStats()
                        .getAverageRawNanos(), sr.getStats().getCount())).forEach(System.out::println);
    }

    /**
     * Test the scalability of a function that requires T input data.
     * 
     * @param <T>
     *            the type of the input data
     * @param function
     *            the function that processes the T data
     * @param scaler
     *            a supplier that can supply T data of different sizes
     * @param reusedata
     *            if true, data of each size will be created just once, and
     *            reused often.
     * @return A UScale instance containing the results of the testing
     */
    public static <T> UScale scale(Function<T, ?> function, IntFunction<T> scaler, final boolean reusedata) {

        final ScaleControl<T> scontrol = new ScaleControl<>(function, scaler, reusedata);

        final TaskRunnerBuilder builder = (name, scale) -> scontrol.buildTask(name, scale);

        return scaleMapper(builder);
    }

    /**
     * Test the scalability of a function that requires an input integer.
     * 
     * @param function
     *            the function that processes the input
     * @param scaler
     *            a supplier that can supply data of different sizes in
     *            proportion to the supply value
     * @return A UScale instance containing the results of the testing
     */
    public static UScale scale(IntFunction<?> function, IntUnaryOperator scaler) {
        return scaleMapper((name, scale) -> buildIntTask(name, MAX_ITERATIONS, function, scaler.applyAsInt(scale)));
    }

    /**
     * Test the scalability of a function that requires an input long.
     * 
     * @param function
     *            the function that processes the input
     * @param scaler
     *            a supplier that can supply data of different sizes in
     *            proportion to the supply value
     * @return A UScale instance containing the results of the testing
     */
    public static UScale scale(LongFunction<?> function, IntToLongFunction scaler) {
        return scaleMapper((name, scale) -> buildLongTask(name, MAX_ITERATIONS, function, scaler.applyAsLong(scale)));
    }

    /**
     * Test the scalability of a function that requires an input double.
     * 
     * @param function
     *            the function that processes the input
     * @param scaler
     *            a supplier that can supply data of different sizes in
     *            proportion to the supply value
     * @return A UScale instance containing the results of the testing
     */
    public static UScale scale(DoubleFunction<?> function, IntToDoubleFunction scaler) {
        return scaleMapper((name, scale) -> buildDoubleTask(name, MAX_ITERATIONS, function, scaler.applyAsDouble(scale)));
    }

    private static final UScale scaleMapper(TaskRunnerBuilder scaleBuilder) {
        UMode.PARALLEL.getModel().executeTasks("Warmup", scaleBuilder.build("warmup", 2));

        final List<ScaleResult> results = new ArrayList<>(20);

        for (int i = 1; i <= 600000; i *= 2) {

            final String runName = "Scale " + i;

            final TaskRunner runner = scaleBuilder.build(runName, i);

            final UStats wstats = UMode.SEQUENTIAL.getModel().executeTasks(runName, runner)[0];
            results.add(new ScaleResult(i, wstats));
            if (wstats.getCount() <= 3) {
                break;
            }
        }

        return new UScale(results);
    }

    private static final TaskRunner buildRunner(final String name, final int max, final Task task) {
        return new TaskRunner(name, task, 0, max, 0, 0.0, TimeUnit.SECONDS.toNanos(1));
    }

    private static TaskRunner buildIntTask(final String name, final int max, final IntFunction<?> function,
            final int scale) {
        return buildRunner(name, max, () -> {

            long start = System.nanoTime();
            function.apply(scale);
            long time = System.nanoTime() - start;
            return time;

        });
    }

    private static TaskRunner buildLongTask(final String name, final int max, final LongFunction<?> function,
            final long scale) {
        return buildRunner(name, max, () -> {

            long start = System.nanoTime();
            function.apply(scale);
            long time = System.nanoTime() - start;
            return time;

        });
    }

    private static TaskRunner buildDoubleTask(final String name, final int max, final DoubleFunction<?> function,
            final double scale) {
        return buildRunner(name, max, () -> {

            long start = System.nanoTime();
            function.apply(scale);
            long time = System.nanoTime() - start;
            return time;

        });
    }

}

package net.tuis.ubench;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Factory class and reporting instances that allow functions to be tested for
 * scalability.
 * 
 * @author rolf
 * @author zomis
 *
 */
public class UScale {

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
     * Test the scalability of a consumer that requires T input data.
     * <p>
     * This method calls <code>scale(Consumer, IntFunction, boolean)</code> with
     * the reusedata parameter set to true:
     * 
     * <pre>
     * return scale(function, scaler, true);
     * </pre>
     * 
     * This means that the data will be generated once for each scale factor,
     * and reused.
     * 
     * @param <T>
     *            the type of the input data
     * @param consumer
     *            the Consumer that processes the T data
     * @param scaler
     *            a supplier that can supply T data of different sizes
     * @return A UScale instance containing the results of the testing
     */
    public static <T> UScale scale(Consumer<T> consumer, IntFunction<T> scaler) {
        return scale(consumer, scaler, true);
    }

    /**
     * Test the scalability of a consumer that requires T input data.
     * 
     * @param <T>
     *            the type of the input data
     * @param consumer
     *            the comsumer that processes the T data
     * @param scaler
     *            a supplier that can supply T data of different sizes
     * @param reusedata
     *            if true, data of each size will be created just once, and
     *            reused often.
     * @return A UScale instance containing the results of the testing
     */
    public static <T> UScale scale(Consumer<T> consumer, IntFunction<T> scaler, final boolean reusedata) {

        final ScaleControl<T> scontrol = new ScaleControl<>(consumer, scaler, reusedata);

        final TaskRunnerBuilder builder = (name, scale) -> scontrol.buildTask(name, scale);

        return scaleMapper(builder);
    }

    /**
     * Test the scalability of a consumer that requires T input data.
     * <p>
     * This method calls <code>scale(Consumer, IntFunction, boolean)</code> with
     * the reusedata parameter set to true:
     * 
     * <pre>
     * return scale(function, scaler, true);
     * </pre>
     * 
     * This means that the data will be generated once for each scale factor,
     * and reused.
     * 
     * @param <T>
     *            the type of the input data
     * @param computer
     *            the Function that computes the T data
     * @param scaler
     *            a supplier that can supply T data of different sizes
     * @return A UScale instance containing the results of the testing
     */
    public static <T> UScale scale(Function<T, ?> computer, IntFunction<T> scaler) {
        return computer(computer, scaler, true);
    }

    /**
     * Test the scalability of a consumer that requires T input data.
     * 
     * @param <T>
     *            the type of the input data
     * @param computer
     *            the computer that processes the T data
     * @param scaler
     *            a supplier that can supply T data of different sizes
     * @param reusedata
     *            if true, data of each size will be created just once, and
     *            reused often.
     * @return A UScale instance containing the results of the testing
     */
    public static <T> UScale computer(Function<T, ?> computer, IntFunction<T> scaler, final boolean reusedata) {
        return scale((t) -> computer.apply(t), scaler, reusedata);
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

}

package net.tuis.ubench;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Factory class and reporting instances that allow functions to be tested for
 * scalability.
 * 
 * @author rolf
 * @author zomis
 *
 */
public class UScale {
    
    private static final long NANO_TICK = computeTick();
    private static final int SCALE_LIMIT = 12_000_000;

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
    
    private static final long singleTick() {
        
        final long start = System.nanoTime();
        long end = start;
        while (end == start) {
            end = System.nanoTime();
        }
//        System.out.printf("Nano Resolution %d\n", end - start);
        return end - start;
        
    }

    private static long computeTick() {
        return LongStream.range(0, 1000).map(i -> singleTick()).min().getAsLong();
    }

    /**
     * Generate and print (System.out) the scalability report.
     */
    public void report() {
        stats.stream()
                .sorted(Comparator.comparingInt(ScaleResult::getScale))
                .map(sr -> String.format(
                        "Scale %4d -> %8d (count %d, threshold %d)\n", 
                        sr.getScale(), sr.getStats().getAverageRawNanos(), sr.getStats().getCount(), NANO_TICK))
                .forEach(System.out::println);
    }

    /**
     * Get the data as JSON data in an array format (<code>[ [scale,nanos], ...]</code>
     * @return a JSON formatted string containing the raw data.
     */
    public String json() {
        return stats.stream().sorted(Comparator.comparingInt(ScaleResult::getScale))
                .map(sr -> String.format("  [%d,%d,%d,%d]", sr.getScale(), sr.getStats().getFastestNanos(), sr.getStats().getCount(), NANO_TICK))
                .collect(Collectors.joining(",\n", "[\n", "\n]"));

    }
    
    private static String readResource(String path) {
        try (InputStream is = ExampleScales.class.getClassLoader().getResourceAsStream(path);) {
            int len = 0;
            byte[] buffer = new byte[2048];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((len = is.read(buffer)) >= 0) {
                baos.write(buffer, 0, len);
            }
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read class loaded stream " + path, e);
        }
    }
    
    /**
     * Create an HTML document (with data and chart) plotting the performance.
     * @param title The Title for the target page.
     * @param target the destination to store the HTML document at.
     * @throws IOException
     */
    public void reportHTML(String title, Path target) throws IOException {
        
        Files.createDirectories(target.toAbsolutePath().getParent());
        
        String html = readResource("net/tuis/ubench/scale/UScale.html");
        Map<String, String> subs = new HashMap<>();
        subs.put("DATA", json());
        subs.put("TITLE", title);
        for (Map.Entry<String, String> me : subs.entrySet()) {
            html = html.replaceAll(Pattern.quote("${" + me.getKey() + "}"), Matcher.quoteReplacement(me.getValue()));
        }
        Files.write(target, html.getBytes(StandardCharsets.UTF_8));
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
     *            the type of the input data needed by the Function
     * @param function
     *            the Function that computes the T data
     * @param scaler
     *            a supplier that can supply T data of different sizes
     * @return A UScale instance containing the results of the testing
     */
    public static <T> UScale function(Function<T, ?> function, IntFunction<T> scaler) {
        return function(function, scaler, true);
    }

    /**
     * Test the scalability of a consumer that requires T input data.
     * 
     * @param <T>
     *            the type of the input data needed by the Function
     * @param function
     *            the computer that processes the T data
     * @param scaler
     *            a supplier that can supply T data of different sizes
     * @param reusedata
     *            if true, data of each size will be created just once, and
     *            reused often.
     * @return A UScale instance containing the results of the testing
     */
    public static <T> UScale function(Function<T, ?> function, IntFunction<T> scaler, final boolean reusedata) {
        return consumer(function::apply, scaler, reusedata);
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
     *            the type of the input data needed by the Consumer
     * @param consumer
     *            the Consumer that processes the T data
     * @param scaler
     *            a supplier that can supply T data of different sizes
     * @return A UScale instance containing the results of the testing
     */
    public static <T> UScale consumer(Consumer<T> consumer, IntFunction<T> scaler) {
        return consumer(consumer, scaler, true);
    }

    /**
     * Test the scalability of a consumer that requires T input data.
     * 
     * @param <T>
     *            the type of the input data needed by the Consumer
     * @param consumer
     *            the Consumer that processes the T data
     * @param scaler
     *            a supplier that can supply T data of different sizes
     * @param reusedata
     *            if true, data of each size will be created just once, and
     *            reused often.
     * @return A UScale instance containing the results of the testing
     */
    public static <T> UScale consumer(Consumer<T> consumer, IntFunction<T> scaler, final boolean reusedata) {

        final ScaleControl<T> scontrol = new ScaleControl<>(consumer, scaler, reusedata);

        final TaskRunnerBuilder builder = (name, scale) -> scontrol.buildTask(name, scale);

        return scaleMapper(builder);
    }

    private static final UScale scaleMapper(final TaskRunnerBuilder scaleBuilder) {
        UMode.PARALLEL.getModel().executeTasks("Warmup", scaleBuilder.build("warmup", 2));

        final List<ScaleResult> results = new ArrayList<>(20);
        
        for (int i = 1; i <= SCALE_LIMIT; i *= 2) {

            results.add(new ScaleResult(i, runStats(i, scaleBuilder)));
            if (results.get(results.size() -1).getStats().getCount() <= 3) {
                break;
            }
        }
        if (results.size() > 4) {
            final int last = results.get(results.size() - 1).getScale();
            int step = last >> 3;
            for (int j = last - step; j > step; j -= step) {
                if (j == last >> 1 || j == last >> 2) {
                    continue;
                }
                results.add(new ScaleResult(j, runStats(j, scaleBuilder)));
            }
        }

        return new UScale(results);
    }

    private static UStats runStats(int i, TaskRunnerBuilder scaleBuilder) {
        final String runName = "Scale " + i;

        final TaskRunner runner = scaleBuilder.build(runName, i);

        return UMode.SEQUENTIAL.getModel().executeTasks(runName, runner)[0];
    }

}

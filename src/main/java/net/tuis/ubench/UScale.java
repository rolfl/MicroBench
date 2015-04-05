package net.tuis.ubench;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.tuis.ubench.scale.MathEquation;

/**
 * Factory class and reporting instances that allow functions to be tested for
 * scalability.
 * 
 * @author rolf
 * @author Simon Forsberg
 *
 */
public class UScale {
    
    private static final Logger LOGGER = UUtils.getLogger(UScale.class);
    
    private static final int SCALE_LIMIT = 12_000_000;
    
    @FunctionalInterface
    private interface TaskRunnerBuilder {
        TaskRunner build(String name, int scale);
    }

    private final List<UStats> stats;
    private final String title;

    private UScale(String title, List<UStats> stats) {
        this.title = title;
        this.stats = stats;
    }
    
    /**
     * Get the name this UScale report was titled with.
     * @return the title used when created
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Generate and print (System.out) the scalability report.
     */
    public void report() {
        try (Writer w = new NonClosingSystemOut()) {
            report(w);
        } catch (IOException e) {
            throw new IllegalStateException("Should never be an exception writing to System.out", e);
        }
    }
    
    /**
     * Generate and print (System.out) the scalability report.
     * @param writer The writer to write the report to
     * @throws IOException in the event that the writer throws one. 
     */
    public void report(Writer writer) throws IOException {
        String report = stats.stream()
                .sorted(Comparator.comparingInt(UStats::getIndex))
                .map(sr -> String.format(
                        "Scale %4d -> %8d (count %d, threshold %d)", 
                        sr.getIndex(), sr.getAverageRawNanos(), sr.getCount(), UUtils.getNanoTick()))
                .collect(Collectors.joining("\n"));
        MathEquation bestFit = determineBestFit();
        writer.write(title);
        char[] uls = new char[title.length()];
        Arrays.fill(uls, '=');
        writer.write(uls);
        writer.write(report);
        writer.write("Best fit is: " + bestFit + "\n");
        writer.flush();
    }

    public MathEquation determineBestFit() {
        return ScaleDetect.detect(this);
    }

    public MathEquation[] fitEquations() {
        return ScaleDetect.rank(this);
    }

    /**
     * Get the data as JSON data in an array format (<code>[ [scale,nanos], ...]</code>
     * @return a JSON formatted string containing the raw data.
     */
    public String toJSONString() {
        
        String rawdata = stats.stream().sorted(Comparator.comparingInt(UStats::getIndex))
                .map(sr -> sr.toJSONString())
                .collect(Collectors.joining(",\n    ", "[\n    ", "\n ]"));
        
        String fields = Stream.of(UStats.getJSONFields()).collect(Collectors.joining("\", \"", "[\"", "\"]"));
        
        String models = Stream.of(ScaleDetect.rank(this)).map(me -> me.toJSONString()).collect(Collectors.joining(",\n    ", "[\n    ", "\n  ]"));
        
        return String.format("{ title: \"%s\",\n  nano_tick: %d,\n  models: %s,\n  fields: %s,\n  data: %s\n}",
                title, UUtils.getNanoTick(), models, fields, rawdata);
    }
    
    /**
     * Create an HTML document (with data and chart) plotting the performance.
     * @param target the destination to store the HTML document at.
     * @throws IOException if there is a problem writing to the target path
     */
    public void reportHTML(final Path target) throws IOException {
        
        Files.createDirectories(target.toAbsolutePath().getParent());
        
        LOGGER.info(() -> "Preparing HTML Report '" + title + "' to path: " + target);
        
        String html = UUtils.readResource("net/tuis/ubench/scale/UScale.html");
        Map<String, String> subs = new HashMap<>();
        subs.put("DATA", toJSONString());
        
        subs.put("TITLE", title);
        for (Map.Entry<String, String> me : subs.entrySet()) {
            html = html.replaceAll(Pattern.quote("${" + me.getKey() + "}"), Matcher.quoteReplacement(me.getValue()));
        }
        Files.write(target, html.getBytes(StandardCharsets.UTF_8));
        LOGGER.info(() -> "Completed HTML Report '" + title + "' to path: " + target);
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
     * @param title 
     *            the title to apply to all reports and results
     * @param function
     *            the Function that computes the T data
     * @param scaler
     *            a supplier that can supply T data of different sizes
     * @return A UScale instance containing the results of the testing
     */
    public static <T> UScale function(String title, Function<T, ?> function, IntFunction<T> scaler) {
        return function(title, function, scaler, true);
    }

    /**
     * Test the scalability of a consumer that requires T input data.
     * 
     * @param <T>
     *            the type of the input data needed by the Function
     * @param title 
     *            the title to apply to all reports and results
     * @param function
     *            the computer that processes the T data
     * @param scaler
     *            a supplier that can supply T data of different sizes
     * @param reusedata
     *            if true, data of each size will be created just once, and
     *            reused often.
     * @return A UScale instance containing the results of the testing
     */
    public static <T> UScale function(String title, Function<T, ?> function, IntFunction<T> scaler, final boolean reusedata) {
        return consumer(title, function::apply, scaler, reusedata);
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
     * @param title 
     *            the title to apply to all reports and results
     * @param consumer
     *            the Consumer that processes the T data
     * @param scaler
     *            a supplier that can supply T data of different sizes
     * @return A UScale instance containing the results of the testing
     */
    public static <T> UScale consumer(String title, Consumer<T> consumer, IntFunction<T> scaler) {
        return consumer(title, consumer, scaler, true);
    }

    /**
     * Test the scalability of a consumer that requires T input data.
     * 
     * @param <T>
     *            the type of the input data needed by the Consumer
     * @param title 
     *            the title to apply to all reports and results
     * @param consumer
     *            the Consumer that processes the T data
     * @param scaler
     *            a supplier that can supply T data of different sizes
     * @param reusedata
     *            if true, data of each size will be created just once, and
     *            reused often.
     * @return A UScale instance containing the results of the testing
     */
    public static <T> UScale consumer(String title, Consumer<T> consumer, IntFunction<T> scaler, final boolean reusedata) {

        final ScaleControl<T> scontrol = new ScaleControl<>(consumer, scaler, reusedata);

        final TaskRunnerBuilder builder = (name, scale) -> scontrol.buildTask(name, scale);

        return scaleMapper(title, builder);
    }

    private static final UScale scaleMapper(final String title, final TaskRunnerBuilder scaleBuilder) {
        LOGGER.info(title + ": Starting Scalability testing");
        final long start = System.nanoTime();
        LOGGER.finer("warming up task");
        UStats[] rep = UMode.PARALLEL.getModel().executeTasks("Warmup", scaleBuilder.build("warmup", 2));
        LOGGER.fine(() -> "Warmed up results:\n" + rep[0].toString());

        final List<UStats> results = new ArrayList<>(20);
        
        for (int i = 1; i <= SCALE_LIMIT; i *= 2) {
            
            results.add(runStats(title, i, scaleBuilder));
            
            if (results.get(results.size() -1).getCount() <= 3) {
                break;
            }
        }
        if (results.size() > 4) {
            final int last = results.get(results.size() - 1).getIndex();
            int step = last >> 3;
            for (int j = last - step; j > step; j -= step) {
                if (j == last >> 1 || j == last >> 2) {
                    continue;
                }
                results.add(runStats(title, j, scaleBuilder));
            }
        }
        
        LOGGER.info(String.format("%s: Completed tests in %.3fms", title, (System.nanoTime() - start) / 1000000.0));

        return new UScale(title, results);
    }

    private static UStats runStats(String title, int i, TaskRunnerBuilder scaleBuilder) {
        final String runName = title + ": Scale " + i;
        
        final long beg = System.nanoTime();

        final TaskRunner runner = scaleBuilder.build(runName, i);
        
        LOGGER.finer(() -> String.format("Built data for %s in %.3fms", runName, (System.nanoTime() - beg) / 1000000.0));

        UStats stats = UMode.SEQUENTIAL.getModel().executeTasks(runName, runner)[0];
        
        if (LOGGER.isLoggable(Level.INFO)) {
            final long time = System.nanoTime() - beg;
            LOGGER.fine(() -> String.format("Completed scale test %s in %.3fms", runName, time / 1000000.0));
        }
        return stats;
    }

    /**
     * Obtain a copy of the statistics produced for this UScale instance.
     * @return a copy of the underlying statistics.
     */
    public List<UStats> getStats() {
        return new ArrayList<>(stats);
    }
}

package net.tuis.ubench;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The UReport class encapsulates the results that are produced by
 * {@link UBench#press(UMode, int, int, double, long, TimeUnit)} and exposes
 * some convenient reporting methods.
 */
public class UReport {

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
    public static final Comparator<UStats> BY_FASTEST = Comparator.comparingLong(UStats::getFastestNanos);
    /**
     * A Comparator which sorts collections of UStats by the slowest time
     * (ascending - quickest of the slowest first)
     */
    public static final Comparator<UStats> BY_SLOWEST = Comparator.comparingLong(UStats::getFastestNanos);
    /**
     * A Comparator which sorts collections of UStats by the time consistency -
     * calculated as the slowest/fastest ratio (ascending - most consistent
     * first)
     */
    public static final Comparator<UStats> BY_CONSISTENCY = Comparator.comparingDouble(s -> s.getSlowestNanos()
            / (s.getFastestNanos() * 1.0));
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

    private final List<UStats> stats;

    /**
     * Construct a report container that includes all the specified statistics.
     * 
     * @param stats
     *            the statistics to report on.
     */
    public UReport(List<UStats> stats) {
        super();
        this.stats = stats;
    }

    /**
     * Retrieve the raw statistics this Report would deliver, sorted in the
     * order they were added to the benchmark.
     * 
     * @return the raw statistics.
     */
    public List<UStats> getStats() {
        return getStats(null);
    }

    /**
     * Retrieve the raw statistics this Report would deliver, sorted in the
     * order specified (or the order they were added to the benchmark, if null).
     * 
     * @param comparator
     *            The comparator to sort the results by.
     * @return the statistics in the specified order.
     */
    public List<UStats> getStats(final Comparator<UStats> comparator) {
        List<UStats> result = new ArrayList<>(stats);
        if (comparator != null) {
            result.sort(comparator);
        }
        return result;
    }

    /**
     * Simple helper method that prints the specified title, underlined with '='
     * characters.
     * 
     * @param writer
     *            the writer to write the title to.
     * @param title
     *            the title to print (null or empty titles will be ignored).
     * @throws IOException
     *             if the writer fails.
     */
    public static void title(Writer writer, String title) throws IOException {
        if (title == null || title.isEmpty()) {
            return;
        }
        String out = String.format("%s\n%s\n\n", title,
                Stream.generate(() -> "=").limit(title.length()).collect(Collectors.joining()));
        writer.write(out);
    }

    /**
     * Generate and print (System.out) the statistics report using the default (
     * {@link #BY_ADDED}) sort order.
     * 
     */
    public void report() {
        reportSO(null, null);
    }

    /**
     * Generate and print (System.out) the statistics report using the specified
     * sort order.
     * 
     * @param comparator
     *            the Comparator to sort the UStats by (see class constants for
     *            some useful suggestions)
     */
    public void report(final Comparator<UStats> comparator) {
        reportSO(null, comparator);
    }

    /**
     * Generate and print (System.out) the statistics report with the supplied
     * title, and using the default ({@link #BY_ADDED}) sort order.
     * 
     * @param title
     *            the title to use (e.g. "Warmup", "Cached Files", etc.)
     */
    public void report(final String title) {
        reportSO(title, null);
    }

    /**
     * Generate and print (System.out) the statistics report with the supplied
     * title, and using the specified sort order.
     * 
     * @param title
     *            the title to use (e.g. "Warmup", "Cached Files", etc.)
     * @param comparator
     *            the Comparator to sort the UStats by (see class constants for
     *            some useful suggestions)
     */
    public void report(final String title, final Comparator<UStats> comparator) {
        reportSO(title, comparator);
    }

    /**
     * Generate and print (System.out) the statistics report using the default (
     * {@link #BY_ADDED}) sort order.
     * 
     * @param writer
     *            the destination to report to
     * @throws IOException
     *             if the writer destination fails
     */
    public void report(final Writer writer) throws IOException {
        report(writer, null, null);
    }

    /**
     * Generate and print (System.out) the statistics report using the specified
     * sort order.
     * 
     * @param writer
     *            the destination to report to
     * @param comparator
     *            the Comparator to sort the UStats by (see class constants for
     *            some useful suggestions)
     * @throws IOException
     *             if the writer destination fails
     */
    public void report(final Writer writer, final Comparator<UStats> comparator) throws IOException {
        report(writer, null, comparator);
    }

    /**
     * Generate and print (System.out) the statistics report with the supplied
     * title, and using the default ({@link #BY_ADDED}) sort order.
     * 
     * @param writer
     *            the destination to report to
     * @param title
     *            the title to use (e.g. "Warmup", "Cached Files", etc.)
     * @throws IOException
     *             if the writer destination fails
     */
    public void report(final Writer writer, final String title) throws IOException {
        report(writer, title, null);
    }

    /**
     * Generate and print (System.out) the statistics report with the supplied
     * title, and using the specified sort order.
     * 
     * @param writer
     *            the destination to report to
     * @param title
     *            the title to use (e.g. "Warmup", "Cached Files", etc.)
     * @param comparator
     *            the Comparator to sort the UStats by (see class constants for
     *            some useful suggestions)
     * @throws IOException
     *             if the writer destination fails
     */
    public void report(final Writer writer, final String title, final Comparator<UStats> comparator) throws IOException {

        title(writer, title);

        long mintime = stats.stream().mapToLong(s -> s.getFastestNanos()).min().getAsLong();
        TimeUnit tUnit = UStats.findBestUnit(mintime);
        for (UStats s : getStats(comparator)) {
            writer.write(s.formatResults(tUnit));
            writer.write("\n");
        }
    }

    private void reportSO(final String title, final Comparator<UStats> comparator) {
        try (Writer w = new NonClosingSystemOut()) {
            report(w, title, comparator);
        } catch (IOException e) {
            throw new IllegalStateException("Should never be an exception writing to System.out", e);
        }
    }

}

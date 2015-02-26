package net.tuis.ubench;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Statistics representing the individual iterations for a given task.
 * <p>
 * Presents various statistics related to the run times that are useful for
 * interpreting the run performance.
 */
public final class UStats {

    /*
     * unit(Bounds|Factor|Order|Name) static members are a way of identifying
     * which time unit is most useful for displaying a time.
     * 
     * A useful unit is one which presents the time as something between 0.1 and
     * 99.999. e.g. it is better to have 2.35668 milliseconds than 2356.82334
     * microseconds, or 0.00235 seconds.
     */
    private static final long[] unitBounds = buildUnitBounds();
    private static final double[] unitFactor = buildUnitFactors();
    private static final TimeUnit[] unitOrder = buildUnitOrders();
    private static final String[] unitName = buildUnitNames();

    private static final double[] buildUnitFactors() {
        TimeUnit[] tus = TimeUnit.values();
        double[] ret = new double[tus.length];
        for (TimeUnit tu : tus) {
            ret[tu.ordinal()] = tu.toNanos(1);
        }
        return ret;
    }

    private static final TimeUnit[] buildUnitOrders() {
        TimeUnit[] tus = TimeUnit.values();
        Arrays.sort(tus, Comparator.comparingLong(u -> u.toNanos(1)));
        return tus;
    }

    private static final long[] buildUnitBounds() {
        TimeUnit[] tus = TimeUnit.values();
        long[] ret = new long[tus.length];
        for (TimeUnit tu : tus) {
            ret[tu.ordinal()] = tu.toNanos(1) / 10L;
        }
        return ret;
    }

    private static final String[] buildUnitNames() {
        TimeUnit[] tus = TimeUnit.values();
        String[] ret = new String[tus.length];
        for (TimeUnit tu : tus) {
            ret[tu.ordinal()] = tu.toString();
        }
        return ret;
    }

    /**
     * Identify a TimeUnit that is convenient for the display of the supplied
     * nanosecond value.
     * <p>
     * The best unit is the one which has no zeros after the decimal, and at
     * most two digits before.
     * <p>
     * The following are examples of "best" displays:
     * <ul>
     * <li>1.2345 seconds
     * <li>0.7247 microseconds
     * <li>82.443 milliseconds
     * </ul>
     * 
     * in contrast, the following would not be suggested as "best" units:
     * 
     * <ul>
     * <li>623.2345 milliseconds
     * <li>0.0000007247 seconds
     * <li>825543.000 nanoseconds
     * </ul>
     * 
     * It is suggested that you should find the best unit for the shortest time
     * value you will have in your data, and then use that same unit to display
     * all times.
     * <p>
     * For example, if you have the following nanosecond times
     * <code>[5432, 8954228, 665390, 492009]</code> you should find the unit for
     * the shortest (5432) which will be TimeUnit.MICROSECONDS, and end up with
     * the display of:
     * 
     * <pre>
     *     5.432
     *  8954.228
     *   665.390
     *   492.009
     * </pre>
     * 
     * </ol>
     * 
     * @param time
     *            the time to display (in nanoseconds)
     * @return A Time Unit that will display the nanosecond time well.
     */
    public static TimeUnit findBestUnit(long time) {
        for (int i = 1; i < unitOrder.length; i++) {
            if (unitBounds[unitOrder[i].ordinal()] > time) {
                return unitOrder[i - 1];
            }
        }
        return unitOrder[unitOrder.length - 1];
    }

    private static final String formatHisto(int[] histogramByXFactor) {
        return IntStream.of(histogramByXFactor).mapToObj(i -> String.format("%5d", i)).collect(Collectors.joining(" "));
    }

    private static final String formatZoneTime(double[] zoneTimes) {
        return DoubleStream.of(zoneTimes).mapToObj(d -> String.format("%.3f", d)).collect(Collectors.joining(" "));
    }

    private static final int logTwo(long numerator, long denominator) {
        long dividend = numerator / denominator;
        long tip = Long.highestOneBit(dividend);
        return Long.numberOfTrailingZeros(tip);
    }

    private final long[] results;
    private final long fastest;
    private final long slowest;
    private final long average;
    private final String suite;
    private final String name;
    private final int[] histogram;
    private final long p95ile;
    private final long p99ile;
    private final TimeUnit unit;
    private final int index;

    /**
     * Package Private: Construct statistics based on the nanosecond times of
     * multiple runs.
     * <p>
     * Compute all derived statistics on the assumption that toString will be
     * called, and one comprehensive scan will have less effect than multiple
     * partial results.
     * 
     * @param name
     *            The name of the task that has been benchmarked
     * @param results
     *            The nano-second run times of each successful run.
     */
    UStats(String suit, String name, int index, long[] results) {
        this.suite = suit;
        this.name = name;
        this.results = results;
        this.index = index;

        // tmp is only used to compute percentile results.
        long[] tmp = Arrays.copyOf(results, results.length);
        Arrays.sort(tmp);

        fastest = tmp[0];
        slowest = tmp[tmp.length - 1];
        int at95 = (int) (tmp.length * (95.0 / 100.0)) - 1;
        int at99 = (int) (tmp.length * (99.0 / 100.0)) - 1;

        p95ile = tmp[Math.min(at95, tmp.length - 1)];
        p99ile = tmp[Math.min(at99, tmp.length - 1)];

        long sum = LongStream.of(results).sum();
        average = sum / tmp.length;
        histogram = new int[logTwo(slowest, fastest) + 1];
        for (long t : tmp) {
            histogram[logTwo(t, fastest)]++;
        }

        unit = findBestUnit(fastest);
    }

    /**
     * The nanosecond time of the 95<sup>th</sup> percentile run.
     * 
     * @return the nanosecond time of the 95<sup>th</sup> percentile run.
     */
    public long get95thPercentileNanos() {
        return p95ile;
    }

    /**
     * The nanosecond time of the 99<sup>th</sup> percentile run.
     * 
     * @return the nanosecond time of the 99<sup>th</sup> percentile run.
     */
    public long get99thPercentileNanos() {
        return p99ile;
    }

    /**
     * The nanosecond time of the fastest run.
     * 
     * @return the nanosecond time of the fastest run.
     */
    public long getFastestRawNanos() {
        return fastest;
    }

    /**
     * The nanosecond time of the slowest run.
     * 
     * @return the nanosecond time of the slowest run.
     */
    public long getSlowestRawNanos() {
        return slowest;
    }

    /**
     * The nanosecond time of the average run.
     * <p>
     * Note, this is in nanoseconds (using integer division of the total time /
     * count). Any sub-nano-second error is considered irrelevant
     * 
     * @return the nanosecond time of the average run.
     */
    public long getAverageRawNanos() {
        return average;
    }

    /**
     * Package Private: Used to identify the order in which the task was added to the UBench instance.
     * @return the index in UBench.
     */
    int getIndex() {
        return index;
    }

    /**
     * Identify what a good time Unit would be to present the results in these statistics.
     * <p>
     * Calculated as the equivalent of <code>findBestUnit(getFastestRawNanos())</code>
     * @return A time unit useful for scaling these statistical results.
     * @See {@link UStats#findBestUnit(long)}
     */
    public TimeUnit getGoodUnit() {
        return unit;
    }

    /**
     * Get the raw data the statistics are based off.
     * 
     * @return (a copy of) the individual test run times (in nanoseconds, and in order of
     *         execution).
     */
    public long[] getRawData() {
        return Arrays.copyOf(results, results.length);
    }

    /**
     * Summarize the time-progression of the run time for each iteration, in
     * order of execution (in milliseconds).
     * <p>
     * An example helps. If there are 200 results, and a request for 10 zones,
     * then return 10 double values representing the average time of the first
     * 20 runs, then the next 20, and so on, until the 10th zone contains the
     * average time of the last 20 runs.
     * <p>
     * This is a good way to see the effects of warm-up times and different
     * compile levels
     * 
     * @param zoneCount
     *            the number of zones to compute
     * @return an array of times (in the given unit) representing the average
     *         time for all runs in the respective zone.
     */
    public final double[] getZoneTimes(int zoneCount, TimeUnit timeUnit) {
        double[] ret = new double[Math.min(zoneCount, results.length)];
        int perblock = results.length / ret.length;
        int overflow = results.length % ret.length;
        int pos = 0;
        double repFactor = unitFactor[timeUnit.ordinal()];
        for (int block = 0; block < ret.length; block++) {
            int count = perblock + (block < overflow ? 1 : 0);
            int limit = pos + count;
            long nanos = 0;
            while (pos < limit) {
                nanos += results[pos];
                pos++;
            }
            ret[block] = (nanos / repFactor) / count;
        }
        return ret;
    }

    /**
     * Compute a log-2-based histogram relative to the fastest run in the data
     * set.
     * <p>
     * This gives a sense of what the general shape of the runs are in terms of
     * distribution of run times. The histogram is based on the fastest run.
     * <p>
     * By way of an example, the output: <code>100, 50, 10, 1, 0, 1</code> would
     * suggest that:
     * <ul>
     * <li>100 runs were between 1 times and 2 times as slow as the fastest.
     * <li>50 runs were between 2 and 4 times slower than the fastest.
     * <li>10 runs were between 4 and 8 times slower
     * <li>1 run was between 8 and 16 times slower
     * <li>1 run was between 32 and 64 times slower
     * 
     * @return
     */
    public final int[] getDoublingHistogram() {
        return Arrays.copyOf(histogram, histogram.length);
    }

    /**
     * The 99<sup>th</sup> percentile of runtimes.
     * <p>
     * 99% of all runs completed in this time, or faster.
     * 
     * @return the time of the 99<sup>th</sup> percentile in the given time unit.
     */
    public final double get99thPercentile(TimeUnit timeUnit) {
        return p99ile / unitFactor[timeUnit.ordinal()];
    }

    /**
     * The 95<sup>th</sup> percentile of runtimes.
     * <p>
     * 95% of all runs completed in this time, or faster.
     * 
     * @return the time of the 95<sup>th</sup> percentile in the given time unit.
     */
    public final double get95thPercentile(TimeUnit timeUnit) {
        return p95ile / unitFactor[timeUnit.ordinal()];
    }

    /**
     * Compute the average time of all runs (in milliseconds).
     * 
     * @return the average time (in milliseconds)
     */
    public final double getAverage(TimeUnit timeUnit) {
        return average / unitFactor[timeUnit.ordinal()];
    }

    /**
     * Compute the slowest run (in milliseconds).
     * 
     * @return The slowest run time (in milliseconds).
     */
    public final double getSlowest(TimeUnit timeUnit) {
        return slowest / unitFactor[timeUnit.ordinal()];
    }

    /**
     * Compute the fastest run (in milliseconds).
     * 
     * @return The fastest run time (in milliseconds).
     */
    public final double getFastest(TimeUnit timeUnit) {
        return fastest / unitFactor[timeUnit.ordinal()];
    }

    @Override
    public String toString() {
        return formatResults(unit);
    }

    /**
     * Present the results from this task in a formatted string output.
     * @param tUnit the units in which to display the times (see {@link UStats#getGoodUnit() } for a suggestion).
     * @return A string representing the statistics.
     * @see UStats#getGoodUnit()
     */
    public String formatResults(TimeUnit tUnit) {
        double avg = getAverage(tUnit);
        double fast = getFastest(tUnit);
        double slow = getSlowest(tUnit);
        double t95p = get95thPercentile(tUnit);
        double t99p = get99thPercentile(tUnit);
        int width = Math.max(8, DoubleStream.of(avg, fast, slow, t95p, t99p).mapToObj(d -> String.format("%.4f", d))
                .mapToInt(String::length).max().getAsInt());
        
        return String.format("Task %s -> %s: (Unit: %s)\n" 
                + "  Count    : %" + width + "d      Average  : %" + width + ".4f\n" 
                + "  Fastest  : %" + width + ".4f      Slowest  : %" + width + ".4f\n"
                + "  95Pctile : %" + width + ".4f      99Pctile : %" + width + ".4f\n" 
                + "  TimeBlock : %s\n" 
                + "  Histogram : %s\n",
                suite, name, unitName[tUnit.ordinal()],
                results.length, avg,
                fast, slow,
                t95p, t99p, 
                formatZoneTime(getZoneTimes(10, tUnit)),
                formatHisto(getDoublingHistogram()));
    }

    /**
     * The name of the UBench Suite this task was run in.
     * @return the suite name.
     */
    public String getSuiteName() {
        return suite;
    }

    /**
     * The name of the UBench task these statistics are from.
     * @return the task name.
     */
    public String getName() {
        return name;
    }

}
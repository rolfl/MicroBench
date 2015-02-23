package net.tuis.ubench;

import java.util.Arrays;
import java.util.LongSummaryStatistics;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Statistics representing the runs in this task.
 * <p>
 * Presents various statistics related to the run times that are useful for
 * interpreting the run performance.
 * 
 * @author rolf
 *
 */
public final class TaskStats {

    private static final double NANOxMILLI = 1000000.0;

    private final long[] results;
    private final long min;
    private final long max;
    private final double average;
    private final String name;

    /**
     * Construct statistics based on the nanosecond times of multiple runs.
     * 
     * @param name
     *            The name of the task that has been benchmarked
     * @param results
     *            The nano-second run times of each successful run.
     */
    public TaskStats(String name, long[] results) {
        this.name = name;
        this.results = results;
        LongSummaryStatistics lss = LongStream.of(results).summaryStatistics();
        min = lss.getMin();
        max = lss.getMax();
        average = lss.getAverage();
    }
    
    /**
     * Get the raw data the statistics are based off.
     * @return the individual test run times (in nanoseconds, and in order of execution).
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
     * @return
     */
    public final double[] getZoneTimesMilli(int zoneCount) {
        double[] ret = new double[Math.min(zoneCount, results.length)];
        int perblock = results.length / ret.length;
        int overflow = results.length % ret.length;
        int pos = 0;
        for (int block = 0; block < ret.length; block++) {
            int count = perblock + (block < overflow ? 1 : 0);
            int limit = pos + count;
            long nanos = 0;
            while (pos < limit) {
                nanos += results[pos];
                pos++;
            }
            ret[block] = (nanos / NANOxMILLI) / count;
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
    public final int[] getHistogramByDoublingFactor() {
        int count = (int) (max / min);
        int[] histo = new int[Integer.numberOfTrailingZeros(Integer.highestOneBit(count)) + 1];
        LongStream.of(results).mapToInt(t -> Integer.numberOfTrailingZeros(Integer.highestOneBit((int) (t / min))))
                .forEach(i -> histo[i]++);
        return histo;
    }

    /**
     * Compute the 95<sup>th</sup> percentile of runtimes (in milliseconds).
     * <p>
     * 95% of all runs completed in this time, or faster.
     * 
     * @return the millisecond time of the 95<sup>th</sup> percentile.
     */
    public final double get95thPercentile() {
        if (results.length < 100) {
            return getSlowest();
        }
        long limit = ((results.length + 1) * 95) / 100;
        return LongStream.of(results).sorted().limit(limit).max().getAsLong() / NANOxMILLI;
    }

    /**
     * Compute the average time of all runs (in milliseconds).
     * 
     * @return the average time (in milliseconds)
     */
    public final double getAverage() {
        return average / NANOxMILLI;
    }

    /**
     * Compute the slowest run (in milliseconds).
     * 
     * @return The slowest run time (in milliseconds).
     */
    public final double getSlowest() {
        return max / NANOxMILLI;
    }

    /**
     * Compute the fastest run (in milliseconds).
     * 
     * @return The fastest run time (in milliseconds).
     */
    public final double getFastest() {
        return min / NANOxMILLI;
    }

    @Override
    public String toString() {
        return String.format("Task %s:\n" +
                             "  Iterations  : %12d\n" + 
                             "  Fastest     : %12.5fms\n" + 
                             "  Average     : %12.5fms\n" + 
                             "  95Pctile    : %12.5fms\n" + 
                             "  Slowest     : %12.5fms\n" + 
                             "  TimeBlock   : %s\n" + 
                             "  FactorHisto : %s\n", 
                name, results.length, getFastest(), getAverage(),
                get95thPercentile(), getSlowest(), formatMillis(getZoneTimesMilli(10)),
                formatHisto(getHistogramByDoublingFactor()));
    }

    private String formatHisto(int[] histogramByXFactor) {
        return IntStream.of(histogramByXFactor).mapToObj(i -> String.format("%5d", i)).collect(Collectors.joining(" "));
    }

    private String formatMillis(double[] zoneTimesMilli) {
        return DoubleStream.of(zoneTimesMilli).mapToObj(d -> String.format("%.5fms", d))
                .collect(Collectors.joining(" "));
    }

}
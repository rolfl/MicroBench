package net.tuis.ubench;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class TestTaskStats {
    
    @Test
    public void testEmptyResults() {
        UStats stats = new UStats("test", "test", 1, new long[0]);
        assertEquals(0, stats.getFastestNanos());
        assertEquals(0, stats.getSlowestNanos());
        assertArrayEquals(new int[0], stats.getDoublingHistogram());
        assertEquals(0, stats.get95thPercentileNanos());
        assertEquals(0, stats.get99thPercentileNanos());
        assertEquals(0, stats.getAverageRawNanos());
        assertArrayEquals(new double[0], stats.getZoneTimes(10, TimeUnit.MICROSECONDS), 0.0);
    }

    @Test
    public void testSingleResults() {
        UStats stats = new UStats("test", "test", 1, new long[]{100});
        assertEquals(100, stats.getFastestNanos());
        assertEquals(100, stats.getSlowestNanos());
        assertArrayEquals(new int[]{1}, stats.getDoublingHistogram());
        assertEquals(100, stats.get95thPercentileNanos());
        assertEquals(100, stats.get99thPercentileNanos());
        assertEquals(100, stats.getAverageRawNanos());
        assertArrayEquals(new double[]{100}, stats.getZoneTimes(10, TimeUnit.NANOSECONDS), 0.0);
    }

    @Test
    public void testGetZoneTimesMilliSimple() {
        long[] times = { 1, 2, 3, 4, 5 };
        UStats stats = new UStats("test", "test", 1, times);
        double[] zones = LongStream.of(times).mapToDouble(t -> t / 1000000.0).toArray();
        assertArrayEquals(zones, stats.getZoneTimes(times.length, TimeUnit.MILLISECONDS), 0.0);
    }

    @Test
    public void testGetZoneTimesMilliUnEven() {
        long[] times = { 100, 100, 200, 200, 300, 400, 500 };
        long[] expect = { 100, 200, 300, 400, 500 };
        UStats stats = new UStats("test", "test", 1, times);
        double[] zones = LongStream.of(expect).mapToDouble(t -> t / 1000000.0).toArray();
        assertArrayEquals(zones, stats.getZoneTimes(5, TimeUnit.MILLISECONDS), 0.0);
    }

    @Test
    public void testGetHistogramByDoublingFactor() {
        long[] times = new long[1000];
        for (int i = 0; i < times.length; i++) {
            times[i] = (i + 1) * 100;
        }
        int[] expect = { 1, 2, 4, 8, 16, 32, 64, 128, 256, 489 };
        UStats stats = new UStats("test", "test", 1, times);
        assertArrayEquals(expect, stats.getDoublingHistogram());
    }

    @Test
    public void testGet95thPercentileSmall() {
        long[] times = { 100, 100, 200, 200, 300, 400, 500 };
        UStats stats = new UStats("test", "test", 1, times);
        assertEquals(0.0005, stats.get95thPercentile(TimeUnit.MILLISECONDS), 0.0);
    }

    @Test
    public void testGet95thPercentileLarge() {
        long[] times = new long[1000];
        for (int i = 0; i < times.length; i++) {
            times[i] = (i + 1) * 100;
        }
        double expect = times[950] / 1000000.0;
        UStats stats = new UStats("test", "test", 1, times);
        assertEquals(expect, stats.get95thPercentile(TimeUnit.MILLISECONDS), 0.0);
    }

    @Test
    public void testGet99thPercentileSmall() {
        long[] times = { 100, 100, 200, 200, 300, 400, 500 };
        UStats stats = new UStats("test", "test", 1, times);
        assertEquals(0.0005, stats.get99thPercentile(TimeUnit.MILLISECONDS), 0.0);
    }

    @Test
    public void testGet99thPercentileLarge() {
        long[] times = new long[1000];
        for (int i = 0; i < times.length; i++) {
            times[i] = (i + 1) * 100;
        }
        double expect = times[990] / 1000000.0;
        UStats stats = new UStats("test", "test", 1, times);
        assertEquals(expect, stats.get99thPercentile(TimeUnit.MILLISECONDS), 0.0);
    }

    @Test
    public void testGetAverage() {
        long[] times = new long[1000];
        long sum = 0;
        for (int i = 0; i < times.length; i++) {
            times[i] = (i + 1) * 100;
            sum += times[i];
        }
        double expect = (sum / 1000000.0) / times.length;
        UStats stats = new UStats("test", "test", 1, times);
        assertEquals(expect, stats.getAverage(TimeUnit.MILLISECONDS), 0.0);
    }

    @Test
    public void testGetSlowest() {
        long[] times = { 100, 100, 200, 200, 300, 400, 500 };
        UStats stats = new UStats("test", "test", 1, times);
        assertEquals(0.0005, stats.getSlowest(TimeUnit.MILLISECONDS), 0.0);
    }

    @Test
    public void testGetFastest() {
        long[] times = { 100, 100, 200, 200, 300, 400, 500 };
        UStats stats = new UStats("test", "test", 1, times);
        assertEquals(0.0001, stats.getFastest(TimeUnit.MILLISECONDS), 0.0);
    }

}

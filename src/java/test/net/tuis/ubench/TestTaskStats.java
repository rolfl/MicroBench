package net.tuis.ubench;

import static org.junit.Assert.*;

import java.util.stream.LongStream;

import org.junit.Test;

public class TestTaskStats {

    @Test
    public void testGetZoneTimesMilliSimple() {
        long[] times = { 1, 2, 3, 4, 5 };
        TaskStats stats = new TaskStats("test", times);
        double[] zones = LongStream.of(times).mapToDouble(t -> t / 1000000.0).toArray();
        assertArrayEquals(zones, stats.getZoneTimesMilli(times.length), 0.0);
    }

    @Test
    public void testGetZoneTimesMilliUnEven() {
        long[] times = { 100, 100, 200, 200, 300, 400, 500 };
        long[] expect = {100, 200, 300, 400, 500};
        TaskStats stats = new TaskStats("test", times);
        double[] zones = LongStream.of(expect).mapToDouble(t -> t / 1000000.0).toArray();
        assertArrayEquals(zones, stats.getZoneTimesMilli(5), 0.0);
    }

    @Test
    public void testGetHistogramByDoublingFactor() {
        long[] times = new long[1000];
        for (int i = 0; i < times.length; i++) {
            times[i] = (i + 1) * 100;
        }
        int[] expect = {1, 2, 4, 8, 16, 32, 64, 128, 256, 489};
        TaskStats stats = new TaskStats("test", times);
        assertArrayEquals(expect, stats.getHistogramByDoublingFactor());
    }

    @Test
    public void testGet95thPercentileSmall() {
        long[] times = { 100, 100, 200, 200, 300, 400, 500 };
        TaskStats stats = new TaskStats("test", times);
        assertEquals(0.0005, stats.get95thPercentile(), 0.0);
    }

    @Test
    public void testGet95thPercentileLarge() {
        long[] times = new long[1000];
        for (int i = 0; i < times.length; i++) {
            times[i] = (i + 1) * 100;
        }
        double expect = times[950 - 1] / 1000000.0;
        TaskStats stats = new TaskStats("test", times);
        assertEquals(expect, stats.get95thPercentile(), 0.0);
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
        TaskStats stats = new TaskStats("test", times);
        assertEquals(expect, stats.getAverage(), 0.0);
    }

    @Test
    public void testGetSlowest() {
        long[] times = { 100, 100, 200, 200, 300, 400, 500 };
        TaskStats stats = new TaskStats("test", times);
        assertEquals(0.0005, stats.getSlowest(), 0.0);
    }

    @Test
    public void testGetFastest() {
        long[] times = { 100, 100, 200, 200, 300, 400, 500 };
        TaskStats stats = new TaskStats("test", times);
        assertEquals(0.0001, stats.getFastest(), 0.0);
    }

}

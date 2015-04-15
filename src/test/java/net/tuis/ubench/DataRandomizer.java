package net.tuis.ubench;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * @author rolfl
 * @author Simon Forsberg
 */
@SuppressWarnings("javadoc")
public class DataRandomizer {

    private static final ConcurrentMap<Integer, AtomicInteger> arrayCounts = new ConcurrentHashMap<>();

    public static final int[] randomData(int size) {
        arrayCounts.computeIfAbsent(size, key -> new AtomicInteger(0)).incrementAndGet();
        // System.out.println("Randomizing " + size);
        Random rand = new Random(size);
        return IntStream.generate(rand::nextInt).limit(size).toArray();
    }

}

package net.tuis.ubench;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@SuppressWarnings("javadoc")
public class ExampleScales {

    private static final ConcurrentMap<Integer, AtomicInteger> arrayCounts = new ConcurrentHashMap<>();

    private static final int[] randomData(int size) {
        arrayCounts.computeIfAbsent(size, key -> new AtomicInteger(0)).incrementAndGet();
        // System.out.println("Randomizing " + size);
        Random rand = new Random(size);
        return IntStream.generate(rand::nextInt).limit(size).toArray();
    }

    public static void main(String[] args) throws IOException {
        UScale.function(div -> div / 3, scale -> scale).report();
        UScale scales = UScale.consumer(Arrays::sort, scale -> randomData(scale), false);
        
        scales.report();
        scales.reportHTML("Arrays::sort", Paths.get("output/ArraysSort.html"));
        System.out.println(scales.json());
        

        arrayCounts.keySet().stream().sorted()
                .map(scale -> String.format("Scale %d -> created %d", scale, arrayCounts.get(scale).get()))
                .forEach(System.out::println);

        UScale.consumer(Arrays::sort, scale -> randomData(scale), true).report();

        arrayCounts.keySet().stream().sorted()
                .map(scale -> String.format("Scale %d -> created %d", scale, arrayCounts.get(scale).get()))
                .forEach(System.out::println);
        
    }


}

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
    
    private static final long linear(long input) {
        long count = 0;
        while (input > 10) {
            input -= 10;
            count++;
        }
        return count;
    }

    public static void main(String[] args) throws IOException {
        //UScale.function(div -> div / 3, scale -> scale).report();

        UScale.function(
                data -> linear(data),
                scale -> scale, true)
           .reportHTML("Linear", Paths.get("output/Linear.html"));

        UScale.consumer(data -> bubbleSort(data), scale -> randomData(scale), false)
            .report();
        
        if (!Boolean.getBoolean("DOALL")) {
            return;
        }
        
        UScale scales = UScale.consumer(Arrays::sort, scale -> randomData(scale), false);
        
        scales.report();
        scales.reportHTML("Arrays::sort", Paths.get("output/ArraysSort.html"));
        System.out.println(scales.toJSONString("Arrays::Sort"));
        

        arrayCounts.keySet().stream().sorted()
                .map(scale -> String.format("Scale %d -> created %d", scale, arrayCounts.get(scale).get()))
                .forEach(System.out::println);

        UScale.consumer(Arrays::sort, scale -> randomData(scale), true).report();

        arrayCounts.keySet().stream().sorted()
                .map(scale -> String.format("Scale %d -> created %d", scale, arrayCounts.get(scale).get()))
                .forEach(System.out::println);
        
    }

    private static void bubbleSort(int[] data) {
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data.length - 1; j++) {
                if (i != j) {
                    int a = data[j];
                    int b = data[j + 1];
                    if (a > b) {
                        int temp = data[j];
                        data[j] = data[j + 1];
                        data[j + 1] = temp;
                    }
                }
            }
        }
    }


}

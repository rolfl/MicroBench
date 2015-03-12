package net.tuis.ubench;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;


@SuppressWarnings("javadoc")
public class RandomSorter {
    
    public static void main(String[] args) {
        Random random = new Random();
        
        final int[] data = IntStream.generate(random::nextInt).limit(10000).toArray();
        final int[] sorted = Arrays.stream(data).sorted().toArray();
        
        Predicate<int[]> validate = v -> Arrays.equals(v, sorted);
        
        Supplier<int[]> stream = () -> Arrays.stream(data).sorted().toArray();
        
        Supplier<int[]> trad = () -> {
            int[] copy = Arrays.copyOf(data, data.length);
            Arrays.sort(copy);
            return copy;
        };
        
        UBench bench = new UBench("Sort Algorithms");
        bench.addTask("Functional", stream, validate);
        bench.addTask("Traditional", trad, validate);
        UBench.report("With Warmup", bench.press(10000));
        
    }

}

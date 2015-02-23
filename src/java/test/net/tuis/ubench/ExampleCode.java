package net.tuis.ubench;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

public class ExampleCode {
    
    private static final ToIntFunction<String> charcount = (line) -> (int) IntStream.range(0, line.length())
            .map(i -> (int) line.charAt(i)).distinct().count();

    private static final int countDistinctChars(String line) {
        if (line.length() <= 1) {
            return line.length();
        }
        char[] chars = line.toCharArray();
        Arrays.sort(chars);
        int count = 1;
        for (int i = 1; i < chars.length; i++) {
            if (chars[i] != chars[i - 1]) {
                count++;
            }
        }
        return count;

    }

    public static void main(String[] args) {
        final String testdata = "abcdefghijklmnopqrstuvwxyz ABCDEFGHIJKLMNOPQRSTUVWXYZ 0123456789";
        final String hello = "Hello World!";
        UBench bench = new UBench("distinct chars");

        bench.addTask(Task.buildCheckedIntTask("Functional alphas", () -> charcount.applyAsInt(testdata), 63));
        bench.addTask(Task.buildCheckedIntTask("Functional hello", () -> charcount.applyAsInt(hello), 9));

        bench.addTask(Task.buildCheckedIntTask("Traditional alphas", () -> countDistinctChars(testdata), 63));
        bench.addTask(Task.buildCheckedIntTask("Traditional hello", () -> countDistinctChars(hello), 9));

        List<TaskStats> times = bench.benchMark(100000, 1000, 10.0, 500, TimeUnit.MILLISECONDS);

        for (TaskStats stats : times) {
            System.out.println(stats);
        }

    }

}

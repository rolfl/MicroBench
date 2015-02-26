package net.tuis.ubench;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

/**
 * Simple example test of UBench
 * 
 * @author rolf
 *
 */
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

    /**
     * Test entry point.
     * 
     * @param args
     *            ignored.
     */
    public static void main(String[] args) {
        final String testdata = "abcdefghijklmnopqrstuvwxyz ABCDEFGHIJKLMNOPQRSTUVWXYZ 0123456789";
        final String hello = "Hello World!";

        UBench bench = new UBench("distinct chars");

        bench.addIntTask("Functional alphas", () -> charcount.applyAsInt(testdata), g -> g == 63);
        bench.addIntTask("Functional hello", () -> charcount.applyAsInt(hello), g -> g == 9);

        bench.addIntTask("Traditional alphas", () -> countDistinctChars(testdata), g -> g == 63);
        bench.addIntTask("Traditional hello", () -> countDistinctChars(hello), g -> g == 9);

        bench.report("Warmup", bench.press(100000, 1000, 10.0, 500, TimeUnit.MILLISECONDS));
        bench.report("Sequential", bench.press(UMode.SEQUENTIAL, 100000, 1000, 10.0, 500, TimeUnit.MILLISECONDS));
        bench.report("Parallel", bench.press(UMode.PARALLEL, 100000, 1000, 10.0, 500, TimeUnit.MILLISECONDS));
        bench.report("Interleaved", bench.press(UMode.INTERLEAVED, 100000, 1000, 10.0, 500, TimeUnit.MILLISECONDS));

    }

}

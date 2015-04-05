package net.tuis.ubench;

import net.tuis.ubench.scale.MathEquation;
import net.tuis.ubench.scale.Models;

import org.junit.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Level;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Simon Forsberg
 */
public class ScaleTest {
    
    static {
        UUtils.setStandaloneLogging(Level.FINE);
    }
    
    @Test
    public void bubbleSort() {
        MathEquation eq = UScale.consumer("BubbleSort", arr -> bubbleSort(arr), i -> DataRandomizer.randomData(i), false).determineBestFit();
        assertTrue(eq.isValid());
        assertEquals(Models.N_SQUARED, eq.getModel());
    }

    @Test
    public void integerDivide() {
        UScale scale = UScale.function("Int Divide", i -> i / 3, i -> i, false);
        MathEquation[] eqs = scale.fitEquations();
        MathEquation eq = scale.determineBestFit();
        System.out.println(Arrays.toString(eqs));
        System.out.println(eq);
        double[] fastest = scale.getStats().stream().mapToDouble(s -> s.getFastestNanos()).toArray();
        double[] avg = scale.getStats().stream().mapToDouble(s -> s.getAverageRawNanos()).toArray();
        int[] scales = scale.getStats().stream().mapToInt(s -> s.getIndex()).toArray();
        System.out.println(Arrays.toString(fastest));
        System.out.println(Arrays.toString(avg));
        System.out.println(Arrays.toString(scales));
        scale.report();
        assertEquals(Models.CONSTANT, eq.getModel());
    }

    @Test
    public void linear() {
        MathEquation eq = UScale.function("Linear", data -> linear(data), scale -> scale, true)
                .determineBestFit();
        assertEquals(Models.LINEAR, eq.getModel());
    }

    @Test
    public void arraySort() {
        UScale scales = UScale.consumer("Array Sort", Arrays::sort, scale -> DataRandomizer.randomData(scale), false);
        MathEquation eq = scales.determineBestFit();
        assertEquals(Models.N_LOG_N, eq.getModel());
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

    private static final long linear(long input) {
        long count = 0;
        while (input > 10) {
            input -= 10;
            count++;
        }
        return count;
    }

}

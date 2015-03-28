package net.tuis.ubench;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;

/**
 * Factory class and reporting instances that allow functions to be tested for scalability.
 *  
 * @author rolf
 * @author zomis
 *
 */
public class UScale {

    private static final class ScaleResult {
        final int scale;
        final UStats stats;
        
        ScaleResult(int scale, UStats stats) {
            super();
            this.scale = scale;
            this.stats = stats;
        }

        public int getScale() {
            return scale;
        }

        public UStats getStats() {
            return stats;
        }
        
    }
    
    private final List<ScaleResult> stats;

    private UScale(List<ScaleResult> stats) {
        this.stats = stats;
    }

    /**
     * Generate and print (System.out) the scalability report.
     */
    public void report() {
        stats.stream()
            .sorted(Comparator.comparingInt(ScaleResult::getScale))
            .map(sr -> String.format("Scale %4d -> %8d (count %d)\n", sr.getScale(), sr.getStats().getAverageRawNanos(), sr.getStats().getCount()))
            .forEach(System.out::println);
    }
   
    /**
     * Test the scalability of a function that requires T input data.
     * @param <T> the type of the input data
     * @param function the function that processes the T data
     * @param scaler a supplier that can supply T data of different sizes
     * @param reusedata if true, data of each size will be created just once, and reused often.
     * @return A UScale instance containing the results of the testing
     */
    public static <T> UScale scale(Consumer<T> function, IntFunction<T> scaler, final boolean reusedata) {
        
        final ScaleControl<T> scontrol = new ScaleControl<>(function, scaler, reusedata);
        
        UMode.PARALLEL.getModel().executeTasks("Warmup", scontrol.buildTask("warmup", 2));
        
        List<ScaleResult> results = new ArrayList<>(20);

        for (int i = 1; i <= 600000; i *= 2) {
            
            UStats wstats = UMode.SEQUENTIAL.getModel().executeTasks("Scale " + i, scontrol.buildTask("Scale " + i, i))[0];
            results.add(new ScaleResult(i, wstats));
            
            if (wstats.getCount() <= 3) {
                break;
            }
            //return new UScale(results);
        }
        return new UScale(results);
    }
    
    /**
     * Test the scalability of a function that requires an input integer.

     * @param function the function that processes the input
     * @param scaler a supplier that can supply data of different sizes in proportion to the supply value
     * @return A UScale instance containing the results of the testing
     */
    
    public static UScale scale(IntFunction<?> function, IntUnaryOperator scaler) {
        
        UMode.PARALLEL.getModel().executeTasks("Warmup", buildIntTask("warmup", function, scaler.applyAsInt(2)));
        
        List<ScaleResult> results = new ArrayList<>(20);
        
        for (int i = 1; i <= 600000; i *= 2) {
            
            TaskRunner runner = buildIntTask("Scale " + i, function, scaler.applyAsInt(i));
            
            UStats wstats = UMode.SEQUENTIAL.getModel().executeTasks("Scale " + i, runner)[0];
            results.add(new ScaleResult(i, wstats));
            if (wstats.getCount() <= 3) {
                break;
            }
        }
        
        return new UScale(results);
    }
    

    private static TaskRunner buildIntTask(final String name, final IntFunction<?> function, final int scale) {
        Task task = () -> {

                long start = System.nanoTime();
                function.apply(scale);
                long time = System.nanoTime() - start;
                return time;
                
        };
        return new TaskRunner(name, task, 0, 100000, 0, 0.0, TimeUnit.SECONDS.toNanos(1));
    }

}

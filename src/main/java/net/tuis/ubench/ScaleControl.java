package net.tuis.ubench;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

class ScaleControl<T> {
    
    private final Map<Integer, Supplier<T>> sources = new HashMap<>(32);
    private final boolean reusedata;
    private final IntFunction<T> scaler;
    private final Consumer<T> function;
    
    public ScaleControl(Consumer<T> function, IntFunction<T> scaler, boolean reusedata) {
        this.function = function;
        this.reusedata = reusedata;
        this.scaler = scaler;
    }


    private Supplier<T> getStaticData(final T data) {
        return () -> data;
    }
    

    private Supplier<T> dataSupply(final int scale) {
        return sources.computeIfAbsent(scale, key -> reusedata ? getStaticData(scaler.apply(scale)) : () -> scaler.apply(scale));
    }
    
    public TaskRunner buildTask(final String name, final int scale) {
        Task task = () -> {
            
            T data = dataSupply(scale).get();
            long start = System.nanoTime();
            function.accept(data);
            long time = System.nanoTime() - start;
            return time;
            
        };
        return new TaskRunner(name, task, scale, 1000000, 0, 0, TimeUnit.SECONDS.toNanos(1));
    }


}

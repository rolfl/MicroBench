package net.tuis.ubench;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Execution model that establishes a thread for each task, runs them all
 * concurrenly, but still fails (relatively) fast if any one task fails
 * 
 * @author rolf
 *
 */
final class ParallelExecutionModel implements TaskExecutionModel, ThreadFactory {

    private static final AtomicInteger threadId = new AtomicInteger();

    private static final class Combiner implements Callable<Combiner> {
        private final int index;
        private final TaskRunner runner;
        private final AtomicBoolean terminated;

        public Combiner(int index, TaskRunner runner, AtomicBoolean terminated) {
            super();
            this.index = index;
            this.runner = runner;
            this.terminated = terminated;
        }

        @Override
        public Combiner call() throws Exception {
            boolean done = false;
            int loops = 0;
            do {
                loops++;
                if (loops % 1000 == 0) {
                    // check each 1000 loops.
                    if (terminated.get()) {
                        return this;
                    }
                }
                done = runner.invoke();
            } while (!done);
            return this;
        }

    }

    @Override
    public final Thread newThread(Runnable r) {
        Thread t = new Thread(r, "Parallel Model " + threadId.incrementAndGet());
        t.setDaemon(true);
        return t;
    }

    @Override
    public UStats[] executeTasks(String suite, TaskRunner[] tasks) {

        UStats[] results = new UStats[tasks.length];
        ExecutorService service = Executors.newFixedThreadPool(tasks.length, this);
        CompletionService<Combiner> completion = new ExecutorCompletionService<>(service);

        final AtomicBoolean terminator = new AtomicBoolean(false);
        try {
            for (int i = 0; i < tasks.length; i++) {
                completion.submit(new Combiner(i, tasks[i], terminator));
            }
            for (int i = 0; i < tasks.length; i++) {
                Future<Combiner> fc = completion.take();
                Combiner c = fc.get();
                results[c.index] = c.runner.collect(suite);
            }
            return results;
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Parallel Execution interrupted. See cause.", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException("Parallel Execution failed. See cause.", cause);
        } finally {
            terminator.set(true);
            service.shutdown();
            if (!service.isTerminated()) {
                try {
                    service.awaitTermination(1, TimeUnit.SECONDS);
                    if (!service.isTerminated()) {
                        throw new IllegalStateException(
                                "Unable to cleanly shut down the Parallel execution in 1 second");
                    }
                } catch (InterruptedException ie) {
                    throw new IllegalStateException("Parallel Execution interrupted. See cause.", ie);
                }
            }
        }

    }

}

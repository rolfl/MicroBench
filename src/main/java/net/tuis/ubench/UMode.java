package net.tuis.ubench;

/**
 * Indicate how to execute the UBench tasks, when run.
 * 
 * @author rolf
 *
 */
public enum UMode {
   
    /**
     * Run all iterations of the tasks one task after the next.
     * <p>
     * With 3 tasks, A, B, C:
     * 
     * <pre>
     * A1 A2 .. An
     *               B1 B2 .. Bn
     *                              C1 C2 .. Cn
     * </pre>
     * 
     */
    SEQUENTIAL(new SequentialExecutionModel()),
    
    /**
     * Allocate a separate thread to each task, and execute them all at once.
     * <p>
     * With 3 tasks, A, B, C:
     * <pre>
     * A1   A2 ..   An
     * B1 B2  ..  Bn
     * C1  C2   .. Cn
     * </pre> 
     */
    PARALLEL(new ParallelExecutionModel()),
    
    /**
     * Run one iteration from each task, then go back to the first task, repeat for all iterations.
     * <p>
     * With 3 tasks, A, B, C:
     * <pre>
     * A1       A2       ..       An
     *    B1       B2       ..       Bn
     *       C1       C2       ..       Cn
     * </pre> 
     */
    INTERLEAVED(new InterleavedExecutionModel());
    
    private final TaskExecutionModel model;
    
    private UMode(TaskExecutionModel model) {
        this.model = model;
    }

    /** 
     * Package Private: return the model implementation
     * @return the actual implementation
     */
    TaskExecutionModel getModel() {
        return model;
    }

}

package net.tuis.ubench;

/**
 * Simple interface for potential execution models (parallel, sequential, etc.).
 * 
 * @author rolf
 *
 */
interface TaskExecutionModel {

    UStats[] executeTasks(String suite, TaskRunner[] tasks);

}

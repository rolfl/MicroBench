package net.tuis.ubench;

/**
 * ExecutionModel that runs all of the first task, then all of the next, and so
 * on.
 * 
 * @author rolf
 *
 */
class SequentialExecutionModel implements TaskExecutionModel {

    @Override
    public UStats[] executeTasks(String suite, TaskRunner[] tasks) {
        UStats[] results = new UStats[tasks.length];
        for (int i = 0; i < tasks.length; i++) {
            boolean complete = false;
            TaskRunner task = tasks[i];
            do {
                complete = task.invoke();
            } while (!complete);
            results[i] = task.collect(suite);
        }
        return results;
    }

}

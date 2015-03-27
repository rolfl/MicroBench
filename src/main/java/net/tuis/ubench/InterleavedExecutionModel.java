package net.tuis.ubench;

/**
 * Simple execution model that runs 1 iteration from each task, then repeats,
 * until all tasks are complete.
 * 
 * @author rolf
 *
 */
class InterleavedExecutionModel implements TaskExecutionModel {

    @Override
    public UStats[] executeTasks(String suite, TaskRunner...tasks) {
        UStats[] results = new UStats[tasks.length];
        boolean allcomplete = false;
        boolean[] complete = new boolean[tasks.length];
        while (!allcomplete) {
            for (int i = 0; i < tasks.length; i++) {

                if (!complete[i] && tasks[i].invoke()) {

                    complete[i] = true;
                    allcomplete = true;
                    for (int j = 0; allcomplete && j < complete.length; j++) {
                        if (!complete[j]) {
                            allcomplete = false;
                        }
                    }

                }
            }
        }
        for (int i = 0; i < tasks.length; i++) {
            results[i] = tasks[i].collect(suite);
        }
        return results;
    }

}

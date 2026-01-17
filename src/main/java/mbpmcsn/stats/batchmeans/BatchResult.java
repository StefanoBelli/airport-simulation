package mbpmcsn.stats.batchmeans;

import mbpmcsn.stats.accumulating.StatCollector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BatchResult {

    /*
     * We save a list of means
     * "NomeMetrica" --> [med_1, ..., med_k]
     * k = # batch
     */
    private final Map<String, List<Double>> batchMeans = new HashMap<>();

    /*
     * Called at the end of every batch
     * Get the statistics from the Stat Collector
     */
    public void addBatchStats(StatCollector stats) {

        // save Population-Based metrics
        // stats.getPopulationStats() return Map<String, PopulationStat>
        stats.getPopulationStats().forEach((key, popStat) -> {
            // create the list for the metric if it not exists
            batchMeans.putIfAbsent(key, new ArrayList<>());
            // add mean of this batch to the list
            batchMeans.get(key).add(popStat.calculateMean());
        });

        // save Time-Based metrics
        // stats.getTimeStats() return Map<String, TimeStat>
        stats.getTimeStats().forEach((key, timeStat) -> {
            batchMeans.putIfAbsent(key, new ArrayList<>());
            batchMeans.get(key).add(timeStat.calculateMean());
        });
    }

    public Map<String, List<Double>> getResults() {
        return batchMeans;
    }

    // return number of batch
    public int getCount() {
        if (batchMeans.isEmpty()) return 0;
        return batchMeans.values().iterator().next().size();
    }
}

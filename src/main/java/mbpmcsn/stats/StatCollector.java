package mbpmcsn.stats;

import java.util.HashMap;
import java.util.Map;

/**
 * utility class for collecting statistical data during the simulation
 */

public final class StatCollector {

    // map used to associate names's metrics (es. "Queue_CheckIn") to accumulators
    private final Map<String, PopulationStat> populationStats = new HashMap<>();
    private final Map<String, TimeStat> timeStats = new HashMap<>();

    public void clear() {
        populationStats.clear();
        timeStats.clear();
    }

    // --- Population Statistics ---
    public void addSample(String name, double value) {
        populationStats.putIfAbsent(name, new PopulationStat());
        populationStats.get(name).add(value);
    }

    public double getPopulationMean(String name) {
        if (!populationStats.containsKey(name)) {
        	return 0.0;
        }

        return populationStats.get(name).calculateMean();
    }

    // --- Time Statistics ---
    public void updateArea(String name, double value, double duration) {
        timeStats.putIfAbsent(name, new TimeStat());
        timeStats.get(name).accumulate(value, duration);
    }

    public double getTimeWeightedMean(String name) {
        if (!timeStats.containsKey(name)) {
        	return 0.0;
        }

        return timeStats.get(name).calculateMean();
    }

    public Map<String, PopulationStat> getPopulationStats() {
        return populationStats;
    }

    public Map<String, TimeStat> getTimeStats() {
        return timeStats;
    }
}

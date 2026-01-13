package mbpmcsn.stats.accumulating;

import java.util.HashMap;
import java.util.Map;

/**
 * The Central Registry for all simulation statistics
 * It acts as a facade between the simulation model (Centers) and the statistical accumulators
 * Centers push data here using a String key (e.g., "Ts_CheckIn"), and this class
 * routes the data to the correct PopulationStat or TimeStat object.
 */

public final class StatCollector {

    /* maps used to associate names's metrics to accumulators */

    // example: "Ts_CheckIn" -> PopulationStat object
    private final Map<String, PopulationStat> populationStats = new HashMap<>();
    // example: "Ns_XRay" -> TimeStat object
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

    // getter
    public Map<String, PopulationStat> getPopulationStats() {
        return populationStats;
    }

    public Map<String, TimeStat> getTimeStats() {
        return timeStats;
    }
}

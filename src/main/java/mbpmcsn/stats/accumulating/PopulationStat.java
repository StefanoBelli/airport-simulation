package mbpmcsn.stats.accumulating;

/**
 * accumulator for population-based statistics
 * for example used for response times or wait times
 * calculates average = sum / numerOfObservations
 */
/**
 * Accumulator for Population-Based Statistics
 * It implements the arithmetic mean formula: E[X] = Sum(x_i) / N
 * Used for discrete metrics like Response Time, Waiting Time, Service Time
 * When Center calls 'sampleResponseTime(job)', it calculates the time difference
 * for that specific job and passes it here via 'add(val)'.
 */

public final class PopulationStat {

    private long count = 0; // N: Total number of observations
    private double sum = 0.0; // Sum(x_i): Accumulator for the values

    /**
     * Records a new observation
     * @param val = the observed value (e.g., the response time of a single job)
     */
    public void add(double val) {
        count++;
        sum += val;
    }

    /**
     * @return The arithmetic mean of all observations so far.
     */
    public double calculateMean() {
        return (count > 0) ? sum / count : 0.0;
    }

    public long getCount() {
        return count;
    }

    public void reset() {
        this.count = 0;
        this.sum = 0.0;
    }

}

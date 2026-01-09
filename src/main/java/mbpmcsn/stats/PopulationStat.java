package mbpmcsn.stats;

/**
 * accumulator for population-based statistics
 * for example used for response times or wait times
 * calculates average = sum / numerOfObservations
 */

public final class PopulationStat {

    private long count = 0; // number of observed passegers
    private double sum = 0.0; // sum of the times

    //add a new observation
    public void add(double val) {
        count++;
        sum += val; // val = observed value (es. time taken by the passenger)
    }

    // calculate the current average
    public double calculateMean() {
        return (count > 0) ? sum / count : 0.0;
    }

    public long getCount() {
        return count;
    }

}

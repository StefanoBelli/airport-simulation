package mbpmcsn.stats;

/**
 * accumulator for time-weighted statistics
 * used for example for number of users in queue or server utilization
 * calculates the time average: Integral(Value * Duration) / TotalTime.
 */

public final class TimeStat {

    private double totalArea = 0.0;   // area under the curve (ValueSum * Duration)
    private double totalTime = 0.0;   // total time of observation

    /**
     * update integral
     * val = the value of the state variable (es. 5 pax in the queue)
     * duration = the time that the value remains constant
     */
    public void accumulate(double val, double duration) {
        totalArea += val * duration;
        totalTime += duration;
    }

    // calculate the time-weighted average
    public double calculateMean() {
        return (totalTime > 0) ? totalArea / totalTime : 0.0;
    }
}

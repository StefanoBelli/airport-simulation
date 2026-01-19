package mbpmcsn.stats.accumulating;

/**
 * Accumulator for Time-Based Statistics
 * It implements the time-weighted mean formula: E[X] = Integral(x(t)dt) / T_total
 * Used for continuous state variables like Queue Length (Nq), Number in System (Ns), Busy Servers
 * Used inside 'Center.collectTimeStats()'
 * Before the state changes (e.g., numJobs++), the Center calculates how long ('duration')
 * the system stayed in the previous state ('val') and calls 'accumulate'
 */
public final class TimeStat {

    private double totalArea = 0.0;   // Integral: Sum(Value * Duration)
    private double totalTime = 0.0;   // T_total: Total simulation time observed

    /**
     * Updates the area under the curve.
     * @param val = the value of the state variable (e.g., 5 people in queue)
     * @param duration = how long this value persisted (e.g., for 12.5 seconds)
     */
    public void accumulate(double val, double duration) {
        totalArea += val * duration;
        totalTime += duration;
    }

    /**
     * @return The time-weighted average.
     */
    public double calculateMean() {
        return (totalTime > 0) ? totalArea / totalTime : 0.0;
    }

    public void reset() {
        this.totalArea = 0.0;
        this.totalTime = 0.0;
    }
}

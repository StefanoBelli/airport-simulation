package mbpmcsn.stats.sampling;

/**
 * Represents a single discrete data point in a time-series
 * It corresponds to a value observed at a specific instant 't'
 * Used to plot the evolution of the system state over time (for es. Transient Analysis)
 */
public class Sample {
    private final double timestamp; // Time 't' of observation
    private final String centerName; // Source of the data (es. "CheckIn")
    private final String metric; // What is being measured (es. "QueueLength")
    private final double value; // The measured value at time 't'

    public Sample(double timestamp, String centerName, String metric, double value) {
        this.timestamp = timestamp;
        this.centerName = centerName;
        this.metric = metric;
        this.value = value;
    }

    public double getTimestamp() { 
    	return timestamp; 
    }

    public String getCenterName() { 
    	return centerName; 
    }

    public String getMetric() { 
    	return metric; 
    }

    public double getValue() { 
    	return value; 
    }

    @Override
    public String toString() {
        //for CSV: Time;Center;Metric;Value
        return String.format("%.2f;%s;%s;%.2f", timestamp, centerName, metric, value);
    }
}

package mbpmcsn.stats;

/*
 *  a single statistic shot
 *  es. "at time 120.5, 5 pax at center CheckIn
 */
public class Sample {
    private final double timestamp;
    private final String centerName;
    private final String metric; // es. "Queue", "InService", "Total"
    private final double value;

    public Sample(double timestamp, String centerName, String metric, double value) {
        this.timestamp = timestamp;
        this.centerName = centerName;
        this.metric = metric;
        this.value = value;
    }

    public double getTimestamp() { return timestamp; }
    public String getCenterName() { return centerName; }
    public String getMetric() { return metric; }
    public double getValue() { return value; }

    @Override
    public String toString() {
        //for CSV: Time;Center;Metric;Value
        return String.format("%.2f;%s;%s;%.2f", timestamp, centerName, metric, value);
    }
}

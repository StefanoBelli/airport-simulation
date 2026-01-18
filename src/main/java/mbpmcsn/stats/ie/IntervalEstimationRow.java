package mbpmcsn.stats.ie;

import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;

import mbpmcsn.csv.annotations.*;

@CsvDescriptor
public final class IntervalEstimationRow {
	private final String metric;
	private final double width;
	private final double mean;
	private final double min;
	private final double max;

	private IntervalEstimationRow(
			String metric,
			double width, 
			double mean, 
			double min, 
			double max) {

		this.metric = metric;
		this.width = width;
		this.mean = mean;
		this.min = min;
		this.max = max;
	}

	@CsvColumn(order = 1, name = "Metric")
	public String getMetric() {
		return metric;
	}

	@CsvColumn(order = 2, name = "Mean")
	public double getMean() {
		return mean;
	}

	@CsvColumn(order = 3, name = "Width")
	public double getWidth() {
		return width;
	}

	@CsvColumn(order = 4, name = "Min")
	public double getMin() {
		return min;
	}

	@CsvColumn(order = 5, name = "Max")
	public double getMax() {
		return max;
	}

	@Override
	public String toString() {
		return String.format("%-30s | %13.4f | +/- %12.4f | [%10.4f ... %10.4f]",
					metric, mean, width, min, max);
	}

	public static List<IntervalEstimationRow> fromMapOfData(Map<String, List<Double>> data) {
		List<IntervalEstimationRow> ies = new ArrayList<>();

		for (final String metricKey : new TreeMap<>(data).keySet()) {
			List<Double> values = data.get(metricKey);

			double width = IntervalEstimation.width(values);
			double mean = values.stream().mapToDouble(v -> v).average().orElse(0.0);
			double min = mean - width;
			double max = mean + width;

			ies.add(new IntervalEstimationRow(metricKey, width, mean, min, max));
		}

		return ies;
	}
}

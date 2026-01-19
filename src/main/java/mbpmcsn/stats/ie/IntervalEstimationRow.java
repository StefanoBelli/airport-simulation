package mbpmcsn.stats.ie;

import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;

import mbpmcsn.csv.annotations.*;
import mbpmcsn.stats.batchmeans.BatchMathUtils;

@CsvDescriptor
public final class IntervalEstimationRow {
	private final String metric;
	private final double width;
	private final double mean;
	private final double min;
	private final double max;
	private final double autocorrelation;
	private final boolean showAc;

	private IntervalEstimationRow(
			String metric,
			double width, 
			double mean, 
			double min, 
			double max,
			double autocorrelation,
			boolean showAc) {

		this.metric = metric;
		this.width = width;
		this.mean = mean;
		this.min = min;
		this.max = max;
		this.autocorrelation = autocorrelation;
		this.showAc = showAc;
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

	@CsvColumn(order = 6, name = "Autocorr")
	public double getAutocorrelation() { return autocorrelation; }

	@Override
	public String toString() {
		String base = String.format("%-30s | %13.4f | +/- %12.4f | [%10.4f ... %10.4f]",
				metric, mean, width, min, max);

		if (this.showAc) {
			return base + String.format(" | AC: %6.3f", autocorrelation);
		} else {
			return base;
		}
	}

	public static List<IntervalEstimationRow> fromMapOfData(Map<String, List<Double>> data) {
		return fromMapOfData(data, false);
	}


	public static List<IntervalEstimationRow> fromMapOfData(Map<String, List<Double>> data, boolean showAc) {
		List<IntervalEstimationRow> ies = new ArrayList<>();

		for (final String metricKey : new TreeMap<>(data).keySet()) {
			List<Double> values = data.get(metricKey);

			double width = IntervalEstimation.width(values);
			double mean = values.stream().mapToDouble(v -> v).average().orElse(0.0);
			double min = mean - width;
			double max = mean + width;
			double ac = BatchMathUtils.computeAutocorrelation(values);

			ies.add(new IntervalEstimationRow(metricKey, width, mean, min, max, ac, showAc));
		}

		return ies;
	}
}

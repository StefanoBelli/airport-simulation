package mbpmcsn.runners.verification;

import mbpmcsn.csv.annotations.*;

@CsvDescriptor
public final class VerificationResultRow {

	private final String centerName;
	private final String metricName;
	private final String modelType;
	private final double simValueMean;
	private final double simValueMax;
	private final double simValueMin;
	private final double simIntvlWidth;
	private final double theoValue;
	private final boolean valueWithinIntvl;

	public VerificationResultRow(
			String centerName, 
			String metricName,
			String modelType, 
			double simValueMean,
			double simValueMin,
			double simValueMax,
			double simIntvlWidth,
			double theoValue,
			boolean valueWithinIntvl) {

		this.centerName = centerName;
		this.metricName = metricName;
		this.modelType = modelType;
		this.simValueMean = simValueMean;
		this.simValueMin = simValueMin;
		this.simValueMax = simValueMax;
		this.simIntvlWidth = simIntvlWidth;
		this.theoValue = theoValue;
		this.valueWithinIntvl = valueWithinIntvl;
	}

	@CsvColumn(order = 1, name = "Center")
	public String getCenterName() { 
		return centerName; 
	}

	@CsvColumn(order = 2, name = "Metric")
	public String getMetricName() {
		return metricName;
	}

	@CsvColumn(order = 3, name = "Model")
	public String getModelType() { 
		return modelType; 
	}

	@CsvColumn(order = 4, name = "SimValueMean")
	public double getSimValueMean() {
		return simValueMean;
	}

	@CsvColumn(order = 5, name = "SimValueMin")
	public double getSimValueMin() {
		return simValueMin;
	}

	@CsvColumn(order = 6, name = "SimValueMax")
	public double getSimValueMax() {
		return simValueMax;
	}

	@CsvColumn(order = 7, name = "SimIntvlWidth")
	public double getSimIntvlWidth() {
		return simIntvlWidth;
	}

	@CsvColumn(order = 8, name = "TheoValue")
	public double getTheoValue() {
		return theoValue;
	}

	@CsvColumn(order = 9, name = "WithinIntvl")
	public String getValueWithinIntvl() {
		return valueWithinIntvl ? "within" : "not within";
	}
}

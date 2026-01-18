package mbpmcsn.stats.ie;

import mbpmcsn.desbook.Rvms;
import java.util.List;

/* package-private */
final class IntervalEstimation {
	public static final double LEVEL_OF_CONFIDENCE = 0.95;

	public static double width(List<Double> values) {
		long n = 0;
		double sum = 0.0;
		double mean = 0.0;
		double stdev;
		double u, t, w = 0.0;
		double diff;

		Rvms rvms = new Rvms();

		for (final Double data : values) {
			n++;
			diff = data - mean;
			sum += diff * diff * (n - 1.0) / n;
			mean += diff / n;
		}

		stdev = Math.sqrt(sum / n);

		if (n > 1) {
			u = 1.0 - 0.5 * (1.0 - LEVEL_OF_CONFIDENCE);
			t = rvms.idfStudent(n - 1, u);
			w = t * stdev / Math.sqrt(n - 1);
		} else {
			throw new RuntimeException("insufficient data to do interval estimation");
		}

		return w;
	}
}

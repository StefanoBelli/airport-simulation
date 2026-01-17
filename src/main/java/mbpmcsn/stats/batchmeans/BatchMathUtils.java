package mbpmcsn.stats.batchmeans;

import java.util.List;

public class BatchMathUtils {

    /**
     * calculate autocorrelation between batch i and batch i+1
     * @param data = list mean of the batch
     * if |returned value| < 0.2, the two batch are sufficiently independent
     */
    public static double computeAutocorrelation(List<Double> data) {
        if (data == null || data.size() < 2) {
            return 1.0; // assume max correlation if non calculable
        }

        int k = data.size();
        double mean = 0.0;

        // calculate sample mean
        for (Double val : data) mean += val;
        mean /= k;

        double numerator = 0.0;
        double denominator = 0.0;

        // calculate ACF Lag-1

        // numerator: covariance between X_i e X_{i+1}
        for (int i = 0; i < k - 1; i++) {
            numerator += (data.get(i) - mean) * (data.get(i + 1) - mean);
        }

        // denominator: sample variance
        for (int i = 0; i < k; i++) {
            denominator += Math.pow(data.get(i) - mean, 2);
        }

        if (denominator == 0) return 0.0; // min correlation, all the dates are equal

        return numerator / denominator;
    }
}

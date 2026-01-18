package mbpmcsn.stats.batchmeans;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public final class BatchResult {

    /*
     * We save a list of means
     * "NomeMetrica" --> [med_1, ..., med_k]
     * k = # batch
     */
    private final Map<String, List<Double>> batchMeans = new HashMap<>();
    private final int k;

    private int currentNumOfCollectedBatches;

    public BatchResult(int k) {
    	this.k = k;
    }

    public int getK() {
    	return k;
    }

    public Map<String, List<Double>> getResults() {
        return batchMeans;
    }

    //shall return false when all k batches collected, true otherwise
    //do all the things with another method for interval est.
    //package-private visibility
    boolean addFullBatch(Map<String, Double> batch) {
    	if(currentNumOfCollectedBatches == k) {
    		return false;
    	}

    	batch.forEach((batchKey, batchValue) -> {
    		batchMeans.putIfAbsent(batchKey, new ArrayList<>());
    		batchMeans.get(batchKey).add(batchValue);
    	});

    	currentNumOfCollectedBatches++;

    	return true;
    }
}

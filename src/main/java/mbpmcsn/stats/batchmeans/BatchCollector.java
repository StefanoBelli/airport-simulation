package mbpmcsn.stats.batchmeans;

import mbpmcsn.stats.accumulating.StatCollector;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public final class BatchCollector {

	/* batch size */
	private final int b;

	/* num of batches */
	private final int k;

	/* batch being set when # of jobCount reaches b */
	private final Map<String, Double> currentBatch;

	/* collect batch stats each # of jobs */
	private final int jobInterval;

	/* enable job-based warmup condition check */
	private final int jobWarmup;

	/* enable time-based warmup condition check */
	private final double timeWarmup;

	/* this is a map of "NomeMetrica" --> [med_1, med_2, ..., med_k] */
	private final Map<String, List<Double>> batchMeans;

	/* when all of k batches are collected... do whatever you want */
	private final OnAllKBatchesDoneCallback onAllKBatchesDoneCallback;

	/* incremented whenever collectBatchStats 
	 * gets called by the center */
	private int jobCount;

	/* incremented as batches are collected */
	private int batchesCount;

	public BatchCollector(
			int b, 
			int k,
			int jobInterval,
			OnAllKBatchesDoneCallback onAllKBatchesDoneCallback) {

		this(
				b,
				k,
				jobInterval, 
				0, 
				0,
				onAllKBatchesDoneCallback);
	}

	public BatchCollector(
			int b, 
			int k,
			int jobInterval, 
			int jobWarmup,
			OnAllKBatchesDoneCallback onAllKBatchesDoneCallback) {

		this(
				b,
				k,
				jobInterval, 
				jobWarmup, 
				0,
				onAllKBatchesDoneCallback);

		if(jobWarmup == 0) {
			throw new IllegalArgumentException("ctor with jobWarmup set to 0");
		}
	}

	public BatchCollector(
			int b, 
			int k,
			int jobInterval, 
			double timeWarmup,
			OnAllKBatchesDoneCallback onAllKBatchesDoneCallback) {

		this(
				b,
				k,
				jobInterval, 
				0, 
				timeWarmup,
				onAllKBatchesDoneCallback);

		if(timeWarmup == 0) {
			throw new IllegalArgumentException("ctor with timeWarmup set to 0");
		}
	}

	private BatchCollector(
			int b, 
			int k,
			int jobInterval, 
			int jobWarmup,
			double timeWarmup,
			OnAllKBatchesDoneCallback onAllKBatchesDoneCallback) {

		this.b = b;
		this.k = k;
		this.currentBatch = new HashMap<>();
		this.batchMeans = new HashMap<>();
		this.jobInterval = jobInterval;
		this.jobWarmup = jobWarmup;
		this.timeWarmup = timeWarmup;
		this.onAllKBatchesDoneCallback = onAllKBatchesDoneCallback;
	}

	/* called by the center anyway, method impl 
	 * will take care of determining whether
	 * to collect or not, based on passed params */
	public void collectBatchStats(double nowtime, StatCollector stats) {
		if(isWarmingUp(nowtime)) {
			/* don't collect transitory data */
			stats.clear();
			return;
		}

		if(jobInterval == 0 || jobCount % jobInterval == 0) {
			addBatchStats(stats);
		}

		jobCount++;
	}

    private void addBatchStats(StatCollector stats) {
    	if(jobCount < b) {
    		return;
    	}

        // save Population-Based metrics
        // stats.getPopulationStats() return Map<String, PopulationStat>
        stats.getPopulationStats().forEach((key, popStat) -> {
            currentBatch.put(key, Double.valueOf(popStat.calculateMean()));
        });

        // save Time-Based metrics
        // stats.getTimeStats() return Map<String, TimeStat>
        stats.getTimeStats().forEach((key, timeStat) -> {
            currentBatch.put(key, Double.valueOf(timeStat.calculateMean()));
        });

    	//did we collect all k batches?
    	boolean allKBatchesDone = addFullBatch(currentBatch);

    	currentBatch.clear();
    	jobCount = 0;

    	if(allKBatchesDone) {
    		onAllKBatchesDoneCallback.onDone(this);
    	}
    }

    private boolean isWarmingUp(double nowtime) {
    	if(jobWarmup == 0 && timeWarmup == 0) {
    		return false;
    	}

    	if(jobWarmup != 0) {
    		return jobCount < jobWarmup;
    	}

    	return nowtime < timeWarmup;
    }

    private boolean addFullBatch(Map<String, Double> batch) {
    	if(batchesCount == k) {
    		return false;
    	}

    	batch.forEach((batchKey, batchValue) -> {
    		batchMeans.putIfAbsent(batchKey, new ArrayList<>());
    		batchMeans.get(batchKey).add(batchValue);
    	});

    	batchesCount++;

    	return true;
    }

	public int getB() {
		return b;
	}

	public int getK() {
    	return k;
    }

    public Map<String, List<Double>> getBatchMeans() {
        return batchMeans;
    }
}


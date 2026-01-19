package mbpmcsn.stats.batchmeans;

import mbpmcsn.stats.accumulating.StatCollector;
import mbpmcsn.center.Center;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public final class BatchCollector {
	
	private static final class PerCenterBatchInfo {
		
		/* how many jobs for this center so far */
		private int jobCount;

		/* how many batches we collected so far */
		private int batchesCount;
	}

	/* batch size */
	private final int b;

	/* num of batches */
	private final int k;

	/* batch being set when # of jobCount reaches b */
	private final Map<String, Double> currentBatch;

	/* enable time-based warmup condition check */
	private final double timeWarmup;

	/* this is a map of "NomeMetrica" --> [med_1, med_2, ..., med_k] */
	private final Map<String, List<Double>> batchMeans;

	/* when all of k batches are collected... do whatever you want */
	private final OnAllKBatchesDoneCallback onAllKBatchesDoneCallback;

	/* batch collection infos for each center */
	private final Map<String, PerCenterBatchInfo> perCenterBatchInfo;

	/* centers participating */
	private List<Center> centers;

	public BatchCollector(
			int b, 
			int k,
			double timeWarmup,
			OnAllKBatchesDoneCallback onAllKBatchesDoneCallback) {

		this.b = b;
		this.k = k;
		this.currentBatch = new HashMap<>();
		this.batchMeans = new HashMap<>();
		this.timeWarmup = timeWarmup;
		this.onAllKBatchesDoneCallback = onAllKBatchesDoneCallback;
		this.perCenterBatchInfo = new HashMap<>();
	}

	public void setCenters(List<Center> centers) {
		this.centers = centers;
	}

	public void initZeroPerCenterBatchInfo() {
		for(final Center center : centers) {
			perCenterBatchInfo.put(center.getName(), new PerCenterBatchInfo());
		}
	}

	/* called by the center anyway, method impl 
	 * will take care of determining whether
	 * to collect or not, based on passed params */
	public void collectBatchStats(String centerName, double nowtime, StatCollector stats) {
		if(isWarmingUp(nowtime)) {
			/* don't collect transitory data */
			stats.clear();
			return;
		}

		PerCenterBatchInfo batchInfo = perCenterBatchInfo.get(centerName);

		if (batchInfo.batchesCount >= k) {
			return;
		}

		addBatchStats(centerName, batchInfo, stats);
	}

    private void addBatchStats(String centerName, PerCenterBatchInfo batchInfo, StatCollector stats) {

		batchInfo.jobCount++;

		if(batchInfo.jobCount < b) {
			return;
		}

        // save Population-Based metrics
        // stats.getPopulationStats() return Map<String, PopulationStat>
        stats.getPopulationStats().forEach((key, popStat) -> {
        	if(key.contains(centerName)) {
        		currentBatch.put(key, Double.valueOf(popStat.calculateMean()));
				popStat.reset();
        	}
        });

        // save Time-Based metrics
        // stats.getTimeStats() return Map<String, TimeStat>
        stats.getTimeStats().forEach((key, timeStat) -> {
        	if(key.contains(centerName)) {
        		currentBatch.put(key, Double.valueOf(timeStat.calculateMean()));
				timeStat.reset();
        	}
        });

		saveBatchAndClean(batchInfo);

		if(areAllCentersDone()) {
			onAllKBatchesDoneCallback.onDone(this);
		}
    }

	private void saveBatchAndClean(PerCenterBatchInfo batchInfo) {
		currentBatch.forEach((batchKey, batchValue) -> {
			batchMeans.putIfAbsent(batchKey, new ArrayList<>());
			batchMeans.get(batchKey).add(batchValue);
		});

		currentBatch.clear();

		batchInfo.jobCount = 0;

		batchInfo.batchesCount++;
	}

	private boolean areAllCentersDone() {
		for(PerCenterBatchInfo info : perCenterBatchInfo.values()) {
			if(info.batchesCount < k) {
				return false; // someone is still working
			}
		}
		return true; // all finish
	}

    private boolean isWarmingUp(double nowtime) {
    	return timeWarmup != 0 && nowtime < timeWarmup;
    }

    private boolean addFullBatch(PerCenterBatchInfo batchInfo) {
    	boolean needsToContinue = false;

    	for(final String centerNameKey : perCenterBatchInfo.keySet()) {
    		if(perCenterBatchInfo.get(centerNameKey).batchesCount < k) {
    			needsToContinue = true;
    			break;
    		}
    	}

    	if(!needsToContinue) {
    		return false;
    	}

    	if(batchInfo.batchesCount == k) {
    		return true;
    	}

    	currentBatch.forEach((batchKey, batchValue) -> {
    		batchMeans.putIfAbsent(batchKey, new ArrayList<>());
    		batchMeans.get(batchKey).add(batchValue);
    	});

    	batchInfo.batchesCount++;

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

    public void clear() {
    	currentBatch.clear();
    	batchMeans.clear();
    	initZeroPerCenterBatchInfo();
    }

	// calculate mean of the means of batches
	public double getBatchGrandMean(String key) {
		List<Double> values = batchMeans.get(key);

		if (values == null || values.isEmpty()) {
			return 0.0;
		}

		double sum = 0.0;
		for (Double v : values) {
			sum += v;
		}
		return sum / values.size();
	}
}


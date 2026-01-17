package mbpmcsn.stats.batchmeans;

import mbpmcsn.stats.accumulating.StatCollector;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public final class BatchCollector {

	/* batch size */
	private final int b;

	/* rotating batch of dim b */
	private final Map<String, List<Double>> currentBatch;

	/* collect batch stats each # of jobs */
	private final int jobInterval;

	/* resulting batch means */
	private final BatchResult batchResult;

	/* what to do if collected all k batches ? */
	private final OnBatchesDoneCallback onBatchesDoneCallback;

	/* incremented whenever collectBatchStats 
	 * gets called by the center */
	private int jobCount;

	/* enable job-based warmup condition check */
	private final int jobWarmup;

	/* enable time-based warmup condition check */
	private final double timeWarmup;

	public BatchCollector(
			int b, 
			int k,
			int jobInterval, 
			OnBatchesDoneCallback onBatchesDoneCallback) {

		this(
				b,
				k,
				jobInterval, 
				onBatchesDoneCallback, 
				0, 
				0);
	}

	public BatchCollector(
			int b, 
			int k,
			int jobInterval, 
			OnBatchesDoneCallback onBatchesDoneCallback,
			int jobWarmup) {

		this(
				b,
				k,
				jobInterval, 
				onBatchesDoneCallback, 
				jobWarmup, 
				0);

		if(jobWarmup == 0) {
			throw new IllegalArgumentException("ctor with jobWarmup set to 0");
		}
	}

	public BatchCollector(
			int b, 
			int k,
			int jobInterval, 
			OnBatchesDoneCallback onBatchesDoneCallback,
			double timeWarmup) {

		this(
				b,
				k,
				jobInterval, 
				onBatchesDoneCallback, 
				0, 
				timeWarmup);

		if(timeWarmup == 0) {
			throw new IllegalArgumentException("ctor with timeWarmup set to 0");
		}
	}

	private BatchCollector(
			int b, 
			int k,
			int jobInterval, 
			OnBatchesDoneCallback onBatchesDoneCallback,
			int jobWarmup,
			double timeWarmup) {

		this.b = b;
		this.currentBatch = new HashMap<>();
		this.jobInterval = jobInterval;
		this.batchResult = new BatchResult(k);
		this.onBatchesDoneCallback = onBatchesDoneCallback;
		this.jobWarmup = jobWarmup;
		this.timeWarmup = timeWarmup;
	}

	public int getB() {
		return b;
	}

	/* called by the center anyway, method impl 
	 * will take care of determining whether
	 * to collect or not, based on passed params */
	public void collectBatchStats(double nowtime, StatCollector stats) {
		if(isWarmingUp(nowtime)) {
			return;
		}

		if(jobInterval == 0 || jobCount % jobInterval == 0) {
			addBatchStats(stats);
		}

		jobCount++;
	}

    public void addBatchStats(StatCollector stats) {
    	if(currentBatch.size() == b) {

    		//did we collect all k batches?
    		if(!batchResult.addFullBatch(currentBatch)) {
    			onBatchesDoneCallback.onBatchesDone();
    			return;
    		}

    		currentBatch.clear();
    	}

        // save Population-Based metrics
        // stats.getPopulationStats() return Map<String, PopulationStat>
        stats.getPopulationStats().forEach((key, popStat) -> {
            currentBatch.putIfAbsent(key, new ArrayList<>());
            currentBatch.get(key).add(popStat.calculateMean());
        });

        // save Time-Based metrics
        // stats.getTimeStats() return Map<String, TimeStat>
        stats.getTimeStats().forEach((key, timeStat) -> {
            currentBatch.putIfAbsent(key, new ArrayList<>());
            currentBatch.get(key).add(timeStat.calculateMean());
        });
    }

    public BatchResult getBatchResult() {
    	return batchResult;
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
}


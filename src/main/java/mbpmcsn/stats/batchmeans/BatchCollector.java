package mbpmcsn.stats.batchmeans;

import mbpmcsn.stats.accumulating.StatCollector;

import java.util.Map;
import java.util.HashMap;

public final class BatchCollector {

	/* batch size */
	private final int b;

	/* batch being set when # of jobCount reaches b */
	private final Map<String, Double> currentBatch;

	/* collect batch stats each # of jobs */
	private final int jobInterval;

	/* resulting batch means */
	private final BatchResult batchResult;

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
			int jobInterval) {

		this(
				b,
				k,
				jobInterval, 
				0, 
				0);
	}

	public BatchCollector(
			int b, 
			int k,
			int jobInterval, 
			int jobWarmup) {

		this(
				b,
				k,
				jobInterval, 
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
			double timeWarmup) {

		this(
				b,
				k,
				jobInterval, 
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
			int jobWarmup,
			double timeWarmup) {

		this.b = b;
		this.currentBatch = new HashMap<>();
		this.jobInterval = jobInterval;
		this.batchResult = new BatchResult(k);
		this.jobWarmup = jobWarmup;
		this.timeWarmup = timeWarmup;
	}

	/* called by the center anyway, method impl 
	 * will take care of determining whether
	 * to collect or not, based on passed params */
	public void collectBatchStats(double nowtime, StatCollector stats) 
			throws AllKBatchesDoneException {

		if(isWarmingUp(nowtime)) {
			return;
		}

		if(jobInterval == 0 || jobCount % jobInterval == 0) {
			addBatchStats(stats);
		}

		jobCount++;
	}

    private void addBatchStats(StatCollector stats) 
    		throws AllKBatchesDoneException {

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
    	if(!batchResult.addFullBatch(currentBatch)) {
    		throw new AllKBatchesDoneException();
    	}

    	currentBatch.clear();
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

	public int getB() {
		return b;
	}

    public BatchResult getBatchResult() {
    	return batchResult;
    }

}


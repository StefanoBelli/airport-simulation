package mbpmcsn.runners.steadystate;

import mbpmcsn.core.Constants;
import mbpmcsn.desbook.Rngs;
import mbpmcsn.core.SimulationModel;
import mbpmcsn.event.*;
import mbpmcsn.stats.accumulating.StatCollector;
import mbpmcsn.stats.batchmeans.BatchCollector;
import mbpmcsn.stats.batchmeans.OnAllKBatchesDoneCallback;
import mbpmcsn.runners.smbuilders.SimulationModelBuilder;

/* we may reuse this for the VerificationRunner */

public final class VeryLongRun {


	private final EventQueue eventQueue;
	private final StatCollector statCollector;
	private final BatchCollector batchCollector;
	private final SimulationModel simulationModel;

	private Boolean simulationGoesOn = true;

	public VeryLongRun(
			SimulationModelBuilder smBuilder,
			Rngs rngs, 
			boolean approxServicesAsExp, 
			double arrivalsMeanTime,
			double timeWarmup) {

		eventQueue = new EventQueue();
		statCollector = new StatCollector();
		batchCollector = new BatchCollector(
				Constants.BATCH_SIZE, Constants.NUM_BATCHES, timeWarmup,
				buildBatchesDoneCallback());
		simulationModel = smBuilder.build(
				rngs, eventQueue, statCollector, 
				null, batchCollector, 
				approxServicesAsExp, arrivalsMeanTime);

		batchCollector.setCenters(simulationModel.getCenters());
		batchCollector.initZeroPerCenterBatchInfo();
	}

	private OnAllKBatchesDoneCallback buildBatchesDoneCallback() {
		return new OnAllKBatchesDoneCallback() {
			@Override
			public void onDone(BatchCollector b) {
				simulationGoesOn = false;
			}
		};
	}

	public void run() {
		statCollector.clear();
		batchCollector.clear();

		// first arrival
        simulationModel.planNextArrival();

        while (simulationGoesOn) {
            // extract the upcoming event and process
            Event e = eventQueue.pop();

            // plan next arrival if got into the entire queueing network
            if (
            		e.getType() == EventType.ARRIVAL && 
            		e.getTime() == e.getJob().getArrivalTime()) {

            	simulationModel.planNextArrival();
            }

            // process popped event
            simulationModel.processEvent(e);
        }
	}

	public StatCollector getStatCollector() {
		return statCollector;
	}

	public BatchCollector getBatchCollector() {
		return batchCollector;
	}
}


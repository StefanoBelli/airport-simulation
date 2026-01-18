package mbpmcsn.core;

import java.util.List;

import mbpmcsn.center.Center;
import mbpmcsn.desbook.Rngs;
import mbpmcsn.event.EventQueue;
import mbpmcsn.stats.accumulating.StatCollector;
import mbpmcsn.stats.sampling.SampleCollector;
import mbpmcsn.stats.batchmeans.BatchCollector;

public final class ImprovedSimulationModel extends SimulationModel {
	public ImprovedSimulationModel(
    		Rngs rngs, 
    		EventQueue eventQueue, 
    		StatCollector statCollector,
    		SampleCollector sampleCollector,
    		BatchCollector batchCollector,
    		boolean approxServicesAsExp,
    		double arrivalsMeanTime) {

    	super(
    			rngs,
    			eventQueue,
    			statCollector,
    			sampleCollector,
    			batchCollector,
    			approxServicesAsExp,
    			arrivalsMeanTime
    	);
    }

    @Override
    protected final void createServiceGenerators(boolean hasToApproxToExpSvc) {

    }

    @Override
    protected final void createArrivalProcess() {

    }

    @Override
    protected final void createCenters() {

    }

    @Override 
    protected final void collectAllCenters() {

    }

    /* called from the runner */

    @Override
    public final void planNextArrival() {

    }

    @Override
    public final List<Center> getCenters() {
    	return null;
    }
}

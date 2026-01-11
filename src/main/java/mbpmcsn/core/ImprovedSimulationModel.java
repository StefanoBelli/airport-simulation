package mbpmcsn.core;

import java.util.List;

import mbpmcsn.center.Center;
import mbpmcsn.desbook.Rngs;
import mbpmcsn.event.EventQueue;
import mbpmcsn.stats.StatCollector;
import mbpmcsn.stats.SampleCollector;

public final class ImprovedSimulationModel extends SimulationModel {
	public ImprovedSimulationModel(
    		Rngs rngs, 
    		EventQueue eventQueue, 
    		StatCollector statCollector,
    		SampleCollector sampleCollector,
    		boolean approxServicesAsExp) {

    	super(
    			rngs,
    			eventQueue,
    			statCollector,
    			sampleCollector,
    			approxServicesAsExp
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

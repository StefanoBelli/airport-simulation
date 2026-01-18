package mbpmcsn.runners.smbuilders;

import mbpmcsn.core.SimulationModel;
import mbpmcsn.core.BaseSimulationModel;
import mbpmcsn.event.EventQueue;
import mbpmcsn.stats.accumulating.StatCollector;
import mbpmcsn.stats.sampling.SampleCollector;
import mbpmcsn.stats.batchmeans.BatchCollector;
import mbpmcsn.desbook.Rngs;

public final class BaseSimulationModelBuilder implements SimulationModelBuilder {
	@Override
	public SimulationModel build(
			Rngs rngs, 
			EventQueue eventQueue, 
			StatCollector statCollector, 
			SampleCollector sampleCollector, 
			BatchCollector batchCollector,
			boolean approxServicesAsExp,
			double arrivalsMeanTime) {

		return new BaseSimulationModel(
				rngs, 
				eventQueue, 
				statCollector, 
				sampleCollector, 
				batchCollector,
				approxServicesAsExp,
				arrivalsMeanTime);
	}
}


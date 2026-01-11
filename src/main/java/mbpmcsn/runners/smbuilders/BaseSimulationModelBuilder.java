package mbpmcsn.runners.smbuilders;

import mbpmcsn.core.SimulationModel;
import mbpmcsn.core.BaseSimulationModel;
import mbpmcsn.event.EventQueue;
import mbpmcsn.stats.StatCollector;
import mbpmcsn.stats.SampleCollector;
import mbpmcsn.desbook.Rngs;

public final class BaseSimulationModelBuilder implements SimulationModelBuilder {
	@Override
	public SimulationModel build(
			Rngs rngs, 
			EventQueue eventQueue, 
			StatCollector statCollector, 
			SampleCollector sampleCollector, 
			boolean approxServicesAsExp) {

		return new BaseSimulationModel(
				rngs, 
				eventQueue, 
				statCollector, 
				sampleCollector, 
				approxServicesAsExp);
	}
}


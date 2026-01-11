package mbpmcsn.runners;

import java.lang.reflect.Constructor;

import mbpmcsn.core.SimulationModel;
import mbpmcsn.event.EventQueue;
import mbpmcsn.event.EventType;
import mbpmcsn.event.Event;
import mbpmcsn.stats.StatCollector;
import mbpmcsn.stats.SampleCollector;
import mbpmcsn.desbook.Rngs;
import static mbpmcsn.core.Constants.SEED;

/**
 * finite horizon simulation with a specific duration (one working day, 06:00 - 24:00).
 */

public final class FiniteHorizonRunner implements Runner {
	private final SimulationModel simulationModel;
	private final EventQueue eventQueue;
	private final StatCollector statCollector;
	private final SampleCollector sampleCollector;
	private final Rngs rngs;
	private final double simulationTime;

	public FiniteHorizonRunner(
			Class<SimulationModel> simulationModelKlass,
			double simulationTime,
			boolean approxServicesAsExp) {

		rngs = new Rngs();
		rngs.plantSeeds(SEED);
		eventQueue = new EventQueue();
		statCollector = new StatCollector();
		sampleCollector = null;

		this.simulationTime = simulationTime;

		try {
			Constructor<SimulationModel> smCtor = 
				simulationModelKlass.getConstructor(
					Rngs.class, 
					EventQueue.class, 
					StatCollector.class, 
					SampleCollector.class, 
					boolean.class);

			simulationModel = smCtor.newInstance(
				rngs, 
				eventQueue, 
				statCollector, 
				sampleCollector, 
				approxServicesAsExp);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override 
	public void runIt() {
        statCollector.clear();
        eventQueue.clear();

        // first arrival
        simulationModel.planNextArrival();

        // Next-Event loop
        while (eventQueue.getCurrentClock() < simulationTime) {
            if (eventQueue.isEmpty()) {
                break;
            }

            // extract the upcoming event and process
            Event e = eventQueue.pop();

            // new arrival
            if (e.getType() == EventType.ARRIVAL) {
                if (e.getTime() == e.getJob().getArrivalTime()) {
                    simulationModel.planNextArrival();
                }
            }

            simulationModel.processEvent(e);
        }
 
	}
}

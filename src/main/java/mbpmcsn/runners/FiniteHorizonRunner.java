package mbpmcsn.runners;

import java.util.List;

import mbpmcsn.center.Center;
import mbpmcsn.core.SimulationModel;
import mbpmcsn.event.EventQueue;
import mbpmcsn.event.EventType;
import mbpmcsn.event.Event;
import mbpmcsn.stats.accumulating.StatCollector;
import mbpmcsn.stats.sampling.Sample;
import mbpmcsn.stats.sampling.SampleCollector;
import mbpmcsn.desbook.Rngs;
import mbpmcsn.runners.smbuilders.SimulationModelBuilder;
import mbpmcsn.stats.accumulating.StatLogger;

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
	private final double samplingInterval;

	public FiniteHorizonRunner(
			SimulationModelBuilder smBuilder,
			double simulationTime,
			boolean approxServicesAsExp,
			double samplingInterval) {

		rngs = new Rngs();
		rngs.plantSeeds(SEED);
		eventQueue = new EventQueue();
		statCollector = new StatCollector();
		sampleCollector = new SampleCollector();
		this.simulationTime = simulationTime;
		this.samplingInterval = samplingInterval;
		simulationModel = smBuilder.build(
				rngs, eventQueue, statCollector, 
				sampleCollector, approxServicesAsExp);
	}

	@Override 
	public void runIt() {
		statCollector.clear();
		eventQueue.clear();
		
		// first arrival
		simulationModel.planNextArrival();

		// pre-schedulung of sampling
		initSamplingEvents();

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

		printSimThings();
	}

	private void printSimThings() {
		System.out.println("\n######################################################################################################################################");
		System.out.println("              FINE SIMULAZIONE (ORIZZONTE FINITO)                  ");
		System.out.println("######################################################################################################################################");
		System.out.printf("Durata Simulazione:       %10.2f sec\n", simulationTime);
		System.out.printf("Intervallo Campionamento: %10.2f sec\n", samplingInterval);

		StatLogger.printReport(statCollector);

		List<Sample> samples = sampleCollector.getSamples();
		int totalSamples = samples.size();

		System.out.println("\n--- DATI TEMPORALI (Sampling) -----------------------------------------------");
		System.out.printf("Totale campioni raccolti: %d\n", totalSamples);
		System.out.println("");

		if (totalSamples > 0) {
			System.out.printf("%-10s | %-20s | %-25s | %s\n", "Time", "Center", "Metric", "Value");
			System.out.println("-----------------------------------------------------------------------------");

			for (int i = 0; i < totalSamples; i++) {
				printPrettySample(samples.get(i));
			}

			System.out.println("-----------------------------------------------------------------------------");
		} else {
			System.out.println("(Nessun campione raccolto. Controlla samplingInterval)");
		}
		System.out.println("######################################################################################################################################\n");
	}

	// helper method to print a sample row nicely aligned
	private void printPrettySample(Sample s) {
		System.out.printf("%-10.2f | %-20s | %-25s | %.4f\n",
				s.getTimestamp(),
				s.getCenterName(),
				s.getMetric(),
				s.getValue());
	}

	private void initSamplingEvents() {
		if (samplingInterval <= 0) {
			return;
		}

		// starts from interval (es. 300s), continues until < simulationTime, increment interval
		for (double t = samplingInterval; t < simulationTime; t += samplingInterval) {
			// schedule SAMPLING events for every Center
			for (final Center c : simulationModel.getCenters()) {
				Event sampleEvent = new Event(t, EventType.SAMPLING, c, null, null);
				eventQueue.add(sampleEvent);
			}
		}
	}


}

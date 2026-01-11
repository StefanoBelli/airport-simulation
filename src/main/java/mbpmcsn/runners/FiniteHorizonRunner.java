package mbpmcsn.runners;

import mbpmcsn.center.Center;
import mbpmcsn.core.SimulationModel;
import mbpmcsn.event.EventQueue;
import mbpmcsn.event.EventType;
import mbpmcsn.event.Event;
import mbpmcsn.stats.StatCollector;
import mbpmcsn.stats.SampleCollector;
import mbpmcsn.desbook.Rngs;
import mbpmcsn.runners.smbuilders.SimulationModelBuilder;
import mbpmcsn.stats.StatLogger;

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

		// PER ORA GESTIAMO COSI' SOLO PER DEBUGGARE
		System.out.println("\n--- Simulazione Terminata ---");
		StatLogger logger = new StatLogger();
		logger.printReport(statCollector);

		// Feedback visivo sui sample raccolti
		System.out.println("[INFO] Campioni raccolti per analisi temporale: " + sampleCollector.getSamples().size());

		// --- DEBUG: STAMPIAMO I CAMPIONI PER VEDERE SE ESISTONO ---
		System.out.println("\n--- ANTEPRIMA DATI CAMPIONATI (SampleCollector) ---");
		System.out.println("Time; Center; Metric; Value");

		// Stampiamo solo i primi 40 per non intasare tutto
		int count = 0;
		for (mbpmcsn.stats.Sample s : sampleCollector.getSamples()) {
			System.out.println(s.toString()); // Il toString() l'abbiamo programmato per uscire come CSV
			count++;
			if (count >= 40) break;
		}
		System.out.println("... (altri " + (sampleCollector.getSamples().size() - 40) + " campioni nascosti) ...");
	}

	private void initSamplingEvents() {
		if (samplingInterval <= 0) return;

		// starts from interval (es. 300s), continues until < simulationTime, increment interval
		for (double t = samplingInterval; t < simulationTime; t += samplingInterval) {
			// schedule SAMPLING events for every Center
			for (Center c : simulationModel.getCenters()) {
				Event sampleEvent = new Event(t, EventType.SAMPLING, c, null, null);
				eventQueue.add(sampleEvent);
			}
		}
	}
}

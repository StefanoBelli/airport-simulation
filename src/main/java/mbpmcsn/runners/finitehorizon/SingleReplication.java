package mbpmcsn.runners.finitehorizon;

import mbpmcsn.center.Center;
import mbpmcsn.core.SimulationModel;
import mbpmcsn.event.EventQueue;
import mbpmcsn.event.EventType;
import mbpmcsn.event.Event;
import mbpmcsn.stats.accumulating.StatCollector;
import mbpmcsn.stats.sampling.SampleCollector;
import mbpmcsn.desbook.Rngs;
import mbpmcsn.runners.smbuilders.SimulationModelBuilder;

/**
 * Executes a SINGLE simulation run (Replication) from time 0 to simulationTime.
 * Does NOT plant seeds. Uses the provided Rngs state.
 */

public final class SingleReplication {
    private final SimulationModel simulationModel;
    private final EventQueue eventQueue;
    private final StatCollector statCollector;
    private final SampleCollector sampleCollector;
    private final double simulationTime;

    // If > 0, activate sampling for this specific run
    private final double samplingInterval;

    public SingleReplication(
            SimulationModelBuilder smBuilder,
            Rngs rngs,
            double simulationTime,
            boolean approxServicesAsExp,
            double arrivalsMeanTime,
            double samplingInterval) {

        eventQueue = new EventQueue();
        statCollector = new StatCollector();
        sampleCollector = new SampleCollector();
        this.simulationTime = simulationTime;
        this.samplingInterval = samplingInterval;
        simulationModel = smBuilder.build(
                rngs, eventQueue, statCollector,
                sampleCollector, null, 
                approxServicesAsExp, arrivalsMeanTime);
    }

    public void runReplication() {
        statCollector.clear();
        eventQueue.clear();

        // first arrival
        simulationModel.planNextArrival();

        if (samplingInterval > 0) {
            initSamplingEvents();
        }

        while (!eventQueue.isEmpty()) {
        	boolean simulationMustContinue = 
        		eventQueue.getCurrentClock() < simulationTime;

            // extract the upcoming event and process
            Event e = eventQueue.pop();

            // new arrival, if simulation shall 
            // terminate, await for queue to drain
            if (
            		simulationMustContinue && 
            		e.getType() == EventType.ARRIVAL && 
            		e.getTime() == e.getJob().getArrivalTime()) {

            	simulationModel.planNextArrival();
            }

            simulationModel.processEvent(e);
        }
    }

    public StatCollector getStatCollector() {
        return statCollector;
    }

    public SampleCollector getSampleCollector() {
        return sampleCollector;
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

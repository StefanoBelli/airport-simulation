package mbpmcsn.core;

import java.util.List;

import mbpmcsn.entity.Job;
import mbpmcsn.center.Center;
import mbpmcsn.event.Event;
import mbpmcsn.event.EventQueue;
import mbpmcsn.stats.accumulating.StatCollector;
import mbpmcsn.stats.sampling.SampleCollector;
import mbpmcsn.stats.batchmeans.BatchCollector;
import mbpmcsn.desbook.Rngs;

public abstract class SimulationModel {

    /* core components */
    protected final EventQueue eventQueue;
    protected final StatCollector statCollector;
    protected final SampleCollector sampleCollector;
    protected final BatchCollector batchCollector;
    protected final Rngs rngs;

    protected final double arrivalsMeanTime;

    protected SimulationModel(
    		Rngs rngs, 
    		EventQueue eventQueue, 
    		StatCollector statCollector,
    		SampleCollector sampleCollector,
    		BatchCollector batchCollector,
    		boolean approxServicesAsExp,
    		double arrivalsMeanTime) {

        this.rngs = rngs;
        this.eventQueue = eventQueue;
        this.statCollector = statCollector;
        this.sampleCollector = sampleCollector;
        this.arrivalsMeanTime = arrivalsMeanTime;
        this.batchCollector = batchCollector;

        createServiceGenerators(approxServicesAsExp);
        createArrivalProcess();
        createCenters();
        collectAllCenters();
    }

    protected abstract void createServiceGenerators(boolean hasToApproxToExpSvc);
    protected abstract void createArrivalProcess();
    protected abstract void createCenters();
    protected abstract void collectAllCenters();

    /* called from the runner */
    public abstract void planNextArrival();
    public abstract List<Center> getCenters();

    /* called from the runner */
    public final void processEvent(Event e) {
        // center that manages the event
        Center target = e.getTargetCenter();

        // if target == null --> exit job from the system
        if (target == null) {
            recordJobSystemExit(e);
            return;
        }

        switch (e.getType()) {
            case ARRIVAL:
                target.onArrival(e, eventQueue);
                break;
            case DEPARTURE:
                target.onDeparture(e, eventQueue);
                break;
            case SAMPLING:
                target.onSampling(e, eventQueue);
                break;
            default:
                throw new IllegalStateException("Tipo evento sconosciuto: " + e.getType());
        }
    }

    /* called by processEvent when job exits the system */
    private final void recordJobSystemExit(Event e) {
        Job job = e.getJob();
        double exitTime = e.getTime();
        double arrivalTime = job.getArrivalTime();

        double responseTime = exitTime - arrivalTime;

        // save statistic "Tempo di Risposta Aeroporto"
        if (job.isSecurityCheckFailed()) {
            statCollector.addSample("SystemResponseTime_Failed", responseTime);
        } else {
            statCollector.addSample("SystemResponseTime_Success", responseTime);
        }
    }

    public double getArrivalsMeanTime() {
    	return arrivalsMeanTime;
    }
}

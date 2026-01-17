package mbpmcsn.core;

import mbpmcsn.center.*;
import mbpmcsn.entity.Job;
import mbpmcsn.event.Event;
import mbpmcsn.event.EventType;
import mbpmcsn.process.ArrivalProcess;
import mbpmcsn.process.ServiceProcess;
import mbpmcsn.process.rvg.ExponentialGenerator;
import mbpmcsn.process.rvg.RandomVariateGenerator;
import mbpmcsn.process.rvg.TruncatedNormalGenerator;
import mbpmcsn.routing.*;
import mbpmcsn.flowpolicy.*;
import mbpmcsn.desbook.Rngs;
import mbpmcsn.event.EventQueue;
import mbpmcsn.stats.accumulating.StatCollector;
import mbpmcsn.stats.sampling.SampleCollector;
import java.util.ArrayList;
import java.util.List;

import static mbpmcsn.core.Constants.*;

/*
 * represents the simulation mind that initializes the model topology,
 * instantiates the centers and generators, and controls the
 * next-event time advance loop
 */

public final class BaseSimulationModel extends SimulationModel {

    /* arrival */
    private ArrivalProcess arrivalProcess;

    /* centers */
    private Center centerCheckIn; // ID 1
    private Center centerVarchi; // ID 2
    private Center centerXRay; // ID 4
    private Center centerTrace; // ID 5
    private Center centerRecupero; // ID 6

    private List<Center> centers;

    /* entry point routing */
    private NetworkRoutingPoint routingIngresso;

    /* service generators */
    private RandomVariateGenerator rvgCheckIn;
    private RandomVariateGenerator rvgVarchi;
    private RandomVariateGenerator rvgXRay;
    private RandomVariateGenerator rvgTrace;
    private RandomVariateGenerator rvgRecupero;

    public BaseSimulationModel(
    		Rngs rngs, 
    		EventQueue eventQueue, 
    		StatCollector statCollector,
    		SampleCollector sampleCollector,
    		boolean approxServicesAsExp,
    		double arrivalsMeanTime) {

    	super(
    			rngs,
    			eventQueue,
    			statCollector,
    			sampleCollector,
    			approxServicesAsExp,
    			arrivalsMeanTime);
    }

    @Override
    protected final void createServiceGenerators(boolean hasToApproxToExpSvc) {
    	if(hasToApproxToExpSvc) {
    		rvgCheckIn = new ExponentialGenerator(MEAN_S1);
    		rvgVarchi = new ExponentialGenerator(MEAN_S2);
    		rvgXRay = new ExponentialGenerator(MEAN_S3);
    		rvgTrace = new ExponentialGenerator(MEAN_S4);
    		rvgRecupero = new ExponentialGenerator(MEAN_S5);
    	} else {
    		rvgCheckIn = new TruncatedNormalGenerator(MEAN_S1, STD_S1, LB1, UB1);
    		rvgVarchi = new TruncatedNormalGenerator(MEAN_S2, STD_S2, LB2, UB2);
    		rvgXRay = new TruncatedNormalGenerator(MEAN_S3, STD_S3, LB3, UB3);
    		rvgTrace = new TruncatedNormalGenerator(MEAN_S4, STD_S4, LB4, UB4);
    		rvgRecupero = new TruncatedNormalGenerator(MEAN_S5, STD_S5, LB5, UB5);
    	}
    }

    @Override
    protected final void createArrivalProcess() {
    	RandomVariateGenerator rvgArrival = new ExponentialGenerator(arrivalsMeanTime);
        arrivalProcess = new ArrivalProcess(rvgArrival, rngs, STREAM_ARRIVALS);
    }

    @Override
    protected final void createCenters() {
        // 5. Recupero Oggetti
        ServiceProcess sp5 = new ServiceProcess(rvgRecupero, rngs, STREAM_S5_SERVICE);
        NetworkRoutingPoint routingRecupero = new FixedRouting(null);
        centerRecupero = new InfiniteServer(
                ID_RECUPERO_OGGETTI,
                "Recupero",
                sp5,
                routingRecupero,
                statCollector,
                this.sampleCollector
        );

        // 4. Trace Detection
        ServiceProcess sp4 = new ServiceProcess(rvgTrace, rngs, STREAM_S4_SERVICE);
        NetworkRoutingPoint routingTrace = new TraceRouting(centerRecupero, STREAM_S4_ROUTING);
        centerTrace = new SingleServerSingleQueue(
                ID_TRACE_DETECTION ,
                "TraceDetection",
                sp4,
                routingTrace,
                statCollector,
                this.sampleCollector
        );

        // 3. X-Ray (MSMQ) - Nota: Erlang
        ServiceProcess sp3 = new ServiceProcess(rvgXRay, rngs, STREAM_S3_SERVICE);
        NetworkRoutingPoint routingXRay = new XRayRouting(centerTrace, centerRecupero, STREAM_S3_ROUTING);
        FlowAssignmentPolicy rrPolicy = new RoundRobinPolicy();
        centerXRay = new MultiServerMultiQueue(
                ID_XRAY,
                "XRay",
                sp3,
                routingXRay,
                statCollector,
                this.sampleCollector,
                M3,
                rrPolicy
        );

        // 2. Varchi (MSMQ)
        ServiceProcess sp2 = new ServiceProcess(rvgVarchi, rngs, STREAM_S2_SERVICE);
        NetworkRoutingPoint routingVarchi = new FixedRouting(centerXRay);
        FlowAssignmentPolicy sqfPolicy = new SqfPolicy(rngs, STREAM_S2_FLOWPOL);
        centerVarchi = new MultiServerMultiQueue(
                ID_VARCHI_ELETTRONICI,
                "Varchi",
                sp2,
                routingVarchi,
                statCollector,
                this.sampleCollector,
                M2,
                sqfPolicy
        );

        // 1. Check-in
        ServiceProcess sp1 = new ServiceProcess(rvgCheckIn, rngs, STREAM_S1_SERVICE);
        NetworkRoutingPoint routingCheckIn = new FixedRouting(centerVarchi);
        centerCheckIn = new MultiServerSingleQueue(
                ID_BANCHI_CHECKIN, 
                "CheckIn", 
                sp1, 
                routingCheckIn, 
                statCollector,
                this.sampleCollector,
                M1
        );

        // 0. Infine, l'Ingresso (Ora che CheckIn e Varchi esistono)
        routingIngresso = new EntryRouting(centerCheckIn, centerVarchi, STREAM_ARRIVALS);
    }

    @Override
    protected final void collectAllCenters() {
    	centers = new ArrayList<>();

        centers.add(centerCheckIn);
        centers.add(centerVarchi);
        centers.add(centerXRay);
        centers.add(centerTrace);
        centers.add(centerRecupero);
    }

    /* called from the runner */
    @Override
	public final void planNextArrival() {
		// calculate when a pax arrives (update sarrival)
		double nextArrivalTime = arrivalProcess.getArrival();

		// create the job associated to the pax
		Job newJob = new Job(nextArrivalTime);

		// initial routing: Check-in o Varchi?
		Center firstCenter = routingIngresso.getNextCenter(rngs, newJob);

		// create event
		Event arrivalEvent = new Event(
				nextArrivalTime,
				EventType.ARRIVAL,
				firstCenter,
				newJob,
				null
		);

		eventQueue.add(arrivalEvent);
	}

    @Override
    public final List<Center> getCenters() {
        return centers;
    }
}

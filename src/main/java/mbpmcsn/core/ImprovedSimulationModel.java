package mbpmcsn.core;

import java.util.ArrayList;
import java.util.List;

import mbpmcsn.center.Center;
import mbpmcsn.center.InfiniteServer;
import mbpmcsn.center.MultiServerSingleQueue;
import mbpmcsn.center.SingleServerSingleQueue;
import mbpmcsn.desbook.Rngs;
import mbpmcsn.entity.Job;
import mbpmcsn.event.Event;
import mbpmcsn.event.EventQueue;
import mbpmcsn.event.EventType;
import mbpmcsn.process.ArrivalProcess;
import mbpmcsn.process.ServiceProcess;
import mbpmcsn.process.rvg.ExponentialGenerator;
import mbpmcsn.process.rvg.RandomVariateGenerator;
import mbpmcsn.process.rvg.TruncatedNormalGenerator;
import mbpmcsn.routing.*;
import mbpmcsn.stats.accumulating.StatCollector;
import mbpmcsn.stats.sampling.SampleCollector;
import mbpmcsn.stats.batchmeans.BatchCollector;

import static mbpmcsn.core.Constants.*;

public final class ImprovedSimulationModel extends SimulationModel {

	private ArrivalProcess arrivalProcess;

	private Center centerCheckIn; // ID 1
	private Center centerVarchi; // ID 2
	private Center centerXRay; // ID 3
	private Center centerTrace; // ID 4
	private Center centerRecupero; // ID 5
	private Center fastTrack; // ID 6

	private List<Center> centers;

	private NetworkRoutingPoint routingIngresso;

	private RandomVariateGenerator rvgCheckIn;
	private RandomVariateGenerator rvgVarchi;
	private RandomVariateGenerator rvgXRay;
	private RandomVariateGenerator rvgTrace;
	private RandomVariateGenerator rvgRecupero;
	private RandomVariateGenerator rvgFastTrack;

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
		if(hasToApproxToExpSvc) {
			rvgCheckIn = new ExponentialGenerator(MEAN_S1);
			rvgVarchi = new ExponentialGenerator(MEAN_S2);
			rvgXRay = new ExponentialGenerator(MEAN_S3);
			rvgTrace = new ExponentialGenerator(MEAN_S4);
			rvgRecupero = new ExponentialGenerator(MEAN_S5);
			rvgFastTrack = new ExponentialGenerator(IMPROVED_MEAN_S6); // stessa media altri server raggi x
		} else {
			rvgCheckIn = new TruncatedNormalGenerator(MEAN_S1, STD_S1, LB1, UB1);
			rvgVarchi = new TruncatedNormalGenerator(MEAN_S2, STD_S2, LB2, UB2);
			rvgXRay = new TruncatedNormalGenerator(MEAN_S3, STD_S3, LB3, UB3);
			rvgTrace = new TruncatedNormalGenerator(MEAN_S4, STD_S4, LB4, UB4);
			rvgRecupero = new TruncatedNormalGenerator(MEAN_S5, STD_S5, LB5, UB5);
			rvgFastTrack = new TruncatedNormalGenerator(IMPROVED_MEAN_S6, IMPROVED_STD_S6, IMPROVED_LB6, IMPROVED_UB6); // stessi parametri raggi x
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
				sampleCollector,
				batchCollector
				);

		// 4. Trace Detection
		ServiceProcess sp4 = new ServiceProcess(rvgTrace, rngs, STREAM_S4_SERVICE);
		NetworkRoutingPoint routingTrace = new TraceRouting(centerRecupero, STREAM_S4_ROUTING);
		centerTrace = new MultiServerSingleQueue(
				ID_TRACE_DETECTION ,
				"TraceDetection",
				sp4,
				routingTrace,
				statCollector,
				sampleCollector,
				batchCollector,
				IMPROVED_M4
				);

		NetworkRoutingPoint routingSecurityExit = new XRayRouting(centerTrace, centerRecupero, STREAM_S3_ROUTING);

		// 3. X-Ray
		ServiceProcess sp3 = new ServiceProcess(rvgXRay, rngs, STREAM_S3_SERVICE);
		centerXRay = new MultiServerSingleQueue(
				ID_XRAY,
				"XRay",
				sp3,
				routingSecurityExit,
				statCollector,
				sampleCollector,
				batchCollector,
				M3
				);

		// --- 6. Fast Track ---
		ServiceProcess sp6 = new ServiceProcess(rvgFastTrack, rngs, STREAM_S6_SERVICE);
		fastTrack = new MultiServerSingleQueue(
				IMPROVED_ID_FAST_TRACK,
				"FastTrack",
				sp6,
				routingSecurityExit, // Converge nello stesso punto di XRay
				statCollector,
				sampleCollector,
				batchCollector,
				IMPROVED_M6
				);

		// --- 2. Varchi Elettronici ---
		ServiceProcess sp2 = new ServiceProcess(rvgVarchi, rngs, STREAM_S2_SERVICE);
		NetworkRoutingPoint routingVarchi = new VarchiRouting(fastTrack, centerXRay, STREAM_S2_ROUTING);
		centerVarchi = new MultiServerSingleQueue(
				ID_VARCHI_ELETTRONICI,
				"Varchi",
				sp2,
				routingVarchi,
				statCollector,
				sampleCollector,
				batchCollector,
				M2
				);

		// --- 1. Check in ---
		ServiceProcess sp1 = new ServiceProcess(rvgCheckIn, rngs, STREAM_S1_SERVICE);
		NetworkRoutingPoint routingCheckIn = new FixedRouting(centerVarchi);
		centerCheckIn = new MultiServerSingleQueue(
				ID_BANCHI_CHECKIN,
				"CheckIn",
				sp1,
				routingCheckIn,
				statCollector,
				sampleCollector,
				batchCollector,
				M1
				);

		// --- INGRESSO ---
		routingIngresso = new EntryRouting(centerCheckIn, centerVarchi, STREAM_ARRIVALS);

	}

	@Override 
	protected final void collectAllCenters() {
		centers = new ArrayList<>();

		centers.add(centerCheckIn);
		centers.add(centerVarchi);
		centers.add(centerXRay);
		centers.add(fastTrack); // Aggiunto
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

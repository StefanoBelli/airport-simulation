package mbpmcsn.center;

import java.util.Queue;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

import mbpmcsn.process.ServiceProcess;
import mbpmcsn.event.Event;
import mbpmcsn.event.EventQueue;
import mbpmcsn.event.EventType;
import mbpmcsn.entity.Job;
import mbpmcsn.routing.NetworkRoutingPoint;
import mbpmcsn.stats.accumulating.StatCollector;
import mbpmcsn.stats.sampling.SampleCollector;

/**
 * Represents a G/G/1 Node: Single Server and a Single FIFO Queue
 * - Capacity: 1
 * - State Variable (X): Binary (0 = Idle, 1 = Busy)
 * - Logic: If server is busy, queue. If idle, serve
 */

public class SingleServerSingleQueue extends Center {
	private boolean activeServer; // binary state
	private Queue<Job> jobQueue = new LinkedList<>(); // FIFO waiting line

	public SingleServerSingleQueue(
			int id,
			String name,
			ServiceProcess serviceProcess,
			NetworkRoutingPoint networkRoutingPoint,
			StatCollector statCollector,
			SampleCollector sampleCollector) {

		super(
				id,
				name,
				serviceProcess,
				networkRoutingPoint,
				statCollector,
				sampleCollector
		);
	}

	@Override
	public void onArrival(Event event, EventQueue eventQueue) {
		double now = eventQueue.getCurrentClock();

		// 1. UPDATE TIME-BASED STATS
		collectTimeStats(now);

		// 2. UPDATE GLOBAL COUNTER
		numJobsInNode++;

		// 3. JOB TIMESTAMPING
		Job job = event.getJob();
		job.setLastQueuedTime(now); // T_in_queue

		// 4. RESOURCE CHECK
		if (activeServer) {
			jobQueue.add(job);
			return;
		}

		// Resource IDLE: activate immediately
		activeServer = true;

		scheduleDepartureEvent(now, job, eventQueue);
	}

	@Override
	public void onDeparture(Event event, EventQueue eventQueue) {
		double now = eventQueue.getCurrentClock();

		// 1. UPDATE TIME-BASED STATS
		collectTimeStats(now);

		// 2. UPDATE GLOBAL COUNTER
		numJobsInNode--;

		// 3. JOB STATS RECORDING
		Job job = event.getJob();
		job.setLastEndServiceTime(now);

		sampleServiceTime(job);
		sampleResponseTime(job);

		// 4. ROUTING
		Center nextCenter = getNextCenter(job);

		if (nextCenter == null) {
			sampleSystemResponseTimeSuccess(now, job);
		} else {
			Event arrivalEvent = new Event(
					now, EventType.ARRIVAL, nextCenter, job, null);

			eventQueue.add(arrivalEvent);
		}

		// 5. RESOURCE MANAGEMENT
		if (jobQueue.isEmpty()) {
			activeServer = false;
			return;
		}

		job = jobQueue.remove();

		scheduleDepartureEvent(now, job, eventQueue);
	}

	private void scheduleDepartureEvent(
			double now, Job job, EventQueue eventQueue) {

		job.setLastStartServiceTime(now);

		sampleQueueTime(job); // Tq = T_start - T_in_queue

		double svc = serviceProcess.getService();
		Event departureEvent = new Event(
				now + svc, EventType.DEPARTURE, this, job, null);

		eventQueue.add(departureEvent);
	}

	@Override
	public Object doSample() {
		Map<String, Number> metrics = new HashMap<>();
		int numJobsInServer = getNumJobsInServer();

		metrics.put(sampleNsKey, numJobsInNode);
		metrics.put(sampleNqKey, numJobsInNode - numJobsInServer); // Nq = N - X
		metrics.put(sampleXKey, getNumJobsInServer()); // X = 0 or 1

		metrics.put(sampleTsKey, getResponseTimeMeanSoFar());
		metrics.put(sampleTqKey, getQueueTimeMeanSoFar());
		metrics.put(sampleSKey, getServiceTimeMeanSoFar());

		metrics.put(statSysTsKey, getSystemResponseTimeSuccessMeanSoFar());


		return metrics;
	}

	@Override
	protected void timeStats(double duration) {
		int numJobsInServer = getNumJobsInServer();

		// E[Ns]
		statCollector.updateArea(statNsKey, numJobsInNode, duration);
		// E[Nq] = N - X
		statCollector.updateArea(statNqKey, numJobsInNode - numJobsInServer, duration);
		// E[X] (Utilization)
		statCollector.updateArea(statXKey, numJobsInServer, duration);
	}

	private int getNumJobsInServer() {
		return activeServer ? 1 : 0;
	}
}


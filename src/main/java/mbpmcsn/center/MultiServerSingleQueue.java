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
 * Represents a G/G/m Node: 'm' parallel servers and a single shared FIFO queue
 * - Capacity: 'm' (numServers)
 * - Queue Discipline: FIFO (First-In-First-Out)
 * - Logic: An arriving job enters service immediately if at least one server is IDLE
 * 			Otherwise, it waits in the jobQueue
 */

public class MultiServerSingleQueue extends Center {

	// total capacity of the node (m)
	private final int numServers;
	// number of busy servers (0 <= X <= m)
	private int numActiveServers;

	// shared Waiting Queue
	private Queue<Job> jobQueue = new LinkedList<>();

	public MultiServerSingleQueue(
			int id,
			String name,
			ServiceProcess serviceProcess,
			NetworkRoutingPoint networkRoutingPoint,
			StatCollector statCollector,
			SampleCollector sampleCollector,
			int numServers) {

		super(
				id,
				name,
				serviceProcess,
				networkRoutingPoint,
				statCollector,
				sampleCollector
		);

		this.numServers = numServers;
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
		if (numActiveServers == numServers) {
			jobQueue.add(job);
			return;
		}

		// 5. SERVER ACQUISITION
		numActiveServers++;

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
		job.setLastEndServiceTime(now); // T_out

		sampleServiceTime(job); // S = T_out - T_start_service
		sampleResponseTime(job); // Ts = T_out - T_in_queue

		// 4. ROUTING (
		Center nextCenter = getNextCenter(job);

		if (nextCenter == null) {
			// EXIT FROM THE NODE: record Global System Response Time
			sampleSystemResponseTimeSuccess(now, job);
		} else {
			Event arrivalEvent = new Event(
					now, EventType.ARRIVAL, nextCenter, job, null);

			eventQueue.add(arrivalEvent);
		}

		// 5. SERVER RELEASE / REALLOCATION LOGIC
		if (jobQueue.isEmpty()) {
			numActiveServers--; // X decreases
			return;
		}

		job = jobQueue.remove();

		scheduleDepartureEvent(now, job, eventQueue);
	}

	private void scheduleDepartureEvent(
			double now, Job job, EventQueue eventQueue) {

		// record when the job leaves the queue and enters the server
		job.setLastStartServiceTime(now);

		// Tq = T_start_service - T_in_queue
		sampleQueueTime(job);

		double svc = serviceProcess.getService();

		Event departureEvent = new Event(
				now + svc, EventType.DEPARTURE, this, job, null);

		eventQueue.add(departureEvent);
	}

	@Override
	public Object doSample() {
		Map<String, Number> metrics = new HashMap<>();

		// Ns: Total users
		metrics.put(sampleNsKey, numJobsInNode);
		// Nq: Users in queue = Total - Users being served
		metrics.put(sampleNqKey, numJobsInNode - numActiveServers);
		// X: Busy servers
		metrics.put(sampleXKey, numActiveServers);

		metrics.put(sampleTsKey, getResponseTimeMeanSoFar());
		metrics.put(sampleTqKey, getQueueTimeMeanSoFar());
		metrics.put(sampleSKey, getServiceTimeMeanSoFar());

		metrics.put(statSysTsKey, getSystemResponseTimeSuccessMeanSoFar());

		return metrics;
	}

	@Override
	protected void timeStats(double duration) {
		// E[Ns]
		statCollector.updateArea(statNsKey, numJobsInNode, duration);
		// E[Nq] = N - X
		statCollector.updateArea(statNqKey, numJobsInNode - numActiveServers, duration);
		// E[X] (Busy Servers)
		statCollector.updateArea(statXKey, numActiveServers, duration);
	}
}

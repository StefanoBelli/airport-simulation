package mbpmcsn.center;

import mbpmcsn.process.ServiceProcess;
import mbpmcsn.event.Event;
import mbpmcsn.event.EventQueue;
import mbpmcsn.event.EventType;
import mbpmcsn.entity.Job;
import mbpmcsn.routing.NetworkRoutingPoint;
import mbpmcsn.stats.accumulating.StatCollector;
import mbpmcsn.stats.sampling.SampleCollector;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents a Delay Node (G/G/infinity queue)
 * - Number of Servers (m) = Infinity
 * - Queueing is impossible: logic implies Nq = 0 and Tq = 0 always
 * - Every arrival enters service immediately
 */

public class InfiniteServer extends Center {
	public InfiniteServer(
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

		// 1. UPDATE TIME-BASED STATS (Integrals)
		collectTimeStats(now);

		// 2. UPDATE STATE
		numJobsInNode++;

		// 3. SERVICE LOGIC
		double svc = serviceProcess.getService();
		Job job = event.getJob();

		// 4. JOB-BASED STATS RECORDING (Arrival Time = Start Service Time)
		job.setLastQueuedTime(now); // T_in_queue
		job.setLastStartServiceTime(now); // T_start_service (immediate)

		// we can sample Queue Time immediately here because the wait is over
		sampleQueueTime(job);

		// 5. SCHEDULE DEPARTURE
		Event departureEvent = new Event(
				now + svc, EventType.DEPARTURE, this, job, null);

		eventQueue.add(departureEvent);
	}

	@Override
	public void onDeparture(Event event, EventQueue eventQueue) {

		double now = eventQueue.getCurrentClock();

		// 1. UPDATE TIME-BASED STATS
		collectTimeStats(now);

		// 2. UPDATE STATE
		numJobsInNode--;

		// 3. JOB-BASED STATS RECORDING
		Job job = event.getJob();
		job.setLastEndServiceTime(now); // T_out

		// calculate Response Time (Ts): T_out - T_in_queue
		sampleResponseTime(job);
		// calculate Service Time (S): T_out - T_start_service
		sampleServiceTime(job);

		// 4. ROUTING
		Center nextCenter = getNextCenter(job);

		if (nextCenter == null) {
			// JOB EXIT: record Global System Response Time
			sampleSystemResponseTimeSuccess(now, job);
		} else {
			Event arrivalEvent = new Event(now, EventType.ARRIVAL, nextCenter, job, null);
			eventQueue.add(arrivalEvent);
		}
	}
	
	@Override
	public Object doSample() {
		Map<String, Number> metrics = new HashMap<>();

		metrics.put(sampleNsKey, numJobsInNode); // Users in System
		metrics.put(sampleNqKey, 0); // Users in Queue (always 0)
		metrics.put(sampleXKey, numJobsInNode); // Busy Servers (always = Ns)

		metrics.put(sampleTsKey, getResponseTimeMeanSoFar());
		metrics.put(sampleTqKey, getQueueTimeMeanSoFar());
		metrics.put(sampleSKey, getServiceTimeMeanSoFar());

		metrics.put(statSysTsKey, getSystemResponseTimeSuccessMeanSoFar());

		return metrics;
	}

	/**
	 * Updates the Area Under the Curve for Time-Based statistics
	 * called by collectTimeStats()
	 */
	@Override
	protected void timeStats(double duration) {
		// E[Ns] -> Area += CurrentJobs * Duration
		statCollector.updateArea(statNsKey, numJobsInNode, duration);
		// E[Nq] -> Area += 0 * Duration (always 0)
		statCollector.updateArea(statNqKey, 0, duration);
		// In Infinite Server, Busy Servers (X) = Number in System (Ns)
		statCollector.updateArea(statXKey, numJobsInNode, duration);
	}
}


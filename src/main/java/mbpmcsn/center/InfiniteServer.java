package mbpmcsn.center;

import mbpmcsn.process.ServiceProcess;
import mbpmcsn.event.Event;
import mbpmcsn.event.EventQueue;
import mbpmcsn.event.EventType;
import mbpmcsn.entity.Job;
import mbpmcsn.routing.NetworkRoutingPoint;
import mbpmcsn.stats.StatCollector;
import mbpmcsn.stats.SampleCollector;
import java.util.Map;
import java.util.HashMap;

/**
 * represents a Delay Node (no queue)
 * every arriving job is immediately served.
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
		collectTimeStats(now);

		numJobsInNode++;

		double svc = serviceProcess.getService();
		Job job = event.getJob();

		job.setLastQueuedTime(now);
		job.setLastStartServiceTime(now);

		sampleQueueTime(job);

		Event departureEvent = new Event(
				now + svc, EventType.DEPARTURE, this, job, null);

		eventQueue.add(departureEvent);
	}

	@Override
	public void onDeparture(Event event, EventQueue eventQueue) {
		double now = eventQueue.getCurrentClock();
		collectTimeStats(now);

		numJobsInNode--;

		Job job = event.getJob();
		job.setLastEndServiceTime(now);

		sampleResponseTime(job);
		sampleServiceTime(job);

		Center nextCenter = getNextCenter(job);

		// if no following center --> job exit -> store Response Time
		if (nextCenter == null) {
			sampleSystemResponseTimeSuccess(now, job);
		} else {
			Event arrivalEvent = new Event(now, EventType.ARRIVAL, nextCenter, job, null);
			eventQueue.add(arrivalEvent);
		}
	}
	
	@Override
	public Object doSample() {
		Map<String, Number> metrics = new HashMap<>();

		metrics.put(sampleNsKey, numJobsInNode);
		metrics.put(sampleNqKey, 0);
		metrics.put(sampleXKey, numJobsInNode);

		metrics.put(sampleTsKey, getResponseTimeMeanSoFar());
		metrics.put(sampleTqKey, getQueueTimeMeanSoFar());
		metrics.put(sampleSKey, getServiceTimeMeanSoFar());

		//this can be done by one center only, or get multiple repeated entries
		//for same time from multiple centers
		metrics.put(statSysTsKey, getSystemResponseTimeSuccessMeanSoFar());

		return metrics;
	}

	@Override
	protected void timeStats(double duration) {
		statCollector.updateArea(statNsKey, numJobsInNode, duration);
		statCollector.updateArea(statNqKey, 0, duration);
		statCollector.updateArea(statXKey, numJobsInNode, duration);
	}
}


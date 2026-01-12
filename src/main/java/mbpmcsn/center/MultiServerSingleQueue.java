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
import mbpmcsn.stats.StatCollector;
import mbpmcsn.stats.SampleCollector;

/**
 * m parallel servers and a single FIFO queue
 * jobs wait in the queue only if all m servers are busy
 */

public class MultiServerSingleQueue extends Center {
	private final int numServers;

	/* maybe this is not needed but makes it more clear */
	private int numActiveServers;

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
		collectTimeStats(now);

		numJobsInNode++;

		Job job = event.getJob();
		job.setLastQueuedTime(now);

		if (numActiveServers == numServers) {
			jobQueue.add(job);
			return;
		}

		numActiveServers++;

		scheduleDepartureEvent(now, job, eventQueue);
	}

	@Override
	public void onDeparture(Event event, EventQueue eventQueue) {
		double now = eventQueue.getCurrentClock();
		collectTimeStats(now);

		numJobsInNode--;

		Job job = event.getJob();
		job.setLastEndServiceTime(now);

		sampleServiceTime(job);
		sampleResponseTime(job);

		Center nextCenter = getNextCenter(job);

		if (nextCenter == null) {
			sampleSystemResponseTimeSuccess(now, job);
		} else {
			Event arrivalEvent = new Event(
					now, EventType.ARRIVAL, nextCenter, job, null);

			eventQueue.add(arrivalEvent);
		}

		if (jobQueue.isEmpty()) {
			numActiveServers--;
			return;
		}

		job = jobQueue.remove();

		scheduleDepartureEvent(now, job, eventQueue);
	}

	private void scheduleDepartureEvent(
			double now, Job job, EventQueue eventQueue) {

		job.setLastStartServiceTime(now);

		sampleQueueTime(job);

		double svc = serviceProcess.getService();

		Event departureEvent = new Event(
				now + svc, EventType.DEPARTURE, this, job, null);

		eventQueue.add(departureEvent);
	}

	@Override
	public Object doSample() {
		Map<String, Number> metrics = new HashMap<>();

		metrics.put(sampleNsKey, numJobsInNode);
		metrics.put(sampleNqKey, numJobsInNode - numActiveServers);
		metrics.put(sampleXKey, numActiveServers);

		metrics.put(sampleTsKey, getResponseTimeMeanSoFar());
		metrics.put(sampleTqKey, getQueueTimeMeanSoFar());
		metrics.put(sampleSKey, getServiceTimeMeanSoFar());

		return metrics;
	}

	@Override
	protected void timeStats(double duration) {
		statCollector.updateArea(statNsKey, numJobsInNode, duration);
		statCollector.updateArea(statNqKey, numJobsInNode - numActiveServers, duration);
		statCollector.updateArea(statXKey, numActiveServers, duration);
	}
}

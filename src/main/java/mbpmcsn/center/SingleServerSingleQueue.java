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
 * 1 server and a single FIFO queue
 */

public class SingleServerSingleQueue extends Center {
	private boolean activeServer;
	private Queue<Job> jobQueue = new LinkedList<>();

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
		collectTimeStats(now);

		numJobsInNode++;

		Job job = event.getJob();
		job.setLastQueuedTime(now);

		if (activeServer) {
			jobQueue.add(job);
			return;
		}

		activeServer = true;

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
			activeServer = false;
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
		int numJobsInServer = getNumJobsInServer();

		metrics.put(sampleNsKey, numJobsInNode);
		metrics.put(sampleNqKey, numJobsInNode - numJobsInServer);
		metrics.put(sampleXKey, getNumJobsInServer());

		metrics.put(sampleTsKey, getResponseTimeMeanSoFar());
		metrics.put(sampleTqKey, getQueueTimeMeanSoFar());
		metrics.put(sampleSKey, getServiceTimeMeanSoFar());

		return metrics;
	}

	@Override
	protected void timeStats(double duration) {
		int numJobsInServer = getNumJobsInServer();

		statCollector.updateArea(statNsKey, numJobsInNode, duration);
		statCollector.updateArea(statNqKey, numJobsInNode - numJobsInServer, duration);
		statCollector.updateArea(statXKey, numJobsInServer, duration);
	}

	private int getNumJobsInServer() {
		return activeServer ? 1 : 0;
	}
}


package mbpmcsn.center;

import mbpmcsn.process.ServiceProcess;
import mbpmcsn.desbook.Rngs;
import mbpmcsn.event.Event;
import mbpmcsn.event.EventQueue;
import mbpmcsn.routing.NetworkRoutingPoint;
import mbpmcsn.stats.StatCollector;
import mbpmcsn.stats.OnSamplingCallback;
import mbpmcsn.entity.Job;

/*
 * abstract base class representing a generic node in the queueing network
 * maintains the state of the center and manages the statistics
 */

public abstract class Center {
	protected final int id; // ID (es. Constants.ID_CHECKIN)
	protected final String name;

	protected final ServiceProcess serviceProcess;
	private final NetworkRoutingPoint networkRoutingPoint;
	protected final StatCollector statCollector;
	private final OnSamplingCallback onSamplingCallback;

	protected long numJobsInNode;
	protected double lastUpdateTime;

	protected Center(
			int id, 
			String name, 
			ServiceProcess serviceProcess, 
			NetworkRoutingPoint networkRoutingPoint,
			StatCollector statCollector,
			OnSamplingCallback onSamplingCallback) {

		this.id = id;
		this.name = name;
		this.serviceProcess = serviceProcess;
		this.networkRoutingPoint = networkRoutingPoint;
		this.statCollector = statCollector;
		this.onSamplingCallback = onSamplingCallback;
	}

	protected void collectTimeStats(double currentClock) {
	 	double duration = currentClock - lastUpdateTime;
	 	statCollector.updateArea("N_" + name, numJobsInNode, duration);
	 	lastUpdateTime = currentClock;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	/* wrapper method */
	protected final Center getNextCenter(Job job) {
		Rngs rngs = serviceProcess.getRngs();
		return networkRoutingPoint.getNextCenter(rngs, job);
	}

	/* called by event handler */
	public abstract void onArrival(Event event, EventQueue eventQueue);

	/* called by event handler */
	public abstract void onDeparture(Event event, EventQueue eventQueue);

	/* called by event handler */
	public final void onSampling(Event event, EventQueue eventQueue) {
		if(onSamplingCallback == null) {
			String err = 
				"sample event received, " + 
				"but no callback provided at center " + 
				name;

			throw new UnsupportedOperationException(err);
		}

		Object data = doSample();
		if(data == null) {
			String warn = 
				"WARNING: sample event received, " + 
				"has valid callback," +
				"but no data was provided by center " +
				name +
				"... ignoring.";

			System.err.println(warn);
		}

		/* nullable data, potential NullPointerException */
		onSamplingCallback.collectSample(event, eventQueue, data);
	}

	/* called when a sampling event is received: 
	 * inheriting class must implement and return its specific data */
	protected abstract Object doSample();
}


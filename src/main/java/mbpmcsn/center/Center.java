package mbpmcsn.center;

import mbpmcsn.process.ServiceProcess;
import mbpmcsn.desbook.Rngs;
import mbpmcsn.event.Event;
import mbpmcsn.stats.StatCollector;

/**
 * abstract base class representing a generic node in the queueing network
 * maintains the state of the center and manages the statistics
 */

public abstract class Center {
	protected final int id; // ID (es. Constants.ID_CHECKIN)
	protected final String name;

	protected final ServiceProcess serviceProcess;
	private final NetworkRoutingPoint networkRoutingPoint;
	private final StatCollector statCollector = new StatCollector();

	protected long numJobsInNode;
	protected double lastUpdateTime;

	protected Center(int id, String name, ServiceProcess serviceProcess, NetworkRoutingPoint networkRoutingPoint) {
		this.id = id;
		this.name = name;
		this.serviceProcess = serviceProcess;
		this.networkRoutingPoint = networkRoutingPoint;
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

	public StatCollector getStatCollector() {
		return statCollector;
	}

	/* utility method (wrapper) that should be used
	 * within onCompletion() to decide the next center
	 * to generate the arrival event for, based on the
	 * routing matrix */
	protected final Center getNextCenter() {
		Rngs rngs = serviceProcess.getRngs();
		int streamIdx = serviceProcess.getStreamIdx() + 1;
		return networkRoutingPoint.getNextCenter(rngs, streamIdx);
	}

	/* called by event handler */
	public abstract void onArrival(Event event);

	/* called by event handler */
	public abstract void onCompletion(Event event);
}


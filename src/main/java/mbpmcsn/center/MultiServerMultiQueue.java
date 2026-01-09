package mbpmcsn.center;

import mbpmcsn.process.ServiceProcess;
import mbpmcsn.event.Event;

/**
 * m server, where each server has its own dedicated queue
 * arriving jobs must choose which queue to join based on a specific policy
 * (es. Shortest Queue, Random, or Round Robin)
 */

public class MultiServerMultiQueue extends Center {
	private final int numFlows;

	public MultiServerMultiQueue(int id, String name, ServiceProcess serviceProcess, NetworkRoutingPoint networkRoutingPoint, int numFlows) {
		super(id, name, serviceProcess, networkRoutingPoint);
		this.numFlows = numFlows;
	}

	@Override
	public void onArrival(Event event) {

	}

	@Override
	public void onCompletion(Event event) {

	}
}

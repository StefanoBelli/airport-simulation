package mbpmcsn.center;

import mbpmcsn.process.ServiceProcess;
import mbpmcsn.event.Event;

/**
 * represents a Delay Node (no queue)
 * every arriving job is immediately served.
 */

public class InfiniteServer extends Center {
	public InfiniteServer(int id, String name, ServiceProcess serviceProcess, NetworkRoutingPoint networkRoutingPoint) {
		super(id, name, serviceProcess, networkRoutingPoint);
	}

	@Override
	public void onArrival(Event event) {

	}

	@Override
	public void onCompletion(Event event) {

	}
}


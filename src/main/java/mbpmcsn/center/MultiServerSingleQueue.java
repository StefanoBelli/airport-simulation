package mbpmcsn.center;

import mbpmcsn.process.ServiceProcess;
import mbpmcsn.event.Event;

/**
 * m parallel servers and a single FIFO queue
 * jobs wait in the queue only if all m servers are busy
 */

public class MultiServerSingleQueue extends Center {
	private final int numServers;

	public MultiServerSingleQueue(int id, String name, ServiceProcess serviceProcess,NetworkRoutingPoint networkRoutingPoint, int numServers) {
		super(id, name, serviceProcess, networkRoutingPoint);
		this.numServers = numServers;
	}

	@Override
	public void onArrival(Event event) {

	}

	@Override
	public void onCompletion(Event event) {

	}
}


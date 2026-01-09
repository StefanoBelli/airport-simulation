package mbpmcsn.center;

import mbpmcsn.process.ServiceProcess;
import mbpmcsn.event.Event;

/**
 * 1 server and a single FIFO queue
 */

public class SingleServerSingleQueue extends Center {
	public SingleServerSingleQueue(int id, String name, ServiceProcess serviceProcess, NetworkRoutingPoint networkRoutingPoint) {
		super(id, name, serviceProcess, networkRoutingPoint);
	}

	@Override
	public void onArrival(Event event) {

	}

	@Override
	public void onCompletion(Event event) {

	}
}


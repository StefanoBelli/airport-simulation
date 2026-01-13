package mbpmcsn.center;

import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Map;
import java.util.HashMap;

import mbpmcsn.process.ServiceProcess;
import mbpmcsn.event.Event;
import mbpmcsn.event.EventQueue;
import mbpmcsn.event.EventType;
import mbpmcsn.entity.Job;
import mbpmcsn.flowpolicy.FlowAssignmentPolicy;
import mbpmcsn.routing.NetworkRoutingPoint;
import mbpmcsn.stats.accumulating.StatCollector;
import mbpmcsn.stats.sampling.SampleCollector;

/**
 * Represents a Node with 'm' parallel servers, each having its own dedicated queue
 * - Arrival Process: Jobs arrive at the center
 * - Flow Assignment: a policy (SQF, Round Robin, Random) routes the job to sub-queue 'i'
 * - Sub-systems: the node behaves like 'm' independent G/G/1 systems running in parallel
 * - Global Stats: aggregated over all sub-queues
 */

public class MultiServerMultiQueue extends Center {

	/* let the client to decide to which sssq to redirect the job */
	private final FlowAssignmentPolicy flowAssignmentPolicy;
	/* should have fixed number of elements, numFlows */
	private final List<SssqStatus> sssqStatus = new ArrayList<>();

	public MultiServerMultiQueue(
			int id,
			String name,
			ServiceProcess serviceProcess,
			NetworkRoutingPoint networkRoutingPoint,
			StatCollector statCollector,
			SampleCollector sampleCollector,
			int numFlows,
			FlowAssignmentPolicy flowAssignmentPolicy) {

		super(
				id,
				name,
				serviceProcess,
				networkRoutingPoint,
				statCollector,
				sampleCollector
		);

		this.flowAssignmentPolicy = flowAssignmentPolicy;

		for (int i = 0; i < numFlows; i++) {
			sssqStatus.add(new SssqStatus());
		}
	}

	@Override
	public void onArrival(Event event, EventQueue eventQueue) {

		double now = eventQueue.getCurrentClock();

		// 1. UPDATE TIME-BASED STATS
		collectTimeStats(now);

		numJobsInNode++; // whole center global counter

		// 2. FLOW ASSIGNMENT

		/* choose the flow based on client-provided policy */
		int flowIdx = flowAssignmentPolicy.assignFlow(sssqStatus);

		/* get the particular sssq associated to the flow */
		SssqStatus sssq = sssqStatus.get(flowIdx);
		boolean activeServer = sssq.hasActiveServer();
		Queue<Job> jobQueue = sssq.getQueue();

		Job job = event.getJob();
		job.setLastQueuedTime(now);

		/* checking for particular sssq associated to the flow */
		if (activeServer) {
			/* add to particular sssq associated to the flow */
			jobQueue.add(job);
			return;
		}

		/* server idle -> starts immediately the service */
		sssq.setActiveServer(true);

		// 4. SCHEDULE DEPARTURE
		scheduleDepartureEvent(now, job, sssq, eventQueue);
	}

	@Override
	public void onDeparture(Event event, EventQueue eventQueue) {

		double now = eventQueue.getCurrentClock();

		// 1. UPDATE TIME-BASED STATS
		collectTimeStats(now);

		numJobsInNode--; /* whole center global counter */

		// 1. IDENTIFY THE SUB-SYSTEM
		SssqStatus sssq = (SssqStatus) event.getArgs();

		// 2. JOB STATS FINALIZATION
		Job job = event.getJob();
		job.setLastEndServiceTime(now); // T_out

		sampleServiceTime(job); // S: T_out - T_start_service
		sampleResponseTime(job); // Ts: T_out - T_in_queue

		// 3. ROUTING
		Center nextCenter = getNextCenter(job);

		if (nextCenter == null) {
			// EXIT FROM THE NODE: record Global System Response Time
			sampleSystemResponseTimeSuccess(now, job);
		} else {
			/* generate arrival event for next center, as usual.
			 * we don't care anymore about sssq within this center */
			Event arrivalEvent = new Event(
					now, EventType.ARRIVAL, nextCenter, job, null);

			eventQueue.add(arrivalEvent);
		}

		// 4. PROCESS NEXT JOB IN THIS SPECIFIC QUEUE
		Queue<Job> jobQueue = sssq.getQueue();

		/* queue for the sssq is empty, nothing to do. */
		if (jobQueue.isEmpty()) {
			sssq.setActiveServer(false);
			return;
		}

		/* keep the sssq server running: do the next job */
		job = jobQueue.remove();

		scheduleDepartureEvent(now, job, sssq, eventQueue);
	}

	private void scheduleDepartureEvent(
			double now, Job job, SssqStatus sssq, EventQueue eventQueue) {

		job.setLastStartServiceTime(now); // T_start_service
		
		sampleQueueTime(job); // calculate Wait Time = T_start - T_in_queue

		double svc = serviceProcess.getService();

		/* optional args are *not* null, they're being used to identify
		 * particular sssq when departure event happens.
		 * this links job to its sssq */
		Event departureEvent = new Event(
				now + svc, EventType.DEPARTURE, this, job, sssq);

		eventQueue.add(departureEvent);
	}

	@Override
	public Object doSample() {
		Map<String, Number> metrics = new HashMap<>();
		int numActiveServers = getNumActiveServers(); // sum of all busy servers

		// Ns = Total jobs in the node
		metrics.put(sampleNsKey, numJobsInNode);
		// Nq = Total jobs - Jobs currently being served
		metrics.put(sampleNqKey, numJobsInNode - numActiveServers);
		// X = Number of busy servers (used for Utilization % calculation)
		metrics.put(sampleXKey, numActiveServers);

		metrics.put(sampleTsKey, getResponseTimeMeanSoFar());
		metrics.put(sampleTqKey, getQueueTimeMeanSoFar());
		metrics.put(sampleSKey, getServiceTimeMeanSoFar());

		metrics.put(statSysTsKey, getSystemResponseTimeSuccessMeanSoFar());

		return metrics;
	}

	@Override
	protected void timeStats(double duration) {
		int numActiveServers = getNumActiveServers();

		// aggregate statistics for the whole center
		statCollector.updateArea(statNsKey, numJobsInNode, duration);
		// E[Nq] = Total Jobs - Jobs in Service
		statCollector.updateArea(statNqKey, numJobsInNode - numActiveServers, duration);
		// E[X] = Sum of busy servers over time
		statCollector.updateArea(statXKey, numActiveServers, duration);
	}

	private int getNumActiveServers() {
		int numActiveServers = 0;

		for (final SssqStatus status : sssqStatus) {
			if (status.hasActiveServer()) {
				numActiveServers++;
			}
		}

		return numActiveServers;
	}
}

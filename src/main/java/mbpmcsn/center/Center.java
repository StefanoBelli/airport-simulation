package mbpmcsn.center;

import mbpmcsn.process.ServiceProcess;
import mbpmcsn.desbook.Rngs;
import mbpmcsn.event.Event;
import mbpmcsn.event.EventQueue;
import mbpmcsn.routing.NetworkRoutingPoint;
import mbpmcsn.stats.StatCollector;
import mbpmcsn.stats.SampleCollector;
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
	private final SampleCollector sampleCollector;

	protected final String statTsKey;
	protected final String statTqKey;
	protected final String statSKey;
	protected final String statNsKey;
	protected final String statNqKey;
	protected final String statXKey;

	protected static final String sampleTsKey = "TimeTotal";
	protected static final String sampleTqKey = "TimeQueue";
	protected static final String sampleSKey = "TimeService";
	protected static final String sampleNsKey = "NumTotal";
	protected static final String sampleNqKey = "NumQueue";
	protected static final String sampleXKey = "Utilization";

	protected static final String statSysTsKey = "SystemResponseTime_Success";

	protected long numJobsInNode;
	protected double lastUpdateTime;

	protected Center(
			int id, 
			String name, 
			ServiceProcess serviceProcess, 
			NetworkRoutingPoint networkRoutingPoint,
			StatCollector statCollector,
			SampleCollector sampleCollector) {

		this.id = id;
		this.name = name;
		this.serviceProcess = serviceProcess;
		this.networkRoutingPoint = networkRoutingPoint;
		this.statCollector = statCollector;
		this.sampleCollector = sampleCollector;

		statTsKey = "Ts_" + name;
		statTqKey = "Tq_" + name;
		statSKey = "S_" + name;
		statNsKey = "Ns_" + name;
		statNqKey = "Nq_" + name;
		statXKey = "X_" + name;
	}

	/* specify here stats you want to collect */
	protected abstract void timeStats(double duration);

	/* call this method when you need to collect */
	protected final void collectTimeStats(double currentClock) {
	 	double duration = currentClock - lastUpdateTime;
	 	timeStats(duration);
	 	lastUpdateTime = currentClock;
	}

	/* helpers, you just need to set things accordingly in job */
	protected final void sampleResponseTime(Job job) {
		double queuedTime = job.getLastQueuedTime();
		double endServiceTime = job.getLastEndServiceTime();

		statCollector.addSample(statTsKey, endServiceTime - queuedTime);
	}

	protected final void sampleServiceTime(Job job) {
		double endServiceTime = job.getLastEndServiceTime();
		double startServiceTime = job.getLastStartServiceTime();

		statCollector.addSample(statSKey, endServiceTime - startServiceTime);
	}

	protected final void sampleQueueTime(Job job) {
		double queuedTime = job.getLastQueuedTime();
		double startServiceTime = job.getLastStartServiceTime();

		statCollector.addSample(statTqKey, startServiceTime - queuedTime);
	}

	protected final void sampleSystemResponseTimeSuccess(double now, Job job) {
		double responseTime = now - job.getArrivalTime();

		statCollector.addSample(statSysTsKey, responseTime);
	}

	protected final double getResponseTimeMeanSoFar() {
		return statCollector.getPopulationMean(statTsKey);
	}

	protected final double getQueueTimeMeanSoFar() {
		return statCollector.getPopulationMean(statTqKey);
	}

	protected final double getServiceTimeMeanSoFar() {
		return statCollector.getPopulationMean(statSKey);
	}

	protected final double getSystemResponseTimeSuccessMeanSoFar() {
		return statCollector.getPopulationMean(statSysTsKey);
	}

	/* common properties  */
	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	/* helper to use the networkRoutingPoint */
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
		if(sampleCollector == null) {
			String err = 
				"sample event received, " + 
				"but no SampleCollector provided at center " + 
				name;

			throw new UnsupportedOperationException(err);
		}

		Object data = doSample();
		if(data == null) {
			String warn = 
				"WARNING: sample event received, " + 
				"has valid SampleCollector," +
				"but no data was provided by center " +
				name +
				"... ignoring.";

			System.err.println(warn);
		}

		/* nullable data, potential NullPointerException */
		sampleCollector.collectSample(event, eventQueue, data);
	}

	/* called when a sampling event is received: 
	 * inheriting class must implement and return its specific data */
	protected abstract Object doSample();
}

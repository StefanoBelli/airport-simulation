package mbpmcsn.center;

import mbpmcsn.process.ServiceProcess;
import mbpmcsn.desbook.Rngs;
import mbpmcsn.event.Event;
import mbpmcsn.event.EventQueue;
import mbpmcsn.routing.NetworkRoutingPoint;
import mbpmcsn.stats.accumulating.StatCollector;
import mbpmcsn.stats.sampling.SampleCollector;
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
	protected final StatCollector statCollector; // accumulator for final averages
	private final SampleCollector sampleCollector; // collector for time-series data

	// --- KEYS FOR JOB-BASED STATS ---
	protected final String statTsKey; // System Response Time (Wait + Service) -> E[Ts]
	protected final String statTqKey; // Queue Time (Wait) -> E[Tq]
	protected final String statSKey; // Service Time -> E[S]
	// --- KEYS FOR TIME-BASED STATS ---
	protected final String statNsKey; // Number in System -> E[Ns]
	protected final String statNqKey; // Number in Queue -> E[Nq]
	protected final String statXKey; // Number of Busy Servers -> Used for Utilization (Rho)
	// --- KEYS FOR SAMPLING  ---
	protected static final String sampleTsKey = "TimeTotal";
	protected static final String sampleTqKey = "TimeQueue";
	protected static final String sampleSKey = "TimeService";
	protected static final String sampleNsKey = "NumTotal";
	protected static final String sampleNqKey = "NumQueue";
	protected static final String sampleXKey = "BusyServers";
	// Special key for the Global System Response Time
	protected static final String statSysTsKey = "SystemResponseTime_Success";

	// --- STATE VARIABLES ---
	protected long numJobsInNode; // current number of jobs in this center
	protected double lastUpdateTime; // timestamp of the last state change

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

	// ------------------------------------------------------------------------
	// METHODS FOR TIME-BASED STATISTICS
	// ------------------------------------------------------------------------

	/**
	 * abstract method to define how to accumulate the area under the curve for Time-Based statistics
	 * implemented by subclasses
	 * @param duration = the time interval since the last update
	 */
	protected abstract void timeStats(double duration);

	/**
	 * updates the integral of the state variables (Area = Value * Duration)
	 * must be called before changing the state (numJobsInNode++ or --)
	 * @param currentClock = the current simulation time
	 */
	protected final void collectTimeStats(double currentClock) {
	 	double duration = currentClock - lastUpdateTime;
	 	timeStats(duration);
	 	lastUpdateTime = currentClock;
	}

	// ------------------------------------------------------------------------
	// HELPER METHODS FOR POPULATION-BASED STATISTICS (Job-Averaged)
	// logic: Average = Sum(Observation_i) / N
	// ------------------------------------------------------------------------

	/**
	 * records the Response Time (Ts) for a single job
	 * Ts = Departure Time - Queue Entry Time
	 */
	protected final void sampleResponseTime(Job job) {
		double queuedTime = job.getLastQueuedTime();
		double endServiceTime = job.getLastEndServiceTime();

		statCollector.addSample(statTsKey, endServiceTime - queuedTime);
	}

	/**
	 * records the Service Time (S) for a single job
	 * S = Service End Time - Service Start Time
	 */
	protected final void sampleServiceTime(Job job) {
		double endServiceTime = job.getLastEndServiceTime();
		double startServiceTime = job.getLastStartServiceTime();

		statCollector.addSample(statSKey, endServiceTime - startServiceTime);
	}

	/**
	 * records the Queue Time (Tq) for a single job
	 * Tq = Service Start Time - Queue Entry Time
	 */
	protected final void sampleQueueTime(Job job) {
		double queuedTime = job.getLastQueuedTime();
		double startServiceTime = job.getLastStartServiceTime();

		statCollector.addSample(statTqKey, startServiceTime - queuedTime);
	}

	/**
	 * records the Global System Response Time.
	 * (only called when the job permanently leaves the system)
	 * SystemTime = System Exit Time - Airport Arrival Time
	 */
	protected final void sampleSystemResponseTimeSuccess(double now, Job job) {
		double responseTime = now - job.getArrivalTime();

		statCollector.addSample(statSysTsKey, responseTime);
	}

	// ------------------------------------------------------------------------
	// GETTERS FOR SAMPLING (Current Means)
	// ------------------------------------------------------------------------
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

	// ------------------------------------------------------------------------
	// EVENT HANDLING
	// ------------------------------------------------------------------------

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

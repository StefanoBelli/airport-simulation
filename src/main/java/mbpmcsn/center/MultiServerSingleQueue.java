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
import mbpmcsn.stats.OnSamplingCallback;

/**
 * m parallel servers and a single FIFO queue
 * jobs wait in the queue only if all m servers are busy
 */

public class MultiServerSingleQueue extends Center {
    private final int numServers;

    /* maybe this is not needed but makes it more clear */
    private int numActiveServers;

    private Queue<Job> jobQueue = new LinkedList<>();

    public MultiServerSingleQueue(
            int id,
            String name,
            ServiceProcess serviceProcess,
            NetworkRoutingPoint networkRoutingPoint,
            StatCollector statCollector,
            OnSamplingCallback onSamplingCallback,
            int numServers) {

        super(
                id,
                name,
                serviceProcess,
                networkRoutingPoint,
                statCollector,
                onSamplingCallback
        );

        this.numServers = numServers;
    }

    @Override
    public void onArrival(Event event, EventQueue eventQueue) {

        double now = eventQueue.getCurrentClock();
        collectTimeStats(now);

        numJobsInNode++;

        Job job = event.getJob();

        if (numActiveServers == numServers) {
            jobQueue.add(job);
            return;
        }

        numActiveServers++;

        scheduleDepartureEvent(now, job, eventQueue);
    }

    @Override
    public void onDeparture(Event event, EventQueue eventQueue) {

        double now = eventQueue.getCurrentClock();
        collectTimeStats(now);

        numJobsInNode--;

        Job job = event.getJob();
        Center nextCenter = getNextCenter(job);

        if (nextCenter == null) {
            statCollector.addSample("SystemResponseTime_Success", now - job.getArrivalTime());
        } else {
            Event arrivalEvent = new Event(
                    now, EventType.ARRIVAL, nextCenter, job, null);

            eventQueue.add(arrivalEvent);
        }

        if (jobQueue.isEmpty()) {
            numActiveServers--;
            return;
        }

        job = jobQueue.remove();

        scheduleDepartureEvent(now, job, eventQueue);
    }

    @Override
    public Object doSample() {
        Map<String, Number> metrics = new HashMap<>();

        // total passengers in the center (queue + service)
        metrics.put("Total", this.numJobsInNode);

        // passengers in the queue
        metrics.put("Queue", this.jobQueue.size());

        // number of busy server
        metrics.put("BusyServers", this.numActiveServers);

        return metrics;
    }

    private void scheduleDepartureEvent(
            double now, Job job, EventQueue eventQueue) {

        double svc = serviceProcess.getService();

        Event departureEvent = new Event(
                now + svc, EventType.DEPARTURE, this, job, null);

        eventQueue.add(departureEvent);
    }
}


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
 * 1 server and a single FIFO queue
 */

public class SingleServerSingleQueue extends Center {
    private boolean activeServer;
    private Queue<Job> jobQueue = new LinkedList<>();

    public SingleServerSingleQueue(
            int id,
            String name,
            ServiceProcess serviceProcess,
            NetworkRoutingPoint networkRoutingPoint,
            StatCollector statCollector,
            OnSamplingCallback onSamplingCallback) {

        super(
                id,
                name,
                serviceProcess,
                networkRoutingPoint,
                statCollector,
                onSamplingCallback
        );
    }

    @Override
    public void onArrival(Event event, EventQueue eventQueue) {

        double now = eventQueue.getCurrentClock();
        collectTimeStats(now);

        numJobsInNode++;

        Job job = event.getJob();

        if (activeServer) {
            jobQueue.add(job);
            return;
        }

        activeServer = true;

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
            activeServer = false;
            return;
        }

        job = jobQueue.remove();

        scheduleDepartureEvent(now, job, eventQueue);
    }

    @Override
    public Object doSample() {
        Map<String, Number> metrics = new HashMap<>();

        metrics.put("Total", this.numJobsInNode);
        metrics.put("Queue", this.jobQueue.size());
        // activeServer is booleano, we convert it to a number (1 or 0)
        metrics.put("BusyServers", this.activeServer ? 1 : 0);

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


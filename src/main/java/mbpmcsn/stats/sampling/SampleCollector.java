package mbpmcsn.stats.sampling;

import mbpmcsn.event.Event;
import mbpmcsn.event.EventQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Collects and stores discrete samples generated during the simulation
 * Unlike StatCollector (which aggregates/averages), this class stores
 * the raw sequence of data points
 */
public final class SampleCollector {

    // The database of all samples collected
    private final List<Sample> samples = new ArrayList<>();

    /**
     * Processing method called when a SAMPLING event occurs
     * @param event = the generic event triggering the sample
     * @param eventQueue = reference to the engine (to get current time 'now')
     * @param data = the payload returned by Center.doSample()
     * Expected to be a Map<String, Number> (es. {"Queue": 10, "Busy": 4})
     */
    @SuppressWarnings("unchecked")
    public void collectSample(Event event, EventQueue eventQueue, Object data) {
        if (data == null) {
        	return;
        }

        double now = eventQueue.getCurrentClock(); // 't'
        String centerName = event.getTargetCenter().getName();

        // a Center must return Map<String, Number>
        if (data instanceof Map) {
            try {
                Map<String, Number> metrics = (Map<String, Number>) data;

                // obtain individual Sample objects from the map
                for (Map.Entry<String, Number> entry : metrics.entrySet()) {
                    samples.add(new Sample(
                            now, // Timestamp
                            centerName, // Source
                            entry.getKey(), // Metric Name
                            entry.getValue().doubleValue() // Value
                    ));
                }
            } catch (ClassCastException e) {
                System.err.println("ERRORE: I dati dal centro " + centerName + " non sono Numeri validi.");
            }
        } else {
            System.err.println("ERRORE: Il centro " + centerName + " non ha restituito una Mappa!");
        }
    }

    /**
     * @return The full history of samples
     */
    public List<Sample> getSamples() {
        return samples;
    }
}

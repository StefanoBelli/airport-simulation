package mbpmcsn.stats;

import mbpmcsn.event.Event;
import mbpmcsn.event.EventQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/* similar logic as StatsCollector, just for samples
//to be used within the OnSampleCallback for each specific center */
public final class SampleCollector implements OnSamplingCallback {

    private final List<Sample> samples = new ArrayList<>();

    @Override
    @SuppressWarnings("unchecked")
    public void collectSample(Event event, EventQueue eventQueue, Object data) {

        if (data == null) return;

        double now = eventQueue.getCurrentClock();
        String centerName = event.getTargetCenter().getName();

        // a Center must return Map<String, Number>
        if (data instanceof Map) {
            try {
                Map<String, Number> metrics = (Map<String, Number>) data;

                for (Map.Entry<String, Number> entry : metrics.entrySet()) {
                    samples.add(new Sample(
                            now,
                            centerName,
                            entry.getKey(),                // es: "Queue", "Service", "Total"
                            entry.getValue().doubleValue() // es: 5.0
                    ));
                }
            } catch (ClassCastException e) {
                System.err.println("ERRORE: I dati dal centro " + centerName + " non sono Numeri validi.");
            }
        } else {
            System.err.println("ERRORE: Il centro " + centerName + " non ha restituito una Mappa!");
        }
    }

    public List<Sample> getSamples() {
        return samples;
    }

}

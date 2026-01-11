package mbpmcsn.stats;

import java.util.Map;

/**
 * handles the output of simulation results
 * for then using the CSV utility classes
 */

public final class StatLogger {

    // PER ORA GESTIAMO COSI' SOLO PER DEBUGGARE
    public void printReport(StatCollector sc) {
        // Stampa Statistiche sui Passeggeri (Tempi di risposta, ecc.)
        System.out.println("\n--- POPULATION BASED (Tempi/Conteggi) ---");
        System.out.printf("%-30s | %12s | %8s\n", "Metric", "Mean", "Count");
        System.out.println("-------------------------------------------------------");

        for (Map.Entry<String, PopulationStat> entry : sc.getPopulationStats().entrySet()) {
            System.out.printf("%-30s | %12.4f | %8d\n",
                    entry.getKey(),
                    entry.getValue().calculateMean(),
                    entry.getValue().getCount());
        }

        // Stampa Statistiche sul Tempo (Code medie, Utilizzo, ecc.)
        System.out.println("\n--- TIME BASED (Code Medie/Utilizzo) ---");
        System.out.printf("%-30s | %12s\n", "Metric", "Mean");
        System.out.println("---------------------------------------------");

        for (Map.Entry<String, TimeStat> entry : sc.getTimeStats().entrySet()) {
            System.out.printf("%-30s | %12.4f\n",
                    entry.getKey(),
                    entry.getValue().calculateMean());
        }
        System.out.println("---------------------------------------------\n");
    }
}

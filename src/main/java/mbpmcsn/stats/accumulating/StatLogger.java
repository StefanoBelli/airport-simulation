package mbpmcsn.stats.accumulating;

import mbpmcsn.core.Constants;

import java.util.Map;
import java.util.TreeMap;

/**
 * Reporting Utility:
 * Iterates through the collected statistics in StatCollector and prints
 * a formatted report to the console
 */

public final class StatLogger {

    // PER ORA GESTIAMO COSI' SOLO PER DEBUGGARE
    public static void printReport(StatCollector sc) {

        System.out.println("\n==========================================================================================================");
        System.out.println("                REPORT FINALE SIMULAZIONE AEROPORTO                ");
        System.out.println("==========================================================================================================");

        // 1. STATISTICHE GLOBALI DI SISTEMA
        System.out.println("\n--- Statistiche Globali ----------------------------------------------------");
        if (sc.getPopulationStats().containsKey("SystemResponseTime_Success")) {
            PopulationStat sysStat = sc.getPopulationStats().get("SystemResponseTime_Success");
            double avgSec = sysStat.calculateMean();
            double avgMin = avgSec / 60.0;
            System.out.printf("Tempo Medio Totale (Ingresso -> Uscita):  %8.2f sec  (~%.1f min)\n", avgSec, avgMin);
            System.out.printf("Passeggeri totali usciti con successo:    %8d\n", sysStat.getCount());
        } else {
            System.out.println("(Nessun passeggero ha completato il percorso)");
        }

        if (sc.getPopulationStats().containsKey("SystemResponseTime_Failed")) {
            PopulationStat failStat = sc.getPopulationStats().get("SystemResponseTime_Failed");
            System.out.printf("Tempo Medio Totale (Failed / Rejected):   %8.2f sec\n", failStat.calculateMean());
            System.out.printf("Passeggeri respinti:                      %8d\n", failStat.getCount());
        }

        // 2. STATISTICHE SUI TEMPI (Population Based) - ORDINATE
        System.out.println("\n--- Job-Averaged -----------------------------------------------------------");
        System.out.printf("%-20s | %-25s | %10s | %8s\n", "Centro", "Metrica", "Media (s)", "Count");
        System.out.println("----------------------------------------------------------------------------");

        Map<String, PopulationStat> sortedPop = new TreeMap<>(sc.getPopulationStats());

        for (Map.Entry<String, PopulationStat> entry : sortedPop.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("SystemResponseTime")) continue; // già stampato sopra

            String[] parts = decodeKey(key); // metodo helper per tradurre
            String center = parts[0];
            String metric = parts[1];

            System.out.printf("%-20s | %-25s | %10.2f | %8d\n",
                    center,
                    metric,
                    entry.getValue().calculateMean(),
                    entry.getValue().getCount());
        }

        // 3. STATISTICHE SUI CONTEGGI (Time Based) - ORDINATE
        System.out.println("\n--- Time-Averaged ------------------------------------------------------------------------------------");
        System.out.printf("%-20s | %-25s | %10s\n", "Centro", "Metrica", "Media", "Note");
        System.out.println("------------------------------------------------------------------------------------------------------");

        Map<String, TimeStat> sortedTime = new TreeMap<>(sc.getTimeStats());

        for (Map.Entry<String, TimeStat> entry : sortedTime.entrySet()) {
            String key = entry.getKey();
            String[] parts = decodeKey(entry.getKey());
            String center = parts[0];
            String metric = parts[1];
            double meanValue = entry.getValue().calculateMean();

            String note = "";
            if (key.startsWith("X_")) { // chiave interna per Busy Servers
                int capacity = getCapacity(center);
                if (capacity > 0) { // solo per centri con capacità finita
                    double utilization = (meanValue / capacity) * 100.0;
                    note = String.format("Utilization: %.2f%% (su %d server)", utilization, capacity);
                } else {
                    note = "(Infinite Server)";
                }
            }

            System.out.printf("%-20s | %-25s | %10.4f | %s\n",
                    center,
                    metric,
                    meanValue,
                    note);
        }
        System.out.println("------------------------------------------------------------------------------------------------------");
        System.out.println("=========================================================================================================\n");
    }

    private static String[] decodeKey(String key) {
        // Formato atteso: PREFISSO_NomeCentro (es. Ts_CheckIn)
        String[] parts = key.split("_", 2);
        if (parts.length < 2) return new String[]{"Sistema", key};

        String prefix = parts[0];
        String center = parts[1];
        String desc = prefix;

        switch (prefix) {
            // Population Based
            case "Ts": desc = "Tempo Risposta (Tot)"; break; // wait + service
            case "Tq": desc = "Tempo Attesa (Coda)"; break;
            case "S":  desc = "Tempo Servizio"; break;
            // Time Based
            case "Ns": desc = "Num. Utenti (Tot)"; break;
            case "Nq": desc = "Num. Utenti (Coda)"; break;
            case "X":  desc = "Server Attivi (Media)"; break;
        }

        return new String[]{center, desc};
    }

    // Helper per ottenere la capacità totale (M) dal file Constants
    private static int getCapacity(String centerName) {
        switch (centerName) {
            case "CheckIn": return Constants.M1;
            case "Varchi":  return Constants.M2;
            case "XRay":    return Constants.M4;
            case "TraceDetection": return 1; // Single Server
            // Infinite Server (non ha senso calcolarla)
            case "Preparazione": return -1;
            case "Recupero":     return -1;
            default: return 1; // Fallback
        }
    }
}

package mbpmcsn.runners.finitehorizon;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import mbpmcsn.stats.accumulating.StatCollector;
import mbpmcsn.stats.sampling.SampleCollector;
import mbpmcsn.stats.sampling.Sample;
import mbpmcsn.desbook.Rngs;
import mbpmcsn.runners.smbuilders.SimulationModelBuilder;
import mbpmcsn.runners.Runner;
import mbpmcsn.stats.accumulating.StatLogger;
import mbpmcsn.stats.ie.IntervalEstimationRow;
import mbpmcsn.csv.CsvWriter;
import mbpmcsn.csv.CsvWriterException;

import static mbpmcsn.core.Constants.SEED;

/**
 * finite horizon simulation with a specific duration (one working day, 06:00 - 24:00).
 */

public final class FiniteHorizonRunner implements Runner {

	private static final int NUM_SHOWN_PILOT_SAMPLES = 40;
	private static final int NUM_REPLICATIONS = 64;

	private final String experimentName;
	private final SimulationModelBuilder builder;
	private final double simulationTime;
	private final boolean approxServicesAsExp;
	private final double arrivalsMeanTime;
	private final double samplingInterval;
	private final Rngs rngs;

	public FiniteHorizonRunner(
			String experimentName,
			SimulationModelBuilder smBuilder,
			double simulationTime,
			boolean approxServicesAsExp,
			double arrivalsMeanTime,
			double samplingInterval) {

		this.experimentName = experimentName;
		this.builder = smBuilder;
		this.simulationTime = simulationTime;
		this.approxServicesAsExp = approxServicesAsExp;
		this.samplingInterval = samplingInterval;
		this.arrivalsMeanTime = arrivalsMeanTime;
		this.rngs = new Rngs();
		this.rngs.plantSeeds(SEED);
	}

	@Override 
	public void runIt() {

		// STAMPA HEADER
		printExperimentHeader();

		// MAPPE  PER RACCOGLIERE I DATI DI TUTTE LE REPLICAZIONI
		Map<String, List<Double>> populationData = new HashMap<>();
		Map<String, List<Double>> timeData = new HashMap<>();
		Map<Integer, List<Sample>> runsSamples = new HashMap<>();

		// LOOP DELLE REPLICAZIONI
		for (int i = 0; i < NUM_REPLICATIONS; i++) {

			// ESECUZIONE DELLA SINGOLA REPLICA
			SingleReplication run = new SingleReplication(
					builder, rngs, simulationTime, 
					approxServicesAsExp, arrivalsMeanTime, 
					samplingInterval
			);

			run.runReplication();
			StatCollector stats = run.getStatCollector();

			// ACCUMULO DATI SU TUTTE LE RUN
			for (final String key : stats.getPopulationStats().keySet()) {
				populationData.putIfAbsent(key, new ArrayList<>());
				populationData.get(key).add(stats.getPopulationMean(key));
			}

			for (final String key : stats.getTimeStats().keySet()) {
				timeData.putIfAbsent(key, new ArrayList<>());
				timeData.get(key).add(stats.getTimeWeightedMean(key));
			}

			SampleCollector sampleCollector = run.getSampleCollector();
			List<Sample> samples = sampleCollector.getSamples();

			runsSamples.put(i, samples);

			// GESTIONE OUTPUT DETTAGLIATO (SOLO RUN 1)
			if (i == 0) {
				printPilotRunDiagnostic(stats, samples);
				printValidationCheck(stats); // PER CAPITOLO 7. VALIDAZIONE
				System.out.println("\n... Esecuzione delle restanti " + (NUM_REPLICATIONS - 1) + " replicazioni in background ...");
			}
		}

		// REPORT MEDIA SU TUTTE LE RUN
		List<IntervalEstimationRow> populationIeRows = 
			IntervalEstimationRow.fromMapOfData(populationData);

		List<IntervalEstimationRow> timeIeRows =
			IntervalEstimationRow.fromMapOfData(timeData);

		writeCsvs(populationIeRows, timeIeRows, runsSamples);

		printFinalScientificResults(populationIeRows, timeIeRows);
	}

	private void printExperimentHeader() {
		System.out.println("\n");
		System.out.println("||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");
		System.out.println("||   AVVIO ESPERIMENTO DI SIMULAZIONE A ORIZZONTE FINITO          ||");
		System.out.printf( "||   Replicazioni: %-3d                                            ||\n", NUM_REPLICATIONS);
		System.out.printf( "||   Durata singola run: %-10.0f secondi                       ||\n", simulationTime);
		System.out.println("||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||\n");
	}

	// Per la prima replica stampiamo sia statistiche time e jov avg sia il sampling
	private void printPilotRunDiagnostic(StatCollector stats, List<Sample> samples) {
		System.out.println("\n");
		System.out.println("####################################################################");
		System.out.println("#  SEZIONE 1: DIAGNOSTICA REPLICAZIONE PILOTA (RUN #1 di 64)       #");
		System.out.println("####################################################################");

		System.out.println("\nA. Medie della Singola Replica ");
		StatLogger.printReport(stats);

		System.out.println("\nB. Campionamento Temporale ");
		System.out.println("");
		int totalSamples = samples.size();
		System.out.printf("Campioni raccolti: %d (Intervallo: %.2f s)\n", totalSamples, samplingInterval);

		if (totalSamples > 0) {
			System.out.println("\nTime       | Center               | Metric                    | Value");
			System.out.println("-----------+----------------------+---------------------------+----------");
			for (int j = 0; j < NUM_SHOWN_PILOT_SAMPLES; j++) {
				Sample s = samples.get(j);
				System.out.printf("%-10.2f | %-20s | %-25s | %.4f\n",
						s.getTimestamp(), s.getCenterName(), s.getMetric(), s.getValue());
			}
			System.out.println("-----------+----------------------+---------------------------+----------");
		} else {
			System.out.println("(Nessun campione raccolto)");
		}
		System.out.println("####################################################################");
	}

	// Stampa il report scientifico finale (Intervalli di confidenza)
	private void printFinalScientificResults(
			List<IntervalEstimationRow> populationIeRows, 
			List<IntervalEstimationRow> timeIeRows) {

		System.out.println("\n\n");
		System.out.println("####################################################################");
		System.out.println("#  SEZIONE 2: RISULTATI SCIENTIFICI FINALI (SU 64 REPLICAZIONI)    #");
		System.out.println("####################################################################");

		System.out.println("\n>>> Statistiche Job Based <<<\n");
		System.out.println("Metrica                        | Media Stimata | Intervallo (95%) | Range Confidenza [Min ... Max]");
		System.out.println("-------------------------------+---------------+------------------+---------------------------------");

		for(final IntervalEstimationRow row : populationIeRows) {
			System.out.println(row);
		}

		System.out.println("\n\n>>> Statistiche Time Based <<<\n");
		System.out.println("Metrica                        | Media Stimata | Intervallo (95%) | Range Confidenza [Min ... Max]");
		System.out.println("-------------------------------+---------------+------------------+---------------------------------");

		for(final IntervalEstimationRow row : timeIeRows) {
			System.out.println(row);
		}

		System.out.println("\n");
	}

	private void writeCsvs(
			List<IntervalEstimationRow> populationIeRows, 
			List<IntervalEstimationRow> timeIeRows, 
			Map<Integer, List<Sample>> runsSamples) {

		String baseDir = "output/" + experimentName;

		try {
			CsvWriter.writeAll(
					baseDir + "/population-ie.csv",
					IntervalEstimationRow.class, 
					populationIeRows);

			CsvWriter.writeAll(
					baseDir + "/time-ie.csv",
					IntervalEstimationRow.class, 
					timeIeRows);
		} catch(CsvWriterException | IOException e) {
			System.err.println("ignoring, not critical...");
			e.printStackTrace();
		}

		for(final Integer runKey : runsSamples.keySet()) {
			try {
				CsvWriter.writeAll(
						String.format("%s/runs-samples/run-%d/sample.csv", baseDir, runKey),
						Sample.class,
						runsSamples.get(runKey));
			} catch(CsvWriterException | IOException e) {
				System.err.println("cannot ignore this");
				throw new RuntimeException(e);
			}
		}
	}

	// NUOVO METODO PER IL CAPITOLO 7 (VALIDAZIONE)
	private void printValidationCheck(StatCollector stats) {
		System.out.println("\n");
		System.out.println("####################################################################");
		System.out.println("#  SEZIONE 1-BIS: VALIDAZIONE PROBABILITA' E FLUSSI (RUN #1)       #");
		System.out.println("####################################################################");

		// 1. Recupero la mappa delle statistiche "Population" (dove ci sono i conteggi)
		var popStats = stats.getPopulationStats();

		// 2. Recupero i conteggi in modo sicuro (controllando se la chiave esiste)

		long totalProcessed = 0;
		if (popStats.containsKey("SystemResponseTime_Success")) {
			totalProcessed = popStats.get("SystemResponseTime_Success").getCount();
		}

		long passCheckIn = 0;
		if (popStats.containsKey("Ts_CheckIn")) {
			passCheckIn = popStats.get("Ts_CheckIn").getCount();
		}

		long passXRay = 0;
		if (popStats.containsKey("Ts_XRay")) {
			passXRay = popStats.get("Ts_XRay").getCount();
		}

		long passTrace = 0;
		if (popStats.containsKey("Ts_TraceDetection")) {
			passTrace = popStats.get("Ts_TraceDetection").getCount();
		}

		// 3. Calcolo Percentuali
		double pDeskSimulata = 0.0;
		if (totalProcessed > 0) {
			// Calcolo percentuale su totale uscite
			pDeskSimulata = (double) passCheckIn / totalProcessed * 100.0;
		}

		double pCheckSimulata = 0.0;
		if (passXRay > 0) {
			// Calcolo percentuale su transiti XRay
			pCheckSimulata = (double) passTrace / passXRay * 100.0;
		}

		// 4. Stampa Report
		System.out.printf("Totale Passeggeri Processati (OUT): %d\n", totalProcessed);
		System.out.println("--------------------------------------------------------------------");

		System.out.println(">>> CHECK 1: Probabilità Check-In (Target: ~38.7%)");
		System.out.printf("    Transiti Check-In:    %d\n", passCheckIn);
		System.out.printf("    Percentuale Simulata: %.4f%%\n", pDeskSimulata);

		System.out.println("\n>>> CHECK 2: Probabilità Trace Detection (Target: ~10.0%)");
		System.out.printf("    Transiti X-Ray:       %d\n", passXRay);
		System.out.printf("    Transiti Trace Det.:  %d\n", passTrace);
		System.out.printf("    Percentuale Simulata: %.4f%%\n", pCheckSimulata);

		System.out.println("####################################################################");

		if (popStats.containsKey("Ts_FastTrack") && popStats.containsKey("Ts_Varchi")) {

			System.out.println("\n--------------------------------------------------------------------");
			System.out.println(">>> CHECK 3: Split Fast Track vs X-Ray Standard (SCENARIO IMPROVED)");

			long passVarchi = popStats.get("Ts_Varchi").getCount();
			long passFastTrack = popStats.get("Ts_FastTrack").getCount();
			// Nota: passXRay l'abbiamo già recuperato sopra

			double pFastSimulata = (passVarchi > 0) ? (double) passFastTrack / passVarchi * 100.0 : 0.0;
			double pStandardSimulata = (passVarchi > 0) ? (double) passXRay / passVarchi * 100.0 : 0.0;

			System.out.printf("    Totale Uscita Varchi: %d\n", passVarchi);
			System.out.println("    ------------------------------------------");

			System.out.printf("    [FAST TRACK] Target: ~33.0%% | Simulato: %.4f%% (Pax: %d)\n", pFastSimulata, passFastTrack);
			System.out.printf("    [STANDARD]   Target: ~67.0%% | Simulato: %.4f%% (Pax: %d)\n", pStandardSimulata, passXRay);
		}

		System.out.println("####################################################################");
	}
}

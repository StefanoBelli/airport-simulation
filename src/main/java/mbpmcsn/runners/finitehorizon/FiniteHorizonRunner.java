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
}

package mbpmcsn.runners.steadystate;

import java.util.List;
import java.util.Map;
import java.io.IOException;
import mbpmcsn.runners.Runner;
import mbpmcsn.runners.smbuilders.SimulationModelBuilder;
import mbpmcsn.desbook.Rngs;
import mbpmcsn.core.Constants;
import mbpmcsn.stats.batchmeans.BatchMathUtils;
import mbpmcsn.stats.batchmeans.BatchRow;
import mbpmcsn.stats.ie.IntervalEstimationRow;
import mbpmcsn.csv.CsvWriter;
import mbpmcsn.csv.CsvWriterException;

/**
 * infinite horizon simulation to estimate stable performance measures
 * we have to discard the initial warm-up period, use the Batch Means
 * an estimate confidence intervals
 */

public final class SteadyStateRunner implements Runner {
	private final String experimentName;
	private final VeryLongRun veryLongRun;

	public SteadyStateRunner(
			String experimentName,
			SimulationModelBuilder builder,
			boolean approxServicesAsExp,
			double arrivalsMeanTime,
			double timeWarmup) {

		this.experimentName = experimentName;

		Rngs rngs = new Rngs();
		rngs.plantSeeds(Constants.SEED);

		veryLongRun = new VeryLongRun(
				builder, 
				rngs, 
				approxServicesAsExp, 
				arrivalsMeanTime,
				timeWarmup);
	}

	@Override
	public void runIt() {
		printExperimentHeader();
		veryLongRun.run();

		Map<String, List<Double>> batchMeans = veryLongRun.getBatchCollector().getBatchMeans();

		/*for(final String sKey : batchMeans.keySet()) {
			System.out.println(sKey+ " - num batches: " + batchMeans.get(sKey).size());
			System.out.println("ac: " + BatchMathUtils.computeAutocorrelation(batchMeans.get(sKey)));
			for(final Double val : batchMeans.get(sKey)) {
				System.out.println(sKey + ": " + val);
			}
		}
		System.out.println(""); */

		List<BatchRow> batchRows = BatchRow.fromMapOfData(batchMeans);

		List<IntervalEstimationRow> ierows =
				IntervalEstimationRow.fromMapOfData(batchMeans, true);

		System.out.println("Risultati Steady State (Media, Intervalli 95%, Autocorrelazione):");
		System.out.println("-------------------------------------------------------------------------------------------------------------------");
		for(final IntervalEstimationRow ierow : ierows) {
			System.out.println(ierow);
		}

		String outputDir = "output/" + experimentName;
		try {
			System.out.println("\n[INFO] Scrittura CSV in corso in: " + outputDir);

			CsvWriter.writeAll(outputDir + "/batch_data.csv", BatchRow.class, batchRows);

			CsvWriter.writeAll(outputDir + "/results_summary.csv", IntervalEstimationRow.class, ierows);

			System.out.println("[OK] File scritti correttamente.");

		} catch (CsvWriterException | IOException e) {
			System.err.println("[ERRORE] Scrittura CSV fallita: " + e.getMessage());
		}

	}

	private void printExperimentHeader() {
		System.out.println("\n");
		System.out.println("||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");
		System.out.println("||   AVVIO ESPERIMENTO DI SIMULAZIONE A ORIZZONTE INFINITO          ||");
		System.out.printf( "||   BatchMeans params: (b=%d,k=%d)                       ||\n",
				Constants.BATCH_SIZE, Constants.NUM_BATCHES);
		System.out.println("||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||\n");
	}

}

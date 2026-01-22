package mbpmcsn.runners.verification;

import mbpmcsn.core.Constants;
import mbpmcsn.csv.CsvWriter;
import mbpmcsn.csv.CsvWriterException;
import mbpmcsn.desbook.Rngs;
import mbpmcsn.runners.Runner;
import mbpmcsn.runners.smbuilders.SimulationModelBuilder;
import mbpmcsn.runners.steadystate.VeryLongRun;
import mbpmcsn.stats.batchmeans.BatchCollector;
import mbpmcsn.center.Center.KeyStatPrefix;
import mbpmcsn.stats.ie.IntervalEstimationRow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VerificationRunner implements Runner {

	private final String experimentName;
	private final SimulationModelBuilder builder;
	private final Rngs rngs;
	private final double arrivalsMeanTime;

	private final List<VerificationResultRow> results = new ArrayList<>();

	public VerificationRunner(
			String experimentName,
			SimulationModelBuilder builder, 
			double arrivalsMeanTime) {

		this.experimentName = experimentName;
		this.builder = builder;
		this.arrivalsMeanTime = arrivalsMeanTime;
		this.rngs = new Rngs();
		this.rngs.plantSeeds(Constants.SEED);
	}

	@Override
	public void runIt() {
		System.out.println("===================================================================");
		System.out.println("   VERIFICATION RUNNER - Analisi M/M/k");
		System.out.println("   [Ipotesi]: Arrivi Poissoniani, Servizi Esponenziali, t -> inf");
		System.out.println("===================================================================");

		// 1. ESECUZIONE SIMULAZIONE, to be changed with steady state, batch means technique!!!
		VeryLongRun run = new VeryLongRun(
				builder,
				rngs,
				true,  // Attiva M/M/k
				arrivalsMeanTime,
				Constants.TIME_WARMUP
		);

		System.out.println(">>> Avvio simulazione Steady State per verifica...");
		run.run();

		BatchCollector batchCollector = run.getBatchCollector();

		// 2. CONFRONTO ANALITICO
		double lambdaTot = 1 / arrivalsMeanTime;

		results.clear();

		System.out.println("\n--- RISULTATI VERIFICA ---");
		System.out.printf("Lambda Totale (Input): %.4f pax/sec\n", lambdaTot);

		// --- VERIFICA CENTRO 1: Check-In (M/M/k) ---
		double lambdaCheckIn = lambdaTot * Constants.P_DESK;
		verifyMMkNode("CheckIn", batchCollector, lambdaCheckIn, Constants.M1, Constants.MEAN_S1, "M/M/" + Constants.M1);

		// --- VERIFICA CENTRO 2: Varchi (M/M/k) ---
		verifyMMkNode("Varchi", batchCollector, lambdaTot, Constants.M2, Constants.MEAN_S2, "M/M/" + Constants.M2);

		// --- VERIFICA CENTRO 3: XRay (M/M/k approssimato) ---
		verifyMMkNode("XRay", batchCollector, lambdaTot, Constants.M3, Constants.MEAN_S3, "M/M/" + Constants.M3);

		// --- VERIFICA CENTRO 4: Trace Detection (M/M/k) ---
		double lambdaTrace = lambdaTot * Constants.P_CHECK;
		verifyMMkNode("TraceDetection", batchCollector, lambdaTrace, Constants.M4, Constants.MEAN_S4, "M/M/" + Constants.M4);

		// --- VERIFICA CENTRO 5: Recupero (M/M/inf) ---
		verifyInfiniteServer("Recupero", lambdaTot, batchCollector, Constants.MEAN_S5);

		saveVerificationReport();
	}

	/*
	 * Verifica per nodi M/M/k (Single Queue o Multi Queue approssimata)
	 */
	private void verifyMMkNode(String name, BatchCollector collector, double lambda, int k, double meanService, String modelName) {
		double mu = 1.0 / meanService;
		double rho = lambda / (k * mu);

		System.out.printf("\n>>> Centro: %-15s [M/M/%d] (Erlang-C)\n", name, k);

		if (checkInstability(rho)) {
			return;
		}

		// Calcolo P0
		double sum = 0.0;
		double a = lambda / mu;

		for (int n = 0; n < k; n++) {
			sum += Math.pow(a, n) / factorial(n);
		}

		double termK = (Math.pow(a, k) / factorial(k)) * (1.0 / (1.0 - rho));
		double p0 = 1.0 / (sum + termK);

		// Erlang-C: Probabilità di attesa in coda
		double pq = (Math.pow(a, k) * p0) / (factorial(k) * (1.0 - rho));

		// Tempi medi
		double E_Tq = pq / (k * mu - lambda);
		double E_Ts = E_Tq + meanService;
		double E_Nq = lambda * E_Tq;
		double E_Ns = lambda * E_Ts;

		List<IntervalEstimationRow> ierows = IntervalEstimationRow.fromMapOfData(collector.getBatchMeans());
		compareAndRecord(KeyStatPrefix.TSYSTEM, name, modelName, ierows, E_Ts);
		compareAndRecord(KeyStatPrefix.TQUEUE, name, modelName, ierows, E_Tq);
		compareAndRecord(KeyStatPrefix.NSYSTEM, name, modelName, ierows, E_Ns);
		compareAndRecord(KeyStatPrefix.NQUEUE, name, modelName, ierows, E_Nq);
		compareAndRecord(KeyStatPrefix.UTILIZATION, name, modelName, ierows, rho);
	}

	/**
	 * Caso: k * M/M/1 (Multi-Coda con Round Robin)
	 * Formula: M/M/1 su flusso diviso
	 */
	private void verifyIndependentMM1(String name, BatchCollector collector, double lambdaTot, int k, double meanService) {
		String modelName = "k*M/M/1 (RoundRobin)";
		System.out.printf("\n>>> Centro: %-15s [k * M/M/1] (Round Robin)\n", name);

		// Dividiamo il flusso, ogni server riceve lambda/k
		double lambdaSingle = lambdaTot / k;
		double mu = 1.0 / meanService;

		// Utilizzo del singolo server (uguale all'utilizzo globale)
		double rho = lambdaSingle / mu;

		if (checkInstability(rho)) {
			return;
		}

		// Formula esatta M/M/1 per il Tempo di Risposta (Wait + Service)
		double E_Ts = 1.0 / (mu - lambdaSingle);
		double E_Tq = (rho * meanService) / (1 - rho);
		double E_Nq = lambdaSingle * E_Tq;
		double E_Ns = lambdaSingle * E_Ts;

		List<IntervalEstimationRow> ierows = IntervalEstimationRow.fromMapOfData(collector.getBatchMeans());
		compareAndRecord(KeyStatPrefix.TSYSTEM, name, modelName, ierows, E_Ts);
		compareAndRecord(KeyStatPrefix.TQUEUE, name, modelName, ierows, E_Tq);
		compareAndRecord(KeyStatPrefix.NSYSTEM, name, modelName, ierows, E_Ns);
		compareAndRecord(KeyStatPrefix.NQUEUE, name, modelName, ierows, E_Nq);
		compareAndRecord(KeyStatPrefix.UTILIZATION, name, modelName, ierows, rho);
	}

	/**
	 * Verifica specifica per nodi M/M/infinito (Infinite Server)
	 * In questi nodi non esiste coda (Tq = 0), quindi Ts = S
	 */
	private void verifyInfiniteServer(String name, double lambda, BatchCollector collector, double meanService) {
		String modelName = "M/M/inf";
		System.out.printf("\n>>> Centro: %-15s [M/M/inf] (Delay)\n", name);

		// Non c'è stabilità da controllare (rho è sempre 0 per definizione)
		// Il tempo di risposta è puramente il tempo di servizio

		double E_Ts = meanService;
		double E_Ns = lambda * E_Ts;

		List<IntervalEstimationRow> ierows = IntervalEstimationRow.fromMapOfData(collector.getBatchMeans());
		compareAndRecord(KeyStatPrefix.TSYSTEM, name, modelName, ierows, E_Ts);
		compareAndRecord(KeyStatPrefix.NSYSTEM, name, modelName, ierows, E_Ns);
	}

	private boolean checkInstability(double rho) {
		if (rho >= 1.0) {
			System.out.printf("    [ERRORE CRITICO] Sistema Instabile (Rho = %.4f >= 1.0).\n", rho);
			System.out.println("    La teoria prevede code infinite. Impossibile verificare.");
			return true;
		}

		return false;
	}

	private boolean checkIfWithinInterval(IntervalEstimationRow ierow, double val) {
		return ierow.getMin() <= val && val <= ierow.getMax();
	}

	private void compareAndRecord(
			KeyStatPrefix keyStatPrefix,
			String name,
			String modelName,
			List<IntervalEstimationRow> ierows,
			double expectedVal) {

		IntervalEstimationRow ie = null;

		for(final IntervalEstimationRow ierow : ierows) {
			if(ierow.getMetric().equals(keyStatPrefix + name)) {
				ie = ierow;
				break;
			}
		}

		if(ie == null) {
			throw new IllegalArgumentException(
					"cannot find an interval estimation result for: " + 
					keyStatPrefix + name);
		}

		boolean withinInterval = checkIfWithinInterval(ie, expectedVal);

		System.out.printf(
				"    %s | SimMean: %.4f | SimMin: %.4f | " +
				"SimMax: %.4f | SimWidth: %.20f | TheoMean: %.4f => %s\n",
				keyStatPrefix.getPrettyName(), ie.getMean(), 
				ie.getMin(), ie.getMax(), ie.getWidth(), 
				expectedVal, 
				withinInterval ? "is within interval" : "is NOT within interval");

		results.add(new VerificationResultRow(
					name, keyStatPrefix.getPrettyName(), 
					modelName, ie.getMean(), ie.getMin(), 
					ie.getMax(), ie.getWidth(), expectedVal, 
					withinInterval));
	}

	private void saveVerificationReport() {
		String path = "output/" + experimentName + "/verification_report.csv";
		try {
			System.out.println("\n[INFO] Salvataggio report verifica in: " + path);
			CsvWriter.writeAll(path, VerificationResultRow.class, results);
			System.out.println("[OK] File salvato correttamente.");
		} catch (CsvWriterException | IOException e) {
			System.err.println("[ERRORE] Impossibile salvare il report: " + e.getMessage());
		}
	}

	// Helper matematico per Erlang-C
	private double factorial(int n) {
		if (n == 0) {
			return 1.0;
		}

		double fact = 1.0;
		for (int i = 1; i <= n; i++) {
			fact *= i;
		}

		return fact;
	}
}

package mbpmcsn.runners;

import mbpmcsn.core.Constants;
import mbpmcsn.desbook.Rngs;
import mbpmcsn.runners.finitehorizon.SingleReplication;
import mbpmcsn.runners.smbuilders.SimulationModelBuilder;
import mbpmcsn.stats.accumulating.StatCollector;

public class VerificationRunner implements Runner {

	private final SimulationModelBuilder builder;
	private final double simulationTime;
	private final Rngs rngs;

	public VerificationRunner(SimulationModelBuilder builder, double simulationTime) {
		this.builder = builder;
		this.simulationTime = simulationTime;
		this.rngs = new Rngs();
		this.rngs.plantSeeds(Constants.SEED);
	}

	@Override
	public void runIt() {
		System.out.println("===================================================================");
		System.out.println("   VERIFICATION RUNNER - Analisi M/M/k");
		System.out.println("   [Ipotesi]: Arrivi Poissoniani, Servizi Esponenziali, t -> inf");
		System.out.println("===================================================================");

		// 1. ESECUZIONE SIMULAZIONE
		SingleReplication run = new SingleReplication(
				builder,
				rngs,
				simulationTime,
				true,  // Attiva M/M/k
				0.0    // Sampling disabilitato
		);

		System.out.println(">>> Avvio simulazione...");
		run.runReplication();

		StatCollector stats = run.getStatCollector();

		// 2. CONFRONTO ANALITICO
		double lambdaTot = Constants.ARRIVAL_MED_RATE;

		System.out.println("\n--- RISULTATI VERIFICA ---");
		System.out.printf("Lambda Totale (Input): %.4f pax/sec\n", lambdaTot);

		// --- VERIFICA CENTRO 1: Check-In (M/M/k) ---
		double lambdaCheckIn = lambdaTot * Constants.P_DESK;
		verifyMMkNode("CheckIn", stats, lambdaCheckIn, Constants.M1, Constants.MEAN_S1);

		// --- VERIFICA CENTRO 2: Varchi (M/M/k) ---
		// Tutti i passeggeri (sia diretti che da check-in) arrivano qui
		// Lambda_Varchi = Lambda_Tot
		// Approssimazione: SQF permette di considerarlo come M/M/k
		verifyMMkNode("Varchi", stats, lambdaTot, Constants.M2, Constants.MEAN_S2);

		// --- VERIFICA CENTRO 3: Preparazione (M/M/inf) ---
		// Infinite Server, non c'è coda
		// Il tempo di risposta deve coincidere col tempo di servizio
		verifyInfiniteServer("Preparazione", stats, Constants.MEAN_S3);

		// --- VERIFICA CENTRO 4: XRay (M/M/k) ---
		// Qui il Round Robin spezza il flusso in k flussi indipendenti
		// NON usiamo Erlang-C, ma la formula M/M/1 sul singolo server
		// La media globale accumulata dal simulatore deve coincidere con la media del singolo M/M/1
		verifyIndependentMM1("XRay", stats, lambdaTot, Constants.M4, Constants.MEAN_S4);

		// --- VERIFICA CENTRO 5: Trace Detection (M/M/k) ---
		double lambdaTrace = lambdaTot * Constants.P_CHECK;
		verifyMMkNode("TraceDetection", stats, lambdaTrace, Constants.M5, Constants.MEAN_S5);

		// --- VERIFICA CENTRO 6: Recupero (M/M/inf) ---
		verifyInfiniteServer("Recupero", stats, Constants.MEAN_S6);
	}

	/*
	 * Verifica per nodi M/M/k (Single Queue o Multi Queue approssimata)
	 */
	private void verifyMMkNode(String name, StatCollector stats, double lambda, int k, double meanService) {
		double mu = 1.0 / meanService;
		double rho = lambda / (k * mu);

		System.out.printf("\n>>> Centro: %-15s [M/M/%d] (Erlang-C)\n", name, k);

		if (checkInstability(rho)) return;

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
		double E_Ts_Theor = E_Tq + meanService;

		compareAndPrint(name, stats, rho, E_Ts_Theor);
	}

	/**
	 * Caso: k * M/M/1 (Multi-Coda con Round Robin)
	 * Formula: M/M/1 su flusso diviso
	 */
	private void verifyIndependentMM1(String name, StatCollector stats, double lambdaTot, int k, double meanService) {
		System.out.printf("\n>>> Centro: %-15s [k * M/M/1] (Round Robin)\n", name);

		// Dividiamo il flusso, ogni server riceve lambda/k
		double lambdaSingle = lambdaTot / k;
		double mu = 1.0 / meanService;

		// Utilizzo del singolo server (uguale all'utilizzo globale)
		double rho = lambdaSingle / mu;

		if (checkInstability(rho)) return;

		// Formula esatta M/M/1 per il Tempo di Risposta (Wait + Service)
		// E[Ts] = 1 / (mu - lambda)
		double E_Ts_Theor = 1.0 / (mu - lambdaSingle);

		compareAndPrint(name, stats, rho, E_Ts_Theor);
	}

	/**
	 * Verifica specifica per nodi M/M/infinito (Infinite Server)
	 * In questi nodi non esiste coda (Tq = 0), quindi Ts = S
	 */
	private void verifyInfiniteServer(String name, StatCollector stats, double meanService) {
		System.out.printf("\n>>> Centro: %-15s [M/M/inf] (Delay)\n", name);

		// Non c'è stabilità da controllare (rho è sempre 0 per definizione)
		// Il tempo di risposta è puramente il tempo di servizio
		compareAndPrint(name, stats, 0.0, meanService);
	}

	private boolean checkInstability(double rho) {
		if (rho >= 1.0) {
			System.out.printf("    [ERRORE CRITICO] Sistema Instabile (Rho = %.4f >= 1.0).\n", rho);
			System.out.println("    La teoria prevede code infinite. Impossibile verificare.");
			return true;
		}
		return false;
	}

	private void compareAndPrint(String name, StatCollector stats, double rho, double expectedVal) {
		// Recupero il valore simulato (Media Globale accumulata)
		double simulatedVal = stats.getPopulationMean("Ts_" + name);

		double error = Math.abs(simulatedVal - expectedVal) / expectedVal * 100.0;

		System.out.printf("    Utilizzo (Rho)   : %.4f\n", rho);
		System.out.printf("    E[Ts] Teorico    : %.4f s\n", expectedVal);
		System.out.printf("    E[Ts] Simulato   : %.4f s\n", simulatedVal);

		if (error < 5.0) {
			System.out.printf("    [OK] Verifica Superata (Errore: %.2f%%)\n", error);
		} else {
			System.out.printf("    [WARNING] Errore alto (%.2f%%). Aumentare durata?\n", error);
		}
	}

	// Helper matematico per Erlang-C
	private double factorial(int n) {
		if (n == 0) return 1.0;
		double fact = 1.0;
		for (int i = 1; i <= n; i++) fact *= i;
		return fact;
	}
}
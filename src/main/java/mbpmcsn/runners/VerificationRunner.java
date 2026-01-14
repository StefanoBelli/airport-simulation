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
		double lambdaTot = Constants.ARRIVAL_PEAK_RATE;

		System.out.println("\n--- RISULTATI VERIFICA ---");
		System.out.printf("Lambda Totale (Input): %.4f pax/sec\n", lambdaTot);

		// --- VERIFICA CENTRO 1: Check-In (M/M/k) ---
		double lambdaCheckIn = lambdaTot * Constants.P_DESK;
		verifyMMkNode("CheckIn", stats, lambdaCheckIn, Constants.M1, Constants.MEAN_S1);

		// --- VERIFICA CENTRO 2: Varchi (M/M/k) ---
		// Tutti i passeggeri (sia diretti che da check-in) arrivano qui
		// Lambda_Varchi = Lambda_Tot
		verifyMMkNode("Varchi", stats, lambdaTot, Constants.M2, Constants.MEAN_S2);

		// --- VERIFICA CENTRO 3: Preparazione (M/M/inf) ---
		// Infinite Server, non c'è coda
		// Il tempo di risposta deve coincidere col tempo di servizio
		verifyInfiniteServer("Preparazione", stats, Constants.MEAN_S3);

		// --- VERIFICA CENTRO 4: XRay (M/M/k) ---
		// Anche qui arriva tutto il flusso
		verifyMMkNode("XRay", stats, lambdaTot, Constants.M4, Constants.MEAN_S4);

		// --- VERIFICA CENTRO 5: Trace Detection (M/M/k) ---
		double lambdaTrace = lambdaTot * Constants.P_CHECK;
		verifyMMkNode("TraceDetection", stats, lambdaTrace, Constants.M5, Constants.MEAN_S5);

		// --- VERIFICA CENTRO 6: Recupero (M/M/inf) ---
		// Infinite Server
		verifyInfiniteServer("Recupero", stats, Constants.MEAN_S6);
	}

	/*
	 * Verifica per nodi M/M/k (Single Queue o Multi Queue approssimata)
	 */
	private void verifyMMkNode(String name, StatCollector stats, double lambda, int k, double meanService) {
		double mu = 1.0 / meanService;  // Tasso di servizio (pax/sec)
		double rho = lambda / (k * mu); // Utilizzazione

		System.out.printf("\n>>> Centro: %-15s [M/M/%d] (Lambda=%.4f)\n", name, k, lambda);

		// Controllo Stabilità
		if (rho >= 1.0) {
			System.out.printf("    [ERRORE] Sistema instabile (Rho = %.4f >= 1).\n", rho);
			return;
		}

		// --- CALCOLO TEORICO ---

		// 1. Calcolo P0 (Probabilità sistema vuoto)
		double sumP0 = 0.0;
		double a = lambda / mu;

		for (int n = 0; n < k; n++) {
			sumP0 += Math.pow(a, n) / factorial(n);
		}
		double termK = (Math.pow(a, k) / factorial(k)) * (1.0 / (1.0 - rho));
		double p0 = 1.0 / (sumP0 + termK);

		// 2. Formula di Erlang-C: PQ = Probabilità di trovare tutti i server occupati
		double probabilityWait = (Math.pow(a, k) * p0) / (factorial(k) * (1.0 - rho));

		// 3. Tempi Medi Attesi
		double E_Tq_Theor = probabilityWait * (1.0 / (k * mu - lambda)); // Tempo medio in Coda (Wait)
		double E_Ts_Theor = E_Tq_Theor + meanService;                    // Tempo medio nel Sistema (Response)

		// --- DATO SIMULATO ---

		// Recuperiamo "Ts_NomeCentro" (Tempo di Risposta Medio Population-Based)
		double E_Ts_Sim = stats.getPopulationMean("Ts_" + name);

		// --- CALCOLO ERRORE RELATIVO ---
		double errorPerc = Math.abs(E_Ts_Sim - E_Ts_Theor) / E_Ts_Theor * 100.0;

		System.out.printf("    Utilizzo (Rho)   : %.4f\n", rho);
		System.out.printf("    E[Ts] Teorico    : %.4f s  (Tq=%.2f + S=%.2f)\n", E_Ts_Theor, E_Tq_Theor, meanService);
		System.out.printf("    E[Ts] Simulato   : %.4f s\n", E_Ts_Sim);

		checkError(errorPerc);
	}

	/**
	 * Verifica specifica per nodi M/M/infinito (Infinite Server)
	 * In questi nodi non esiste coda (Tq = 0), quindi Ts = S
	 */
	private void verifyInfiniteServer(String name, StatCollector stats, double meanService) {
		System.out.printf("\n>>> Centro: %-15s [M/M/inf] (Delay Center)\n", name);

		double E_Ts_Theor = meanService;
		double E_Ts_Sim = stats.getPopulationMean("Ts_" + name);

		double errorPerc = Math.abs(E_Ts_Sim - E_Ts_Theor) / E_Ts_Theor * 100.0;

		System.out.printf("    E[Ts] Teorico    : %.4f s (Solo servizio)\n", E_Ts_Theor);
		System.out.printf("    E[Ts] Simulato   : %.4f s\n", E_Ts_Sim);

		checkError(errorPerc);
	}

	private void checkError(double errorPerc) {
		if (errorPerc < 5.0) {
			System.out.printf("    [OK] VALIDAZIONE SUPERATA (Errore: %.2f%%)\n", errorPerc);
		} else {
			System.out.printf("    [FAIL] ERRORE ECCESSIVO (Errore: %.2f%%). Aumentare durata simulazione?\n", errorPerc);
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
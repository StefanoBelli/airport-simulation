package mbpmcsn;

import mbpmcsn.core.Constants;
import mbpmcsn.runners.verification.VerificationRunner;
import mbpmcsn.runners.finitehorizon.FiniteHorizonRunner;
import mbpmcsn.runners.steadystate.SteadyStateRunner;
import mbpmcsn.runners.Runner;
import mbpmcsn.runners.smbuilders.BaseSimulationModelBuilder;

import java.util.InputMismatchException;
import java.util.Scanner;

public class App {

	public static void main(String[] args) {
		System.out.println("==============================================");
		System.out.println("   AIRPORT SECURITY SIMULATION - PMCSN");
		System.out.println("==============================================");

		runModeSelector();
	}

	private static void runModeSelector() {

		Scanner scanner = new Scanner(System.in);
		boolean exit = false;

		while (!exit) {
			System.out.println("\nSeleziona la modalità di simulazione (Runner):");
			System.out.println("----------------------------------------------");
			System.out.println("1. FINITE HORIZON (Replicazioni Indipendenti)");

			System.out.println("2. INFINITE HORIZON (Batch Means, double med mean time)");

			System.out.println("3. VERIFICATION (double med mean time)");

			System.out.println("4. TRANSIENT ANALYSIS (double med mean time)");

			System.out.println("5. TRANSIENT ANALYSIS");

			System.out.println("6. Esci");
			System.out.println("----------------------------------------------");
			System.out.print("> Scelta: ");

			try {
				int choice = scanner.nextInt();
				scanner.nextLine(); // Consuma newline

				Runner runner = null;

				switch (choice) {
					case 1:
						System.out.println("\n[INFO] Avvio Finite Horizon Experiment...");
						runner = new FiniteHorizonRunner(
								"finite-horizon-workday-base-medmeantime",
								new BaseSimulationModelBuilder(),
								Constants.WORK_DAY,
								false,
								Constants.ARRIVAL_MED_MEAN_TIME,
								Constants.FINITE_HORIZON_SAMPLING_INTERVAL);
						break;

					case 2:
						System.out.println("\n[INFO] Avvio Infinite Horizon Experiment (Batch Means, double med mean time)...");
						runner = new SteadyStateRunner(
								"steady-state-base-doublemedmeantime",
								new BaseSimulationModelBuilder(),
								false,
								Constants.ARRIVAL_MED_MEAN_TIME * 2,
								Constants.TIME_WARMUP);
						break;

					case 3:
						System.out.println("\n[INFO] Avvio Verification (M/M/k vs Simulation)...");
						System.out.println("[INFO] Nota: La durata è determinata dal raggiungimento dei Batch (k=96, double med mean time).");
						runner = new VerificationRunner(
								"verification-base-doublemedmeantime",
								new BaseSimulationModelBuilder(),
								Constants.ARRIVAL_MED_MEAN_TIME * 2
						);
						break;

					case 4:
						System.out.println("\n[INFO] Avvio Analisi del Transitorio (double med mean time)...");
						runner = new FiniteHorizonRunner(
								"transient-analysis-base-doublemedmeantime",
								new BaseSimulationModelBuilder(),
								Constants.TRANSIENT_DURATION,
								false,
								Constants.ARRIVAL_MED_MEAN_TIME * 2,
								Constants.TRANSIENT_SAMPLING_INTERVAL);
						break;

					case 5:
						System.out.println("\n[INFO] Avvio Analisi del Transitorio...");
						runner = new FiniteHorizonRunner(
								"transient-analysis-base-medmeantime",
								new BaseSimulationModelBuilder(),
								Constants.TRANSIENT_DURATION,
								false,
								Constants.ARRIVAL_MED_MEAN_TIME,
								Constants.TRANSIENT_SAMPLING_INTERVAL);
						break;

					case 6:
						System.out.println("Uscita.");
						exit = true;
						break;

					default:
						System.err.println("Opzione non valida.");
				}

				if (runner != null) {
					runner.runIt();
					System.out.println("\n[DONE] Simulazione completata. Premi INVIO per continuare...");
					scanner.nextLine();
				}

			}catch (InputMismatchException e) {
				System.err.println("Errore: Inserire un numero intero.");
				scanner.nextLine();
			} catch (Exception e) {
				System.err.println("Errore durante l'esecuzione: " + e.getMessage());
				e.printStackTrace();
			}
		}
		scanner.close();
	}
}


package mbpmcsn;

import mbpmcsn.runners.finitehorizon.FiniteHorizonRunner;
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

			System.out.println("2. INFINITE HORIZON (Batch Means)");

			System.out.println("3. VERIFICATION");

			//System.out.println("4. TRANSIENT ANALYSIS"); do we really need this?

			System.out.println("5. Esci");
			System.out.println("----------------------------------------------");
			System.out.print("> Scelta: ");

			try {
				int choice = scanner.nextInt();
				scanner.nextLine(); // Consuma newline

				Runner runner = null;

				switch (choice) {
					case 1:
						final int workDay = 4800; //seconds in 80 min
						final double samplingInterval = 100.00; //seconds
						System.out.println("\n[INFO] Avvio Finite Horizon Experiment...");
						runner = new FiniteHorizonRunner(new BaseSimulationModelBuilder(), workDay, false, samplingInterval);
						break;

					case 2:
						System.out.println("\n[INFO] Avvio Infinite Horizon Experiment (Batch Means)...");
						break;

					case 3:
						System.out.println("\n[INFO] Avvio Verification (M/M/k vs Simulation)...");
						break;

					/*case 4: do we really need this?
						System.out.println("\n[INFO] Avvio Analisi del Transitorio...");
						break;*/

					case 5:
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


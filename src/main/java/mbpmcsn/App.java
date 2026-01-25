package mbpmcsn;

import mbpmcsn.core.Constants;
import mbpmcsn.runners.smbuilders.ImprovedSimulationModelBuilder;
import mbpmcsn.runners.verification.VerificationRunner;
import mbpmcsn.runners.verification.ImprovedVerificationRunner;
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

        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.println("\n--- SELEZIONE SCENARIO ---");
            System.out.println("1. Scenario BASE");
            System.out.println("2. Scenario MIGLIORATIVO");
            System.out.println("3. Esci");
            System.out.print("> Scelta: ");

            try {
                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        runBaseExperiments(scanner);
                        break;
                    case 2:
                        runImprovedExperiments(scanner);
                        break;
                    case 3:
                        exit = true;
                        System.out.println("Uscita.");
                        break;
                    default:
                        System.err.println("Scelta non valida.");
                }

            } catch (InputMismatchException e) {
                System.err.println("Errore: Inserire un numero intero.");
                scanner.nextLine();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        scanner.close();
	}


	private static void runBaseExperiments(Scanner scanner) {

        boolean back = false;

		while (!back) {
			System.out.println("\nMENU SCENARIO BASE: Seleziona la modalità di simulazione:");
			System.out.println("----------------------------------------------");
			System.out.println("1. FINITE HORIZON (Independent Replications)");
			System.out.println("2. INFINITE HORIZON (Batch Means, double med mean time)");
			System.out.println("3. VERIFICATION (double med mean time)");
			System.out.println("4. VERIFICATION");
			System.out.println("5. TRANSIENT ANALYSIS (double med mean time)");
			System.out.println("6. TRANSIENT ANALYSIS");
			System.out.println("7. Indietro");
			System.out.println("----------------------------------------------");
			System.out.print("> Scelta Base: ");

			try {
				int choice = scanner.nextInt();
				scanner.nextLine();

				Runner runner = null;

				switch (choice) {
					case 1:
						System.out.println("\n[BASE] Avvio Finite Horizon Experiment...");
						runner = new FiniteHorizonRunner(
								"finite-horizon-workday-base-medMeanTime",
								new BaseSimulationModelBuilder(),
								Constants.WORK_DAY,
								false,
								Constants.ARRIVAL_MED_MEAN_TIME,
								Constants.FINITE_HORIZON_SAMPLING_INTERVAL);
						break;

					case 2:
						System.out.println("\n[BASE] Avvio Infinite Horizon Experiment (Batch Means, double med mean time)...");
						runner = new SteadyStateRunner(
								"steady-state-base-doubleMedMeantime",
								new BaseSimulationModelBuilder(),
								false,
								Constants.ARRIVAL_MED_MEAN_TIME * 2,
								Constants.TIME_WARMUP);
						break;

					case 3:
						System.out.println("\n[BASE] Avvio Verification (M/M/k vs Simulation)...");
						System.out.println("[INFO] Nota: La durata è determinata dal raggiungimento dei Batch (k=96, double med mean time).");
						runner = new VerificationRunner(
								"verification-base-doubleMedMeanTime",
								new BaseSimulationModelBuilder(),
								Constants.ARRIVAL_MED_MEAN_TIME * 2
						);
						break;

					case 4:
						System.out.println("\n[BASE] Avvio Verification (M/M/k vs Simulation)...");
						System.out.println("[INFO] Nota: La durata è determinata dal raggiungimento dei Batch (k=96, med mean time).");
						runner = new VerificationRunner(
								"verification-base-medMeanTime",
								new BaseSimulationModelBuilder(),
								Constants.ARRIVAL_MED_MEAN_TIME
						);
						break;

					case 5:
						System.out.println("\n[BASE] Avvio Analisi del Transitorio (double med mean time)...");
						runner = new FiniteHorizonRunner(
								"transient-analysis-base-doubleMedMeanTime",
								new BaseSimulationModelBuilder(),
								Constants.TRANSIENT_DURATION,
								false,
								Constants.ARRIVAL_MED_MEAN_TIME * 2,
								Constants.TRANSIENT_SAMPLING_INTERVAL);
						break;

					case 6:
						System.out.println("\n[BASE] Avvio Analisi del Transitorio...");
						runner = new FiniteHorizonRunner(
								"transient-analysis-base-medMeanTime",
								new BaseSimulationModelBuilder(),
								Constants.TRANSIENT_DURATION,
								false,
								Constants.ARRIVAL_MED_MEAN_TIME,
								Constants.TRANSIENT_SAMPLING_INTERVAL);
						break;

					case 7:
						System.out.println("Uscita.");
                        back = true;
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
	}

    private static void runImprovedExperiments(Scanner scanner) {
        boolean back = false;
        while (!back) {
            System.out.println("\nMENU SCENARIO MIGLIORATIVO: Seleziona la modalità di simulazione:");
            System.out.println("----------------------------------------------");
            System.out.println("1. FINITE HORIZON (Independent Replications)");
            System.out.println("2. INFINITE HORIZON (Batch Means)");
            System.out.println("3. VERIFICATION");
            System.out.println("4. TRANSIENT ANALYSIS");
            System.out.println("5. Indietro");
            System.out.println("----------------------------------------------");
            System.out.print("> Scelta Migliorativo: ");

            try {
                int choice = scanner.nextInt();
                scanner.nextLine();
                Runner runner = null;

                switch (choice) {
                    case 1:
                        System.out.println("\n[IMPROVED] Avvio Finite Horizon Experiment...");
                        runner = new FiniteHorizonRunner(
                                "finite-horizon-workday-improved-medMeanTime",
                                new ImprovedSimulationModelBuilder(),
                                Constants.WORK_DAY,
                                false,
                                Constants.ARRIVAL_MED_MEAN_TIME,
                                Constants.FINITE_HORIZON_SAMPLING_INTERVAL);
                        break;

                    case 2:
                        System.out.println("\n[IMPROVED] Avvio Infinite Horizon Experiment (Batch Means)...");
                        runner = new SteadyStateRunner(
                                "steady-state-improved-medMeantime",
                                new ImprovedSimulationModelBuilder(),
                                false,
                                Constants.ARRIVAL_MED_MEAN_TIME,
                                Constants.TIME_WARMUP);
                        break;

                    case 3:
                        System.out.println("\n[IMPROVED] Avvio Verification (M/M/k vs Simulation)...");
                        System.out.println("[INFO] Nota: La durata è determinata dal raggiungimento dei Batch (k=96, med mean time).");
                        runner = new ImprovedVerificationRunner(
                                "verification-improved-medMeanTime",
                                new ImprovedSimulationModelBuilder(),
                                Constants.ARRIVAL_MED_MEAN_TIME
                        );
                        break;

                    case 4:
                        System.out.println("\n[IMPROVED] Avvio Analisi del Transitorio...");
                        runner = new FiniteHorizonRunner(
                                "transient-analysis-improved-medMeanTime",
                                new ImprovedSimulationModelBuilder(),
                                Constants.TRANSIENT_DURATION,
                                false,
                                Constants.ARRIVAL_MED_MEAN_TIME,
                                Constants.TRANSIENT_SAMPLING_INTERVAL);
                        break;

                    case 5:
                        System.out.println("Uscita.");
                        back = true;
                        break;

                    default:
                        System.err.println("Opzione non valida.");
                }

                if (runner != null) {
                    runner.runIt();
                    System.out.println("\n[DONE] Premi INVIO per continuare...");
                    scanner.nextLine();
                }
            } catch (Exception e) {
                System.err.println("Errore: " + e.getMessage());
                scanner.nextLine();
            }
        }
    }
}


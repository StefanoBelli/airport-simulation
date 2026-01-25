import os

print("--- AVVIO GENERAZIONE GRAFICI ---")

# -----------------------------------------------------------
# Lancia questo script dalla cartella PRINCIPALE del progetto
# Comando: py scripts/make_plots.py
# -----------------------------------------------------------

# MIGLIORATIVO
os.system("py scripts/plot_result_finite_horizon.py output/finite-horizon-workday-improved-medMeanTime/runs-samples/run-0/sample.csv")
os.system("py scripts/table_results_finite_horizon.py output/finite-horizon-workday-improved-medMeanTime/population-ie.csv")
os.system("py scripts/table_results_finite_horizon.py output/finite-horizon-workday-improved-medMeanTime/time-ie.csv")
os.system("py scripts/plot_transient_analysis.py output/transient-analysis-improved-medMeanTime NormalLambda")
os.system("py scripts/plot_verification.py output/verification-improved-medMeanTime/verification_report.csv")
os.system("py scripts/plot_batches_steady_state.py output/steady-state-improved-medMeanTime/batch_data.csv")
os.system("py scripts/plot_summary_steady_state.py output/steady-state-improved-medMeanTime/results_summary.csv")

# BASE
# os.system("py scripts/plot_result_finite_horizon.py output/finite-horizon-workday-base-medMeanTime/runs-samples/run-0/sample.csv")
# os.system("py scripts/table_results_finite_horizon.py output/finite-horizon-workday-base-medMeanTime/population-ie.csv")
# os.system("py scripts/table_results_finite_horizon.py output/finite-horizon-workday-base-medMeanTime/time-ie.csv")
# os.system("py scripts/plot_transient_analysis.py output/transient-analysis-base-medMeanTime NormalLambda")
# os.system("py scripts/plot_transient_analysis.py output/transient-analysis-base-doubleMedMeanTime ReducedlLambda")
# os.system("py scripts/plot_verification.py output/verification-base-medMeanTime/verification_report.csv")
# os.system("py scripts/plot_verification.py output/verification-base-doubleMedMeanTime/verification_report.csv")
# os.system("py scripts/plot_batches_steady_state.py output/steady-state-base-doubleMedMeanTime/batch_data.csv")
# os.system("py scripts/plot_summary_steady_state.py output/steady-state-base-doubleMedMeanTime/results_summary.csv")


print("--- TUTTO FINITO ---")
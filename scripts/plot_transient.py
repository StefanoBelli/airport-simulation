import pandas as pd
import matplotlib.pyplot as plt
import sys
import os

# Lancia py scripts/plot_transient.py output/transient-analysis ReducedLambda
# Oppure py scripts/plot_transient.py output/transient-analysis MedLambda

# CONFIGURAZIONE
NUM_REPLICATIONS_TO_PLOT = 7

# Aggiunto parametro opzionale 'filename_suffix'
def generate_transient_plots(experiment_path, filename_suffix=""):
    runs_dir = os.path.join(experiment_path, "runs-samples")

    if not os.path.exists(runs_dir):
        print(f"ERRORE: Cartella runs-samples non trovata in: {experiment_path}")
        return

    print(f"--- Generazione Plot per: {experiment_path} (Suffisso: '{filename_suffix}') ---")

    # Definiamo dove salvare i grafici
    output_dir = os.path.join(experiment_path, "plot")
    os.makedirs(output_dir, exist_ok=True)

    # Dizionario per accumulare i dati
    collected_data = {
        'System_Ts': [],
        'XRay_Ts': [],
        'XRay_Nq': []
    }

    # 1. RACCOLTA DATI
    for i in range(NUM_REPLICATIONS_TO_PLOT):
        csv_path = os.path.join(runs_dir, f"run-{i}", "sample.csv")

        if not os.path.exists(csv_path):
            print(f"[WARNING] Run-{i} non trovata, salto.")
            continue

        try:
            df = pd.read_csv(csv_path, sep=',')
            df.columns = df.columns.str.strip()
            df['Metric'] = df['Metric'].str.strip()
            df['Center'] = df['Center'].str.strip()

            # A) E[Ts] System
            sys_ts = df[df['Metric'] == 'SystemResponseTime_Success'].copy()
            sys_ts['RunID'] = i
            collected_data['System_Ts'].append(sys_ts)

            # B) E[Ts] XRay
            xray_ts = df[(df['Metric'] == 'TimeTotal') & (df['Center'] == 'XRay')].copy()
            xray_ts['RunID'] = i
            collected_data['XRay_Ts'].append(xray_ts)

            # C) E[Nq] XRay
            xray_nq = df[(df['Metric'] == 'NumQueue') & (df['Center'] == 'XRay')].copy()
            xray_nq['RunID'] = i
            collected_data['XRay_Nq'].append(xray_nq)

            print(f" -> Caricata Run {i} (Seed {i})...")

        except Exception as e:
            print(f"ERRORE leggendo Run {i}: {e}")

    # 2. FUNZIONE PLOTTING
    def plot_spaghetti(data_list, title, filename, y_label):
        if not data_list:
            print(f"[SKIP] Nessun dato per {title}")
            return

        plt.figure(figsize=(12, 7))

        # Colormap 'tab10' offre colori ben distinti per le categorie
        colors = plt.cm.tab10(range(NUM_REPLICATIONS_TO_PLOT))

        for idx, df_run in enumerate(data_list):
            run_id = df_run['RunID'].iloc[0]

            # Legenda: Run ID = Seed RNGS
            label_text = f"Run {run_id} (Seed #{run_id})"

            plt.plot(df_run['Time'], df_run['Value'],
                     label=label_text,
                     color=colors[idx],
                     linewidth=1.5,
                     alpha=0.75)

        plt.title(f"Transient Analysis: {title}", fontsize=15, fontweight='bold')
        plt.xlabel("Tempo di Simulazione (s)", fontsize=12)
        plt.ylabel(y_label, fontsize=12)

        plt.legend(loc='upper right', title="Configurazione Seed", frameon=True, shadow=True)

        plt.grid(True, linestyle='--', alpha=0.4)

        out_path = os.path.join(output_dir, filename)
        plt.savefig(out_path, dpi=200, bbox_inches='tight')
        plt.close()
        print(f" -> Generato Grafico: {filename}")

    # 3. GENERAZIONE
    print("\nGenerazione Grafici...")

    # Qui aggiungiamo il suffisso al nome del file
    plot_spaghetti(collected_data['System_Ts'],
                   "System Response Time E[Ts]",
                   f"Transient_System_Ts{filename_suffix}.png",
                   "Tempo Risposta (s)")

    plot_spaghetti(collected_data['XRay_Ts'],
                   "XRay Response Time E[Ts]",
                   f"Transient_XRay_Ts{filename_suffix}.png",
                   "Tempo Risposta XRay (s)")

    plot_spaghetti(collected_data['XRay_Nq'],
                   "XRay Queue Length E[Nq]",
                   f"Transient_XRay_Nq{filename_suffix}.png",
                   "Utenti in Coda XRay")

    print(f"\n--- Finito. Grafici salvati in: {output_dir} ---")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python scripts/plot_transient.py <cartella_esperimento> [suffisso_opzionale]")
        print("Esempio: python scripts/plot_transient.py output/transient-analysis ReducedLambda")
    else:
        path = sys.argv[1]
        suffix = ""
        # Se c'è un terzo argomento, usalo come suffisso (aggiungendo l'underscore automaticamente)
        if len(sys.argv) > 2:
            suffix = "_" + sys.argv[2] # Es. diventa "_ReducedLambda"

        generate_transient_plots(path, suffix)

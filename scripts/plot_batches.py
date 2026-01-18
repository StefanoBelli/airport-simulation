import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import sys
import os

# Lancia con py plot_batches.py path/to/batch_data.csv

# CONFIGURAZIONE METRICHE
METRICS_TO_PLOT = [
    'Ts_XRay', 'Ns_XRay', 'Nq_XRay',
    'Ts_Varchi', 'Ns_Varchi',
    'Ts_CheckIn'
]

def plot_batch_trends(csv_path):
    if not os.path.exists(csv_path):
        print(f"ERRORE: File non trovato: {csv_path}")
        return

    # Calcolo cartella di output dinamica:
    # Input: .../output/NOME_ESPERIMENTO/batch_data.csv
    # Output: .../output/NOME_ESPERIMENTO/plots/batches/
    experiment_dir = os.path.dirname(csv_path)
    output_dir = os.path.join(experiment_dir, "plot")
    os.makedirs(output_dir, exist_ok=True)

    print(f"--- Elaborazione Batches: {csv_path} ---")
    print(f"Output: {output_dir}")

    df = pd.read_csv(csv_path)
    sns.set_theme(style="whitegrid")

    for metric in METRICS_TO_PLOT:
        subset = df[df['Metric'] == metric].copy() # .copy() per evitare warning
        if subset.empty:
            continue

        plt.figure(figsize=(10, 6))

        # 1. Dati grezzi
        sns.lineplot(data=subset, x='NumBatch', y='Value', alpha=0.3, color='blue', label='Valore Batch')

        # 2. Media Mobile
        subset['RollingMean'] = subset['Value'].rolling(window=5).mean()
        sns.lineplot(data=subset, x='NumBatch', y='RollingMean', linewidth=2, color='red', label='Media Mobile (5)')

        # 3. Media Globale
        grand_mean = subset['Value'].mean()
        plt.axhline(grand_mean, color='green', linestyle='--', label=f'Media Globale ({grand_mean:.2f})')

        plt.title(f'Stabilità: {metric}', fontsize=16)
        plt.xlabel('Numero Batch')
        plt.ylabel('Valore')
        plt.legend()

        out_file = os.path.join(output_dir, f"{metric}_trend.png")
        plt.savefig(out_file, dpi=300)
        plt.close()
        print(f" -> Salvato: {os.path.basename(out_file)}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python plot_batches.py <path_to_batch_data.csv>")
    else:
        plot_batch_trends(sys.argv[1])
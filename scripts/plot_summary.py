import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import sys
import os

# Lancia con py plot_summary.py path/to/results_summary.csv

def plot_summary_clean(csv_path):
    if not os.path.exists(csv_path):
        print(f"ERRORE: File non trovato: {csv_path}")
        return

    # Calcolo cartella di output dinamica
    experiment_dir = os.path.dirname(csv_path)
    output_dir = os.path.join(experiment_dir, "plot")
    os.makedirs(output_dir, exist_ok=True)

    print(f"--- Elaborazione Summary: {csv_path} ---")
    print(f"Output: {output_dir}")

    df = pd.read_csv(csv_path)
    sns.set_theme(style="whitegrid")

    prefixes = [('Ts_', 'Tempi di Risposta Medi (E[Ts])', 'Secondi'),
                ('Ns_', 'Popolazione Media (E[Ns])', 'Utenti')]

    for prefix, title, ylabel in prefixes:
        subset = df[df['Metric'].str.startswith(prefix)].sort_values('Mean', ascending=False)
        if subset.empty: continue

        plt.figure(figsize=(12, 8))
        barplot = sns.barplot(data=subset, x='Metric', y='Mean', palette='viridis', edgecolor='black', alpha=0.8)

        plt.title(f'{title}', fontsize=16, fontweight='bold')
        plt.ylabel(ylabel, fontsize=12)
        plt.xlabel('')
        plt.xticks(rotation=45, ha='right')
        plt.ylim(0, subset['Mean'].max() * 1.2)

        for i, p in enumerate(barplot.patches):
            height = p.get_height()
            width_val = subset.iloc[i]['Width']
            barplot.annotate(f"{height:.2f}\n(±{width_val:.2f})",
                             (p.get_x() + p.get_width() / 2., height),
                             ha='center', va='bottom', fontsize=10, color='black', fontweight='bold',
                             xytext=(0, 5), textcoords='offset points')

        plt.tight_layout()
        out_file = os.path.join(output_dir, f"{prefix}summary.png")
        plt.savefig(out_file, dpi=300)
        plt.close()
        print(f" -> Salvato: {os.path.basename(out_file)}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python plot_summary.py <path_to_results_summary.csv>")
    else:
        plot_summary_clean(sys.argv[1])
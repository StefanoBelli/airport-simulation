import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import sys
import os

# Lancia con plot_verification.py path/to/verification_report.csv

def plot_verification(csv_path):
    if not os.path.exists(csv_path):
        print(f"ERRORE: File non trovato: {csv_path}")
        return

    # Calcolo cartella di output dinamica
    experiment_dir = os.path.dirname(csv_path)
    output_dir = os.path.join(experiment_dir, "plot")
    os.makedirs(output_dir, exist_ok=True)

    print(f"--- Elaborazione Verifica: {csv_path} ---")
    print(f"Output: {output_dir}")

    df = pd.read_csv(csv_path)

    centers = df['Center']
    theory = df['Theory_Ts']
    sim = df['Sim_Ts']
    x = np.arange(len(centers))
    width = 0.35

    fig, ax = plt.subplots(figsize=(12, 7))
    rects1 = ax.bar(x - width/2, theory, width, label='Teorico', color='#1f77b4', alpha=0.8)
    rects2 = ax.bar(x + width/2, sim, width, label='Simulato', color='#ff7f0e', alpha=0.8)

    ax.set_ylabel('Tempo di Risposta E[Ts] (s)')
    ax.set_title('Verifica Modello: Teoria vs Simulazione', fontsize=16)
    ax.set_xticks(x)
    ax.set_xticklabels(centers)
    ax.legend()
    ax.grid(axis='y', linestyle='--', alpha=0.5)

    max_height = max(df['Theory_Ts'].max(), df['Sim_Ts'].max())
    ax.set_ylim(0, max_height * 1.2)

    # Annotazioni
    i = 0
    for rect in rects2:
        height = rect.get_height()
        error_perc = df.iloc[i]['Error_%']
        status = df.iloc[i]['Status']
        color = 'green' if status == 'OK' else 'red'

        ax.annotate(f'{error_perc:.1f}%\n({status})',
                    xy=(rect.get_x() + rect.get_width() / 2, height),
                    xytext=(0, 5), textcoords="offset points",
                    ha='center', va='bottom', color=color, fontweight='bold', fontsize=9)
        i += 1

    plt.tight_layout()
    out_file = os.path.join(output_dir, "verification_chart.png")
    plt.savefig(out_file, dpi=300)
    print(f" -> Grafico salvato: {out_file}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python plot_verification.py <path_to_verification_report.csv>")
    else:
        plot_verification(sys.argv[1])
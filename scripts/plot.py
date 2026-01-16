#!/usr/bin/python

import matplotlib.pyplot as plt
import pandas as pd
import sys

def plot_metrics_from_csv(csv_file, output_file='metrics_plot.png'):
    """
    Genera plot da file CSV con colonne: Time, Center, Metric, Value

    Parametri:
    - csv_file: path del file CSV
    - output_file: nome file di output per il plot (default: metrics_plot.png)
    """

    # Carica i dati dal CSV
    df = pd.read_csv(csv_file)

    # Converte la colonna Time in datetime
    df['Time'] = pd.to_datetime(df['Time'])

    # Ottiene le metriche uniche e i sistemi unici
    metrics = df['Metric'].unique()
    systems = df['Center'].unique()

    # Crea una figura con subplot per ogni metrica
    n_metrics = len(metrics)
    fig, axes = plt.subplots(n_metrics, 1, figsize=(12, 4*n_metrics))

    # Gestisce il caso di una sola metrica
    if n_metrics == 1:
        axes = [axes]

    # Genera un plot per ogni metrica
    for idx, metric in enumerate(metrics):
        ax = axes[idx]

        # Filtra i dati per la metrica corrente
        metric_data = df[df['Metric'] == metric]

        # Plotta una linea per ogni sistema
        for system in systems:
            system_data = metric_data[metric_data['Center'] == system]
            ax.plot(system_data['Time'], system_data['Value'], 
                   marker='o', label=system, linewidth=2)

        ax.set_xlabel('Time', fontsize=10)
        ax.set_ylabel('Value', fontsize=10)
        ax.set_title(f'{metric}', fontsize=12, fontweight='bold')
        ax.legend(loc='best')
        ax.grid(True, alpha=0.3)

        # Ruota le etichette dell'asse x per migliore leggibilità
        plt.setp(ax.xaxis.get_majorticklabels(), rotation=45, ha='right')

    plt.tight_layout()
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"Plot salvato in: {output_file}")

    return fig

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python script.py <file_csv> [file_output.png]")
        print("\nEsempio di utilizzo:")
        print("  python script.py dati.csv")
        print("  python script.py dati.csv grafico.png")
        sys.exit(1)

    csv_file = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) > 2 else 'metrics_plot.png'

    plot_metrics_from_csv(csv_file, output_file)

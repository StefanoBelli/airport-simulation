import pandas as pd
import matplotlib.pyplot as plt
import sys
import os

# Lancia py scripts/table_results.py output/finite-horizon-workday/population-ie.csv
# E poi py scripts/table_results.py output/finite-horizon-workday/time-ie.csv

def generate_tables(csv_file_path):
    if not os.path.exists(csv_file_path):
        print(f"ERRORE: File non trovato: {csv_file_path}")
        return

    print(f"--- Generazione TABELLE per: {csv_file_path} ---")

    # 1. Caricamento Dati
    try:
        df = pd.read_csv(csv_file_path, sep=',')
    except Exception as e:
        print(f"ERRORE Lettura CSV: {e}")
        return

    # 2. Parsing Metrica
    split_data = df['Metric'].str.split('_', n=1, expand=True)
    if split_data.shape[1] == 2:
        df['MetricType'] = split_data[0]
        df['Center'] = split_data[1]
    else:
        df['MetricType'] = df['Metric'] # es. SystemResponseTime
        df['Center'] = "Sistema"

    # Ordiniamo le colonne per pulizia
    df_clean = df[['MetricType', 'Center', 'Mean', 'Width', 'Min', 'Max']]

    # Output Dir
    file_dir = os.path.dirname(csv_file_path)
    experiment_dir = os.path.dirname(file_dir) if "runs-samples" in file_dir else file_dir
    output_dir = os.path.join(experiment_dir, "tables")
    os.makedirs(output_dir, exist_ok=True)

    # 3. Generazione Tabelle per Tipo
    unique_types = df['MetricType'].unique()

    for m_type in unique_types:
        subset = df_clean[df_clean['MetricType'] == m_type].copy()

        # Rimuoviamo la colonna MetricType (è nel titolo)
        subset_to_plot = subset[['Center', 'Mean', 'Width', 'Min', 'Max']]

        # Arrotondamento per visualizzazione
        subset_to_plot = subset_to_plot.round(6)

        # Creazione Immagine
        fig, ax = plt.subplots(figsize=(8, len(subset) * 0.6 + 1)) # Altezza dinamica
        ax.axis('off')

        # Titolo descrittivo
        desc = get_metric_description(m_type)
        plt.title(f"{desc} ({m_type}) - Confidenza 95%", fontsize=12, pad=20, fontweight='bold')

        # Disegna Tabella
        table = ax.table(cellText=subset_to_plot.values,
                         colLabels=subset_to_plot.columns,
                         cellLoc='center',
                         loc='center')

        # Stile Tabella
        table.auto_set_font_size(False)
        table.set_fontsize(10)
        table.scale(1.2, 1.5) # Scaling larghezza/altezza celle

        # Colora Header
        for (row, col), cell in table.get_celld().items():
            if row == 0:
                cell.set_text_props(weight='bold', color='white')
                cell.set_facecolor('#4c72b0')
            elif col == 0: # Prima colonna (Center) in grassetto
                cell.set_text_props(weight='bold')

        filename = f"TABLE_{m_type}.png"
        out_path = os.path.join(output_dir, filename)
        plt.savefig(out_path, dpi=200, bbox_inches='tight')
        plt.close()
        print(f" -> Generata: {filename}")

    print(f"--- Finito. Tabelle salvate in: {output_dir} ---")

def get_metric_description(metric_code):
    map_desc = {
        'Nq': 'Numero Utenti in Coda',
        'Ns': 'Numero Utenti nel Sistema',
        'Ts': 'Tempo di Risposta (Wait+Svc)',
        'Tq': 'Tempo di Attesa in Coda',
        'S':  'Tempo di Servizio',
        'X':  'Server Attivi',
        'SystemResponseTime': 'Tempo Totale Attraversamento'
    }
    return map_desc.get(metric_code, metric_code)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: py scripts/table_results.py <path_csv>")
    else:
        generate_tables(sys.argv[1])
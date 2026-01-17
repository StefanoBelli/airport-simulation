import pandas as pd
import matplotlib.pyplot as plt
import sys
import os

# Lancia con py scripts/plot_result_finite_horizon.py output/finite-horizon-workday/runs-samples/run-0/sample.csv

def generate_plots(csv_file_path):
    if not os.path.exists(csv_file_path):
        print(f"ERRORE: File non trovato: {csv_file_path}")
        return

    print(f"--- Elaborazione: {csv_file_path} ---")

    # 1. Caricamento Dati
    try:
        df = pd.read_csv(csv_file_path, sep=',')
        # Rimuove spazi bianchi dai nomi delle colonne e dai valori stringa
        df.columns = df.columns.str.strip()
        df['Center'] = df['Center'].str.strip()
        df['Metric'] = df['Metric'].str.strip()
    except Exception as e:
        print(f"ERRORE Lettura CSV: {e}")
        return

    # 2. Determinazione Cartella di Output (MODIFICATO)
    # Percorso atteso del CSV: output/NOME_ESPERIMENTO/runs-samples/run-X/sample.csv

    # Risaliamo la struttura delle directory
    run_dir = os.path.dirname(csv_file_path)           # .../run-X
    runs_samples_dir = os.path.dirname(run_dir)        # .../runs-samples
    experiment_dir = os.path.dirname(runs_samples_dir) # .../NOME_ESPERIMENTO

    # Definiamo la cartella di destinazione 'plot' dentro la cartella dell'esperimento
    output_dir = os.path.join(experiment_dir, "plot")

    # Creiamo la cartella se non esiste
    os.makedirs(output_dir, exist_ok=True)
    print(f"Output grafici in: {output_dir}")

    # =================================================================
    # FUNZIONI HELPER PER I DUE TIPI DI GRAFICI
    # =================================================================

    # Aggiunto parametro opzionale show_legend
    def plot_multi_center(metric_name, filename_suffix, title_desc, show_legend=True):
        """ Genera grafici con TUTTI i centri per una data metrica """
        data = df[df['Metric'] == metric_name]
        if data.empty:
            print(f"[SKIP] Nessun dato per {metric_name}")
            return

        plt.figure(figsize=(10, 6))
        centers = data['Center'].unique()

        for center in centers:
            subset = data[data['Center'] == center].sort_values(by='Time')
            plt.plot(subset['Time'], subset['Value'], label=center, linewidth=1.5)

        plt.title(f"{title_desc}", fontsize=14) # Rimossa la parte fissa "Confronto Centri:" per pulizia
        plt.xlabel("Tempo simulato (s)")
        plt.ylabel("Valore medio corrente")

        # Gestione condizionale della legenda (MODIFICATO)
        if show_legend:
            plt.legend(loc='best')

        plt.grid(True, linestyle='--', alpha=0.5)

        out_path = os.path.join(output_dir, f"plot_ALL_{filename_suffix}.png")
        plt.savefig(out_path, dpi=150, bbox_inches='tight')
        plt.close()
        print(f" -> Generato: {os.path.basename(out_path)}")

    def plot_specific_center(metric_name, center_name, filename, title_desc):
        """ Genera grafico per UN solo centro e UNA metrica """
        # Filtra per metrica E per centro
        data = df[(df['Metric'] == metric_name) & (df['Center'] == center_name)]
        if data.empty:
            print(f"[SKIP] Nessun dato per {center_name} - {metric_name}")
            return

        plt.figure(figsize=(10, 6))
        data = data.sort_values(by='Time')

        # Colore rosso per evidenziare i critici
        plt.plot(data['Time'], data['Value'], color='#d62728', linewidth=1.5)

        plt.title(f"Focus {center_name}: {title_desc}", fontsize=14)
        plt.xlabel("Tempo simulato (s)")
        plt.ylabel("Valore medio corrente")
        plt.grid(True, linestyle='--', alpha=0.5)

        out_path = os.path.join(output_dir, filename)
        plt.savefig(out_path, dpi=150, bbox_inches='tight')
        plt.close()
        print(f" -> Generato: {os.path.basename(out_path)}")

    # =================================================================
    # ESECUZIONE DEI 6 OBIETTIVI
    # =================================================================

    print("Generazione Grafici Gruppo A (Panoramica)...")
    # (1) E[Ts] per tutti i centri
    plot_multi_center("TimeTotal", "TimeTotal", "Tempo di Attraversamento Medio nei Centri (E[Ts])")

    # (2) E[Ns] per tutti i centri
    plot_multi_center("NumTotal", "NumTotal", "Numero Medio Utenti nei Centri (E[Ns])")

    # (3) X(t) per tutti i centri
    plot_multi_center("BusyServers", "BusyServers", "Numero Medio Server Attivi (E[X])")

    print("Generazione Grafici Gruppo B (Focus)...")
    # (4) System Response Time (MODIFICATO: show_legend=False)
    plot_multi_center("SystemResponseTime_Success", "SystemResponseTime", "Tempo di Risposta Medio Totale Aeroporto", show_legend=False)

    # (5) E[Tq] XRay
    plot_specific_center("TimeQueue", "XRay", "plot_XRay_TimeQueue.png", "Tempo Attesa Medio in Coda (E[Tq])")

    # (6) E[Ns] XRay
    plot_specific_center("NumTotal", "XRay", "plot_XRay_NumTotal.png", "Numero Medio Utenti Totali (E[Ns])")

    print(f"--- Finito. Grafici salvati in: {output_dir} ---")

if __name__ == "__main__":
    # Esempio uso: py scripts/plot_result_finite_horizon.py output/finite-horizon-workday/runs-samples/run-0/sample.csv
    if len(sys.argv) < 2:
        print("Uso: python plot_results.py <path_al_csv>")
    else:
        generate_plots(sys.argv[1])
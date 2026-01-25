import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import sys
import os

# Lancia con: py scripts/plot_verification.py output/verification_report.csv

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

    try:
        df = pd.read_csv(csv_path)
    except Exception as e:
        print(f"Errore nella lettura del CSV: {e}")
        return

    # Ottieni la lista dei centri univoci
    unique_centers = df['Center'].unique()

    for center in unique_centers:
        print(f" -> Generazione grafico per: {center}")
        
        # Filtra i dati per il centro corrente
        df_center = df[df['Center'] == center].copy()
        
        metrics = df_center['Metric'].tolist()
        sim_values = df_center['SimValueMean'].tolist()
        theo_values = df_center['TheoValue'].tolist()
        within_status = df_center['WithinIntvl'].tolist()

        x = np.arange(len(metrics))
        width = 0.35

        fig, ax = plt.subplots(figsize=(12, 7))
        
        # Coppia di barre: Simulato (Sinistra) e Teorico (Destra)
        rects_sim = ax.bar(x - width/2, sim_values, width, label='Simulato (Mean)', color='#ff7f0e', alpha=0.9)
        rects_theo = ax.bar(x + width/2, theo_values, width, label='Teorico', color='#1f77b4', alpha=0.9)

        ax.set_ylabel('Valore')
        ax.set_title(f'Verifica Modello: {center}', fontsize=16)
        ax.set_xticks(x)
        ax.set_xticklabels(metrics, rotation=15, ha="right")
        ax.legend()
        ax.grid(axis='y', linestyle='--', alpha=0.5)

        # Imposta il limite Y per lasciare spazio al testo sopra le barre
        max_height = 0
        if len(sim_values) > 0 and len(theo_values) > 0:
            max_height = max(max(sim_values), max(theo_values))
        ax.set_ylim(0, max_height * 1.25)

        # Annotazioni: Scrivi "within" o "not within" sopra la barra Teorica
        for i, rect in enumerate(rects_theo):
            height = rect.get_height()
            status = within_status[i]
            
            # Colore testo: Verde se within, Rosso se not within
            text_color = 'green' if 'within' in str(status).lower() and 'not' not in str(status).lower() else 'red'
            
            ax.annotate(f'{status}',
                        xy=(rect.get_x() + rect.get_width() / 2, height),
                        xytext=(0, 5),  # 5 punti di offset verticale
                        textcoords="offset points",
                        ha='center', va='bottom', 
                        color=text_color, fontweight='bold', fontsize=9, rotation=0)

        plt.tight_layout()
        
        # Salva un file per ogni centro
        # Pulisci il nome del centro da caratteri strani per il filename
        clean_center_name = "".join([c for c in center if c.isalnum() or c in (' ', '_')]).strip().replace(" ", "_")
        out_file = os.path.join(output_dir, f"verification_{clean_center_name}.png")
        plt.savefig(out_file, dpi=300)
        plt.close(fig) # Chiude la figura per liberare memoria

    print("--- Elaborazione completata ---")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python plot_verification.py <path_to_verification_report.csv>")
    else:
        plot_verification(sys.argv[1])

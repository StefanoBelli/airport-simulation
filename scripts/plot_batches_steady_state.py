import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import sys
import os


# Lancia con: py scripts/plot_batches_steady_state.py output/steady-state-base-doubleMedMeanTime/batch_data.csv


# CONFIGURAZIONE METRICHE
# Ho aggiunto TraceDetection e Recupero (Ns, Ts, Nq)
METRICS_TO_PLOT = [
   # --- XRay (Collo di Bottiglia) ---
   'Ts_XRay', 'Ns_XRay', 'Nq_XRay',


   # --- Varchi ---
   'Ts_Varchi', 'Ns_Varchi', 'Nq_Varchi',


   # --- CheckIn ---
   'Ts_CheckIn', 'Ns_CheckIn', 'Nq_CheckIn',


   # --- Trace Detection (AGGIUNTO) ---
   'Ts_TraceDetection', 'Ns_TraceDetection', 'Nq_TraceDetection',

   # --- Trace Detection (AGGIUNTO) ---
      'Ts_FastTrack', 'Ns_FastTrack', 'Nq_FastTrack',


   # --- Recupero Oggetti (AGGIUNTO) ---
   'Ts_Recupero', 'Ns_Recupero', 'Nq_Recupero',


   # --- System Global ---
   'SystemResponseTime_Success'
]


def plot_batch_trends(csv_path):
   if not os.path.exists(csv_path):
       print(f"ERRORE: File non trovato: {csv_path}")
       return


   # Calcolo cartella di output dinamica:
   experiment_dir = os.path.dirname(csv_path)
   output_dir = os.path.join(experiment_dir, "plot", "batches") # Ho aggiunto una sottocartella 'batches' per ordine
   os.makedirs(output_dir, exist_ok=True)


   print(f"--- Elaborazione Batches: {csv_path} ---")
   print(f"Output: {output_dir}")


   try:
       df = pd.read_csv(csv_path)
   except Exception as e:
       print(f"ERRORE nella lettura del CSV: {e}")
       return


   # Pulizia nomi colonne (trim spazi)
   df.columns = df.columns.str.strip()
   if 'Metric' in df.columns:
       df['Metric'] = df['Metric'].str.strip()


   sns.set_theme(style="whitegrid")


   for metric in METRICS_TO_PLOT:
       # Filtra il dataframe per la metrica specifica
       subset = df[df['Metric'] == metric].copy()


       if subset.empty:
           print(f"[SKIP] Metrica non trovata nel CSV: {metric}")
           continue


       plt.figure(figsize=(10, 6))


       # 1. Dati grezzi (Scatter + Linea leggera)
       sns.lineplot(data=subset, x='NumBatch', y='Value', alpha=0.4, color='dodgerblue', label='Valore Batch', linewidth=1)


       # 2. Media Mobile (per vedere il trend smussato)
       # Window=10 per lisciare meglio se ci sono molti batch (96)
       subset['RollingMean'] = subset['Value'].rolling(window=10, min_periods=1).mean()
       sns.lineplot(data=subset, x='NumBatch', y='RollingMean', linewidth=2.5, color='crimson', label='Media Mobile (10)')


       # 3. Media Globale (Grand Mean)
       grand_mean = subset['Value'].mean()
       plt.axhline(grand_mean, color='forestgreen', linestyle='--', linewidth=2, label=f'Media Globale ({grand_mean:.2f})')


       # Titoli e Label
       plt.title(f'Analisi Stabilità Batch: {metric}', fontsize=15, fontweight='bold')
       plt.xlabel('Numero Progressivo Batch', fontsize=12)
       plt.ylabel('Valore Medio del Batch', fontsize=12)
       plt.legend(loc='best', frameon=True)
       plt.grid(True, linestyle=':', alpha=0.6)


       # Salvataggio
       out_file = os.path.join(output_dir, f"BatchTrend_{metric}.png")
       plt.tight_layout()
       plt.savefig(out_file, dpi=200)
       plt.close()
       print(f" -> Salvato: {os.path.basename(out_file)}")


if __name__ == "__main__":
   if len(sys.argv) < 2:
       print("Uso: python scripts/plot_batches_steady_state.py <path_to_batch_data.csv>")
   else:
       plot_batch_trends(sys.argv[1])


package mbpmcsn.core;

/**
 * contains all global static parameters for the system configuration
 * (es. peak arrival rates, service rates, number of servers)
 * and simulation control (es. stop time)
 */

public final class Constants {

    // --- Simulation Control ---
    public static final long SEED = 123456789L;

    public static final int STREAM_ARRIVALS      = 0;

    public static final int STREAM_S1_SERVICE    = 10;

    public static final int STREAM_S2_SERVICE    = 20;
    public static final int STREAM_S2_FLOWPOL    = 21; // SQF

    public static final int STREAM_S3_SERVICE    = 40;
    public static final int STREAM_S3_ROUTING    = 41; // trace/recupero

    public static final int STREAM_S4_SERVICE    = 50;
    public static final int STREAM_S4_ROUTING    = 51; // espulsione/recupero

    public static final int STREAM_S5_SERVICE    = 60;

    // --- Topology Node IDs ---
    public static final int ID_INGRESSO = 0;       // Source
    public static final int ID_BANCHI_CHECKIN = 1;        // MSSQ
    public static final int ID_VARCHI_ELETTRONICI = 2;         // MSMQ
    public static final int ID_XRAY = 3;           // MSMQ
    public static final int ID_TRACE_DETECTION = 4;          // SSQ
    public static final int ID_RECUPERO_OGGETTI = 5;       // IS (Infinite Server)

    // --- Arrival Process ---
    public static final double ARRIVAL_PEAK_RATE = 0.222383; /* pax/sec */
    public static final double ARRIVAL_PEAK_MEAN_TIME = 4.496747; /* (1 / ARRIVAL_PEAK_RATE) sec */

    public static final double ARRIVAL_LOW_RATE = 0.090598;
    public static final double ARRIVAL_LOW_MEAN_TIME = 11.037771;

    public static final double ARRIVAL_MED_RATE = 0.127205;
    public static final double ARRIVAL_MED_MEAN_TIME = 7.861326;

    public static final int WORK_DAY = 64800; // secondi in 18h
    public static final int PEAK_TIME = 4800; // secondi in 80 min
    public static final double FINITE_HORIZON_SAMPLING_INTERVAL = 100.00;
    public static final double TRANSIENT_DURATION = 100000.0;
    public static final double TRANSIENT_SAMPLING_INTERVAL = 400.0;
    public static final double TIME_WARMUP = 60000.0;
    public static final int NUM_BATCHES = 96;
    public static final int BATCH_SIZE = 1080;

    // --- Routing Probabilities ---
    public static final double P_DESK = 0.387181; // Vai ai Banchi Accettazione
    public static final double P_DIRECT = 0.612819; // Vai direttamente ai Varchi Elettronici
    public static final double P_CHECK = 0.1; // Vai al controllo approfondito (Trace Detection)
    public static final double P_STANDARD = 0.9; // Salta Trace Detection
    public static final double P_FAIL = 0.0001; // Espulso dal sistema
    public static final double P_SUCCESS = 0.9999; // Supera controlli sicurezza

    // --- Center 1: Banchi Accettazione (MSSQ), Truncated Normal
    public static final int M1 = 8; // numero server centro 1
    public static final double MEAN_S1 = 105.0;
    public static final double STD_S1 = 35.0;
    public static final double LB1 = 60.0;
    public static final double UB1 = 150.0;
    // --- Center 2: Varchi Elettronici (MSMQ), Truncated Normal
    public static final int M2 = 4; // numero server centro 2
    public static final double MEAN_S2 = 12;
    public static final double STD_S2 = 4;
    public static final double LB2 = 10.0;
    public static final double UB2 = 24;
    // --- Center 3: Controlli a Raggi X (MSMQ), Erlang-k (k=3)
    public static final int M3 = 6; // numero server centro 4
    public static final double MEAN_S3 = 45;
    public static final double STD_S3 = 25.0;
    public static final double LB3 = 20.0;
    public static final double UB3 = 90;
    // --- Center 4: Trace Detection (SSQ), Truncated Normal ---
	public static final int M4 = 1;
    public static final double MEAN_S4 = 60.0;
    public static final double STD_S4 = 20.0;
    public static final double LB4 = 30.0;
    public static final double UB4 = 100;
    // --- Center 5: Recupero Oggetti Utente (IS), Truncated Normal ---
    public static final double MEAN_S5 = 120.0;
    public static final double STD_S5 = 40.0;
    public static final double LB5 = 60.0;
    public static final double UB5 = 340.0;

}

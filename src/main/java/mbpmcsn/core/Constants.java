package mbpmcsn.core;

/**
 * contains all global static parameters for the system configuration
 * (es. peak arrival rates, service rates, number of servers)
 * and simulation control (es. stop time)
 */

public final class Constants {

    // --- Simulation Control ---
    public static final long SEED = 123456789L;

    // --- Topology Node IDs ---
    public static final int ID_INGRESSO = 0;       // Source
    public static final int ID_BANCHI_CHECKIN = 1;        // MSSQ
    public static final int ID_VARCHI_ELETTRONICI = 2;         // MSMQ
    public static final int ID_PREPARAZIONE_OGGETTI = 3;   // IS (Infinite Server)
    public static final int ID_XRAY = 4;           // MSMQ
    public static final int ID_TRACE_DETECTION = 5;          // SSQ
    public static final int ID_RECUPERO_OGGETTI = 6;       // IS (Infinite Server)

    // --- Arrival Process ---
    public static final double ARRIVAL_PEAK_RATE = 0.222383; /* pax/sec */
    public static final double ARRIVAL_MEAN_TIME = 4.496747; /* sec */

    // --- Routing Probabilities ---
    public static final double P_DESK = 0.387181; // Vai ai Banchi Accettazione
    public static final double P_DIRECT = 0.612819; // Vai direttamente ai Varchi Elettronici
    public static final double P_CHECK = 0.1; // Vai al controllo approfondito (Trace Detection)
    public static final double P_STANDARD = 0.9; // Salta Trace Detection
    public static final double P_FAIL = 0.0001; // Espulso dal sistema
    public static final double P_SUCCESS = 0.9999; // Supera controlli sicurezza

    // --- Center 1: Banchi Accettazione (MSSQ), Truncated Normal ---
    public static final int M1 = 4; // numero server centro 1
    public static final double MEAN_S1 = 150.0;
    public static final double STD_S1 = 50.0;
    public static final double LB1 = 50.0;
    public static final double UB1 = 300.0;

    // --- Center 2: Varchi Elettronici (MSMQ), Truncated Normal ---
    public static final int M2 = 4; // numero server centro 2
    public static final double MEAN_S2 = 15.0;
    public static final double STD_S2 = 4.0;
    public static final double LB2 = 4.0;
    public static final double UB2 = 30.0;

    // --- Center 3: Preparazione Oggetti Utente (IS), Truncated Normal ---
    public static final double MEAN_S3 = 90.0;
    public static final double STD_S3 = 30.0;
    public static final double LB3 = 30.0;
    public static final double US3 = 210.0;

    // --- Center 4: Controlli a Raggi X (MSMQ), Erlang-k (k=3) ---
    public static final int M4 = 6; // numero server centro 4
    public static final int K = 3;
    public static final double MEAN_S4 = 60.0;
    public static final double LB4 = 30.0;

    // --- Center 5: Trace Detection (SSQ), Truncated Normal ---
    public static final int M5 = 1; // numero server centro 5
    // Distribution: Truncated Normal
    public static final double MEAN_S5 = 120.0;
    public static final double STD_S5 = 40.0;
    public static final double LB5 = 40.0;
    public static final double UB5 = 280.0;

    // --- Center 6: Recupero Oggetti Utente (IS), Truncated Normal ---
    public static final double MEAN_S6 = 180.0;
    public static final double STD_S6 = 60.0;
    public static final double LB6 = 60.0;
    public static final double UB6 = 420.0;
}

package mbpmcsn.runners.verification;

import mbpmcsn.csv.annotations.*;

@CsvDescriptor
public final class VerificationResultRow {

    private final String centerName;
    private final String modelType;
    private final double rho;
    private final double theoreticalTs;
    private final double simulatedTs;
    private final double errorPerc;
    private final String status; // "OK" o "WARNING"

    public VerificationResultRow(String centerName, String modelType, double rho, double theoreticalTs, double simulatedTs, double errorPerc, String status) {
        this.centerName = centerName;
        this.modelType = modelType;
        this.rho = rho;
        this.theoreticalTs = theoreticalTs;
        this.simulatedTs = simulatedTs;
        this.errorPerc = errorPerc;
        this.status = status;
    }

    @CsvColumn(order = 1, name = "Center")
    public String getCenterName() { return centerName; }

    @CsvColumn(order = 2, name = "Model")
    public String getModelType() { return modelType; }

    @CsvColumn(order = 3, name = "Rho")
    public double getRho() { return rho; }

    @CsvColumn(order = 4, name = "Theory_Ts")
    public double getTheoreticalTs() { return theoreticalTs; }

    @CsvColumn(order = 5, name = "Sim_Ts")
    public double getSimulatedTs() { return simulatedTs; }

    @CsvColumn(order = 6, name = "Error_%")
    public double getErrorPerc() { return errorPerc; }

    @CsvColumn(order = 7, name = "Status")
    public String getStatus() { return status; }
}
package mbpmcsn.entity;

public final class Job {

    private static int ID_COUNTER = 0;
    private final int id;

    private final double arrivalTime; // entry time in the system
    private double lastQueuedTime; // T_in_queue (arrival at center)
    private double lastStartServiceTime; // T_start_service (service begins)
    private double lastEndServiceTime; // T_out (departure from center)

    private boolean checkedBaggage;
    private boolean securityCheckRequested;
    private boolean securityCheckFailed;

    public Job(double arrivalTime) {
        this.id = ++ID_COUNTER;
        this.arrivalTime = arrivalTime;
        this.checkedBaggage = false;
        this.securityCheckFailed = false;
        this.securityCheckRequested = false;
    }

    public int getId() { 
    	return id; 
    }

    public double getArrivalTime() { 
    	return arrivalTime; 
    }

    public boolean hasCheckedBaggage() { 
    	return checkedBaggage; 
    }

    public void setCheckedBaggage(boolean checkedBaggage) { 
    	this.checkedBaggage = checkedBaggage; 
    }

    public boolean isSecurityCheckRequested() {
    	return securityCheckRequested;
    }

    public void setSecurityCheckRequested(boolean securityCheckRequested) {
    	this.securityCheckRequested = securityCheckRequested;
    }

    public boolean isSecurityCheckFailed() {
    	return securityCheckFailed;
    }

    public void setSecurityCheckFailed(boolean securityCheckFailed) {
    	this.securityCheckFailed = securityCheckFailed;
    }

    public double getLastQueuedTime() {
    	return lastQueuedTime;
    }

    public void setLastQueuedTime(double lastQueuedTime) {
    	this.lastQueuedTime = lastQueuedTime;
    }

    public double getLastStartServiceTime() {
    	return lastStartServiceTime;
    }

    public void setLastStartServiceTime(double lastStartServiceTime) {
    	this.lastStartServiceTime = lastStartServiceTime;
    }

    public double getLastEndServiceTime() {
    	return lastEndServiceTime;
    }

    public void setLastEndServiceTime(double lastEndServiceTime) {
    	this.lastEndServiceTime = lastEndServiceTime;
    }

    @Override
    public String toString() {
        return "Job#" + id;
    }

}

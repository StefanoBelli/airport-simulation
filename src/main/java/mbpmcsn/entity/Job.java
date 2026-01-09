package mbpmcsn.entity;

public final class Job {

    private static int ID_COUNTER = 0;

    private final int id;
    private final double arrivalTime; // entry time in the system

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

    @Override
    public String toString() {
        return "Job#" + id;
    }
}

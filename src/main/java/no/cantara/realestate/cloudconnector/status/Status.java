package no.cantara.realestate.cloudconnector.status;

public class Status {

    private final boolean isHealthy;

    private String messageDistributor = "active";
    private String messageRouter = "error";
    private String presentValueIngestion = "idle";
    private String trendObservationIngestion = "active";


    public Status(boolean isHealthy) {
        this.isHealthy = isHealthy;
    }

    public String isHealthy() {
        return isHealthy ? "healthy" : "unhealthy";
    }

    public String getMessageDistributor() {
        if (isHealthy)
            return "active";
        else
            return "idle";
    }

    public String getMessageRouter() {
        if (isHealthy)
            return "active";
        else
            return "error";
    }

    public String getPresentValueIngestion() {
        if (isHealthy)
            return "active";
        else
            return "idle";
    }

    public String getTrendObservationIngestion() {
        if (isHealthy)
            return "active";
        else
            return "idle";
    }
}

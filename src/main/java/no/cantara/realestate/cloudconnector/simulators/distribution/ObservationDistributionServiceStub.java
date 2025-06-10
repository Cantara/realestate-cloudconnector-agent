package no.cantara.realestate.cloudconnector.simulators.distribution;

import no.cantara.realestate.cloudconnector.audit.AuditTrail;
import no.cantara.realestate.distribution.ObservationDistributionClient;
import no.cantara.realestate.observations.ObservationMessage;
import no.cantara.realestate.plugins.distribution.DistributionService;
import no.cantara.realestate.utils.LimitedArrayList;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.List;
import java.util.Properties;

import static org.slf4j.LoggerFactory.getLogger;


public class ObservationDistributionServiceStub implements DistributionService, ObservationDistributionClient {
    private static final Logger log = getLogger(ObservationDistributionServiceStub.class);

    private AuditTrail auditTrail;
    public static final int DEFAULT_MAX_SIZE = 10000;
    private final List<ObservationMessage> observedMessages;
    private boolean isConnected = false;
    private long numberOfMessagesObserved = 0;
    private long numberOfMessagesFailed = 0;
    private long numberOfMessagesPublished = 0;
    private Instant whenLastMessageDistributed = null;

    public ObservationDistributionServiceStub(AuditTrail auditTrail) {
        this(DEFAULT_MAX_SIZE, auditTrail);
    }

    public ObservationDistributionServiceStub(int maxSize, AuditTrail auditTrail) {
        observedMessages = new LimitedArrayList(maxSize);
        this.auditTrail = auditTrail;
    }

    @Override
    public String getName() {
        return "ObservationDistributionServiceStub";
    }

    @Override
    public void openConnection() {
        log.info("Opening Connection to Stub");
        isConnected = true;
    }

    @Override
    public void closeConnection() {
        log.info("Closing Connection to Stub");
        isConnected = false;
    }

    @Override
    public boolean isConnectionEstablished() {
        return isConnected;
    }

    @Override
    public void publish(ObservationMessage message) {
        boolean added = observedMessages.add(message);
        if (added) {
            log.trace("Added 1 message");
            numberOfMessagesObserved ++;
            whenLastMessageDistributed = Instant.now();
            String twinId = message.getSensorId();
            auditTrail.logObsevationDistributed(twinId,"ObservationDistributionServiceStub");
        } else {
            observedMessages.remove(0);
            publish(message);
        }
    }

    @Override
    public long getNumberOfMessagesObserved() {
        return numberOfMessagesObserved;
    }


    @Override
    public List<ObservationMessage> getObservedMessages() {
        return observedMessages;
    }

    @Override
    public long getNumberOfMessagesFailed() {
        return numberOfMessagesFailed;
    }

    @Override
    public void initialize(Properties properties) {

    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    @Override
    public long getNumberOfMessagesPublished() {
        return numberOfMessagesObserved;
    }

    @Override
    public long getNumberOfMessagesInQueue() {
        return 0;
    }

    @Override
    public Instant getWhenLastMessageDistributed() {
        return whenLastMessageDistributed;
    }
}

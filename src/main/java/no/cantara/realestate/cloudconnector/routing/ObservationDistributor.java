package no.cantara.realestate.cloudconnector.routing;

import no.cantara.realestate.cloudconnector.mappedid.MappedIdRepository;
import no.cantara.realestate.cloudconnector.semantics.ObservationMapper;
import no.cantara.realestate.observations.ObservationMessage;
import no.cantara.realestate.observations.ObservedValue;
import no.cantara.realestate.plugins.distribution.DistributionService;
import no.cantara.realestate.sensors.MappedSensorId;
import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.UniqueKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ObservationDistributor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ObservationDistributor.class);
    private final ObservationsRepository observationsRepository;
    private final List<DistributionService> distributionServices;
    private final MappedIdRepository mappedIdRepository;
    private static final long DEFAULT_SLEEP_PERIOD_MS = 100;
    private long sleepPeriod;
    private long observedValueDistributedCount;

    public ObservationDistributor(ObservationsRepository observationsRepository, List<DistributionService> distributionServices, MappedIdRepository mappedIdRepository) {
        this.observationsRepository = observationsRepository;
        this.distributionServices = distributionServices;
        this.mappedIdRepository = mappedIdRepository;
        sleepPeriod = DEFAULT_SLEEP_PERIOD_MS;
    }

    @Override
    public void run() {
        log.info("Starting ObservationDistributor");
        do {
            while (observationsRepository.hasObservedValues()) {
                ObservedValue observedValue = observationsRepository.takeFirstObservedValue();
                addSemanticsAndDistribute(observedValue);
            }
            try {
                Thread.sleep(sleepPeriod);
            } catch (InterruptedException e) {
                //Interupted sleep. No probblem, and ignored.
            }
        } while (true);

    }

    protected void addSemanticsAndDistribute(ObservedValue observedValue) {
        log.trace("Fetched observedValue {} from the queue", observedValue);

        SensorId sensorId = observedValue.getSensorId();
        UniqueKey uniqueKey = sensorId.getMappingKey();
        List<MappedSensorId> mappedIds = mappedIdRepository.find(uniqueKey);
        if (mappedIds.size() > 1) {
            log.info("Found {} mappedIds for uniqueKey {}", mappedIds.size(), uniqueKey);
            return;
        } else if (mappedIds.size() == 0) {
            log.trace("No mappedIds found for uniqueKey {}", uniqueKey);
            return;
        }
        MappedSensorId mappedSensorId = mappedIds.get(0);
        log.trace("Found mappedSensorId {} for uniqueKey {}", mappedSensorId, uniqueKey);
        ObservationMessage observationMessage = ObservationMapper.buildRecObservation(mappedSensorId, observedValue);
        for (DistributionService distributionService : distributionServices) {
            distributionService.publish(observationMessage);
        }
        addObservedValueDistributedCount();

    }

    protected void addObservedValueDistributedCount() {
        if (observedValueDistributedCount == Integer.MAX_VALUE) {
            observedValueDistributedCount = 0;
        }
        observedValueDistributedCount++;
    }

    public long getObservedValueDistributedCount() {
        return observedValueDistributedCount;
    }

    public boolean isHealthy() {
        //TODO Should check that all distributionServices are healthy, and that the queue is not growing.
        return true;
    }
}

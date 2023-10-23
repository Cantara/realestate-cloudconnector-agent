package no.cantara.realestate.cloudconnector.routing;

import no.cantara.realestate.mappingtable.repository.MappedIdRepository;
import no.cantara.realestate.observations.ObservedValue;
import no.cantara.realestate.plugins.distribution.DistributionService;
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
        //FIXME: Add semantics
        //FIXME: return getHealth and getDistributedValuesCount
    }
}

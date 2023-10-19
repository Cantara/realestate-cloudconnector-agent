package no.cantara.realestate.cloudconnector.observations;

import no.cantara.config.ApplicationProperties;
import no.cantara.realestate.cloudconnector.routing.MessageRouter;
import no.cantara.realestate.observations.ObservationMessage;
import no.cantara.realestate.plugins.distribution.DistributionService;
import no.cantara.realestate.plugins.ingestion.IngestionService;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class ScheduledObservationMessageRouter implements MessageRouter {
    private static final Logger log = getLogger(ScheduledObservationMessageRouter.class);
    private final List<DistributionService> distributionServices;
    private final List<IngestionService> ingestionServices;

    private final int DEFAULT_SECONDS_BETWEEN_SCHEDULED_IMPORT_RUNS = 60 * 10;
    public static final String IMPORT_SCHEDULE_MINUTES_KEY = "import_schedule_minutes";
    private static boolean scheduled_import_started = false;
    private static boolean scheduled_import_running = false;
    private final int SECONDS_BETWEEN_SCHEDULED_IMPORT_RUNS;
    private final ApplicationProperties config;
    private final Properties properties;

    public ScheduledObservationMessageRouter(ApplicationProperties config, List<DistributionService> distributionServices, List<IngestionService> ingestionServices) {
        this.distributionServices = distributionServices;
        this.ingestionServices = ingestionServices;
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        this.config = config;
        this.properties = new Properties();
        this.properties.putAll(config.map());
        Integer scheduleMinutes = findScheduledMinutes(config);
        if (scheduleMinutes != null) {
            SECONDS_BETWEEN_SCHEDULED_IMPORT_RUNS = scheduleMinutes * 60;
        } else {
            SECONDS_BETWEEN_SCHEDULED_IMPORT_RUNS = DEFAULT_SECONDS_BETWEEN_SCHEDULED_IMPORT_RUNS;
        }
    }

    private Integer findScheduledMinutes(ApplicationProperties config) {
        Integer scheduleMinutes = null;
        String scheduleMinutesValue = config.get(IMPORT_SCHEDULE_MINUTES_KEY);
        if (scheduleMinutesValue != null) {
            try {
                scheduleMinutes = Integer.valueOf(scheduleMinutesValue);
            } catch (NumberFormatException nfe) {
                log.debug("Failed to create scheduledMinutes from [{}]", scheduleMinutesValue);
            }
        }
        return scheduleMinutes;
    }

    @Override
    public void start() {
        if (!scheduled_import_started) {
            log.info("Starting ScheduledImportManager");

            scheduled_import_started = true;
            ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
            for (DistributionService distributionService : distributionServices) {
                try {
                    if (!distributionService.isInitialized()) {
                        log.info("Initializing distributionService {}", distributionService.getName());
                        distributionService.initialize(properties);
                    }
                } catch (Exception e) {
                    log.error("Exception trying to initialize distributionService {}. Reason: {}", distributionService, e.getMessage());
                }
            }
            for (IngestionService ingestionService : ingestionServices) {
                try {
                    if (!ingestionService.isInitialized()) {
                        log.info("Initializing ingestionService {}", ingestionService.getName());
                        ingestionService.initialize(properties);
                    }
                } catch (Exception e) {
                    log.error("Exception trying to initialize ingestionService {}. Reason: {}", ingestionService, e.getMessage());
                }
            }
            Runnable task1 = () -> {
                if (scheduled_import_running == false) {
                    log.info("Running an new import round.");
                    for (IngestionService ingestionService : ingestionServices) {
                        try {
                            scheduled_import_running = true;
                            List<ObservationMessage> observationMessages = ingestionService.readAll();
                            for (DistributionService distributionService : distributionServices) {
                                try {
                                    for (ObservationMessage observationMessage : observationMessages) {
                                        if (distributionService.publish(observationMessage)) {
                                            log.debug("Published observationMessage {} to distributionService {}", observationMessage, distributionService.getName());
                                        } else {
                                            log.warn("Failed to publish observationMessage {} to distributionService {}", observationMessage, distributionService.getName());
                                        }
                                    }
                                } catch (Exception e) {
                                    log.error("Exception trying to publish observationMessage to distributionService {}. Reason: {}", distributionService, e.getMessage());
                                }
                            }
                            log.info("Imported {} observations from {}. ", observationMessages.size(), ingestionService.getName());
                            scheduled_import_running = false;
                        } catch (Exception e) {
                            log.info("Exception trying to run scheduled imports of observations for {}. Reason: {}", ingestionService.getName(), e.getMessage());
                            scheduled_import_running = false;
                        }
                    }
                    log.info("Now waiting {} seconds for next scheduled run at: {}", SECONDS_BETWEEN_SCHEDULED_IMPORT_RUNS, Instant.now().plusSeconds(SECONDS_BETWEEN_SCHEDULED_IMPORT_RUNS));
                } else {
                    log.info("Last round of imports has not finished yet. ");
                }
            };

            // init Delay = 5, repeat the task every 60 second
            ScheduledFuture<?> scheduledFuture = ses.scheduleAtFixedRate(task1, 5, SECONDS_BETWEEN_SCHEDULED_IMPORT_RUNS, TimeUnit.SECONDS);
        } else {
            log.info("ScheduledImportManager is is already started");
        }
    }

    @Override
    public void stop() {

    }
}

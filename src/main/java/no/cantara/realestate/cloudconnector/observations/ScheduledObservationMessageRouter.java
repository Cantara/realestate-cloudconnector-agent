package no.cantara.realestate.cloudconnector.observations;

import no.cantara.config.ApplicationProperties;
import no.cantara.realestate.cloudconnector.routing.MessageRouter;
import no.cantara.realestate.observations.ObservationListener;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.plugins.ingestion.IngestionService;
import no.cantara.realestate.plugins.ingestion.PresentValueIngestionService;
import no.cantara.realestate.plugins.ingestion.TrendsIngestionService;
import no.cantara.realestate.plugins.notifications.NotificationListener;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class ScheduledObservationMessageRouter implements MessageRouter {
    private static final Logger log = getLogger(ScheduledObservationMessageRouter.class);
    private final List<IngestionService> ingestionServices;
    private final ObservationListener observationListener;

    private final int DEFAULT_SECONDS_BETWEEN_SCHEDULED_IMPORT_RUNS = 60 * 10;
    public static final String INGESTION_INTERVAL_MINUTES = "ingestion.interval.minutes";

    private static boolean scheduled_import_started = false;
    private static boolean scheduled_import_running = false;
    private final int ingestionIntervalSec;
    private final ApplicationProperties config;
    private final PluginConfig pluginConfig;
    private final NotificationListener notificationListener;

    public ScheduledObservationMessageRouter(ApplicationProperties config, List<IngestionService> ingestionServices, ObservationListener observationListener, NotificationListener notificationListener) {
        this.observationListener = observationListener;
        this.ingestionServices = ingestionServices;
        this.notificationListener = notificationListener;
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        this.config = config;
        this.pluginConfig = PluginConfig.fromMap(config.map());
        Integer scheduleMinutes = findScheduledMinutes(config);
        if (scheduleMinutes != null) {
            ingestionIntervalSec = scheduleMinutes * 60;
        } else {
            ingestionIntervalSec = DEFAULT_SECONDS_BETWEEN_SCHEDULED_IMPORT_RUNS;
        }
    }


    private Integer findScheduledMinutes(ApplicationProperties config) {
        Integer scheduleMinutes = null;
        String scheduleMinutesValue = config.get(INGESTION_INTERVAL_MINUTES);
        if (scheduleMinutesValue == null) {
            scheduleMinutesValue = config.get("import_schedule_minutes");
        }
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

            for (IngestionService ingestionService : ingestionServices) {
                try {
                    if (!ingestionService.isInitialized()) {
                        log.info("Initializing ingestionService {}", ingestionService.getName());
                        boolean initializedOk = ingestionService.initialize(pluginConfig);
                        if (initializedOk) {
                            ingestionService.openConnection(observationListener, notificationListener);
                        } else {
                            log.warn("Failed to initialize ingestionService {}", ingestionService.getName());
                        }

                    }
                } catch (Exception e) {
                    log.error("Exception trying to initialize ingestionService {}. Reason: {}", ingestionService, e.getMessage());
                }
            }
            Runnable task1 = () -> {
                    log.info("Request a new ingestion round for {} ingestion services.", ingestionServices.size());
                    for (IngestionService ingestionService : ingestionServices) {
                        log.debug("Request a new ingestion round for {} ingestion service.", ingestionService.getName());
                        try {
                            log.trace("Running...ingest for {} ", ingestionService.getName());
                            if (ingestionService instanceof PresentValueIngestionService) {
                                ((PresentValueIngestionService)ingestionService).ingestPresentValues();
                            } else if (ingestionService instanceof TrendsIngestionService) {
                                ((TrendsIngestionService)ingestionService).ingestTrends();
                            }

                        } catch (Exception e) {
                            log.info("Exception trying to run scheduled imports of observations for {}. Reason: {}", ingestionService.getName(), e.getMessage());
                        }
                    }
                    log.info("Now waiting {} seconds for next scheduled run at: {}", ingestionIntervalSec, Instant.now().plusSeconds(ingestionIntervalSec));
            };

            // init Delay = 5, repeat the task every 60 second
            ScheduledFuture<?> scheduledFuture = ses.scheduleAtFixedRate(task1, 5, ingestionIntervalSec, TimeUnit.SECONDS);
        } else {
            log.info("ScheduledImportManager is is already started");
        }
    }

    @Override
    public void stop() {

    }
}

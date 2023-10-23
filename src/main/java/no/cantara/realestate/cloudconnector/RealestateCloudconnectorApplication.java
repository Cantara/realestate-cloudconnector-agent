package no.cantara.realestate.cloudconnector;

import no.cantara.config.ApplicationProperties;
import no.cantara.realestate.SensorId;
import no.cantara.realestate.cloudconnector.notifications.NotificationService;
import no.cantara.realestate.cloudconnector.notifications.SlackNotificationService;
import no.cantara.realestate.cloudconnector.observations.ScheduledObservationMessageRouter;
import no.cantara.realestate.cloudconnector.routing.MessageRouter;
import no.cantara.realestate.cloudconnector.routing.ObservationReceiver;
import no.cantara.realestate.cloudconnector.sensors.simulated.SimulatedCo2Sensor;
import no.cantara.realestate.cloudconnector.sensors.simulated.SimulatedTempSensor;
import no.cantara.realestate.plugins.distribution.DistributionService;
import no.cantara.realestate.plugins.ingestion.IngestionService;
import no.cantara.stingray.application.AbstractStingrayApplication;
import no.cantara.stingray.application.health.StingrayHealthService;
import no.cantara.stingray.security.StingraySecurity;
import org.slf4j.Logger;

import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

public class RealestateCloudconnectorApplication extends AbstractStingrayApplication<RealestateCloudconnectorApplication> {
    private static final Logger log = getLogger(RealestateCloudconnectorApplication.class);
    private boolean enableStream;
    private boolean enableScheduledImport;
    private NotificationService notificationService;
    private Map<String, DistributionService> distributionServices;

    private Map<String, IngestionService> ingestionServices;
    private ObservationReceiver observationReceiver;

    public RealestateCloudconnectorApplication(ApplicationProperties config) {
        super("RealestateCloudconnector",
                readMetaInfMavenPomVersion("no.cantara.realestate", "realestate-cloudconnector-agent"),
                config
        );
    }


    public static void main(String[] args) {
        ApplicationProperties config = new RealestateCloudconnectorApplicationFactory()
                .conventions(ApplicationProperties.builder())
                .buildAndSetStaticSingleton();

        try {
            RealestateCloudconnectorApplication application = new RealestateCloudconnectorApplication(config).init().start();
            log.info("Server started. See status on {}:{}{}/health", "http://localhost", config.get("server.port"), config.get("server.context-path"));
//            application.startImportingObservations();
        } catch (Exception e) {
            log.error("Failed to start RealestateCloudconnectorApplication", e);
        }

    }

    /*
    private void startImportingObservations() {
        if (enableScheduledImport) {
            get(ScheduledImportManager.class).startScheduledImportOfTrendIds();
        }
    }
    */



    @Override
    protected void doInit() {
        initBuiltinDefaults();
        StingraySecurity.initSecurity(this);

        initNotificationServices();
        initObservationReceiver();
        initDistributionController();
        initIngestionController();
        subscribeToSimulatedSensors();
        initRouter();

        /*
        boolean doImportData = config.asBoolean("import.data");
        enableStream = config.asBoolean("sd.stream.enabled");
        enableScheduledImport = config.asBoolean("sd.scheduledImport.enabled");
//        SdClient sdClient = createSdClient(config);

        ServiceLoader<ObservationDistributionClient> observationDistributionClients = ServiceLoader.load(ObservationDistributionClient.class);
        ObservationDistributionClient observationDistributionClient = null;
        for (ObservationDistributionClient distributionClient : observationDistributionClients) {
            if (distributionClient != null && distributionClient instanceof AzureObservationDistributionClient) {
                log.info("Found implementation of ObservationDistributionClient on classpath: {}", distributionClient.toString());
                observationDistributionClient = distributionClient;
            }
            get(StingrayHealthService.class).registerHealthProbe(observationDistributionClient.getName() +"-isConnected: ", observationDistributionClient::isConnectionEstablished);
            get(StingrayHealthService.class).registerHealthProbe(observationDistributionClient.getName() +"-numberofMessagesObserved: ", observationDistributionClient::getNumberOfMessagesObserved);
        }
        if (observationDistributionClient == null) {
            log.warn("No implementation of ObservationDistributionClient was found on classpath. Creating a ObservationDistributionServiceStub explicitly.");
            observationDistributionClient = new ObservationDistributionServiceStub();
            get(StingrayHealthService.class).registerHealthProbe(observationDistributionClient.getName() +"-isConnected: ", observationDistributionClient::isConnectionEstablished);
            get(StingrayHealthService.class).registerHealthProbe(observationDistributionClient.getName() +"-numberofMessagesDistributed: ", observationDistributionClient::getNumberOfMessagesObserved);
        }
        observationDistributionClient.openConnection();
        log.info("Establishing and verifying connection to Azure.");
        if (observationDistributionClient.isConnectionEstablished()) {
            ObservationMessage stubMessage = ObservationMesstageStubs.buildStubObservation();
            observationDistributionClient.publish(stubMessage);
        }
        String measurementsName = config.get("measurements.name");
        MetricsDistributionClient metricsDistributionClient = new MetricsDistributionServiceStub(measurementsName);
        MappedIdRepository mappedIdRepository = init(MappedIdRepository.class, () -> createMappedIdRepository(doImportData));
        ObservationDistributionClient finalObservationDistributionClient = observationDistributionClient;
        ScheduledImportManager scheduledImportManager = init(ScheduledImportManager.class, () -> wireScheduledImportManager(sdClient, finalObservationDistributionClient, metricsDistributionClient, mappedIdRepository));
        ObservationDistributionResource observationDistributionResource = initAndRegisterJaxRsWsComponent(ObservationDistributionResource.class, () -> createObservationDistributionResource(finalObservationDistributionClient));

        get(StingrayHealthService.class).registerHealthProbe("mappedIdRepository.size", mappedIdRepository::size);
        get(StingrayHealthService.class).registerHealthProbe(sdClient.getName() + "-isHealthy: ", sdClient::isHealthy);
        get(StingrayHealthService.class).registerHealthProbe(sdClient.getName() + "-isLogedIn: ", sdClient::isLoggedIn);
        get(StingrayHealthService.class).registerHealthProbe(sdClient.getName() + "-numberofTrendsSamples: ", sdClient::getNumberOfTrendSamplesReceived);
        get(StingrayHealthService.class).registerHealthProbe("observationDistribution.message.count", observationDistributionResource::getDistributedCount);
        //Random Example
        init(Random.class, this::createRandom);
        RandomizerResource randomizerResource = initAndRegisterJaxRsWsComponent(RandomizerResource.class, this::createRandomizerResource);
*/
        //Wire up the stream importer
        

    }

    private void subscribeToSimulatedSensors() {
        List<SensorId> sensorIds = new ArrayList<>();
        sensorIds.add(new SimulatedCo2Sensor("1"));
        sensorIds.add(new SimulatedTempSensor("2"));
        for (IngestionService ingestionService : ingestionServices.values()) {
            ingestionService.addSubscriptions(sensorIds);
        }
    }

    void initRouter() {
        List<IngestionService> ingestors = new ArrayList<>();
        for (IngestionService ingestionService : ingestionServices.values()) {
            ingestors.add(ingestionService);
        }
        MessageRouter messageRouter = new ScheduledObservationMessageRouter(config, ingestors, observationReceiver);
        messageRouter.start();
    }

    void initIngestionController() {
        ServiceLoader<IngestionService> ingestionServicesFound = ServiceLoader.load(IngestionService.class);

        ingestionServices = new HashMap<>();
        for (IngestionService service : ingestionServicesFound) {
            log.info("I've found a Ingestion service called '" + service.getName() + "' !");
            ingestionServices.put(service.getName(), service);
            get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-isHealthy: ", service::isHealthy);
            get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-subscriptionsCount: ", service::getSubscriptionsCount);
            get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-numberofObservationsIngested: ", service::getNumberOfMessagesImported);
            get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-numberofFailedIngestions: ", service::getNumberOfMessagesFailed);
        }
        log.info("Found " + ingestionServices.size() + " ingestion services!");
    }

    protected void initDistributionController() {
        ServiceLoader<DistributionService> distributionServicesFound = ServiceLoader.load(DistributionService.class);

        distributionServices = new HashMap<>();
        for (DistributionService service : distributionServicesFound) {
            log.info("I've found a  Distribution service called '" + service.getName() + "' !");
            distributionServices.put(service.getName(), service);
            get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-isHealthy: ", service::isHealthy);
            get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-numberofObservationsIngested: ", service::getNumberOfMessagesPublished);
            get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-numberofFailedIngestions: ", service::getNumberOfMessagesFailed);
        }
        log.info("Found " + distributionServices.size() + " distribution services!");
    }

    private void initNotificationServices() {
        ServiceLoader<NotificationService> notificationServices = ServiceLoader.load(NotificationService.class);
        if (notificationServices != null && notificationServices.iterator().hasNext()) {
            notificationService = notificationServices.findFirst().orElse(null);
            log.trace("Alerts and Warnings will be sent with NotificationService: {}", notificationService);
        } else {
            log.warn("ServiceLoader could not find any implementation of NotificationService. Using SlackNotificationService.");
            notificationService = new SlackNotificationService();
        }
    }
    private void initObservationReceiver() {
        observationReceiver = new ObservationReceiver();
        get(StingrayHealthService.class).registerHealthProbe("ObservationReceiver-isHealthy: ", observationReceiver::isHealthy);
        get(StingrayHealthService.class).registerHealthProbe("ObservationReceiver-ObservedValues-received: ", observationReceiver::getObservedValueCount);
        get(StingrayHealthService.class).registerHealthProbe("ObservationReceiver-ConfigValues-received: ", observationReceiver::getObservedConfigValueCount);
        get(StingrayHealthService.class).registerHealthProbe("ObservationReceiver-ConfigMessages-received: ", observationReceiver::getObservedConfigMessageCount);
    }



}

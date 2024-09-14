package no.cantara.realestate.cloudconnector;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import no.cantara.config.ApplicationProperties;
import no.cantara.realestate.cloudconnector.sensorid.InMemorySensorIdRepository;
import no.cantara.realestate.cloudconnector.sensorid.SensorIdRepository;
import no.cantara.realestate.cloudconnector.simulators.ingestion.SimulatorPresentValueIngestionService;
import no.cantara.realestate.cloudconnector.simulators.ingestion.SimulatorTrendsIngestionService;
import no.cantara.realestate.cloudconnector.mappedid.MappedIdRepository;
import no.cantara.realestate.cloudconnector.mappedid.MappedIdRepositoryImpl;
import no.cantara.realestate.cloudconnector.notifications.NotificationService;
import no.cantara.realestate.cloudconnector.notifications.SlackNotificationService;
import no.cantara.realestate.cloudconnector.observations.ScheduledObservationMessageRouter;
import no.cantara.realestate.cloudconnector.routing.MessageRouter;
import no.cantara.realestate.cloudconnector.routing.ObservationDistributor;
import no.cantara.realestate.cloudconnector.routing.ObservationsRepository;
import no.cantara.realestate.cloudconnector.simulators.sensors.SimulatedCo2Sensor;
import no.cantara.realestate.cloudconnector.simulators.sensors.SimulatedTempSensor;
import no.cantara.realestate.cloudconnector.status.HealthListener;
import no.cantara.realestate.cloudconnector.status.MappedIdRepositoryResource;
import no.cantara.realestate.cloudconnector.status.SensorIdsRepositoryResource;
import no.cantara.realestate.cloudconnector.status.SystemStatusResource;
import no.cantara.realestate.plugins.RealEstatePluginFactory;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.plugins.distribution.DistributionService;
import no.cantara.realestate.plugins.ingestion.IngestionService;
import no.cantara.realestate.semantics.rec.SensorRecObject;
import no.cantara.realestate.sensors.*;
import no.cantara.realestate.sensors.tfm.Tfm;
import no.cantara.stingray.application.AbstractStingrayApplication;
import no.cantara.stingray.application.health.StingrayHealthService;
import no.cantara.stingray.security.StingraySecurity;
import org.slf4j.Logger;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

public class RealestateCloudconnectorApplication extends AbstractStingrayApplication<RealestateCloudconnectorApplication> {
    private static final Logger log = getLogger(RealestateCloudconnectorApplication.class);
    public static final Logger auditLog = getLogger("AuditLog");
    private boolean enableStream;
    private boolean enableScheduledImport;

    private HealthListener notificationListener;
    private NotificationService notificationService;
    private Map<String, DistributionService> distributionServices;

    private Map<String, IngestionService> ingestionServices;
    private ObservationsRepository observationsRepository;
    private ObservationDistributor observationDistributor;
    private MappedIdRepository mappedIdRepository;
    private SensorIdRepository sensorIdRepository;
    private Thread observationDistributorThread;
    private MetricRegistry metricRegistry;

    public RealestateCloudconnectorApplication(ApplicationProperties config) {
        super("RealestateCloudconnector",
                readMetaInfMavenPomVersion("no.cantara.realestate", "realestate-cloudconnector-agent"),
                config
        );
    }

    public RealestateCloudconnectorApplication(ApplicationProperties config, String groupId, String artifactId) {
        super("RealestateCloudconnector",
                readMetaInfMavenPomVersion(groupId, artifactId),
                config
        );
    }


    public static void main(String[] args) {
        ApplicationProperties config = new RealestateCloudconnectorApplicationFactory()
                .conventions(ApplicationProperties.builder())
                .buildAndSetStaticSingleton();

        try {
            RealestateCloudconnectorApplication application = new RealestateCloudconnectorApplication(config).init().start();
            String baseUrl = "http://localhost:"+config.get("server.port")+config.get("server.context-path");
            log.info("Server started. See status on {}/health", baseUrl);
            log.info("   SensorIds: {}/sensorids/status", baseUrl);
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

    public void importSensorIds() {

    }


    @Override
    protected void doInit() {
        boolean useSimulatedSensors = config.asBoolean("sensormappings.simulator.enabled");
        initBuiltinDefaults();
        StingraySecurity.initSecurity(this);
        metricRegistry = get(MetricRegistry.class);
        if (metricRegistry == null) {
            throw new RealestateCloudconnectorException("Missing Metric Registry");
        }
        mappedIdRepository = createMappedIdRepository(useSimulatedSensors);
        sensorIdRepository = createSensorIdRepository(useSimulatedSensors);
        put(SensorIdRepository.class, sensorIdRepository);
        initNotificationServices();
        initObservationReceiver();
        initDistributionController();
        initPluginFactories();
        initIngestionController();
        subscribeToSensors(useSimulatedSensors);
        initRouter();
        initObservationDistributor();

        //Setup Metrics observation
        initMetrics();
        initSafeShutdownMetrics();

        //StatusGui
        init(Random.class, this::createRandom);
        SystemStatusResource systemStatusResource = initAndRegisterJaxRsWsComponent(SystemStatusResource.class, this::createSystemStatusResource);
        MappedIdRepositoryResource mappedIdRepositoryResource = initAndRegisterJaxRsWsComponent(MappedIdRepositoryResource.class, this::createMappedIdRepositoryStatusResource);
        SensorIdsRepositoryResource sensorIdRepositoryResource = initAndRegisterJaxRsWsComponent(SensorIdsRepositoryResource.class, this::createSensorIdRepositoryResource);
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
            get(StingrayHealthService.class).registerHealthProbe(observationDistributionClient.getName() +"-isConnected", observationDistributionClient::isConnectionEstablished);
            get(StingrayHealthService.class).registerHealthProbe(observationDistributionClient.getName() +"-numberofMessagesObserved", observationDistributionClient::getNumberOfMessagesObserved);
        }
        if (observationDistributionClient == null) {
            log.warn("No implementation of ObservationDistributionClient was found on classpath. Creating a ObservationDistributionServiceStub explicitly.");
            observationDistributionClient = new ObservationDistributionServiceStub();
            get(StingrayHealthService.class).registerHealthProbe(observationDistributionClient.getName() +"-isConnected", observationDistributionClient::isConnectionEstablished);
            get(StingrayHealthService.class).registerHealthProbe(observationDistributionClient.getName() +"-numberofMessagesDistributed", observationDistributionClient::getNumberOfMessagesObserved);
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
        get(StingrayHealthService.class).registerHealthProbe(sdClient.getName() + "-isHealthy", sdClient::isHealthy);
        get(StingrayHealthService.class).registerHealthProbe(sdClient.getName() + "-isLogedIn", sdClient::isLoggedIn);
        get(StingrayHealthService.class).registerHealthProbe(sdClient.getName() + "-numberofTrendsSamples", sdClient::getNumberOfTrendSamplesReceived);
        get(StingrayHealthService.class).registerHealthProbe("observationDistribution.message.count", observationDistributionResource::getDistributedCount);
        //Random Example
        init(Random.class, this::createRandom);
        RandomizerResource randomizerResource = initAndRegisterJaxRsWsComponent(RandomizerResource.class, this::createRandomizerResource);
*/
        //Wire up the stream importer

        importSensorIds();
    }

    private void initMetrics() {
        metricRegistry.register(MetricRegistry.name(ObservationsRepository.class, "ObservationsQueue", "size"),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return observationsRepository.getObservedValuesQueueSize();
                    }
                });
        metricRegistry.register(MetricRegistry.name(MappedIdRepositoryImpl.class, "MappedIdRepository", "size"),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return mappedIdRepository.size();
                    }
                });
    }

    private void initSafeShutdownMetrics() {
        metricRegistry.register(MetricRegistry.name("SafeToShutdown"), new Gauge<Boolean>() {
            @Override
            public Boolean getValue() {
                return observationsRepository.getObservedValuesQueueSize() < 1;
            }
        });
    }

    private MappedIdRepositoryResource createMappedIdRepositoryStatusResource() {
        TemplateEngine templateEngine = new TemplateEngine();
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setPrefix("/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false); // Set to true for production
        templateEngine.setTemplateResolver(templateResolver);
        return new MappedIdRepositoryResource(templateEngine, mappedIdRepository);
    }

    private SensorIdsRepositoryResource createSensorIdRepositoryResource() {
        TemplateEngine templateEngine = new TemplateEngine();
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setPrefix("/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false); // Set to true for production
        templateEngine.setTemplateResolver(templateResolver);
        return new SensorIdsRepositoryResource(templateEngine, sensorIdRepository);
    }
    //createSensorIdRepositoryResource

    protected void subscribeToSensors(boolean useSimulatedSensors) {

        if (useSimulatedSensors) {
            List<SensorId> simulatedSensorIds = new ArrayList<>();
            SensorId simulatedCo2Sensor = new SimulatedCo2Sensor("1");
            simulatedSensorIds.add(simulatedCo2Sensor);
            SensorId simulatedTempSensor = new SimulatedTempSensor("2");
            simulatedSensorIds.add(simulatedTempSensor);
            for (IngestionService ingestionService : ingestionServices.values()) {
                ingestionService.addSubscriptions(simulatedSensorIds);
            }
        } else {
            for (IngestionService ingestionService : ingestionServices.values()) {
                List<SensorId> sensorIds = new ArrayList<>();
                List<MappedSensorId> mappedSensorIds = findSensorsToSubscribeTo(ingestionService.getName(), ingestionService.getClass());
                log.debug("Adding {} sensorIds from ingestionService: {}", mappedSensorIds.size(), ingestionService.getName());
                for (MappedSensorId mappedSensorId : mappedSensorIds) {
                    log.debug("Subscribe to sensorId: {}", mappedSensorId.getSensorId());
                    sensorIds.add(mappedSensorId.getSensorId());
                }
                ingestionService.addSubscriptions(sensorIds);
            }
        }
    }

    protected List<MappedSensorId> findSensorsToSubscribeTo(String ingestionServiceName, Class<? extends IngestionService> ingestionClass) {
        //FIXME need propper implementation with query bassed on ingestionServiceName
        String realestatesToImport = config.get("importsensorsQuery.realestates");
        List<MappedSensorId> mappedSensorIds = new ArrayList<>();
        List<String> importAllFromRealestates = findListOfRealestatesToImportFrom();
        log.info("Importallres: {}", realestatesToImport);
        if (importAllFromRealestates != null && importAllFromRealestates.size() > 0) {
            for (String realestate : importAllFromRealestates) {
                MappedIdQuery idQuery = new MappedIdQueryBuilder().realEstate(realestate).build();
                log.debug("Querying for MappedSensorIds with query: {} from realestate: {}", idQuery, realestate);
                try {
                    List<MappedSensorId> sensorIds = mappedIdRepository.find(idQuery);
                    log.debug("Found {} MappedSensorIds from realestate: {}", sensorIds.size(), realestate);
                    if (sensorIds != null) {
                        mappedSensorIds.addAll(sensorIds);
                    }
                } catch (Exception e) {
                    log.warn("Failed to find MappedSensorIds from realestate: {}", realestate, e);
                }
            }
        }

        return mappedSensorIds;
    }

    public static SensorRecObject buildRecStub(String roomName, SensorType sensorType) {
        SensorRecObject recObject = new SensorRecObject(UUID.randomUUID().toString());
        recObject.setTfm(new Tfm(roomName + "-" + sensorType.name()));
        recObject.setRealEstate("TestRealEstate");
        recObject.setBuilding("TestBuilding");
        recObject.setFloor("1");
        recObject.setServesRoom(roomName);
        recObject.setPlacementRoom(roomName);
        recObject.setSensorType(sensorType.name());
        return recObject;
    }

    void initRouter() {
        List<IngestionService> ingestors = new ArrayList<>();
        for (IngestionService ingestionService : ingestionServices.values()) {
            ingestors.add(ingestionService);
        }
        MessageRouter messageRouter = new ScheduledObservationMessageRouter(config, ingestors, observationsRepository, notificationListener);
        messageRouter.start();
    }

    void initPluginFactories() {

        Map<String, String> propertiesMap = config.map();
        PluginConfig pluginConfig = PluginConfig.fromMap(propertiesMap);
        ServiceLoader<RealEstatePluginFactory> pluginFactories = ServiceLoader.load(RealEstatePluginFactory.class);
        for (RealEstatePluginFactory pluginFactory : pluginFactories) {
            log.info("I've found a pluginFactory called '" + pluginFactory.getDisplayName() + "' !");
            //#14 FIXME filter properties based on plugin Id
//            pluginConfig = PluginConfig.fromMap(config.subMap(pluginFactory.getId()));
            pluginFactory.initialize(pluginConfig);
            List<MappedSensorId> mappedSensorIds = pluginFactory.createSensorMappingImporter().importSensorMappings();
            log.debug("Adding {} sensorIds from pluginFactory: {}", mappedSensorIds.size(), pluginFactory.getDisplayName());
            mappedIdRepository.addAll(mappedSensorIds);
            List<IngestionService> pluginIngestionServices = pluginFactory.createIngestionServices(observationsRepository, notificationListener);
            log.debug("Found {} ingestion services from pluginFactory: {}", pluginIngestionServices.size(), pluginFactory.getDisplayName());
            for (IngestionService service : pluginIngestionServices) {
                log.info("{} has a Ingestion service called {}.", pluginFactory.getId(), service.getName());
                initIngestionService(service);
            }
        }
    }

    void initIngestionController() {
        ServiceLoader<IngestionService> ingestionServicesFound = ServiceLoader.load(IngestionService.class);

        boolean useIngestionSimulator = config.asBoolean("ingestion.simulator.enabled", false);
        for (IngestionService service : ingestionServicesFound) {
            log.info("ServiceLoader found a Ingestion service called '" + service.getName() + "' !");
            if (service instanceof SimulatorPresentValueIngestionService || service instanceof SimulatorTrendsIngestionService) {
                if (useIngestionSimulator) {
                    initIngestionService(service);
                }
            } else {
                initIngestionService(service);
            }
        }
        log.info("ServiceLoader found " + ingestionServices.size() + " ingestion services!");
    }

    private void initIngestionService(IngestionService service) {
        //FIXME how to initialize these when PluginFactories have added these.
        if (ingestionServices == null) {
            ingestionServices = new HashMap<>();
        }
        ingestionServices.put(service.getName(), service);
        get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-isHealthy", service::isHealthy);
        get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-subscriptionsCount", service::getSubscriptionsCount);
        get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-numberofObservationsImported", service::getNumberOfMessagesImported);
        get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-numberofFailedObservationImports", service::getNumberOfMessagesFailed);
        get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-whenLastObservationImported", service::getWhenLastMessageImported);
        metricRegistry.register(MetricRegistry.name(service.getName(), "SubscriptionsCount", "size"),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return service.getSubscriptionsCount();
                    }
                });
        metricRegistry.register(MetricRegistry.name(service.getName(), "ObservationsIngested", "size"),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return service.getNumberOfMessagesImported();
                    }
                });
        metricRegistry.register(MetricRegistry.name(service.getName(), "FailedObservationIngestions", "size"),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return service.getNumberOfMessagesFailed();
                    }
                });
    }

    protected void initDistributionController() {
        ServiceLoader<DistributionService> distributionServicesFound = ServiceLoader.load(DistributionService.class);

        distributionServices = new HashMap<>();
        for (DistributionService service : distributionServicesFound) {
            log.info("ServiceLoader found a Distribution service called {}!", service.getName());
            service.initialize(null);
            distributionServices.put(service.getName(), service);
            get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-isHealthy", service::isHealthy);
            get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-numberofObservationsDistributed", service::getNumberOfMessagesPublished);
            get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-numberofFailedDistributed", service::getNumberOfMessagesFailed);
            get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-numberofMessagesInQueue", service::getNumberOfMessagesInQueue);
            get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-whenLastMessageDistributed", service::getWhenLastMessageDistributed);
            get(StingrayHealthService.class).registerHealthCheck(service.getName() + "-isHealthy:", new HealthCheck() {
                @Override
                protected HealthCheck.Result check() throws Exception {
                    if (service.isHealthy()) {
                        return Result.healthy();
                    } else {
                        return Result.unhealthy(service.getName() + " Failed on connection or login ");
                    }
                }
            });
        }
        log.info("ServiceLoader found " + distributionServices.size() + " distribution services!");
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
        this.notificationListener = new HealthListener(notificationService);
    }

    private void initObservationReceiver() {
        observationsRepository = new ObservationsRepository(metricRegistry);
        get(StingrayHealthService.class).registerHealthProbe("ObservationsRepository-isHealthy", observationsRepository::isHealthy);
        get(StingrayHealthService.class).registerHealthProbe("ObservationsRepository-ObservedValues-received", observationsRepository::getObservedValueCount);
        get(StingrayHealthService.class).registerHealthProbe("ObservationsRepository-ObservedValuesQueue-size", observationsRepository::getObservedValuesQueueSize);
        get(StingrayHealthService.class).registerHealthProbe("ObservationsRepository-ConfigValues-received", observationsRepository::getObservedConfigValueCount);
        get(StingrayHealthService.class).registerHealthProbe("ObservationsRepository-ConfigMessages-received", observationsRepository::getObservedConfigMessageCount);
    }

    private void initObservationDistributor() {
        if (observationsRepository == null) {
            log.warn("ObservationsRepository is null. Cannot start ObservationDistributor");
            throw new RealestateCloudconnectorException("ObservationsRepository is null. Cannot start ObservationDistributor");
        }
        observationDistributor = new ObservationDistributor(observationsRepository, new ArrayList<>(distributionServices.values()), mappedIdRepository, metricRegistry);
        get(StingrayHealthService.class).registerHealthProbe("ObservationDistributor-isHealthy", observationDistributor::isHealthy);
        get(StingrayHealthService.class).registerHealthProbe("ObservationsRepository-ObservedValues-distributed", observationDistributor::getObservedValueDistributedCount);
        observationDistributorThread = new Thread(observationDistributor);
        observationDistributorThread.start();
        log.trace("Started ObservationDistributor thread");
        /*
          //Register health checks
            get(StingrayHealthService.class).registerHealthCheck(streamClient.getName() + ".isLoggedIn", new HealthCheck() {
                @Override
                protected Result check() throws Exception {
                    if (streamClient.isHealthy() && streamClient.isLoggedIn()) {
                        return Result.healthy();
                    } else {
                        return Result.unhealthy(streamClient.getName() + " is not logged in. ");
                    }
                }
            });
         */
    }


    protected MappedIdRepository createMappedIdRepository(boolean useSimulatedSensors) {
        MappedIdRepository mappedIdRepository = new MappedIdRepositoryImpl();
        get(StingrayHealthService.class).registerHealthProbe("MappedIdRepository-size", mappedIdRepository::size);
        if (useSimulatedSensors) {
            log.warn("Using simulated SensorId's");
            List<SensorId> sensorIds = new ArrayList<>();
            SensorId simulatedCo2Sensor = new SimulatedCo2Sensor("1");
            sensorIds.add(simulatedCo2Sensor);
            MappedSensorId mappedSimulatedCo2Sensor = new MappedSensorId(simulatedCo2Sensor, buildRecStub("room1", SensorType.co2));
            mappedIdRepository.add(mappedSimulatedCo2Sensor);
            SensorId simulatedTempSensor = new SimulatedTempSensor("2");
            sensorIds.add(simulatedTempSensor);
            MappedSensorId mappedSimulatedTempSensor = new MappedSensorId(simulatedTempSensor, buildRecStub("room1", SensorType.temp));
            mappedIdRepository.add(mappedSimulatedTempSensor);
        }

        return mappedIdRepository;
    }

    protected SensorIdRepository createSensorIdRepository(boolean useSimulatedSensors) {
        SensorIdRepository sensorIdRepository = new InMemorySensorIdRepository();
        get(StingrayHealthService.class).registerHealthProbe("SensorIdRepository-size", sensorIdRepository::size);
        if (useSimulatedSensors) {
            log.warn("Using simulated SensorId's");
            List<SensorId> sensorIds = new ArrayList<>();
            SensorId simulatedCo2Sensor = new SimulatedCo2Sensor("1");
            sensorIdRepository.add(simulatedCo2Sensor);
            SensorId simulatedTempSensor = new SimulatedTempSensor("2");
            sensorIdRepository.add(simulatedTempSensor);
        }

        return sensorIdRepository;
    }

    protected List<String> findListOfRealestatesToImportFrom() {
        List<String> realEstates = null;
        try {
            String reCsvSplitted = config.get("importsensorsQuery.realestates");
            if (reCsvSplitted != null) {
                realEstates = Arrays.asList(reCsvSplitted.split(","));
            }
        } catch (Exception e) {
            log.warn("Failed to read list of RealEstates used for import.", e);
        }
        return realEstates;
    }

    private Random createRandom() {
        return new Random(System.currentTimeMillis());
    }

    private SystemStatusResource createSystemStatusResource() {
        Random random = get(Random.class);
        return new SystemStatusResource(random);
    }


}

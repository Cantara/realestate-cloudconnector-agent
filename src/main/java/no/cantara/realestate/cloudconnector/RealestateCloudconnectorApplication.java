package no.cantara.realestate.cloudconnector;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import no.cantara.config.ApplicationProperties;
import no.cantara.realestate.azure.AzureObservationDistributionClient;
import no.cantara.realestate.cloudconnector.audit.AuditResource;
import no.cantara.realestate.cloudconnector.audit.AuditTrail;
import no.cantara.realestate.cloudconnector.audit.InMemoryAuditTrail;
import no.cantara.realestate.cloudconnector.metrics.AzureApplicationInsightsMetricsClient;
import no.cantara.realestate.cloudconnector.notifications.NotificationService;
import no.cantara.realestate.cloudconnector.notifications.SlackNotificationService;
import no.cantara.realestate.cloudconnector.observations.ScheduledObservationMessageRouter;
import no.cantara.realestate.cloudconnector.rec.RecRepositoryInMemory;
import no.cantara.realestate.cloudconnector.routing.MessageRouter;
import no.cantara.realestate.cloudconnector.routing.ObservationDistributor;
import no.cantara.realestate.cloudconnector.routing.ObservationsRepository;
import no.cantara.realestate.cloudconnector.sensorid.InMemorySensorIdRepository;
import no.cantara.realestate.cloudconnector.sensorid.SensorIdRepository;
import no.cantara.realestate.cloudconnector.simulators.distribution.ObservationDistributionResource;
import no.cantara.realestate.cloudconnector.simulators.distribution.ObservationDistributionServiceStub;
import no.cantara.realestate.cloudconnector.simulators.ingestion.SimulatorPresentValueIngestionService;
import no.cantara.realestate.cloudconnector.simulators.ingestion.SimulatorTrendsIngestionService;
import no.cantara.realestate.cloudconnector.simulators.sensors.SimulatedCo2Sensor;
import no.cantara.realestate.cloudconnector.simulators.sensors.SimulatedTempSensor;
import no.cantara.realestate.cloudconnector.status.*;
import no.cantara.realestate.distribution.ObservationDistributionClient;
import no.cantara.realestate.metrics.MetricsDistributionClient;
import no.cantara.realestate.observations.ObservationListener;
import no.cantara.realestate.plugins.distribution.DistributionService;
import no.cantara.realestate.plugins.ingestion.IngestionService;
import no.cantara.realestate.plugins.notifications.NotificationListener;
import no.cantara.realestate.rec.RecRepository;
import no.cantara.realestate.rec.RecTags;
import no.cantara.realestate.rec.SensorRecObject;
import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.SensorSystem;
import no.cantara.realestate.sensors.SensorType;
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
    private List<DistributionService> distributionServices;
    protected AuditTrail auditTrail;
    private Map<String, IngestionService> ingestionServices;
    private ObservationsRepository observationsRepository;
    private ObservationDistributor observationDistributor;
    private RecRepository recRepository;
    //    private MappedIdRepository mappedIdRepository;
    private SensorIdRepository sensorIdRepository;
    private Thread observationDistributorThread;
    protected MetricRegistry metricRegistry;
    protected MetricsDistributionClient metricsDistributionClient;

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
            String baseUrl = "http://localhost:" + config.get("server.port") + config.get("server.context-path");
            log.info("Server started. See status on {}/health", baseUrl);
            log.info("   SensorIds: {}/sensorids/status", baseUrl);
            log.info("   Recs: {}/rec/status", baseUrl);
            log.info("   Audit: {}/audit", baseUrl);
            log.info("   Distribution: {}/distribution", baseUrl);
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
        FaviconResource faviconResource = initAndRegisterJaxRsWsComponent(FaviconResource.class, this::createFaviconResource);
        initMetrics();
        initAuditTrail();
        recRepository = createRecRepository(useSimulatedSensors);
        put(RecRepository.class, recRepository);
        //Disable MappedIdRepository for now
//        mappedIdRepository = createMappedIdRepository(useSimulatedSensors);
//        put(MappedIdRepository.class, mappedIdRepository);
        sensorIdRepository = createSensorIdRepository(useSimulatedSensors);
        put(SensorIdRepository.class, sensorIdRepository);
        initNotificationServices();
        initObservationReceiver();
//        initDistributionController();
        initObservationDistributor();
        put(ObservationsRepository.class, observationsRepository);
//        Disable for now
//        initPluginFactories();
        //Move to implementations in Desigo and Metasys
        /*
        initIngestionController();
        importSensorIds();
        subscribeToSensors(useSimulatedSensors);
        initRouter();
        initObservationDistributor();
        */
//        initObservationDistributor();

        //Setup Metrics observation
//        initMetrics();
        initSafeShutdownMetrics();

        //StatusGui
        init(Random.class, this::createRandom);
        SystemStatusResource systemStatusResource = initAndRegisterJaxRsWsComponent(SystemStatusResource.class, this::createSystemStatusResource);
//        MappedIdRepositoryResource mappedIdRepositoryResource = initAndRegisterJaxRsWsComponent(MappedIdRepositoryResource.class, this::createMappedIdRepositoryStatusResource);
        RecRepositoryResource recRepositoryResource = initAndRegisterJaxRsWsComponent(RecRepositoryResource.class, this::createRecRepositoryStatusResource);
        SensorIdsRepositoryResource sensorIdRepositoryResource = initAndRegisterJaxRsWsComponent(SensorIdsRepositoryResource.class, this::createSensorIdRepositoryResource);
        ObservationDistributionResource observationDistributionResource = initAndRegisterJaxRsWsComponent(ObservationDistributionResource.class, this::createObservationDistributionResource);

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


    }

    protected FaviconResource createFaviconResource() {
        String faviconPath = getFaviconPath();
        return new FaviconResource(faviconPath);
    }

    /**
     * Override this method to provide a custom path for the favicon.
     * @return
     */
    protected String getFaviconPath() {
        return "/static/favicon.ico"; // Standard path
    }

    private void initAuditTrail() {
        auditTrail = init(InMemoryAuditTrail.class, InMemoryAuditTrail::new);
        put(AuditTrail.class, auditTrail);
        AuditResource auditResource = initAndRegisterJaxRsWsComponent(AuditResource.class, () -> new AuditResource(auditTrail));
    }

    private void initMetrics() {
        metricsDistributionClient = init(MetricsDistributionClient.class, () -> new AzureApplicationInsightsMetricsClient());
        metricRegistry = get(MetricRegistry.class);
        if (metricRegistry == null) {
            throw new RealestateCloudconnectorException("Missing Metric Registry");
        }
        metricRegistry.register(MetricRegistry.name(ObservationsRepository.class, "ObservationsQueue", "size"),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return observationsRepository.getObservedValuesQueueSize();
                    }
                });

        //Disable MappedIdRepository
        /*
        metricRegistry.register(MetricRegistry.name(MappedIdRepositoryImpl.class, "MappedIdRepository", "size"),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return mappedIdRepository.size();
                    }
                });

         */
        metricRegistry.register(MetricRegistry.name(RecRepositoryInMemory.class, "RecRepository", "size"),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return recRepository.size();
                    }
                });
        metricRegistry.register(MetricRegistry.name(SensorIdRepository.class, "SensorIdRepository", "size"),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return sensorIdRepository.size();
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
    /*
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

     */

    private RecRepositoryResource createRecRepositoryStatusResource() {
        TemplateEngine templateEngine = new TemplateEngine();
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setPrefix("/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false); // Set to true for production
        templateEngine.setTemplateResolver(templateResolver);
        return new RecRepositoryResource(templateEngine, recRepository);
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

    private ObservationDistributionResource createObservationDistributionResource() {
        TemplateEngine templateEngine = new TemplateEngine();
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setPrefix("/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false); // Set to true for production
        templateEngine.setTemplateResolver(templateResolver);
        List<ObservationDistributionClient> observationDistributionClients = new ArrayList<>();
        AzureObservationDistributionClient azureObservationDistributionClient = getOrNull(AzureObservationDistributionClient.class);
        if (azureObservationDistributionClient != null) {
            observationDistributionClients.add(azureObservationDistributionClient);
        }
        ObservationDistributionServiceStub observationDistributionStub = getOrNull(ObservationDistributionServiceStub.class);
        if (observationDistributionStub != null) {
            observationDistributionClients.add(observationDistributionStub);
        }

        return new ObservationDistributionResource(templateEngine, observationDistributionClients);
    }

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
                List<SensorId> sensorIds = sensorIdRepository.all(); //new ArrayList<>();
                /*
                List<MappedSensorId> mappedSensorIds = findSensorsToSubscribeTo(ingestionService.getName(), ingestionService.getClass());
                log.debug("Adding {} sensorIds from ingestionService: {}", mappedSensorIds.size(), ingestionService.getName());
                for (MappedSensorId mappedSensorId : mappedSensorIds) {
                    log.debug("Subscribe to sensorId: {}", mappedSensorId.getSensorId());
                    sensorIds.add(mappedSensorId.getSensorId());
                }

                 */
                ingestionService.addSubscriptions(sensorIds);
            }
        }
    }

    /*
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

     */

    public static RecTags buildRecTagsStub(String roomName, SensorType sensorType) {
        String twinId = "Sensor-Twin-" + roomName + "-" + sensorType.name();
        RecTags recTags = new RecTags();
        recTags.setTfm(roomName + "-" + sensorType.name());
        recTags.setRealEstate("TestRealEstate");
        recTags.setBuilding("TestBuilding");
        recTags.setFloor("1");
        recTags.setServesRoom(roomName);
        recTags.setPlacementRoom(roomName);
        recTags.setSensorType(sensorType.name());
        recTags.setSensorId(twinId);
        recTags.setTwinId(twinId);
        recTags.setTfm("TFM-" + roomName + "-" + sensorType.name());
        return recTags;
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

    protected void initRouter() {
        List<IngestionService> ingestors = new ArrayList<>();
        for (IngestionService ingestionService : ingestionServices.values()) {
            ingestors.add(ingestionService);
        }
        MessageRouter messageRouter = new ScheduledObservationMessageRouter(config, ingestors, observationsRepository, notificationListener);
        messageRouter.start();
    }

    /*
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
    */

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

    protected void initIngestionService(IngestionService service) {
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

    /*
    @Deprecated
    protected void initDistributionController() {
        boolean useInfluxDb = config.asBoolean("influxdb.enabled", false);
        if (useInfluxDb) {
            log.info("Using InfluxDB for distribution.");
        } else {
            log.info("InfluxDB is not enabled.");
        }
        boolean useAzureIoThub = config.asBoolean("azure.iot.enabled", false);
        if (useAzureIoThub) {
            log.info("Using Azure IoT Hub for distribution.");
            String deviceConnectionString = config.get("azure.iot.connectionString");
            AzureObservationDistributionClient azureObservationsClient = new AzureObservationDistributionClient(deviceConnectionString);
        } else {
            log.info("Azure IoT Hub is not enabled.");

        }
        ServiceLoader<DistributionService> distributionServicesFound = ServiceLoader.load(DistributionService.class);

        distributionServices = new HashMap<>();
        for (DistributionService service : distributionServicesFound) {
            log.info("ServiceLoader found a Distribution service called {}!", service.getName());
            service.initialize(null);
            distributionServices.put(service.getName(), service);
            addDistributionServiceHealth(service);
        }
        log.info("ServiceLoader found " + distributionServices.size() + " distribution services!");
    }

     */

    private void initNotificationServices() {
        ServiceLoader<NotificationService> notificationServices = ServiceLoader.load(NotificationService.class);
        if (notificationServices != null && notificationServices.iterator().hasNext()) {
            notificationService = notificationServices.findFirst().orElse(null);
            log.trace("Alerts and Warnings will be sent with NotificationService: {}", notificationService);
        } else {
            log.warn("ServiceLoader could not find any implementation of NotificationService. Using SlackNotificationService.");
            notificationService = new SlackNotificationService();
            put(NotificationService.class, notificationService);
        }
        this.notificationListener = new HealthListener(notificationService);
        put(NotificationListener.class, notificationListener);
    }

    private void initObservationReceiver() {
        observationsRepository = new ObservationsRepository(metricRegistry);
        put(ObservationListener.class, observationsRepository);
        get(StingrayHealthService.class).registerHealthProbe("ObservationsRepository-isHealthy", observationsRepository::isHealthy);
        get(StingrayHealthService.class).registerHealthProbe("ObservationsRepository-ObservedValues-received", observationsRepository::getObservedValueCount);
        get(StingrayHealthService.class).registerHealthProbe("ObservationsRepository-ObservedValuesQueue-size", observationsRepository::getObservedValuesQueueSize);
        get(StingrayHealthService.class).registerHealthProbe("ObservationsRepository-ConfigValues-received", observationsRepository::getObservedConfigValueCount);
        get(StingrayHealthService.class).registerHealthProbe("ObservationsRepository-ConfigMessages-received", observationsRepository::getObservedConfigMessageCount);
    }

    private void initObservationDistributor() {
//        List<DistributionService> distributionServices = new ArrayList<>();
        if (distributionServices == null) {
            distributionServices = new ArrayList<>();
        }
        put("DistributionServices", distributionServices);
        /*
        DistributionService observationDistributionClient = new ObservationDistributionServiceStub(auditTrail);
        put(DistributionService.class, observationDistributionClient);
        if(observationDistributionClient instanceof ObservationDistributionClient) {
            put(ObservationDistributionClient.class, (ObservationDistributionClient) observationDistributionClient);
        }
        distributionServicesList.add(observationDistributionClient);
        log.info("Establishing and verifying connection to Azure.");
        ObservationMessage stubMessage = ObservationMesstageStubs.buildStubObservation();
        observationDistributionClient.publish(stubMessage);
        if (observationsRepository == null) {
            log.warn("ObservationsRepository is null. Cannot start ObservationDistributor");
            throw new RealestateCloudconnectorException("ObservationsRepository is null. Cannot start ObservationDistributor");
        }

         */
        // Stub implementation
        boolean useDistributionServiceStub = config.asBoolean("distributionServiceStub.enabled", true);
        if (useDistributionServiceStub) {
            log.info("Using ObservationDistributionServiceStub for distribution.");
            ObservationDistributionServiceStub observationDistributionServiceStub = new ObservationDistributionServiceStub(auditTrail);
            distributionServices.add(observationDistributionServiceStub);
        } else {
            log.info("ObservationDistributionServiceStub is not enabled.");
        }

        // InfluxDb
        boolean useInfluxDb = config.asBoolean("influxdb.enabled", false);
        if (useInfluxDb) {
            log.info("Using InfluxDB for distribution.");
        } else {
            log.info("InfluxDB is not enabled.");
        }
        // Azure IoT Hub
        boolean useAzureIoThub = config.asBoolean("azure.iot.enabled", false);
        if (useAzureIoThub) {
            log.info("Using Azure IoT Hub for distribution.");
            String deviceConnectionString = config.get("azure.iot.connectionString");
            AzureObservationDistributionClient azureObservationsClient = new AzureObservationDistributionClient(deviceConnectionString);
            distributionServices.add(azureObservationsClient);
        } else {
            log.info("Azure IoT Hub is not enabled.");
        }

        for (DistributionService service : distributionServices) {
            try {
                log.info("Initializing Distribution service called {}!", service.getName());
                service.initialize(null);
                //Add the service to be discovered by other services in the application.
                put(service.getClass().getName(), service);
                //Enable health checks for the distribution service
                addDistributionServiceHealth(service);
            } catch (Exception e) {
                log.error("Failed to initialize Distribution service: " + service.getName(), e);
                throw new RealestateCloudconnectorException("Failed to initialize Distribution service: " + service.getName(), e);
            }

        }
        log.info("ServiceLoader found " + distributionServices.size() + " distribution services!");


        observationDistributor = new ObservationDistributor(observationsRepository, distributionServices, recRepository, metricRegistry, auditTrail);
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

    protected void addDistributionServiceHealth(DistributionService service) {
        get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-isHealthy", service::isHealthy);
        get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-numberofObservationsDistributed", service::getNumberOfMessagesPublished);
        get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-numberofFailedDistributed", service::getNumberOfMessagesFailed);
        get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-numberofMessagesInQueue", service::getNumberOfMessagesInQueue);
        get(StingrayHealthService.class).registerHealthProbe(service.getName() + "-whenLastMessageDistributed", service::getWhenLastMessageDistributed);
        get(StingrayHealthService.class).registerHealthCheck(service.getName() + "-isHealthy:", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                if (service.isHealthy()) {
                    return Result.healthy();
                } else {
                    return Result.unhealthy(service.getName() + " Failed on connection or login ");
                }
            }
        });
    }

    protected RecRepository createRecRepository(boolean useSimulatedSensors) {
        RecRepository recRepository = new RecRepositoryInMemory();
        get(StingrayHealthService.class).registerHealthProbe("RecRepository-size", recRepository::size);
        if (useSimulatedSensors) {
            log.warn("Using simulated SensorId's");
            List<SensorRecObject> sensorRecs = new ArrayList<>();
            RecTags simulatedCo2SensorRecTags = buildRecTagsStub("room1", SensorType.co2);
            SensorId sesorId = new SensorId(simulatedCo2SensorRecTags.getTwinId(), SensorSystem.simulator, Map.of("sensorId", simulatedCo2SensorRecTags.getSensorId()));
            recRepository.addRecTags(sesorId, simulatedCo2SensorRecTags);
            RecTags simulatedTempSensorRecTags = buildRecTagsStub("room1", SensorType.temp);
            SensorId simulatedTempSensorId = new SensorId(simulatedTempSensorRecTags.getTwinId(), SensorSystem.simulator, Map.of("sensorId", simulatedTempSensorRecTags.getSensorId()));
            recRepository.addRecTags(simulatedTempSensorId, simulatedTempSensorRecTags);
        }
        return recRepository;
    }

    /*
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
    */

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

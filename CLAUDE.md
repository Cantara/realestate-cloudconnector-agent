# Agent Context

This repository has a `knowledge.yaml` manifest at the root.
Read it to understand structure, relevant documents, and org relationships before exploring code.

Part of the [Cantara](https://wiki.cantara.no) open source ecosystem — federated KCP knowledge graph.
Org manifest: https://wiki.cantara.no/knowledge.yaml

---

## What this project does

Java 21 Maven application that bridges Building Automation Systems (Desigo, Metasys, EcoStruxure) to cloud platforms (Azure IoT Hub). Built on [Cantara Stingray](https://github.com/Cantara/stingray) — a lightweight Jersey/JAX-RS framework.

## Commands

```bash
# Build fat JAR (maven-shade)
mvn package

# Run locally
mvn exec:java

# Run all tests
mvn test

# Run a single test class or method
mvn test -Dtest=SensorIdsCsvReaderTest
mvn test -Dtest=SensorIdsCsvReaderTest#methodName
```

Dependencies are fetched from Cantara's Nexus (`mvnrepo.cantara.no`) in addition to Maven Central.

## Architecture

### Data flow pipeline

Observations move through three stages:

1. **Ingestion** — `IngestionService` plugins (loaded via Java `ServiceLoader`) poll BAS systems on a schedule. `ScheduledObservationMessageRouter` drives polling (default 10 min, configurable via `ingestion.interval.minutes`). Results are written to `ObservationsRepository`.

2. **Buffering** — `ObservationsRepository` is a bounded `LinkedBlockingDeque` (capacity 10 000) decoupling ingestion from distribution.

3. **Distribution** — `ObservationDistributor` runs as a background thread, drains the queue, enriches each observation with REC semantic tags via `ObservationMapper`, then publishes to all registered `DistributionService` implementations.

### Plugin extension points

Concrete BAS implementations (Desigo, Metasys) live in separate sibling repositories and are added as dependencies. Register new plugins in:

- `META-INF/services/no.cantara.realestate.plugins.ingestion.IngestionService`
- `META-INF/services/no.cantara.realestate.plugins.distribution.DistributionService`

### Semantic enrichment

`RecRepository` maps each `SensorId` to `RecTags` (Real Estate Core metadata: building, floor, room, sensor type, TFM, digital twin ID). `ObservationMapper` uses these tags to build a rich `ObservationMessage`. If no REC tags exist for a sensor, a minimal twin-ID-only message is emitted.

### Application wiring

`RealestateCloudconnectorApplication` extends `AbstractStingrayApplication`. Singletons are registered with `put(Class, instance)` and retrieved with `get(Class)`. JAX-RS resources are registered via `initAndRegisterJaxRsWsComponent()`. This class is designed to be subclassed — BAS-specific connectors override `createRecRepository()`, `createSensorIdRepository()`, and add ingestion wiring.

## Configuration

Primary config: `src/main/resources/RealestateCloudconnector/application.properties`
Local overrides: `local_override.properties` at project root (see `local_override.properties_template`)

Key properties:

| Property | Purpose |
|---|---|
| `sensormappings.simulator.enabled` | Use in-memory simulated sensors instead of real BAS |
| `distributionServiceStub.enabled` | Route observations to in-memory stub instead of cloud |
| `ingestion.interval.minutes` | Polling interval (default 10 min) |
| `azure.iot.enabled` / `azure.iot.connectionString` | Enable Azure IoT Hub distribution |
| `slack_alerting_enabled` / `slack_token` | Enable Slack alerting |

## Runtime endpoints

Base URL: `http://localhost:8083/cloudconnector`

| Path | Purpose |
|---|---|
| `/health` | Health status and all registered probes |
| `/sensorids/status` | SensorId repository contents |
| `/rec/status` | REC repository contents |
| `/audit` | Audit trail |
| `/distribution` | Distribution service status |
| `/admin/metrics/app/*` | Dropwizard metrics |

## Sensor CSV import

`SensorIdsCsvReader` parses semicolon-delimited CSV with header `SensorId;SensorSystem;Identificator`. Supported `SensorSystem` values: `desigo`, `metasys`, `ecostructure`, `simulator`.

## Observability

- Dropwizard `MetricRegistry` meters/gauges (queue sizes, ingestion and distribution counts)
- Azure Application Insights configured in `applicationinsights.json`
- `AuditLog` SLF4J logger traces per-observation lifecycle events (fetch from queue → distribute)

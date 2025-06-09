package no.cantara.realestate.cloudconnector.mappedid;

import no.cantara.realestate.rec.RecObject;
import no.cantara.realestate.sensors.MappedIdQuery;
import no.cantara.realestate.sensors.MappedSensorId;
import no.cantara.realestate.sensors.UniqueKey;
import no.cantara.realestate.sensors.tfm.Tfm;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Deprecated
public class MappedIdRepositoryImpl implements MappedIdRepository {
    private static final Logger log = getLogger(MappedIdRepositoryImpl.class);

    public MappedIdRepositoryImpl() {
        log.info("MappedIdRepositoryImpl created");
    }

    List<MappedSensorId> mappedSensorIds = new ArrayList<>();
    @Override
    public void add(MappedSensorId sensorId) {
        if (sensorId != null) {
            mappedSensorIds.add(sensorId);
        }
    }

    @Override
    public void addAll(List<MappedSensorId> sensorIds) {
        mappedSensorIds.addAll(sensorIds);
    }

    @Override
    public List<MappedSensorId> find(UniqueKey mappingKey) {
        List<MappedSensorId> matching = null;
        if ( mappingKey != null && mappingKey.getKey() != null) {
            if (mappingKey.getKey() instanceof Tfm) {
                matching = mappedSensorIds.stream()
                        .filter(Objects::nonNull)
                        .filter(r -> Objects.nonNull(r.getRec().getTfm()))
                        .filter(r -> r.getRec().getTfm().equals(mappingKey.getKey()))
                        .collect(Collectors.toList());
            } else {
                matching = mappedSensorIds.stream()
                        .filter(Objects::nonNull)
                        .filter(r -> Objects.nonNull(r.getRec().getTfm()))
                        .filter(r -> r.getSensorId().getMappingKey().equals(mappingKey))
                        .collect(Collectors.toList());
            }
        }
        if (matching == null || matching.isEmpty()) {
            log.debug("No matching MappedSensorId found for {}. Please verify that \"Tfm\" and \"Rec\" has values.", mappingKey);
        }
        return matching;
    }

    public List<MappedSensorId> find(MappedIdQuery mappedIdQuery) {
        log.debug("Find from {}", mappedIdQuery);
        Predicate<MappedSensorId> predicate = mappedIdQuery.getPredicate();
        List<MappedSensorId> matching = mappedSensorIds.stream()
                .filter(predicate)
                .collect(Collectors.toList());
        return matching;
    }

    @Override
    public long updateRec(RecObject recObject) {
        return 0;
    }

    @Override
    public long size() {
        if (mappedSensorIds == null) {
            return 0;
        }
        return mappedSensorIds.size() ;
    }

    public List<MappedSensorId> getAll() {
        return mappedSensorIds;
    }
}

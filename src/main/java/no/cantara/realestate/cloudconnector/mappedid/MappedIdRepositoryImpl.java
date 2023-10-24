package no.cantara.realestate.cloudconnector.mappedid;

import no.cantara.realestate.semantics.rec.RecObject;
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

public class MappedIdRepositoryImpl implements MappedIdRepository {
    private static final Logger log = getLogger(MappedIdRepositoryImpl.class);

    public MappedIdRepositoryImpl() {
        log.warn("********* MappedIdRepositoryImpl created ***********");
    }

    List<MappedSensorId> sensorIds = new ArrayList<>();
    @Override
    public void add(MappedSensorId sensorId) {
        if (sensorId != null) {
            sensorIds.add(sensorId);
        }
    }

    @Override
    public List<MappedSensorId> find(UniqueKey mappingKey) {
        List<MappedSensorId> matching = null;
        if ( mappingKey != null && mappingKey.getKey() != null) {
            if (mappingKey.getKey() instanceof Tfm) {
                matching = sensorIds.stream()
                        .filter(Objects::nonNull)
                        .filter(r -> Objects.nonNull(r.getRec().getTfm()))
                        .filter(r -> r.getRec().getTfm().equals(mappingKey.getKey()))
                        .collect(Collectors.toList());
            } else {
                matching = sensorIds.stream()
                        .filter(Objects::nonNull)
                        .filter(r -> Objects.nonNull(r.getRec().getTfm()))
                        .filter(r -> r.getSensorId().getMappingKey().equals(mappingKey))
                        .collect(Collectors.toList());
            }
        }
        return matching;
    }

    public List<MappedSensorId> find(MappedIdQuery mappedIdQuery) {
        Predicate<MappedSensorId> predicate = mappedIdQuery.getPredicate();
        List<MappedSensorId> matching = sensorIds.stream()
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
        if (sensorIds == null) {
            return 0;
        }
        return sensorIds.size() ;
    }

    protected List<MappedSensorId> getAll() {
        return sensorIds;
    }
}

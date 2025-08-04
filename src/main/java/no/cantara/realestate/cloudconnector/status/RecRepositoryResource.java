package no.cantara.realestate.cloudconnector.status;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.cantara.realestate.rec.RecRepository;
import no.cantara.realestate.rec.RecTags;
import no.cantara.realestate.sensors.SensorId;
import no.cantara.stingray.security.application.StingrayAction;
import no.cantara.stingray.security.application.StingraySecurityOverride;
import org.thymeleaf.TemplateEngine;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/rec")
public class RecRepositoryResource {

    private final RecRepository recRepository;
    private final TemplateEngine templateEngine;

    public RecRepositoryResource( TemplateEngine templateEngine, RecRepository recRepository) {
        this.templateEngine = templateEngine;
        this.recRepository = recRepository;
    }

    @GET
    @Path("/status")
    @Produces(MediaType.TEXT_HTML)
    @StingrayAction("status")
    @StingraySecurityOverride
    public Response getStatus() {
        long size = recRepository.size();
        Map<SensorId,RecTags> sensorRecMap = recRepository.getAll();
        List<Map<String, Object>> recTagsList = buildRecList(sensorRecMap);

        org.thymeleaf.context.Context ctx = new org.thymeleaf.context.Context();
        ctx.setVariable("count", size);
        ctx.setVariable("recTags", recTagsList);

        StringWriter stringWriter = new StringWriter();
        templateEngine.process("RecRepositoryStatus", ctx, stringWriter);
        String html = stringWriter.toString();
        return Response.ok(html).build();
    }


    protected List<Map<String, Object>> buildRecList(Map<SensorId,RecTags> sensorRecMap) {
        List<Map<String,Object>> sensorDetails = new ArrayList<>();
        for (SensorId sensorId : sensorRecMap.keySet()) {
            RecTags recTags = sensorRecMap.get(sensorId);
            Map<String, Object> sensorDetail = new HashMap<>();
            sensorDetail.put("sensorId", sensorId.getId());
            sensorDetail.put("sensorType", recTags.getSensorType());
            Map<String, String> identifiers = sensorId.getIdentifiers();
            for (String identifier : identifiers.keySet()) {
                sensorDetail.put("sensorIdentifier-" + identifier, identifiers.get(identifier));
            }
            sensorDetail.put("recSensorId", recTags.getSensorId());
            sensorDetail.put("recTwinId", recTags.getTwinId());
            sensorDetail.put("recRealEstate", recTags.getRealEstate());
            sensorDetail.put("recBuilding", recTags.getBuilding());
            sensorDetails.add(sensorDetail);
        }
        return sensorDetails;
    }

    /*
    protected List<Map<String, Object>> buildList(List<MappedSensorId> sensors) {
        List<Map<String,Object>> sensorDetails = new ArrayList<>();
        for (MappedSensorId sensor : sensors) {
            Map<String, Object> sensorDetail = new HashMap<>();
            if (sensor.getSensorId() != null) {
                SensorId sensorId = sensor.getSensorId();
                sensorDetail.put("type", sensorId.getClass().getSimpleName());
                sensorDetail.put("sensorId", sensorId.getId());
                sensorDetail.put("mappingKey", sensor.getSensorId().getMappingKey().getKey().toString());
                if (sensor.getSensorId() instanceof DesigoSensorId) {
                    sensorDetail.put("desigoId", ((DesigoSensorId) sensorId).getDesigoId());
                    sensorDetail.put("desigoPropertyId", ((DesigoSensorId) sensorId).getDesigoPropertyId());
//                    sensorDetail.put("trendId", ((DesigoSensorId) sensorId).getTrendId());
                }
            }

            if (sensor.getRec() != null) {
                sensorDetail.put("realEstate", sensor.getRec().getRealEstate());
            }
            sensorDetails.add(sensorDetail);
        }
        return sensorDetails;
    }

     */

}

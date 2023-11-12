package no.cantara.realestate.cloudconnector.status;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.cantara.realestate.cloudconnector.mappedid.MappedIdRepository;
import no.cantara.realestate.cloudconnector.mappedid.MappedIdRepositoryImpl;
import no.cantara.realestate.sensors.MappedSensorId;
import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import no.cantara.stingray.security.application.StingrayAction;
import no.cantara.stingray.security.application.StingraySecurityOverride;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/repository")
public class RepositoryResource {

    private final MappedIdRepository mappedIdRepository;
    private final TemplateEngine templateEngine;

    public RepositoryResource(TemplateEngine templateEngine, MappedIdRepository mappedIdRepository) {
        this.templateEngine = templateEngine;
        this.mappedIdRepository = mappedIdRepository;
    }

    @GET
    @Path("/status")
    @Produces(MediaType.TEXT_HTML)
    @StingrayAction("status")
    @StingraySecurityOverride
    public Response getStatus() {
        long size = mappedIdRepository.size();
        List<MappedSensorId> sensors = ((MappedIdRepositoryImpl)mappedIdRepository).getAll();
        List<Map<String, String>> sensorList = buildList(sensors);

        Context ctx = new Context();
        ctx.setVariable("count", size);
        ctx.setVariable("sensorList", sensorList);
        StringWriter stringWriter = new StringWriter();
        templateEngine.process("RepositoryStatus", ctx, stringWriter);
        String html = stringWriter.toString();
        return Response.ok(html).build();

    }

    protected List<Map<String, String>> buildList(List<MappedSensorId> sensors) {
        List<Map<String,String>> sensorDetails = new ArrayList<>();
        for (MappedSensorId sensor : sensors) {
            Map<String, String> sensorDetail = new HashMap<>();
            if (sensor.getSensorId() != null) {
                SensorId sensorId = sensor.getSensorId();
                sensorDetail.put("type", sensorId.getClass().getSimpleName());
                sensorDetail.put("sensorId", sensorId.getId());
                if (sensor.getSensorId() instanceof DesigoSensorId) {
                    sensorDetail.put("desigoId", ((DesigoSensorId) sensorId).getDesigoId());
                    sensorDetail.put("desigoPropertyId", ((DesigoSensorId) sensorId).getDesigoPropertyId());
                    sensorDetail.put("trendId", ((DesigoSensorId) sensorId).getTrendId());
                }
            }
            sensorDetail.put("mappingKey", sensor.getSensorId().getMappingKey().getKey().toString());
            if (sensor.getRec() != null) {
                sensorDetail.put("realEstate", sensor.getRec().getRealEstate());
            }
            sensorDetails.add(sensorDetail);
        }
        return sensorDetails;
    }
}

package no.cantara.realestate.cloudconnector.status;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.cantara.realestate.cloudconnector.sensorid.SensorIdRepository;
import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.SensorSystem;
import no.cantara.stingray.security.application.StingrayAction;
import no.cantara.stingray.security.application.StingraySecurityOverride;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.cantara.realestate.cloudconnector.utils.StringUtils.hasValue;

@Path("/sensorids")
public class SensorIdsRepositoryResource {

    private final SensorIdRepository sensorIdRepository;
    private final TemplateEngine templateEngine;

    public SensorIdsRepositoryResource(TemplateEngine templateEngine, SensorIdRepository sensorIdRepository) {
        this.templateEngine = templateEngine;
        this.sensorIdRepository = sensorIdRepository;
    }

    @GET
    @Path("/status")
    @Produces(MediaType.TEXT_HTML)
    @StingrayAction("status")
    @StingraySecurityOverride
    public Response getStatus(@QueryParam("system") String system, @QueryParam("id") String id, @QueryParam("identifiers") String identifier) {

        long totalCount = sensorIdRepository.size();
        List<SensorId> sensors;
        if (hasValue(system)) {
            SensorSystem sensorsystem = SensorSystem.valueOf(system);
            sensors = sensorIdRepository.find(sensorsystem);
        } else {
            sensors = sensorIdRepository.all();
        }
        if (hasValue(id)) {
            List<SensorId> filteredSensors = new ArrayList<>();
            for (SensorId sensor : sensors) {
                if (sensor.getId().contains(id)) {
                    filteredSensors.add(sensor);
                }
            }
            sensors = filteredSensors;
        }

        List<Map<String, Object>> sensorList = buildList(sensors, identifier);


        long selectionCount = sensorList.size();

        Context ctx = new Context();
        ctx.setVariable("totalCount", totalCount);
        ctx.setVariable("selectionCount", selectionCount);
        ctx.setVariable("sensorList", sensorList);
        StringWriter stringWriter = new StringWriter();
        templateEngine.process("SensorIdsStatus", ctx, stringWriter);
        String html = stringWriter.toString();
        return Response.ok(html).build();

    }

    protected List<Map<String, Object>> buildList(List<SensorId> sensors, String identifier) {
        boolean filterByIdentifier = hasValue(identifier);
        List<Map<String, Object>> sensorDetails = new ArrayList<>();
        for (SensorId sensorId : sensors) {
            Map<String, Object> sensorDetail = new HashMap<>();
            sensorDetail.put("system", sensorId.getSensorSystem());
            sensorDetail.put("sensorId", sensorId.getId());
            Map<String, String> identifiers = sensorId.getIdentifiers();
            String identifiersAsString = identifiers != null ? identifiers.toString() : "";
            sensorDetail.put("identifiers", identifiersAsString);
            if (filterByIdentifier) {
                if (identifiersAsString.contains(identifier)) {
                    sensorDetails.add(sensorDetail);
                }
            } else {
                sensorDetails.add(sensorDetail);
            }
        }
        return sensorDetails;
    }

}

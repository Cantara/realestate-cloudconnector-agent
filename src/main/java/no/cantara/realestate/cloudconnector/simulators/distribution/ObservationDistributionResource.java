package no.cantara.realestate.cloudconnector.simulators.distribution;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.cantara.realestate.cloudconnector.RealestateCloudconnectorException;
import no.cantara.realestate.distribution.ObservationDistributionClient;
import no.cantara.realestate.json.RealEstateObjectMapper;
import no.cantara.realestate.observations.ObservationMessage;
import no.cantara.stingray.security.application.StingrayAction;
import no.cantara.stingray.security.application.StingraySecurityOverride;
import org.slf4j.Logger;
import org.thymeleaf.TemplateEngine;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@Path("/distribution")
public class ObservationDistributionResource {
    private static final Logger log = getLogger(ObservationDistributionResource.class);

    private final ObservationDistributionClient distributionClient;
    private final TemplateEngine templateEngine;

    public ObservationDistributionResource(TemplateEngine templateEngine, ObservationDistributionClient distributionClient) {
        this.templateEngine = templateEngine;
        this.distributionClient = distributionClient;
    }

    @GET
    @Path("/")
    @StingraySecurityOverride
    @Produces(MediaType.APPLICATION_JSON)
    @StingrayAction("getDistributed")
    public Response getDistributed() {
        String body = null;
        List<ObservationMessage> observationMessages = null;

        try {
            observationMessages = distributionClient.getObservedMessages();
            if (observationMessages != null) {
                body = RealEstateObjectMapper.getInstance().getObjectMapper().writeValueAsString(observationMessages);
                return Response.ok(body, MediaType.APPLICATION_JSON_TYPE).build();
            } else {
                return Response.ok("[]", MediaType.APPLICATION_JSON_TYPE).build();
            }
        } catch (JsonProcessingException e) {
            String msg = "Failed to convert observationMessages to json. Reason is: " + e.getMessage();
            RealestateCloudconnectorException mcce = new RealestateCloudconnectorException(msg, e);
            log.debug("Failed to getDistributedObservations",mcce);
            return Response.status(412, "Failed to create json from observed messages.").build();        }
    }

    public long getDistributedCount() {
        return -1;
    }
}

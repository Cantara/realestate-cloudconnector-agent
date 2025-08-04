package no.cantara.realestate.cloudconnector.simulators.distribution;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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

    private final List<ObservationDistributionClient> distributionClients;
    private final TemplateEngine templateEngine;
    private final String contextPath;

    public ObservationDistributionResource(String contextPath, TemplateEngine templateEngine, List<ObservationDistributionClient> distributionClients) {
        this.contextPath = contextPath;
        this.templateEngine = templateEngine;
        this.distributionClients = distributionClients;
    }

    @GET
    @Path("/")
    @StingraySecurityOverride
    @Produces(MediaType.TEXT_HTML)
    @StingrayAction("getDistributed")
    public Response getDistributed() {
        String body = """
                <html>
                <head>
                    <title>Distributed Observations</title>
                    <link rel="icon" type="image/x-icon" href="%s/favicon.ico">
                </head>
                <body>
                <h1>Distributed Observations</h1>
                Please select a client to view the observations:
                <ul>
                  <li><a href="./distribution/ObservationDistributionServiceStub">Stub</a></li>
                  <li><a href="./distribution/AzureObservationDistributionClient">Azure</a></li>
                </ul>
                </body>
                </html>
                """.formatted(contextPath);
        return Response.ok(body, MediaType.TEXT_HTML_TYPE).build();


    }

    @GET
    @Path("/{distributionClientName}")
    @StingraySecurityOverride
    @Produces(MediaType.APPLICATION_JSON)
    @StingrayAction("getDistributedByClientName")
    public Response getDistributedByClientName(@PathParam("distributionClientName")String distributionClientName) {
        List<ObservationMessage> observationMessages = null;

        try {
            ObservationDistributionClient distributionClient = distributionClients.stream()
                    .filter(client -> client.getName().equals(distributionClientName))
                    .findFirst().orElse(null);
            if (distributionClient == null) {
                String msg = "No distribution client found with name '%s'.".formatted(distributionClientName);
                RealestateCloudconnectorException mcce = new RealestateCloudconnectorException(msg);
                log.debug("Failed to getDistributedObservations with name: {}", distributionClientName, mcce);
                return Response.status(404, msg).build();
            }
            observationMessages = distributionClient.getObservedMessages();
            if (observationMessages != null) {
                String body = RealEstateObjectMapper.getInstance().getObjectMapper().writeValueAsString(observationMessages);
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

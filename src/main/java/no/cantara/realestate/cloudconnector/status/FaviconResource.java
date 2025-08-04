package no.cantara.realestate.cloudconnector.status;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import no.cantara.stingray.security.application.StingraySecurityOverride;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

@Path("/favicon.ico")
public class FaviconResource {

    private static final Logger log = LoggerFactory.getLogger(FaviconResource.class);
    private final String faviconPath;

    public FaviconResource(String faviconPath) {
        this.faviconPath = faviconPath != null ? faviconPath : "/static/favicon.ico";
    }

    @GET
    @StingraySecurityOverride
    public Response getFavicon() {
        try {
            InputStream inputStream = getClass().getResourceAsStream(faviconPath);

            if (inputStream == null) {
                log.debug("Favicon not found at: {}", faviconPath);
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            log.debug("Serving favicon from: {}", faviconPath);

            return Response.ok(inputStream)
                    .header("Content-Type", "image/x-icon")
                    .header("Cache-Control", "public, max-age=86400") // Cache i 24 timer
                    .build();

        } catch (Exception e) {
            log.error("Error serving favicon from: " + faviconPath, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
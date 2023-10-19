package no.cantara.realestate.cloudconnector;

import no.cantara.config.ApplicationProperties;
import no.cantara.stingray.application.StingrayApplication;
import no.cantara.stingray.application.StingrayApplicationFactory;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class RealestateCloudconnectorApplicationFactory implements StingrayApplicationFactory<RealestateCloudconnectorApplication> {
    private static final Logger log = getLogger(RealestateCloudconnectorApplicationFactory.class);

    @Override
    public Class<?> providerClass() {
        return RealestateCloudconnectorApplication.class;
    }

    @Override
    public String alias() {
        return "RealestateCloudconnector";
    }

    @Override
    public StingrayApplication<RealestateCloudconnectorApplication> create(ApplicationProperties applicationProperties) {
        return new RealestateCloudconnectorApplication(applicationProperties);
    }

}

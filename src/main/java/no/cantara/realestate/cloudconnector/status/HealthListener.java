package no.cantara.realestate.cloudconnector.status;

import no.cantara.realestate.cloudconnector.notifications.NotificationService;
import no.cantara.realestate.plugins.notifications.NotificationListener;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class HealthListener implements NotificationListener {
    private static final Logger log = getLogger(HealthListener.class);

    protected static List<String> latestErrors = new LinkedList<>();
    Map<String, Boolean> serviceStatus = new HashMap<>();
    private static boolean isHealthy = true;

    private final NotificationService notificationService;

    public HealthListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void sendWarning(String pluginId, String service, String message) {
        log.warn("Warning received from plugin: {} for service: {} with message: {}", pluginId, service, message);
        notificationService.sendWarning(service, message);
    }

    @Override
    public void sendAlarm(String pluginId, String service, String message) {
        log.error("Alarm received from plugin: {} for service: {} with message: {}", pluginId, service, message);
        notificationService.sendAlarm(service, message);
    }

    @Override
    public void clearService(String pluginId, String service) {
        log.info("Clearing service: {} from plugin: {}", service, pluginId);
        notificationService.clearService(service);
    }

    @Override
    public void setHealthy(String pluginId, String service) {
        log.info("Setting service: {} from plugin: {} to healthy", service, pluginId);
        serviceStatus.put(service, true);

    }

    @Override
    public void setUnhealthy(String pluginId, String service, String message) {
        log.info("Setting service: {} from plugin: {} to unhealthy due to {}", service, pluginId, message);
        serviceStatus.put(service, false);
    }

    @Override
    public void addError(String pluginId, String service, String message) {
        log.info("Adding error for service: {} from plugin: {} with message: {}", service, pluginId, message);
        latestErrors.add(pluginId + " - " + service + " - " + message);
    }
}

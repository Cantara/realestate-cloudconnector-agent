package no.cantara.realestate.cloudconnector.routing;

import com.codahale.metrics.health.HealthCheck;
import no.cantara.realestate.cloudconnector.notifications.NotificationService;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Stingray health check for the {@link ObservationDistributor}, with edge-triggered notifications.
 *
 * <p>Why a class instead of the anonymous check it replaces: the health service runs every check
 * on each round, and the old check sent a warning on every round it was unhealthy. Combined with a
 * health flag that never recovered, that produced a warning every few seconds until the process was
 * restarted. This check notifies once when the distributor goes unhealthy, clears once when it
 * recovers, and tells which of the three derived conditions failed so the cause is readable from
 * {@code /health} and the notification alone.
 *
 * <p>Example:
 * <pre>{@code
 * healthService.registerHealthCheck("ObservationDistributor-isHealthy",
 *         new ObservationDistributorHealthCheck(observationDistributor, notificationService));
 * }</pre>
 */
public class ObservationDistributorHealthCheck extends HealthCheck {
    public static final String SERVICE_NAME = "ObservationDistributor";

    private final ObservationDistributor distributor;
    private final NotificationService notificationService;
    private final AtomicBoolean reportedUnhealthy = new AtomicBoolean(false);

    public ObservationDistributorHealthCheck(ObservationDistributor distributor, NotificationService notificationService) {
        this.distributor = distributor;
        this.notificationService = notificationService;
    }

    @Override
    protected Result check() {
        if (distributor.isHealthy()) {
            if (reportedUnhealthy.compareAndSet(true, false) && notificationService != null) {
                notificationService.clearService(SERVICE_NAME);
            }
            return Result.healthy();
        }
        String reason = describeUnhealthyReason();
        if (reportedUnhealthy.compareAndSet(false, true) && notificationService != null) {
            notificationService.sendWarning(SERVICE_NAME, reason);
        }
        return Result.unhealthy(reason);
    }

    protected String describeUnhealthyReason() {
        if (!distributor.isLoopAlive()) {
            return "ObservationDistributor thread is not running. Nothing is distributed until the process is restarted.";
        }
        if (distributor.isHandoffFaulted()) {
            return "Distribution service rejects messages (connection not established or unstable). Retrying with backoff.";
        }
        return "A distribution service reports unhealthy (no ack from IoT Hub, circuit breaker open, or link unstable).";
    }
}

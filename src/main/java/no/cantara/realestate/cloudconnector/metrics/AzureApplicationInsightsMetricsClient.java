package no.cantara.realestate.cloudconnector.metrics;

import com.microsoft.applicationinsights.TelemetryClient;
import no.cantara.realestate.metrics.Metric;
import no.cantara.realestate.metrics.MetricsDistributionClient;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class AzureApplicationInsightsMetricsClient implements MetricsDistributionClient {
    private static final Logger log = getLogger(AzureApplicationInsightsMetricsClient.class);
    private final TelemetryClient telemetryClient;

    public AzureApplicationInsightsMetricsClient() {
        telemetryClient = new TelemetryClient();
    }

    @Override
    public void sendMetrics(Metric metric) {
        String key = metric.getMeasurementName();
        try {
            Number value = metric.getValue();
            if (value != null) {
                log.trace("sendMetrics called with metric: {}, value: {}", key, value);
                telemetryClient.trackMetric(key, value.doubleValue());
            }
        } catch (Exception e) {
            log.trace("Failed to send metric: {}. Reason: {}", metric, e.getMessage());
        }
    }

    @Override
    public void sendLongValue(String metricName, long value) {
        if (metricName != null && !metricName.isEmpty() ) {
            log.trace("sendValue(String,long) called with metricName: {}, value: {}", metricName, value);
            telemetryClient.trackMetric(metricName, value);
        } else {
            log.trace("sendValue(String,long) called with null metricName value: {}", metricName, value);
        }
    }

    @Override
    public void sendDoubleValue(String metricName, double value) {
        if (metricName != null && !metricName.isEmpty() ) {
            log.trace("sendValue(String,double) called with metricName: {}, value: {}", metricName, value);
            telemetryClient.trackMetric(metricName, value);
        } else {
            log.trace("sendValue(String,double) called with null metricName value: {}", metricName, value);
        }
    }

    @Override
    public void sendStringValue(String metricName, String value) {
        if (metricName != null && !metricName.isEmpty() ) {
            log.trace("sendValue(String,String) called with metricName: {}, value: {}. This is not supported by " +
                    "Azure ApplicationInsights, and is ignored.", metricName, value);
        } else {
            log.trace("sendValue(String,String) called with null metricName value: {}", metricName, value);
        }
    }

    @Override
    public void sendBooleanValue(String metricName, boolean value) {
        if (metricName != null && !metricName.isEmpty() ) {
            log.trace("sendValue(String,boolean) called with metricName: {}, value: {}", metricName, value);
            if (value) {
                telemetryClient.trackMetric(metricName, 1.0);
            } else {
                telemetryClient.trackMetric(metricName, 0.0);
            }
        } else {
            log.trace("sendValue(String,boolean) called with null metricName value: {}", metricName, value);
        }
    }
}


package no.cantara.realestate.cloudconnector;

import org.slf4j.helpers.MessageFormatter;

import java.util.UUID;

public class RealestateCloudconnectorException extends RuntimeException {
    private final UUID id;
    private Enum<StatusType> statusType = null;

    public RealestateCloudconnectorException(String message) {
        super(message);
        id = UUID.randomUUID();
    }

    public RealestateCloudconnectorException(String message, Throwable throwable) {
        super(message, throwable);
        this.id = UUID.randomUUID();
    }

    public RealestateCloudconnectorException(String message, Throwable throwable, Object... parameters) {
        this(MessageFormatter.format(message, parameters).getMessage(),throwable);

    }

    public RealestateCloudconnectorException(String msg, StatusType statusType) {
        this(msg);
        this.statusType = statusType;
    }
    public RealestateCloudconnectorException(String msg, Throwable t, StatusType statusType) {
        this(msg,t);
        this.statusType = statusType;
    }

    public RealestateCloudconnectorException(String msg, Exception e, StatusType statusType) {
        this(msg, e);
        this.statusType = statusType;
    }


    @Override
    public String getMessage() {

        String message = super.getMessage() +" MessageId: " + id.toString();
        if (getCause() != null) {
            message = message + "\n\tCause: " + getCause().getMessage();
        }
        return message;
    }

    public String getMessageId() {
        return id.toString();
    }

    public Enum<StatusType> getStatusType() {
        return statusType;
    }

    public UUID getId() {
        return id;
    }
}


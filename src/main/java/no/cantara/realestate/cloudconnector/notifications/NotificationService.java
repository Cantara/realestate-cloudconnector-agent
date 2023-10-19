package no.cantara.realestate.cloudconnector.notifications;

public interface NotificationService {
    boolean sendWarning(String service, String warningMessage) ;

    boolean sendAlarm(String service, String alarmMessage);

    boolean clearService(String service);
}

package gov.usgs.earthquake.nats;

import gov.usgs.earthquake.distribution.*;
import gov.usgs.util.Config;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

public class NATSStreamingNotificationSender extends DefaultNotificationSender {

  private static final Logger LOGGER = Logger
          .getLogger(DefaultNotificationSender.class.getName());

  public static String CLUSTER_ID_PROPERTY = "clusterid";
  public static String CLIENT_ID_PROPERTY = "clientid";
  public static String SUBJECT_PROPERTY = "subject";

  public static String DEFAULT_CLUSTER_ID_PROPERTY = "";
  public static String DEFAULT_CLIENT_ID_PROPERTY = "";
  public static String DEFAULT_SUBJECT_PROPERTY = "";

  private String clusterId;
  private String clientId;
  private String subject;
  private StreamingConnection stanConnection;

  @Override
  public void configure(Config config) throws Exception{
    super.configure(config);

    clusterId = config.getProperty(CLUSTER_ID_PROPERTY, DEFAULT_CLUSTER_ID_PROPERTY);
    clientId = config.getProperty(CLIENT_ID_PROPERTY, DEFAULT_CLIENT_ID_PROPERTY);
    subject = config.getProperty(SUBJECT_PROPERTY, DEFAULT_SUBJECT_PROPERTY);
  }

  @Override
  public void sendMessage(String message) throws Exception {
    try {
      stanConnection.publish(subject, message.getBytes());
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "[" + getName() + "] exception publishing NATSStreaming notification:");
      throw e;
    }
  }

  //TODO: Consider merging with sendMessage
  @Override
  public String notificationToString(Notification notification) throws Exception{
    return URLNotificationJSONConverter.toJSON((URLNotification) notification);
  }

  @Override
  public void startup() throws Exception {
    super.startup();
    StreamingConnectionFactory factory = new StreamingConnectionFactory(clusterId,clientId);
    factory.setNatsUrl("nats://" + this.serverHost + ":" + this.serverPort);
    stanConnection = factory.createConnection();
  }

  @Override
  public void shutdown() throws Exception {
    try {
      stanConnection.close();
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "[" + getName() + "] failed to close NATS connection");
    }
    stanConnection = null;
    super.shutdown();
  }
}
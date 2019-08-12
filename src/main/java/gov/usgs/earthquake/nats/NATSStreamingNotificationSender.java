package gov.usgs.earthquake.nats;

import gov.usgs.earthquake.distribution.*;
import gov.usgs.util.Config;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

//TODO: How are host and port used? Shouldn't they be?
public class NATSStreamingNotificationSender extends DefaultNotificationSender {

  private static final Logger LOGGER = Logger
          .getLogger(DefaultNotificationSender.class.getName());

  public static String CLUSTER_ID_PROPERTY = "clusterid";
  public static String CLIENT_ID_PROPERTY = "clientid";
  public static String SUBJECT_PROPERTY = "subject";

  private String clusterId;
  private String clientId;
  private String subject;
  private StreamingConnection stanConnection;

  @Override
  public void configure(Config config) throws Exception{
    super.configure(config);

    clusterId = config.getProperty(CLUSTER_ID_PROPERTY);
    if (clusterId == null) {
      throw new ConfigurationException("[" + getName() + "] " + CLUSTER_ID_PROPERTY + " must be defined");
    }
    clientId = config.getProperty(CLIENT_ID_PROPERTY);
    if (clientId == null) {
      throw new ConfigurationException("[" + getName() + "] " + CLIENT_ID_PROPERTY + " must be defined");
    }
    subject = config.getProperty(SUBJECT_PROPERTY);
    if (subject == null) {
      throw new ConfigurationException("[" + getName() + "] " + SUBJECT_PROPERTY + " must be defined");
    }
  }

  //TODO: paradigm for exception handling
  @Override
  public void sendMessage(String message) throws Exception{
    try {
      stanConnection.publish(subject, message.getBytes());
    } catch (TimeoutException te) {
      // if we lose connection to server
      LOGGER.warning("[" + getName() + "] NATS Streaming server inaccessible. Can't publish notification " + message);
      throw te;
    } catch (InterruptedException ie) {
      // if we're interrupted when sending
      LOGGER.warning("[" + getName() + "] Interrupted while sending notification " + message);
      throw ie;
    } catch (IOException ioe) {
      // if there's a problem with the message
      throw ioe;
    }
  }

  //TODO: Decide if DefaultNotificationSender should always be using URLNotifications, or if a Notification is sufficient
  // everywhere
  @Override
  public String notificationToString(Notification notification) throws Exception{
    return URLNotificationJSONConverter.toJSON((URLNotification) notification);
  }

  @Override
  public void startup() throws Exception {
    super.startup();
    stanConnection = new StreamingConnectionFactory(clusterId,clientId).createConnection();
  }

  @Override
  public void shutdown() throws Exception {
    stanConnection.close();
    super.shutdown();
  }
}
package helpers;

import java.util.HashMap;
import java.util.Map;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Identity;
import com.zeroc.Ice.Object;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;
import com.zeroc.IceStorm.AlreadySubscribed;
import com.zeroc.IceStorm.BadQoS;
import com.zeroc.IceStorm.InvalidSubscriber;
import com.zeroc.IceStorm.NoSuchTopic;
import com.zeroc.IceStorm.TopicExists;
import com.zeroc.IceStorm.TopicManagerPrx;
import com.zeroc.IceStorm.TopicPrx;

/**
 * zero c ice helper
 */
public class IceHelper {
	// base port number
	private static String PORT_NUM = "9999";
	// protocol
	private static String COMM_PROTOCOL = "default";
	// location server port
	public static String LSPort = "10002";
	// preference repository port
	public static String PRPort = "10020";
	// context manager port
	public static String CMPort = "10008";
	
	/**
	 * get topic proxy
	 * @param topicManager topic manager
	 * @param topicName topic name
	 * @return topic proxy
	 */
	public static TopicPrx getTopicPrx(TopicManagerPrx topicManager, String topicName) {
		TopicPrx topicPrx = null;
		try {
			topicPrx = topicManager.retrieve(topicName);
		} catch(NoSuchTopic e) {
			try {
				topicPrx = topicManager.create(topicName);
			} catch(TopicExists ex) {
                System.err.println("Error: Cannot create a topic with this name, as the topic already exists.");
			}
		}
		return topicPrx;
	}
	
	/**
	 * get publisher
	 * @param topicName topic name
	 * @param communicator zero c ice communicator
	 * @return object proxy
	 */
	public static ObjectPrx getPublisher(String topicName, Communicator communicator) {
		ObjectPrx genericProxy = communicator.stringToProxy("IceStorm/TopicManager:" + COMM_PROTOCOL + " -p " + PORT_NUM); 
		TopicManagerPrx topicManagerPrx = TopicManagerPrx.checkedCast(genericProxy);
		
		if (topicManagerPrx == null) {
            System.err.println("invalid proxy");
        }
		
		TopicPrx topicPrx = getTopicPrx(topicManagerPrx, topicName);
		ObjectPrx publisher = topicPrx.getPublisher();
		return publisher;
	}
	
	/**
	 * subscriber helper
	 * @param <T> interacting class
	 * @param topicName topic name
	 * @param communicator zero c ice communicator
	 * @param endpointsConfig end points configuration
	 * @param interactingObject interacting object
	 * @param subscriberName subscriber name
	 */
	public static <T> void subscriberHandler(String topicName, Communicator communicator, 
			String endpointsConfig, T interactingObject, String subscriberName) {
		ObjectPrx genericProxy = communicator.stringToProxy("IceStorm/TopicManager:" + COMM_PROTOCOL + " -p " + PORT_NUM); 
        TopicManagerPrx topicManagerPrx = TopicManagerPrx.checkedCast(genericProxy);
        
        if(topicManagerPrx == null) {
            System.err.println("invalid proxy");
        }
        
        TopicPrx topicPrx = getTopicPrx(topicManagerPrx, topicName);
        
        ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(endpointsConfig, COMM_PROTOCOL);
        Identity id = new Identity(null, "");
        id.name = java.util.UUID.randomUUID().toString();
        
        Object object = (Object) interactingObject;
        
        ObjectPrx subscriber = adapter.add(object, id);
        ShutdownHelper.add(subscriberName, topicPrx, subscriber);
        adapter.activate();
        
        Map<String, String> qualityOfService = new HashMap<>();
        
        try {
            topicPrx.subscribeAndGetPublisher(qualityOfService, subscriber); 
        }
        catch(AlreadySubscribed e) {
            // This should never occur when subscribing with an UUID
            e.printStackTrace();
            System.out.println("reactivating persistent subscriber");
        }
        catch(InvalidSubscriber e) {
            e.printStackTrace();
        }
        catch(BadQoS e) {
            e.printStackTrace();
        }       
	}
	
	/**
	 * add shutdown hook to subscriber
	 * @param topicPrx topic proxy
	 * @param subscriber subscriber
	 * @param communicator zero c ice communicator
	 * @param destroyHook destroy hook
	 */
	public static void shutdownHookToUnsubscribe(TopicPrx topicPrx, ObjectPrx subscriber, 
			Communicator communicator, Thread destroyHook) {
		final TopicPrx topicF = topicPrx;
        final ObjectPrx subscriberF = subscriber;
        
        Runtime.getRuntime().addShutdownHook(
    		new Thread(() -> {
	            try {
	                topicF.unsubscribe(subscriberF);
	            }
	            finally {
	                communicator.destroy();
	            }
	    	})
	    );
        
        Runtime.getRuntime().removeShutdownHook(destroyHook); 
	}
	
	/**
	 * RMI server handler
	 * @param <T> interacting class
	 * @param communicator zero c ice communicator
	 * @param interactingObject interacting object
	 * @param endpointName end point name
	 * @param identity identity of RMI
	 * @param portNum port number
	 */
	public static <T> void serverRMIHandler(Communicator communicator, T interactingObject, 
			String endpointName, String identity, String portNum) {
		Object object = (Object) interactingObject;
		ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(endpointName, "default -p " + portNum);
		
		adapter.add(object, Util.stringToIdentity(identity));
		adapter.activate();
	}
	
	/**
	 * RMI client handler
	 * @param communicator zero c ice communicator
	 * @param identity identity of RMI
	 * @param portNum port number
	 * @return object proxy
	 */
	public static ObjectPrx clientRMIHandler(Communicator communicator, String identity, String portNum) {
		ObjectPrx genericProxy = communicator.stringToProxy(identity + ": default -p  " + portNum);
		return genericProxy;
	}
}

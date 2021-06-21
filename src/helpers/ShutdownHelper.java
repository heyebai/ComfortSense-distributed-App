package helpers;

import java.util.HashMap;
import java.util.Map;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.IceStorm.TopicPrx;

/**
 * shutdown helper
 */
public class ShutdownHelper {
	// record subscriber information
	private static Map<String, Map<ObjectPrx, TopicPrx>> subscribers = new HashMap<>();

	/**
	 * shut down server and publisher
	 * @param communicator zero c ice communicator
	 */
	public static void shutdown(Communicator communicator) {
		communicator.destroy();
	}
	
	/**
	 * shut down subscriber
	 * @param communicator zero c ice communicator
	 * @param subscriber subscirber name
	 */
	public static void shutdown(Communicator communicator, String subscriber) {
		Thread thread = new Thread(() -> {
			try {
				subscribers.get(subscriber).forEach((k, v) -> {
					v.unsubscribe(k);
				});
			} 
			finally {
				communicator.destroy();
			}
		});
		thread.start();
	}
	
	/**
	 * add subscriber information
	 * @param subscriber subscriber name
	 * @param topicPrx topic proxy
	 * @param objectPrx object proxy
	 */
	public static void add(String subscriber, TopicPrx topicPrx, ObjectPrx objectPrx) {
		subscribers.put(subscriber, new HashMap<>());
		subscribers.get(subscriber).put(objectPrx, topicPrx);
	}
}

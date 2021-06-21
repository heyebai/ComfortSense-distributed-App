package components;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.CommunicatorDestroyedException;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;
import ComfortSenseCM.ShutdownPS;
import ComfortSenseLocation.LocationServerPSPrx;
import ComfortSenseSensors.Sensors;
import helpers.ConsoleLog;
import helpers.IceHelper;
import helpers.ShutdownHelper;
import services.LocationService;

/**
 * location server component
 */
public class LocationServer {
	// zero c ice communicator
	private static Communicator communicator;
	// list of indoor and outdoor locations
	private static ArrayList<String> indoorLocations, outdoorLocations;
	// all communicators used in location server by subscribers
	private Map<String, Communicator> subscribers = new HashMap<>();
	// destroy hook
	private static Thread destroyHook;
	
	/**
	 * location server component
	 * @param args file name
	 * @throws IOException
	 */
	public LocationServer(String[] args) throws IOException {
		//read location file
		indoorLocations = new ArrayList<>();
		outdoorLocations = new ArrayList<>();
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream("datafiles/" + args[0]), "UTF-8"));
		String line = reader.readLine();
		loadLocationFile(reader, indoorLocations);
		loadLocationFile(reader, outdoorLocations);
	}
	
	/**
	 * get information from the file
	 * @param reader buffered reader
	 * @param locationList location list
	 * @throws IOException
	 */
	private void loadLocationFile(BufferedReader reader, ArrayList<String> locationList) throws IOException {
        String line = reader.readLine();
        String[] tempStringList = line.split(":")[1].split(",");
        for (int i = 0; i < tempStringList.length; i++) {
        	locationList.add(tempStringList[i].trim());
        }        
	}
	
	/**
	 * to run location server
	 * @param args file name
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		LocationServer locationServer = new LocationServer(args);
		
		communicator = Util.initialize(args);
		destroyHook = new Thread(() -> { communicator.destroy(); });
		Runtime.getRuntime().addShutdownHook(destroyHook);
		
		try {
			locationServer.subscribeLocationData();
			locationServer.subscribeShutdown();
		} catch (Exception ex) {
			// TODO: handle exception
			ex.printStackTrace();
		}
		locationServer.initializeLocationRMIServer(communicator);
	}
	
	/**
	 * initialize RMI server for context manager
	 * @param communicator
	 */
	private void initializeLocationRMIServer(Communicator communicator) {
		IceHelper.serverRMIHandler(communicator, new LocationService(indoorLocations), 
				"LocationServerAdapter", "IndoorLocations", IceHelper.LSPort);
		communicator.waitForShutdown();
	}
	
	/**
	 * subscribe location data
	 */
	private void subscribeLocationData() {
		Communicator communicator = Util.initialize();
		
		String topicName = "LocationSensor";
		String endpointsConfig = "Sensors.Subscriber";
		subscribers.put(topicName, communicator);
		IceHelper.subscriberHandler(topicName, communicator, endpointsConfig, 
				new LocationDataSubscriber(), topicName);
	}
	
	// record whether users are in indoor or outdoor
	private Map<String, ArrayList<String>> usersStatus = new HashMap<>(); //key:username //0:location 1:userStatus
	// indoor status
	private String indoorStatus = "indoor";
	// outdoor status
	private String outdoorStatus = "outdoor";
	
	/**
	 *	subscriber for location data
	 */
	private class LocationDataSubscriber implements Sensors{

		/**
		 * get sensor data
		 */
		@Override
		public void sensorData(String username, String sensorType, String data, Current current) {
			// TODO Auto-generated method stub
			ConsoleLog.psLog("Received", "location data value");
			String userLocation = data.split(",")[0];
			String userStatus = getUserStatus(userLocation);
			
			// when users change their status, send info to cm
			if (usersStatus.containsKey(username)) {
				if (!userLocation.matches(usersStatus.get(username).get(0))) {
					usersStatus.get(username).set(0, userLocation);
					usersStatus.get(username).set(1, userStatus);
					//publish data to cm
					try {
						publishUserStatus(communicator, username, userStatus, userLocation);
					} catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			} else {
				usersStatus.put(username, new ArrayList<>());
				usersStatus.get(username).add(userLocation);
				usersStatus.get(username).add(userStatus);
				
				//publish data to cm
				try {
					publishUserStatus(communicator, username, userStatus, userLocation);
				} catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		
		/**
		 * publish user's status
		 * @param communicator zero c ice communicator
		 * @param username user's name
		 * @param userStatus user's status
		 * @param userLocation user current location
		 */
		private void publishUserStatus(Communicator communicator, String username, String userStatus,
				String userLocation) {
			ObjectPrx userStatusPublisher = IceHelper.getPublisher("UserStatus", communicator);
			LocationServerPSPrx locationServerPSPrx = 
					LocationServerPSPrx.uncheckedCast(userStatusPublisher);
			try {
				locationServerPSPrx.userStatus(username, userStatus, userLocation);
				ConsoleLog.psLog("Sent", "wether user is in indoor or outdoor");
			} catch (CommunicatorDestroyedException e) {
				// TODO: handle exception
			}
		}
		
		/**
		 * get user status
		 * @param userLocation user current location
		 * @return indoor or outdoor status
		 */
		private String getUserStatus(String userLocation) {
			if (indoorLocations.contains(userLocation)) {
				return indoorStatus;
			}
			return outdoorStatus;
		}
	}
	
	/**
	 * subscribe shutdown signal
	 */
	public void subscribeShutdown() {
		Communicator communicator = Util.initialize();
		
		String topicName = "Shutdown";
		String endpointsConfig = "Shutdown.Subscriber";
		subscribers.put("LS", communicator);
		IceHelper.subscriberHandler(topicName, communicator, endpointsConfig, 
				new ShutdownSubscriber(), "LS");
	}
	
	/**
	 *	subscriber for shutdown signal
	 */
	private class ShutdownSubscriber implements ShutdownPS{

		/**
		 * get shutdown signal
		 */
		@Override
		public void shutdown(boolean shutdown, Current current) {
			// TODO Auto-generated method stub
			if (shutdown) {
				ConsoleLog.psLog("Received", "shutdown signal");
				destroyHook.start();
				subscribers.forEach((k, v) -> {
					ShutdownHelper.shutdown(v, k);
				});
				System.exit(0);
			}
		}
		
	}

}

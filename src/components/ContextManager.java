package components;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.CommunicatorDestroyedException;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;

import ComfortSenseCM.SuggestionsPSPrx;
import ComfortSenseCM.CMRMIServer;
import ComfortSenseCM.ShutdownPSPrx;
import ComfortSenseLocation.LocationServerPS;
import ComfortSenseLocation.LocationServerRMIPrx;
import ComfortSensePrefRepo.PrefRepoPS;
import ComfortSensePrefRepo.UserPrefPrx;
import ComfortSenseWeather.WeatherAlarm;
import ComfortSenseSensors.Sensors;
import ComfortSenseUI.UserExitPS;
import helpers.ConsoleLog;
import helpers.IceHelper;
import helpers.ShutdownHelper;


/**
 *	context manager component
 */
public class ContextManager {
	// location information
	private static Map<String, ArrayList<String>> locationInfo; //key:location name; 0:location 1:info
	// services in the location
	private static Map<String, ArrayList<String>> locationServices; //key:location name; value:services
	// indoor locations
	private static ArrayList<String> indoorLocations; // locationA locationB
	// zero c ice communicator
	private Communicator communicator;
	// destroy hook
	private Thread destroyHook;
	// context manager component
	private static ContextManager contextManager = new ContextManager();
	// all communicators used by subscribers in context manager
	private Map<String, Communicator> subscribers = new HashMap<>();
	
	/**
	 * to run context manager
	 * @param args location information file name
	 */
	public static void main(String[] args) {
		initializeCM("datafiles/" + args[0]);
		contextManager.communicator = Util.initialize();
		contextManager.destroyHook = new Thread(() -> { contextManager.communicator.destroy(); });
		Runtime.getRuntime().addShutdownHook(contextManager.destroyHook);
		contextManager.getIndoorLocationsRMIClient();	
		
		try {
			contextManager.subscribeSensorsData();
			contextManager.subscribeWeatherData();
			contextManager.subscribeUserStatus();
			contextManager.subscribeOnlineUserInfo();
			contextManager.subscribeUserExit();
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		contextManager.initializeCMRMIServer(contextManager.communicator);
		contextManager.communicator.waitForShutdown();
	}
	
	/**
	 * read location information file to get data
	 * @param fileName file name
	 */
	public static void initializeCM(String fileName) {
		locationInfo = new HashMap<>();
		locationServices = new HashMap<>();
		BufferedReader reader = null;
		String line = null;
		String locationName = "";
		String locationInfoString = "";
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(fileName), "UTF-8"));
	        line=reader.readLine();
	        
	        while (line != null) {
				if (line.startsWith("name")) {
					locationName = line.split(":")[1].trim();
					locationInfo.put(locationName, new ArrayList<>());
				} else if (line.startsWith("location")) {
					String location = line.split(":")[1].trim();
					locationInfo.get(locationName).add(location);
				} else if (line.startsWith("information")) {
					locationInfoString = line.split(":")[1].trim();
				} else if (line.startsWith("services")) {
					locationInfo.get(locationName).add(locationInfoString);
					String[] services = line.split(": ")[1].split(", ");
					locationServices.put(locationName, new ArrayList<>(Arrays.asList(services)));
				} else {
					if (!line.isBlank()) {
						locationInfoString += " ";
						locationInfoString += line.trim();
					}
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			// TODO: handle exception
			System.out.println("Context manager file cannot be found.");
		}	
	}
	
	/**
	 * initialize RMI server for UI
	 * @param communicator zero c ice communicator
	 */
	public void initializeCMRMIServer(Communicator communicator) {
		IceHelper.serverRMIHandler(communicator, new CMService(), "CMAdapter", "LocationInfo", 
				IceHelper.CMPort);
	}
	
	/**
	 * get indoor locations from location server by RMI
	 */
	public void getIndoorLocationsRMIClient() {
		ObjectPrx genericProxy = IceHelper.clientRMIHandler(communicator, "IndoorLocations", IceHelper.LSPort);
		LocationServerRMIPrx locationServerRMIPrx = LocationServerRMIPrx.checkedCast(genericProxy);
		String[] indoorLocations = locationServerRMIPrx.getAllIndoorLocations();
		ContextManager.indoorLocations = new ArrayList<String>(Arrays.asList(indoorLocations));
		ConsoleLog.rmiLog("Received", "Location Server", "list of indoor locations");
	}
	
	/**
	 * get user preference from Preference Repository
	 * @param username user's name
	 * @param triggeringType triggering type (humidity, noise, weather)
	 * @return user preference
	 */
	public String getUserPrefRMIClient(String username, String triggeringType) {
		ObjectPrx genericProxy = IceHelper.clientRMIHandler(communicator, "GetUserPref", IceHelper.PRPort);
		UserPrefPrx userPrefPrx = UserPrefPrx.checkedCast(genericProxy);
		ConsoleLog.rmiLog("Received", "Preference Repository", "user preference");
		return userPrefPrx.getUserPref(username, triggeringType);
	}
	
	/**
	 * publish suggestions to users
	 * @param username user's name
	 * @param warningType warning type
	 * @param currentValue current value
	 * @param limitValue user limit value
	 * @param weatherAlarm whether there is a weather alarm
	 * @param currentLocation user current location
	 * @param userPref user's preference
	 * @param suggestedLocations suggested locations
	 */
	public void publishSuggestions(String username, String warningType, int currentValue, 
			int limitValue, boolean weatherAlarm, String currentLocation, String userPref, 
			String[] suggestedLocations) {
		ObjectPrx suggestionsPublisher = IceHelper.getPublisher("Suggestions", communicator);
		SuggestionsPSPrx suggestionsPSPrx = SuggestionsPSPrx.uncheckedCast(suggestionsPublisher);
		
		try {
			suggestionsPSPrx.suggestedContent(username, warningType, currentValue, limitValue, 
					weatherAlarm, currentLocation, userPref, suggestedLocations);
			ConsoleLog.psLog("Sent", "suggestions towards users concerning username, "
					+ "current value, user limit value, warning type, user current location, "
					+ "user preference and suggested locations");
		} catch (CommunicatorDestroyedException e) {
			// TODO: handle exception
		}
	}
	
	/**
	 * publish shutdown signal
	 */
	public void publishShutdown() {
		ObjectPrx shutdownPublisher = IceHelper.getPublisher("Shutdown", communicator);
		ShutdownPSPrx shutdownPSPrx = ShutdownPSPrx.uncheckedCast(shutdownPublisher);
		
		try {
			shutdownPSPrx.shutdown(true);
			ConsoleLog.psLog("Sent", "shutdown signal");
		} catch (CommunicatorDestroyedException e) {
			// TODO: handle exception
		}
	}
	
	/**
	 * get locations based on user's preference
	 * @param userPref user's preference
	 * @return locations including user's preference
	 */
	public String[] getSuggestedLocations(String userPref) {
		ArrayList<String> locations = new ArrayList<>();
		for (String key : locationServices.keySet()) {
			if (locationServices.get(key).contains(userPref)) {
				locations.add(key);
			}
		}
		return locations.toArray(new String[0]);
	}
	
	/**
	 * get indoor locations based on user's preference
	 * @param userPref user's preference
	 * @return indoor locations including user's preference
	 */
	public String[] getSuggestedIndoorLocations(String userPref) {
		String[] suggestedLocations = getSuggestedLocations(userPref);
		ArrayList<String> temp = new ArrayList<>();
		ArrayList<String> indoorLocationNames = getIndoorLovationNames();
		for (String location : suggestedLocations) {
			if (indoorLocationNames.contains(location)) {
				temp.add(location);
			}
		}
		return temp.toArray(new String[0]);
	}
	
	/**
	 * get location names
	 * @return location names
	 */
	private ArrayList<String> getIndoorLovationNames() {
		ArrayList<String> locationNames = new ArrayList<>();
		for (String name : locationInfo.keySet()) {
			if (indoorLocations.contains(locationInfo.get(name).get(0))) {
				locationNames.add(name);
			}
		}
		return locationNames;
	}
	
	/**
	 * subscribe humidity and noise data 
	 */
	public void subscribeSensorsData() {
		Communicator communicator = Util.initialize();
		
		String topicName = "Sensors";
		String endpointsConfig = "Sensors.Subscriber";
		subscribers.put(topicName, communicator);
		IceHelper.subscriberHandler(topicName, communicator, endpointsConfig, 
				new SensorsDataSubscriber(), topicName);
	}
	
	/**
	 * subscriber for sensor data
	 */
	private class SensorsDataSubscriber implements Sensors{

		/**
		 * get sensor data
		 */
		@Override
		public void sensorData(String username, String sensorType, String data, Current current) {
			// TODO Auto-generated method stub
			ConsoleLog.psLog("Received", "humidity data value and noise data value");
			if (onlineUsersInfo.containsKey(username)) {
				int dataInt = Integer.parseInt(data.split(",")[0]);
				if (sensorType.matches("humidity")) {
					humidityDataHandler(username, dataInt);
				} else if (sensorType.matches("noise")) {
					noiseDataHandler(username, dataInt);
				}
			}
		}
	}
	
	// record last user's humidity value
	private Map<String, Integer> lastUserHumidity = new HashMap<>();
	
	/**
	 * determine whether current humidity value exceed user's limit
	 * @param username
	 * @param data
	 */
	private void humidityDataHandler(String username, int data) {
		if (!lastUserHumidity.containsKey(username)) {
			// publish suggestion to UI
			lastUserHumidity.put(username, data);
			if (data >= onlineUsersInfo.get(username).get(0)) {
				publishHumiditySuggestion(username, data);
			}
		} else {
			if (data != lastUserHumidity.get(username)) {
				// publish suggestion to UI
				lastUserHumidity.put(username, data);
				if (data >= onlineUsersInfo.get(username).get(0)) {
					publishHumiditySuggestion(username, data);
				}
			}
		}
	}
	
	/**
	 * publish humidity suggestion to users
	 * @param username user's name
	 * @param data current humidity value
	 */
	public void publishHumiditySuggestion(String username, int data) {
		String warningType = "humidity";
		int userLimit = onlineUsersInfo.get(username).get(0);
		String currentLocation = usersStatus.get(username).get(0);
		String userPref = contextManager.getUserPrefRMIClient(username, warningType);
		String[] suggestedLocations = new String[0];
		if (weatherAlarmStatus) {
			suggestedLocations = getSuggestedIndoorLocations(userPref);
		} else {
			suggestedLocations = getSuggestedLocations(userPref);
		}
		publishSuggestions(username, warningType, data, userLimit, weatherAlarmStatus, 
				currentLocation, userPref, suggestedLocations);
	}
	
	// record user outdoor duration
	private Map<String, Integer> userOurdoorDuration = new HashMap<>();
	// record user current Noise Pollution Level
	private Map<String, Integer> userCurrentNPL = new HashMap<>(); 
	
	/**
	 * determine whether meet user noise threshold
	 * @param username user's name
	 * @param data noise level
	 */
	private void noiseDataHandler(String username, int data) {
		String warningType = "noise";
		String currentLocation = usersStatus.get(username).get(0);
		
		if (usersStatus.get(username).get(1).matches("outdoor")) {
			if (!userOurdoorDuration.containsKey(username)) {
				userOurdoorDuration.put(username, 1);
				userCurrentNPL.put(username, data);
			} else {
				if (data != userCurrentNPL.get(username)) { 
					userOurdoorDuration.put(username, 1);
					userCurrentNPL.put(username, data);
				} else {
					int hearing = onlineUsersInfo.get(username).get(1);
					if (userOurdoorDuration.get(username) == getNoiseThreshold(data, hearing)) {
						// publish suggestions
						System.err.print("noise");
						int currentValue = userOurdoorDuration.get(username);
						userOurdoorDuration.put(username, currentValue + 1);
						int userLimit = getNoiseThreshold(data, hearing);
						String userPref = contextManager.getUserPrefRMIClient(username, warningType);
						String[] suggestedIndoorLocations = getSuggestedIndoorLocations(userPref);
						publishSuggestions(username, warningType, currentValue, userLimit, weatherAlarmStatus, 
								currentLocation, userPref, suggestedIndoorLocations);
					} else if (userOurdoorDuration.get(username) < getNoiseThreshold(data, hearing)){
						int duration = userOurdoorDuration.get(username);
						userOurdoorDuration.put(username, duration + 1);
					}
				}
			}
		} else {
			if (userOurdoorDuration.containsKey(username)) {
				userOurdoorDuration.remove(username);
				userCurrentNPL.remove(username);
			}
		}
	}
	
	/**
	 * calculate user's noise threshold
	 * @param data noise level
	 * @param hearing user's hearing condition type
	 * @return noise threshold
	 */
	private int getNoiseThreshold(int data, int hearing) {
		int noiseThreshold = 0;
		if (data >= 0 && data <= 70) {
			noiseThreshold = hearing * 30;
		} else if (data >= 71 && data <= 90) {
			noiseThreshold = hearing * 15;
		} else if (data >= 91 && data <= 110) {
			noiseThreshold = hearing * 10;
		} else {
			noiseThreshold = hearing * 5;
		}
		return noiseThreshold;
	}
	
	/**
	 * get online user info
	 */
	public void subscribeOnlineUserInfo() {
		Communicator communicator = Util.initialize();
		
		String topicName = "UserInfo";
		String endpointsConfig = "OnlineUserInfo.Subscriber";
		subscribers.put(topicName, communicator);
		IceHelper.subscriberHandler(topicName, communicator, endpointsConfig, 
				new OnlineUserInfoSubscriber(), topicName);
	}
	
	// record online user info
	private Map<String, ArrayList<Integer>> onlineUsersInfo = new HashMap<>(); // key:username value 0:humidity 1 hearing
	
	/**
	 * subscriber for online info
	 */
	private class OnlineUserInfoSubscriber implements PrefRepoPS{

		/**
		 * get user info
		 */
		@Override
		public void userInfo(String username, int humidity, int hearing, Current current) {
			// TODO Auto-generated method stub
			ConsoleLog.psLog("Received", "username, his or her humidity threshold and hearing condition type");
			if (!onlineUsersInfo.containsKey(username)) {
				onlineUsersInfo.put(username, new ArrayList<>());
				onlineUsersInfo.get(username).add(humidity);
				onlineUsersInfo.get(username).add(hearing);
			}
		}	
	}
	
	/**
	 * subscribe weather condition
	 */
	public void subscribeWeatherData() {
		Communicator communicator = Util.initialize();
		
		String topicName = "Weather";
		String endpointsConfig = "WeatherAlarm.Subscriber";
		IceHelper.subscriberHandler(topicName, communicator, endpointsConfig, 
				new WeatherDataSubscriber(), topicName);
	}
	
	// whether there is a weather alarm
	private boolean weatherAlarmStatus = false;
	
	/**
	 * subscriber for weather condition
	 */
	private class WeatherDataSubscriber implements WeatherAlarm{

		/**
		 * get weather condition
		 */
		@Override
		public void weatherData(String weather, Current current) {
			// TODO Auto-generated method stub	
			ConsoleLog.psLog("Received", weather);
			if (!weather.matches("NORMAL")) {
				//publish suggestion to all online user
				String warningType = "weather";
				weatherAlarmStatus = true;
				for (String username : onlineUsersInfo.keySet()) {
					String currentLocation = usersStatus.get(username).get(0);
					String userPref = contextManager.getUserPrefRMIClient(username, warningType);
					String[] suggestedIndoorLocations = getSuggestedIndoorLocations(userPref);
					publishSuggestions(username, warningType, 0, 0, weatherAlarmStatus,
							currentLocation, userPref, suggestedIndoorLocations);
				}
			} else {
				weatherAlarmStatus = false;
			}
		}
	}
	
	/**
	 * get whether user is in indoor or outdoor
	 */
	public void subscribeUserStatus() {
		Communicator communicator = Util.initialize();
		
		String topicName = "UserStatus";
		String endpointsConfig = "UserStatus.Subscriber";
		subscribers.put(topicName, communicator);
		IceHelper.subscriberHandler(topicName, communicator, endpointsConfig, 
				new UserStatusSubscriber(), topicName);
	}
	
	// record whether user is in indoor or outdoor
	private Map<String, ArrayList<String>> usersStatus = new HashMap<>();//0:userLocation 1:userStatus
	
	/**
	 *	subscriber for whether user is in indoor or outdoor
	 */
	private class UserStatusSubscriber implements LocationServerPS{

		/**
		 * get whether user is in indoor or outdoor
		 */
		@Override
		public void userStatus(String username, String userStatus, String userLocation,
				Current current) {
			// TODO Auto-generated method stub
			ConsoleLog.psLog("Received", "whether user is in indoor or outdoor");
			if (!usersStatus.containsKey(username)) {
				usersStatus.put(username, new ArrayList<>());
				usersStatus.get(username).add(userLocation);
				usersStatus.get(username).add(userStatus);
			} else {
				usersStatus.get(username).set(0, userLocation);
				usersStatus.get(username).set(1, userStatus);
			}
		}
	}
	
	/**
	 * get who exit the app
	 */
	public void subscribeUserExit() {
		Communicator communicator = Util.initialize();
		
		String topicName = "UserExit";
		String endpointsConfig = "UserExit.Subscriber";
		subscribers.put(topicName, communicator);
		IceHelper.subscriberHandler(topicName, communicator, endpointsConfig, 
				new UserExitSubscriber(), endpointsConfig);
	}
	
	/**
	 *	subscriber for user exit signal
	 */
	private class UserExitSubscriber implements UserExitPS{

		/**
		 * get who exit the app
		 */
		@Override
		public void exit(String username, Current current) {
			// TODO Auto-generated method stub
			ConsoleLog.psLog("Received", "user exit signal");
			onlineUsersInfo.remove(username);
			if (userOurdoorDuration.containsKey(username)) {
				userOurdoorDuration.remove(username);
			} else if (userCurrentNPL.containsKey(username)) {
				userCurrentNPL.remove(username);
			} else if (lastUserHumidity.containsKey(username)) {
				lastUserHumidity.remove(username);
			}
			if (onlineUsersInfo.size() == 0) {
				// publish shutdown to LS WA PR
				publishShutdown();
				// server publisher
				destroyHook.start();
				// subscriber
				subscribers.forEach((k, v) -> {
					ShutdownHelper.shutdown(v, k);
				});
				System.exit(0);
			} 
		}
	}
	
	/**
	 *	services for UI
	 */
	private class CMService implements CMRMIServer{

		/**
		 * information about a specific location
		 */
		@Override
		public String specificItemOfInterest(String name, Current current) {
			// TODO Auto-generated method stub
			String result = "";
			if (locationInfo.containsKey(name)) {
				result = locationInfo.get(name).get(1);
			}
			ConsoleLog.rmiLog("Sent", "UI", "information about a specific location");
			return result;
		}

		/**
		 * search for items of interest in user current location
		 */
		@Override
		public String[] searchForItemOfInterest(String username, Current current) {
			// TODO Auto-generated method stub
			String currentLocation = usersStatus.get(username).get(0);
			String locationName = "";
			for (String location : locationInfo.keySet()) {
				if (locationInfo.get(location).get(0).matches(currentLocation)) {
					locationName = location;
				}
			}
			ConsoleLog.rmiLog("Sent", "UI", "List of items of interest in user current location");
			return locationServices.get(locationName).toArray(new String[0]);
		}
	}
}

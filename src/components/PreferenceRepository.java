package components;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.Object;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;

import ComfortSenseCM.ShutdownPS;
import helpers.ConsoleLog;
import helpers.IceHelper;
import helpers.ShutdownHelper;
import services.UserPref;
import services.UserValidation;

/**
 * preference repository component
 */
public class PreferenceRepository {
	// record user information
	private static Map<String, ArrayList<String>> userInfo;  //0:humidity 1:hearing
	// record user preference
	private static Map<String, ArrayList<String>> userPref;  //0:humidity 1:noise 2:weather
	// zero c ice communicator
	private static Communicator communicator;
	// subscriber's communicator
	private Communicator subCommunicator;
	// destroy hook
	private static Thread destroyHook;
	
	/**
	 * to run preference repository
	 * @param args file name
	 */
	public static void main(String[] args) {
		PreferenceRepository pr = new PreferenceRepository();
		initializePrefRepo("datafiles/" + args[0]);
		communicator = Util.initialize();
		
		destroyHook = new Thread(() -> { communicator.destroy(); });
        Runtime.getRuntime().addShutdownHook(destroyHook);
        
        try {
			pr.subscribeShutdown();
		} catch (Exception e) {
			// TODO: handle exception
		}
		userValidationAndGetUserPrefServer(communicator);
		communicator.waitForShutdown();
	}
	
	/**
	 * get information form the file
	 * @param fileName file name
	 */
	public static void initializePrefRepo(String fileName) {
		userInfo = new HashMap<>();
		userPref = new HashMap<>();
		String line = null;
		String username = "";
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(fileName), "UTF-8"));
	        line=reader.readLine();
	        
	        while (line != null) {
				if (line.startsWith("name")) {
					username = line.split(":")[1].trim();
					userInfo.put(username, new ArrayList<>());
					userPref.put(username, new ArrayList<>());
				} else if (line.startsWith("Humidity")) {
					String humidityThreshold = line.split(":")[1].trim();
					userInfo.get(username).add(humidityThreshold);
				} else if (line.startsWith("Hearing")) {
					String hearingConditionType = line.split(":")[1].trim();
					userInfo.get(username).add(hearingConditionType);
				} else if (line.startsWith("pref")) {
					String[] userPreference = line.split(" ");
					userPref.get(username).add(userPreference[userPreference.length - 1]);
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			// TODO: handle exception
			System.out.println("Preference repository file cannot be found.");
		}	
	}
	
	/**
	 * initialize RMI server for context manager
	 * @param communicator zero c ice communicator
	 */
	public static void userValidationAndGetUserPrefServer(Communicator communicator) {
		Object object = (Object) new UserValidation(userInfo, communicator);
		Object object2 = (Object) new UserPref(userPref);
		ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
				"UserValidationServerAdapter", "default -p " + IceHelper.PRPort);
		
		adapter.add(object, Util.stringToIdentity("UserValidation"));
		adapter.add(object2, Util.stringToIdentity("GetUserPref"));
		adapter.activate();
	}
	
	/**
	 * subscribe shutdown signal
	 */
	public void subscribeShutdown() {
		subCommunicator = Util.initialize();
		String topicName = "Shutdown";
		String endpointsConfig = "Shutdown.Subscriber";
		IceHelper.subscriberHandler(topicName, communicator, endpointsConfig, 
				new ShutdownSubscriber(), "PR");
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
				ShutdownHelper.shutdown(subCommunicator, "PR");
				System.exit(0);
			}
		}
	}
}

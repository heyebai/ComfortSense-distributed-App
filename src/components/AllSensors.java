package components;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.CommunicatorDestroyedException;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;
import ComfortSenseCM.ShutdownPS;
import ComfortSenseSensors.SensorsPrx;
import ComfortSenseUI.UserExitPS;
import helpers.ConsoleLog;
import helpers.FileReader;
import helpers.IceHelper;
import helpers.ShutdownHelper;


/**
 * Read and publish sensor data
 */
public class AllSensors {
	// humidity sensor type
	private static String humiditySensor = "humidity";
	// noise sensor type
	private static String noiseSensor = "noise";
	// location sensor type
	private static String locationSensor = "location";
	// sensor timer
	private static int humidityDuration = 0, noiseDuration = 0, locationDuration = 0;
	// user's name
	private static String userName;
	// destroy hook
	private static Thread destroyHook;
	// all subscribers' communicator, used to unsubscribe and destroy
	private Map<String, Communicator> subscribers = new HashMap<>();
	
	/**
	 * to run allSensors component
	 * @param args user's name
	 */
	public static void main(String[] args) {
		AllSensors allSensors = new AllSensors();
		
		Communicator communicator = Util.initialize(args);
		//destroy hook
		destroyHook = new Thread(() -> { communicator.destroy(); });
        Runtime.getRuntime().addShutdownHook(destroyHook);

        userName = args[0];
		try {
			allSensors.subscribeUserExit();
			allSensors.subscribeShutdown();
			publishSensorData(args[0], communicator);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	// file readers
	private static FileReader humidityFileReader, noiseFileReader, locationFileReader;
	
	/**
	 * publish sensor data
	 * @param username user's name
	 * @param communicator zero c ice communicator
	 * @throws IOException
	 */
	private static void publishSensorData(String username, Communicator communicator) throws IOException {
		ObjectPrx sensorsPublisher = IceHelper.getPublisher("Sensors", communicator);
		ObjectPrx locationSensorPublisher = IceHelper.getPublisher("LocationSensor", communicator);
		
		SensorsPrx sensorsPrx = SensorsPrx.uncheckedCast(sensorsPublisher);
		SensorsPrx LocationSensorPrx = SensorsPrx.uncheckedCast(locationSensorPublisher);
		
		try {
			humidityFileReader = new FileReader("datafiles/" + username + "Humidity.txt");
			String humidityData = humidityFileReader.getInfiniteDataRow();

			noiseFileReader = new FileReader("datafiles/" + username + "Noise.txt");
			String noiseData = noiseFileReader.getInfiniteDataRow();
			
			locationFileReader = new FileReader("datafiles/" + username + "Location.txt");	
			String locationData = locationFileReader.getInfiniteDataRow();
			
			while (true) {
				sensorsPrx.sensorData(username, humiditySensor, humidityData);
				sensorsPrx.sensorData(username, noiseSensor, noiseData);
				LocationSensorPrx.sensorData(username, locationSensor, locationData);
				humidityDuration++;
				noiseDuration++;
				locationDuration++;
				if (humidityDuration % Integer.parseInt(humidityData.split(",")[1]) == 0) {
					humidityData = humidityFileReader.getInfiniteDataRow();
					humidityDuration = 0;
				} else if (noiseDuration % Integer.parseInt(noiseData.split(",")[1]) == 0) {
					noiseData = noiseFileReader.getInfiniteDataRow();
					noiseDuration = 0;
				} else if (locationDuration % Integer.parseInt(locationData.split(",")[1]) ==0) {
					locationData = locationFileReader.getInfiniteDataRow();
					locationDuration = 0;
				}
				ConsoleLog.psLog("Sent", "data value");
				try {
                    Thread.currentThread();
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                }
			} 
		} catch (CommunicatorDestroyedException ex) {
        }
	}
	
	/**
	 * subscribe user exit signal
	 */
	public void subscribeUserExit() {
		Communicator communicator = Util.initialize();
		
		String topicName = "UserExit";
		String endpointsConfig = "UserExit.Subscriber";
		subscribers.put(userName, communicator);
		IceHelper.subscriberHandler(topicName, communicator, endpointsConfig, 
				new UserExitSubscriber(), userName);
	}
	
	/**
	 *	subscriber for user exit signal
	 */
	private class UserExitSubscriber implements UserExitPS{

		/**
		 * get the information about who exit the app
		 */
		@Override
		public void exit(String username, Current current) {
			// TODO Auto-generated method stub
//			System.out.println(username);
			if (userName.matches(username)) {
				ConsoleLog.psLog("Received", "user exit signal");
				// shutdown publisher
				destroyHook.start();
				// shutdown subscriber
				subscribers.forEach((k, v) -> {
					ShutdownHelper.shutdown(v, k);
				});
				System.exit(0);
			}
		}
	}
	
	/**
	 * subscribe shutdown signal
	 */
	public void subscribeShutdown() {
		Communicator communicator = Util.initialize();
		
		String topicName = "Shutdown";
		String endpointsConfig = "Shutdown.Subscriber";
		subscribers.put("AS", communicator);
		IceHelper.subscriberHandler(topicName, communicator, endpointsConfig, 
				new ShutdownSubscriber(), "AS");
	}
	
	/**
	 * subscriber for shutdown signal
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

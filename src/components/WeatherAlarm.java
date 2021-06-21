package components;

import java.io.IOException;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.CommunicatorDestroyedException;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;

import ComfortSenseCM.ShutdownPS;
import ComfortSenseWeather.WeatherAlarmPrx;
import helpers.ConsoleLog;
import helpers.FileReader;
import helpers.IceHelper;
import helpers.ShutdownHelper;

/**
 *	weather alarm component
 */
public class WeatherAlarm {
	// publisher communicator
	private static Communicator communicator;
	// subscriber communicator
	private Communicator subCommunicator;
	// destroy hook
	private static Thread destroyHook;
	
	/**
	 * to run weather alarm
	 * @param args file name
	 */
	public static void main(String[] args) {
		WeatherAlarm weatherAlarm = new WeatherAlarm();	
		communicator = Util.initialize(args);
		//destroy hook
		destroyHook = new Thread(() -> { communicator.destroy(); });
        Runtime.getRuntime().addShutdownHook(destroyHook);

		try {
			weatherAlarm.subscribeShutdown();
			publishWeatherData(args[0], communicator);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * publish weather condition
	 * @param fileName file name
	 * @param communicator zero c ice communicator
	 * @throws IOException
	 */
	public static void publishWeatherData(String fileName, Communicator communicator) throws IOException {
		ObjectPrx weatherPublisher = IceHelper.getPublisher("Weather", communicator);
		WeatherAlarmPrx weatherAlarmPrx = WeatherAlarmPrx.uncheckedCast(weatherPublisher);
		
		try {
			FileReader weatherFileReader = new FileReader("datafiles/" + fileName);
			String weatherCondition = weatherFileReader.getInfiniteDataRow();
			while (true) {
				weatherAlarmPrx.weatherData(weatherCondition);
				ConsoleLog.psLog("Sent", weatherCondition);
				try {
                    Thread.currentThread();
                    Thread.sleep(30000);
                }
                catch (InterruptedException e) {
                }
				weatherCondition = weatherFileReader.getInfiniteDataRow();
			}
		} catch (CommunicatorDestroyedException e) {
			// TODO: handle exception
		}
	}
	
	/**
	 * subscribe shutdown signal
	 */
	public void subscribeShutdown() {
		subCommunicator = Util.initialize();
		String topicName = "Shutdown";
		String endpointsConfig = "Shutdown.Subscriber";
		IceHelper.subscriberHandler(topicName, communicator, endpointsConfig, 
				new ShutdownSubscriber(), "WA");
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
				ShutdownHelper.shutdown(subCommunicator, "WA");
				System.exit(0);
			}
		}
		
	}
}

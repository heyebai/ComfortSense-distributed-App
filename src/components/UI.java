package components;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.CommunicatorDestroyedException;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;

import ComfortSenseCM.CMRMIServerPrx;
import ComfortSenseCM.SuggestionsPS;
import ComfortSensePrefRepo.UserValidationPrx;
import ComfortSenseUI.UserExitPSPrx;
import helpers.IceHelper;
import helpers.ShutdownHelper;

/**
 *	UI component
 */
public class UI {
	// user's name
	private String userName;
	// zero c ice communicator
	private static Communicator communicator;
	// destroy hook
	private static Thread destroyHook;
	// subscriber's communicator
	private Communicator subCommunicator;

	/**
	 * to run UI
	 * @param args nothing
	 */
	public static void main(String[] args) {
		communicator = Util.initialize(args);
		//destroy hook
		destroyHook = new Thread(() -> { communicator.destroy(); });
        Runtime.getRuntime().addShutdownHook(destroyHook);
        
		UI ui = new UI();
		while (ui.userLogin());
		try {
			ui.subscribeSuggestions();
		} catch (Exception e) {
			// TODO: handle exception
		}
		while (ui.mainMenu());
		System.exit(0);
	}
	
	/**
	 * handle user login
	 * @return whether user login
	 */
	public boolean userLogin() {
		Scanner input = new Scanner(System.in);
		System.out.println("Context-aware ComfortSense Application");
		System.out.println("Please enter your user name:");
		String name = input.next().trim().toLowerCase();
		String[] userInfo = userValidationRMIClient(name);
		if (userInfo.length > 0) {
			userName = name;
			name = name.substring(0, 1).toUpperCase() + name.substring(1); 
			System.out.println("Hello, " + name);
			System.out.println("Your environmental threshold values are:");
			System.out.println("Humidity: " + userInfo[1]);
			System.out.println("Noise: " + userInfo[2]);
			return false;
		} else {
			System.out.println("Error: The provided name was not found in the stored user profiles. "
					+ "Please check the name, restart the user interface, and enter the name again.");
			return true;
		}
	}
	
	/**
	 * log main menu
	 * @return constantly run main menu
	 */
	public boolean mainMenu() {
		Scanner input = new Scanner(System.in);
		System.out.println("--Please select an option--:");
		System.out.println("1. Search for information on a specific item of interest");
		System.out.println("2. Search for items of interest in current location");
		System.out.println("E. Exit");
		String option = input.next();
		if (option.matches("1")) {
			specificItemOfInterest();
		} else if (option.matches("2")) {
			searchForItemOfInterest();
		} else if (option.matches("E") || option.matches("e")) {
			publishUserExit();
			destroyHook.start();
			ShutdownHelper.shutdown(subCommunicator, userName + "UI");
			return false;
		} else {
			System.out.println("Please re-enter a valid option.");
		}
		return true;
	}
	
	/**
	 * search for specific items of interest
	 */
	private void specificItemOfInterest() {
		Scanner input = new Scanner(System.in);
		System.out.println("Please enter name of item of interest:");
		String itemOfInterest = input.nextLine().trim();
		CMRMIServerPrx cmrmiServerPrx = getLocationInfoRMIClient();
		String result = cmrmiServerPrx.specificItemOfInterest(itemOfInterest);
		if (result.matches("")) {
			System.out.println("No match found for item of interest.");
		} else {
			System.out.println("Information about " + itemOfInterest + ":");
			System.out.println(result);
		}
		String backtoMainMenu = input.nextLine().trim();
		while (!backtoMainMenu.matches("")) {
			backtoMainMenu = input.nextLine().trim();
		}
	}
	
	/**
	 * get items of interest in current location
	 */
	private void searchForItemOfInterest() {
		CMRMIServerPrx cmrmiServerPrx = getLocationInfoRMIClient();
		String[] items = cmrmiServerPrx.searchForItemOfInterest(userName);
		if (items.length == 0) {
			System.out.println("There are no items of interest in your current location.");
		} else {
			System.out.println("The following items of interest are in your location:");
			for (String item : items) {
				System.out.println(item);
			}
		}
		Scanner input = new Scanner(System.in);
		String backtoMainMenu = input.nextLine().trim();
		while (!backtoMainMenu.matches("")) {
			backtoMainMenu = input.nextLine().trim();
		}
	}
	
	/**
	 * validate user
	 * @param username user's name
	 * @return
	 */
	public String[] userValidationRMIClient(String username) {
		ObjectPrx genericProxy = IceHelper.clientRMIHandler(communicator, "UserValidation", IceHelper.PRPort);
		UserValidationPrx userValidationPrx = UserValidationPrx.checkedCast(genericProxy);
		String[] userInfo = userValidationPrx.validate(username);
		return userInfo;
	}
	
	/**
	 * get location information
	 * @return RMI proxy
	 */
	public CMRMIServerPrx getLocationInfoRMIClient() {
		ObjectPrx genericProxy = IceHelper.clientRMIHandler(communicator, "LocationInfo", IceHelper.CMPort);
		return CMRMIServerPrx.checkedCast(genericProxy);
	}
	
	/**
	 * publish user exit signal
	 */
	public void publishUserExit() {
		ObjectPrx userExitPublisher = IceHelper.getPublisher("UserExit", communicator);
		UserExitPSPrx userExitPSPrx = UserExitPSPrx.uncheckedCast(userExitPublisher);
		
		try {
			userExitPSPrx.exit(userName);
		} catch (CommunicatorDestroyedException e) {
			// TODO: handle exception
		}
	}
	
	/**
	 * subscribe suggestions
	 */
	public void subscribeSuggestions() {
		subCommunicator = Util.initialize();
		
		String topicName = "Suggestions";
		String endpointsConfig = "Suggestions.Subscriber";
		IceHelper.subscriberHandler(topicName, subCommunicator, endpointsConfig, 
				new SuggestionsSubscirber(), userName + "UI");
	}
	
	/**
	 *	subscriber for suggestions
	 */
	private class SuggestionsSubscirber implements SuggestionsPS{

		/**
		 * get suggestions
		 */
		@Override
		public void suggestedContent(String username, String warningType, int currentValue, int limitValue,
				boolean weatherAlarm, String currentLocation, String userPref, String[] suggestedLocations,
				Current current) {
			// TODO Auto-generated method stub
			Date date = new Date();
		    SimpleDateFormat ft = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
		    String alarmStatus = "";
			if (username.matches(userName)) {
				if (weatherAlarm) {
					alarmStatus = "There is an alarm";
				} else {
					alarmStatus = "NO ALARM";
				}
				String locations = String.join(", ", suggestedLocations);
				System.out.println("-------------- " + ft.format(date) + "--------------");
				if (warningType.matches("humidity") || warningType.matches("noise")) {
					System.out.println("Warning, " + warningType.toUpperCase() + " is now " + 
							currentValue + " (user limit is " + limitValue + ").");
				} 
				System.out.println("Current Location: " + currentLocation);
				System.out.println("Current weather alarm status: " + alarmStatus);
				System.out.println("Suggestion - please go to the " + userPref.toUpperCase() + " at one of these locations:");
				System.out.println(locations);
				System.out.println();
				System.out.println("<show main menu again here>");
			}
		}
	}
}

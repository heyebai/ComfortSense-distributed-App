package services;

import java.util.ArrayList;
import java.util.Map;

import com.zeroc.Ice.Current;

import helpers.ConsoleLog;

/**
 * preference repository service
 */
public class UserPref implements ComfortSensePrefRepo.UserPref{
	// user preference map
	private Map<String, ArrayList<String>> userPref;
	
	/**
	 * create a preference repository service
	 * @param userPref user preference map
	 */
	public UserPref(Map<String, ArrayList<String>> userPref) {
		this.userPref = userPref;
	}

	/**
	 * get user preference
	 */
	@Override
	public String getUserPref(String username, String triggeringType, Current current) {
		// TODO Auto-generated method stub
		ConsoleLog.rmiLog("Sent", "Context Manager", "user's preference");
		if (triggeringType.matches("humidity")) {
			return userPref.get(username).get(0);
		} else if (triggeringType.matches("noise")) {
			return userPref.get(username).get(1);
		} else {
			return userPref.get(username).get(2);
		}
	}
}

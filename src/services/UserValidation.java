package services;

import java.util.ArrayList;
import java.util.Map;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.CommunicatorDestroyedException;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.ObjectPrx;
import ComfortSensePrefRepo.PrefRepoPSPrx;
import helpers.ConsoleLog;
import helpers.IceHelper;

/**
 * preference repository service
 */
public class UserValidation implements ComfortSensePrefRepo.UserValidation{
	// user information map
	private Map<String, ArrayList<String>> userInfo;
	// user information
	private String username, humidityThreshold, hearingConditionType;
	// zero c ice communicator
	private Communicator communicator;
	
	/**
	 * create a preference repository service
	 * @param userInfo user information map
	 * @param communicator zero c ice communicator
	 */
	public UserValidation(Map<String, ArrayList<String>> userInfo, Communicator communicator) {
		this.userInfo = userInfo;
		this.communicator = communicator;
	}

	/**
	 * validate username
	 */
	@Override
	public String[] validate(String username, Current current) {
		// TODO Auto-generated method stub
		if (userInfo.containsKey(username)) {
			String[] result = new String[3];
			result[0] = username;
			result[1] = userInfo.get(username).get(0);
			result[2] = userInfo.get(username).get(1);
			this.username = result[0];
			this.humidityThreshold = result[1];
			this.hearingConditionType = result[2];
			ConsoleLog.rmiLog("Sent", "UI", "User's humidity threshold and hearing condition type");
			publishUserInfoToCM();
			return result;
		}
		return new String[0];
	}
	
	/**
	 * publish online user information
	 */
	public void publishUserInfoToCM() {
        try {
        	ObjectPrx userInfoPublisher = IceHelper.getPublisher("UserInfo", communicator);
        	PrefRepoPSPrx prefRepoPSPrx = PrefRepoPSPrx.uncheckedCast(userInfoPublisher);
        	
        	try {
				prefRepoPSPrx.userInfo(username, Integer.parseInt(humidityThreshold), 
						Integer.parseInt(hearingConditionType));
				ConsoleLog.psLog("Sent", "User's name, humidity threshold and hearing condition type");
			} catch (CommunicatorDestroyedException e) {
				// TODO: handle exception
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}

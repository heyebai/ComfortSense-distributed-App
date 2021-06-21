package services;

import java.util.ArrayList;

import com.zeroc.Ice.Current;

import ComfortSenseLocation.LocationServerRMI;
import helpers.ConsoleLog;

/**
 * location server service
 */
public class LocationService implements LocationServerRMI{
	// indoor locations
	private ArrayList<String> indoorLocations;
	
	/**
	 * create a location server service
	 * @param indoorLocations indoor locations
	 */
	public LocationService(ArrayList<String> indoorLocations) {
		// TODO Auto-generated constructor stub
		this.indoorLocations = indoorLocations;
	}

	/**
	 * get indoor locations
	 */
	@Override
	public String[] getAllIndoorLocations( Current current) {
		// TODO Auto-generated method stub
		ConsoleLog.rmiLog("Sent", "Context Manager", "indoor locations");
		return indoorLocations.toArray(new String[indoorLocations.size()]);
	}

}

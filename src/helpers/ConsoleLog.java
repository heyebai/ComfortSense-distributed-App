package helpers;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * console log helper
 */
public class ConsoleLog {

	/**
	 * console log information
	 * @param transmissionType sent or received
	 * @param component interacting component
	 * @param value data description or value
	 */
	public static void rmiLog(String transmissionType, String component, String value) {
		 Date date = new Date();
	     SimpleDateFormat ft = new SimpleDateFormat("hh:mm:ss");
	     System.out.println(ft.format(date) + " " + transmissionType + " " +
	     component + " " + '\"' + value + '\"');
	}
	
	/**
	 * console log information
	 * @param transmissionType sent or received
	 * @param value data description or value
	 */
	public static void psLog(String transmissionType, String value) {
		Date date = new Date();
	    SimpleDateFormat ft = new SimpleDateFormat("hh:mm:ss");
	    System.out.println(ft.format(date) + " " + transmissionType + " " + " " + '\"' + value + '\"');
	}
}

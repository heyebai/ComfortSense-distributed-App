module ComfortSenseSensors
{
	// all sensors to cm and location server (pub/sub)
	interface Sensors 
	{
		void sensorData(string username, string sensorType, string data);
	}
}

module ComfortSenseWeather
{
	// weather alarm to cm (pub/sub)
	interface WeatherAlarm 
	{
		void weatherData(string weather);
	}
}

module ComfortSenseLocation
{
	// rmi for cm and location server
	sequence<string> Locations;
	interface LocationServerRMI
	{
		Locations getAllIndoorLocations();
	}
	
	// send user status from location server to cm
	interface LocationServerPS
	{
		void userStatus(string username, string userStatus, string location);
	}
}

module ComfortSensePrefRepo
{
	// rmi between ui and self
	sequence<string> UserInfo;  // 0:username 1:humidity threshold 2:hearing condition type
	//dictionary<string, UserData> UserInfo;
	interface UserValidation
	{
		UserInfo validate(string username);
	}
	
	// send info to cm (pub/sub)
	interface PrefRepoPS
	{
		void userInfo(string username, int humidity, int hearing);
	}
	
	// rmi between cm and self
	interface UserPref
	{
		string getUserPref(string username, string triggeringType);
	}
	
}

module ComfortSenseCM
{
	sequence<string> SuggestedLocations;
	interface SuggestionsPS
	{
		void suggestedContent(string username, string warningType, int currentValue, int limitValue, bool weatherAlarm, 
		string currentLocation, string userPref, SuggestedLocations suggestedLocations);
	}
	
	sequence<string> ItemsOfInterest;
	interface CMRMIServer
	{
		string specificItemOfInterest(string name);
		
		ItemsOfInterest searchForItemOfInterest(string username);
	}
	
	interface ShutdownPS
	{
		void shutdown(bool shutdown);
	}
}

module ComfortSenseUI
{
	interface UserExitPS
	{
		void exit(string username);
	}
}
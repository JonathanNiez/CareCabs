package com.capstone.carecabs.Utility;

import android.net.Uri;

import com.mapbox.geojson.Point;

public class StaticDataPasser {
	public static String storeFirstName = "";
	public static String storeLastName = "";
	public static String storeSelectedSex = "";
	public static String storeSelectedDisability = "";
	public static String storeCurrentBirthDate = "";
	public static String storeRegisterType = "";
	public static String storeRegisterUserType = "";
	public static String storeSelectedMedicalCondition = "";
	public static int storeCurrentAge = 0;
	public static String storeUserType = "";
	public static String storePhoneNumber = "";
	public static int storeFontSize = 17;
	public static String storeCurrentFontSize = "normal";
	public static Uri storeUri = null;
	public static Uri storeProfilePictureUri = null;
	public static Uri storeVehiclePictureUri = null;
	public static String storeProfilePictureURL = "default";
	public static String storeVehiclePictureURL = "none";
	public static String storeSelectedMonth = "";
	public static String storeBirthdate = "";
	public static String storePassengerID = "";
	public static Double storePickupLatitude = 0.0;
	public static Double storePickupLongitude = 0.0;
	public static Double storeDestinationLatitude = 0.0;
	public static Double storeDestinationLongitude = 0.0;
	public static String storeTripID = "";
	public static Point storePoint = Point.fromLngLat(0.0, 0.0);
}

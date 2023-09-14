package com.capstone.carecabs.Utility;

import android.content.Context;
import android.content.pm.PackageManager;
import android.Manifest;

import androidx.core.content.ContextCompat;

public class LocationPermissionChecker {
	private static final String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;

	/**
	 * Check if the location permission is granted.
	 *
	 * @param context The application context.
	 * @return True if the location permission is granted, false otherwise.
	 */
	public static boolean isLocationPermissionGranted(Context context) {
		int permissionStatus = ContextCompat.checkSelfPermission(context, LOCATION_PERMISSION);
		return permissionStatus == PackageManager.PERMISSION_GRANTED;
	}
}

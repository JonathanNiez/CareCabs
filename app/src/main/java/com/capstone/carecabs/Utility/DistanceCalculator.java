package com.capstone.carecabs.Utility;

public class DistanceCalculator {
	private static final int EARTH_RADIUS = 6371; // Radius of the Earth in kilometers

	public static double calculateDistance(double startLat, double startLon, double endLat, double endLon) {
		// Convert latitude and longitude from degrees to radians
		double dLat = Math.toRadians(endLat - startLat);
		double dLon = Math.toRadians(endLon - startLon);

		// Calculate the distance using Haversine formula
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
				Math.cos(Math.toRadians(startLat)) * Math.cos(Math.toRadians(endLat)) *
						Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		// Calculate the distance in kilometers
		double distance = EARTH_RADIUS * c;

		// Convert distance from kilometers to meters
		return distance * 1000;
	}

	public static long calculateArrivalTime(double distance) {
		// Assuming average speed in meters per minute
		double averageSpeed = 50; // Adjust this value based on your use case

		// Calculate time in minutes
		double timeInMinutes = distance / averageSpeed;

		// Convert time from minutes to milliseconds
		return (long) (timeInMinutes * 60 * 1000);
	}
}

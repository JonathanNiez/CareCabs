package com.capstone.carecabs.Model

data class TripModel(
    val tripID: String = "",
    val bookingID: String = "",
    val tripStatus: String = "",
    val userDriverID: String = "",
    val userPassengerID: String = "",
    val tripDate: String = "",
    val currentLongitude: Double = 0.0,
    val currentLatitude: Double = 0.0,
    val destinationLongitude: Double = 0.0,
    val destinationLatitude: Double = 0.0
)

package com.capstone.carecabs.Model

data class TripHistoryModel(
    val tripID: String = "",
    val bookingID: String = "",
    val tripStatus: String = "",
    val driverUserID: String = "",
    val driverName: String = "",
    val passengerUserID: String = "",
    val passengerName: String = "",
    val passengerType: String = "",
    val tripDate: String = "",
    val pickupLocation: String = "",
    val pickupLongitude: Double = 0.0,
    val pickupLatitude: Double = 0.0,
    val destination: String = "",
    val destinationLongitude: Double = 0.0,
    val destinationLatitude: Double = 0.0
)

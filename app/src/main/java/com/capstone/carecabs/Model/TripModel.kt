package com.capstone.carecabs.Model

data class TripModel(
    val tripID: String = "",
    val isComplete : Boolean = false,
    val bookingID: String = "",
    val tripStatus: String = "",
    val driverUserID: String = "",
    val passengerUserID: String = "",
    val tripDate: String = "",
    val pickupLongitude: Double = 0.0,
    val pickupLatitude: Double = 0.0,
    val destinationLongitude: Double = 0.0,
    val destinationLatitude: Double = 0.0
)

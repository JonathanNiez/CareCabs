package com.capstone.carecabs.Model

data class CurrentTripModel(
    val tripID: String = "",
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

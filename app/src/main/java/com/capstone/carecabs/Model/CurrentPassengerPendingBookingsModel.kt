package com.capstone.carecabs.Model

data class CurrentPassengerPendingBookingsModel(
    val passengerUserID: String = "",
    val driverUserID: String = "",
    val bookingID: String = "",
    val tripID: String = "",
    val bookingStatus: String = "",
    val currentLongitude: Double = 0.0,
    val currentLatitude: Double = 0.0,
    val destinationLongitude: Double = 0.0,
    val destinationLatitude: Double = 0.0,
    val bookingTime: String = "",
    val passengerFirstname: String = "",
    val passengerLastname: String = "",
    val passengerProfilePicture: String = "",
    val passengerUserType: String = "",
    val passengerDisability: String = "",
    val passengerMedicalCondition: String = ""
)

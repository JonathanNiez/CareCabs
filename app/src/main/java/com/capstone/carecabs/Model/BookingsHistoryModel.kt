package com.capstone.carecabs.Model

data class BookingsHistoryModel(
    val passengerUserID: String = "",
    val bookingID: String = "",
    val bookingStatus: String = "",
    val pickupLocation: String = "",
    val pickupLongitude: Double = 0.0,
    val pickupLatitude: Double = 0.0,
    val destination: String = "",
    val destinationLongitude: Double = 0.0,
    val destinationLatitude: Double = 0.0,
    val bookingDate: String = "",
    val passengerFirstname: String = "",
    val passengerLastname: String = "",
    val passengerProfilePicture: String = "",
    val passengerUserType: String = "",
    val passengerDisability: String = "",
    val passengerMedicalCondition: String = ""
)

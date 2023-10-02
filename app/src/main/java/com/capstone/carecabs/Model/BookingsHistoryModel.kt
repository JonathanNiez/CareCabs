package com.capstone.carecabs.Model

data class BookingsHistoryModel(
    val passengerUserID: String = "",
    val bookingID: String = "",
    val bookingStatus: String = "",
    val currentLongitude: Double? = null,
    val currentLatitude: Double? = null,
    val destinationLongitude: Double = 0.0, // Provide a default value
    val destinationLatitude: Double = 0.0,  // Provide a default value
    val bookingDate: String = "",
    val passengerFirstname: String = "",
    val passengerLastname: String = "",
    val passengerProfilePicture: String = "",
    val passengerUserType: String = "",
    val passengerDisability: String = "",
    val passengerMedicalCondition: String = ""
)

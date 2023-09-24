package com.capstone.carecabs.Model

data class PWDLocationModel(
    val passengerUserID: String = "",
    val locationID: String = "",
    val currentLongitude: Double? = null,
    val currentLatitude: Double? = null,
    val destinationLongitude: Double = 0.0, // Provide a default value
    val destinationLatitude: Double = 0.0,  // Provide a default value
    val locationTime: String = "",
    val passengerFirstname: String = "",
    val passengerLastname: String = "",
    val passengerProfilePicture: String = "",
    val passengerUserType: String = "",
    val passengerDisability: String = ""
)

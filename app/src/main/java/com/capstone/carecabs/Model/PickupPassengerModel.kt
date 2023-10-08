package com.capstone.carecabs.Model

data class PickupPassengerModel(
    val fcmToken: String = "",
    val passengerUserID: String = "",
    val driverUserID: String = "",
    val vehicleColor: String = "",
    val vehiclePlateNumber: String = "",
    val bookingID: String = "",
    val tripID: String = "",
    val bookingStatus: String = "",
    val pickupLongitude: Double = 0.0,
    val pickupLatitude: Double = 0.0,
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
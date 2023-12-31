package com.capstone.carecabs.Model

data class CurrentBookingModel(
    val passengerUserID: String = "",
    val driverUserID: String = "",
    val vehicleColor: String = "",
    val vehiclePlateNumber: String = "",
    val driverArrivalTime: Long = 0L,
    val driverPingedLocation: String = "Driver location not pinged yet",
    val bookingID: String = "",
    val tripID: String = "",
    val bookingStatus: String = "",
    val ratingStatus : String = "",
    val pickupLongitude: Double = 0.0,
    val pickupLatitude: Double = 0.0,
    val pickupLocation : String = "",
    val destinationLongitude: Double = 0.0,
    val destinationLatitude: Double = 0.0,
    val destination : String = "",
    val bookingTime: String = "",
    val passengerFirstname: String = "",
    val passengerLastname: String = "",
    val passengerProfilePicture: String = "",
    val passengerUserType: String = "",
    val passengerDisability: String = "",
    val passengerMedicalCondition: String = ""
)

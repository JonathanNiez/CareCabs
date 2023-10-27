package com.capstone.carecabs.Model

data class PassengerBookingModel(
    val bookingID: String = "",
    val bookingStatus: String = "",
    val ratingStatus : String = "",
    val bookingDate: String = "",
    val fcmToken: String = "",
    val tripID: String = "",
    val passengerUserID: String = "",
    val passengerName: String = "",
    val passengerType: String = "",
    val passengerProfilePicture: String = "",
    val passengerMedicalCondition: String = "",
    val passengerDisability: String = "",
    val driverUserID: String = "",
    val driverName : String = "",
    val driverProfilePicture : String = "",
    val driverArrivalTime: Long = 0L,
    val vehicleColor: String = "",
    val vehiclePlateNumber: String = "",
    val pickupLocation : String = "",
    val pickupLongitude: Double = 0.0,
    val pickupLatitude: Double = 0.0,
    val destinationLocation : String = "",
    val destinationLongitude: Double = 0.0,
    val destinationLatitude: Double = 0.0,
)


package com.capstone.carecabs.Model

data class DriverModel(
    val isRegisterComplete : Boolean = false,
    val firstname : String = "",
    val lastname : String = "",
    val age : Int = 0,
    val isAvailable : Boolean = true,
    val birthDate : String = "",
    val sex : String = "",
    val userType : String = "",
    val driverRating : Double = 0.0,
    val passengersTransported : Int = 0,
    val vehicleColor : String = "",
    val vehiclePlateNumber : String = ""
)

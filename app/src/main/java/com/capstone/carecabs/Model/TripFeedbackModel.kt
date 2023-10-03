package com.capstone.carecabs.Model

data class TripFeedbackModel(
    val tripFeedbackID : String = "",
    val passengerID : String = "",
    val driverID : String = "",
    val tripID : String = "",
    val driverRatings : Double = 0.0,
    val comment : String = "",
    val isDone : Boolean = false
)

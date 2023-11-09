package com.capstone.carecabs.Model

import com.mapbox.geojson.Point

data class PickupPassengerBottomSheetData(
    val bookingID : String,
    val passengerID : String,
    val passengerName : String,
    val passengerType : String,
    val driverName : String,
    val pickupLocation : String,
    val pickupCoordinates : Point,
    val destination : String,
    val destinationCoordinates : Point
)

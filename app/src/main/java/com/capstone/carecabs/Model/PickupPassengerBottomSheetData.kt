package com.capstone.carecabs.Model

import com.mapbox.geojson.Point

data class PickupPassengerBottomSheetData(
    val bookingID : String,
    val passengerID : String,
    val pickupCoordinates : Point,
    val destinationCoordinates : Point
)

package com.capstone.carecabs.Model

import com.mapbox.geojson.Point

data class RenavigateData(
    val pickupCoordinates: Point,
    val destinationCoordinates: Point
)

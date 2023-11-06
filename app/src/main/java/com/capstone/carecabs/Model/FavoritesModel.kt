package com.capstone.carecabs.Model

data class FavoritesModel(
    val favoriteID: String = "",
    val bookingID: String = "",
    val userID: String = "",
    val destination: String = "",
    val destinationLatitude: Double = 0.0,
    val destinationLongitude: Double = 0.0,
)

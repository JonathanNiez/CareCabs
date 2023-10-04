package com.capstone.carecabs.Model

data class ChatOverviewModel(
    val chatID: String = "",
    val bookingID: String = "",
    val chatDate: String = "",
    val sender: String = "",
    val receiver: String = "",
    val message: String = "",
    val chatStatus : String = ""
)

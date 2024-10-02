package com.sciencehackfest.trive_sci.model

import com.google.firebase.Timestamp

data class Reservation(
    var id: String = "",
    var chargerType: String = "",
    var chargerCapacity: String = "",
    var stationName: String = "",
    var stationImg: String = "",
    var address: String = "",
    var price: Long = 0,
    var power: Double = 0.0,
    val mail: String = "",
    var createdAt: Timestamp? = null
)

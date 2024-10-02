package com.sciencehackfest.trive_sci.model

data class Charger(
    var id: Int? = 0,
    val stationName: String = "",
    val chargerType: String = "",
    val chargerCapacity: String = "",
    val chargerTotal: Int = 0
)
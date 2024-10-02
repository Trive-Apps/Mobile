package com.sciencehackfest.triveadmin

import com.google.firebase.Timestamp

data class Partner(
    var id: String? = null,
    val stationName: String = "",
    val stationImg: String = "",
    val description: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    val address: String = "",
    val timeOpen: String = "",
    val timeClose: String = "",
    val haveBank: Boolean = false,
    val haveCoffeeShop: Boolean = false,
    val haveMosque: Boolean = false,
    val haveMinimart: Boolean = false,
    val haveRestaurant: Boolean = false,
    val haveToilet: Boolean = false,
    val haveWifi: Boolean = false,
    val price: Long = 0,
    val owner: String = "",
    val mail: String = "",
    val phone: String = "",
    var created: Timestamp = Timestamp.now()
)
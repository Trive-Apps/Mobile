package com.sciencehackfest.trive_sci.model

import com.google.firebase.Timestamp

data class Payment(
    val barcode: String = "",
    val carBrand: String = "",
    var carType: String = "",
    var name: String = "",
    val capacity: String = "",
    val type: String = "",
    val power: Long = 0,
    val price: Long = 0,
    var created: Timestamp = Timestamp.now()
)
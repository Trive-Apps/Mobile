package com.sciencehackfest.trive_sci.model

import com.google.firebase.Timestamp

data class User(
    var mail: String,
    var fullname: String,
    var phone: String,
    var money: Long,
    var point: Long,
    var token: String,
    var created: Timestamp
)

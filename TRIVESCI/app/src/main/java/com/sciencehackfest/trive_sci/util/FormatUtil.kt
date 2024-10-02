package com.sciencehackfest.trive_sci.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

fun timestampToString(timestamp: Timestamp): String? {
    return if (timestamp != null) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        dateFormat.format(timestamp.toDate())
    } else null
}
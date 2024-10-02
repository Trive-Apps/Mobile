package com.sciencehackfest.trive_sci.model

data class Car(
    var token: String = "",
    var type: String = "",
    var brand: String = "",
    var chargerType: String = "",
    var isActive: Boolean = false,
    val voltage: Float = 0f,
    val temperature: Float = 0f,
    val current: Float = 0f,
    val power: Float = 0f,
    val persentagePower: Float = 0f,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var timestamp: Long = 0L
)

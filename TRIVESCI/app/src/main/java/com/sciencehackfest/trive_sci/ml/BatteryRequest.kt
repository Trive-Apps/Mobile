package com.sciencehackfest.trive_sci.ml

data class BatteryRequest(
    val battery_temperature: Float,
    val battery_voltage: Float,
    val battery_current: Float,
    val weather_cloudy: Int,
    val weather_dark: Int,
    val weather_dark_little_rainy: Int,
    val weather_rainy: Int,
    val weather_slightly_cloudy: Int,
    val weather_sunny: Int,
    val weather_sunrise: Int,
    val weather_sunset: Int
)
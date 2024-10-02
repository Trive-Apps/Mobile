package com.sciencehackfest.trive_sci.ml

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface BatteryService {
    @POST("predict")
    fun predictBatteryHealth(@Body requestBody: BatteryRequest): Call<BatteryResponse>
}
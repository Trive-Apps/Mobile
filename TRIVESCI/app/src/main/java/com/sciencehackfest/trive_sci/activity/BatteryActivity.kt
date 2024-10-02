package com.sciencehackfest.trive_sci.activity

import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.sciencehackfest.trive_sci.R
import com.sciencehackfest.trive_sci.databinding.ActivityBatteryBinding
import com.sciencehackfest.trive_sci.ml.BatteryRequest
import com.sciencehackfest.trive_sci.ml.BatteryResponse
import com.sciencehackfest.trive_sci.ml.BatteryService
import com.sciencehackfest.trive_sci.model.Car
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BatteryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBatteryBinding
    private val token by lazy { intent.getStringExtra("token") }
    private val db by lazy { Firebase.firestore }
    private lateinit var car: Car

    val retrofit = Retrofit.Builder()
        .baseUrl("https://trive-api-564094235109.us-central1.run.app/") // Base URL
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val batteryService = retrofit.create(BatteryService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityBatteryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigationBarColor()

        ViewCompat.setOnApplyWindowInsetsListener(binding.battery) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.percentProgressBattery.max = 100

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        getData()
    }

    private fun getData() {
        db.collection("car")
            .whereEqualTo("token", token)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]

                    car = Car(
                        token = document.getString("token").orEmpty(),
                        type = document.getString("type").orEmpty(),
                        brand = document.getString("brand").orEmpty(),
                        isActive = document.getBoolean("isActive") ?: false,
                        voltage = document.getDouble("voltage")?.toFloat() ?: 0f,
                        temperature = document.getDouble("temperature")?.toFloat() ?: 0f,
                        current = document.getDouble("current")?.toFloat() ?: 0f,
                        power = document.getDouble("power")?.toFloat() ?: 0f,
                        persentagePower = document.getDouble("persentagePower")?.toFloat() ?: 0f,
                        latitude = document.getDouble("latitude") ?: 0.0,
                        longitude = document.getDouble("logitude") ?: 0.0,
                    )

                    predictBatteryHealth(car)

                    val maxPower = car.power / (car.persentagePower / 100)
                    binding.currentBattery.text = String.format("%.2f", car.power)
                    binding.totalBattery.text = String.format("/%.2f kWh", maxPower)

                    val currentProgressBattery = car.persentagePower.toInt()
                    ObjectAnimator.ofInt(binding.percentProgressBattery, "progress", currentProgressBattery)
                        .setDuration(1000)
                        .start()

                    binding.percentBattery.text = "${currentProgressBattery}% Tersedia"

                    when {
                        currentProgressBattery in 91..100 -> {
                            binding.percentProgressBattery.progressDrawable = ContextCompat.getDrawable(this, R.drawable.progress_bar_high)
                            binding.icBattery.setImageResource(R.drawable.ic_battery_full)
                            binding.icBattery.setColorFilter(ContextCompat.getColor(this, R.color.primary400))
                        }
                        currentProgressBattery in 61..90 -> {
                            binding.percentProgressBattery.progressDrawable = ContextCompat.getDrawable(this, R.drawable.progress_bar_high)
                            binding.icBattery.setImageResource(R.drawable.ic_battery_high)
                            binding.icBattery.setColorFilter(ContextCompat.getColor(this, R.color.primary400))
                        }
                        currentProgressBattery in 31..60 -> {
                            binding.percentProgressBattery.progressDrawable = ContextCompat.getDrawable(this, R.drawable.progress_bar_medium)
                            binding.icBattery.setImageResource(R.drawable.ic_battery_medium)
                            binding.icBattery.setColorFilter(ContextCompat.getColor(this, R.color.secondary400))
                        }
                        currentProgressBattery in 1..30 -> {
                            binding.percentProgressBattery.progressDrawable = ContextCompat.getDrawable(this, R.drawable.progress_bar_low)
                            binding.icBattery.setImageResource(R.drawable.ic_battery_low)
                            binding.icBattery.setColorFilter(ContextCompat.getColor(this, R.color.system_red))
                        }
                        currentProgressBattery == 0 -> {
                            binding.percentProgressBattery.progressDrawable = ContextCompat.getDrawable(this, R.drawable.progress_bar_low)
                            binding.icBattery.setImageResource(R.drawable.ic_battery_empty)
                            binding.icBattery.setColorFilter(ContextCompat.getColor(this, R.color.system_red))
                        }
                    }

                    binding.voltage.text = String.format("%.1f V", car.voltage)
                    binding.current.text = String.format("%.2f A", car.current)
                    binding.temp.text = String.format("%.1f°C", car.temperature)
                }
            }
    }

    private fun predictBatteryHealth(car: Car) {
        val batteryRequest = BatteryRequest(
            battery_temperature = car.temperature,
            battery_voltage = car.voltage,
            battery_current = car.current,
            weather_cloudy = 0,
            weather_dark = 0,
            weather_dark_little_rainy = 0,
            weather_rainy = 0,
            weather_slightly_cloudy = 0,
            weather_sunny = 0,
            weather_sunrise = 0,
            weather_sunset = 1
        )

        val call = batteryService.predictBatteryHealth(batteryRequest)
        call.enqueue(object : retrofit2.Callback<BatteryResponse> {
            override fun onResponse(call: Call<BatteryResponse>, response: retrofit2.Response<BatteryResponse>) {
                if (response.isSuccessful) {
                    val predictedSoc = response.body()?.predicted_soc ?: 0f
                    val predictedSocPercent =  predictedSoc * 100
                    binding.batteryHealth.visibility = View.VISIBLE
                    binding.batteryProgress.visibility = View.GONE
                    binding.batteryHealth.text = String.format("%.2f%%", predictedSocPercent)
                } else {
                    binding.batteryHealth.visibility = View.VISIBLE
                    binding.batteryProgress.visibility = View.GONE
                    binding.batteryHealth.text = "Predict error"
                }
            }
            override fun onFailure(call: Call<BatteryResponse>, t: Throwable) {
                binding.batteryHealth.visibility = View.VISIBLE
                binding.batteryProgress.visibility = View.GONE
                binding.batteryHealth.text = "${t.message}"
            }
        })
    }


    private fun setNavigationBarColor() {
        val statusBarColor = ContextCompat.getColor(this, R.color.backgroundPrimary)
        window.statusBarColor = statusBarColor
        val navigationBarColor = ContextCompat.getColor(this, R.color.backgroundSecondary)
        window.navigationBarColor = navigationBarColor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }
}
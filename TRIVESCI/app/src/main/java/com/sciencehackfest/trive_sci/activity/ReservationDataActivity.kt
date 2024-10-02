package com.sciencehackfest.trive_sci.activity

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.midtrans.sdk.corekit.core.MidtransSDK
import com.midtrans.sdk.corekit.core.TransactionRequest
import com.midtrans.sdk.corekit.core.themes.CustomColorTheme
import com.midtrans.sdk.uikit.SdkUIFlowBuilder
import com.sciencehackfest.trive_sci.R
import com.sciencehackfest.trive_sci.adapter.ChargerAdapter
import com.sciencehackfest.trive_sci.adapter.ReservationAdapter
import com.sciencehackfest.trive_sci.databinding.ActivityReservationDataBinding
import com.sciencehackfest.trive_sci.databinding.ActivityTopupBinding
import com.sciencehackfest.trive_sci.model.Charger
import com.sciencehackfest.trive_sci.model.Community
import com.sciencehackfest.trive_sci.model.Reservation
import com.sciencehackfest.trive_sci.model.User
import com.sciencehackfest.trive_sci.preference.PreferenceManager
import com.sciencehackfest.trive_sci.util.PrefUtil
import com.sciencehackfest.trive_sci.view.CurrencyTextWatcher
import com.sciencehackfest.trive_sci.view.SpacingItemDecoration

class ReservationDataActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReservationDataBinding
    private val pref by lazy { PreferenceManager(this) }
    private val db by lazy { Firebase.firestore }
    private lateinit var user: User
    private lateinit var reservationAdapter: ReservationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityReservationDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigationBarColor()
        setupList()

        ViewCompat.setOnApplyWindowInsetsListener(binding.topup) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onStart() {
        super.onStart()
        getReservationData()
    }

    private fun setupList() {
        reservationAdapter = ReservationAdapter(
            arrayListOf(),
            object : ReservationAdapter.AdapterListener {
                override fun onClick(reservation: Reservation) {
                }
            }
        )
        binding.rvReservation.adapter = reservationAdapter
        binding.rvReservation.addItemDecoration(SpacingItemDecoration(0, 20))

    }

    private fun getReservationData() {
        binding.progressBar.visibility = View.VISIBLE
        val mail = pref.getString(PrefUtil.pref_mail)
        val reservations: ArrayList<Reservation> = arrayListOf()
        db.collection("reservation")
            .whereEqualTo("mail", mail)
            .get()
            .addOnSuccessListener { result ->
                binding.progressBar.visibility = View.GONE
                result.forEach { doc ->
                    reservations.add(
                        Reservation(
                            id = doc.reference.id,
                            stationName = doc.data["stationName"].toString(),
                            stationImg = doc.data["stationImg"].toString(),
                            address = doc.data["address"].toString(),
                            chargerType = doc.data["chargerType"].toString(),
                            chargerCapacity = doc.data["chargerCapacity"].toString(),
                            price = (doc.data["price"] as? Number)?.toLong() ?: 0L,
                            power = (doc.data["power"] as? Number)?.toDouble() ?: 0.0,
                            mail = doc.data["mail"].toString(),
                            createdAt = doc.data["createdAt"] as Timestamp
                        )
                    )
                }
                reservationAdapter.setData(reservations)
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun setNavigationBarColor() {
        val statusBarColor = ContextCompat.getColor(this, R.color.backgroundPrimary)
        window.statusBarColor = statusBarColor
        val color = ContextCompat.getColor(this, R.color.backgroundSecondary)
        window.navigationBarColor = color
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
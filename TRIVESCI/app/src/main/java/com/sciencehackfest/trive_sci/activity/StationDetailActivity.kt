package com.sciencehackfest.trive_sci.activity

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.GridLayout
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.tabs.TabLayout
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.sciencehackfest.trive_sci.R
import com.sciencehackfest.trive_sci.adapter.ChargerAdapter
import com.sciencehackfest.trive_sci.databinding.ActivityStationDetailBinding
import com.sciencehackfest.trive_sci.model.Car
import com.sciencehackfest.trive_sci.model.Charger
import com.sciencehackfest.trive_sci.model.Partner
import com.sciencehackfest.trive_sci.preference.PreferenceManager
import com.sciencehackfest.trive_sci.util.PrefUtil
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.lang.Exception
import kotlin.time.times

class StationDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStationDetailBinding
    private val partnerId by lazy { intent.getStringExtra("id") }
    private val distance by lazy { intent.getStringExtra("distance") }
    private val db by lazy { Firebase.firestore }
    private lateinit var partner: Partner
    private lateinit var chargerAdapter: ChargerAdapter
    private val pref by lazy { PreferenceManager(this) }
    private lateinit var car: Car

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityStationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupList()
        setNavigationBarColor()

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY > 0) {
                setStatusBarWhite()
            } else {
                setStatusBarTransparent()
            }
        }

        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val backgroundOpacity = Color.parseColor("#FFFFFFFF")
            if (scrollY > 0) {
                binding.navbar.setBackgroundColor(resources.getColor(R.color.backgroundPrimary))
                binding.btnClose.backgroundTintList = ContextCompat.getColorStateList(this, R.color.backgroundPrimary)
                binding.titleNavbar.visibility = View.VISIBLE
            } else {
                binding.navbar.setBackgroundColor(resources.getColor(android.R.color.transparent))
                binding.btnClose.backgroundTintList = ColorStateList.valueOf(backgroundOpacity)
                binding.titleNavbar.visibility = View.INVISIBLE
            }
        }

        val statusBarHeightResId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (statusBarHeightResId > 0) {
            resources.getDimensionPixelSize(statusBarHeightResId)
        } else {
            0
        }

        val additionalPaddingInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 14f, resources.displayMetrics
        ).toInt()

        val totalPaddingTop = statusBarHeight + additionalPaddingInPx

        binding.navbar.setPadding(
            binding.navbar.paddingLeft,
            totalPaddingTop,
            binding.navbar.paddingRight,
            binding.navbar.paddingBottom
        )

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        binding.chargerList.visibility = View.VISIBLE
                        binding.pricingList.visibility = View.GONE
                        binding.informationList.visibility = View.GONE
                    }
                    1 -> {
                        binding.chargerList.visibility = View.GONE
                        binding.pricingList.visibility = View.VISIBLE
                        binding.informationList.visibility = View.GONE
                    }
                    2 -> {
                        binding.chargerList.visibility = View.GONE
                        binding.pricingList.visibility = View.GONE
                        binding.informationList.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.btnClose.setOnClickListener {
            finish()
        }

        binding.btnReservation.setOnClickListener {
            val reservationIntent = Intent(this, ReservationActivity::class.java)
            reservationIntent.putExtra("id", partnerId)
            startActivity(reservationIntent)
        }
    }

    override fun onStart() {
        super.onStart()
        detailPartner()
    }

    private fun setupList() {
        chargerAdapter = ChargerAdapter(
            arrayListOf(),
            object : ChargerAdapter.AdapterListener {
                override fun onClick(charger: Charger) {
                }
            }
        )
        binding.chargerList.adapter = chargerAdapter
    }

    private fun removeViewIfFalse(condition: Boolean, view: View) {
        if (!condition) {
            val parent = view.parent as? ViewGroup
            parent?.removeView(view)
        }
    }

    private fun detailPartner() {
        db.collection("partner")
            .document(partnerId!!)
            .get()
            .addOnSuccessListener { result ->
                partner = Partner (
                    id = result.id,
                    stationName = result["stationName"].toString(),
                    stationImg = result["stationImg"].toString(),
                    address = result["address"].toString(),
                    description = result["description"].toString(),
                    latitude = result["latitude"].toString().toDouble(),
                    longitude = result["longitude"].toString().toDouble(),
                    timeOpen = result["timeOpen"].toString(),
                    timeClose = result["timeClose"].toString(),
                    haveBank = result.getBoolean("haveBank") ?: false,
                    haveCoffeeShop = result.getBoolean("haveCoffeeShop") ?: false,
                    haveMosque = result.getBoolean("haveMosque") ?: false,
                    haveMinimart = result.getBoolean("haveMinimart") ?: false,
                    haveRestaurant = result.getBoolean("haveRestaurant") ?: false,
                    haveToilet = result.getBoolean("haveToilet") ?: false,
                    haveWifi = result.getBoolean("haveWifi") ?: false,
                    price = result["price"].toString().toLong(),
                    owner = result["wwner"].toString(),
                    mail = result["mail"].toString(),
                    phone = result["phone"].toString(),
                    created = result["created"] as Timestamp,
                )

                binding.stationName.setText( partner.stationName )
                binding.address.setText( partner.address )
                binding.time.setText( "${partner.timeOpen}-${partner.timeClose}" )
                binding.distance.setText( distance )

                if (partner.description.isEmpty()) {
                    binding.descriptionLayout.visibility = View.GONE
                } else {
                    binding.description.setText( partner.description )
                }
                removeViewIfFalse(partner.haveBank, binding.isHaveBank)
                removeViewIfFalse(partner.haveCoffeeShop, binding.isHaveCoffeeShop)
                removeViewIfFalse(partner.haveMosque, binding.isHaveMosque)
                removeViewIfFalse(partner.haveMinimart, binding.isHaveMinimart)
                removeViewIfFalse(partner.haveRestaurant, binding.isHaveRestaurant)
                removeViewIfFalse(partner.haveToilet, binding.isHaveToilet)
                removeViewIfFalse(partner.haveWifi, binding.isHaveWifi)

                val viewsToShow = listOf(
                    binding.isHaveBank to partner.haveBank,
                    binding.isHaveCoffeeShop to partner.haveCoffeeShop,
                    binding.isHaveMosque to partner.haveMosque,
                    binding.isHaveMinimart to partner.haveMinimart,
                    binding.isHaveRestaurant to partner.haveRestaurant,
                    binding.isHaveToilet to partner.haveToilet,
                    binding.isHaveWifi to partner.haveWifi
                ).filter { it.second }

                viewsToShow.forEach { (view, _) -> removeViewIfFalse(true, view) }
                val visibleCount = viewsToShow.size
                val emptyLayoutsToAdd = when {
                    visibleCount == 1 -> 3
                    visibleCount == 2 -> 2
                    visibleCount == 3 -> 1
                    else -> 0
                }

                if ( visibleCount == 0) {
                    binding.titleFacility.text = "Tidak ada fasilitas yang tersedia"
                } else {
                    for (i in 0 until emptyLayoutsToAdd) {
                        val emptyLayout = LinearLayout(this).apply {
                            layoutParams = GridLayout.LayoutParams().apply {
                                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f) // Menggunakan columnWeight
                            }
                            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
                        }
                        binding.facility.addView(emptyLayout) // Ganti parentLayout dengan parent yang sesuai
                    }
                }

                val stationImg = partner.stationImg
                Picasso.get().load(stationImg).into(binding.stationImg, object : Callback {
                    override fun onSuccess() {}
                    override fun onError(e: Exception?) {}
                })

                val chargers: ArrayList<Charger> = arrayListOf()
                db.collection("charger")
                    .whereEqualTo("stationName", partner.stationName)
                    .get()
                    .addOnSuccessListener { result ->
                        var totalUnits = 0
                        result.forEach { doc ->
                            val chargerTotal = doc.data["chargerTotal"].toString().toInt()
                            totalUnits += chargerTotal
                            chargers.add(
                                Charger(
                                    stationName = doc.data["stationName"].toString(),
                                    chargerType = doc.data["chargerType"].toString(),
                                    chargerCapacity = doc.data["chargerCapacity"].toString(),
                                    chargerTotal = doc.data["chargerTotal"].toString().toInt(),
                                )
                            )
                        }
                        chargerAdapter.setData(chargers)
                        binding.totalCharger.text = "$totalUnits/$totalUnits"
                    }

                db.collection("car")
                    .whereEqualTo("token", pref.getString(PrefUtil.pref_token))
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
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
                                persentagePower = document.getDouble(" persentagePower = ")?.toFloat() ?: 0f,
                                latitude = document.getDouble("latitude") ?: 0.0,
                                longitude = document.getDouble("longitude") ?: 0.0
                            )

                            val bmwPower = 0.163 // kWh per kilometer
                            val distanceFormatted = distance?.replace(",", "")?.toFloat() ?: 0f
                            val powerPredict = bmwPower * distanceFormatted
                            val formattedPowerPredict = String.format("%.2f", powerPredict)
                            binding.power.text = "$formattedPowerPredict kWh"
                            if (car.power < powerPredict) {
                                binding.batteryNotEnough.visibility = View.VISIBLE
                            } else {
                                binding.batteryEnough.visibility = View.VISIBLE
                            }
                        } else {
                            binding.isNotHaveEv.visibility = View.VISIBLE
                            binding.isHaveEv.visibility = View.GONE
                        }
                    }
            }
    }


    private fun setNavigationBarColor() {
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

    private fun setStatusBarWhite() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.backgroundSecondary)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    private fun setStatusBarTransparent() {
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController?.setSystemBarsAppearance(
                0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

}
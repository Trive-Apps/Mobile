package com.sciencehackfest.triveadmin

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.sciencehackfest.triveadmin.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    lateinit var binding: ActivityMainBinding
    lateinit var googleMap: GoogleMap
    val db by lazy { Firebase.firestore }
    private lateinit var stationAdapter: StationAdapter
    private val boundsBuilder = LatLngBounds.Builder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setNavigationBarColor()
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        stationAdapter = StationAdapter(
            arrayListOf(),
            object : StationAdapter.AdapterListener {
                override fun onClick(station: Partner) {
                }
            }
        )
        binding.rvStation?.adapter = stationAdapter
        binding.rvStation?.addItemDecoration(SpacingItemDecoration(8, 24))
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap

        val indonesiaBounds = LatLngBounds(
            LatLng(-11.0, 95.0),
            LatLng(6.0, 141.0)
        )
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(indonesiaBounds, 100))
        fetchPartners()
    }

    private fun fetchPartners() {
        val db = com.google.firebase.ktx.Firebase.firestore
        db.collection("partner")
            .get()
            .addOnSuccessListener { result ->
                val partners = mutableListOf<Partner>()
                for (document in result) {
                    val id = document.id
                    val partner = document.toObject(Partner::class.java).copy(id = id)
                    partners.add(partner)
                    addManyMarker(partners)
                }
                val totalData = partners.size
                binding.station?.text = totalData.toString()
                binding.active?.text = totalData.toString()
                binding.activeEnd?.text = "/$totalData"
                binding.inactiveEnd?.text = "/$totalData"
                stationAdapter.setData(partners)
            }
            .addOnFailureListener { exception -> }
    }

    private fun addManyMarker(partners: List<Partner>) {
        partners.forEach { partner ->
            val latLng = LatLng(partner.latitude, partner.longitude)
            val markerOptions = MarkerOptions()
                .position(latLng)
                .title(partner.stationName)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.img_marker_station))

            val marker = googleMap.addMarker(markerOptions)

            marker?.tag = partner
            boundsBuilder.include(latLng)
        }
    }

    private fun setNavigationBarColor() {
        val statusBarColor = ContextCompat.getColor(this, R.color.backgroundPrimary)
        window.statusBarColor = statusBarColor
        val color = ContextCompat.getColor(this, R.color.black)
        window.navigationBarColor = color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController?.setSystemBarsAppearance(
                0,
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        }
    }
}
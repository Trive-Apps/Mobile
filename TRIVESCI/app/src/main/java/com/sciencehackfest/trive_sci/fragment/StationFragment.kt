package com.sciencehackfest.trive_sci.fragment

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.sciencehackfest.trive_sci.R
import com.sciencehackfest.trive_sci.activity.StationDetailActivity
import com.sciencehackfest.trive_sci.model.Car
import com.sciencehackfest.trive_sci.model.Charger
import com.sciencehackfest.trive_sci.model.GoogleMapDTO
import com.sciencehackfest.trive_sci.model.Partner
import com.sciencehackfest.trive_sci.preference.PreferenceManager
import com.sciencehackfest.trive_sci.util.PrefUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StationFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private val boundsBuilder = LatLngBounds.Builder()
    private lateinit var searchContainer: View
    private lateinit var containerPopup: View
    private lateinit var stationName: TextView
    private lateinit var address: TextView
    private lateinit var isOpen: TextView
    private lateinit var chargerType: TextView
    private lateinit var chargerTotal: TextView
    private lateinit var distance: TextView
    private lateinit var duration: TextView
    private val pref by lazy { PreferenceManager(requireContext()) }
    private lateinit var btnDetail: LinearLayout
    private lateinit var btnReservation: LinearLayout
    private val db by lazy { Firebase.firestore }
    private var lastClickedMarker: Marker? = null
    private var chargersList: List<Charger> = listOf()
    private var currentPolyline: Polyline? = null
    private lateinit var car: Car
    private val fromBottom: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.from_bottom_anim) }
    private val toBottom: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.to_bottom_anim) }
    private var calculatedDistance: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_station, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        containerPopup = view.findViewById(R.id.container_popup)
        stationName = view.findViewById(R.id.station_name)
        address = view.findViewById(R.id.address)
        isOpen = view.findViewById(R.id.is_open)
        chargerType = view.findViewById(R.id.charger_type)
        chargerTotal = view.findViewById(R.id.charger_total)
        distance = view.findViewById(R.id.distance)
        duration = view.findViewById(R.id.duration)
        btnDetail = view.findViewById(R.id.btn_detail)
        btnReservation = view.findViewById(R.id.btn_reservation)


        activity?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.insetsController?.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                window.statusBarColor = android.graphics.Color.TRANSPARENT
            }
        }

        val btnFocus = view.findViewById<ImageButton>(R.id.btn_focus)
        btnFocus.setOnClickListener {
            focusToUserLocation()
        }

        val statusBarHeightResId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (statusBarHeightResId > 0) {
            resources.getDimensionPixelSize(statusBarHeightResId)
        } else {
            0
        }

        val additionalMarginInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics
        ).toInt()

        val totalMarginTop = statusBarHeight + additionalMarginInPx

        searchContainer = view.findViewById(R.id.search_container)
        val layoutParams = searchContainer.layoutParams as ViewGroup.MarginLayoutParams

        layoutParams.setMargins(
            searchContainer.marginLeft,
            totalMarginTop,
            searchContainer.marginRight,
            searchContainer.marginBottom
        )

        searchContainer.layoutParams = layoutParams

        getCar()

        val btnCarFocus = view.findViewById<ImageButton>(R.id.btn_car_focus)
        btnCarFocus.setOnClickListener {
            focusToCarLocation()
        }
    }

    private fun getCar() {
        db.collection("car")
            .whereEqualTo("token", pref.getString(PrefUtil.pref_token))
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
                        temperature = document.getDouble("temperature")?.toFloat()
                            ?: 0f,
                        current = document.getDouble("current")?.toFloat() ?: 0f,
                        power = document.getDouble("power")?.toFloat() ?: 0f,
                        persentagePower = document.getDouble(" persentagePower = ")
                            ?.toFloat() ?: 0f,
                        latitude = document.getDouble("latitude") ?: -5.136320,
                        longitude = document.getDouble("longitude") ?: 119.487793,
                    )

                    val carLocation = LatLng(car.latitude, car.longitude)
                    val carMarkerOptions = MarkerOptions()
                        .position(carLocation)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.img_marker_car))
                        .title(car.brand)
                    googleMap.addMarker(carMarkerOptions)
                }
            }
    }

    private fun focusToCarLocation() {
        if (::car.isInitialized) {
            val carLocation = LatLng(car.latitude, car.longitude)
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(carLocation, 15f))
        } else {
            Log.e("StationFragment", "Lokasi kendaraan tidak ditemukan.")
        }
    }

    private fun focusToUserLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.myLocation?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            }
        } else {
            // Toast here...
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        googleMap.setOnMarkerClickListener { marker -> onMarkerClick(marker) }
        googleMap.setOnMapClickListener { onMapClick() }
        getMyLocation()
        fetchPartners()

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
                    val powerRadius = car.power / bmwPower * 850

                    val primary400 = ContextCompat.getColor(requireContext(), R.color.primary400)
                    val circleOptions = CircleOptions()
                        .center(LatLng(car.latitude, car.longitude))
                        .radius(powerRadius)
                        .strokeColor(primary400)
                        .fillColor(0x2271B465)
                        .strokeWidth(2f)

                    googleMap.addCircle(circleOptions)
                } else {
                    Toast.makeText(requireContext(), "Tidak ada data kendaraan", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }

    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                getMyLocation()
            }
        }

    private fun getMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun fetchPartners() {
        val db = Firebase.firestore
        db.collection("partner")
            .get()
            .addOnSuccessListener { result ->
                val partners = mutableListOf<Partner>()
                for (document in result) {
                    val id = document.id
                    val partner = document.toObject(Partner::class.java).copy(id = id)
                    partners.add(partner)
                }
                db.collection("charger")
                    .get()
                    .addOnSuccessListener { chargerResult ->
                        val chargers = mutableListOf<Charger>()
                        for (document in chargerResult) {
                            val charger = document.toObject(Charger::class.java)
                            chargers.add(charger)
                        }
                        chargersList = chargers
                        addManyMarker(partners, chargers)
                    }
            }
            .addOnFailureListener { exception -> }
    }

    private fun addManyMarker(partners: List<Partner>, chargers: List<Charger>) {
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

        val bounds: LatLngBounds = boundsBuilder.build()
        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds,
                resources.displayMetrics.widthPixels,
                resources.displayMetrics.heightPixels,
                300
            )
        )

        googleMap.setOnMarkerClickListener { marker ->
            val partner = marker.tag as? Partner
            partner?.let {
                val calendar = Calendar.getInstance()
                val currentTime = calendar.time

                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                val timeOpen = timeFormat.parse(partner.timeOpen)
                val timeClose = timeFormat.parse(partner.timeClose)

                val calendarOpen = Calendar.getInstance().apply {
                    time = timeOpen
                    set(Calendar.YEAR, calendar.get(Calendar.YEAR))
                    set(Calendar.MONTH, calendar.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH))
                }

                val calendarClose = Calendar.getInstance().apply {
                    time = timeClose
                    set(Calendar.YEAR, calendar.get(Calendar.YEAR))
                    set(Calendar.MONTH, calendar.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH))
                }

                val isOpenStatus =
                    if (currentTime.after(calendarOpen.time) && currentTime.before(calendarClose.time)) {
                        isOpen.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.primary400
                            )
                        )
                        val drawable =
                            ContextCompat.getDrawable(requireContext(), R.drawable.ic_small_time)
                        DrawableCompat.setTint(
                            drawable!!,
                            ContextCompat.getColor(requireContext(), R.color.primary400)
                        )
                        isOpen.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                        "Buka"
                    } else {
                        isOpen.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.system_red
                            )
                        )
                        val drawable =
                            ContextCompat.getDrawable(requireContext(), R.drawable.ic_small_time)
                        DrawableCompat.setTint(
                            drawable!!,
                            ContextCompat.getColor(requireContext(), R.color.system_red)
                        )
                        isOpen.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                        "Tutup"
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
                        } else {
                            car = Car()
                        }

                        val userLoc = if (car.token.isEmpty()) {
                            googleMap.myLocation?.let { userLocation ->
                                android.location.Location("userLoc").apply {
                                    latitude = userLocation.latitude
                                    longitude = userLocation.longitude
                                }
                            }
                        } else {
                            android.location.Location("carLoc").apply {
                                latitude = car.latitude
                                longitude = car.longitude
                            }
                        }

                        userLoc?.let { userLocation ->
                            val stationLocation = LatLng(it.latitude, it.longitude)
                            val stationLoc = android.location.Location("stationLoc").apply {
                                latitude = stationLocation.latitude
                                longitude = stationLocation.longitude
                            }

                            btnDetail.setOnClickListener {
                                val intent = Intent(context, StationDetailActivity::class.java)
                                intent.putExtra("id", partner.id)
                                intent.putExtra("distance", calculatedDistance)
                                startActivity(intent)
                            }

                            val userLatLng = LatLng(userLocation.latitude, userLocation.longitude)
                            val stationLatLng = LatLng(stationLoc.latitude, stationLoc.longitude)
                            val URL = getDirectionURL(userLatLng, stationLatLng)

                            Log.d("GoogleMap", "URL : $URL")
                            GetDirection(URL).execute()
                            currentPolyline?.remove()

                            val boundsBuilder = LatLngBounds.Builder()
                            boundsBuilder.include(userLatLng)
                            boundsBuilder.include(stationLatLng)
                            val bounds = boundsBuilder.build()
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200), 2000, null)

                        } ?: run {
                            Toast.makeText(context, "Lokasi Anda tidak tersedia", Toast.LENGTH_SHORT).show()
                        }
                    }


                val matchingChargers = chargersList.filter { charger ->
                    charger.stationName == partner.stationName
                }

                val chargerTypeNum = matchingChargers.size
                val totalChargerTotal = matchingChargers.sumOf { charger ->
                    charger.chargerTotal
                }

                stationName.text = it.stationName
                address.text = it.address
                isOpen.text = isOpenStatus
                chargerType.text = "$chargerTypeNum Jenis Sakelar"
                chargerTotal.text = "$totalChargerTotal/$totalChargerTotal"

                containerPopup.visibility = View.VISIBLE
                containerPopup.startAnimation(fromBottom)
            }
            true
        }
    }

    private fun onMarkerClick(marker: Marker): Boolean {
        lastClickedMarker?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.img_marker_station))
        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.img_click_marker_station))
        lastClickedMarker = marker
        return true
    }

    private fun onMapClick() {
        lastClickedMarker?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.img_marker_station))
        lastClickedMarker = null

        containerPopup.visibility = View.GONE
        containerPopup.startAnimation(toBottom)
    }

    fun getDirectionURL(origin:LatLng,dest:LatLng) : String{
        val apiKey = "AIzaSyBSfk9sNYbUFgQQcFkn25L83tJCijdaxSc"
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${dest.latitude},${dest.longitude}&sensor=false&mode=driving&key=$apiKey"
    }

    private inner class GetDirection(val url: String) : AsyncTask<Void, Void, Pair<List<List<LatLng>>, Pair<String, String>>>() {
        override fun doInBackground(vararg params: Void?): Pair<List<List<LatLng>>, Pair<String, String>> {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val data = response.body!!.string()
            Log.d("GoogleMap" , " data : $data")
            val result =  ArrayList<List<LatLng>>()
            var distanceText = ""
            var durationText = ""
            try{
                val respObj = Gson().fromJson(data, GoogleMapDTO::class.java)
                val path = ArrayList<LatLng>()
                val steps = respObj.routes[0].legs[0].steps

                for (i in steps.indices) {
                    path.addAll(decodePolyline(steps[i].polyline.points))
                }
                result.add(path)

                val leg = respObj.routes[0].legs[0]
                distanceText = leg.distance.text
                durationText = leg.duration.text
            }catch (e:Exception){
                e.printStackTrace()
            }
            return Pair(result, Pair(distanceText, durationText))
        }

        override fun onPostExecute(result: Pair<List<List<LatLng>>, Pair<String, String>>) {
            val lineoption = PolylineOptions()
            Log.d("GoogleMap", "Polyline result size: ${result.first.size}")

            val primary400 = ContextCompat.getColor(requireContext(), R.color.primary400)

            for (i in result.first.indices) {
                lineoption.addAll(result.first[i])
                lineoption.width(10f)
                lineoption.color(primary400)
                lineoption.geodesic(true)
            }
            currentPolyline = googleMap.addPolyline(lineoption)

            val distanceText = result.second.first
            val durationText = result.second.second

            distance.text = distanceText
            duration.text = durationText

            val distance = distanceText.replace(" km", "").trim()
            calculatedDistance = distance

            Log.d("GoogleMap", "Jarak: $distanceText, Waktu Tempuh: $durationText")
        }
    }

    public fun decodePolyline(encoded: String): List<LatLng> {

        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng((lat.toDouble() / 1E5),(lng.toDouble() / 1E5))
            poly.add(latLng)
        }

        return poly
    }
}
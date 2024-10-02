package com.sciencehackfest.trive_sci.activity

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.view.WindowInsetsController
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.sciencehackfest.trive_sci.R
import com.sciencehackfest.trive_sci.adapter.ChargerCapacityAdapter
import com.sciencehackfest.trive_sci.adapter.ChargerCapacityListener
import com.sciencehackfest.trive_sci.adapter.ChargerTypeAdapter
import com.sciencehackfest.trive_sci.adapter.ChargerTypeListener
import com.sciencehackfest.trive_sci.databinding.ActivityAddPartnerBinding
import com.sciencehackfest.trive_sci.databinding.LayoutChargerBinding
import com.sciencehackfest.trive_sci.fragment.MapBottomSheetFragment
import com.sciencehackfest.trive_sci.model.Charger
import com.sciencehackfest.trive_sci.model.Partner
import com.sciencehackfest.trive_sci.preference.PreferenceManager
import com.sciencehackfest.trive_sci.util.PrefUtil
import com.sciencehackfest.trive_sci.view.CurrencyTextWatcher
import com.squareup.picasso.Picasso
import java.util.Calendar
import java.util.UUID

class AddPartnerActivity : AppCompatActivity(),
    OnMapReadyCallback,
    MapBottomSheetFragment.OnLocationSelectedListener,
    ChargerTypeListener,
    ChargerCapacityListener {

    lateinit var binding: ActivityAddPartnerBinding
    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    lateinit var mMap: GoogleMap
    private val pref by lazy { PreferenceManager(this) }
    private val chargerBindings = mutableListOf<LayoutChargerBinding>()
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var bottomSheetDialog: BottomSheetDialog? = null
    private var selectedChargerType: String? = null
    private var selectedChargerCapacity: String? = null
    var stationImgUri: Uri? = null
    val firebaseStorage: FirebaseStorage = FirebaseStorage.getInstance()
    val storageReference: StorageReference = firebaseStorage.reference
    val db by lazy { Firebase.firestore }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAddPartnerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.resultStationImg.visibility = View.GONE
        binding.addStationImg.visibility = View.VISIBLE

        setNavigationBarColor()
        registerActivityForResult()
        useUserDetail()

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY > 0) {
                setStatusBarWhite()
            } else {
                setStatusBarTransparent()
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

        val additionalPaddingInPx2 = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics
        ).toInt()

        val totalPaddingTop = statusBarHeight + additionalPaddingInPx
        val totalPaddingTop2 = statusBarHeight + additionalPaddingInPx2

        binding.navbar.setPadding(
            binding.navbar.paddingLeft,
            totalPaddingTop,
            binding.navbar.paddingRight,
            binding.navbar.paddingBottom
        )

        binding.partnerHeading.setPadding(
            binding.partnerHeading.paddingLeft,
            totalPaddingTop2,
            binding.partnerHeading.paddingRight,
            binding.partnerHeading.paddingBottom
        )

        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY > 24) {
                binding.navbar.setBackgroundColor(resources.getColor(R.color.primary200))
                binding.btnBack.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary200)
                binding.titleNavbar.visibility = View.VISIBLE
            } else {
                binding.navbar.setBackgroundColor(resources.getColor(android.R.color.transparent))
                binding.btnBack.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary200)
                binding.titleNavbar.visibility = View.INVISIBLE
            }
        }

        binding.addStationImg.setOnClickListener {
            chooseStationImg()
        }

        binding.resultStationImg.setOnClickListener {
            chooseStationImg()
        }

        binding.chooseLocation.setOnClickListener {
            val bottomSheetFragment = MapBottomSheetFragment()
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }

        binding.etOpen.setOnClickListener {
            showTimePicker(binding.etOpen)
        }

        binding.etClose.setOnClickListener {
            showTimePicker(binding.etClose)
        }

        binding.etPrice.addTextChangedListener(CurrencyTextWatcher(binding.etPrice))

        binding.addCharger.setOnClickListener {
            addChargerInput()
        }

        binding.btnApplyPartner.setOnClickListener {
            if (isRequired()) {
                progress(true)
                val stationName = binding.etStationName.text.toString()
                checkStationNameDuplicate(stationName) { isDuplicate ->
                    if (isDuplicate) {
                        binding.alertStationName.visibility = View.VISIBLE
                        binding.scrollView.smoothScrollTo(0, 0)
                    } else {
                        binding.alertStationName.visibility = View.GONE
                        binding.scrollView.post {
                            binding.scrollView.smoothScrollTo(0, binding.scrollView.getChildAt(0).height)
                        }
                        uploadStationImg()
                        addCharger()
                    }
                    progress(false)
                }
            }
        }

        binding.etStationName.addTextChangedListener(textWatcher)
        binding.etAddress.addTextChangedListener(textWatcher)
        binding.etOpen.addTextChangedListener(textWatcher)
        binding.etClose.addTextChangedListener(textWatcher)
        binding.etPrice.addTextChangedListener(textWatcher)
        binding.etOwner.addTextChangedListener(textWatcher)
        binding.etMail.addTextChangedListener(textWatcher)
        updateApplyPartnerButtonState()
    }

    fun chooseStationImg() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1)
        } else {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            activityResultLauncher.launch(intent)
        }
    }

    fun uploadStationImg() {
        val stationImgName = UUID.randomUUID().toString()
        val stationImgReference = storageReference.child("stationImages").child(stationImgName)

        stationImgUri?.let { uri ->
            stationImgReference.putFile(uri).addOnSuccessListener {
                val myUploadStationImgReference = storageReference.child("stationImages").child(stationImgName)
                myUploadStationImgReference.downloadUrl.addOnSuccessListener { url ->
                    val stationImgURL = url.toString()
                    addPartner(stationImgURL)
                }
            }.addOnFailureListener {
                Toast.makeText(applicationContext, it.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun registerActivityForResult() {
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback { result ->
                val resultCode = result.resultCode
                val imageData = result.data
                if (resultCode == RESULT_OK && imageData != null) {
                    stationImgUri = imageData.data
                    stationImgUri?.let {
                        Picasso.get().load(it).into(binding.resultStationImg)
                        binding.resultStationImg.visibility = View.VISIBLE
                        binding.addStationImg.visibility = View.GONE
                    }
                }
            })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            activityResultLauncher.launch(intent)
        }
    }

    private fun showTimePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            R.style.CustomTimePickerDialog,
            { _, selectedHour, selectedMinute ->
                // HH:mm
                val time = String.format("%02d:%02d", selectedHour, selectedMinute)
                editText.setText(time)
            },
            hour,
            minute,
            true
        )
        timePickerDialog.show()
    }

    private fun addPartner(stationImg: String) {
        val partner = Partner(
            id = null,
            stationName = binding.etStationName.text.toString(),
            stationImg = stationImg,
            description = binding.etDescription.text.toString(),
            latitude = latitude.toString().toDouble(),
            longitude = longitude.toString().toDouble(),
            address = binding.etAddress.text.toString(),
            timeOpen = binding.etOpen.text.toString(),
            timeClose = binding.etClose.text.toString(),
            haveBank = binding.isHaveBank.isChecked,
            haveCoffeeShop = binding.isHaveCoffeeShop.isChecked,
            haveMosque = binding.isHaveMosque.isChecked,
            haveMinimart = binding.isHaveMinimart.isChecked,
            haveRestaurant = binding.isHaveRestaurant.isChecked,
            haveToilet = binding.isHaveToilet.isChecked,
            haveWifi = binding.isHaveWifi.isChecked,
            price = binding.etPrice.text.toString().replace(".", "").toLong(),
            owner = binding.etOwner.text.toString(),
            mail = binding.etMail.text.toString(),
            phone = binding.etPhone.text.toString(),
            created = Timestamp.now()
        )
        db.collection("partner")
            .add(partner)
            .addOnSuccessListener {
                Toast.makeText(applicationContext, "SPKLU berhasil diajukan sebagai mitra", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
    }

    private fun checkStationNameDuplicate(stationName: String, callback: (Boolean) -> Unit) {
        db.collection("partner")
            .whereEqualTo("stationName", stationName)
            .get()
            .addOnSuccessListener { result ->
                callback(result.documents.isNotEmpty())
            }
            .addOnFailureListener { exception ->
                Toast.makeText(applicationContext, "Gagal mengecek nama stasiun: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }

    private fun addCharger() {
        val chargers = chargerBindings.mapIndexedNotNull { index, bindingCharger ->
            val type = bindingCharger.etChargerType.text.toString()
            val capacity = bindingCharger.etChargerCapacity.text.toString()
            val total = bindingCharger.etChargerTotal.text.toString().toIntOrNull()

            if (type.isNotEmpty() && capacity.isNotEmpty() && total != null) {
                Charger(
                    id = index + 1,
                    stationName = binding.etStationName.text.toString(),
                    chargerType = type,
                    chargerCapacity = capacity,
                    chargerTotal = total,
                )
            } else {
                null
            }
        }

        val batch = db.batch()
        chargers.forEach { charger ->
            val docRef = db.collection("charger").document()
            batch.set(docRef, charger)
        }

        batch.commit()
            .addOnSuccessListener {
            }
            .addOnFailureListener {
            }
    }

    private fun bottomSheetChargerType() {
        val bottomSheet: View = layoutInflater.inflate(R.layout.bottom_sheet_dropdown, null)
        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        bottomSheetDialog?.setContentView(bottomSheet)

        val title: TextView = bottomSheet.findViewById(R.id.title_bottom_sheet)
        val recyclerView: RecyclerView = bottomSheet.findViewById(R.id.rv_dropdown)
        val chargerTypes = chargerTypes()
        val adapter = ChargerTypeAdapter(chargerTypes, this, selectedChargerType)

        title.text = "Jenis Sakelar"
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        bottomSheetDialog?.show()
    }

    override fun onChargerTypeSelected(chargerType: String) {
        selectedChargerType = chargerType
        val selectedBinding = chargerBindings.lastOrNull()
        selectedBinding?.etChargerType?.setText(chargerType)
        bottomSheetDialog?.dismiss()
    }

    private fun chargerTypes(): List<String> {
        return listOf(
            "CHAdeMO",
            "CCS2",
            "Type 2",
        )
    }

    private fun bottomSheetChargerCapacity() {
        val bottomSheet: View = layoutInflater.inflate(R.layout.bottom_sheet_dropdown, null)
        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        bottomSheetDialog?.setContentView(bottomSheet)

        val title: TextView = bottomSheet.findViewById(R.id.title_bottom_sheet)
        val recyclerView: RecyclerView = bottomSheet.findViewById(R.id.rv_dropdown)
        val chargerCapacities = chargerCapacities()
        val adapter = ChargerCapacityAdapter(chargerCapacities, this, selectedChargerCapacity)


        title.text = "Kapasitas Sakelar"
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        bottomSheetDialog?.show()
    }

    override fun onChargerCapacitySelected(chargerCapacity: String) {
        selectedChargerCapacity = chargerCapacity
        val selectedBinding = chargerBindings.lastOrNull()
        selectedBinding?.etChargerCapacity?.setText(chargerCapacity)
        bottomSheetDialog?.dismiss()
    }

    private fun chargerCapacities(): List<String> {
        return listOf(
            "Standar • 7 kW",
            "Medium • 8 kW - 22 kW",
            "Fast • 23 kW - 25 kW",
            "Fast • 26 kW - 50 kW",
            "Fast • 51 kW - 100 kW",
            "Fast • > 101 kW"
        )
    }

    private fun addChargerInput() {
        val inflater = layoutInflater
        val bindingCharger = LayoutChargerBinding.inflate(inflater, binding.chargerContainer, false)
        binding.chargerContainer.addView(bindingCharger.root)
        chargerBindings.add(bindingCharger)

        bindingCharger.etChargerType.setOnClickListener {
            bottomSheetChargerType()
        }

        bindingCharger.etChargerType.addTextChangedListener { text ->
            if (text.toString().isNotEmpty()) {
                bindingCharger.etChargerCapacity.isEnabled = true
                bindingCharger.etChargerCapacity.backgroundTintList = null
                bindingCharger.etChargerCapacity.setHintTextColor(ContextCompat.getColor(this, R.color.textSecondary))
                bindingCharger.etChargerCapacity.compoundDrawableTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.textSecondary))

                bindingCharger.etChargerTotal.isEnabled = true
                bindingCharger.etChargerTotal.backgroundTintList = null
                bindingCharger.etChargerTotal.setHintTextColor(ContextCompat.getColor(this, R.color.textSecondary))
                bindingCharger.etChargerTotal.compoundDrawableTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.textSecondary))
            }
        }

        bindingCharger.etChargerCapacity.setOnClickListener {
            bottomSheetChargerCapacity()
        }

        bindingCharger.etChargerType.addTextChangedListener(textWatcher)
        bindingCharger.etChargerCapacity.addTextChangedListener(textWatcher)
        bindingCharger.etChargerTotal.addTextChangedListener(textWatcher)
        updateApplyPartnerButtonState()
    }

    private fun useUserDetail() {
        var isDetailUsed = false

        binding.userDetail.setOnClickListener {
            if (!isDetailUsed) {
                binding.etOwner.setText(pref.getString(PrefUtil.pref_fullname))
                binding.etMail.setText(pref.getString(PrefUtil.pref_mail))
                binding.etPhone.setText(pref.getString(PrefUtil.pref_phone))

                binding.userDetail.text = "Hapus detail saya"
                binding.userDetail.setTextColor(ContextCompat.getColor(this, R.color.system_red))
                binding.userDetail.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.system_red_light)
                )

                isDetailUsed = true
            } else {
                binding.etOwner.setText("")
                binding.etMail.setText("")
                binding.etPhone.setText("")

                binding.userDetail.text = "Pakai detail saya"
                binding.userDetail.setTextColor(ContextCompat.getColor(this, R.color.primary500))
                binding.userDetail.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.system_green_light)
                )

                isDetailUsed = false
            }
        }
    }

    private fun progress(progress: Boolean) {
        when (progress) {
            true -> {
                binding.btnApplyPartner.text = "Memuat..."
                val disabledColor = resources.getColor(R.color.primary200, null)
                binding.btnApplyPartner.backgroundTintList = ColorStateList.valueOf(disabledColor)
                binding.btnApplyPartner.isEnabled = false
            }
            false -> {
                binding.btnApplyPartner.text = "Ajukan Mitra"
                val enabledColor = resources.getColor(R.color.primary400, null)
                binding.btnApplyPartner.backgroundTintList = ColorStateList.valueOf(enabledColor)
                binding.btnApplyPartner.isEnabled = true
            }
        }
    }

    private fun isRequired(): Boolean {
        val stationName = binding.etStationName.text.toString().isNotEmpty()
        val address = binding.etAddress.text.toString().isNotEmpty()
        val open = binding.etOpen.text.toString().isNotEmpty()
        val close = binding.etClose.text.toString().isNotEmpty()
        val price = binding.etPrice.text.toString().isNotEmpty()
        val owner = binding.etOwner.text.toString().isNotEmpty()
        val mail = binding.etMail.text.toString().isNotEmpty()
        val stationImg = stationImgUri != null
        val latitude = latitude != null
        val longitude = longitude != null

        val allCharger = chargerBindings.all { bindingCharger ->
            bindingCharger.etChargerType.text.toString().isNotEmpty() &&
                    bindingCharger.etChargerCapacity.text.toString().isNotEmpty() &&
                    bindingCharger.etChargerTotal.text.toString().isNotEmpty()
        }

        return stationName && address && open && close && price && owner && mail && stationImg && latitude && longitude && allCharger
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            updateApplyPartnerButtonState()
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    private fun updateApplyPartnerButtonState() {
        val isEnabled = isRequired()
        binding.btnApplyPartner.isEnabled = isEnabled
        val backgroundColor = if (isEnabled) {
            resources.getColor(R.color.primary400, null)
        } else {
            resources.getColor(R.color.primary200, null)
        }
        binding.btnApplyPartner.backgroundTintList = ColorStateList.valueOf(backgroundColor)
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
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary200)
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

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (latitude != null && longitude != null) {

            val selectedLocation = LatLng(latitude!!, longitude!!)
            mMap.addMarker(
                MarkerOptions()
                    .position(selectedLocation)
            )
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 100f))
            binding.chooseLocation.visibility = View.GONE
        } else {
            val indonesiaBounds = LatLngBounds(
                LatLng(-11.0, 95.0),
                LatLng(6.0, 141.0)
            )

            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(indonesiaBounds, 100))
        }
    }

    override fun onLocationSelected(latitude: Double, longitude: Double) {
        this.latitude = latitude
        this.longitude = longitude

        if (mMap != null) {
            val selectedLocation = LatLng(latitude, longitude)
            mMap.clear()  // Hapus marker lama jika ada
            mMap.addMarker(
                MarkerOptions()
                    .position(selectedLocation)
            )
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 100f))
            binding.chooseLocation.visibility = View.GONE
        }
    }
}
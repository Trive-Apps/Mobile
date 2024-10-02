package com.sciencehackfest.trive_sci.fragment

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.sciencehackfest.trive_sci.R
import com.sciencehackfest.trive_sci.activity.AddPartnerActivity
import com.sciencehackfest.trive_sci.activity.BatteryActivity
import com.sciencehackfest.trive_sci.activity.PaymentActivity
import com.sciencehackfest.trive_sci.activity.ReservationDataActivity
import com.sciencehackfest.trive_sci.activity.TopupActivity
import com.sciencehackfest.trive_sci.databinding.FragmentHomeBinding
import com.sciencehackfest.trive_sci.model.Car
import com.sciencehackfest.trive_sci.model.User
import com.sciencehackfest.trive_sci.preference.PreferenceManager
import com.sciencehackfest.trive_sci.util.PrefUtil
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem
import java.text.NumberFormat
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val db by lazy { Firebase.firestore }
    private val pref by lazy { PreferenceManager(requireContext()) }
    private lateinit var user: User
    private lateinit var car: Car

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupScanButton()
        setupImageCarousel()
        getBalance()

        activity?.window?.statusBarColor = Color.parseColor("#FFDEDEE4")
        activity?.window?.let { window ->
            ViewCompat.getWindowInsetsController(window.decorView)?.let { controller ->
                controller.isAppearanceLightStatusBars = true
            }
        }

        binding.btnReservation.setOnClickListener {
            val intent = Intent(context, ReservationDataActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        val additionalPaddingInPx2 = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 82f, resources.displayMetrics
        ).toInt()

        val totalPaddingTop2 = statusBarHeight + additionalPaddingInPx2

        binding.car.setPadding(
            binding.car.paddingLeft,
            totalPaddingTop2,
            binding.car.paddingRight,
            binding.car.paddingBottom
        )

    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                isGranted: Boolean ->
            if (isGranted) {
                showCamera()
            } else {}
        }

    private val scanLauncher =
        registerForActivityResult(ScanContract()) {
                result: ScanIntentResult ->
            run {
                if (result.contents == null) {
                    Toast.makeText(context, "Scan barcode dibatalkan", Toast.LENGTH_SHORT).show()
                } else {
                    setResult(result.contents)
                }
            }
        }

    private fun setResult(result: String) {
        val intent = Intent(requireContext(), PaymentActivity::class.java).apply {
            putExtra("barcode_result", result)
        }
        startActivity(intent)
    }

    private fun showCamera() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.CODE_128)
        options.setCameraId(0)
        options.setPrompt("Scan barcode pengisian daya kendaraan listrik")
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        options.setOrientationLocked(true)

        scanLauncher.launch(options)
    }

    private fun getBalance() {
        db.collection("user")
            .whereEqualTo("mail", pref.getString(PrefUtil.pref_mail))
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]

                    user = User(
                        fullname = document.getString("fullname").orEmpty(),
                        mail = document.getString("mail").orEmpty(),
                        phone = document.getString("phone").orEmpty(),
                        money = document.getLong("money") ?: 0L,
                        point = document.getLong("price") ?: 0L,
                        token = document.getString("token").orEmpty(),
                        created = document.getTimestamp("created") ?: Timestamp.now()
                    )

                    binding.btnBattery.setOnClickListener {
                        val intent = Intent(context, BatteryActivity::class.java)
                        intent.putExtra("token", user.token)
                        startActivity(intent)
                    }

                    val formattedMoney = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
                        maximumFractionDigits = 0
                    }.format(user.money)

                    val formattedPoint = "${user.point} Point"

                    binding.money.text = formattedMoney
                    binding.point.text = formattedPoint

                    binding.progressBar.visibility = View.VISIBLE
                    db.collection("car")
                        .whereEqualTo("token", user.token)
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .addSnapshotListener { querySnapshot, error ->
                            binding.progressBar.visibility = View.GONE

                            if (error != null) {
                                return@addSnapshotListener
                            }

                            if (querySnapshot != null && !querySnapshot.isEmpty) {
                                val document = querySnapshot.documents[0]

                                car = Car(
                                    token = document.getString("token").orEmpty(),
                                    type = document.getString("type").orEmpty(),
                                    brand = document.getString("brand").orEmpty(),
                                    chargerType = document.getString("chargerType").orEmpty(),
                                    isActive = document.getBoolean("isActive") ?: false,
                                    voltage = document.getDouble("voltage")?.toFloat() ?: 0f,
                                    temperature = document.getDouble("temperature")?.toFloat() ?: 0f,
                                    current = document.getDouble("current")?.toFloat() ?: 0f,
                                    power = document.getDouble("power")?.toFloat() ?: 0f,
                                    persentagePower = document.getDouble("persentagePower")?.toFloat() ?: 0f,
                                    latitude = document.getDouble("latitude") ?: 0.0,
                                    longitude = document.getDouble("longitude") ?: 0.0,
                                    timestamp = document.getLong("timestamp") ?: 0L,
                                )

                                binding.battery.visibility = View.VISIBLE
                                binding.batteryText.visibility = View.VISIBLE
                                binding.percent.visibility = View.VISIBLE
                                binding.icBattery.visibility = View.VISIBLE

                                if (car.isActive) {
                                    binding.isActive.text = "Active"
                                    binding.isActive.visibility = View.VISIBLE
                                    binding.isActive.backgroundTintList = ColorStateList.valueOf(
                                        ContextCompat.getColor(requireContext(), R.color.primary400)
                                    )
                                } else {
                                    binding.isActive.text = "Inactive"
                                    binding.isActive.visibility = View.VISIBLE
                                    binding.isActive.backgroundTintList = ColorStateList.valueOf(
                                        ContextCompat.getColor(requireContext(), R.color.primary300)
                                    )
                                }

                                binding.carType.text = car.type
                                val battery = car.persentagePower.toInt()
                                binding.battery.text = battery.toString()

                                when {
                                    battery in 91..100 -> {
                                        binding.icBattery.setImageResource(R.drawable.ic_battery_full)
                                        binding.icBattery.setColorFilter(
                                            ContextCompat.getColor(requireContext(), R.color.primary400)
                                        )
                                    }
                                    battery in 61..90 -> {
                                        binding.icBattery.setImageResource(R.drawable.ic_battery_high)
                                        binding.icBattery.setColorFilter(
                                            ContextCompat.getColor(requireContext(), R.color.primary400)
                                        )
                                    }
                                    battery in 31..60 -> {
                                        binding.icBattery.setImageResource(R.drawable.ic_battery_medium)
                                        binding.icBattery.setColorFilter(
                                            ContextCompat.getColor(requireContext(), R.color.secondary400)
                                        )
                                    }
                                    battery in 1..30 -> {
                                        binding.icBattery.setImageResource(R.drawable.ic_battery_low)
                                        binding.icBattery.setColorFilter(
                                            ContextCompat.getColor(requireContext(), R.color.system_red)
                                        )
                                    }
                                    battery == 0 -> {
                                        binding.icBattery.setImageResource(R.drawable.ic_battery_empty)
                                        binding.icBattery.setColorFilter(
                                            ContextCompat.getColor(requireContext(), R.color.system_red)
                                        )
                                    }
                                }

                                when (car.brand) {
                                    "BMW" -> {
                                        binding.imgBrand.setImageResource(R.drawable.img_bmw)

                                        when (car.type) {
                                            "BMW I3" -> binding.imgCar.setImageResource(R.drawable.img_bmw_i3_electric)
                                        }
                                    }
                                }
                            }
                        }
                }
            }
    }

    private fun setupScanButton() {
        binding.btnBarcode.setOnClickListener {
            checkPermissionCamera(requireContext())
        }

        binding.btnAdd.setOnClickListener {
            startActivity(Intent(activity, TopupActivity::class.java))
        }

        binding.btnPartner.setOnClickListener {
            startActivity(Intent(activity, AddPartnerActivity::class.java))
        }
    }

    private fun checkPermissionCamera(context: Context) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showCamera()
        } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)) {
            Toast.makeText(context, "Memerlukan izin kamera", Toast.LENGTH_SHORT).show()
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun setupImageCarousel() {
        val carouselItems = listOf(
            R.drawable.img_carousel_trive1,
            R.drawable.img_carousel_trive2,
            R.drawable.img_carousel_trive3,
        ).map { CarouselItem(imageDrawable = it) }

        binding.carousel.setData(carouselItems)
    }
}
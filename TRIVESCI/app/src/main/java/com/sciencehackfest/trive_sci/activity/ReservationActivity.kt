package com.sciencehackfest.trive_sci.activity

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.firestore
import com.sciencehackfest.trive_sci.R
import com.sciencehackfest.trive_sci.adapter.ChargerReservationAdapter
import com.sciencehackfest.trive_sci.databinding.ActivityReservationBinding
import com.sciencehackfest.trive_sci.databinding.BottomSheetChargerBinding
import com.sciencehackfest.trive_sci.model.Charger
import com.sciencehackfest.trive_sci.model.Partner
import com.sciencehackfest.trive_sci.model.Reservation
import com.sciencehackfest.trive_sci.model.User
import com.sciencehackfest.trive_sci.preference.PreferenceManager
import com.sciencehackfest.trive_sci.util.PrefUtil
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.lang.Exception
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

class ReservationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReservationBinding
    private lateinit var partner: Partner
    private lateinit var user: User
    private lateinit var chargerAdapter: ChargerReservationAdapter
    private val partnerId by lazy { intent.getStringExtra("id") }
    private val pref by lazy { PreferenceManager(this) }
    private val db by lazy { Firebase.firestore }
    private var bottomSheetDialog: BottomSheetDialog? = null
    private var chargers: ArrayList<Charger> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityReservationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigationBarColor()

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

        binding.charger.setOnClickListener {
            bottomSheetCharger()
        }
    }

    override fun onStart() {
        super.onStart()
        detailPartner()
    }

    private fun bottomSheetCharger() {
        val bottomSheetBinding = BottomSheetChargerBinding.inflate(layoutInflater)
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        bottomSheetDialog.setContentView(bottomSheetBinding.root)

        chargerAdapter = ChargerReservationAdapter(
            arrayListOf(),
            object : ChargerReservationAdapter.AdapterListener {
                override fun onClick(charger: Charger) {
                    if ( charger.chargerType == "CHAdeMO") {
                        Picasso.get().load(R.drawable.img_charger_chademo).into(binding.chargingImg, object : Callback {
                            override fun onSuccess() {}
                            override fun onError(e: Exception?) {}
                        })
                    } else if ( charger.chargerType == "CCS2") {
                        Picasso.get().load(R.drawable.img_charger_ccs_2).into(binding.chargingImg, object : Callback {
                            override fun onSuccess() {}
                            override fun onError(e: Exception?) {}
                        })
                    } else if ( charger.chargerType == "Type 2") {
                        Picasso.get().load(R.drawable.img_charger_type_2).into(binding.chargingImg, object : Callback {
                            override fun onSuccess() {}
                            override fun onError(e: Exception?) {}
                        })
                    }

                    binding.chargingType.text = charger.chargerType
                    binding.chargingCapacity.text = charger.chargerCapacity
                    bottomSheetDialog?.dismiss()
                }
            }
        )
        bottomSheetBinding.rvCharger.layoutManager = LinearLayoutManager(this)
        bottomSheetBinding.rvCharger.adapter = chargerAdapter

        chargerAdapter.setData(chargers)

        bottomSheetDialog?.show()
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
                    price = result["price"].toString().toLong()
                )

                binding.stationName.setText( partner.stationName )
                binding.address.setText( partner.address )
                val stationImg = partner.stationImg
                Picasso.get().load(stationImg).into(binding.stationImg, object : Callback {
                    override fun onSuccess() {}
                    override fun onError(e: Exception?) {}
                })


                val formattedMoney = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
                    maximumFractionDigits = 0
                }.format(partner.price)
                binding.price.text = formattedMoney

                binding.etPower.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {

                        binding.payment.visibility = View.VISIBLE

                        val powerReservation = s.toString().toDoubleOrNull() ?: 0.0
                        val totalPrice = partner.price * powerReservation
                        val totalPayment = totalPrice + 2000

                        val formattedTotalPrice = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
                            maximumFractionDigits = 0
                        }.format(totalPrice)

                        val formattedTotalPayment = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
                            maximumFractionDigits = 0
                        }.format(totalPayment)

                        binding.calculatePrice.text = formattedTotalPrice
                        binding.totalPrice.text = formattedTotalPayment
                        binding.totalPayment.text = formattedTotalPayment

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
                                        token = document.id,
                                        created = document.getTimestamp("created") ?: Timestamp.now()
                                    )

                                    val money = user.money
                                    updatePaymentButtonState(money, totalPayment)

                                    binding.btnPay.setOnClickListener {
                                        val updatedMoney = user.money - totalPayment

                                        fun generateRandomId(length: Int): String {
                                            val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
                                            return (1..length)
                                                .map { allowedChars.random() }
                                                .joinToString("")
                                        }

                                        val reservation = Reservation(
                                            id = generateRandomId(8),
                                            chargerType = binding.chargingType.text.toString(),
                                            chargerCapacity = binding.chargingCapacity.text.toString(),
                                            stationName = partner.stationName,
                                            stationImg = partner.stationImg,
                                            address = partner.address,
                                            price = totalPayment.toLong(),
                                            power = binding.etPower.text.toString().toDouble(),
                                            mail = pref.getString(PrefUtil.pref_mail).toString(),
                                            createdAt = Timestamp.now()
                                        )

                                        db.collection("user")
                                            .document(user.token)
                                            .update("money", updatedMoney)
                                            .addOnSuccessListener {
                                                db.collection("reservation")
                                                    .document(reservation.id)
                                                    .set(reservation)
                                                    .addOnSuccessListener {
                                                        Toast.makeText(this@ReservationActivity, "Pembayaran reservasi berhasil!", Toast.LENGTH_SHORT).show()
                                                        val intent = Intent(this@ReservationActivity, ReservationDataActivity::class.java)
                                                        startActivity(intent)
                                                        finish()
                                                    }
                                                    .addOnFailureListener { exception ->
                                                        Toast.makeText(this@ReservationActivity, "Gagal menyimpan reservasi: ${exception.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                            .addOnFailureListener { exception ->
                                                Toast.makeText(this@ReservationActivity, "Gagal mengupdate saldo: ${exception.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }

                                }
                            }
                    }
                })

                db.collection("charger")
                    .whereEqualTo("stationName", partner.stationName)
                    .get()
                    .addOnSuccessListener { result ->
                        chargers.clear()
                        result.forEach { doc ->
                            chargers.add(
                                Charger(
                                    stationName = doc.data["stationName"].toString(),
                                    chargerType = doc.data["chargerType"].toString(),
                                    chargerCapacity = doc.data["chargerCapacity"].toString(),
                                )
                            )
                        }

                        if (::chargerAdapter.isInitialized && bottomSheetDialog?.isShowing == true) {
                            chargerAdapter.setData(chargers)
                        }
                    }
            }
    }

    private fun updatePaymentButtonState(userMoney: Long, totalCost: Double) {
        val isEnough = userMoney >= totalCost
        binding.btnPay.isEnabled = isEnough
        val backgroundColor = if (isEnough) {
            resources.getColor(R.color.primary400, null)
        } else {
            resources.getColor(R.color.primary200, null)
        }

        if (userMoney < totalCost) {
//            binding.needMoney.visibility = View.VISIBLE
        }
        binding.btnPay.backgroundTintList = ColorStateList.valueOf(backgroundColor)
    }

    private fun setNavigationBarColor() {
        val color = ContextCompat.getColor(this, R.color.backgroundPrimary)
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
}
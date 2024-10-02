package com.sciencehackfest.trive_sci.activity

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.firestore
import com.sciencehackfest.trive_sci.R
import com.sciencehackfest.trive_sci.databinding.ActivityPaymentBinding
import com.sciencehackfest.trive_sci.model.Payment
import com.sciencehackfest.trive_sci.model.User
import com.sciencehackfest.trive_sci.preference.PreferenceManager
import com.sciencehackfest.trive_sci.util.PrefUtil
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private val paymentId by lazy { intent.getStringExtra("barcode_result") }
    private val db by lazy { Firebase.firestore }
    private val pref by lazy { PreferenceManager(this) }
    private lateinit var payment: Payment
    private lateinit var user: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigationBarColor()

        ViewCompat.setOnApplyWindowInsetsListener(binding.payment) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnTopUp.setOnClickListener {
            val TopupIntent = Intent(this, TopupActivity::class.java)
            startActivity(TopupIntent)
        }

    }

    override fun onStart() {
        super.onStart()
        detailPayment()
    }

    private fun detailPayment() {
        db.collection("payment")
            .whereEqualTo("barcode", paymentId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]

                    payment = Payment(
                        barcode = document.getString("barcode").orEmpty(),
                        carBrand = document.getString("carBrand").orEmpty(),
                        carType = document.getString("carType").orEmpty(),
                        name = document.getString("name").orEmpty(),
                        capacity = document.getString("capacity").orEmpty(),
                        type = document.getString("type").orEmpty(),
                        power = document.getLong("power") ?: 0L,
                        price = document.getLong("price") ?: 0L,
                        created = document.getTimestamp("created") ?: Timestamp.now()
                    )

                    val formattedMoney = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
                        maximumFractionDigits = 0
                    }.format(payment.price)

                    val adminFee = 2000L
                    val totalCost = payment.price + adminFee
                    val formattedTotalCost = NumberFormat.getInstance(Locale("in", "ID")).apply {
                        isGroupingUsed = true
                        maximumFractionDigits = 0
                    }.format(totalCost)

                    val dateFormat = SimpleDateFormat("HH:mm, d MMM yyyy", Locale("in", "ID"))
                    val formattedDate = dateFormat.format(payment.created.toDate())

                    binding.payments.setText(formattedMoney)
                    binding.name.setText(payment.name)
                    binding.id.setText(payment.barcode)
                    binding.totalPayment.setText(formattedTotalCost)
                    binding.totalPayments.setText("Rp${formattedTotalCost}")
                    binding.created.setText(formattedDate)


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
                                updatePaymentButtonState(money, totalCost)

                                binding.btnPayment.setOnClickListener {
                                    val updatedMoney = user.money - totalCost

                                    db.collection("user")
                                        .document(user.token)
                                        .update("money", updatedMoney)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Pembayaran berhasil!", Toast.LENGTH_SHORT).show()

                                            val intent = Intent(this, MainActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                        .addOnFailureListener { exception ->
                                            Toast.makeText(this, "Gagal mengupdate saldo: ${exception.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }

                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to get payment details: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updatePaymentButtonState(userMoney: Long, totalCost: Long) {
        val isEnough = userMoney >= totalCost
        binding.btnPayment.isEnabled = isEnough
        val backgroundColor = if (isEnough) {
            resources.getColor(R.color.primary400, null)
        } else {
            resources.getColor(R.color.primary200, null)
        }

        if (userMoney < totalCost) {
            binding.needMoney.visibility = View.VISIBLE
        }
        binding.btnPayment.backgroundTintList = ColorStateList.valueOf(backgroundColor)
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
package com.sciencehackfest.trive_sci.activity

import android.content.res.ColorStateList
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
import com.midtrans.sdk.corekit.callback.TransactionFinishedCallback
import com.midtrans.sdk.corekit.core.MidtransSDK
import com.midtrans.sdk.corekit.core.TransactionRequest
import com.midtrans.sdk.corekit.core.themes.CustomColorTheme
import com.midtrans.sdk.uikit.SdkUIFlowBuilder
import com.sciencehackfest.trive_sci.R
import com.sciencehackfest.trive_sci.databinding.ActivityTopupBinding
import com.sciencehackfest.trive_sci.model.User
import com.sciencehackfest.trive_sci.preference.PreferenceManager
import com.sciencehackfest.trive_sci.util.PrefUtil
import com.sciencehackfest.trive_sci.view.CurrencyTextWatcher
import java.text.NumberFormat
import java.util.Locale

class TopupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTopupBinding
    private val pref by lazy { PreferenceManager(this) }
    private val db by lazy { Firebase.firestore }
    private lateinit var user: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTopupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigationBarColor()
        getBalance()

        ViewCompat.setOnApplyWindowInsetsListener(binding.topup) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val user = pref.getString(PrefUtil.pref_fullname) ?: ""
        val mail = pref.getString(PrefUtil.pref_mail) ?: ""
        val phone = pref.getString(PrefUtil.pref_phone) ?: ""

        SdkUIFlowBuilder.init()
            .setClientKey("SB-Mid-client-dzXb0afV5S-CyFf0")
            .setContext(applicationContext)
            .setTransactionFinishedCallback(TransactionFinishedCallback {
                    result ->
            })
            .setMerchantBaseUrl("https://trive-web-436423.et.r.appspot.com/index.php/")
            .enableLog(true)
            .setColorTheme(CustomColorTheme(
                R.color.primary400.toString(),
                R.color.primary600.toString(),
                R.color.secondary400.toString())
            )
            .setLanguage("id")
            .buildSDK()

        binding.btnPayment.setOnClickListener {
            if (isRequired()) {
                val topUp = binding.etBalance.text.toString().toInt()
                val admin = 2000
                val totalTopUp = topUp + admin

                val transactionRequest = TransactionRequest("TRIVE" + System.currentTimeMillis().toShort() + "",
                    totalTopUp.toDouble()
                )

                val detail = com.midtrans.sdk.corekit.models.ItemDetails("id", totalTopUp.toDouble(), 1, "TrivePay")
                val itemDetails = ArrayList<com.midtrans.sdk.corekit.models.ItemDetails>()
                itemDetails.add(detail)
                UiKitDetails(transactionRequest, user, mail, phone)
                transactionRequest.itemDetails = itemDetails
                MidtransSDK.getInstance().transactionRequest = transactionRequest
                MidtransSDK.getInstance().startPaymentUiFlow(this)
            }
        }

        binding.etBalance.addTextChangedListener(CurrencyTextWatcher(binding.etBalance))
        binding.etBalance.addTextChangedListener(textWatcher)
        binding.etBalance.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateBalanceAndTotal()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        updateRegisterButtonState()
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

                    val formattedMoney = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
                        maximumFractionDigits = 0
                    }.format(user.money)


                    binding.myBalance.text = "Saldo TrivePay Anda saat ini: ${formattedMoney}"
                }
            }
    }

    private fun updateBalanceAndTotal() {
        val balanceText = binding.etBalance.text.toString()

        if (balanceText.isNotEmpty()) {
            val cleanedBalanceText = balanceText.replace("Rp", "").replace(".", "")
            val balance = cleanedBalanceText.toIntOrNull() ?: 0

            val adminFee = 2000
            val totalPayment = balance + adminFee

            binding.balance.text = formatCurrency(balance)
            binding.totalPayment.text = formatCurrency(totalPayment)
        } else {
            binding.balance.text = formatCurrency(0)
            binding.totalPayment.text = formatCurrency(0)
        }
    }


    private fun formatCurrency(amount: Int): String {
        return "Rp" + String.format("%,d", amount).replace(',', '.')
    }

    fun UiKitDetails(transactionRequest: TransactionRequest, user: String, mail: String, phone: String) {
        val userDetails = com.midtrans.sdk.corekit.models.CustomerDetails()
        userDetails.customerIdentifier = user
        userDetails.email = mail
        userDetails.phone = phone

        transactionRequest.customerDetails = userDetails
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            updateRegisterButtonState()
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    private fun isRequired(): Boolean {
        return binding.etBalance.text.toString().isNotEmpty()
    }

    private fun updateRegisterButtonState() {
        val isEnabled = isRequired()
        binding.btnPayment.isEnabled = isEnabled
        val backgroundColor = if (isEnabled) {
            resources.getColor(R.color.primary400, null)
        } else {
            resources.getColor(R.color.primary200, null)
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
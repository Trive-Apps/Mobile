package com.sciencehackfest.trive_sci.activity

import android.content.Intent
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
import com.google.firebase.firestore.FirebaseFirestore
import com.sciencehackfest.trive_sci.R
import com.sciencehackfest.trive_sci.databinding.ActivityInitialRegisterBinding

class InitialRegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInitialRegisterBinding
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityInitialRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.initialRegister) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setBarColor()
        setupListener()
    }

    private fun setupListener() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnRegister.setOnClickListener {
            if (isRequired()) {
                if (checkMailFormat()) {
                    checkMail()
                }
            }
        }

        binding.btnLogin.setOnClickListener {
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
        }

        binding.etMail.addTextChangedListener(textWatcher)
        updateRegisterButtonState()
    }

    private fun progress(progress: Boolean) {
        binding.alertRegister.visibility = View.GONE
        when (progress) {
            true -> {
                binding.btnRegister.visibility = View.INVISIBLE
            }
            false -> {
                binding.btnRegister.visibility = View.VISIBLE
            }
        }
    }

    private fun isRequired(): Boolean {
        return binding.etMail.text.toString().isNotEmpty()
    }

    private fun checkMailFormat(): Boolean {
        val email = binding.etMail.text.toString().trim()
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return if (email.matches(Regex(emailPattern))) {
            true
        } else {
            binding.alertRegister.visibility = View.VISIBLE
            binding.alertRegister.text = "Alamat email yang Anda masukkan tidak valid"
            false
        }
    }

    private fun checkMail() {
        progress(true)
        val mail = binding.etMail.text.toString()
        db.collection("user")
            .whereEqualTo("mail", binding.etMail.text.toString())
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    val intent = Intent(this, RegisterActivity::class.java)
                    intent.putExtra("mail", mail)
                    startActivity(intent)
                    progress(false)
                }
                else {
                    progress(false)
                    binding.alertRegister.visibility = View.VISIBLE
                    binding.alertRegister.text = "Alamat email telah terdaftar!"
                }
            }
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            updateRegisterButtonState()
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    private fun updateRegisterButtonState() {
        val isEnabled = isRequired()
        binding.btnRegister.isEnabled = isEnabled
        val backgroundColor = if (isEnabled) {
            resources.getColor(R.color.primary400, null)
        } else {
            resources.getColor(R.color.primary200, null)
        }
        binding.btnRegister.backgroundTintList = ColorStateList.valueOf(backgroundColor)
    }

    private fun setBarColor() {
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

package com.sciencehackfest.trive_sci.activity

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.sciencehackfest.trive_sci.util.timestampToString
import com.sciencehackfest.trive_sci.R
import com.sciencehackfest.trive_sci.databinding.ActivityLoginBinding
import com.sciencehackfest.trive_sci.model.User
import com.sciencehackfest.trive_sci.preference.PreferenceManager

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val pref by lazy { PreferenceManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.login) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = Firebase.auth
        setBarColor()
        setupListener()
    }

    private fun setupListener() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnShowPassword.setOnClickListener {
            togglePasswordVisibility(binding.etPassword, it as ImageButton)
        }

        binding.btnLogin.setOnClickListener {
            val mail = binding.etMail.text.toString()
            val password = binding.etPassword.text.toString()

            if (isRequired()) {
                if (checkMailFormat()) {
                    progress(true)
                    auth.signInWithEmailAndPassword(mail, password).addOnCompleteListener {
                        if(it.isSuccessful) {
                            db.collection("user")
                                .whereEqualTo("mail", mail)
                                .get()
                                .addOnSuccessListener { result ->
                                    progress(false)
                                    result.forEach{ document ->
                                        saveSession(
                                            User(
                                                mail = document.data["mail"].toString(),
                                                fullname = document.data["fullname"].toString(),
                                                phone = document.data["phone"].toString(),
                                                money = document.data["money"].toString().toDouble().toLong(),
                                                point = document.data["point"].toString().toLong(),
                                                token = document.data["token"].toString(),
                                                created = document.data["created"] as Timestamp
                                            )
                                        )
                                    }
                                }
                            Toast.makeText(applicationContext,
                                "Berhasil masuk", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            progress(false)
                            binding.alertLogin.visibility = View.VISIBLE
                            binding.alertLogin.text = "Gagal masuk! Periksa alamat email / kata sandi Anda"
                            Log.e("Error: ", it.exception.toString())
                        }
                    }
                }
            }
        }

        binding.btnRegister.setOnClickListener {
            val initialRegisterIntent = Intent(this, InitialRegisterActivity::class.java)
            startActivity(initialRegisterIntent)
        }

        binding.etMail.addTextChangedListener(textWatcher)
        binding.etPassword.addTextChangedListener(textWatcher)
        updateLoginButtonState()
    }

    private fun togglePasswordVisibility(editText: EditText, button: ImageButton) {
        val selectionStart = editText.selectionStart
        val selectionEnd = editText.selectionEnd
        if (editText.transformationMethod == PasswordTransformationMethod.getInstance()) {
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            button.setImageResource(R.drawable.ic_eye)
        } else {
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            button.setImageResource(R.drawable.ic_eye_slash)
        }
        editText.setSelection(selectionStart, selectionEnd)
    }

    private fun progress(progress: Boolean) {
        binding.alertLogin.visibility = View.GONE
        when (progress) {
            true -> {
                binding.btnLogin.visibility = View.INVISIBLE
            }
            false -> {
                binding.btnLogin.visibility = View.VISIBLE
            }
        }
    }

    private fun isRequired(): Boolean {
        return (
                binding.etMail.text.toString().isNotEmpty() &&
                        binding.etPassword.text.toString().isNotEmpty()
                )
    }

    private fun checkMailFormat(): Boolean {
        val email = binding.etMail.text.toString().trim()
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"

        return if (email.matches(Regex(emailPattern))) {
            true
        } else {
            binding.alertLogin.visibility = View.VISIBLE
            binding.alertLogin.text = "Alamat email yang Anda masukkan tidak valid"
            false
        }
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            updateLoginButtonState()
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    private fun updateLoginButtonState() {
        val isEnabled = isRequired()
        binding.btnLogin.isEnabled = isEnabled
        val backgroundColor = if (isEnabled) {
            resources.getColor(R.color.primary400, null)
        } else {
            resources.getColor(R.color.primary200, null)
        }
        binding.btnLogin.backgroundTintList = ColorStateList.valueOf(backgroundColor)
    }

    private fun saveSession(user: User) {
        Log.e("LoginActivity", user.toString())
        pref.put("pref_is_login", 1)
        pref.put("pref_mail", user.mail)
        pref.put("pref_fullname", user.fullname)
        pref.put("pref_phone", user.phone)
        pref.put("pref_token", user.token)
        pref.put("pref_date", timestampToString(user.created)!!)
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
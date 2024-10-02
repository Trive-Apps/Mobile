package com.sciencehackfest.trive_sci.activity

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.sciencehackfest.trive_sci.R
import com.sciencehackfest.trive_sci.databinding.ActivityRegisterBinding
import com.sciencehackfest.trive_sci.databinding.BottomSheetAddVehicleBinding
import com.sciencehackfest.trive_sci.model.User

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.register) { v, insets ->
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

        binding.btnShowPasswordConfirmation.setOnClickListener {
            togglePasswordVisibility(binding.etPasswordConfirmation, it as ImageButton)
        }

        binding.btnRegister.setOnClickListener {
            val mail = intent.getStringExtra("mail").toString()
            val password = binding.etPassword.text.toString()

            if (!validatePassword()) {
                return@setOnClickListener
            }
            if (isRequired()) {
                progress(true)
                auth.createUserWithEmailAndPassword(mail, password).addOnCompleteListener {
                    if(it.isSuccessful) {
                        addUser()
                    } else {
                        val errorMessage = when (it.exception) {
                            is FirebaseAuthWeakPasswordException -> "Kata sandi terlalu lemah"
                            is FirebaseAuthInvalidCredentialsException -> "Email tidak valid"
                            is FirebaseAuthUserCollisionException -> "Email sudah terdaftar"
                            else -> "Pendaftaran gagal, coba lagi nanti"
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                        progress(false)
                    }
                }
            }
        }

        val mail = intent.getStringExtra("mail").toString()
        setStyledSubheading(mail)

        binding.etFullname.addTextChangedListener(textWatcher)
        binding.etPassword.addTextChangedListener(textWatcher)
        binding.etPasswordConfirmation.addTextChangedListener(textWatcher)
        updateRegisterButtonState()
    }

    private fun progress(progress: Boolean) {
        binding.alertPassword.visibility = View.GONE
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
        return (
                binding.etFullname.text.toString().isNotEmpty() &&
                        binding.etPassword.text.toString().isNotEmpty() &&
                        binding.etPasswordConfirmation.text.toString().isNotEmpty()
                )
    }

    private fun validatePassword(): Boolean {
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etPasswordConfirmation.text.toString()

        if (password != confirmPassword) {
            binding.alertPassword.visibility = View.VISIBLE
            return false
        } else {
            binding.alertPassword.visibility = View.GONE
            return true
        }
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

    private fun addUser() {
        if (!validatePassword()) {
            progress(false)
            return
        }
        progress(true)
        val mail = intent.getStringExtra("mail").toString()
        val fullname = binding.etFullname.text.toString()
        val phone = binding.etPhone.text.toString()

        val user = User(
            mail = mail,
            fullname = fullname,
            phone = phone,
            money = 0,
            point = 0,
            token = "",
            created = Timestamp.now()
        )

        db.collection("user")
            .add(user)
            .addOnSuccessListener {
                progress(false)
                auth.signOut()
                Toast.makeText(applicationContext, "Akun berhasil terdaftar", Toast.LENGTH_SHORT).show()
                bottomSheetAddVehicle(user.mail)
            }
            .addOnFailureListener {
                progress(false)
                Toast.makeText(applicationContext, "Gagal menambahkan user ke Firestore", Toast.LENGTH_SHORT).show()
            }
    }

    private fun bottomSheetAddVehicle(mail: String) {
        val bottomSheetBinding = BottomSheetAddVehicleBinding.inflate(layoutInflater)
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        bottomSheetDialog.setContentView(bottomSheetBinding.root)

        fun progress(progress: Boolean) {
            when (progress) {
                true -> {
                    bottomSheetBinding.btnVerify.visibility = View.INVISIBLE
                }
                false -> {
                    bottomSheetBinding.btnVerify.visibility = View.VISIBLE
                }
            }
        }

        bottomSheetBinding.btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
            finish()
        }

        bottomSheetBinding.btnVerify.setOnClickListener {
            progress(true)
            val machineNumber = bottomSheetBinding.etMachineNumber.text.toString()

            if (machineNumber.isEmpty()) {
                progress(false)
                bottomSheetBinding.alertError.text = "Masukkan nomor mesin Anda"
                bottomSheetBinding.alertError.visibility = View.VISIBLE
                bottomSheetBinding.alertMachineNumber.visibility = View.GONE

                return@setOnClickListener
            }

            db.collection("car")
                .whereEqualTo("token", machineNumber)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        progress(false)
                        bottomSheetBinding.alertError.text = "Nomor mesin tidak valid atau kendaraan tidak terdaftar"
                        bottomSheetBinding.alertError.visibility = View.VISIBLE
                        bottomSheetBinding.alertMachineNumber.visibility = View.GONE
                    } else {
                        progress(false)
                        bottomSheetBinding.alertSuccess.text = "Kendaraan berhasil diverifikasi!"
                        bottomSheetBinding.alertSuccess.visibility = View.VISIBLE
                        bottomSheetBinding.alertError.visibility = View.GONE
                        bottomSheetBinding.alertMachineNumber.visibility = View.GONE

                        val car = documents.first()
                        val brand = car.getString("brand")
                        val type = car.getString("type")

                        bottomSheetBinding.btnVerify.visibility = View.INVISIBLE
                        bottomSheetBinding.btnRegisterVehicle.visibility = View.VISIBLE
                        bottomSheetBinding.vehicleContainer.visibility = View.VISIBLE
                        bottomSheetBinding.brand.text = brand
                        bottomSheetBinding.type.text = type
                        when (car.getString("brand")) {
                            "BMW" -> bottomSheetBinding.imgVehicleLogo.setImageResource(R.drawable.img_bmw)
                        }

                    }
                }
                .addOnFailureListener { exception ->
                    progress(false)
                    Toast.makeText(this, "Gagal memverifikasi token: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }

        bottomSheetBinding.btnRegisterVehicle.setOnClickListener {
            val machineNumber = bottomSheetBinding.etMachineNumber.text.toString()
            db.collection("user")
                .whereEqualTo("mail", mail)
                .get()
                .addOnSuccessListener { userDocuments ->
                    if (!userDocuments.isEmpty) {
                        val userDocId = userDocuments.documents.first().id
                        val token = machineNumber

                        db.collection("user").document(userDocId)
                            .update("token", token)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Kendaraan berhasil ditambahkan ke akun Anda!", Toast.LENGTH_SHORT).show()
                                bottomSheetDialog.dismiss()
                                val loginIntent = Intent(this, LoginActivity::class.java)
                                startActivity(loginIntent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Gagal menambahkan token: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Pengguna tidak ditemukan.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Gagal mendapatkan data pengguna: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }

        bottomSheetDialog.setOnDismissListener {
            bottomSheetDialog.dismiss()
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
            finish()
        }

        bottomSheetDialog.show()
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

    private fun setStyledSubheading(mail: String) {
        val fullText = "Selangkah lagi nih! Anda akan masuk dengan alamat email $mail"
        val spannableString = SpannableString(fullText)
        val start = fullText.indexOf(mail)
        val end = start + mail.length

        val color = ContextCompat.getColor(this, R.color.primary400)
        spannableString.setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.registerSubheading.text = spannableString
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
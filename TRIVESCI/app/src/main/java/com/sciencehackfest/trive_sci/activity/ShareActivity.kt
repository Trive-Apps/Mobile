package com.sciencehackfest.trive_sci.activity

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
import androidx.core.widget.addTextChangedListener
import com.sciencehackfest.trive_sci.R
import com.sciencehackfest.trive_sci.databinding.ActivityShareBinding
import com.sciencehackfest.trive_sci.preference.PreferenceManager
import com.sciencehackfest.trive_sci.util.PrefUtil

class ShareActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShareBinding
    private val pref by lazy { PreferenceManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityShareBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setNavigationBarColor()

        ViewCompat.setOnApplyWindowInsetsListener(binding.share) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val fullname = pref.getString(PrefUtil.pref_fullname)
        val initials = getInitials(fullname)
        binding.fullname.text = fullname
        binding.initial.text = initials


        updateShareButtonState(binding.etPost.text.toString().isNotEmpty())

        binding.etPost.addTextChangedListener {
            val isPostNotEmpty = it?.toString()?.isNotEmpty() ?: false
            updateShareButtonState(isPostNotEmpty)
        }

        binding.btnShare.setOnClickListener {
            Toast.makeText(this, "Maaf belum bisa memposting saat ini", Toast.LENGTH_SHORT).show()
        }

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnImage.setOnClickListener {
            Toast.makeText(this, "Maaf, fitur ini tidak tersedia saat ini", Toast.LENGTH_SHORT).show()
        }

        binding.btnExpression.setOnClickListener {
            Toast.makeText(this, "Maaf, fitur ini tidak tersedia saat ini", Toast.LENGTH_SHORT).show()
        }

        binding.btnLocation.setOnClickListener {
            Toast.makeText(this, "Maaf, fitur ini tidak tersedia saat ini", Toast.LENGTH_SHORT).show()
        }

        binding.btnCamera.setOnClickListener {
            Toast.makeText(this, "Maaf, fitur ini tidak tersedia saat ini", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateShareButtonState(isEnabled: Boolean) {
        binding.btnShare.isEnabled = isEnabled
        if (isEnabled) {
            binding.btnShare.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary500)
        } else {
            binding.btnShare.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary200)
        }
    }

    private fun setNavigationBarColor() {
        val navBarColor = ContextCompat.getColor(this, R.color.backgroundPrimary)
        val statusBarColor = ContextCompat.getColor(this, R.color.backgroundPrimary)
        window.navigationBarColor = navBarColor
        window.statusBarColor = statusBarColor
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

    private fun getInitials(fullname: String?): String {
        if (fullname.isNullOrEmpty()) return ""

        val names = fullname.trim().split("\\s+".toRegex()).toTypedArray()
        val builder = StringBuilder()

        for (name in names) {
            if (builder.length >= 2) {
                break
            }
            if (name.isNotEmpty()) {
                builder.append(name[0].toUpperCase())
            }
        }

        return builder.toString()
    }

}

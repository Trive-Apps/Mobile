package com.sciencehackfest.trive_sci.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sciencehackfest.trive_sci.R
import com.sciencehackfest.trive_sci.databinding.ActivityWelcomeBinding

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.welcome) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        setBarColor()
        setMarginSkipButton()
        setupListener()
    }

    private fun setupListener() {
        binding.btnRegister.setOnClickListener {
            val registerIntent = Intent(this, InitialRegisterActivity::class.java)
            startActivity(registerIntent)
        }

        binding.btnLogin.setOnClickListener {
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
        }

        binding.btnSkip.setOnClickListener {
            val skipIntent = Intent(this, MainActivity::class.java)
            startActivity(skipIntent)
            finish()
        }
    }

    private fun setMarginSkipButton() {
        val statusBarHeightResId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (statusBarHeightResId > 0) {
            resources.getDimensionPixelSize(statusBarHeightResId)
        } else {
            0
        }

        val additionalPaddingInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics
        ).toInt()

        val totalPaddingTop = statusBarHeight + additionalPaddingInPx

        val params = binding.btnSkip.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(
            params.leftMargin,
            totalPaddingTop,
            params.rightMargin,
            params.bottomMargin
        )
        binding.btnSkip.layoutParams = params
    }
    private fun setBarColor() {
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
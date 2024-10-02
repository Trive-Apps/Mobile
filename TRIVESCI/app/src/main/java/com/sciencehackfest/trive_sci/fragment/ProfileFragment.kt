package com.sciencehackfest.trive_sci.fragment

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.sciencehackfest.trive_sci.R
import com.sciencehackfest.trive_sci.databinding.FragmentProfileBinding
import com.sciencehackfest.trive_sci.preference.PreferenceManager
import com.sciencehackfest.trive_sci.util.PrefUtil
import com.sciencehackfest.trive_sci.activity.WelcomeActivity

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val pref by lazy { PreferenceManager(requireContext()) }
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        firebaseAuth = FirebaseAuth.getInstance()

        val fullname = pref.getString(PrefUtil.pref_fullname)
        val mail = pref.getString(PrefUtil.pref_mail)
        val initials = getInitials(fullname)
        binding.fullname.text = fullname
        binding.mail.text = mail
        binding.initial.text = initials

        binding.logout.setOnClickListener {
            firebaseAuth.signOut()
            pref.clear()
            Toast.makeText(requireContext(), "Akun berhasil keluar", Toast.LENGTH_SHORT).show()
            startActivity(
                Intent(requireActivity(), WelcomeActivity::class.java)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                Intent.FLAG_ACTIVITY_NEW_TASK
                    )
            )
            requireActivity().finish()
        }

        activity?.window?.statusBarColor = ContextCompat.getColor(requireContext(), R.color.primary400)
        activity?.window?.let { window ->
            ViewCompat.getWindowInsetsController(window.decorView)?.let { controller ->
                controller.isAppearanceLightStatusBars = false
            }
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

        binding.bgProfile.setPadding(
            binding.bgProfile.paddingLeft,
            totalPaddingTop2,
            binding.bgProfile.paddingRight,
            binding.bgProfile.paddingBottom
        )

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

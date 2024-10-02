package com.sciencehackfest.trive_sci.fragment

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.addTextChangedListener
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sciencehackfest.trive_sci.R
import com.sciencehackfest.trive_sci.activity.ShareActivity
import com.sciencehackfest.trive_sci.adapter.CommunityAdapter
import com.sciencehackfest.trive_sci.databinding.FragmentCommunityBinding
import com.sciencehackfest.trive_sci.model.Community
import com.sciencehackfest.trive_sci.preference.PreferenceManager
import com.sciencehackfest.trive_sci.util.PrefUtil

class CommunityFragment : Fragment(), CommunityAdapter.AdapterListener {

    private lateinit var binding: FragmentCommunityBinding
    private lateinit var adapter: CommunityAdapter
    private val db by lazy { Firebase.firestore }
    private val pref by lazy { PreferenceManager(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCommunityBinding.inflate(inflater, container, false)

        adapter = CommunityAdapter(arrayListOf(), this)
        binding.rvCommunity.adapter = adapter

        getCommunity()

        binding.etPost.addTextChangedListener {
            val isPostEmpty = it.isNullOrEmpty()
            updateShareButtonState(isPostEmpty)
        }

        val isPostEmpty = binding.etPost.text.isNullOrEmpty()
        updateShareButtonState(isPostEmpty)

        binding.postShare.setOnClickListener {
            startActivity(Intent(activity, ShareActivity::class.java))
        }

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(activity, ShareActivity::class.java))
        }

        binding.btnImage.setOnClickListener {
            Toast.makeText(requireContext(), "Maaf, fitur ini tidak tersedia saat ini", Toast.LENGTH_SHORT).show()
        }

        binding.btnExpression.setOnClickListener {
            Toast.makeText(requireContext(), "Maaf, fitur ini tidak tersedia saat ini", Toast.LENGTH_SHORT).show()
        }

        binding.btnCamera.setOnClickListener {
            Toast.makeText(requireContext(), "Maaf, fitur ini tidak tersedia saat ini", Toast.LENGTH_SHORT).show()
        }

        binding.btnShare.setOnClickListener {
            Toast.makeText(requireContext(), "Maaf belum bisa memposting saat ini", Toast.LENGTH_SHORT).show()
        }

        activity?.window?.statusBarColor = ContextCompat.getColor(requireContext(), R.color.backgroundPrimary)
        activity?.window?.let { window ->
            ViewCompat.getWindowInsetsController(window.decorView)?.let { controller ->
                controller.isAppearanceLightStatusBars = true
            }
        }

        val fullname = pref.getString(PrefUtil.pref_fullname)
        val initials = getInitials(fullname)
        binding.initial.text = initials

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
    }

    private fun getCommunity() {
        binding.progressBar.visibility = View.VISIBLE
        val communities: ArrayList<Community> = arrayListOf()
        db.collection("community")
            .get()
            .addOnSuccessListener { result ->
                binding.progressBar.visibility = View.GONE
                result.forEach { doc ->
                    val totalLike = (doc.data["totalLike"] as? Long)?.toInt() ?: 0
                    val totalComment = (doc.data["totalComment"] as? Long)?.toInt() ?: 0
                    val totalBookmark = (doc.data["totalBookmark"] as? Long)?.toInt() ?: 0

                    communities.add(
                        Community(
                            id = doc.reference.id,
                            fullname = doc.data["fullname"].toString(),
                            status = doc.data["status"].toString(),
                            photoStatusUrl = doc.data["photoStatusUrl"].toString(),
                            totalLike = totalLike,
                            totalComment = totalComment,
                            totalBookmark = totalBookmark,
                            createdAt = doc.data["createdAt"] as Timestamp
                        )
                    )
                }
                adapter.setData(communities)
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
            }
    }

    override fun onClick(communities: Community) {
        Toast.makeText(requireContext(), "Detail posting belum tersedia", Toast.LENGTH_SHORT).show()
    }

    private fun updateShareButtonState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.btnShare.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.primary200)
            binding.btnShare.isEnabled = false
        } else {
            binding.btnShare.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.primary500)
            binding.btnShare.isEnabled = true
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
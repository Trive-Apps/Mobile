package com.sciencehackfest.trive_sci.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sciencehackfest.trive_sci.R

class MapBottomSheetFragment : BottomSheetDialogFragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var selectedLocation: LatLng? = null
    private lateinit var listener: OnLocationSelectedListener
    private var btnSetLocation: Button? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as OnLocationSelectedListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement OnLocationSelectedListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnSetLocation = view.findViewById(R.id.btn_set_location)
        btnSetLocation?.setOnClickListener {
            selectedLocation?.let { location ->
                listener.onLocationSelected(location.latitude, location.longitude)
                dismiss()
            }
        }

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val indonesiaBounds = LatLngBounds(
            LatLng(-11.0, 95.0),
            LatLng(6.0, 141.0)
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(indonesiaBounds, 50))

        updateButtonVisibility()

        mMap.setOnMapClickListener { latLng ->
            selectedLocation = latLng
            mMap.clear()
            mMap.addMarker(MarkerOptions().position(latLng).title("Lokasi SPKLU"))
            updateButtonVisibility()
        }
    }

    private fun updateButtonVisibility() {
        btnSetLocation?.visibility = if (selectedLocation != null) View.VISIBLE else View.GONE
    }

    interface OnLocationSelectedListener {
        fun onLocationSelected(latitude: Double, longitude: Double)
    }
}

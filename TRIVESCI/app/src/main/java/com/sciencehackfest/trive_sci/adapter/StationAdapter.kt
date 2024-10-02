package com.sciencehackfest.trive_sci.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.sciencehackfest.trive_sci.R
import com.sciencehackfest.trive_sci.databinding.AdapterStationBinding
import com.sciencehackfest.trive_sci.model.Partner
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StationAdapter(
    private var station: List<Partner>,
    private val userLocation: com.google.android.gms.maps.model.LatLng,
    private val listener: AdapterListener
) : RecyclerView.Adapter<StationAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationAdapter.ViewHolder {
        val binding = AdapterStationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = station.size

    override fun onBindViewHolder(holder: StationAdapter.ViewHolder, position: Int) {
        val item = station[position]
        var formattedDistance: String? = null

        holder.binding.stationName.text = item.stationName
        holder.binding.address.text = item.address

        val calendar = Calendar.getInstance()
        val currentTime = calendar.time

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val timeOpen = timeFormat.parse(item.timeOpen)
        val timeClose = timeFormat.parse(item.timeClose)

        val calendarOpen = Calendar.getInstance().apply {
            time = timeOpen
            set(Calendar.YEAR, calendar.get(Calendar.YEAR))
            set(Calendar.MONTH, calendar.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH))
        }

        val calendarClose = Calendar.getInstance().apply {
            time = timeClose
            set(Calendar.YEAR, calendar.get(Calendar.YEAR))
            set(Calendar.MONTH, calendar.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH))
        }

        val isOpenStatus = if (currentTime.after(calendarOpen.time) && currentTime.before(calendarClose.time)) {
            holder.binding.isOpen.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.primary400))
            val drawable = ContextCompat.getDrawable(holder.itemView.context, R.drawable.ic_small_time)
            DrawableCompat.setTint(drawable!!, ContextCompat.getColor(holder.itemView.context, R.color.primary400))
            holder.binding.isOpen.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
            "Buka"
        } else {
            holder.binding.isOpen.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.system_red))
            val drawable = ContextCompat.getDrawable(holder.itemView.context, R.drawable.ic_small_time)
            DrawableCompat.setTint(drawable!!, ContextCompat.getColor(holder.itemView.context, R.color.system_red))
            holder.binding.isOpen.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
            "Tutup"
        }

        holder.binding.isOpen.text = isOpenStatus

        userLocation?.let { location ->
            val stationLocation =
                com.google.android.gms.maps.model.LatLng(item.latitude, item.longitude)
            val distanceInMeters = FloatArray(1)
            android.location.Location.distanceBetween(
                location.latitude, location.longitude,
                stationLocation.latitude, stationLocation.longitude,
                distanceInMeters
            )
            val distanceInKilometers = distanceInMeters[0] / 1000
            formattedDistance = String.format(Locale.getDefault(), "%.2f", distanceInKilometers)
            holder.binding.distance.text = String.format(Locale.getDefault(), "%.2f KM", distanceInKilometers)
        }

        val stationImg = item.stationImg
        Picasso.get().load(stationImg).into(holder.binding.stationImg, object : Callback {
            override fun onSuccess() {}
            override fun onError(e: Exception?) {}
        })

        holder.binding.container.setOnClickListener {
            listener?.onClick(item, formattedDistance)
        }
    }


    class ViewHolder(val binding: AdapterStationBinding) : RecyclerView.ViewHolder(binding.root)

    fun setData(data: List<Partner>) {
        station = data
        notifyDataSetChanged()
    }

    interface AdapterListener {
        fun onClick(station: Partner, distance: String?)
    }
}
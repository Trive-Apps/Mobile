package com.sciencehackfest.trive_sci.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sciencehackfest.trive_sci.databinding.AdapterReservationBinding
import com.sciencehackfest.trive_sci.model.Reservation
import com.sciencehackfest.trive_sci.R
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.lang.Exception
import java.text.NumberFormat
import java.util.*

class ReservationAdapter(
    private var reservations: List<Reservation>,
    private val listener: AdapterListener
) : RecyclerView.Adapter<ReservationAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservationAdapter.ViewHolder {
        val binding = AdapterReservationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )

        return ViewHolder(binding)
    }

    override fun getItemCount() = reservations.size

    override fun onBindViewHolder(holder: ReservationAdapter.ViewHolder, position: Int) {
        val item = reservations[position]

        holder.binding.stationName.text = item.stationName
        holder.binding.address.text = item.address

        val stationImg = item.stationImg
        Picasso.get().load(stationImg).into(holder.binding.stationImg, object : Callback {
            override fun onSuccess() {}
            override fun onError(e: Exception?) {}
        })

        val formattedTotalPrice = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
            maximumFractionDigits = 0
        }.format(item.price)
        holder.binding.price.text = formattedTotalPrice
        holder.binding.power.text = "${item.power} kWh"


        holder.binding.container.setOnClickListener {
            listener.onClick(item)
        }
    }

    class ViewHolder(val binding: AdapterReservationBinding) : RecyclerView.ViewHolder(binding.root)

    fun setData(data: List<Reservation>) {
        reservations = data
        notifyDataSetChanged()
    }

    interface AdapterListener {
        fun onClick(reservations: Reservation)
    }

    private fun getInitials(name: String): String {
        return name.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.toString() }
            .joinToString("")
            .uppercase()
    }
}

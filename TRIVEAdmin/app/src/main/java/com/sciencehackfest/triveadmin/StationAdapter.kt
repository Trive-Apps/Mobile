package com.sciencehackfest.triveadmin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sciencehackfest.triveadmin.databinding.AdapterStationBinding
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.lang.Exception

class StationAdapter(
    private var station: List<Partner>,
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

        holder.binding.stationName.text = item.stationName
        holder.binding.address.text = item.address

        val stationImg = item.stationImg
        Picasso.get().load(stationImg).into(holder.binding.stationImg, object : Callback {
            override fun onSuccess() {}
            override fun onError(e: Exception?) {}
        })

        holder.binding.container.setOnClickListener {
            listener?.onClick(item)
        }
    }


    class ViewHolder(val binding: AdapterStationBinding) : RecyclerView.ViewHolder(binding.root)

    fun setData(data: List<Partner>) {
        station = data
        notifyDataSetChanged()
    }

    interface AdapterListener {
        fun onClick(station: Partner)
    }
}
package com.sciencehackfest.trive_sci.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.recyclerview.widget.RecyclerView
import com.sciencehackfest.trive_sci.R
import com.sciencehackfest.trive_sci.databinding.ListItemChargerBinding
import com.sciencehackfest.trive_sci.model.Charger
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.lang.Exception

class ChargerReservationAdapter (
    var chargers: ArrayList<Charger>,
    var listener: AdapterListener?
): RecyclerView.Adapter<ChargerReservationAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChargerReservationAdapter.ViewHolder {
        return ViewHolder(
            ListItemChargerBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemCount() = chargers.size

    override fun onBindViewHolder(holder: ChargerReservationAdapter.ViewHolder, position: Int) {
        val charger = chargers[position]

        if ( charger.chargerType == "CHAdeMO") {
            Picasso.get().load(R.drawable.img_charger_chademo).into(holder.binding.chargingImg, object : Callback {
                override fun onSuccess() {}
                override fun onError(e: Exception?) {}
            })
        } else if ( charger.chargerType == "CCS2") {
            Picasso.get().load(R.drawable.img_charger_ccs_2).into(holder.binding.chargingImg, object : Callback {
                override fun onSuccess() {}
                override fun onError(e: Exception?) {}
            })
        } else if ( charger.chargerType == "Type 2") {
            Picasso.get().load(R.drawable.img_charger_type_2).into(holder.binding.chargingImg, object : Callback {
                override fun onSuccess() {}
                override fun onError(e: Exception?) {}
            })
        }

        holder.binding.chargingType.text = charger.chargerType
        holder.binding.chargingCapacity.text = charger.chargerCapacity
        holder.binding.container.setOnClickListener {
            listener?.onClick(charger)
        }
    }


    class ViewHolder(val binding: ListItemChargerBinding): RecyclerView.ViewHolder(binding.root)

    public fun setData(data: List<Charger>) {
        chargers.clear()
        chargers.addAll(data)
        notifyDataSetChanged()
    }

    fun setFilteredList(chargers: ArrayList<Charger>){
        this.chargers = chargers
        notifyDataSetChanged()
    }

    interface AdapterListener {
        fun onClick(charger: Charger)
    }
}

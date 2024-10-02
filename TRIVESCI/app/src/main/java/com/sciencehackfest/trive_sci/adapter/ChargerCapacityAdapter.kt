package com.sciencehackfest.trive_sci.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.recyclerview.widget.RecyclerView
import com.sciencehackfest.trive_sci.R

interface ChargerCapacityListener {
    fun onChargerCapacitySelected(chargerCapacity: String)
}

class ChargerCapacityAdapter(
    private val chargerCapacities: List<String>,
    private val chargerCapacityListener: ChargerCapacityListener,
    private var selectedChargerCapacity: String?
) : RecyclerView.Adapter<ChargerCapacityAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chargerCapacityRadioButton: RadioButton = itemView.findViewById(R.id.dropdown)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewCapacity: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_dropdown, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chargerCapacity = chargerCapacities[position]
        holder.chargerCapacityRadioButton.text = chargerCapacity

        holder.chargerCapacityRadioButton.isChecked = (selectedChargerCapacity == chargerCapacity)
        holder.chargerCapacityRadioButton.setOnClickListener {
            selectedChargerCapacity = chargerCapacity
            notifyDataSetChanged()

            chargerCapacityListener.onChargerCapacitySelected(selectedChargerCapacity ?: "")
        }
    }

    override fun getItemCount(): Int = chargerCapacities.size
}
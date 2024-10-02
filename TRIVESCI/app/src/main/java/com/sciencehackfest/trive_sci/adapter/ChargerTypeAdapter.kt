package com.sciencehackfest.trive_sci.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.recyclerview.widget.RecyclerView
import com.sciencehackfest.trive_sci.R

interface ChargerTypeListener {
    fun onChargerTypeSelected(chargerType: String)
}

class ChargerTypeAdapter(
    private val chargerTypes: List<String>,
    private val chargerTypeListener: ChargerTypeListener,
    private var selectedChargerType: String?
) : RecyclerView.Adapter<ChargerTypeAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chargerTypeRadioButton: RadioButton = itemView.findViewById(R.id.dropdown)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_dropdown, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chargerType = chargerTypes[position]
        holder.chargerTypeRadioButton.text = chargerType

        holder.chargerTypeRadioButton.isChecked = (selectedChargerType == chargerType)
        holder.chargerTypeRadioButton.setOnClickListener {
            selectedChargerType = chargerType
            notifyDataSetChanged()

            chargerTypeListener.onChargerTypeSelected(selectedChargerType ?: "")
        }
    }

    override fun getItemCount(): Int = chargerTypes.size
}
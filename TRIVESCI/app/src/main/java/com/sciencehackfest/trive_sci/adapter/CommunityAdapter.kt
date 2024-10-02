package com.sciencehackfest.trive_sci.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sciencehackfest.trive_sci.databinding.AdapterCommunityBinding
import com.sciencehackfest.trive_sci.model.Community
import com.sciencehackfest.trive_sci.R
import java.util.*

class CommunityAdapter(
    private var communities: List<Community>,
    private val listener: AdapterListener
) : RecyclerView.Adapter<CommunityAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityAdapter.ViewHolder {
        val binding = AdapterCommunityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )

        return ViewHolder(binding)
    }

    override fun getItemCount() = communities.size

    override fun onBindViewHolder(holder: CommunityAdapter.ViewHolder, position: Int) {
        val item = communities[position]

        holder.binding.fullname.text = item.fullname
        holder.binding.status.text = item.status
        holder.binding.totalLike.text = item.totalLike.toString()
        holder.binding.totalComment.text = item.totalComment.toString()
        holder.binding.totalBookmark.text = item.totalBookmark.toString()

        val fullnameInitial = getInitials(item.fullname)
        holder.binding.initial.text = fullnameInitial

        if (item.photoStatusUrl.isNullOrEmpty()) {
            holder.binding.containerImgNews.visibility = View.GONE
        } else {
            holder.binding.containerImgNews.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(item.photoStatusUrl)
                .placeholder(R.drawable.img_placeholder)
                .into(holder.binding.photoUrl)
        }

        item.createdAt?.let { timestamp ->
            val now = Date()
            val date = timestamp.toDate()
            val diff = now.time - date.time

            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            val weeks = days / 7
            val months = days / 30
            val years = days / 365

            val formattedDate = when {
                years > 0 -> "$years" + "y"
                months > 0 -> "$months" + "mo"
                weeks > 0 -> "$weeks" + "w"
                days > 0 -> "$days" + "d"
                hours > 0 -> "$hours" + "h"
                minutes > 0 -> "$minutes" + "m"
                else -> "$seconds" + "s"
            }

            holder.binding.timestamp.text = formattedDate
        }

        holder.binding.container.setOnClickListener {
            listener.onClick(item)
        }
    }

    class ViewHolder(val binding: AdapterCommunityBinding) : RecyclerView.ViewHolder(binding.root)

    fun setData(data: List<Community>) {
        communities = data
        notifyDataSetChanged()
    }

    interface AdapterListener {
        fun onClick(communities: Community)
    }

    private fun getInitials(name: String): String {
        return name.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.toString() }
            .joinToString("")
            .uppercase()
    }
}

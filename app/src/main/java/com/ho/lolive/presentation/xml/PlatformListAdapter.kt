package com.ho.lolive.presentation.xml

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.card.MaterialCardView
import com.ho.lolive.R
import com.ho.lolive.domain.model.LivePlatform

class PlatformListAdapter(
    private val onClick: (LivePlatform) -> Unit,
) : ListAdapter<LivePlatform, PlatformListAdapter.PlatformViewHolder>(DIFF_CALLBACK) {
    private var selectedAddress: String? = null

    fun submitPlatforms(
        platforms: List<LivePlatform>,
        selectedAddress: String?,
        onCommitted: () -> Unit = {},
    ) {
        this.selectedAddress = selectedAddress
        submitList(platforms.toList(), onCommitted)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlatformViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_platform, parent, false)
        return PlatformViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: PlatformViewHolder, position: Int) {
        holder.bind(getItem(position), selectedAddress)
    }

    class PlatformViewHolder(
        itemView: View,
        private val onClick: (LivePlatform) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.platformItemCard)
        private val iconView: ImageView = itemView.findViewById(R.id.platformItemIcon)
        private val titleView: TextView = itemView.findViewById(R.id.platformItemTitle)
        private val onlineView: TextView = itemView.findViewById(R.id.platformItemOnline)
        private val selectedTag: TextView = itemView.findViewById(R.id.platformSelectedTag)

        fun bind(item: LivePlatform, selectedAddress: String?) {
            val context = itemView.context
            val selected = item.address == selectedAddress

            titleView.text = item.title
            onlineView.text = context.getString(R.string.online_count, item.onlineCount)
            selectedTag.isVisible = selected
            iconView.load(item.iconUrl) {
                crossfade(true)
                placeholder(R.drawable.logo)
                error(R.drawable.logo)
            }

            val bgColor = if (selected) {
                ContextCompat.getColor(context, R.color.platform_selected_bg)
            } else {
                ContextCompat.getColor(context, android.R.color.white)
            }
            val strokeColor = if (selected) {
                ContextCompat.getColor(context, R.color.platform_selected_stroke)
            } else {
                ContextCompat.getColor(context, R.color.home_outline)
            }
            card.setCardBackgroundColor(bgColor)
            card.strokeColor = strokeColor
            card.strokeWidth = if (selected) 3 else 2

            itemView.setOnClickListener {
                onClick(item)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<LivePlatform>() {
            override fun areItemsTheSame(oldItem: LivePlatform, newItem: LivePlatform): Boolean {
                return oldItem.address == newItem.address
            }

            override fun areContentsTheSame(oldItem: LivePlatform, newItem: LivePlatform): Boolean {
                return oldItem == newItem
            }
        }
    }
}

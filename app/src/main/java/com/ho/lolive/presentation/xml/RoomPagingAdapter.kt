package com.ho.lolive.presentation.xml

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.ho.lolive.R
import com.ho.lolive.domain.model.LiveRoom

class RoomPagingAdapter(
    private val onClick: (String) -> Unit,
) : PagingDataAdapter<LiveRoom, RoomPagingAdapter.RoomViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_live_room, parent, false)
        return RoomViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RoomViewHolder(
        itemView: View,
        private val onClick: (String) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {
        private val coverImage: ImageView = itemView.findViewById(R.id.roomCoverImage)
        private val titleText: TextView = itemView.findViewById(R.id.roomTitleText)
        private val platformIcon: ImageView = itemView.findViewById(R.id.roomPlatformIcon)
        private val platformText: TextView = itemView.findViewById(R.id.roomPlatformText)

        private var roomId: String? = null

        init {
            itemView.setOnClickListener {
                roomId?.let(onClick)
            }
        }

        fun bind(item: LiveRoom?) {
            if (item == null) {
                roomId = null
                titleText.text = ""
                platformText.text = ""
                coverImage.setImageResource(R.drawable.logo)
                platformIcon.setImageResource(R.drawable.logo)
                itemView.isEnabled = false
                return
            }

            roomId = item.id
            titleText.text = item.title
            platformText.text = item.platformTitle
            coverImage.load(item.coverUrl) {
                crossfade(false)
                placeholder(R.drawable.logo)
                error(R.drawable.logo)
            }
            platformIcon.load(item.platformIconUrl) {
                crossfade(false)
                placeholder(R.drawable.logo)
                error(R.drawable.logo)
            }
            itemView.isEnabled = true
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<LiveRoom>() {
            override fun areItemsTheSame(oldItem: LiveRoom, newItem: LiveRoom): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: LiveRoom, newItem: LiveRoom): Boolean {
                return oldItem == newItem
            }
        }
    }
}

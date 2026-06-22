package com.ho.lolive.presentation.xml

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.ho.lolive.R
import com.ho.lolive.domain.model.LivePlatform

object PlatformSelectorBottomSheet {
    fun show(
        context: Context,
        allPlatforms: List<LivePlatform>,
        selectedAddress: String?,
        onSelected: (String) -> Unit,
    ) {
        val dialog = BottomSheetDialog(context)
        val contentView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_platform_selector, null, false)
        dialog.setContentView(contentView)

        val searchEdit = contentView.findViewById<TextInputEditText>(R.id.platformSearchEdit)
        val countText = contentView.findViewById<TextView>(R.id.platformCountText)
        val emptyText = contentView.findViewById<TextView>(R.id.platformEmptyText)
        val recyclerView = contentView.findViewById<RecyclerView>(R.id.platformRecycler)

        val adapter = PlatformListAdapter { platform ->
            onSelected(platform.address)
            dialog.dismiss()
        }
        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        var hasAutoPositionedSelected = false

        fun render(query: String) {
            val filtered = allPlatforms.filter { platform ->
                if (query.isBlank()) {
                    true
                } else {
                    platform.title.contains(query, ignoreCase = true)
                }
            }
            countText.text = context.getString(
                R.string.platform_sheet_count,
                filtered.size,
                allPlatforms.size,
            )
            emptyText.isVisible = filtered.isEmpty()
            recyclerView.isVisible = filtered.isNotEmpty()
            adapter.submitPlatforms(filtered, selectedAddress) {
                if (hasAutoPositionedSelected || query.isNotBlank()) return@submitPlatforms
                val selectedIndex = filtered.indexOfFirst { it.address == selectedAddress }
                if (selectedIndex < 0) return@submitPlatforms
                recyclerView.post {
                    layoutManager.scrollToPositionWithOffset(selectedIndex, 0)
                }
                hasAutoPositionedSelected = true
            }
        }

        searchEdit.doAfterTextChanged { editable ->
            render(editable?.toString().orEmpty())
        }
        render("")
        dialog.show()
    }
}

package mega.privacy.android.app.meeting.list.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemMeetingDataBinding
import mega.privacy.android.app.databinding.ItemMeetingHeaderBinding
import mega.privacy.android.app.meeting.list.MeetingItem
import mega.privacy.android.app.meeting.list.MeetingItemDiffCallback
import mega.privacy.android.app.utils.AdapterUtils.isValidPosition
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TimeUtils

class MeetingsAdapter constructor(
    private val itemCallback: (Long) -> Unit,
    private val itemMoreCallback: (Long) -> Unit,
) : ListAdapter<MeetingItem, RecyclerView.ViewHolder>(MeetingItemDiffCallback()), SectionTitleProvider {

    companion object {
        const val TYPE_HEADER = 100
        const val TYPE_DATA = 101
    }

    init {
        setHasStableIds(true)
    }

    private var enableScheduleMeetings: Boolean = false
    var tracker: SelectionTracker<Long>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_DATA -> {
                val binding = ItemMeetingDataBinding.inflate(layoutInflater, parent, false)
                MeetingDataViewHolder(binding).apply {
                    binding.root.setOnClickListener {
                        if (isValidPosition(bindingAdapterPosition)) {
                            itemCallback.invoke(getItem(bindingAdapterPosition).id)
                        }
                    }
                    binding.btnMore.setOnClickListener {
                        if (isValidPosition(bindingAdapterPosition)) {
                            itemMoreCallback.invoke(getItem(bindingAdapterPosition).id)
                        }
                    }
                }
            }
            else -> {
                val binding = ItemMeetingHeaderBinding.inflate(layoutInflater, parent, false)
                MeetingHeaderViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (getItemViewType(position)) {
            TYPE_DATA -> {
                val isItemSelected = tracker?.isSelected(item.id) ?: false
                (holder as MeetingDataViewHolder).bind(item as MeetingItem.Data, isItemSelected)
            }
            else -> {
                (holder as MeetingHeaderViewHolder).bind(item as MeetingItem.Header)
            }
        }
    }

    override fun getItemId(position: Int): Long =
        getItem(position).id

    override fun getSectionTitle(position: Int): String? {
        val item = getItem(position)
        return if (item is MeetingItem.Data && item.startTimestamp != null) {
            val timeStamp = item.startTimestamp
            TimeUtils.formatDate(timeStamp, TimeUtils.DATE_SHORT_SHORT_FORMAT)
        } else {
            null
        }
    }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is MeetingItem.Data -> TYPE_DATA
            else -> TYPE_HEADER
        }

    override fun submitList(list: List<MeetingItem>?) {
        if (enableScheduleMeetings && !list.isNullOrEmpty()) {
            super.submitList(addSectionHeaders(list))
        } else {
            super.submitList(list)
        }
    }

    override fun submitList(list: List<MeetingItem>?, commitCallback: Runnable?) {
        if (enableScheduleMeetings && !list.isNullOrEmpty()) {
            super.submitList(addSectionHeaders(list), commitCallback)
        } else {
            super.submitList(list, commitCallback)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun addSectionHeaders(list: List<MeetingItem>): List<MeetingItem> {
        val itemsWithHeader = mutableListOf<MeetingItem>()
        val sortedItems = (list as List<MeetingItem.Data>).sortedWith(
            compareByDescending<MeetingItem.Data> { it.isScheduled() }
                .thenBy { it.startTimestamp }
                .thenByDescending { it.timeStamp }
        )

        sortedItems.forEachIndexed { index, item ->
            val previousItem = sortedItems.getOrNull(index - 1)
            when {
                item.isScheduled() && !isSameDay(item.startTimestamp, previousItem?.startTimestamp) ->
                    itemsWithHeader.add(MeetingItem.Header(item.getStartDay()))
                !item.isScheduled() && previousItem?.isScheduled() == true ->
                    itemsWithHeader.add(MeetingItem.Header(StringResourcesUtils.getString(R.string.meetings_list_past_header)))
            }
            itemsWithHeader.add(item)
        }
        return itemsWithHeader
    }

    private fun isSameDay(timeStampA: Long?, timeStampB: Long?): Boolean =
        if (timeStampA == null || timeStampB == null) {
            false
        } else {
            TimeUtils.isSameDate(timeStampA, timeStampB)
        }

    fun setScheduleMeetingsEnabled(enable: Boolean) {
        enableScheduleMeetings = enable
        notifyDataSetChanged()
    }
}

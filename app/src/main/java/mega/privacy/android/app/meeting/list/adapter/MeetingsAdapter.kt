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
import mega.privacy.android.app.meeting.list.MeetingAdapterItemDiffCallback
import mega.privacy.android.app.utils.AdapterUtils.isValidPosition
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.domain.entity.chat.MeetingRoomItem
import java.time.Instant
import java.time.ZoneId

class MeetingsAdapter constructor(
    private val itemCallback: (Long) -> Unit,
    private val itemMoreCallback: (Long) -> Unit,
) : ListAdapter<MeetingAdapterItem, RecyclerView.ViewHolder>(MeetingAdapterItemDiffCallback()), SectionTitleProvider {

    companion object {
        const val TYPE_HEADER = 100
        const val TYPE_DATA = 101
    }

    init {
        setHasStableIds(true)
    }

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
                (holder as MeetingDataViewHolder).bind(item as MeetingAdapterItem.Data, isItemSelected)
            }
            else -> {
                (holder as MeetingHeaderViewHolder).bind(item as MeetingAdapterItem.Header)
            }
        }
    }

    override fun getItemId(position: Int): Long =
        getItem(position).id

    override fun getSectionTitle(position: Int): String? {
        val item = getItem(position)
        return if (item is MeetingAdapterItem.Data && item.room.scheduledStartTimestamp != null) {
            val timeStamp = item.room.scheduledStartTimestamp ?: return null
            TimeUtils.formatDate(timeStamp, TimeUtils.DATE_SHORT_SHORT_FORMAT)
        } else {
            null
        }
    }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is MeetingAdapterItem.Data -> TYPE_DATA
            else -> TYPE_HEADER
        }

    fun submitRoomList(list: List<MeetingRoomItem>?, commitCallback: Runnable? = null) {
        super.submitList(list.addSectionHeaders(), commitCallback)
    }

    @Suppress("UNCHECKED_CAST")
    private fun List<MeetingRoomItem>?.addSectionHeaders(): List<MeetingAdapterItem>? {
        if (isNullOrEmpty() || none(MeetingRoomItem::isPending)) {
            return this?.map(MeetingAdapterItem::Data)
        }

        val itemsWithHeader = mutableListOf<MeetingAdapterItem>()
        forEachIndexed { index, item ->
            val previousItem = getOrNull(index - 1)
            when {
                !item.isPending && previousItem?.isPending == true -> {
                    itemsWithHeader.add(MeetingAdapterItem.Header(StringResourcesUtils.getString(R.string.meetings_list_past_header)))
                }
                item.isPending && !isSameDay(item.scheduledStartTimestamp, previousItem?.scheduledStartTimestamp) -> {
                    itemsWithHeader.add(MeetingAdapterItem.Header(item.scheduledStartTimestamp!!.getHeaderTitle()))
                }
            }
            itemsWithHeader.add(MeetingAdapterItem.Data(item))
        }
        return itemsWithHeader
    }

    private fun isSameDay(timeStampA: Long?, timeStampB: Long?): Boolean =
        if (timeStampA == null || timeStampB == null) {
            false
        } else {
            val dayA = Instant.ofEpochSecond(timeStampA)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val dayB = Instant.ofEpochSecond(timeStampB)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            dayA.isEqual(dayB)
        }

    private fun Long.getHeaderTitle(): String =
        TimeUtils.formatDate(this, TimeUtils.DATE_WEEK_DAY_FORMAT, true)
}

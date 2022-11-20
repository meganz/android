package mega.privacy.android.app.meeting.list.adapter

import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemMeetingDataBinding
import mega.privacy.android.app.databinding.ItemMeetingHeaderBinding
import mega.privacy.android.app.meeting.list.MeetingItem
import mega.privacy.android.app.meeting.list.MeetingItemDiffCallback
import mega.privacy.android.app.utils.AdapterUtils.isValidPosition
import mega.privacy.android.app.utils.TimeUtils
import java.util.Calendar

class MeetingsAdapter constructor(
    private val itemCallback: (Long) -> Unit,
    private val itemMoreCallback: (Long) -> Unit,
) : ListAdapter<MeetingItem, RecyclerView.ViewHolder>(MeetingItemDiffCallback()), SectionTitleProvider {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_DATA = 1
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
                MeetingPastViewHolder(binding).apply {
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
                (holder as MeetingPastViewHolder).bind(item as MeetingItem.Data, isItemSelected)
            }
            else -> {
                (holder as MeetingHeaderViewHolder).bind(item as MeetingItem.Header)
            }
        }
    }

    override fun getItemId(position: Int): Long =
        getItem(position).id

    override fun getSectionTitle(position: Int): String {
        val item = getItem(position)
        return when (getItemViewType(position)) {
            TYPE_DATA ->
                TimeUtils.formatDate((item as MeetingItem.Data).timeStamp, TimeUtils.DATE_SHORT_SHORT_FORMAT)
            else ->
                (item as MeetingItem.Header).title
        }
    }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is MeetingItem.Data -> TYPE_DATA
            else -> TYPE_HEADER
        }

    override fun submitList(list: List<MeetingItem>?) {
        if (enableScheduleMeetings && !list.isNullOrEmpty()) {
            val sortedList: MutableList<MeetingItem> = list.sortedByDescending { (it as MeetingItem.Data).isScheduled() }.toMutableList()
            sortedList.addSectionHeaders()
            super.submitList(sortedList)
        } else {
            super.submitList(list)
        }
    }

    override fun submitList(list: List<MeetingItem>?, commitCallback: Runnable?) {
        if (enableScheduleMeetings && !list.isNullOrEmpty()) {
            val sortedList: MutableList<MeetingItem> = list.sortedByDescending { (it as MeetingItem.Data).isScheduled() }.toMutableList()
            sortedList.addSectionHeaders()
            super.submitList(sortedList, commitCallback)
        } else {
            super.submitList(list, commitCallback)
        }
    }

    private fun MutableList<MeetingItem>.addSectionHeaders() {
        val headerItems = mutableMapOf<Int, MeetingItem.Header>()
        forEachIndexed { index, item ->
            if (item !is MeetingItem.Data) return
            val previousItem = getOrNull(index - 1) as? MeetingItem.Data?
            val nextItem = getOrNull(index + 1) as? MeetingItem.Data?
            when {
                item.isScheduled() && !isSameDay(previousItem?.startTimestamp, item.startTimestamp) -> {
                    headerItems[index] = MeetingItem.Header(item.getStartDay())
                }
                item.isScheduled() && nextItem != null && !nextItem.isScheduled() -> {
                    headerItems[index] = MeetingItem.Header("Past meetings")
                }
            }
        }
        headerItems.forEach { (index, item) -> add(index, item) }
    }

    private fun isSameDay(timeStampA: Long?, timeStampB: Long?): Boolean {
        if (timeStampA == null || timeStampB == null) return false

        val dateFormat = SimpleDateFormat("ddMMyyyy")
        val calendarA = Calendar.getInstance().apply { timeInMillis = timeStampA }
        val calendarB = Calendar.getInstance().apply { timeInMillis = timeStampB }
        return dateFormat.format(calendarA.time) == dateFormat.format(calendarB.time)
    }

    fun setScheduleMeetingsEnabled(enable: Boolean) {
        enableScheduleMeetings = enable
        notifyDataSetChanged()
    }
}

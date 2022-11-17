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
        private const val TYPE_MEETING_PAST = 1
        private const val TYPE_MEETING_SCHEDULED = 2
    }

    init {
        setHasStableIds(true)
    }

    private val headerItems = mutableMapOf<Int, String>()
    private var enableScheduleMeetings: Boolean = false
    var tracker: SelectionTracker<Long>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_MEETING_PAST, TYPE_MEETING_SCHEDULED -> {
                val binding = ItemMeetingDataBinding.inflate(layoutInflater, parent, false)
                MeetingPastViewHolder(binding).apply {
                    binding.root.setOnClickListener {
                        if (isValidPosition(bindingAdapterPosition)) {
                            itemCallback.invoke(getItem(bindingAdapterPosition).chatId)
                        }
                    }
                    binding.btnMore.setOnClickListener {
                        if (isValidPosition(bindingAdapterPosition)) {
                            itemMoreCallback.invoke(getItem(bindingAdapterPosition).chatId)
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
            TYPE_MEETING_PAST, TYPE_MEETING_SCHEDULED -> {
                val isItemSelected = tracker?.isSelected(item.chatId) ?: false
                (holder as MeetingPastViewHolder).bind(item, isItemSelected)
            }
            else -> {
                (holder as MeetingHeaderViewHolder).bind(headerItems[position]!!)
            }
        }
    }

    override fun getItemId(position: Int): Long =
        getItem(position).chatId

    override fun getSectionTitle(position: Int): String =
        TimeUtils.formatDate(getItem(position).timeStamp, TimeUtils.DATE_SHORT_SHORT_FORMAT)

    override fun getItemViewType(position: Int): Int {
        if (!enableScheduleMeetings) return TYPE_MEETING_PAST
//        val currentItem = getItem(position)
//        val currentTime = System.currentTimeMillis()
        return when {
            headerItems.containsKey(position) -> TYPE_HEADER
//            (currentItem.startTimestamp ?: 0) > currentTime -> TYPE_MEETING_SCHEDULED
            else -> TYPE_MEETING_PAST
        }
    }

    override fun submitList(list: List<MeetingItem>?) {
        val sortedList = list?.sortedByDescending { it.isScheduled() }
        list.addHeadersIfRequired()
        super.submitList(sortedList)
    }

    override fun submitList(list: List<MeetingItem>?, commitCallback: Runnable?) {
        val sortedList = list?.sortedByDescending { it.isScheduled() }
        list.addHeadersIfRequired()
        super.submitList(sortedList, commitCallback)
    }

    private fun List<MeetingItem>?.addHeadersIfRequired() {
        if (enableScheduleMeetings) {
            this?.forEachIndexed { index, item ->
                val previousItem = getOrNull(index - 1)
                val nextItem = getOrNull(index + 1)
                when {
                    item.isScheduled() && isSameDay(previousItem?.startTimestamp, item.startTimestamp) -> {
                        headerItems[index] = item.getStartDay()
                    }
                    item.isScheduled() && nextItem != null && !nextItem.isScheduled() -> {
                        headerItems[index] = "Past meetings"
                    }
                }
            }
        }
    }

    private fun isSameDay(timeStampA: Long?, timeStampB: Long?): Boolean {
        if (timeStampA == null || timeStampB == null) return false

        val dateFormat = SimpleDateFormat("ddMMyyyy")
        val calendarA = Calendar.getInstance().apply {
            timeInMillis = timeStampA
        }
        val calendarB = Calendar.getInstance().apply {
            timeInMillis = timeStampB
        }
        return dateFormat.format(calendarA.time) == dateFormat.format(calendarB.time)
    }

    fun setScheduleMeetingsEnabled(enable: Boolean) {
        enableScheduleMeetings = enable
        notifyDataSetChanged()
    }
}

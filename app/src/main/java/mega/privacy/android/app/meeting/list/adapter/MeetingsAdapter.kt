package mega.privacy.android.app.meeting.list.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemMeetingHeaderBinding
import mega.privacy.android.app.databinding.ItemMeetingPastBinding
import mega.privacy.android.app.meeting.list.MeetingItem
import mega.privacy.android.app.utils.AdapterUtils.isValidPosition
import mega.privacy.android.app.utils.TimeUtils
import kotlin.random.Random

class MeetingsAdapter constructor(
    private val itemCallback: (Long) -> Unit,
    private val itemMoreCallback: (Long) -> Unit,
) : ListAdapter<MeetingItem, RecyclerView.ViewHolder>(MeetingItem.DiffCallback()), SectionTitleProvider {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_DATA = 1
    }

    init {
        setHasStableIds(true)
    }

    var tracker: SelectionTracker<Long>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_DATA -> {
                val binding = ItemMeetingPastBinding.inflate(layoutInflater, parent, false)
                MeetingDataViewHolder(binding).apply {
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
            TYPE_DATA -> {
                val isItemSelected = tracker?.isSelected(item.chatId) ?: false
                (holder as MeetingDataViewHolder).bind(item, isItemSelected)
            }
            else -> {
                (holder as MeetingHeaderViewHolder).bind(if (Random.nextBoolean()) "Today" else "Past meetings")
            }
        }
    }

    override fun getItemId(position: Int): Long =
        getItem(position).chatId

    override fun getSectionTitle(position: Int): String =
        TimeUtils.formatDate(getItem(position).timeStamp, TimeUtils.DATE_SHORT_SHORT_FORMAT)

    override fun getItemViewType(position: Int): Int =
        when (position) {
            0, 3 -> TYPE_HEADER
            else -> TYPE_DATA
        }
}

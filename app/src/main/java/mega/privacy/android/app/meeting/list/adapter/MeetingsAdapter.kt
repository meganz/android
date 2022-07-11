package mega.privacy.android.app.meeting.list.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemMeetingBinding
import mega.privacy.android.app.meeting.list.MeetingItem
import mega.privacy.android.app.utils.AdapterUtils.isValidPosition
import mega.privacy.android.app.utils.AvatarUtil

class MeetingsAdapter constructor(
    private val itemCallback: (Long) -> Unit,
    private val itemMoreCallback: (Long) -> Unit,
) : ListAdapter<MeetingItem, MeetingsViewHolder>(MeetingItem.DiffCallback()), SectionTitleProvider {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeetingsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemMeetingBinding.inflate(layoutInflater, parent, false)
        return MeetingsViewHolder(binding).apply {
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

    override fun onBindViewHolder(holder: MeetingsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long =
        getItem(position).chatId

    override fun getSectionTitle(position: Int): String =
        AvatarUtil.getFirstLetter(getItem(position).title)
}

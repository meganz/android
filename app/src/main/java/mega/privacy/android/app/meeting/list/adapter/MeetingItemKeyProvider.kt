package mega.privacy.android.app.meeting.list.adapter

import androidx.recyclerview.selection.ItemKeyProvider

class MeetingItemKeyProvider(private val adapter: MeetingsAdapter) :
    ItemKeyProvider<Long>(SCOPE_CACHED) {

    override fun getKey(position: Int): Long =
        adapter.currentList[position].id

    override fun getPosition(key: Long): Int =
        adapter.currentList.indexOfFirst { it.id == key }
}

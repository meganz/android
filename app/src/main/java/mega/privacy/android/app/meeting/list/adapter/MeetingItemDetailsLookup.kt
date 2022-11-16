package mega.privacy.android.app.meeting.list.adapter

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView

class MeetingItemDetailsLookup(private val recyclerView: RecyclerView) : ItemDetailsLookup<Long>() {

    override fun getItemDetails(event: MotionEvent): ItemDetails<Long>? =
        recyclerView.findChildViewUnder(event.x, event.y)?.let { view ->
            (recyclerView.getChildViewHolder(view) as? MeetingPastViewHolder?)?.getItemDetails()
        }
}

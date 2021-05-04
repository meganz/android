package mega.privacy.android.app.meeting.adapter

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder
import mega.privacy.android.app.R
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView
import mega.privacy.android.app.meeting.fragments.InMeetingFragment
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.meeting.listeners.GridViewListener
import mega.privacy.android.app.utils.LogUtil.logDebug

class GridViewPagerAdapter(
    private val inMeetingViewModel: InMeetingViewModel,
    private val fragment: Fragment?,
    private val maxWidth: Int,
    private val maxHeight: Int,
    private val listener: GridViewListener
) : BaseBannerAdapter<List<Participant>>() {

    var adapter: VideoGridViewAdapter? = null
    var gridView:CustomizedGridCallRecyclerView? = null
    override fun bindData(
        holder: BaseViewHolder<List<Participant>>,
        data: List<Participant>,
        position: Int,
        pageSize: Int
    ) {
        gridView = holder.findViewById(R.id.grid_view)
        gridView?.let { recyclerView ->
            recyclerView.itemAnimator = DefaultItemAnimator()
            recyclerView.setOnTouchCallback {
                (fragment as InMeetingFragment).onPageClick()
            }

            if(position == 0) {
                recyclerView.setColumnWidth(
                    when (data.size) {
                        1 -> maxWidth
                        2 -> maxWidth
                        3 -> (maxWidth * 0.8).toInt()
                        else -> maxWidth / 2
                    }
                )
            } else {
                recyclerView.setColumnWidth(maxWidth / 2)
            }

            adapter = VideoGridViewAdapter(
                inMeetingViewModel,
                recyclerView,
                maxWidth,
                maxHeight,
                position,
                listener
            )

            adapter?.let {
                it.submitList(data)
                recyclerView.adapter = it
                it.notifyDataSetChanged()
            }
        }
    }

    fun updateParticipantPrivileges(participant: Participant){
        adapter?.updateParticipantPrivileges(participant)
    }

    fun updateParticipantName(participant: Participant){
        adapter?.updateParticipantName(participant)
    }

    fun updateOnHold(participant: Participant, isOnHold:Boolean){
        adapter?.updateOnHoldSession(participant, isOnHold)
    }

    fun updateParticipantAudioVideo(typeChange:Int, participant: Participant) {
        adapter?.updateParticipantAudioVideo(typeChange, participant)
    }

    override fun getLayoutId(viewType: Int): Int {
        return R.layout.grid_view_call_item
    }
}
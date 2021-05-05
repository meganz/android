package mega.privacy.android.app.meeting.adapter

import android.content.res.Configuration
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
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
    private var maxWidth: Int,
    private var maxHeight: Int,
    private val listener: GridViewListener
) : BaseBannerAdapter<List<Participant>>() {
    private var orientation = Configuration.ORIENTATION_PORTRAIT

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

            setParamsForGridView(position, data, recyclerView)
            setColumnWidth(position, recyclerView, data.size, orientation)

            adapter = VideoGridViewAdapter(
                inMeetingViewModel,
                recyclerView,
                maxWidth,
                maxHeight,
                position,
                listener,orientation
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

    fun updateParticipantRes(participant: Participant){
        adapter?.updateParticipantRes(participant)
    }

    fun updateParticipantName(participant: Participant){
        adapter?.updateParticipantName(participant)
    }

    fun updateCallOnHold(participant: Participant, isOnHold:Boolean){
        adapter?.updateCallOnHold(participant, isOnHold)
    }

    fun updateOnHold(participant: Participant, isOnHold:Boolean){
        adapter?.updateSessionOnHold(participant, isOnHold)
    }

    fun updateParticipantAudioVideo(typeChange:Int, participant: Participant) {
        adapter?.updateParticipantAudioVideo(typeChange, participant)
    }

    override fun getLayoutId(viewType: Int): Int {
        return R.layout.grid_view_call_item
    }

    /**
     * Set new layout params for the grid view
     *
     * @param position current selected page
     * @param data the current data list
     * @param recyclerView the recycler view need to draw
     */
    private fun setParamsForGridView(
        position: Int,
        data: List<Participant>,
        recyclerView: CustomizedGridCallRecyclerView
    ) {
        val layoutParams = recyclerView.layoutParams as RecyclerView.LayoutParams
        when(orientation){
            Configuration.ORIENTATION_LANDSCAPE -> {
                if (position == 0) {
                    if (data.size == 4) {
                        layoutParams.setMargins(maxWidth / 4, 0, maxWidth / 4, 0)
                    } else if (data.size > 4) {
                        layoutParams.setMargins(maxWidth / 8, 0, maxWidth / 8, 0)
                    }
                } else {
                    layoutParams.setMargins(maxWidth / 8, 0, maxWidth / 8, 0)
                }
            }
            else -> {
                layoutParams.setMargins(0, 0, 0, 0)
            }

        }
    }

    private fun setColumnWidth(
        position: Int,
        gridView: CustomizedGridCallRecyclerView,
        size: Int,
        orientation: Int
    ) {
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            if(position == 0) {
                gridView.setColumnWidth(
                    when (size) {
                        1 -> maxWidth
                        2 -> maxWidth
                        3 -> (maxWidth * 0.8).toInt()
                        else -> maxWidth / 2
                    }
                )
            } else {
                gridView.setColumnWidth(maxWidth / 2)
            }
        } else {
            // Landscape width
            if (position == 0) {
                gridView.setColumnWidth(
                    when (size) {
                        1, 2 -> maxWidth / 2
                        3 -> (maxWidth / 3)
                        else -> maxWidth / 4
                    }
                )
            } else {
                gridView.setColumnWidth(maxWidth / 4)
            }
        }
    }

    /**
     * Change the layout when the orientation is changing
     *
     * @param newOrientation the new orientation
     * @param widthPixels the new width
     * @param heightPixels the new height
     */
    fun updateOrientation(newOrientation: Int, widthPixels: Int, heightPixels: Int) {
        orientation = newOrientation
        maxWidth = widthPixels
        maxHeight = heightPixels
        notifyDataSetChanged()
    }
}
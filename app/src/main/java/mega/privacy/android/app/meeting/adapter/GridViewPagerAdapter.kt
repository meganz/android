package mega.privacy.android.app.meeting.adapter

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder
import mega.privacy.android.app.R
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView
import mega.privacy.android.app.meeting.fragments.InMeetingFragment

class GridViewPagerAdapter(
    private val fragment: Fragment?,
    private val maxWidth: Int,
    private val maxHeight: Int,
) : BaseBannerAdapter<List<Participant>>() {

    override fun bindData(
        holder: BaseViewHolder<List<Participant>>,
        data: List<Participant>,
        position: Int,
        pageSize: Int
    ) {
        val gridView = holder.findViewById<CustomizedGridCallRecyclerView>(R.id.grid_view)
        gridView.itemAnimator = DefaultItemAnimator()
        gridView.setOnTouchCallback {
            (fragment as? InMeetingFragment)?.onPageClick()
        }

        if(position == 0) {
            gridView.setColumnWidth(
                when (data.size) {
                    2 -> maxWidth
                    3 -> (maxWidth * 0.8).toInt()
                    else -> maxWidth / 2
                }
            )
        } else {
            gridView.setColumnWidth(maxWidth / 2)
        }

        val adapter = VideoGridViewAdapter(gridView, maxWidth, maxHeight, position)
        adapter.submitList(data)

        gridView.adapter = adapter

        adapter.notifyDataSetChanged()
    }

    override fun getLayoutId(viewType: Int): Int {
        return R.layout.grid_view_call_item
    }
}
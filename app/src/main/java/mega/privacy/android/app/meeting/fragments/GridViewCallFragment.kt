package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.grid_view_call_fragment.*
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.TestTool
import mega.privacy.android.app.meeting.adapter.ParticipantVideoAdapter

class GridViewCallFragment : MeetingBaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.grid_view_call_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler_view_cameras.itemAnimator = DefaultItemAnimator()
        recycler_view_cameras.setOnTouchCallback {
            (parentFragment as InMeetingFragment).onPageClick()
        }

        val adapter = ParticipantVideoAdapter()
        recycler_view_cameras.columnWidth = 90
        recycler_view_cameras.adapter = adapter
        adapter.submitList(TestTool.getTestParticipants(view.context))
    }


    companion object {

        const val TAG = "GridViewCallFragment"

        @JvmStatic
        fun newInstance() = GridViewCallFragment()
    }

}
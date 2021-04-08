package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import kotlinx.android.synthetic.main.grid_view_call_fragment.*
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.TestTool
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.adapter.ParticipantVideoAdapter

class GridViewCallFragment : MeetingBaseFragment() {

    lateinit var adapter: ParticipantVideoAdapter

    var maxWidth = 0

    var maxHeight = 0

    // TODO test code
    val data: MutableList<Participant> = mutableListOf(Participant("Katayama Fumiki", null, "#1223ff", false, false, false, true))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.grid_view_call_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val display = meetingActivity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        maxWidth = outMetrics.widthPixels
        maxHeight = outMetrics.heightPixels

        adapter = ParticipantVideoAdapter(grid_view, maxWidth, maxHeight)
        adapter.submitList(data)

        grid_view.setOnTouchCallback {
            (parentFragment as InMeetingFragment).onPageClick()
        }
        grid_view.itemAnimator = DefaultItemAnimator()

        grid_view.adapter = adapter
    }

    private fun refreshUI() {
        grid_view.setColumnWidth(
            when (data.size) {
                2 -> maxWidth
                3 -> (maxWidth * 0.8).toInt()
                4, 5, 6 -> maxWidth / 2
                else -> maxWidth / 2
            }
        )

        adapter.notifyDataSetChanged()
    }


    // TODO test code
    fun loadParticipants(add : Boolean) {
        if(add) {
            // Random.nextInt(TestTool.testData().size)
            if (data.size < 6) {
                data.add(TestTool.testData()[data.size])
            } else {
                data.removeAll(data.subList(2, data.size))
            }
        } else {
            if(data.size > 2) {
                // Random.nextInt(data.size)
                data.removeAt(data.size - 1)
            }
        }

        refreshUI()
    }
    // TODO test code

    companion object {

        const val TAG = "GridViewCallFragment"

        @JvmStatic
        fun newInstance() = GridViewCallFragment()
    }
}
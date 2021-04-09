package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.speaker_view_call_fragment.*
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.TestTool
import mega.privacy.android.app.meeting.adapter.VideoListViewAdapter
import mega.privacy.android.app.utils.Util

class SpeakerViewCallFragment : MeetingBaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.speaker_view_call_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        participants_list.layoutManager = layoutManager
        participants_list.itemAnimator = Util.noChangeRecyclerViewItemAnimator()
        participants_list.clipToPadding = true
        participants_list.setHasFixedSize(true)

        val adapter = VideoListViewAdapter()
        adapter.submitList(TestTool.testData())
        participants_list.adapter = adapter
    }

    companion object {

        const val TAG = "SpeakerViewCallFragment"

        @JvmStatic
        fun newInstance() = SpeakerViewCallFragment()
    }
}
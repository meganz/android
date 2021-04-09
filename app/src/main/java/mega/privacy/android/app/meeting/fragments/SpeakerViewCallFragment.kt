package mega.privacy.android.app.meeting.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.speaker_view_call_fragment.*
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.ItemOperationViewModel
import mega.privacy.android.app.lollipop.adapters.InvitationContactsAdapter
import mega.privacy.android.app.meeting.TestTool
import mega.privacy.android.app.meeting.adapter.ItemClickViewModel
import mega.privacy.android.app.meeting.adapter.VideoListViewAdapter
import mega.privacy.android.app.utils.Util

class SpeakerViewCallFragment : MeetingBaseFragment() {

    lateinit var adapter: VideoListViewAdapter

    val itemClickViewModel by viewModels<ItemClickViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.speaker_view_call_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val lm = LinearLayoutManager(context)
        lm.orientation = LinearLayoutManager.HORIZONTAL

        participants_list.apply {
            layoutManager = lm
            itemAnimator = Util.noChangeRecyclerViewItemAnimator()
            clipToPadding = true
            setHasFixedSize(true)
        }

        itemClickViewModel.clickItemEvent.observe(viewLifecycleOwner, EventObserver {
            video.setBackgroundColor(Color.parseColor(it.avatarBackground))
        })

        adapter = VideoListViewAdapter(itemClickViewModel)

        // TODO test code
        adapter.submitList(TestTool.testData())

        participants_list.adapter = adapter
    }

    companion object {

        const val TAG = "SpeakerViewCallFragment"

        @JvmStatic
        fun newInstance() = SpeakerViewCallFragment()
    }
}
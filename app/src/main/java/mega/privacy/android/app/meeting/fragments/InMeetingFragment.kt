package mega.privacy.android.app.meeting.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.ChatChangeVideoStreamListener
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.VideoCaptureUtils

class InMeetingFragment : MeetingBaseFragment() {

    private lateinit var gridViewMenuItem: MenuItem
    private lateinit var speakerViewMenuItem: MenuItem

    companion object {
        fun newInstance() = InMeetingFragment()
    }

    private lateinit var viewModel: InMeetingViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        meetingActivity.window.statusBarColor = Color.TRANSPARENT
        meetingActivity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        meetingActivity.setBottomFloatingPanelViewHolder(true)

        return inflater.inflate(R.layout.in_meeting_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.in_meeting_fragment_menu, menu)

        speakerViewMenuItem = menu.findItem(R.id.speaker_view)
        gridViewMenuItem = menu.findItem(R.id.grid_view)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(InMeetingViewModel::class.java)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                findNavController().navigate(R.id.createMeetingFragment)
                true
            }
            R.id.swap_camera -> {
                logDebug("Swap camera.")
                VideoCaptureUtils.swapCamera(ChatChangeVideoStreamListener(requireContext()))
                true
            }
            R.id.grid_view -> {
                logDebug("Change to grid view.")
                gridViewMenuItem.isVisible = false
                speakerViewMenuItem.isVisible = true
                true
            }
            R.id.speaker_view -> {
                logDebug("Change to speaker view.")
                gridViewMenuItem.isVisible = true
                speakerViewMenuItem.isVisible = false
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
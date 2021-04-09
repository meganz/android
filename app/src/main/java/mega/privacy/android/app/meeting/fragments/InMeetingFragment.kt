package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.in_meeting_fragment.*
import kotlinx.android.synthetic.main.in_meeting_fragment.view.*
import mega.privacy.android.app.R
import mega.privacy.android.app.lollipop.megachat.calls.OnDragTouchListener
import mega.privacy.android.app.meeting.AnimationTool.fadeInOut
import mega.privacy.android.app.utils.LogUtil.logDebug

class InMeetingFragment : MeetingBaseFragment() {

    private lateinit var gridViewMenuItem: MenuItem
    private lateinit var speakerViewMenuItem: MenuItem

    private lateinit var individualCallFragment: IndividualCallFragment
    private lateinit var floatingWindowFragment: IndividualCallFragment
    private lateinit var gridViewCallFragment: GridViewCallFragment
    private lateinit var speakerViewCallFragment: SpeakerViewCallFragment

    companion object {
        fun newInstance() = InMeetingFragment()
    }

    private lateinit var viewModel: InMeetingViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        meetingActivity.setBottomFloatingPanelViewHolder(true)
        meetingActivity.collpaseFloatingPanel()
        meetingActivity.hideActionBar()

        return inflater.inflate(R.layout.in_meeting_fragment, container, false)
    }

    var lastTouch: Long = 0

    fun onPageClick() {
        if (System.currentTimeMillis() - lastTouch > 500) {
            in_meeting_toolbar.fadeInOut(toTop = true)
            meetingActivity.bottomFloatingPanelInOut()

            if (in_meeting_toolbar.visibility == View.VISIBLE) {
                meetingActivity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            } else {
                meetingActivity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }

            lastTouch = System.currentTimeMillis()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setOnClickListener {
            onPageClick()
        }

        self_feed_floating_window_container.setOnTouchListener(
            OnDragTouchListener(
                view.self_feed_floating_window_container,
                view
            )
        )

        gridViewCallFragment = GridViewCallFragment.newInstance()
//        loadChildFragment(
//            R.id.meeting_container,
//            gridViewCallFragment,
//            GridViewCallFragment.TAG
//        )

        speakerViewCallFragment = SpeakerViewCallFragment.newInstance()
        loadChildFragment(
            R.id.meeting_container,
            speakerViewCallFragment,
            SpeakerViewCallFragment.TAG
        )

        floatingWindowFragment = IndividualCallFragment.newInstance(1, 2, true)
        loadChildFragment(
            R.id.self_feed_floating_window_container,
            floatingWindowFragment,
            IndividualCallFragment.TAG
        )

        meetingActivity.setSupportActionBar(view.in_meeting_toolbar)
        val actionBar = meetingActivity.supportActionBar ?: return
        actionBar.setHomeButtonEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        setHasOptionsMenu(true)

        view.setOnApplyWindowInsetsListener { _, insets ->
            insets
        }
    }

    private fun loadChildFragment(containerId: Int, fragment: Fragment, tag: String) {
        childFragmentManager.beginTransaction().replace(
            containerId,
            fragment,
            tag
        ).commit()
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
                //TODO test code
                gridViewCallFragment.loadParticipants(true)
//                logDebug("Swap camera.")
//                VideoCaptureUtils.swapCamera(ChatChangeVideoStreamListener(requireContext()))
                true
            }
            R.id.grid_view -> {
                logDebug("Change to grid view.")
                gridViewMenuItem.isVisible = false
                speakerViewMenuItem.isVisible = true

                loadChildFragment(
                    R.id.meeting_container,
                    gridViewCallFragment,
                    GridViewCallFragment.TAG
                )
                true
            }
            R.id.speaker_view -> {
                logDebug("Change to speaker view.")
                gridViewMenuItem.isVisible = true
                speakerViewMenuItem.isVisible = false

                loadChildFragment(
                    R.id.meeting_container,
                    speakerViewCallFragment,
                    SpeakerViewCallFragment.TAG
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
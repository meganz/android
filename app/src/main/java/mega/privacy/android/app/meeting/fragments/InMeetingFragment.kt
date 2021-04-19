package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.activity_meeting.*
import kotlinx.android.synthetic.main.in_meeting_fragment.*
import kotlinx.android.synthetic.main.in_meeting_fragment.view.*
import mega.privacy.android.app.R
import mega.privacy.android.app.lollipop.megachat.calls.OnDragTouchListener
import mega.privacy.android.app.meeting.AnimationTool.fadeInOut
import mega.privacy.android.app.meeting.AnimationTool.moveY
import mega.privacy.android.app.utils.LogUtil.logDebug
import kotlin.random.Random

class InMeetingFragment : MeetingBaseFragment() {

    private lateinit var gridViewMenuItem: MenuItem
    private lateinit var speakerViewMenuItem: MenuItem

    private lateinit var individualCallFragment: IndividualCallFragment
    private lateinit var floatingWindowFragment: IndividualCallFragment
    private lateinit var gridViewCallFragment: GridViewCallFragment
    private lateinit var speakerViewCallFragment: SpeakerViewCallFragment

    val inMeetingViewModel by viewModels<InMeetingViewModel>()

    companion object {
        fun newInstance() = InMeetingFragment()
    }

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

    var previousY = -1f

    fun onPageClick() {
        // Prevent fast tapping.
        if (System.currentTimeMillis() - lastTouch < 500) return

        in_meeting_toolbar.fadeInOut(toTop = true)
        meetingActivity.bottomFloatingPanelInOut()

        if (in_meeting_toolbar.visibility == View.VISIBLE) {
            meetingActivity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            meetingActivity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        placeFloatingWindow()

        lastTouch = System.currentTimeMillis()
    }

    private fun placeFloatingWindow() {
        checkRelativePositionWithToolbar()
        checkRelativePositionWithBottomSheet()
    }

    private fun checkRelativePositionWithToolbar() {
        val isIntersect = (in_meeting_toolbar.bottom - self_feed_floating_window_container.y) > 0
        if (in_meeting_toolbar.visibility == View.VISIBLE && isIntersect) {
            self_feed_floating_window_container.moveY(in_meeting_toolbar.bottom.toFloat())
        }

        val isIntersectPreviously = (in_meeting_toolbar.bottom - previousY) > 0
        if (in_meeting_toolbar.visibility == View.GONE && isIntersectPreviously && previousY >= 0) {
            self_feed_floating_window_container.moveY(previousY)
        }
    }

    private fun checkRelativePositionWithBottomSheet() {
        val bottom =
            self_feed_floating_window_container.y + self_feed_floating_window_container.height
        val top = meetingActivity.bottom_floating_panel.top
        val margin1 = bottom - top

        val isIntersect = margin1 > 0
        if (meetingActivity.bottom_floating_panel.visibility == View.VISIBLE && isIntersect) {
            self_feed_floating_window_container.moveY(self_feed_floating_window_container.y - margin1)
        }

        val margin2 =
            previousY + self_feed_floating_window_container.height - meetingActivity.bottom_floating_panel.top
        val isIntersectPreviously = margin2 > 0
        if (meetingActivity.bottom_floating_panel.visibility == View.GONE && isIntersectPreviously && previousY >= 0) {
            self_feed_floating_window_container.moveY(previousY)
        }
    }

    private lateinit var dragTouchListener: OnDragTouchListener

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.setOnClickListener {
            onPageClick()
        }

        dragTouchListener = OnDragTouchListener(
            view.self_feed_floating_window_container,
            view,
            object : OnDragTouchListener.OnDragActionListener {

                override fun onDragStart(view: View?) {
                    if (in_meeting_toolbar.visibility == View.VISIBLE) {
                        dragTouchListener.setToolbarHeight(in_meeting_toolbar.bottom)
                        dragTouchListener.setBottomSheetHeight(meetingActivity.bottom_floating_panel.top)
                    } else {
                        dragTouchListener.setToolbarHeight(0)
                        dragTouchListener.setBottomSheetHeight(0)
                    }
                }

                override fun onDragEnd(view: View) {
                    // Record the last Y of the floating window after dragging ended.
                    previousY = view.y
                }

            }
        )
        self_feed_floating_window_container.setOnTouchListener(dragTouchListener)

        individualCallFragment = IndividualCallFragment.newInstance(1, 2, false)
        gridViewCallFragment = GridViewCallFragment.newInstance()
        speakerViewCallFragment = SpeakerViewCallFragment.newInstance()

        //TODO test code start
        loadChildFragment(
            R.id.meeting_container,
            gridViewCallFragment,
            GridViewCallFragment.TAG
        )

        floatingWindowFragment = IndividualCallFragment.newInstance(1, 2, true)
        loadChildFragment(
            R.id.self_feed_floating_window_container,
            floatingWindowFragment,
            IndividualCallFragment.TAG
        )
        //TODO test code end

        meetingActivity.setSupportActionBar(view.in_meeting_toolbar)
        val actionBar = meetingActivity.supportActionBar ?: return
        actionBar.setHomeButtonEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        setHasOptionsMenu(true)

        // decor.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        meetingActivity.window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or 0x00000010

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                findNavController().navigate(R.id.createMeetingFragment)
                true
            }
            R.id.swap_camera -> {
                //TODO test code start: add or remove last participants
                inMeetingViewModel.addParticipant(Random.nextBoolean())
//                logDebug("Swap camera.")
//                VideoCaptureUtils.swapCamera(ChatChangeVideoStreamListener(requireContext()))
                //TODO test code end: add or remove last participants
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
package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.view.*
import android.widget.RelativeLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.in_meeting_fragment.*
import kotlinx.android.synthetic.main.in_meeting_fragment.view.*
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.ChatChangeVideoStreamListener
import mega.privacy.android.app.lollipop.megachat.calls.OnDragTouchListener
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.VideoCaptureUtils

class InMeetingFragment : MeetingBaseFragment() {

    private lateinit var gridViewMenuItem: MenuItem
    private lateinit var speakerViewMenuItem: MenuItem
    private lateinit var root: View

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        root = view
        view.setOnClickListener {
            if (System.currentTimeMillis() - lastTouch > 500) {
                root.in_meeting_toolbar.flick(-220f)
                meetingActivity.BottomFloatingPanelInOut()
                lastTouch = System.currentTimeMillis()
            }
        }

        self_feed_floating_window_container.setOnTouchListener(
            OnDragTouchListener(
                view.self_feed_floating_window_container,
                view
            )
        )

        loadChildFragment(
            R.id.meeting_container,
            GridViewCallFragment.newInstance(),
            "sdas"
        )

        loadChildFragment(
            R.id.self_feed_floating_window_container,
            SelfFeedFloatingWindowFragment.newInstance(1, 2),
            "sdas"
        )

        meetingActivity.setSupportActionBar(view.in_meeting_toolbar)
        val actionBar = meetingActivity.supportActionBar ?: return
        actionBar.setHomeButtonEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        setHasOptionsMenu(true)

//        meetingActivity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        view.setOnApplyWindowInsetsListener { _, insets ->
            insets
        }
    }

    private fun View.flick(to: Float) {
        if (visibility == View.VISIBLE) {
            animate().translationY(to).setDuration(500).withEndAction {
                visibility = View.GONE
            }.withStartAction {
                meetingActivity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }.start()
        } else {
            animate().translationY(0f).setDuration(500).withStartAction {
                visibility = View.VISIBLE
                meetingActivity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }.start()
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
                logDebug("Swap camera.")
                VideoCaptureUtils.swapCamera(ChatChangeVideoStreamListener(requireContext()))
                true
            }
            R.id.grid_view -> {
                logDebug("Change to grid view.")
                gridViewMenuItem.isVisible = false
                speakerViewMenuItem.isVisible = true
                loadChildFragment(
                    R.id.meeting_container,
                    GridViewCallFragment.newInstance(),
                    "sdas"
                )
                true
            }
            R.id.speaker_view -> {
                logDebug("Change to speaker view.")
                gridViewMenuItem.isVisible = true
                speakerViewMenuItem.isVisible = false

                loadChildFragment(
                    R.id.meeting_container,
                    IndividualCallFragment.newInstance(),
                    "sdas"
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
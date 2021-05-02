package mega.privacy.android.app.meeting

import android.content.res.ColorStateList
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import mega.privacy.android.app.R
import mega.privacy.android.app.components.OnOffFab
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.databinding.InMeetingFragmentBinding
import mega.privacy.android.app.lollipop.megachat.AppRTCAudioManager
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.adapter.ParticipantsAdapter
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.Util

/**
 * Bottom Panel view holder package the view and logic code of floating panel
 *
 * @property binding InMeetingFragmentBinding, get views from this binding
 * @property listener listen to the actions of all buttons
 * @property isGuest the flag for determining if the current user is guest
 * @property isModerator the flag for determining if the current user is moderator
 */
class BottomFloatingPanelViewHolder(
    private val binding: InMeetingFragmentBinding,
    private val listener: BottomFloatingPanelListener,
    private var isGuest: Boolean,
    private var isModerator: Boolean,
    private var isGroup: Boolean = false
) {
    private val context = binding.root.context
    private val floatingPanelView = binding.bottomFloatingPanel

    private val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomFloatingPanel.root)
    val propertyUpdaters = ArrayList<(Float) -> Unit>()

    private var bottomFloatingPanelExpanded = false
    private var expandedTop = 0
    private var collapsedTop = 0

    /**
     * Save the Mic & Cam state, for revering state when hold state changed
     */
    private var savedMicState: Boolean = false
    private var savedCamState: Boolean = false
    private var savedSpeakerState: AppRTCAudioManager.AudioDevice =
        AppRTCAudioManager.AudioDevice.SPEAKER_PHONE

    private val participantsAdapter = ParticipantsAdapter(listener)

    init {
        initButtonsState()
        setupBottomSheet()
        listenButtons()
        setupRecyclerView()
        initShareAndInviteButton()

        /**
         * Expanded bottom sheet when the meeting is group chat
         * If the meeting is one-to-one chat, just show the control button, and would not let user drag the bottom panel
         */
        if (isGroup) {
            expand()
        } else {
            collapse()
        }

        floatingPanelView.indicator.isVisible = isGroup
    }

    /**
     * Init the visibility of `ShareLink` & `Invite` Button
     *
     */
    private fun initShareAndInviteButton() {
        floatingPanelView.shareLink.isVisible = !isGuest
        floatingPanelView.invite.isVisible = !isGuest
        floatingPanelView.guestShareLink.isVisible = isGuest
    }

    /**
     * Init the state for the Mic, Cam and End button on button bar
     */
    private fun initButtonsState() {
        floatingPanelView.fabMic.isOn = savedMicState
        floatingPanelView.fabCam.isOn = savedCamState
        updateSpeakerIcon(savedSpeakerState)
        floatingPanelView.fabEnd.setImageResource(R.drawable.ic_remove)
    }

    /**
     * Init Participants and update the list, and update the text showing participants size
     *
     * @param participants newest participant list
     */
    fun setParticipants(participants: List<Participant>) {
        participantsAdapter.submitList(participants.toMutableList())

        floatingPanelView.participantsNum.text = getString(
            R.string.participants_number, participants.size
        )
    }

    /**
     * Set the listener for bottom sheet behavior and property list
     *
     */
    private fun setupBottomSheet() {
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                Log.d("bottomSheetBehavior", "newState:$newState")
                bottomFloatingPanelExpanded = newState == BottomSheetBehavior.STATE_EXPANDED
                if (newState == BottomSheetBehavior.STATE_DRAGGING && !isGroup) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }

                when {
                    newState == BottomSheetBehavior.STATE_EXPANDED && expandedTop == 0 -> {
                        expandedTop = binding.bottomFloatingPanel.root.top
                    }
                    newState == BottomSheetBehavior.STATE_COLLAPSED && collapsedTop == 0 -> {
                        collapsedTop = binding.bottomFloatingPanel.root.top
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                Log.d("bottomSheetBehavior", "onSlide")
                onBottomFloatingPanelSlide(slideOffset)
            }
        })

        propertyUpdaters.add(
            propertyUpdater(
                binding.bottomFloatingPanel.backgroundMask,
                BOTTOM_PANEL_MIN_ALPHA, 1F
            ) { view, value -> view.alpha = value })

        val indicatorColorStart = 0x4F
        val indicatorColorEnd = 0xBD
        propertyUpdaters.add(
            propertyUpdater(
                binding.bottomFloatingPanel.indicator, indicatorColorStart, indicatorColorEnd
            ) { view, value ->
                view.backgroundTintList = ColorStateList.valueOf(composeColor(value))
            })

        setupFabUpdater()
        setupFabLabelUpdater()

        // Set the half expanded ratio, to set the height for the `STATE_HALF_EXPANDED`
        // if ratio is not between 0 and 1, will set to 0.001f
        post {
            val peekHeight =
                context.resources.getDimensionPixelSize(R.dimen.meeting_bottom_floating_panel_peek_height)

            var ratio = peekHeight.toFloat() / binding.root.measuredHeight
            if ((ratio <= 0) || (ratio >= 1)) {
                ratio = 0.001f
            }
            bottomSheetBehavior.halfExpandedRatio = ratio
        }
    }

    /**
     * Init listener for all the button
     */
    private fun listenButtons() {
        floatingPanelView.apply {
            fabMic.setOnOffCallback {
                savedMicState = it
                listener.onChangeMicState(binding.bottomFloatingPanel.fabMic.isOn)
            }

            fabCam.setOnOffCallback {
                savedCamState = it
                listener.onChangeCamState(binding.bottomFloatingPanel.fabCam.isOn)
            }

            fabSpeaker.setOnClickListener {
                listener.onChangeSpeakerState()
            }

            fabHold.setOnOffCallback {
                listener.onChangeHoldState(binding.bottomFloatingPanel.fabHold.isOn)
            }

            fabEnd.setOnClickListener {
                listener.onEndMeeting()
            }

            shareLink.setOnClickListener {
                listener.onShareLink()
            }

            guestShareLink.setOnClickListener {
                listener.onShareLink()
            }

            invite.setOnClickListener {
                listener.onInviteParticipants()
            }
        }
    }

    /**
     * Init recyclerview
     *
     */
    private fun setupRecyclerView() {
        floatingPanelView.participants.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = null
            clipToPadding = false
            adapter = participantsAdapter
            addItemDecoration(SimpleDividerItemDecoration(context))
        }
    }

    private fun updateBottomFloatingPanelIfNeeded() {
        if (bottomFloatingPanelExpanded) {
            onBottomFloatingPanelSlide(1F)
        }
    }

    private fun onBottomFloatingPanelSlide(slideOffset: Float) {
        val ratio = if (slideOffset < BOTTOM_PANEL_PROPERTY_UPDATER_OFFSET_THRESHOLD) {
            slideOffset / BOTTOM_PANEL_PROPERTY_UPDATER_OFFSET_THRESHOLD
        } else {
            1F
        }

        for (updater in propertyUpdaters) {
            updater(ratio)
        }
    }

    private fun setupFabLabelUpdater() {
        setupFabLabelUpdater(binding.bottomFloatingPanel.fabMicLabel)
        setupFabLabelUpdater(binding.bottomFloatingPanel.fabCamLabel)
        setupFabLabelUpdater(binding.bottomFloatingPanel.fabHoldLabel)
        setupFabLabelUpdater(binding.bottomFloatingPanel.fabSpeakerLabel)
        setupFabLabelUpdater(binding.bottomFloatingPanel.fabEndLabel)
    }

    private fun setupFabLabelUpdater(label: TextView) {
        val isDarkMode = Util.isDarkMode(context)
        val fabLabelColorStart = if (isDarkMode) 0xE2 else 0xFF
        val fabLabelColorEnd = if (isDarkMode) 0xE2 else 0x21

        propertyUpdaters.add(
            propertyUpdater(
                label, fabLabelColorStart, fabLabelColorEnd
            ) { view, value -> view.setTextColor(composeColor(value)) })
    }

    private fun setupFabUpdater() {
        setupFabBackgroundTintUpdater(binding.bottomFloatingPanel.fabMic)
        setupFabBackgroundTintUpdater(binding.bottomFloatingPanel.fabCam)
        setupFabBackgroundTintUpdater(binding.bottomFloatingPanel.fabHold)
        setupFabBackgroundTintUpdater(binding.bottomFloatingPanel.fabSpeaker)
    }

    private fun setupFabBackgroundTintUpdater(fab: OnOffFab) {
        val isDarkMode = Util.isDarkMode(context)
        val fabBackgroundTintStart = if (isDarkMode) 0x6C else 0x4F
        val fabBackgroundTintEnd = if (isDarkMode) 0x6C else 0x75

        propertyUpdaters.add(
            propertyUpdater(
                fab, fabBackgroundTintStart, fabBackgroundTintEnd
            ) { view, value ->
                if (view.isOn) {
                    view.backgroundTintList = ColorStateList.valueOf(composeColor(value))
                }
            })
    }

    private fun <V : View> propertyUpdater(
        view: V,
        startP: Int,
        endP: Int,
        update: (view: V, value: Int) -> Unit
    ): (Float) -> Unit {
        return {
            update(view, (startP + (endP - startP) * it).toInt())
        }
    }

    private fun <V : View> propertyUpdater(
        view: V,
        startP: Float,
        endP: Float,
        update: (view: V, value: Float) -> Unit
    ): (Float) -> Unit {
        return {
            update(view, startP + (endP - startP) * it)
        }
    }

    private fun composeColor(component: Int): Int {
        return ((component.shl(16) or component.shl(8) or component).toLong() or 0xFF000000).toInt()
    }

    /**
     * Collapse the bottom sheet
     *
     */
    fun collapse() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomFloatingPanelExpanded = false
        onBottomFloatingPanelSlide(0F)
    }

    /**
     * Expand the bottom sheet
     *
     */
    fun expand() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomFloatingPanelExpanded = true
        onBottomFloatingPanelSlide(1F)
    }

    fun updateMicIcon(micOn: Boolean) {
        floatingPanelView.fabMic.isOn = micOn
    }

    fun enableHoldIcon(isEnabled: Boolean, isHold: Boolean) {
        floatingPanelView.fabHold.enable = isEnabled
        updateHoldIcon(isHold)
    }

    fun updateHoldIcon(isHold: Boolean) {
        floatingPanelView.fabHold.isOn = !isHold

        floatingPanelView.fabMic.apply {
            enable = !isHold
        }
        floatingPanelView.fabCam.apply {
            enable = !isHold
        }
    }

    fun updateCamIcon(micOn: Boolean) {
        floatingPanelView.fabCam.isOn = micOn
    }

    fun updateSpeakerIcon(device: AppRTCAudioManager.AudioDevice) {
        when (device) {
            AppRTCAudioManager.AudioDevice.SPEAKER_PHONE -> {
                floatingPanelView.fabSpeaker.isOn = true
                floatingPanelView.fabSpeaker.setOnIcon(R.drawable.ic_speaker_on)
                floatingPanelView.fabSpeakerLabel.text = getString(R.string.general_speaker)
            }
            AppRTCAudioManager.AudioDevice.EARPIECE -> {
                floatingPanelView.fabSpeaker.isOn = false
                floatingPanelView.fabSpeaker.setOnIcon(R.drawable.ic_speaker_off)
                floatingPanelView.fabSpeakerLabel.text = getString(R.string.general_speaker)
            }
            else -> {
                floatingPanelView.fabSpeaker.isOn = true
                floatingPanelView.fabSpeaker.setOnIcon(R.drawable.ic_headphone)
                floatingPanelView.fabSpeakerLabel.text = getString(R.string.general_headphone)
            }
        }
    }

    companion object {
        private const val BOTTOM_PANEL_MIN_ALPHA = 0.66F
        private const val BOTTOM_PANEL_PROPERTY_UPDATER_OFFSET_THRESHOLD = 0.5F
    }
}


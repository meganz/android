package mega.privacy.android.app.meeting

import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import mega.privacy.android.app.R
import mega.privacy.android.app.components.OnOffFab
import mega.privacy.android.app.databinding.ActivityMeetingBinding
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.adapter.ParticipantsAdapter
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator

/**
 * Necessary value
 *
 * Mic, Cam, Speaker state
 */
class BottomFloatingPanelViewHolder(
    private val binding: ActivityMeetingBinding,
    private val listener: BottomFloatingPanelListener,
    private var isGuest: Boolean,
    private var isModerator: Boolean
) {
    private val context = binding.root.context
    private val floatingPanelView = binding.bottomFloatingPanel

    private val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomFloatingPanel.root)
    private val propertyUpdaters = ArrayList<(Float) -> Unit>()

    private var bottomFloatingPanelExpanded = false
    private var expandedTop = 0
    private var collapsedTop = 0

    /**
     * Save the Mic & Cam state, for revering state when hold state changed
     */
    private var savedMicState: Boolean = false
    private var savedCamState: Boolean = false

    private val participantsAdapter = ParticipantsAdapter(listener)

    private val speakerVH = SpeakerButtonViewHolder(
        binding.bottomFloatingPanel.fabSpeaker,
        binding.bottomFloatingPanel.fabSpeakerLabel
    ) {
        updateBottomFloatingPanelIfNeeded()

        listener.onChangeAudioDevice(it)
    }

    init {
        initButtonsState()
        setupBottomSheet()
        listenButtons()
        setupRecyclerView()
        initShareAndInviteButton()

        /**
         * Expanded bottom sheet when init
         */
        post {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottomFloatingPanelExpanded = true
            onBottomFloatingPanelSlide(1F)
        }
    }

    private fun initShareAndInviteButton() {
        floatingPanelView.shareLink.isVisible = !isGuest
        floatingPanelView.invite.isVisible = !isGuest
        floatingPanelView.guestShareLink.isVisible = isGuest
    }

    /**
     * Init the state for the buttons on button bar
     */
    private fun initButtonsState() {
        floatingPanelView.fabMic.isOn = savedMicState
        floatingPanelView.fabCam.isOn = savedCamState
        // End meeting for all isn't included in MVP version.
        floatingPanelView.fabEnd.setImageResource(if (isModerator) R.drawable.ic_end_call else R.drawable.ic_remove)
    }

    fun setParticipants(participants: List<Participant>) {
        participantsAdapter.submitList(participants)

        floatingPanelView.participantsNum.text = getString(
            R.string.participants_number, participants.size
        )
    }

    fun onHeadphoneConnected(wiredHeadset: Boolean, bluetooth: Boolean) {
        speakerVH.onHeadphoneConnected(wiredHeadset, bluetooth)

        updateBottomFloatingPanelIfNeeded()
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                bottomFloatingPanelExpanded = newState == BottomSheetBehavior.STATE_EXPANDED

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

        post {
            val peekHeight =
                context.resources.getDimensionPixelSize(R.dimen.meeting_bottom_floating_panel_peek_height)
            bottomSheetBehavior.halfExpandedRatio =
                peekHeight.toFloat() / binding.root.measuredHeight
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

            fabHold.setOnOffCallback {
                // if isHold is off, should disable the camera and mic
                updateHoldState(it)
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
     * If meeting is hold, disable the cam & mic
     *
     * If meeting isn't hold, change to the previous state
     */
    private fun updateHoldState(isNotHold: Boolean) {
        floatingPanelView.fabMic.isOn = if (isNotHold) savedMicState else false
        floatingPanelView.fabCam.isOn = if (isNotHold) savedCamState else false
    }

    private fun setupRecyclerView() {
        floatingPanelView.participants.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = noChangeRecyclerViewItemAnimator()
            clipToPadding = false
            setHasFixedSize(true)
            adapter = participantsAdapter
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

    fun collpase() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomFloatingPanelExpanded = false
        onBottomFloatingPanelSlide(0F)
    }

    companion object {
        private const val BOTTOM_PANEL_MIN_ALPHA = 0.66F
        private const val BOTTOM_PANEL_PROPERTY_UPDATER_OFFSET_THRESHOLD = 0.5F
    }
}


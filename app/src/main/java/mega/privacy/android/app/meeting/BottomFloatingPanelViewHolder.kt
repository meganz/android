package mega.privacy.android.app.meeting

import android.animation.ArgbEvaluator
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import mega.privacy.android.app.R
import mega.privacy.android.app.components.OnOffFab
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.databinding.InMeetingFragmentBinding
import mega.privacy.android.app.lollipop.megachat.AppRTCAudioManager
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_CREATE
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_GUEST
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.adapter.ParticipantsAdapter
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaChatSession

/**
 * Bottom Panel view holder package the view and logic code of floating panel
 *
 * @property binding InMeetingFragmentBinding, get views from this binding
 * @property listener listen to the actions of all buttons
 * @property isGuest the flag for determining if the current user is guest
 * @property isModerator the flag for determining if the current user is moderator
 */
class BottomFloatingPanelViewHolder(
    private val inMeetingViewModel: InMeetingViewModel,
    private val binding: InMeetingFragmentBinding,
    private val listener: BottomFloatingPanelListener,
    private var isGroup: Boolean = true
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

    private val participantsAdapter = ParticipantsAdapter(inMeetingViewModel, listener)

    init {
        initButtonsState()
        setupBottomSheet()
        listenButtons()
        setupRecyclerView()
        updateShareAndInviteButton()

        updatePanel()
    }

    /**
     * Expanded bottom sheet when the meeting is group chat
     * If the meeting is one-to-one chat, just show the control button, and would not let user drag the bottom panel
     */
    private fun updatePanel() {
        if (isGroup) {
            expand()
        } else {
            collapse()
        }

        floatingPanelView.indicator.isVisible = isGroup
        updateShareAndInviteButton()
    }

    /**
     * Init the visibility of `ShareLink` & `Invite` Button
     *
     */
    fun updateShareAndInviteButton() {
        floatingPanelView.shareLink.isVisible = inMeetingViewModel.isLinkVisible()
        floatingPanelView.invite.isVisible = inMeetingViewModel.isLinkVisible()
        floatingPanelView.guestShareLink.isVisible = inMeetingViewModel.isGuest()
    }

    /**
     * Init the state for the Mic, Cam and End button on button bar
     */
    private fun initButtonsState() {
        floatingPanelView.fabMic.isOn = savedMicState
        floatingPanelView.fabCam.isOn = savedCamState
        updateSpeakerIcon(savedSpeakerState)
    }

    /**
     * Init Participants and update the list, and update the text showing participants size
     *
     * @param participants newest participant list
     */
    fun setParticipants(participants: MutableList<Participant>, myOwnInfo: Participant) {
        participants.add(myOwnInfo)
        participantsAdapter.submitList(
            participants.sortedWith(
                compareBy(
                    { !it.isModerator },
                    { !it.isMe },
                    { it.name })
            ).toMutableList()
        )
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

        initUpdaters()

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

    private fun initUpdaters() {
        propertyUpdaters.add(
            propertyUpdater(
                binding.bottomFloatingPanel.backgroundMask,
                BOTTOM_PANEL_MIN_ALPHA, 1F
            ) { view, value ->
                run {
                    val argbEvaluator = ArgbEvaluator()
                    val background = argbEvaluator.evaluate(
                        value,
                        ContextCompat.getColor(context, R.color.grey_alpha_070),
                        ContextCompat.getColor(context, R.color.white_grey_900)
                    ) as Int

                    val grad: GradientDrawable = view.background as GradientDrawable
                    grad.setColor(background)

                    view.background = grad
                }
            })

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
            fabMic.setOnChangeCallback {
                updateBottomFloatingPanelIfNeeded()
            }

            fabCam.setOnOffCallback {
                savedCamState = it
                listener.onChangeCamState(binding.bottomFloatingPanel.fabCam.isOn)
            }

            fabCam.setOnChangeCallback {
                updateBottomFloatingPanelIfNeeded()
            }

            fabSpeaker.setOnClickListener {
                listener.onChangeSpeakerState()
            }

            fabSpeaker.setOnChangeCallback {
                updateBottomFloatingPanelIfNeeded()
            }

            fabHold.setOnOffCallback {
                listener.onChangeHoldState(binding.bottomFloatingPanel.fabHold.isOn)
            }

            fabHold.setOnChangeCallback {
                updateBottomFloatingPanelIfNeeded()
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
     * When the meeting change, will update the panel
     *
     * @param group The flag that determine if this meeting is group call
     */
    fun updateMeetingType(group: Boolean) {
        isGroup = group
        updatePanel()
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
     * Get current state
     */
    fun getState(): Int {
        return bottomSheetBehavior.state
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

    /**
     * Update the mic icon, also update the own item's mic icon
     *
     * @param micOn
     */
    fun updateMicIcon(micOn: Boolean) {
        savedMicState = micOn
        floatingPanelView.fabMic.isOn = micOn
        participantsAdapter.updateIcon(ParticipantsAdapter.MIC, micOn)
    }

    /**
     * Update the cam icon, also update the own item's cam icon
     *
     * @param micOn
     */
    fun updateCamIcon(camOn: Boolean) {
        savedCamState = camOn
        floatingPanelView.fabCam.isOn = camOn
        participantsAdapter.updateIcon(ParticipantsAdapter.CAM, camOn)
    }

    fun enableHoldIcon(isEnabled: Boolean, isHold: Boolean) {
        floatingPanelView.fabHold.enable = isEnabled
        updateHoldIcon(isHold)
    }

    fun changeOnHoldIcon(isAnotherCallOnHold: Boolean) {
        if (isAnotherCallOnHold) {
            changeOnHoldIconDrawable(true)
            floatingPanelView.fabHold.isOn = true
        } else {
            floatingPanelView.fabHold.isOn = false
            changeOnHoldIconDrawable(false)
        }
    }

    fun changeOnHoldIconDrawable(existsAnotherCallOnHold: Boolean) {
        if (existsAnotherCallOnHold) {
            floatingPanelView.fabHold.setOnIcon(R.drawable.ic_call_swap)
        } else {
            floatingPanelView.fabHold.setOnIcon(R.drawable.ic_transfers_pause)
        }
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

    /**
     * Change the panel's width for landscape and portrait screen
     *
     */
    fun updateWidth(orientation: Int, widthPixels: Int) {
        val params = floatingPanelView.root.layoutParams
        params.width = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            widthPixels / 2
        } else FrameLayout.LayoutParams.MATCH_PARENT

        floatingPanelView.root.layoutParams = params
    }

    /**
     * Update UI for privilege changing
     *
     * @param ownPrivileges current privilege
     */
    fun updatePrivilege(ownPrivileges: Int) {
        participantsAdapter.updateIcon(
            ParticipantsAdapter.MODERATOR,
            ownPrivileges == MegaChatRoom.PRIV_MODERATOR
        )
        updateShareAndInviteButton()
    }

    /**
     * Check changes in remote A/V flags
     *
     * @param session MegaChatSession
     */
    fun updateRemoteAudioVideo(session: MegaChatSession) {
        participantsAdapter.updateParticipantAudioVideo(session.peerid, session.clientid)
    }

    fun updateRemotePrivileges(updateParticipantsPrivileges: MutableSet<Participant>) {
        updateParticipantsPrivileges.forEach { participant ->
            participantsAdapter.updateParticipantPermission(
                participant.peerId,
                participant.clientId
            )
        }
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


    companion object {
        private const val BOTTOM_PANEL_MIN_ALPHA = 0.32F
        private const val BOTTOM_PANEL_PROPERTY_UPDATER_OFFSET_THRESHOLD = 0.5F
    }
}


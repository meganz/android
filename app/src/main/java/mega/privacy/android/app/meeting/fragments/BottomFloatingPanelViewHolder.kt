package mega.privacy.android.app.meeting.fragments

import android.animation.ArgbEvaluator
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
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
import mega.privacy.android.app.meeting.listeners.BottomFloatingPanelListener
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaChatSession

/**
 * Bottom Panel view holder package the view and logic code of floating panel
 *
 * @property inMeetingViewModel InMeetingViewModel, get some values and do some logic actions
 * @property binding  InMeetingFragmentBinding, get views from this binding
 * @property listener listen to the actions of all buttons
 * @property isGroup determine if the current chat is group
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

    private var popWindow: PopupWindow? = null

    /**
     * Save the Mic & Cam state, for revering state when hold state changed
     */
    private var savedMicState: Boolean = false
    private var savedCamState: Boolean = false
    private var savedSpeakerState: AppRTCAudioManager.AudioDevice =
        AppRTCAudioManager.AudioDevice.SPEAKER_PHONE
    private val participantsAdapter = ParticipantsAdapter(inMeetingViewModel, listener)

    private var currentHeight = 0

    val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        initTipsAndRatio()
    }

    init {
        initButtonsState()
        setupBottomSheet()
        listenButtons()
        setupRecyclerView()
        updateShareAndInviteButton()
        updatePanel()
        initTipsAndRatio()

        binding.root.addOnLayoutChangeListener(layoutListener)
    }

    /**
     * Remove listener and dismiss the pop window when activity is destroyed
     */
    fun onDestroy() {
        binding.root.removeOnLayoutChangeListener(layoutListener)
        popWindow?.dismiss()
    }

    /**
     * Init the tips window & Calculate the ratio for bottom sheet behavior
     */
    private fun initTipsAndRatio() {
        floatingPanelView.backgroundMask.post {
            if (binding.root.measuredHeight == currentHeight)
                return@post

            currentHeight = binding.root.measuredHeight
            if (inMeetingViewModel.shouldShowTips()) {
                initPopWindow(floatingPanelView.backgroundMask)
            }

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
     * Determine if the tips is showing
     */
    fun isPopWindowShowing(): Boolean = popWindow != null && popWindow?.isShowing == true

    /**
     * Dismiss the tips window
     */
    fun dismissPopWindow() {
        popWindow?.dismiss()
        inMeetingViewModel.updateShowTips()
    }

    /**
     * Init the tips window
     *
     * @param anchor the anchor view, the tips widow should show base on it's location
     */
    fun initPopWindow(anchor: View) {
        if (inMeetingViewModel.isOneToOneCall()) {
            return
        }

        val view: View = LayoutInflater.from(context)
            .inflate(R.layout.view_tip_meeting_bottom_panel, null, false)
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

        if (isPopWindowShowing()) {
            popWindow?.dismiss()
        }

        popWindow = PopupWindow(
            view,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isFocusable = false
            isOutsideTouchable = false
            setBackgroundDrawable(null)
        }

        val confirm = view.findViewById<Button>(R.id.bt_ok)
        confirm.setOnClickListener {
            dismissPopWindow()
        }

        val location = intArrayOf(0, 0)
        anchor.getLocationInWindow(location)

        popWindow?.let {
            it.showAtLocation(
                anchor,
                Gravity.NO_GRAVITY,
                (location[0] + anchor.width / 2) - view.measuredWidth / 2,
                location[1] - view.measuredHeight
            )
        }
    }

    /**
     * Expanded bottom sheet when the meeting is group chat
     * If the meeting is one-to-one chat, just show the control button, and would not let user drag the bottom panel
     */
    private fun updatePanel(shouldExpand: Boolean = true) {
        if (shouldExpand) {
            collapse()
        }

        floatingPanelView.indicator.isVisible = isGroup
        updateShareAndInviteButton()
    }


    fun genCheckedDrawable(background: Int): Drawable {
        val roundRect = GradientDrawable()
        roundRect.shape = GradientDrawable.RECTANGLE
        roundRect.cornerRadius = context.resources.getDimension(R.dimen.elevation_upgrade_low)
        roundRect.setColor(background)

        val round2 = GradientDrawable()
        round2.shape = GradientDrawable.RECTANGLE
        round2.setColor(ContextCompat.getColor(context, R.color.white_alpha_007))
        round2.cornerRadius = context.resources.getDimension(R.dimen.elevation_upgrade_low)
        val insetLayer2 = InsetDrawable(round2, 0, 0, 0, 0)

        return LayerDrawable(arrayOf(roundRect, insetLayer2))
    }

    /**
     * Init the visibility of `ShareLink` & `Invite` Button
     *
     */
    fun updateShareAndInviteButton() {
        floatingPanelView.shareLink.isVisible = inMeetingViewModel.isLinkVisible()
        floatingPanelView.invite.isVisible = inMeetingViewModel.isLinkVisible()
        floatingPanelView.guestShareLink.apply {
            isVisible = inMeetingViewModel.isGuestLinkVisible()
            text = inMeetingViewModel.getGuestLinkTitle()
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                if (inMeetingViewModel.isModeratorOfPrivateRoom()) R.drawable.ic_invite_contact else R.drawable.ic_social_share_white,
                0,
                0,
                0
            )
        }
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
                if (slideOffset > 0.1f) {
                    dismissPopWindow()
                }
            }
        })

        initUpdaters()
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
                listener.onShareLink(true)
            }

            guestShareLink.setOnClickListener {
                if (inMeetingViewModel.isModeratorOfPrivateRoom()) {
                    listener.onInviteParticipants()
                } else {
                    listener.onShareLink(true)
                }
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
        updatePanel(false)
        updatePrivilege(inMeetingViewModel.getOwnPrivileges())
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
                        ContextCompat.getColor(
                            context, R.color.grey_070_dark_grey_066
                        ), ContextCompat.getColor(
                            context, R.color.white_dark_grey
                        )
                    ) as Int

                    view.background =
                        if (Util.isDarkMode(context)) {
                            genCheckedDrawable(background)
                        } else {
                            val grad: GradientDrawable = view.background as GradientDrawable
                            grad.setColor(background)
                            grad
                        }
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
        propertyUpdaters.add(
            propertyUpdater(
                fab, 0.0f, 1.0f
            ) { view, value ->
                if (view.isOn || !view.enable) {
                    run {
                        val argbEvaluator = ArgbEvaluator()
                        val background = argbEvaluator.evaluate(
                            value,
                            ContextCompat.getColor(context, R.color.grey_032_white_054),
                            ContextCompat.getColor(context, R.color.grey_060_white_054)
                        ) as Int

                        view.backgroundTintList = ColorStateList.valueOf(background)
                    }
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
     */
    fun expand() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomFloatingPanelExpanded = true
        onBottomFloatingPanelSlide(1F)
    }

    /**
     * Update the mic icon, also update the own item's mic icon
     *
     * @param micOn True, if the audio is on. False, if the audio is off
     */
    fun updateMicIcon(micOn: Boolean) {
        savedMicState = micOn
        floatingPanelView.fabMic.isOn = micOn
        participantsAdapter.updateIcon(ParticipantsAdapter.MIC, micOn)
    }

    /**
     * Update the cam icon, also update the own item's cam icon
     *
     * @param camOn True, if the video is on. False, if the video is off
     */
    fun updateCamIcon(camOn: Boolean) {
        savedCamState = camOn
        floatingPanelView.fabCam.isOn = camOn
        participantsAdapter.updateIcon(ParticipantsAdapter.CAM, camOn)
    }

    /**
     * Enabling or disabling the on hold button
     *
     * @param isEnabled True, if enabled. False, if disabled
     * @param isHold True, if it is an on hold button. False, if it is switch call button
     */
    fun enableHoldIcon(isEnabled: Boolean, isHold: Boolean) {
        floatingPanelView.fabHold.enable = isEnabled
        updateHoldIcon(isHold)
    }

    /**
     * Method to control when to switch the button from on hold to switch call
     *
     * @param isAnotherCallOnHold True, if another call is in progress. False, if not
     */
    fun changeOnHoldIcon(isAnotherCallOnHold: Boolean) {
        if (isAnotherCallOnHold) {
            changeOnHoldIconDrawable(true)
            floatingPanelView.fabHold.isOn = true
        } else {
            floatingPanelView.fabHold.isOn = false
            changeOnHoldIconDrawable(false)
        }
    }

    /**
     * Method of changing the on hold button icon appropriately
     *
     * @param existsAnotherCallOnHold True, if another call is in progress. False, if not.
     */
    fun changeOnHoldIconDrawable(existsAnotherCallOnHold: Boolean) {
        floatingPanelView.fabHold.setOnIcon(
            if (existsAnotherCallOnHold) R.drawable.ic_call_swap
            else R.drawable.ic_transfers_pause
        )
    }

    /**
     * Method that enables or disables the mic and camera buttons when the call on hold status is changed
     *
     * @param isHold True, if the call is on hold. False, otherwise
     */
    fun updateHoldIcon(isHold: Boolean) {
        floatingPanelView.fabHold.isOn = !isHold
        floatingPanelView.fabMic.enable = !isHold
        floatingPanelView.fabCam.enable = !isHold
    }

    /**
     * Method that updates the speaker icon according to the selected AudioDevice
     *
     * @param device Current device selected
     */
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

    /**
     * Check changes in remote chat privileges
     *
     * @param updateParticipantsPrivileges List of participants to be updated
     */
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

    fun updateMicPermissionWaring(isGranted: Boolean) {
        floatingPanelView.micWarning.isVisible = !isGranted
    }

    fun updateCamPermissionWaring(isGranted: Boolean) {
        floatingPanelView.camWarning.isVisible = !isGranted
    }

    companion object {
        private const val BOTTOM_PANEL_MIN_ALPHA = 0.32F
        private const val BOTTOM_PANEL_PROPERTY_UPDATER_OFFSET_THRESHOLD = 0.5F
    }
}

